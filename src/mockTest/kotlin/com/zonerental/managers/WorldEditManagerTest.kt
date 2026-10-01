package com.zonerental.managers

import com.zonerental.testsupport.MockPluginTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorldEditManagerTest : MockPluginTest() {

    private lateinit var schematics: File

    @BeforeEach
    fun setUpFolder() {
        useRealConfigManager()
        schematics = File(dataFolder, "schematics").apply { mkdirs() }
    }

    /** Regression 2.9.1: deleteCapture() looked up the wrong file name, so /zrremove left schematics behind. */
    @Test
    fun `deleteCapture removes the world-aware schematic file`() {
        val file = File(schematics, "world:shop1.schem").apply { writeText("placeholder") }
        val manager = WorldEditManager(plugin)
        assertTrue(manager.hasCapture("shop1", world))
        assertFalse(manager.hasCapture("shop1", nether))

        manager.deleteCapture("shop1", world)
        assertFalse(file.exists())
        assertFalse(manager.hasCapture("shop1", world))
    }

    @Test
    fun `deleteCapture leaves other worlds alone`() {
        File(schematics, "world:shop1.schem").writeText("a")
        val netherFile = File(schematics, "world_nether:shop1.schem").apply { writeText("b") }
        WorldEditManager(plugin).deleteCapture("shop1", world)
        assertTrue(netherFile.exists())
    }

    @Test
    fun `empty legacy dat files are removed when auto-delete is enabled`() {
        val legacy = File(schematics, "world:shop1.dat").apply { writeText("") }
        val manager = WorldEditManager(plugin)
        assertFalse(legacy.exists())
        assertFalse(manager.hasCapture("shop1", world))
    }

    @Test
    fun `restore without a capture fails cleanly`() {
        assertFalse(WorldEditManager(plugin).restoreRegion("missing", world))
    }
}
