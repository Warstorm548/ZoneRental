package com.zonerental.commands

import com.zonerental.commands.OverrideSetting.ParseResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class OverrideSettingTest {

    @Test
    fun `fromName resolves every setting case-insensitively`() {
        assertSame(OverrideSetting.Price, OverrideSetting.fromName("price"))
        assertSame(OverrideSetting.Duration, OverrideSetting.fromName("DURATION"))
        assertSame(OverrideSetting.MaxExtensions, OverrideSetting.fromName("maxextensions"))
        assertSame(OverrideSetting.ExtensionPrice, OverrideSetting.fromName("extensionprice"))
        assertSame(OverrideSetting.AllowExtensions, OverrideSetting.fromName("allowextensions"))
        assertSame(OverrideSetting.ExtensionDuration, OverrideSetting.fromName("extensionduration"))
        assertNull(OverrideSetting.fromName("max-extensions"))
        assertEquals(6, OverrideSetting.all.size)
    }

    @Test
    fun `price accepts zero and positive numbers only`() {
        assertEquals(ParseResult.Success(500.0), OverrideSetting.Price.parseValue("500"))
        assertEquals(ParseResult.Success(0.0), OverrideSetting.Price.parseValue("0"))
        assertIs<ParseResult.Error>(OverrideSetting.Price.parseValue("-1"))
        assertIs<ParseResult.Error>(OverrideSetting.Price.parseValue("abc"))
    }

    @Test
    fun `duration requires a positive whole number`() {
        assertEquals(ParseResult.Success(14), OverrideSetting.Duration.parseValue("14"))
        assertIs<ParseResult.Error>(OverrideSetting.Duration.parseValue("0"))
        assertIs<ParseResult.Error>(OverrideSetting.Duration.parseValue("1.5"))
    }

    @Test
    fun `max extensions accepts zero but not negatives`() {
        assertEquals(ParseResult.Success(0), OverrideSetting.MaxExtensions.parseValue("0"))
        assertIs<ParseResult.Error>(OverrideSetting.MaxExtensions.parseValue("-1"))
    }

    /** Tab completion suggests "unlimited" but the parser rejects it (known issue, see known-issues.md). */
    @Test
    fun `max extensions rejects the word unlimited`() {
        assertIs<ParseResult.Error>(OverrideSetting.MaxExtensions.parseValue("unlimited"))
    }

    @Test
    fun `extension price accepts zero for auto-calculation`() {
        assertEquals(ParseResult.Success(0.0), OverrideSetting.ExtensionPrice.parseValue("0"))
        assertIs<ParseResult.Error>(OverrideSetting.ExtensionPrice.parseValue("-0.01"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["true", "TRUE", "false", "False"])
    fun `allow extensions accepts booleans in any case`(input: String) {
        assertEquals(ParseResult.Success(input.lowercase() == "true"), OverrideSetting.AllowExtensions.parseValue(input))
    }

    @ParameterizedTest
    @ValueSource(strings = ["yes", "1", ""])
    fun `allow extensions rejects non-booleans`(input: String) {
        assertIs<ParseResult.Error>(OverrideSetting.AllowExtensions.parseValue(input))
    }

    @Test
    fun `extension duration requires a positive whole number`() {
        assertEquals(ParseResult.Success(7), OverrideSetting.ExtensionDuration.parseValue("7"))
        assertIs<ParseResult.Error>(OverrideSetting.ExtensionDuration.parseValue("0"))
    }
}
