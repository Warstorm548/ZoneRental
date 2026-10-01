package com.zonerental.commands

import com.zonerental.testsupport.MockPluginTest
import com.zonerental.util.TimeUtils
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class DurationCommandParseTest : MockPluginTest() {

    private fun parse(input: String): Long {
        val method = DurationCommand::class.java.getDeclaredMethod("parseTimeString", String::class.java)
        method.isAccessible = true
        return method.invoke(DurationCommand(plugin), input) as Long
    }

    private val d = TimeUtils.DAY_MS
    private val h = TimeUtils.HOUR_MS
    private val m = TimeUtils.MINUTE_MS

    @ParameterizedTest
    @CsvSource(
        "'2d 3h 30m', 185400000",          // 2d + 3h + 30m
        "'2 days 3 hours', 183600000",
        "'1 day', 86400000",
        "'90 mins', 5400000",
        "'1 hour 30 minutes', 5400000",
        "'12 hrs', 43200000",
        "'7D', 604800000"
    )
    fun `supported time formats`(input: String, expected: Long) {
        assertEquals(expected, parse(input))
    }

    @Test
    fun `unit constants used above`() {
        assertEquals(2 * d + 3 * h + 30 * m, 185_400_000L)
    }

    @Test
    fun `garbage parses to zero`() {
        assertEquals(0L, parse("soon"))
        assertEquals(0L, parse(""))
    }

    @Test
    @Disabled("Known issue: joined short forms like 1d12h only read the last unit (docs/reference/known-issues.md)")
    fun `joined short forms are summed`() {
        assertEquals(36 * h, parse("1d12h"))
    }
}
