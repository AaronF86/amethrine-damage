import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import javax.inject.Inject

private const val ARTIFACT_COLLECTION = "sh.tangled.repo.artifact"

// Tangled lists release files as sh.tangled.repo.artifact records naming the tag object, so the tag must be annotated.
// An earlier record for the same jar and tag is deleted once its replacement exists, so a failed run can be repeated.
abstract class UploadTangledArtifacts : DefaultTask() {
    @get:InputFiles
    abstract val jars: ConfigurableFileCollection

    @get:Input
    abstract val tag: Property<String>

    @get:Input
    abstract val repoUrl: Property<String>

    @get:Input
    abstract val pds: Property<String>

    @get:Input
    abstract val identifier: Property<String>

    @get:Internal
    abstract val appPassword: Property<String>

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun upload() {
        // Made here rather than in a field, which the configuration cache would try to store
        val http = HttpClient.newHttpClient()
        val tag = required(tag, "Set TANGLED_REF_NAME to the release tag")
        val repoDid =
            required(
                repoUrl,
                "Set TANGLED_REPO_URL to the repository URL, https://<knot>/<repo did>",
            ).substringAfterLast('/')
        val identifier = required(identifier, "Set ATP_IDENTIFIER to the handle or DID that owns the repository")
        val password = required(appPassword, "Set ATP_APP_PASSWORD to an app password for that account")
        val pds = pds.get()
        // The record wants the tag object's raw 20-byte id, base64 encoded without padding
        val tagBytes = Base64.getEncoder().withoutPadding().encodeToString(hexToBytes(annotatedTag(tag)))

        val session =
            xrpc(
                http,
                post(
                    "$pds/xrpc/com.atproto.server.createSession",
                    json(mapOf("identifier" to identifier, "password" to password)),
                ),
            )
        val jwt = session["accessJwt"] as String
        val did = session["did"] as String

        // The record names the repository by the at:// URI of its sh.tangled.repo record, found by the repo's DID
        val repoUri =
            records(http, pds, did, "sh.tangled.repo")
                .firstOrNull { value(it)["repoDid"] == repoDid }
                ?.get("uri") as String?
                ?: throw GradleException("No sh.tangled.repo record for $repoDid in $identifier's repository")
        val existing = records(http, pds, did, ARTIFACT_COLLECTION)

        for (jar in jars.files.sortedBy { it.name }) {
            val old =
                existing
                    .filter { value(it)["name"] == jar.name && (value(it)["tag"] as Map<*, *>?)?.get("\$bytes") == tagBytes }
                    .map { (it["uri"] as String).substringAfterLast('/') }

            logger.lifecycle("Attaching ${jar.name} to $tag")
            val blob =
                xrpc(
                    http,
                    HttpRequest
                        .newBuilder(URI("$pds/xrpc/com.atproto.repo.uploadBlob"))
                        .header("Authorization", "Bearer $jwt")
                        .header("Content-Type", "application/java-archive")
                        .POST(HttpRequest.BodyPublishers.ofFile(jar.toPath()))
                        .build(),
                )["blob"]
            val record =
                mapOf(
                    "repo" to did,
                    "collection" to ARTIFACT_COLLECTION,
                    "validate" to false,
                    "record" to
                        mapOf(
                            "\$type" to ARTIFACT_COLLECTION,
                            "tag" to mapOf("\$bytes" to tagBytes),
                            "name" to jar.name,
                            "repo" to repoUri,
                            "repoDid" to repoDid,
                            "artifact" to blob,
                            "createdAt" to Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                        ),
                )
            xrpc(http, post("$pds/xrpc/com.atproto.repo.createRecord", json(record), jwt))

            for (rkey in old) {
                logger.lifecycle("Deleting the earlier ${jar.name} record ($rkey)")
                val delete = mapOf("repo" to did, "collection" to ARTIFACT_COLLECTION, "rkey" to rkey)
                xrpc(http, post("$pds/xrpc/com.atproto.repo.deleteRecord", json(delete), jwt))
            }
        }
    }

    private fun required(
        property: Property<String>,
        message: String,
    ): String = property.orNull?.takeIf { it.isNotEmpty() } ?: throw GradleException(message)

    private fun annotatedTag(tag: String): String {
        val out = ByteArrayOutputStream()
        val result =
            exec.exec {
                commandLine("git", "rev-parse", "--verify", "--quiet", "$tag^{tag}")
                standardOutput = out
                isIgnoreExitValue = true
            }
        if (result.exitValue != 0) {
            throw GradleException("$tag is not an annotated tag; create it with git tag -a")
        }
        return out.toString().trim()
    }

    private fun hexToBytes(hex: String) = ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private fun json(value: Any) = JsonOutput.toJson(value)

    private fun post(
        url: String,
        body: String,
        jwt: String? = null,
    ): HttpRequest {
        val request =
            HttpRequest
                .newBuilder(URI(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
        jwt?.let { request.header("Authorization", "Bearer $it") }
        return request.build()
    }

    private fun xrpc(
        http: HttpClient,
        request: HttpRequest,
    ): Map<*, *> {
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw GradleException("${request.method()} ${request.uri().path} failed with ${response.statusCode()}: ${response.body()}")
        }
        return JsonSlurper().parseText(response.body()) as Map<*, *>
    }

    private fun records(
        http: HttpClient,
        pds: String,
        did: String,
        collection: String,
    ): List<Map<*, *>> {
        val query = "repo=${URLEncoder.encode(did, Charsets.UTF_8)}&collection=$collection&limit=100"
        val request = HttpRequest.newBuilder(URI("$pds/xrpc/com.atproto.repo.listRecords?$query")).GET().build()
        return (xrpc(http, request)["records"] as List<*>).map { it as Map<*, *> }
    }

    private fun value(record: Map<*, *>) = record["value"] as Map<*, *>
}
