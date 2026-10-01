package com.zonerental.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimeUtilsTest {

    @Test
    fun `millisecond constants are consistent`() {
        assertEquals(1_000L, TimeUtils.SECOND_MS)
        assertEquals(60_000L, TimeUtils.MINUTE_MS)
        assertEquals(3_600_000L, TimeUtils.HOUR_MS)
        assertEquals(86_400_000L, TimeUtils.DAY_MS)
    }

    @Test
    fun `day and hour conversions round down`() {
        assertEquals(7 * TimeUtils.DAY_MS, TimeUtils.daysToMillis(7))
        assertEquals(7 * TimeUtils.DAY_MS, TimeUtils.daysToMillis(7L))
        assertEquals(1, TimeUtils.millisToDays(TimeUtils.DAY_MS + TimeUtils.HOUR_MS * 23))
        assertEquals(0, TimeUtils.millisToDays(TimeUtils.DAY_MS - 1))
        assertEquals(25, TimeUtils.millisToHours(TimeUtils.DAY_MS + TimeUtils.HOUR_MS + 59 * TimeUtils.MINUTE_MS))
        assertEquals(90, TimeUtils.millisToMinutes(90 * TimeUtils.MINUTE_MS))
    }

    @Test
    fun `formatDuration handles zero and negatives`() {
        assertEquals("0 minutes", TimeUtils.formatDuration(0))
        assertEquals("0 minutes", TimeUtils.formatDuration(-5))
        assertEquals("0 minutes", TimeUtils.formatDuration(TimeUtils.SECOND_MS * 30))
    }

    @Test
    fun `formatDuration uses singular and plural units`() {
        assertEquals("1 minute", TimeUtils.formatDuration(TimeUtils.MINUTE_MS))
        assertEquals("1 hour 30 minutes", TimeUtils.formatDuration(TimeUtils.HOUR_MS + 30 * TimeUtils.MINUTE_MS))
        assertEquals("1 day", TimeUtils.formatDuration(TimeUtils.DAY_MS))
        assertEquals("2 days 3 hours", TimeUtils.formatDuration(2 * TimeUtils.DAY_MS + 3 * TimeUtils.HOUR_MS))
    }

    @Test
    fun `formatDuration hides minutes once a day or more remains`() {
        assertEquals("1 day 1 hour", TimeUtils.formatDuration(TimeUtils.DAY_MS + TimeUtils.HOUR_MS + 45 * TimeUtils.MINUTE_MS))
    }

    @Test
    fun `formatDurationShort shows most significant unit`() {
        assertEquals("0m", TimeUtils.formatDurationShort(0))
        assertEquals("2d", TimeUtils.formatDurationShort(2 * TimeUtils.DAY_MS + TimeUtils.HOUR_MS))
        assertEquals("5h", TimeUtils.formatDurationShort(5 * TimeUtils.HOUR_MS))
        assertEquals("15m", TimeUtils.formatDurationShort(15 * TimeUtils.MINUTE_MS))
    }

    @Test
    fun `parseTimeString parses single unit values`() {
        assertEquals(7 * TimeUtils.DAY_MS, TimeUtils.parseTimeString("7d"))
        assertEquals(24 * TimeUtils.HOUR_MS, TimeUtils.parseTimeString(" 24H "))
        assertEquals(30 * TimeUtils.MINUTE_MS, TimeUtils.parseTimeString("30m"))
        assertEquals(10 * TimeUtils.SECOND_MS, TimeUtils.parseTimeString("10s"))
        assertNull(TimeUtils.parseTimeString(""))
        assertNull(TimeUtils.parseTimeString("7x"))
        assertNull(TimeUtils.parseTimeString("d"))
    }

    @Test
    fun `int extensions convert to milliseconds`() {
        assertEquals(3 * TimeUtils.DAY_MS, 3.days)
        assertEquals(4 * TimeUtils.HOUR_MS, 4.hours)
        assertEquals(5 * TimeUtils.MINUTE_MS, 5.minutes)
        assertEquals(TimeUtils.DAY_MS + 2 * TimeUtils.HOUR_MS, 1.days + 2.hours)
    }

    @Test
    fun `timeRemaining never goes negative`() {
        assertEquals(0L, TimeUtils.timeRemaining(System.currentTimeMillis() - 10_000))
    }
}
