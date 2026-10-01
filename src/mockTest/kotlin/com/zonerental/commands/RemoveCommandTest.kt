package com.zonerental.commands

import com.zonerental.testsupport.CommandTestSupport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RemoveCommandTest : CommandTestSupport() {

    @BeforeEach
    fun setUp() = wireCommandDependencies()

    /** Regression 3.1.2: /zrremove left the region in its group. */
    @Test
    fun `remove takes the region out of its group but keeps group overrides`() {
        groups.createGroup("shops", listOf("world:shop1", "world:shop2"))
        regions.setGroupPrice("shops", 1000.0)

        server.consoleSender.run(RemoveCommand(plugin), "world:shop1")

        assertNull(groups.getRegionGroup("world:shop1"))
        assertEquals(listOf("world:shop2"), groups.getGroupRegions("shops"))
        assertTrue(regions.hasGroupOverrides("shops"), "other members still use the group overrides")
    }

    @Test
    fun `remove deletes individual overrides of an ungrouped region`() {
        regions.setRegionPrice("shop1", world, 500.0)
        server.consoleSender.run(RemoveCommand(plugin), "world:shop1")
        assertFalse(regions.hasRegion("shop1", world))
    }

    @Test
    fun `remove only touches the given world`() {
        regions.setRegionPrice("shop1", nether, 500.0)
        server.consoleSender.run(RemoveCommand(plugin), "world:shop1")
        assertTrue(regions.hasRegion("shop1", nether))
    }

    @Test
    fun `remove refuses unknown regions`() {
        io.mockk.every { worldGuard.regionExists("ghost", world) } returns false
        regions.setRegionPrice("ghost", world, 1.0)
        server.consoleSender.run(RemoveCommand(plugin), "world:ghost")
        assertTrue(regions.hasRegion("ghost", world), "nothing removed for a missing WorldGuard region")
    }
}
