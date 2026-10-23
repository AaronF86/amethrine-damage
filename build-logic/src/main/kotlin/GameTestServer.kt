import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.w3c.dom.Element
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

interface GameTestSlots : BuildService<BuildServiceParameters.None>

fun gameTestJobs(): Int {
    val meminfo = File("/proc/meminfo")
    if (!meminfo.canRead()) {
        return 1
    }
    var kib =
        meminfo
            .readLines()
            .firstOrNull { it.startsWith("MemAvailable:") }
            ?.split(Regex("\\s+"))
            ?.get(1)
            ?.toLongOrNull() ?: return 1
    for (limit in listOf("/sys/fs/cgroup/memory.max", "/sys/fs/cgroup/memory/memory.limit_in_bytes")) {
        File(limit)
            .takeIf { it.canRead() }
            ?.readText()
            ?.trim()
            ?.toLongOrNull()
            ?.let { kib = minOf(kib, it / 1024) }
    }
    return maxOf(1, (kib / (2L * 1024 * 1024)).toInt())
}

// The installed server in [serverDir] is kept between runs; each run starts from a fresh world and mods folder
abstract class GameTestServer : DefaultTask() {
    @get:Internal
    abstract val label: Property<String>

    @get:Input
    abstract val minecraftVersion: Property<String>

    /** The Fabric Loader or NeoForge version to run */
    @get:Input
    abstract val loaderVersion: Property<String>

    /** The Fabric installer's server launcher, or the NeoForge installer */
    @get:InputFiles
    abstract val installer: ConfigurableFileCollection

    @get:InputFiles
    abstract val mods: ConfigurableFileCollection

    @get:Internal
    abstract val serverDir: DirectoryProperty

    @get:Internal
    abstract val reportFile: RegularFileProperty

    @get:Internal
    abstract val logFile: RegularFileProperty

    @get:Internal
    abstract val timeoutSeconds: Property<Long>

    @get:Internal
    abstract val eula: Property<String>

    @get:Nested
    abstract val javaLauncher: Property<JavaLauncher>

    @get:Inject
    abstract val workers: WorkerExecutor

    protected abstract fun serverRun(): Class<out ServerRun>

    @TaskAction
    fun run() {
        if (eula.orNull != "true") {
            throw GradleException(
                "The test server needs the Minecraft EULA (https://aka.ms/MinecraftEULA). Set MINECRAFT_EULA=true to accept it.",
            )
        }
        // A work item, so the runs in one project can go in parallel
        workers.noIsolation().submit(serverRun()) {
            // .set() rather than =, which needs an import IDEs remove as unused
            label.set(this@GameTestServer.label)
            minecraftVersion.set(this@GameTestServer.minecraftVersion)
            loaderVersion.set(this@GameTestServer.loaderVersion)
            installer.from(this@GameTestServer.installer)
            mods.from(this@GameTestServer.mods)
            serverDir.set(this@GameTestServer.serverDir)
            reportFile.set(this@GameTestServer.reportFile)
            logFile.set(this@GameTestServer.logFile)
            timeoutSeconds.set(this@GameTestServer.timeoutSeconds)
            java.set(
                this@GameTestServer
                    .javaLauncher
                    .get()
                    .executablePath.asFile.absolutePath,
            )
        }
    }
}

abstract class FabricGameTestServer : GameTestServer() {
    override fun serverRun() = FabricServerRun::class.java
}

abstract class NeoForgeGameTestServer : GameTestServer() {
    override fun serverRun() = NeoForgeServerRun::class.java
}

interface ServerRunParameters : WorkParameters {
    val label: Property<String>
    val minecraftVersion: Property<String>
    val loaderVersion: Property<String>
    val installer: ConfigurableFileCollection
    val mods: ConfigurableFileCollection
    val serverDir: DirectoryProperty
    val reportFile: RegularFileProperty
    val logFile: RegularFileProperty
    val timeoutSeconds: Property<Long>
    val java: Property<String>
}

private const val REPORT_FILE = "report.xml"

abstract class ServerRun : WorkAction<ServerRunParameters> {
    /** Installs the server into [dir] if it is not there yet, and returns the command that runs the tests */
    protected abstract fun prepare(dir: File): List<String>

    protected val java: String get() = parameters.java.get()

