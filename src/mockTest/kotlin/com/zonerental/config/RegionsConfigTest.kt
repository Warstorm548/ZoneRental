package com.zonerental.config

import com.zonerental.testsupport.MockPluginTest
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegionsConfigTest : MockPluginTest() {

    private lateinit var groups: GroupsConfig

    @BeforeEach
    fun wireGroups() {
        groups = GroupsConfig(plugin)
        every { plugin.groupsConfig } returns groups
    }

    @Test
    fun `defaults are used when no override exists`() {
        val regions = RegionsConfig(plugin)
        assertEquals(100.0, regions.getRegionPrice("shop1", world, 100.0))
        assertEquals(7, regions.getRegionDuration("shop1", world, 7))
        assertEquals(10, regions.getRegionMaxExtensions("shop1", world, 10))
        assertEquals(true, regions.getRegionAllowExtensions("shop1", world, true))
        assertEquals(7, regions.getRegionExtensionDuration("shop1", world, 7))
    }

    @Test
    fun `region overrides are world specific`() {
        val regions = RegionsConfig(plugin)
        regions.setRegionPrice("shop1", world, 500.0)
        regions.setRegionDuration("shop1", world, 14)
        assertEquals(500.0, regions.getRegionPrice("shop1", world, 100.0))
        assertEquals(14, regions.getRegionDuration("shop1", world, 7))
        assertEquals(100.0, regions.getRegionPrice("shop1", nether, 100.0), "same name in another world is independent")
        assertTrue(regions.hasRegionOverrides("world:shop1"))
    }

    @Test
    fun `group override wins over region override`() {
        val regions = RegionsConfig(plugin)
        regions.setRegionPrice("shop1", world, 500.0)
        groups.createGroup("shops", listOf("world:shop1"))
        regions.setGroupPrice("shops", 1000.0)
        regions.setGroupMaxExtensions("shops", 3)
        regions.setGroupAllowExtensions("shops", false)
        regions.setGroupExtensionDuration("shops", 2)
        assertEquals(1000.0, regions.getRegionPrice("shop1", world, 100.0))
        assertEquals(3, regions.getRegionMaxExtensions("shop1", world, 10))
        assertEquals(false, regions.getRegionAllowExtensions("shop1", world, true))
        assertEquals(2, regions.getRegionExtensionDuration("shop1", world, 7))
    }

    @Test
    fun `group without a given setting falls back to region override`() {
        val regions = RegionsConfig(plugin)
        regions.setRegionDuration("shop1", world, 14)
        groups.createGroup("shops", listOf("world:shop1"))
        regions.setGroupPrice("shops", 1000.0)
        assertEquals(14, regions.getRegionDuration("shop1", world, 7))
    }

    @Test
    fun `non-positive extension price falls through to the default`() {
        val regions = RegionsConfig(plugin)
        regions.setRegionExtensionPrice("shop1", world, 0.0)
        assertEquals(12.5, regions.getRegionExtensionPrice("shop1", world, 12.5))
        regions.setRegionExtensionPrice("shop1", world, 20.0)
        assertEquals(20.0, regions.getRegionExtensionPrice("shop1", world, 12.5))
    }

    @Test
    fun `removing group overrides restores region lookups`() {
        val regions = RegionsConfig(plugin)
        groups.createGroup("shops", listOf("world:shop1"))
        regions.setGroupPrice("shops", 1000.0)
        regions.removeGroupOverrides("shops")
        assertFalse(regions.hasGroupOverrides("shops"))
        assertEquals(100.0, regions.getRegionPrice("shop1", world, 100.0))
    }

    @Test
    fun `legacy keys without a world migrate to the first world`() {
        File(dataFolder, "regions.yml").writeText("regions:\n  shop1:\n    price: 250.0\n")
        val regions = RegionsConfig(plugin)
        assertEquals(setOf("world:shop1"), regions.allRegions)
        assertEquals(250.0, regions.getRegionPrice("shop1", world, 100.0))
    }

    @Test
    fun `overrides persist after save`() {
        RegionsConfig(plugin).apply { setRegionPrice("shop1", world, 42.0); save() }
        assertEquals(42.0, RegionsConfig(plugin).getRegionPrice("shop1", world, 100.0))
    }
}
