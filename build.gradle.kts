plugins {
    alias(libs.plugins.loom) apply false
    alias(libs.plugins.moddev) apply false
    alias(libs.plugins.spotless)
    id("multiloader-ci")
}

repositories {
    mavenCentral()
}

spotless {
    java {
        target("*/src/**/*.java", "gametest/*/src/**/*.java")
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts", "*/*.gradle.kts", "gametest/*/*.gradle.kts", "build-logic/src/**/*.kts", "build-logic/src/**/*.kt")
        ktlint()
    }
    format("misc") {
        target("*.md", "docs/*.md", "*/src/**/*.json", "gametest/*/src/**/*.json", ".tangled/workflows/*.yml", ".githooks/*")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
        if (providers.gradleProperty("strict").isPresent) {
            options.compilerArgs.add("-Werror")
        }
    }
    tasks.withType<Javadoc>().configureEach {
        // Missing @param/@return is fine: the API uses one-line javadoc
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
    }
}
