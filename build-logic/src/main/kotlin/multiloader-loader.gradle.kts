plugins {
    id("multiloader-common")
    id("com.modrinth.minotaur")
}

val commonPath = optionalProp("common_project") ?: ":common"

val commonJava =
    configurations.create("commonJava") {
        isCanBeResolved = true
    }
val commonResources =
    configurations.create("commonResources") {
        isCanBeResolved = true
    }

dependencies {
    compileOnly(project(commonPath)) {
        attributes {
            attribute(LOADER_ATTRIBUTE, "common")
        }
    }
    commonJava(project(path = commonPath, configuration = "commonJava"))
    commonResources(project(path = commonPath, configuration = "commonResources"))
}

tasks.compileJava {
    dependsOn(commonJava)
    source(commonJava)
}

tasks.processResources {
    dependsOn(commonResources)
    from(commonResources)
}

tasks.javadoc {
    dependsOn(commonJava)
    source(commonJava)
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(commonJava)
    from(commonJava)
    dependsOn(commonResources)
    from(commonResources)
}

val modName = prop("mod_name")
val minecraftVersion = prop("minecraft_version")
val releaseTarget = optionalProp("modrinth_game_versions")

modrinth {
    val loaderName = mapOf("fabric" to "Fabric", "neoforge" to "NeoForge").getValue(project.name)
    projectId = prop("modrinth_id")
    versionNumber = "${project.version}+$minecraftVersion-${project.name}"
    versionName = "$modName ${project.version} ($loaderName $minecraftVersion)"
    versionType = prop("modrinth_version_type")
    uploadFile.set(tasks.jar)
    gameVersions = gameVersions(releaseTarget)
    loaders = listOf(project.name)
    detectLoaders = false
    val changelogFile = rootProject.file("CHANGELOG.md")
    if (changelogFile.exists()) {
        val section =
            changelogFile
                .readLines()
                .dropWhile { it.trim() != "## ${project.version}" }
                .drop(1)
                .takeWhile { !it.startsWith("## ") }
        changelog = section.joinToString("\n").trim()
    }
    debugMode = providers.gradleProperty("modrinth_debug").map { it.toBoolean() }.getOrElse(false)
}

tasks.named("modrinth") {
    dependsOn(":verifyRelease")
    val mc = minecraftVersion
    val isReleaseTarget = releaseTarget != null
    doFirst {
        if (!isReleaseTarget) {
            throw GradleException(
                "Minecraft $mc is a dev/test target, not a release target. Publish from a versions/ file that sets modrinth_game_versions.",
            )
        }
    }
}
