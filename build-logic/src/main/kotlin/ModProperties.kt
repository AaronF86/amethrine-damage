import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import java.util.Properties

/** A mod property that must be set */
fun Project.prop(name: String): String = property(name)?.toString() ?: throw IllegalStateException("Property $name is not set")

/** A mod property that may be missing, such as compat_src or modrinth_game_versions */
fun Project.optionalProp(name: String): String? = findProperty(name)?.toString()

fun Project.versionProps(mc: String): Map<String, String> {
    val file = rootProject.file("versions/$mc.properties")
    if (!file.exists()) {
        throw GradleException("No version file for Minecraft $mc, expected $file")
    }
    val props = Properties()
    file.inputStream().use { props.load(it) }
    return props.entries.associate { (k, v) -> k.toString() to v.toString() }
}

fun gameVersions(list: String?): List<String> =
    list
        .orEmpty()
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

/** Every release target: the versions/ files that set modrinth_game_versions */
fun Project.releaseTargets(): List<String> =
    rootProject
        .file("versions")
        .listFiles { file -> file.extension == "properties" }
        .orEmpty()
        .map { it.nameWithoutExtension }
        .filter { versionProps(it)["modrinth_game_versions"] != null }
        .sorted()

/** The mcgradleconventions attribute that tells common, fabric and neoforge variants apart */
val LOADER_ATTRIBUTE: Attribute<String> = Attribute.of("io.github.mcgradleconventions.loader", String::class.java)

/**
 * Marks this project's published variants, plus each source set's classpaths and [perSourceSet] configurations,
 * with the loader attribute. [extra] names more configurations the loader's plugin adds.
 */
@Suppress("unused")
fun Project.markLoader(
    loader: String,
    extra: List<String> = emptyList(),
    perSourceSet: (SourceSet) -> List<String> = { emptyList() },
) {
    (listOf("apiElements", "runtimeElements", "sourcesElements", "javadocElements") + extra).forEach { name ->
        configurations.named(name) { attributes.attribute(LOADER_ATTRIBUTE, loader) }
    }
    extensions.getByType<SourceSetContainer>().configureEach {
        (
            listOf(
                compileClasspathConfigurationName,
                runtimeClasspathConfigurationName,
            ) + perSourceSet(this)
        ).forEach { name ->
            configurations.named(name) { attributes.attribute(LOADER_ATTRIBUTE, loader) }
        }
    }
}
