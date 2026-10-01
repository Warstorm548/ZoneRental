package com.zonerental.models

import com.zonerental.testsupport.MockPluginTest
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Lives in the mock suite because creating ItemStacks needs a (mock) server. */
class StorageGUISessionTest : MockPluginTest() {

    private fun session(count: Int) =
        StorageGUISession.create(UUID.randomUUID(), List(count) { ItemStack(Material.STONE) })

    @ParameterizedTest
    @CsvSource("0, 1", "1, 1", "45, 1", "46, 2", "90, 2", "91, 3")
    fun `total pages`(items: Int, pages: Int) {
        assertEquals(pages, session(items).totalPages)
    }

    @Test
    fun `page navigation is bounded`() {
        val s = session(91)
        assertFalse(s.previousPage())
        assertEquals(45, s.currentPageItems.size)
        assertTrue(s.nextPage())
        assertTrue(s.nextPage())
        assertEquals(1, s.currentPageItems.size)
        assertFalse(s.nextPage())
        assertEquals(2, s.currentPage)
    }

    @Test
    fun `create copies the list and records the original count`() {
        val items = mutableListOf(ItemStack(Material.STONE))
        val s = StorageGUISession.create(UUID.randomUUID(), items)
        items.clear()
        assertEquals(1, s.itemCount)
        assertEquals(1, s.originalItemCount)
    }
}
