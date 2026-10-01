package com.zonerental.testsupport

/** Reads the built-in message defaults from ConfigManager.loadMessages() source. */
object MessageDefaults {

    private const val CONFIG_MANAGER = "src/main/kotlin/com/zonerental/config/ConfigManager.kt"
    private val ENTRY = Regex("^\\s*\"([a-z0-9-]+)\" to \"((?:[^\"\\\\]|\\\\.)*)\"", RegexOption.MULTILINE)

    val defaults: Map<String, String> by lazy {
        val text = ProjectFiles.text(CONFIG_MANAGER)
        val body = text.substringAfter("private fun loadMessages()").substringBefore("// Override with config values")
        ENTRY.findAll(body).associate { it.groupValues[1] to it.groupValues[2] }
    }
}
