package com.zonerental.consistency

import com.zonerental.testsupport.ProjectFiles
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression 3.0.2: subcommands registered in ZoneRental.registerCommands() were missing from
 * the conflict-detection list in checkPrefixConflicts(), so prefix conflicts went undetected.
 */
class CommandRegistrationConsistencyTest {

    private val mainClass by lazy { ProjectFiles.text("src/main/java/com/zonerental/ZoneRental.java") }

    private val registered by lazy {
        Regex("registerCommandWithPrefix\\(\\s*commandMap\\s*,\\s*activePrefix\\s*,\\s*\"([a-z]*)\"")
            .findAll(mainClass).map { it.groupValues[1] }.filter { it.isNotEmpty() }.toSet()
    }

    private val conflictChecked by lazy {
        val array = Regex("String\\[\\]\\s+subcommands\\s*=\\s*\\{([^}]*)\\}").find(mainClass)
            ?: error("subcommands array not found in checkPrefixConflicts()")
        Regex("\"([a-z]+)\"").findAll(array.groupValues[1]).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun `registered subcommands are found`() {
        assertTrue(registered.size >= 15, "Found only $registered")
    }

    @Test
    fun `every registered subcommand is checked for prefix conflicts`() {
        assertEquals(registered, conflictChecked,
            "Keep checkPrefixConflicts() in sync with registerCommands() in ZoneRental.java")
    }
}
