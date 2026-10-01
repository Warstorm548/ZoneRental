package com.zonerental.managers

import com.zonerental.config.GroupsConfig
import com.zonerental.config.RegionsConfig
import com.zonerental.testsupport.MockPluginTest
import com.zonerental.util.TimeUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.milkbowl.vault.economy.Economy
import org.bukkit.OfflinePlayer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.entity.PlayerMock
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RentalManagerTest : MockPluginTest() {

    private lateinit var economy: Economy
    private lateinit var worldGuard: WorldGuardManager
    private lateinit var renter: PlayerMock
    private lateinit var friend: PlayerMock

    @BeforeEach
    fun wireManagers() {
        useRealConfigManager()
        every { plugin.groupsConfig } returns GroupsConfig(plugin)
        every { plugin.regionsConfig } returns RegionsConfig(plugin)
        economy = mockk(relaxed = true)
        every { plugin.economy } returns economy
        worldGuard = mockk(relaxed = true)
        every { plugin.worldGuardManager } returns worldGuard
        every { plugin.worldEditManager } returns mockk(relaxed = true)
        every { plugin.signManager } returns mockk(relaxed = true)
        every { plugin.ezChestShopManager } returns null
        val storage = mockk<StorageManager>(relaxed = true)
        every { storage.getRegionChunkCount(any(), any()) } returns 1
        every { plugin.storageManager } returns storage
        renter = server.addPlayer("Steve")
        friend = server.addPlayer("Alex")
    }

    private fun newManager() = RentalManager(plugin).also { it.loadAllRentals() }

    @Test
    fun `createRental registers the renter in WorldGuard and the index`() {
        val manager = newManager()
        assertTrue(manager.createRental("shop1", world, renter, 7, 100.0))
        assertTrue(manager.isRented("shop1", world))
        assertFalse(manager.isRented("shop1", nether), "rentals are per world")
        assertEquals(listOf("world:shop1"), manager.getPlayerRentals(renter.uniqueId).map { it.compositeKey })
        verify { worldGuard.addPlayerToRegion("shop1", world, renter.uniqueId) }
        assertFalse(manager.createRental("shop1", world, friend, 7, 100.0), "already rented")
    }

    @Test
    fun `createRental enforces max rentals per player`() {
        val manager = newManager()
        repeat(3) { assertTrue(manager.createRental("shop$it", world, renter, 7, 10.0)) }
        assertFalse(manager.createRental("shop3", world, renter, 7, 10.0))
    }

    @Test
    fun `createRental refuses regions over the chunk limit`() {
        every { plugin.storageManager.getRegionChunkCount("huge", world) } returns 5000
        assertFalse(newManager().createRental("huge", world, renter, 7, 10.0))
    }

    @Test
    fun `extendRental enforces owner and max extensions`() {
        val manager = newManager()
        manager.createRental("shop1", world, renter, 7, 100.0)
        assertFalse(manager.extendRental("shop1", world, friend, 7, 100.0), "only the owner can extend")
        repeat(10) { assertTrue(manager.extendRental("shop1", world, renter, 7, 10.0)) }
        assertFalse(manager.extendRental("shop1", world, renter, 7, 10.0), "max-extensions is 10")
        assertEquals(200.0, manager.getRental("shop1", world)!!.totalPaid)
    }

    /** 2.5.0: owner/member indexes must stay in sync through the rental lifecycle. */
    @Test
    fun `player and member indexes follow members and expiry`() {
        val manager = newManager()
        manager.createRental("shop1", world, renter, 7, 100.0)
        assertTrue(manager.addMemberToRental("shop1", world, renter.uniqueId, friend.uniqueId))
        assertFalse(manager.addMemberToRental("shop1", world, renter.uniqueId, friend.uniqueId), "no duplicates")
        assertFalse(manager.addMemberToRental("shop1", world, friend.uniqueId, renter.uniqueId), "only the owner adds members")
        assertEquals(listOf("world:shop1"), manager.getRentalsWhereMember(friend.uniqueId).map { it.compositeKey })

        manager.expireRental("shop1", world)
        assertTrue(manager.getPlayerRentals(renter.uniqueId).isEmpty())
        assertTrue(manager.getRentalsWhereMember(friend.uniqueId).isEmpty())
        verify { worldGuard.removePlayerFromRegion("shop1", world, renter.uniqueId) }
        verify { worldGuard.removePlayerFromRegion("shop1", world, friend.uniqueId) }
    }

    @Test
    fun `member limit is enforced`() {
        bukkitConfig.set("members.max-members", 1)
        val manager = newManager()
        manager.createRental("shop1", world, renter, 7, 100.0)
        assertTrue(manager.addMemberToRental("shop1", world, renter.uniqueId, friend.uniqueId))
        assertFalse(manager.addMemberToRental("shop1", world, renter.uniqueId, server.addPlayer().uniqueId))
    }

    @Test
    fun `rentals survive save and reload`() {
        val manager = newManager()
        manager.createRental("shop1", nether, renter, 7, 100.0)
        manager.extendRental("shop1", nether, renter, 7, 50.0)
        manager.addMemberToRental("shop1", nether, renter.uniqueId, friend.uniqueId)
        val original = manager.getRental("shop1", nether)!!
        manager.issueRefund(original, 20.0, "time_removal", "Admin")
        manager.saveAllRentals()

        val loaded = newManager().getRental("shop1", nether)
        assertNotNull(loaded)
        assertEquals(original.playerUUID, loaded.playerUUID)
        assertEquals(original.playerName, loaded.playerName)
        assertEquals(original.startDate, loaded.startDate)
        assertEquals(original.endDate, loaded.endDate)
        assertEquals(1, loaded.extensionCount)
        assertEquals(150.0, loaded.totalPaid)
        assertEquals(100.0, loaded.initialPrice)
        assertEquals(20.0, loaded.totalRefunded)
        assertEquals(original.getRefundHistory(), loaded.getRefundHistory())
        assertEquals(setOf(friend.uniqueId), loaded.getMembers())
    }

    @Test
    fun `legacy rentals without a world load into the first world`() {
        File(dataFolder, "rentals.yml").writeText(
            """
            rentals:
              shop1:
                player-uuid: ${renter.uniqueId}
                player-name: Steve
                start-date: 1
                end-date: ${System.currentTimeMillis() + TimeUtils.DAY_MS}
                total-paid: 100.0
            """.trimIndent()
        )
        val rental = newManager().getRental("shop1", world)
        assertNotNull(rental)
        assertEquals(100.0, rental.initialPrice, "initial price defaults to total paid")
    }

    @Test
    fun `issueRefund is capped at the net refundable amount`() {
        val manager = newManager()
        manager.createRental("shop1", world, renter, 7, 100.0)
        val rental = manager.getRental("shop1", world)!!
        val result = manager.issueRefund(rental, 500.0, "manual", "Admin")!!
        assertEquals(100.0, result["actualAmount"])
        verify { economy.depositPlayer(any<OfflinePlayer>(), 100.0) }
        assertEquals(false, manager.issueRefund(rental, 1.0, "manual", "Admin")!!["success"])
    }

    /** Regression (refund system): extension refund followed by an admin reset must not refund extensions twice. */
    @Test
    fun `duration reset refund followed by admin reset refunds only the remainder`() {
        val manager = newManager()
        manager.createRental("shop1", world, renter, 7, 100.0)
        manager.extendRental("shop1", world, renter, 7, 125.0)
        manager.extendRental("shop1", world, renter, 7, 125.0)
        val rental = manager.getRental("shop1", world)!!
        manager.issueRefund(rental, rental.extensionCost, "duration_reset", "Admin")

        val details = manager.resetRentalWithRefund("shop1", world)!!
        assertEquals(100.0, details["refundAmount"])
        verify(exactly = 1) { economy.depositPlayer(any<OfflinePlayer>(), 250.0) }
        verify(exactly = 1) { economy.depositPlayer(any<OfflinePlayer>(), 100.0) }
        assertFalse(manager.isRented("shop1", world))
    }

    @Test
    fun `proportional refund uses net paid and total duration`() {
        val manager = newManager()
        manager.createRental("shop1", world, renter, 7, 70.0)
        val rental = manager.getRental("shop1", world)!!
        assertEquals(10.0, manager.calculateProportionalRefund(rental, 1), 0.01)
        assertEquals(70.0, manager.calculateProportionalRefund(rental, 30), 0.01, "capped at everything paid")
        assertEquals(0.0, manager.calculateProportionalRefund(rental, 0))
    }

    @Test
    @Disabled("Known issue D2: setting endDate directly doesn't mark rentals dirty, so saveAllRentals() skips the write (docs/reference/known-issues.md)")
    fun `duration changes persist after save`() {
        val manager = newManager()
        manager.createRental("shop1", world, renter, 7, 100.0)
        manager.saveAllRentals()
        val rental = manager.getRental("shop1", world)!!
        val newEnd = rental.endDate + TimeUtils.DAY_MS
        rental.endDate = newEnd
        manager.saveAllRentals()
        assertEquals(newEnd, newManager().getRental("shop1", world)!!.endDate)
    }

    @Test
    @Disabled("Known issue D2: the multi-world migration save is skipped because nothing is marked dirty (docs/reference/known-issues.md)")
    fun `legacy migration is written back to disk`() {
        val file = File(dataFolder, "rentals.yml")
        file.writeText("rentals:\n  shop1:\n    player-uuid: ${renter.uniqueId}\n    end-date: 1\n    total-paid: 1.0\n")
        newManager()
        assertTrue(file.readText().contains("world: world"), file.readText())
    }
}
