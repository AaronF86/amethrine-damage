plugins {
    id("multiloader-loader")
    id("net.fabricmc.fabric-loom")
}

dependencies {
    minecraft("com.mojang:minecraft:${prop("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${prop("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_version")}")
}

modrinth {
    dependencies {
        required.project("fabric-api")
    }
}

loom {
    val aw = project(":common").file("src/main/resources/${prop("mod_id")}.accesswidener")
    if (aw.exists()) {
        accessWidenerPath = aw
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("runs/client")
        }
        named("server") {
            server()
            configName = "Fabric Server"
            ideConfigGenerated(true)
            runDir("runs/server")
        }
    }
}

markLoader("fabric", extra = listOf("includeInternal", "modCompileClasspath"))
