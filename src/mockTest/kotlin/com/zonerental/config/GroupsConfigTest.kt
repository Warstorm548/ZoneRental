package com.zonerental.config

import com.zonerental.testsupport.MockPluginTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupsConfigTest : MockPluginTest() {

    @ParameterizedTest
    @ValueSource(strings = ["ab", "shop_group", "Shops2025", "a234567890123456789012345678_0"])
    fun `valid group names`(name: String) {
        val groups = GroupsConfig(plugin)
        assertTrue(groups.isValidGroupName(name))
        assertNull(groups.getValidationError(name))
    }

    @ParameterizedTest
    @ValueSource(strings = ["a", "has space", "dash-name", "a2345678901234567890123456789_1", "all", "None", "DEFAULT"])
    fun `invalid group names`(name: String) {
        val groups = GroupsConfig(plugin)
        assertFalse(groups.isValidGroupName(name))
        assertNotNull(groups.getValidationError(name))
    }

    @Test
    fun `group lifecycle and membership lookups`() {
        val groups = GroupsConfig(plugin)
        assertTrue(groups.createGroup("shops", listOf("world:shop1")))
        assertFalse(groups.createGroup("shops", emptyList()), "duplicate group names are rejected")

        groups.addRegionsToGroup("shops", listOf("world:shop1", "world_nether:shop2"))
        assertEquals(listOf("world:shop1", "world_nether:shop2"), groups.getGroupRegions("shops"), "no duplicates added")
        assertEquals("shops", groups.getRegionGroup("world_nether:shop2"))
        assertTrue(groups.isRegionInGroup("world:shop1"))
        assertEquals(mapOf("world:shop1" to "shops"), groups.checkGroupMembership(listOf("world:shop1", "world:other")))

        groups.removeRegionsFromGroup("shops", listOf("world:shop1"))
        assertFalse(groups.isRegionInGroup("world:shop1"))
        assertEquals(1, groups.getGroupSize("shops"))

        assertTrue(groups.deleteGroup("shops"))
        assertFalse(groups.groupExists("shops"))
        assertNull(groups.getRegionGroup("world_nether:shop2"))
    }

    @Test
    fun `groups persist after save`() {
        val groups = GroupsConfig(plugin)
        groups.createGroup("shops", listOf("world:shop1"))
        groups.save()
        assertEquals(listOf("world:shop1"), GroupsConfig(plugin).getGroupRegions("shops"))
    }

    @Test
    fun `orphaned groups are reported`() {
        val groups = GroupsConfig(plugin)
        groups.createGroup("empty", emptyList())
        groups.createGroup("full", listOf("world:shop1"))
        assertEquals(listOf("empty"), groups.orphanedGroups)
    }
}
