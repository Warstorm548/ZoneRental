package com.zonerental.testsupport

import java.io.File

/**
 * Access to project files for consistency tests.
 * Gradle runs tests with workingDir = projectDir (see build.gradle.kts).
 */
object ProjectFiles {

    val root: File = File(".").canonicalFile

    fun file(path: String): File {
        val f = File(root, path)
        require(f.exists()) { "Expected project file not found: ${f.path}" }
        return f
    }

    fun text(path: String): String = file(path).readText()

    val configYml: String get() = text("src/main/resources/config.yml")
    val pluginYml: String get() = text("src/main/resources/plugin.yml")
    val changelog: String get() = text("CHANGELOG.md")
    val buildScript: String get() = text("build.gradle.kts")

    /** All main Kotlin and Java sources, keyed by path relative to the project root. */
    val mainSources: Map<String, String> by lazy {
        listOf("src/main/kotlin", "src/main/java")
            .map { File(root, it) }
            .filter { it.exists() }
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }.toList() }
            .associate { it.relativeTo(root).path to it.readText() }
    }
}
