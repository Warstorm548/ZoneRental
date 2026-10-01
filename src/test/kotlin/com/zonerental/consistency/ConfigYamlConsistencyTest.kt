package com.zonerental.consistency

import com.zonerental.testsupport.MessageDefaults
import com.zonerental.testsupport.ProjectFiles
import com.zonerental.testsupport.YamlFiles
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigYamlConsistencyTest {

    companion object {
        /**
         * Known issue: `restoration:` is defined twice in config.yml (docs/reference/known-issues.md).
         * Remove from this set when fixed; the test fails if the set doesn't match reality.
         */
        val KNOWN_DUPLICATE_KEYS = setOf("restoration")

        private val LEGACY_CODE = Regex("[&§][0-9a-fk-orA-FK-OR]")
        private val LEFTOVER_TAG = Regex("<[a-z_#/!][^>]*>")
    }

    private val config by lazy { YamlFiles.load(ProjectFiles.configYml) }

    @Test
    fun `config yml has no duplicate keys except known issues`() {
        val duplicates = YamlFiles.duplicateKeyPaths(ProjectFiles.configYml).toSet()
        assertEquals(KNOWN_DUPLICATE_KEYS, duplicates,
            "Duplicate keys in config.yml differ from the known-issue allowlist. " +
            "Bukkit keeps only the last occurrence, silently ignoring the earlier block.")
    }

    @Test
    fun `plugin yml has no duplicate keys`() {
        assertEquals(emptyList(), YamlFiles.duplicateKeyPaths(ProjectFiles.pluginYml))
    }

    /** Guards the 3.2.0 Adventure migration: every formatted value must be MiniMessage, not legacy codes. */
    @Test
    fun `formatted text uses MiniMessage and no legacy colour codes`() {
        val values = formattedConfigValues() + MessageDefaults.defaults.mapKeys { "ConfigManager default ${it.key}" }
        assertTrue(values.size > 50, "Expected to find the message set, found ${values.size}")

        val miniMessage = MiniMessage.miniMessage()
        val plain = PlainTextComponentSerializer.plainText()
        val problems = mutableListOf<String>()
        for ((name, value) in values) {
            if (LEGACY_CODE.containsMatchIn(value)) problems += "$name contains a legacy colour code: $value"
            val rendered = plain.serialize(miniMessage.deserialize(value))
            if (LEFTOVER_TAG.containsMatchIn(rendered)) problems += "$name has an unknown or malformed tag: $rendered"
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun `sign formats have at most four lines`() {
        val signs = YamlFiles.section(config, "signs")
        for (key in listOf("available-format", "rented-format", "expiring-format")) {
            val lines = signs[key] as? List<*>
            assertTrue(lines != null && lines.size in 1..4, "signs.$key must have 1-4 lines")
        }
    }

    private fun formattedConfigValues(): Map<String, String> {
        val values = mutableMapOf<String, String>()
        YamlFiles.section(config, "messages").forEach { (k, v) -> if (v is String) values["messages.$k"] = v }
        YamlFiles.section(config, "signs").forEach { (k, v) ->
            if (k.endsWith("-format") && v is List<*>) v.forEachIndexed { i, line -> values["signs.$k[$i]"] = line.toString() }
        }
        (YamlFiles.section(config, "general")["prefix"] as? String)?.let { values["general.prefix"] = it }
        return values
    }
}
