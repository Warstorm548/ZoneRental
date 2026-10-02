package com.zonerental.config

import com.zonerental.testsupport.MockPluginTest
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignsConfigTest : MockPluginTest() {

    private fun loc(x: Int, y: Int, z: Int, w: org.bukkit.World = world) = Location(w, x.toDouble(), y.toDouble(), z.toDouble())

    private val signsFile get() = File(dataFolder, "signs.yml")

    @Test
    fun `signs are keyed by the region world, not the sign world`() {
        val signs = SignsConfig(plugin)
        signs.addSign("shop1", world, loc(10, 64, 10))
        signs.addSign("shop1", nether, loc(10, 64, 10, nether))
        val lobbySign = signs.addSign("shop1", world, loc(50, 70, 50, nether))

        assertEquals(setOf("world:shop1", "world_nether:shop1"), signs.getRegisteredRegions())
        assertEquals("world:shop1", signs.getRegionByLocation(loc(10, 64, 10)))
        assertEquals("world_nether:shop1", signs.getRegionByLocation(loc(10, 64, 10, nether)))
        assertEquals("world:shop1", signs.getRegionByLocation(loc(50, 70, 50, nether)))
        assertEquals("world_nether", lobbySign.signWorld)
        assertNull(signs.getRegionByLocation(loc(11, 64, 10)))
    }

    /** Regression 3.3.0: a second sign used to overwrite the first, so only one sign per region was tracked. */
    @Test
    fun `a region keeps every sign with its own ID`() {
        val signs = SignsConfig(plugin)
        val ids = (1..3).map { signs.addSign("shop1", world, loc(it, 64, 0)).id }
        assertEquals(listOf(1, 2, 3), ids)
        assertEquals(3, signs.getSignCount("shop1", world))
        assertEquals(listOf(1, 2, 3), signs.getSigns("shop1", world).map { it.id })
        (1..3).forEach { assertEquals("world:shop1", signs.getRegionByLocation(loc(it, 64, 0))) }
    }

    @Test
    fun `sign IDs are never reused, even after a reload`() {
        val signs = SignsConfig(plugin)
        repeat(3) { signs.addSign("shop1", world, loc(it, 64, 0)) }
        signs.removeSign("shop1", world, 3)
        signs.save()

        val reloaded = SignsConfig(plugin)
        assertEquals(4, reloaded.addSign("shop1", world, loc(9, 64, 0)).id)
    }

    @Test
    fun `signs and registrations survive save and reload`() {
        val signs = SignsConfig(plugin)
        val sign = signs.addSign("shop1", world, loc(10, 64, 10, nether))
        signs.setSupportBlock(sign, loc(10, 64, 11, nether), "STONE", "minecraft:stone")
        signs.registerRegion("empty", world)
        signs.save()

        val reloaded = SignsConfig(plugin)
        assertEquals(setOf("world:shop1", "world:empty"), reloaded.getRegisteredRegions())
        assertTrue(reloaded.getSigns("empty", world).isEmpty())
        val loaded = reloaded.getSign("shop1", world, 1)!!
        assertEquals("world_nether", loaded.signWorld)
        assertEquals("STONE", loaded.support?.originalType)
    }

    @Test
    fun `removing the last sign keeps the region registered`() {
        val signs = SignsConfig(plugin)
        signs.addSign("shop1", world, loc(1, 64, 1))
        signs.removeSign("shop1", world, 1)
        assertTrue(signs.isRegistered("shop1", world))
        assertNull(signs.getSignAt(loc(1, 64, 1)))
    }

    @Test
    fun `unregistering removes every sign from the indexes`() {
        val signs = SignsConfig(plugin)
        val a = signs.addSign("shop1", world, loc(1, 64, 1))
        signs.setSupportBlock(a, loc(1, 63, 1), "STONE", "")
        signs.addSign("shop1", world, loc(2, 64, 1))

        assertEquals(2, signs.unregisterRegion("shop1", world).size)
        assertFalse(signs.isRegistered("shop1", world))
        assertFalse(signs.isProtectedBlock(loc(1, 64, 1)))
        assertFalse(signs.isProtectedBlock(loc(1, 63, 1)))
        assertTrue(signs.getSignsInChunk("world", 0, 0).isEmpty())
    }

    /** 2.5.0: support blocks are found through an O(1) index that must stay in sync. */
    @Test
    fun `support block index follows add, remove and reload`() {
        val signs = SignsConfig(plugin)
        val sign = signs.addSign("shop1", world, loc(10, 64, 10))
        signs.setSupportBlock(sign, loc(10, 64, 11), "STONE", "minecraft:stone")
        assertEquals("world:shop1", signs.getSupportBlockByLocation(loc(10, 64, 11)))

        signs.save()
        val reloaded = SignsConfig(plugin)
        assertEquals("world:shop1", reloaded.getSupportBlockByLocation(loc(10, 64, 11)))

        reloaded.removeSign("shop1", world, 1)
        assertNull(reloaded.getSupportBlockByLocation(loc(10, 64, 11)))
    }

    @Test
    fun `a support block shared by two signs stays indexed until both are gone`() {
        val signs = SignsConfig(plugin)
        val a = signs.addSign("shop1", world, loc(10, 64, 9))
        val b = signs.addSign("shop2", world, loc(10, 64, 11))
        signs.setSupportBlock(a, loc(10, 64, 10), "STONE", "")
        signs.setSupportBlock(b, loc(10, 64, 10), "STONE", "")
        assertEquals(2, signs.getSignsOnSupportBlock(loc(10, 64, 10)).size)

        signs.removeSign("shop1", world, 1)
        assertEquals(listOf("world:shop2"), signs.getSignsOnSupportBlock(loc(10, 64, 10)).map { it.regionKey })
        signs.removeSign("shop2", world, 1)
        assertFalse(signs.isProtectedBlock(loc(10, 64, 10)))
    }

    @Test
    fun `signs are indexed by chunk`() {
        val signs = SignsConfig(plugin)
        signs.addSign("shop1", world, loc(17, 64, -1))
        assertEquals(1, signs.getSignsInChunk("world", 1, -1).size)
        assertTrue(signs.getSignsInChunk("world", 0, 0).isEmpty())
    }

    @Test
    fun `pre-3_3_0 single sign migrates to sign 1 and is backed up`() {
        signsFile.writeText(
            "signs:\n  world:shop1:\n    world: world\n    x: 1\n    y: 2\n    z: 3\n" +
            "    support-block:\n      x: 1\n      y: 1\n      z: 3\n      original-type: DIRT\n      original-data: minecraft:dirt\n"
        )
        val signs = SignsConfig(plugin)

        val sign = signs.getSign("shop1", world, 1)!!
        assertEquals(Triple(1, 2, 3), Triple(sign.x, sign.y, sign.z))
        assertEquals("DIRT", sign.support?.originalType)
        assertEquals(2, signs.addSign("shop1", world, loc(5, 5, 5)).id)
        assertTrue(File(dataFolder, "signs.yml.pre-3.3.0.bak").readText().contains("original-type: DIRT"))

        val saved = YamlConfiguration.loadConfiguration(signsFile)
        assertFalse(saved.contains("signs.world:shop1.x"), "old layout is gone from the file")
        assertEquals(1, saved.getInt("signs.world:shop1.1.x"))
    }

    @Test
    fun `legacy signs without a world prefix migrate using their world field`() {
        signsFile.writeText("signs:\n  shop1:\n    world: world_nether\n    x: 1\n    y: 2\n    z: 3\n")
        val signs = SignsConfig(plugin)
        assertEquals(setOf("world_nether:shop1"), signs.getRegisteredRegions())
        assertEquals("world_nether:shop1", signs.getRegionByLocation(loc(1, 2, 3, nether)))
    }

    @Test
    fun `a legacy sign whose world-prefixed key already exists is kept, not merged`() {
        signsFile.writeText(
            "signs:\n" +
            "  shop1:\n    world: world\n    x: 9\n    y: 9\n    z: 9\n" +
            "  world:shop1:\n    next-id: 2\n    '1':\n      world: world\n      x: 1\n      y: 2\n      z: 3\n"
        )
        val signs = SignsConfig(plugin)

        val sign = signs.getSign("shop1", world, 1)!!
        assertEquals(Triple(1, 2, 3), Triple(sign.x, sign.y, sign.z), "existing entry not overwritten")
        assertEquals(1, signs.getSignCount("shop1", world))

        signs.save()
        val saved = YamlConfiguration.loadConfiguration(signsFile)
        assertEquals(9, saved.getInt("signs.shop1.x"), "legacy entry survives saving")
        assertEquals(1, saved.getInt("signs.world:shop1.1.x"))
    }

    @Test
    fun `current files are not migrated or backed up`() {
        val signs = SignsConfig(plugin)
        signs.addSign("shop1", world, loc(1, 2, 3))
        signs.save()
        SignsConfig(plugin)
        assertFalse(File(dataFolder, "signs.yml.pre-3.3.0.bak").exists())
    }
}
