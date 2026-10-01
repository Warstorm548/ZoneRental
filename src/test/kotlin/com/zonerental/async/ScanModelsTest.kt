package com.zonerental.async

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScanModelsTest {

    private fun region(minX: Int, maxX: Int, minZ: Int, maxZ: Int, minY: Int = 60, maxY: Int = 70) =
        ScanRegion("shop1", "world", minX, maxX, minY, maxY, minZ, maxZ)

    @Test
    fun `single chunk region`() {
        val r = region(0, 15, 0, 15)
        assertEquals(1, r.chunkCount)
        assertEquals(16, r.sizeX)
        assertEquals(11, r.sizeY)
        assertEquals(16L * 11 * 16, r.totalBlocks)
        assertEquals("world:shop1", r.compositeKey)
    }

    @Test
    fun `region crossing a chunk boundary spans two chunks`() {
        assertEquals(2, region(15, 16, 0, 0).chunkCount)
    }

    @Test
    fun `negative coordinates use floor chunk division`() {
        // -1 is in chunk -1, 0 is in chunk 0
        assertEquals(2, region(-1, 0, 0, 0).chunkCount)
        // -16..-1 is exactly chunk -1
        assertEquals(1, region(-16, -1, -16, -1).chunkCount)
        assertEquals(4, region(-17, -1, -17, -1).chunkCount)
    }

    @ParameterizedTest
    @CsvSource(
        "1, Tiny", "49, Tiny", "50, Small", "199, Small", "200, Medium", "499, Medium",
        "500, Large", "999, Large", "1000, VeryLarge", "1999, VeryLarge", "2000, Extreme", "50000, Extreme"
    )
    fun `strategy thresholds`(chunks: Int, expected: String) {
        assertEquals(expected, ScanStrategy.fromChunkCount(chunks)::class.simpleName)
    }

    @Test
    fun `larger strategies use smaller batches and longer delays`() {
        val ordered = listOf(
            ScanStrategy.Tiny, ScanStrategy.Small, ScanStrategy.Medium,
            ScanStrategy.Large, ScanStrategy.VeryLarge, ScanStrategy.Extreme
        )
        ordered.zipWithNext().forEach { (a, b) ->
            assertTrue(a.batchSize >= b.batchSize, "$a vs $b batch size")
            assertTrue(a.delayBetweenBatchesMs <= b.delayBetweenBatchesMs, "$a vs $b delay")
        }
    }

    @Test
    fun `tps levels throttle progressively`() {
        assertTrue(TpsLevel.HEALTHY.delayMultiplier < TpsLevel.WARNING.delayMultiplier)
        assertTrue(TpsLevel.WARNING.delayMultiplier < TpsLevel.CRITICAL.delayMultiplier)
        assertTrue(TpsLevel.HEALTHY.concurrencyMultiplier > TpsLevel.CRITICAL.concurrencyMultiplier)
    }

    @Test
    fun `scan result accessors`() {
        assertEquals(listOf(1), ScanResult.Success(listOf(1)).getOrNull())
        assertEquals(listOf(2), ScanResult.PartialSuccess(listOf(2), emptyList()).getOrNull())
        assertNull(ScanResult.Failure("boom").getOrNull())
        assertTrue(ScanResult.Success(1).isSuccess())
        assertFalse(ScanResult.Failure("boom").isSuccess())
    }
}
