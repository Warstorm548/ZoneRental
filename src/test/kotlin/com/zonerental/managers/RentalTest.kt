package com.zonerental.managers

import com.zonerental.util.TimeUtils
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RentalTest {

    private val owner = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val member = UUID.fromString("00000000-0000-0000-0000-000000000002")

    private fun newRental(price: Double = 100.0, endDate: Long = System.currentTimeMillis() + TimeUtils.DAY_MS) =
        Rental.create("shop1", "world", owner, "Steve", endDate, price)

    @Test
    fun `create initialises payment tracking from the price`() {
        val rental = newRental(price = 100.0)
        assertEquals("world:shop1", rental.compositeKey)
        assertEquals(100.0, rental.initialPrice)
        assertEquals(100.0, rental.totalPaid)
        assertEquals(0.0, rental.totalRefunded)
        assertEquals(0, rental.extensionCount)
        assertEquals(0.0, rental.extensionCost)
        assertTrue(rental.getRefundHistory().isEmpty())
        assertEquals(0, rental.memberCount)
    }

    @Test
    fun `fromStorage restores every field`() {
        val history = mutableListOf(Rental.RefundRecord(10.0, 123L, "time_removal", "Admin"))
        val rental = Rental.fromStorage(
            "shop1", "world_nether", owner, "Steve", 1000L, 2000L,
            2, 300.0, 100.0, 10.0, history, mutableSetOf(member)
        )
        assertEquals("world_nether:shop1", rental.compositeKey)
        assertEquals(1000L, rental.startDate)
        assertEquals(2000L, rental.endDate)
        assertEquals(2, rental.extensionCount)
        assertEquals(200.0, rental.extensionCost)
        assertEquals(290.0, rental.netRefundableAmount)
        assertEquals(history, rental.getRefundHistory())
        assertTrue(rental.hasMember(member))
    }

    @Test
    fun `extendRental adds time, payment and increments the count`() {
        val rental = newRental(price = 100.0)
        val before = rental.endDate
        rental.extendRental(7, 50.0)
        assertEquals(before + 7 * TimeUtils.DAY_MS, rental.endDate)
        assertEquals(1, rental.extensionCount)
        assertEquals(150.0, rental.totalPaid)
        assertEquals(50.0, rental.extensionCost)
    }

    @Test
    fun `addTimeWithCharge does not count as an extension`() {
        val rental = newRental(price = 100.0)
        rental.addTimeWithCharge(3, 30.0)
        assertEquals(0, rental.extensionCount)
        assertEquals(130.0, rental.totalPaid)
    }

    @Test
    fun `changing time clears sent warnings`() {
        val rental = newRental()
        rental.markWarningSent("warning_24h")
        assertTrue(rental.hasWarningBeenSent("warning_24h"))
        rental.resetTime(7)
        assertFalse(rental.hasWarningBeenSent("warning_24h"))

        rental.markWarningSent("warning_12h")
        rental.endDate = rental.endDate + 1
        assertFalse(rental.hasWarningBeenSent("warning_12h"))
    }

    @Test
    fun `recordRefund updates total and history`() {
        val rental = newRental(price = 100.0)
        rental.recordRefund(40.0, "time_removal", "Admin")
        assertEquals(40.0, rental.totalRefunded)
        assertEquals(60.0, rental.netRefundableAmount)
        val record = rental.getRefundHistory().single()
        assertEquals(40.0, record.amount)
        assertEquals("time_removal", record.reason)
        assertEquals("Admin", record.adminName)
    }

    /**
     * Regression (refund system): a duration-reset extension refund followed by an admin reset
     * used to refund the extension costs twice. Paid 350 (100 + 250 extensions), extension refund
     * 250 already given -> only 100 may still be refunded.
     */
    @Test
    fun `net refundable amount prevents double refunds`() {
        val rental = newRental(price = 100.0)
        rental.extendRental(7, 125.0)
        rental.extendRental(7, 125.0)
        assertEquals(350.0, rental.totalPaid)

        rental.recordRefund(rental.extensionCost, "duration_reset", "Admin")
        assertEquals(100.0, rental.netRefundableAmount)
    }

    @Test
    fun `net refundable amount never goes negative`() {
        val rental = newRental(price = 100.0)
        rental.recordRefund(150.0, "manual", "Admin")
        assertEquals(0.0, rental.netRefundableAmount)
    }

    @Test
    fun `expired rental reports zero remaining time`() {
        val rental = newRental(endDate = System.currentTimeMillis() - 1_000)
        assertTrue(rental.isExpired)
        assertEquals(0, rental.daysRemaining)
        assertEquals(0, rental.hoursRemaining)
    }

    @Test
    fun `members can be added, removed and cleared`() {
        val rental = newRental()
        assertTrue(rental.addMember(member))
        assertFalse(rental.addMember(member))
        assertEquals(1, rental.memberCount)
        assertTrue(rental.removeMember(member))
        assertFalse(rental.removeMember(member))
        rental.addMember(member)
        rental.clearMembers()
        assertEquals(0, rental.memberCount)
    }

    @Test
    fun `rentals are equal by world and region`() {
        val a = Rental.create("shop1", "world", owner, "Steve", 1L, 1.0)
        val b = Rental.create("shop1", "world", member, "Alex", 2L, 2.0)
        val c = Rental.create("shop1", "world_nether", owner, "Steve", 1L, 1.0)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertFalse(a == c)
    }
}
