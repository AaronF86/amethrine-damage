val loaders = listOf("fabric", "neoforge")
val currentMc = prop("minecraft_version")

tasks.register("lint") {
    group = "verification"
    description = "Checks formatting and compiles everything, with -Pstrict failing on any warning"
    dependsOn("spotlessCheck")
    subprojects.forEach { dependsOn("${it.path}:compileJava", "${it.path}:compileTestJava") }
    dependsOn(":common:javadoc")
}

tasks.register("precommit") {
    group = "verification"
    description = "The quick checks to run before committing: lint and unit tests (run with -Pstrict, as CI does)"
    dependsOn("lint", ":common:test")
}

tasks.register("gametest") {
    group = "verification"
    description = "Runs the in-game tests for Minecraft $currentMc on both loaders"
    dependsOn(loaders.map { ":gametest-$it:gametest" })
}

val releaseJars =
    configurations.create("releaseJars") {
        isCanBeConsumed = false
        isTransitive = false
    }
dependencies {
    loaders.forEach { releaseJars(project(path = ":$it", configuration = "runtimeElements")) }
}

tasks.register<Copy>("dist") {
    description = "Copies the release jars for Minecraft $currentMc into dist/"
    from(releaseJars)
    into(layout.projectDirectory.dir("dist"))
}

val cleanDist =
    tasks.register<Delete>("cleanDist") {
        delete(layout.projectDirectory.dir("dist"))
    }

val childArgs =
    gradle.startParameter.projectProperties
        .filterKeys { it != "mc" }
        .map { (k, v) -> "-P$k=$v" } + listOfNotNull("--continue".takeIf { gradle.startParameter.isContinueOnFailure })

fun forEveryTarget(
    name: String,
    description: String,
    vararg paths: String,
): TaskProvider<Task> {
    val steps =
        releaseTargets().map { target ->
            if (target == currentMc) {
                paths.toList()
            } else {
                tasks.register<Exec>("$name-$target") {
                    commandLine(
                        listOf(
                            rootProject.file("gradlew").path,
                            *paths,
                            "-Pmc=$target",
                            "-x",
                            ":verifyRelease",
                        ) + childArgs,
                    )
                    mustRunAfter(subprojects.map { sub -> sub.tasks.matching { it.name != "modrinth" } })
                }
            }
        }
    return tasks.register(name) {
        group = "build"
        this.description = description
        dependsOn(steps)
    }
}

val buildAll = forEveryTarget("buildAll", "Builds every release target into dist/", ":dist")
buildAll.configure { dependsOn(cleanDist) }
tasks.named("dist") {
    mustRunAfter(cleanDist)
    mustRunAfter(tasks.matching { it.name.startsWith("spotless") })
}

val gametestAll = forEveryTarget("gametestAll", "Runs the in-game tests for every release target", ":gametest")
val publishAll =
    forEveryTarget("publishAll", "Publishes every release target to Modrinth", ":fabric:modrinth", ":neoforge:modrinth")

val checkRelease =
    tasks.register("checkRelease") {
        group = "verification"
        description = "Checks TANGLED_REF_NAME is an annotated v<mod_version> tag with a CHANGELOG.md section"
        val version = project.version.toString()
        val tag = providers.environmentVariable("TANGLED_REF_NAME")
        val tagType =
            tag.flatMap { ref ->
                providers
                    .exec {
                        commandLine("git", "cat-file", "-t", ref)
                        isIgnoreExitValue = true
                    }.standardOutput.asText
                    .map { it.trim() }
            }
        val changelog = layout.projectDirectory.file("CHANGELOG.md").asFile
        doLast {
            val ref = tag.orNull ?: throw GradleException("Set TANGLED_REF_NAME to the release tag")
            if (ref != "v$version") {
                throw GradleException("Tag $ref does not match mod_version=$version in gradle.properties")
            }
            if (tagType.get() != "tag") {
                throw GradleException("$ref is not an annotated tag; create it with git tag -a")
            }
            if (!changelog.exists() || changelog.readLines().none { it == "## $version" }) {
                throw GradleException("CHANGELOG.md has no '## $version' heading")
            }
        }
    }

val verifyRelease =
    tasks.register("verifyRelease") {
        group = "verification"
        description = "Runs the release checks, lint, unit tests and in-game tests, and builds every release target"
        dependsOn(checkRelease, "lint", ":common:test", gametestAll, buildAll)
    }

val uploadTangledArtifacts =
    tasks.register<UploadTangledArtifacts>("uploadTangledArtifacts") {
        group = "publishing"
        description = "Attaches the jars in dist/ to the release tag on Tangled"
        dependsOn(verifyRelease)
        mustRunAfter(publishAll)
        jars.from(
            layout.projectDirectory
                .dir("dist")
                .asFileTree
                .matching { include("*.jar") },
        )
        tag = providers.environmentVariable("TANGLED_REF_NAME")
        repoUrl = providers.environmentVariable("TANGLED_REPO_URL")
        pds = providers.environmentVariable("ATP_PDS").orElse("https://tngl.sh")
        identifier = providers.environmentVariable("ATP_IDENTIFIER")
        appPassword = providers.environmentVariable("ATP_APP_PASSWORD")
    }

tasks.register("release") {
    group = "publishing"
    description =
        "Checks the tag, runs every test, publishes every release target to Modrinth and attaches the jars on Tangled"
    dependsOn(publishAll, uploadTangledArtifacts)
}
