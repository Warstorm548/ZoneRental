package com.zonerental.listeners

import com.zonerental.testsupport.CommandTestSupport
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.milkbowl.vault.economy.Economy
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.entity.PlayerMock
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignInteractListenerTest : CommandTestSupport() {

    private lateinit var economy: Economy
    private lateinit var listener: SignInteractListener
    private lateinit var player: PlayerMock
    private lateinit var sign: Block

    @BeforeEach
    fun setUp() {
        wireCommandDependencies()
        economy = mockk(relaxed = true)
        every { economy.has(any<OfflinePlayer>(), any<Double>()) } returns true
        every { plugin.economy } returns economy

        sign = world.getBlockAt(10, 64, 10).apply { type = Material.OAK_SIGN }
        every { signManager.getRegionFromSign(sign.location) } returns "world:shop1"
        every { signManager.isRentalSign(sign.location) } returns true

        listener = SignInteractListener(plugin)
        player = server.addPlayer().apply { isOp = true }
    }

    private fun rightClick(block: Block = sign) {
        listener.onPlayerInteract(PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.NORTH))
    }

    /** Regression 2.0.1: the composite key from the sign must be split before looking up the rental. */
    @Test
    fun `clicking a sign looks up the region name, not the composite key`() {
        rightClick()
        verify { rentalManager.getRental("shop1", world) }
        verify(exactly = 0) { rentalManager.getRental("world:shop1", any()) }
    }

    @Test
    fun `successful rent withdraws the region price`() {
        every { rentalManager.createRental("shop1", world, player, 7, 100.0) } returns true
        rightClick()
        verify { economy.withdrawPlayer(player, 100.0) }
        verify(exactly = 0) { economy.depositPlayer(any<OfflinePlayer>(), any<Double>()) }
    }

    @Test
    fun `failed rent refunds the payment`() {
        every { rentalManager.createRental(any(), any(), any(), any(), any()) } returns false
        rightClick()
        verify { economy.withdrawPlayer(player, 100.0) }
        verify { economy.depositPlayer(player, 100.0) }
    }

    @Test
    fun `insufficient funds does not create a rental`() {
        every { economy.has(any<OfflinePlayer>(), any<Double>()) } returns false
        rightClick()
        verify(exactly = 0) { economy.withdrawPlayer(any<OfflinePlayer>(), any<Double>()) }
        verify(exactly = 0) { rentalManager.createRental(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `region override price is charged`() {
        regions.setRegionPrice("shop1", world, 250.0)
        every { rentalManager.createRental(any(), any(), any(), any(), any()) } returns true
        rightClick()
        verify { economy.withdrawPlayer(player, 250.0) }
    }

    @Test
    fun `non-rental signs are ignored`() {
        val other = world.getBlockAt(20, 64, 20).apply { type = Material.OAK_SIGN }
        every { signManager.getRegionFromSign(other.location) } returns null
        rightClick(other)
        verify(exactly = 0) { rentalManager.getRental(any(), any()) }
    }

    @Test
    fun `players cannot break rental signs`() {
        val regular = server.addPlayer()
        val event = BlockBreakEvent(sign, regular)
        listener.onBlockBreak(event)
        assertTrue(event.isCancelled)
    }

    @Test
    fun `breaksign permission allows breaking rental signs`() {
        val admin = server.addPlayer()
        admin.addAttachment(MockBukkit.createMockPlugin(), "zonerental.admin.breaksign", true)
        val event = BlockBreakEvent(sign, admin)
        listener.onBlockBreak(event)
        assertFalse(event.isCancelled)
    }

    @Test
    fun `support blocks are protected too`() {
        val support = world.getBlockAt(10, 63, 10).apply { type = Material.STONE }
        every { signManager.getSupportBlockRegion(support.location) } returns "world:shop1"
        val event = BlockBreakEvent(support, server.addPlayer())
        listener.onBlockBreak(event)
        assertTrue(event.isCancelled)
    }
}
