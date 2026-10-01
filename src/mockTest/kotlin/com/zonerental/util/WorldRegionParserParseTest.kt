package com.zonerental.util

import com.zonerental.testsupport.MockPluginTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WorldRegionParserParseTest : MockPluginTest() {

    @Test
    fun `plain region uses the player's world`() {
        val player = server.addPlayer()
        player.teleport(nether.spawnLocation)
        val parsed = assertNotNull(WorldRegionParser.parse("shop1", player))
        assertEquals("world_nether:shop1", parsed.getCompositeKey())
        assertEquals(nether, parsed.getWorld())
    }

    @Test
    fun `explicit world works for console`() {
        val parsed = assertNotNull(WorldRegionParser.parse("world_nether:shop1", server.consoleSender))
        assertEquals("world_nether", parsed.worldName)
        assertEquals("shop1", parsed.regionName)
    }

    @Test
    fun `unknown world is rejected`() {
        assertNull(WorldRegionParser.parse("nope:shop1", server.addPlayer()))
    }

    @Test
    fun `console must give a world`() {
        assertNull(WorldRegionParser.parse("shop1", server.consoleSender))
    }
}
