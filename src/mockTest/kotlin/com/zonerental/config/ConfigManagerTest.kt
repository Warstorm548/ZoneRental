package com.zonerental.config

import com.zonerental.testsupport.MockPluginTest
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigManagerTest : MockPluginTest() {

    private val plain = PlainTextComponentSerializer.plainText()

    @Test
    fun `loads defaults from bundled config yml`() {
        val config = useRealConfigManager()
        assertEquals("zr", config.commandPrefix)
        assertEquals(100.0, config.defaultPrice)
        assertEquals(7, config.defaultDuration)
        assertEquals(10, config.maxExtensions)
        assertEquals(3, config.maxRentalsPerPlayer)
        assertEquals(4, config.availableSignFormat.size)
        assertEquals(4, config.rentedSignFormat.size)
    }

    /** Documents current behaviour: invalid prefixes fall back to "rr" (config.yml comment says "zr"; see known-issues.md). */
    @ParameterizedTest
    @CsvSource("zr, zr", "Rent, rent", "abcdefghij, abcdefghij", "a, rr", "abcdefghijk, rr", "zr!, rr", "'', rr")
    fun `command prefix validation`(input: String, expected: String) {
        bukkitConfig.set("commands.prefix", input)
        assertEquals(expected, useRealConfigManager().commandPrefix)
    }

    @Test
    fun `getMessage prepends prefix, fills placeholders and renders MiniMessage`() {
        val config = useRealConfigManager()
        val text = plain.serialize(config.getMessage("rental-success", "{region}", "shop1", "{days}", "7"))
        assertEquals("[ZoneRental] You have successfully rented shop1 for 7 days!", text)
    }

    @Test
    fun `getMessage uses config yml overrides`() {
        bukkitConfig.set("messages.no-permission", "<red>Nope")
        val text = plain.serialize(useRealConfigManager().getMessage("no-permission"))
        assertEquals("[ZoneRental] Nope", text)
    }

    @Test
    fun `getMessage falls back to built-in defaults when config omits a key`() {
        val text = plain.serialize(useRealConfigManager().getMessage("ezchestshop-removed", "{region}", "shop1"))
        assertTrue(text.contains("shop1"), text)
    }

    @Test
    fun `unknown message key renders a missing-message notice`() {
        val text = plain.serialize(useRealConfigManager().getMessage("does-not-exist"))
        assertEquals("[ZoneRental] Missing message: does-not-exist", text)
    }

    @Test
    fun `price and duration fall back to defaults without regions config`() {
        val config = useRealConfigManager()
        io.mockk.every { plugin.regionsConfig } returns RegionsConfig(plugin)
        assertEquals(100.0, config.getPriceForRegion("shop1", world))
        assertEquals(7, config.getDurationForRegion("shop1", world))
    }

    @Test
    fun `extension price per day derives from region price and duration`() {
        val config = useRealConfigManager()
        io.mockk.every { plugin.regionsConfig } returns RegionsConfig(plugin)
        assertEquals(100.0 / 7, config.getExtensionPrice("shop1", world), 1e-9)
        bukkitConfig.set("duration.add-price-per-day", 5.0)
        assertEquals(5.0, config.getExtensionPrice("shop1", world))
    }
}
