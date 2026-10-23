plugins {
    `java-library`
    `maven-publish`
}

val modId = prop("mod_id")
val modName = prop("mod_name")
val modAuthor = prop("mod_author")
val minecraftVersion = prop("minecraft_version")

base {
    archivesName = "$modId-${project.name}-$minecraftVersion"
}

optionalProp("compat_src")?.let { compatSrc ->
    sourceSets.main {
        java.srcDir("src/$compatSrc/java")
        resources.srcDir("src/$compatSrc/resources")
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(prop("java_version"))
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Jar>().configureEach {
    // A local copy, since the configuration cache can't store a lambda that captures the script
    val licenseName = "LICENSE_$modName"
    from(rootProject.file("LICENSE")) {
        rename { licenseName }
    }
}

tasks.jar {
    manifest {
        attributes(
            "Specification-Title" to modName,
            "Specification-Vendor" to modAuthor,
            "Specification-Version" to archiveVersion,
            "Implementation-Title" to project.name,
            "Implementation-Version" to archiveVersion,
            "Implementation-Vendor" to modAuthor,
            "Built-On-Minecraft" to minecraftVersion,
        )
    }
}

tasks.processResources {
    val minecraftVersionRange = prop("minecraft_version_range")
    val fabricMinecraftRange =
        minecraftVersionRange.replace(Regex("""^\[\s*([^,\s]+)\s*,\s*([^)\s]+)\s*\)$"""), ">=$1 <$2-")
    val expandProps =
        mapOf(
            "version" to project.version.toString(),
            "group" to project.group.toString(), // Else we target the task's group.
            "minecraft_version" to minecraftVersion,
            "minecraft_version_range" to minecraftVersionRange,
            "fabric_minecraft_version_range" to fabricMinecraftRange,
            "fabric_version" to prop("fabric_version"),
            "fabric_loader_version" to prop("fabric_loader_version"),
            "mod_name" to modName,
            "mod_author" to modAuthor,
            "mod_id" to modId,
            "license" to prop("license"),
            "description" to project.description.orEmpty(),
            "neoforge_version" to prop("neoforge_version"),
            "neoforge_loader_version_range" to prop("neoforge_loader_version_range"),
            "credits" to prop("credits"),
            "java_version" to prop("java_version"),
        )
    val jsonExpandProps = expandProps.mapValues { (_, value) -> value.replace("\n", "\\\\n") }

    filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
        expand(expandProps)
    }

    filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "*.mixins.json")) {
        expand(jsonExpandProps)
    }

    inputs.properties(expandProps)
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
    repositories {
        System.getenv("local_maven_url")?.let { localMaven ->
            maven { url = uri(localMaven) }
        }
    }
}
