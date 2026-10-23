pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven {
                    name = "Fabric"
                    url = uri("https://maven.fabricmc.net")
                }
            }
            filter {
                includeGroupAndSubgroups("net.fabricmc")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Ametrine-Damage"
includeBuild("build-logic")
include("common")
include("fabric")
include("neoforge")
include("gametest-fabric")
project(":gametest-fabric").projectDir = file("gametest/fabric")
include("gametest-neoforge")
project(":gametest-neoforge").projectDir = file("gametest/neoforge")

val mcVersion = providers.gradleProperty("mc").orElse(providers.gradleProperty("default_mc")).get()
val versionFile = file("versions/$mcVersion.properties")
if (!versionFile.exists()) {
    throw GradleException("No version file for Minecraft $mcVersion, expected $versionFile")
}
val versionProps = java.util.Properties()
versionFile.inputStream().use { versionProps.load(it) }
gradle.beforeProject {
    versionProps.forEach { (k, v) -> extra.set(k as String, v) }
    version = property("mod_version")!!
}
