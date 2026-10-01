package com.zonerental.consistency

import com.zonerental.testsupport.MessageDefaults
import com.zonerental.testsupport.ProjectFiles
import com.zonerental.testsupport.SourceScanner
import com.zonerental.testsupport.YamlFiles
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks every ConfigManager.getMessage("key", "{ph}", value, ...) call against config.yml.
 */
class MessageUsageConsistencyTest {

    companion object {
        /** Known issue: used by the code but missing from config.yml `messages:` (falls back to the built-in default). */
        val KNOWN_KEYS_MISSING_FROM_CONFIG = setOf("ezchestshop-removed")

        /** Known issue: message text contains placeholders that the code never fills in (`{current}/{max}`). */
        val KNOWN_UNFILLED_PLACEHOLDERS = setOf("max-rentals-reached", "max-extensions-reached")

        private val PLACEHOLDER = Regex("\\{([a-zA-Z_-]+)\\}")
    }

    private val configMessages by lazy {
        YamlFiles.section(YamlFiles.load(ProjectFiles.configYml), "messages").mapValues { it.value.toString() }
    }

    private data class Usage(val file: String, val key: String, val placeholders: Set<String>)

    private val usages by lazy {
        SourceScanner.findCalls(ProjectFiles.mainSources, "getMessage").mapNotNull { call ->
            val key = call.args.firstOrNull()?.let(SourceScanner::stringLiteral) ?: return@mapNotNull null
            val supplied = call.args.drop(1).filterIndexed { i, _ -> i % 2 == 0 }
                .mapNotNull(SourceScanner::stringLiteral).toSet()
            Usage(call.file, key, supplied)
        }
    }

    @Test
    fun `scanner finds the message calls`() {
        assertTrue(usages.size > 40, "Expected many getMessage calls, found ${usages.size}")
    }

    @Test
    fun `every used key has text somewhere`() {
        val missing = usages.map { it.key }.filter { it !in configMessages && it !in MessageDefaults.defaults }.toSet()
        assertEquals(emptySet(), missing, "These keys would render as 'Missing message'")
    }

    @Test
    fun `every used key is configurable in config yml except known issues`() {
        val missing = usages.map { it.key }.filter { it !in configMessages }.toSet()
        assertEquals(KNOWN_KEYS_MISSING_FROM_CONFIG, missing)
    }

    @Test
    fun `placeholders in message text are supplied by every call site except known issues`() {
        val unfilled = mutableMapOf<String, MutableList<String>>()
        for (usage in usages) {
            val text = configMessages[usage.key] ?: MessageDefaults.defaults[usage.key] ?: continue
            val needed = PLACEHOLDER.findAll(text).map { "{${it.groupValues[1]}}" }.toSet()
            val missing = needed - usage.placeholders
            if (missing.isNotEmpty()) unfilled.getOrPut(usage.key) { mutableListOf() } += "${usage.file}: missing $missing"
        }
        assertEquals(KNOWN_UNFILLED_PLACEHOLDERS, unfilled.keys,
            "Unfilled placeholders differ from the known-issue allowlist:\n" +
            unfilled.entries.joinToString("\n") { "${it.key} -> ${it.value}" })
    }
}
