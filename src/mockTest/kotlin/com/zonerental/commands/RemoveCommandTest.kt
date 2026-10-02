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

        server.consoleSender.run(RemoveCommand(plugin), "all", "world:shop1")

        assertNull(groups.getRegionGroup("world:shop1"))
        assertEquals(listOf("world:shop2"), groups.getGroupRegions("shops"))
        assertTrue(regions.hasGroupOverrides("shops"), "other members still use the group overrides")
    }

    @Test
    fun `remove deletes individual overrides of an ungrouped region`() {
        regions.setRegionPrice("shop1", world, 500.0)
        server.consoleSender.run(RemoveCommand(plugin), "all", "world:shop1")
        assertFalse(regions.hasRegion("shop1", world))
    }

    @Test
    fun `remove only touches the given world`() {
        regions.setRegionPrice("shop1", nether, 500.0)
        server.consoleSender.run(RemoveCommand(plugin), "all", "world:shop1")
        assertTrue(regions.hasRegion("shop1", nether))
    }

    @Test
    fun `remove refuses unknown regions`() {
        io.mockk.every { worldGuard.regionExists("ghost", world) } returns false
        regions.setRegionPrice("ghost", world, 1.0)
        server.consoleSender.run(RemoveCommand(plugin), "all", "world:ghost")
        assertTrue(regions.hasRegion("ghost", world), "nothing removed for a missing WorldGuard region")
    }

    /** Regression 3.3.0: the full teardown needs "all", so a bare region argument can't wipe a region by accident. */
    @Test
    fun `a region without all or a sign ID only shows usage`() {
        regions.setRegionPrice("shop1", world, 500.0)
        server.consoleSender.run(RemoveCommand(plugin), "world:shop1")
        assertTrue(regions.hasRegion("shop1", world))
        io.mockk.verify(exactly = 0) { signManager.removeRegionSetup(any(), any()) }
    }

    @Test
    fun `all can tear down a registered space whose WorldGuard region is gone`() {
        io.mockk.every { worldGuard.regionExists("ghost", world) } returns false
        signs.registerRegion("ghost", world)
        server.consoleSender.run(RemoveCommand(plugin), "all", "world:ghost")
        io.mockk.verify { signManager.removeRegionSetup("ghost", world) }
    }

    @Test
    fun `a sign ID removes only that sign`() {
        regions.setRegionPrice("shop1", world, 500.0)
        val sign = signs.addSign("shop1", world, world.getBlockAt(1, 64, 1).location)
        server.consoleSender.run(RemoveCommand(plugin), "world:shop1", "1")
        io.mockk.verify { signManager.removeSign(sign) }
        io.mockk.verify(exactly = 0) { signManager.removeRegionSetup(any(), any()) }
        assertTrue(regions.hasRegion("shop1", world), "overrides stay")
    }

    @Test
    fun `an unknown sign ID is reported`() {
        signs.registerRegion("shop1", world)
        server.consoleSender.run(RemoveCommand(plugin), "world:shop1", "7")
        io.mockk.verify(exactly = 0) { signManager.removeSign(any()) }
        assertTrue(messages(server.consoleSender).any { it.contains("#7") })
    }

    @Test
    fun `looking at a rental sign removes it`() {
        val block = world.getBlockAt(1, 64, 1).apply { type = org.bukkit.Material.OAK_SIGN }
        val sign = signs.addSign("shop1", world, block.location)
        val admin = server.addPlayer().apply { isOp = true }
        val looking = io.mockk.spyk(admin)
        io.mockk.every { looking.getTargetBlock(null, 5) } returns block
        looking.run(RemoveCommand(plugin))
        io.mockk.verify { signManager.removeSign(sign) }
    }

    @Test
    fun `tab completion offers all, rental spaces and sign IDs`() {
        signs.addSign("shop1", world, world.getBlockAt(1, 64, 1).location)
        signs.addSign("shop1", world, world.getBlockAt(2, 64, 1).location)
        val remove = RemoveCommand(plugin)
        assertEquals(listOf("all", "world:shop1"), remove.onTabComplete(server.consoleSender, command, "zrremove", arrayOf("")))
        assertEquals(listOf("1", "2"), remove.onTabComplete(server.consoleSender, command, "zrremove", arrayOf("world:shop1", "")))
        assertEquals(listOf("world:shop1"), remove.onTabComplete(server.consoleSender, command, "zrremove", arrayOf("all", "")))
    }
}
