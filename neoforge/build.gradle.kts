plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev")
}

val modId = prop("mod_id")

neoForge {
    version = prop("neoforge_version")
    val at = project(":common").file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }
    runs {
        configureEach {
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            ideName = "NeoForge ${name.replaceFirstChar { it.uppercase() }} (${project.path})"
        }
        register("client") {
            client()
            gameDirectory = file("runs/client")
        }
        register("data") {
            clientData()
            gameDirectory = file("runs/data")
            programArguments.addAll(
                "--mod",
                modId,
                "--all",
                "--output",
                file("src/generated/resources/").absolutePath,
                "--existing",
                file("src/main/resources/").absolutePath,
            )
        }
        register("server") {
            server()
            gameDirectory = file("runs/server")
        }
    }
    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

sourceSets.main {
    resources.srcDir("src/generated/resources")
}

markLoader("neoforge", perSourceSet = { listOf(it.getTaskName(null, "jarJar")) })
