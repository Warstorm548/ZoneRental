package com.zonerental.config

import com.zonerental.testsupport.MockPluginTest
import org.bukkit.Location
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignsConfigTest : MockPluginTest() {

    private fun loc(x: Int, y: Int, z: Int, w: org.bukkit.World = world) = Location(w, x.toDouble(), y.toDouble(), z.toDouble())

    @Test
    fun `signs are keyed by world and region`() {
        val signs = SignsConfig(plugin)
        signs.addSign("shop1", loc(10, 64, 10))
        signs.addSign("shop1", loc(10, 64, 10, nether))
        assertEquals(setOf("world:shop1", "world_nether:shop1"), signs.getAllSigns().keys)
        assertEquals("world:shop1", signs.getRegionByLocation(loc(10, 64, 10)))
        assertEquals("world_nether:shop1", signs.getRegionByLocation(loc(10, 64, 10, nether)))
        assertNull(signs.getRegionByLocation(loc(11, 64, 10)))
    }

    /** 2.5.0: support blocks are found through an O(1) index that must stay in sync. */
    @Test
    fun `support block index follows add, remove and reload`() {
        val signs = SignsConfig(plugin)
        signs.addSign("shop1", loc(10, 64, 10))
        signs.addSupportBlock("shop1", world, loc(10, 64, 11), "STONE", "minecraft:stone")
        assertEquals("world:shop1", signs.getSupportBlockByLocation(loc(10, 64, 11)))

        signs.save()
        val reloaded = SignsConfig(plugin)
        assertEquals("world:shop1", reloaded.getSupportBlockByLocation(loc(10, 64, 11)))
        assertEquals(mapOf("type" to "STONE", "data" to "minecraft:stone"), reloaded.getSupportBlockData("shop1", world))

        reloaded.removeSign("shop1", world)
        assertNull(reloaded.getSupportBlockByLocation(loc(10, 64, 11)))
        assertFalse(reloaded.hasSign("shop1", world))
    }

    @Test
    fun `removeSupportBlock keeps the sign`() {
        val signs = SignsConfig(plugin)
        signs.addSign("shop1", loc(10, 64, 10))
        signs.addSupportBlock("shop1", world, loc(10, 63, 10), "DIRT", "minecraft:dirt")
        signs.removeSupportBlock("shop1", world)
        assertTrue(signs.hasSign("shop1", world))
        assertFalse(signs.hasSupportBlock("shop1", world))
        assertNull(signs.getSupportBlockByLocation(loc(10, 63, 10)))
    }

    @Test
    fun `legacy signs without a world prefix migrate using their world field`() {
        File(dataFolder, "signs.yml").writeText("signs:\n  shop1:\n    world: world_nether\n    x: 1\n    y: 2\n    z: 3\n")
        val signs = SignsConfig(plugin)
        assertEquals(setOf("world_nether:shop1"), signs.getAllSigns().keys)
    }
}
