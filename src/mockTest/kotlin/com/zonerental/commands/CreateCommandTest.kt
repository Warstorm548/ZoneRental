package com.zonerental.commands

import com.sk89q.worldguard.protection.regions.ProtectedRegion
import com.zonerental.testsupport.CommandTestSupport
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreateCommandTest : CommandTestSupport() {

    @BeforeEach
    fun setUp() {
        wireCommandDependencies()
        val shop1 = mockk<ProtectedRegion> { every { id } returns "shop1" }
        every { worldGuard.getRegion(any(), world) } returns null
        every { worldGuard.getRegion(match { it.equals("shop1", ignoreCase = true) }, world) } returns shop1
    }

    @Test
    fun `create registers a rental space without a sign`() {
        server.consoleSender.run(CreateCommand(plugin), "world:shop1")
        assertTrue(signs.isRegistered("shop1", world))
        assertTrue(signs.getSigns("shop1", world).isEmpty())
    }

    @Test
    fun `create stores the WorldGuard ID so case differences can't register twice`() {
        server.consoleSender.run(CreateCommand(plugin), "world:Shop1")
        assertEquals(setOf("world:shop1"), signs.getRegisteredRegions())
        server.consoleSender.run(CreateCommand(plugin), "world:SHOP1")
        assertEquals(setOf("world:shop1"), signs.getRegisteredRegions())
    }

    @Test
    fun `create refuses regions WorldGuard doesn't know`() {
        server.consoleSender.run(CreateCommand(plugin), "world:ghost")
        assertFalse(signs.isRegistered("ghost", world))
    }

    @Test
    fun `create is saved immediately`() {
        server.consoleSender.run(CreateCommand(plugin), "world:shop1")
        assertTrue(com.zonerental.config.SignsConfig(plugin).isRegistered("shop1", world))
    }
}
