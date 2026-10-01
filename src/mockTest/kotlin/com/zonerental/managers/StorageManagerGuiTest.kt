package com.zonerental.managers

import com.zonerental.config.StorageConfig
import com.zonerental.models.StorageGUISession
import com.zonerental.testsupport.MockPluginTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.entity.PlayerMock
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StorageManagerGuiTest : MockPluginTest() {

    private lateinit var storageConfig: StorageConfig
    private lateinit var player: PlayerMock
    private lateinit var manager: StorageManager

    /** Distinct item types, so the GUI's addItem() can't merge them into fewer slots. */
    private val materials by lazy { Material.entries.filter { !it.isLegacy && it.isItem && !it.isAir } }
    private fun items(count: Int) = List(count) { ItemStack(materials[it], 1) }

    @BeforeEach
    fun setUpManager() {
        useRealConfigManager()
        storageConfig = mockk(relaxed = true)
        every { plugin.storageConfig } returns storageConfig
        player = server.addPlayer()
        manager = StorageManager(plugin)
        // MockBukkit ignores listeners registered for the MockK plugin, so register against a real mock plugin
        server.pluginManager.registerEvents(manager, MockBukkit.createMockPlugin())
    }

    private fun openWith(count: Int): Inventory {
        every { storageConfig.getAllStoredItems(player.uniqueId) } returns items(count)
        manager.openRetrievalGUI(player)
        return player.openInventory.topInventory
    }

    private fun click(slot: Int) {
        player.simulateInventoryClick(player.openInventory, ClickType.LEFT, slot)
    }

    @Test
    fun `no stored items does not open a GUI`() {
        every { storageConfig.getAllStoredItems(player.uniqueId) } returns emptyList()
        manager.openRetrievalGUI(player)
        assertEquals(0, manager.activeSessionCount)
    }

    /** Regression 3.0.5: close button and page indicator must always be present, even on a single page. */
    @Test
    fun `single page shows page indicator and close button but no arrows`() {
        val gui = openWith(10)
        assertEquals(Material.PAPER, gui.getItem(49)?.type)
        assertEquals(Material.BARRIER, gui.getItem(50)?.type)
        assertNull(gui.getItem(45))
        assertNull(gui.getItem(53))
        assertEquals(10, (0 until 45).count { gui.getItem(it) != null })
    }

    /** Regression 3.1.1: changing page used to drop the session, making navigation buttons movable. */
    @Test
    fun `navigating pages keeps the session and blocks moving buttons`() {
        val page1 = openWith(100)
        assertEquals(Material.ARROW, page1.getItem(53)?.type)

        click(53)
        assertEquals(1, manager.activeSessionCount, "session must survive the page change")
        val page2 = player.openInventory.topInventory
        assertEquals(Material.ARROW, page2.getItem(45)?.type, "previous arrow on page 2")
        assertEquals(Material.ARROW, page2.getItem(53)?.type, "next arrow on page 2 of 3")

        val event = player.simulateInventoryClick(player.openInventory, ClickType.LEFT, 49)
        assertTrue(event.isCancelled, "navigation slots must not be movable on page 2")
    }

    /** Regression 3.1.1: closing the GUI used to keep only the items of the page that was visible. */
    @Test
    fun `closing after taking items on two pages keeps every remaining item`() {
        val original = items(100)
        val taken = mutableListOf<Material>()
        fun take(inv: Inventory, slot: Int) {
            taken += inv.getItem(slot)!!.type
            inv.setItem(slot, null)
        }

        val page1 = openWith(100)
        take(page1, 0)
        take(page1, 1)

        // Remaining items consolidate forward when changing page, so page 2 starts at original[47]
        click(53)
        take(player.openInventory.topInventory, 0)

        val remaining = slot<List<ItemStack>>()
        every { storageConfig.updatePartialStorage(player.uniqueId, capture(remaining)) } returns Unit
        player.closeInventory()

        assertEquals(3, taken.size)
        val expected = original.map { it.type }.filter { it !in taken }
        assertEquals(expected, remaining.captured.map { it.type }, "every item not taken must be kept, in order")
        assertEquals(0, manager.activeSessionCount)
    }

    @Test
    fun `taking everything clears storage`() {
        val gui = openWith(3)
        (0 until 3).forEach { gui.setItem(it, null) }
        player.closeInventory()
        verify { storageConfig.clearPlayerStorage(player.uniqueId) }
    }

    /** 2.5.0: sessions must not leak when a player disconnects. */
    @Test
    fun `quitting removes the session`() {
        openWith(5)
        assertEquals(1, manager.activeSessionCount)
        manager.onPlayerQuit(PlayerQuitEvent(player, Component.empty(), PlayerQuitEvent.QuitReason.DISCONNECTED))
        assertEquals(0, manager.activeSessionCount)
    }

    @Test
    fun `items per page constant matches the GUI layout`() {
        assertEquals(45, StorageGUISession.ITEMS_PER_PAGE)
        assertNotNull(openWith(46).getItem(44))
    }
}
