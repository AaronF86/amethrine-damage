plugins {
    id("multiloader-common")
    id("net.neoforged.moddev")
}

neoForge {
    neoFormVersion = prop("neo_form_version")
    addModdingDependenciesTo(sourceSets.test.get())
    val at = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }
}

dependencies {
    compileOnly(libs.mixin)
    compileOnly(libs.mixinextras.common)
    annotationProcessor(libs.mixinextras.common)
}

val commonJava =
    configurations.create("commonJava") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
val commonResources =
    configurations.create("commonResources") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }

artifacts {
    val main = sourceSets.main.get()
    main.java.srcDirs
        .filter { it.exists() }
        .forEach { add(commonJava.name, it) }
    main.resources.srcDirs
        .filter { it.exists() }
        .forEach { add(commonResources.name, it) }
}

markLoader("common")

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