    protected fun runSetup(
        dir: File,
        command: List<String>,
    ) {
        val status =
            ProcessBuilder(command)
                .directory(dir)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(parameters.logFile.get().asFile))
                .start()
                .waitFor()
        if (status != 0) {
            throw GradleException("${command.joinToString(" ")} failed (log: ${parameters.logFile.get().asFile})")
        }
    }

    override fun execute() {
        val dir = parameters.serverDir.get().asFile
        val report = parameters.reportFile.get().asFile
        val log = parameters.logFile.get().asFile
        val label = parameters.label.get()

        listOf("mods", "gametestserver", "logs", REPORT_FILE).forEach { dir.resolve(it).deleteRecursively() }
        dir.resolve("mods").mkdirs()
        parameters.mods.forEach { it.copyTo(dir.resolve("mods/${it.name}")) }
        dir.resolve("eula.txt").writeText("eula=true\n")
        Files.deleteIfExists(report.toPath())
        report.parentFile.mkdirs()
        log.parentFile.mkdirs()
        log.writeText("")

        val timeout = parameters.timeoutSeconds.get()
        val process =
            ProcessBuilder(prepare(dir))
                .directory(dir)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(log))
                .start()
        val finished = process.waitFor(timeout, TimeUnit.SECONDS)
        if (!finished) {
            process.descendants().forEach { it.destroyForcibly() }
            process.destroyForcibly().waitFor()
        }
        dir.resolve(REPORT_FILE).takeIf { it.exists() }?.copyTo(report, overwrite = true)

        val (summary, failures) = if (report.exists()) summarize(report) else "no test report was written" to emptyList()
        val passed = finished && process.exitValue() == 0 && report.exists()
        val logger = Logging.getLogger(ServerRun::class.java)
        if (passed) {
            logger.lifecycle("PASS  $label: $summary")
            return
        }
        val reason = if (finished) summary else "timed out after ${timeout}s, $summary"
        val tail = if (report.exists()) emptyList() else log.readLines().takeLast(30)
        throw GradleException(
            (listOf("FAIL  $label: $reason") + failures + tail + "Server log: $log").joinToString("\n"),
        )
    }

    private fun summarize(report: File): Pair<String, List<String>> {
        val cases =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(report)
                .getElementsByTagName("testcase")
        val failures =
            (0 until cases.length).map { cases.item(it) as Element }.mapNotNull { case ->
                (case.getElementsByTagName("failure").item(0) as Element?)?.let {
                    "  - ${case.getAttribute("name")}: ${it.getAttribute("message")}"
                }
            }
        return "${cases.length} tests, ${failures.size} failed" to failures
    }
}

abstract class FabricServerRun : ServerRun() {
    // What Loom's ServerProductionRunTask does: the installer's server launcher reads the versions from an
    // install.properties on its classpath, then downloads and starts the server
    override fun prepare(dir: File): List<String> {
        val installProperties = dir.resolve("install-properties.jar")
        ZipOutputStream(installProperties.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("install.properties"))
            zip.write(
                "fabric-loader-version=${parameters.loaderVersion.get()}\ngame-version=${parameters.minecraftVersion.get()}\n"
                    .toByteArray(),
            )
        }
        val classpath = (parameters.installer.files + installProperties).joinToString(File.pathSeparator)
        return listOf(
            java,
            "-Xmx1G",
            "-Dfabric-api.gametest",
            "-Dfabric-api.gametest.report-file=${dir.resolve(REPORT_FILE)}",
            "-cp",
            classpath,
            "net.fabricmc.installer.ServerLauncher",
            "nogui",
        )
    }
}

abstract class NeoForgeServerRun : ServerRun() {
    override fun prepare(dir: File): List<String> {
        val argsFile = dir.resolve("libraries/net/neoforged/neoforge/${parameters.loaderVersion.get()}/unix_args.txt")
        if (!argsFile.exists()) {
            runSetup(dir, listOf(java, "-jar", parameters.installer.singleFile.absolutePath, "--install-server", "."))
        }
        dir.resolve("gametest_args.txt").writeText(
            argsFile.readText().replace("net.neoforged.fml.startup.Server", "net.neoforged.fml.startup.GameTestServer"),
        )
        return listOf(java, "-Xmx1G", "-Dneoforge.enableGameTest=true", "@gametest_args.txt", "--report", REPORT_FILE)
    }
}
