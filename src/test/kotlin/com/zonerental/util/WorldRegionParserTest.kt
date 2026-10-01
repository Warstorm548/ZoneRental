package com.zonerental.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorldRegionParserTest {

    /** Regression 2.0.1: sign clicks failed with "region world:shop1 not found" because the composite key wasn't split. */
    @Test
    fun `extractRegionName strips the world from a composite key`() {
        assertEquals("shop1", WorldRegionParser.extractRegionName("world:shop1"))
        assertEquals("shop1", WorldRegionParser.extractRegionName("world_nether:shop1"))
    }

    @Test
    fun `extractRegionName returns plain names unchanged`() {
        assertEquals("shop1", WorldRegionParser.extractRegionName("shop1"))
    }

    @Test
    fun `extractRegionName only splits on the first colon`() {
        assertEquals("shop:1", WorldRegionParser.extractRegionName("world:shop:1"))
    }

    @Test
    fun `extractRegionName and extractWorldName handle null`() {
        assertNull(WorldRegionParser.extractRegionName(null))
        assertNull(WorldRegionParser.extractWorldName(null))
    }

    @Test
    fun `extractWorldName returns the world or null`() {
        assertEquals("world_nether", WorldRegionParser.extractWorldName("world_nether:shop1"))
        assertNull(WorldRegionParser.extractWorldName("shop1"))
    }

    @Test
    fun `parsed region builds composite key`() {
        assertEquals("world:shop1", WorldRegionParser.ParsedRegion("world", "shop1").getCompositeKey())
    }
}
