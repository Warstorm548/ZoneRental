package com.zonerental.managers

import com.zonerental.async.AsyncScanService
import com.zonerental.testsupport.MockPluginTest
import org.bukkit.Material
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** The container list is duplicated in StorageManager and AsyncScanService; they must match. */
class ContainerTypesConsistencyTest : MockPluginTest() {

    @Test
    fun `storage manager and async scanner agree on container types`() {
        val field = StorageManager::class.java.getDeclaredField("CONTAINER_TYPES").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val storageTypes = field.get(null) as Set<Material>
        assertEquals(AsyncScanService.CONTAINER_TYPES, storageTypes)
    }
}
