plugins {
    java
}

base {
    archivesName = "${prop("mod_id")}-${project.name}-${prop("minecraft_version")}"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(prop("java_version"))

sourceSets.main {
    java.srcDir("../common/src/main/java")
    resources.srcDir("../common/src/main/resources")
}

dependencies {
    compileOnly(project(":common")) {
        attributes {
            attribute(LOADER_ATTRIBUTE, "common")
        }
    }
}

tasks.processResources {
    val props =
        mapOf(
            "version" to project.version.toString(),
            "license" to prop("license"),
            "neoforge_loader_version_range" to prop("neoforge_loader_version_range"),
        )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml")) {
        expand(props)
    }
}

val loader = project.name.removePrefix("gametest-")

val releaseMod =
    configurations.create("releaseMod") {
        isCanBeConsumed = false
        isTransitive = false
    }
dependencies {
    releaseMod(project(path = ":$loader", configuration = "runtimeElements"))
}

val gameTestSlots =
    gradle.sharedServices.registerIfAbsent("gameTestSlots", GameTestSlots::class) {
        maxParallelUsages = providers.gradleProperty("gametest_jobs").map { it.toInt() }.getOrElse(gameTestJobs())
    }
val fabricInstaller =
    extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")
        .findVersion("fabric-installer")
        .get()
        .requiredVersion
val testedVersions = gameVersions(optionalProp("modrinth_game_versions")).ifEmpty { listOf(prop("minecraft_version")) }

fun detached(notation: String) =
    configurations.detachedConfiguration(dependencies.create(notation)).apply {
        isTransitive = false
    }

val runs =
    testedVersions.map { mc ->
        val props = versionProps(mc)
        val type = if (loader == "fabric") FabricGameTestServer::class else NeoForgeGameTestServer::class
        tasks.register("gametest-$mc", type) {
            group = "verification"
            description = "Runs the in-game tests on a $loader server for Minecraft $mc"
            label = "$loader on Minecraft $mc"
            minecraftVersion = mc
            mods.from(releaseMod, tasks.jar)
            serverDir = rootProject.layout.projectDirectory.dir(".cache/gametest/$loader-$mc")
            reportFile = rootProject.layout.buildDirectory.file("gametest/$loader-$mc.xml")
            logFile = rootProject.layout.buildDirectory.file("gametest/logs/$loader-$mc.log")
            timeoutSeconds = providers.gradleProperty("gametest_timeout").map { it.toLong() }.orElse(300L)
            eula = providers.environmentVariable("MINECRAFT_EULA")
            javaLauncher = javaToolchains.launcherFor(java.toolchain)
            usesService(gameTestSlots)
            if (loader == "fabric") {
                loaderVersion = props.getValue("fabric_loader_version")
                installer.from(detached("net.fabricmc:fabric-installer:$fabricInstaller:server"))
                // The Fabric API jar leaves out its GameTest module, which is what reads -Dfabric-api.gametest,
                // so add the version that release pins
                val fabricApi =
                    configurations.detachedConfiguration(
                        dependencies.create("net.fabricmc.fabric-api:fabric-api:${props.getValue("fabric_version")}"),
                    )
                mods.from(
                    fabricApi.incoming
                        .artifactView {
                            componentFilter {
                                it is ModuleComponentIdentifier &&
                                    it.group == "net.fabricmc.fabric-api" &&
                                    it.module in setOf("fabric-api", "fabric-gametest-api-v1")
                            }
                        }.files,
                )
            } else {
                loaderVersion = props.getValue("neoforge_version")
                installer.from(detached("net.neoforged:neoforge:${props.getValue("neoforge_version")}:installer"))
            }
        }
    }

tasks.register("gametest") {
    group = "verification"
    description = "Runs the in-game tests on a $loader server for ${testedVersions.joinToString()}"
    dependsOn(runs)
}
