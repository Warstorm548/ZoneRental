package com.zonerental.commands

import com.zonerental.testsupport.CommandTestSupport
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupCommandTest : CommandTestSupport() {

    private lateinit var groupCommand: GroupCommand

    @BeforeEach
    fun setUp() {
        wireCommandDependencies()
        groupCommand = GroupCommand(plugin)
    }

    private fun editCompletions() =
        groupCommand.onTabComplete(server.consoleSender, command, "zrgroup", arrayOf("edit", ""))

    /** Regression 2.5.1: the tab-completion cache wasn't invalidated after group changes. */
    @Test
    fun `tab completion reflects new and deleted groups immediately`() {
        assertEquals(emptyList(), editCompletions(), "primes the 5 second cache")

        server.consoleSender.run(groupCommand, "create", "shops", "world:shop1")
        assertEquals(listOf("shops"), editCompletions())

        server.consoleSender.run(groupCommand, "delete", "shops", "confirm")
        assertEquals(emptyList(), editCompletions())
    }

    @Test
    fun `create, add and remove regions`() {
        server.consoleSender.run(groupCommand, "create", "shops", "world:shop1,world_nether:shop2")
        assertEquals(listOf("world:shop1", "world_nether:shop2"), groups.getGroupRegions("shops"))

        server.consoleSender.run(groupCommand, "edit", "shops", "remove", "world:shop1")
        assertEquals(listOf("world_nether:shop2"), groups.getGroupRegions("shops"))
    }

    @Test
    fun `joining a group clears individual overrides`() {
        regions.setRegionPrice("shop1", world, 500.0)
        server.consoleSender.run(groupCommand, "create", "shops", "world:shop1")
        assertFalse(regions.hasRegion("shop1", world))
    }

    @Test
    fun `a region cannot join two groups`() {
        server.consoleSender.run(groupCommand, "create", "a_group", "world:shop1")
        server.consoleSender.run(groupCommand, "create", "b_group", "world:shop1")
        assertFalse(groups.groupExists("b_group"))
    }

    @Test
    fun `delete requires confirmation`() {
        server.consoleSender.run(groupCommand, "create", "shops", "world:shop1")
        server.consoleSender.run(groupCommand, "delete", "shops")
        assertTrue(groups.groupExists("shops"))
    }

    /** Regression 2.2.1: signs weren't redrawn when regions left a group or the group was deleted. */
    @Test
    fun `group changes mark affected signs for redraw`() {
        server.consoleSender.run(groupCommand, "create", "shops", "world:shop1,world:shop2")
        verify { signManager.bulkMarkSignsDirty(listOf("world:shop1", "world:shop2")) }

        server.consoleSender.run(groupCommand, "edit", "shops", "remove", "world:shop1")
        verify { signManager.bulkMarkSignsDirty(listOf("world:shop1")) }

        server.consoleSender.run(groupCommand, "delete", "shops", "confirm")
        verify { signManager.bulkMarkSignsDirty(listOf("world:shop2")) }
    }

    @Test
    fun `chat prompt flow for players`() {
        val player = server.addPlayer().apply { isOp = true }
        player.run(groupCommand, "create", "shops")
        assertTrue(groupCommand.hasPendingAction(player.uniqueId))
        groupCommand.handleChatInput(player, "shop1")
        assertFalse(groupCommand.hasPendingAction(player.uniqueId))
        assertEquals(listOf("world:shop1"), groups.getGroupRegions("shops"))
    }
}
