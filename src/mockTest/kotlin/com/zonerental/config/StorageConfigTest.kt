package com.zonerental.config

import com.zonerental.testsupport.MockPluginTest
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StorageConfigTest : MockPluginTest() {

    private val player = UUID.fromString("00000000-0000-0000-0000-0000000000aa")

    private fun stacks(vararg types: Material) = types.map { ItemStack(it, 3) }

    @Test
    fun `stored items survive save and reload`() {
        val storage = StorageConfig(plugin)
        storage.storeItems(player, "shop1", stacks(Material.DIAMOND), stacks(Material.OAK_PLANKS))
        assertTrue(storage.isDirty)
        storage.save()
        assertFalse(storage.isDirty)

        val reloaded = StorageConfig(plugin)
        assertTrue(reloaded.hasStoredItems(player))
        assertEquals(listOf("shop1"), reloaded.getStoredRegions(player))
        val all = reloaded.getAllStoredItems(player)
        assertEquals(setOf(Material.DIAMOND, Material.OAK_PLANKS), all.map { it.type }.toSet())
        assertTrue(all.all { it.amount == 3 })
    }

    @Test
    fun `retrieveItems returns container items and removes the entry`() {
        val storage = StorageConfig(plugin)
        storage.storeItems(player, "shop1", stacks(Material.DIAMOND))
        assertEquals(listOf(Material.DIAMOND), storage.retrieveItems(player, "shop1").map { it.type })
        assertFalse(storage.hasStoredItems(player))
    }

    @Test
    fun `partial retrieval replaces all entries with the remaining items`() {
        val storage = StorageConfig(plugin)
        storage.storeItems(player, "shop1", stacks(Material.DIAMOND))
        storage.storeItems(player, "shop2", stacks(Material.EMERALD))
        storage.updatePartialStorage(player, stacks(Material.GOLD_INGOT))
        assertEquals(listOf("partial_retrieval"), storage.getStoredRegions(player))
        assertEquals(listOf(Material.GOLD_INGOT), storage.getAllStoredItems(player).map { it.type })

        storage.updatePartialStorage(player, emptyList())
        assertFalse(storage.hasStoredItems(player))
    }

    /** 2.5.1 save pattern: the dirty flag must only be cleared when the save actually succeeds. */
    @Test
    fun `failed save keeps the dirty flag`() {
        val storage = StorageConfig(plugin)
        storage.storeItems(player, "shop1", stacks(Material.DIAMOND))
        val file = File(dataFolder, "storage.yml")
        assertTrue(file.setWritable(false))
        try {
            storage.save()
            assertTrue(storage.isDirty, "isDirty must stay true when the file can't be written")
        } finally {
            file.setWritable(true)
        }
    }

    @Test
    @Disabled("Known issue D3: a second store for the same player and region overwrites the first (docs/reference/known-issues.md)")
    fun `second expiry of the same region keeps both batches`() {
        val storage = StorageConfig(plugin)
        storage.storeItems(player, "shop1", stacks(Material.DIAMOND))
        storage.storeItems(player, "shop1", stacks(Material.EMERALD))
        assertEquals(setOf(Material.DIAMOND, Material.EMERALD), storage.getAllStoredItems(player).map { it.type }.toSet())
    }
}
