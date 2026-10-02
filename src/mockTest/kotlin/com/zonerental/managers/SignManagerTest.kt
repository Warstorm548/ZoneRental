package com.zonerental.managers

import com.zonerental.managers.SignManager.CreateResult
import com.zonerental.testsupport.CommandTestSupport
import io.mockk.every
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignManagerTest : CommandTestSupport() {

    private lateinit var manager: SignManager

    @BeforeEach
    fun setUp() {
        wireCommandDependencies()
        manager = SignManager(plugin)
        every { plugin.signManager } returns manager
    }

    private fun signBlock(x: Int, z: Int = 0, w: World = world, type: Material = Material.OAK_SIGN): Block {
        w.getBlockAt(x, 63, z).type = Material.STONE
        return w.getBlockAt(x, 64, z).apply { this.type = type }
    }

    private fun create(block: Block, region: String = "shop1", regionWorld: World = world) =
        assertIs<CreateResult.Created>(manager.createSign(region, regionWorld, block)).sign

    private fun line(block: Block, index: Int, side: Side = Side.FRONT): String =
        PlainTextComponentSerializer.plainText().serialize((block.state as Sign).getSide(side).line(index))

    private fun useConfig(path: String, value: Any) {
        bukkitConfig.set(path, value)
        useRealConfigManager()
    }

    @Test
    fun `every sign of a region is redrawn when the rental changes`() {
        val blocks = (1..3).map { signBlock(it * 2) }
        blocks.forEach { create(it) }
        blocks.forEach { assertEquals("[AVAILABLE]", line(it, 0)) }

        every { rentalManager.getRental("shop1", world) } returns
            Rental.create("shop1", "world", UUID.randomUUID(), "Steve", System.currentTimeMillis() + 86_400_000, 100.0)
        manager.markSignDirty("shop1", world)
        manager.updateAllSigns()

        blocks.forEach {
            assertEquals("[RENTED]", line(it, 0))
            assertEquals("Steve", line(it, 2))
        }
    }

    @Test
    fun `a sign in another world shows the region's own price`() {
        regions.setRegionPrice("shop1", world, 250.0)
        val block = signBlock(5, w = nether)
        val sign = create(block)
        assertEquals("world:shop1", sign.regionKey)
        assertEquals("world_nether", sign.signWorld)
        assertEquals("$250.00", line(block, 2))
    }

    @Test
    fun `standing signs get text on both sides`() {
        val block = signBlock(1)
        create(block)
        assertEquals("[AVAILABLE]", line(block, 0, Side.BACK))
    }

    @Test
    fun `the default limit is three signs per region`() {
        (1..3).forEach { create(signBlock(it * 2)) }
        assertEquals(CreateResult.LimitReached(3), manager.createSign("shop1", world, signBlock(20)))
        assertTrue(manager.createSign("shop2", world, signBlock(22)) is CreateResult.Created, "limit is per region")
    }

    @Test
    fun `-1 means unlimited signs`() {
        useConfig("signs.max-per-region", -1)
        (1..5).forEach { create(signBlock(it * 2)) }
        assertEquals(5, signs.getSignCount("shop1", world))
    }

    @Test
    fun `0 is invalid and falls back to three`() {
        useConfig("signs.max-per-region", 0)
        assertEquals(3, plugin.configManager.maxSignsPerRegion)
        useConfig("signs.max-per-region", -5)
        assertEquals(3, plugin.configManager.maxSignsPerRegion)
    }

    @Test
    fun `a sign can only belong to one region`() {
        val block = signBlock(1)
        create(block)
        val result = manager.createSign("shop2", world, block)
        assertIs<CreateResult.AlreadyRegistered>(result)
        assertEquals("world:shop1", result.existing.regionKey)
    }

    @Test
    fun `createSign refuses blocks that are not signs`() {
        assertEquals(CreateResult.NotASign, manager.createSign("shop1", world, world.getBlockAt(1, 64, 1).apply { type = Material.STONE }))
    }

    @Test
    fun `createSign registers the region automatically and records the support block`() {
        val sign = create(signBlock(1))
        assertTrue(signs.isRegistered("shop1", world))
        assertEquals(Triple(1, 63, 0), sign.support?.let { Triple(it.x, it.y, it.z) })
        assertEquals("STONE", sign.support?.originalType)
    }

    @Test
    fun `a missing sign is dropped only after the second failed check`() {
        val block = signBlock(1)
        create(block)
        block.type = Material.AIR

        manager.updateSign("shop1", world)
        assertNotNull(signs.getSign("shop1", world, 1), "first miss may be a restore in progress")

        manager.updateAllSigns() // the first miss queued another check
        assertNull(signs.getSign("shop1", world, 1))
        assertTrue(signs.isRegistered("shop1", world), "cleanup never removes the rental space")
    }

    @Test
    fun `a sign that comes back is not dropped`() {
        val block = signBlock(1)
        create(block)
        block.type = Material.AIR
        manager.updateSign("shop1", world)
        block.type = Material.OAK_SIGN
        manager.updateSign("shop1", world)
        block.type = Material.AIR
        manager.updateSign("shop1", world)
        assertNotNull(signs.getSign("shop1", world, 1))
    }

    @Test
    fun `removing a sign breaks it and restores its support block`() {
        val block = signBlock(1)
        val sign = create(block)
        world.getBlockAt(1, 63, 0).type = Material.DIRT // changed since registration

        manager.removeSign(sign)
        assertEquals(Material.AIR, block.type)
        assertEquals(Material.STONE, world.getBlockAt(1, 63, 0).type)
        assertTrue(signs.isRegistered("shop1", world))
    }

    @Test
    fun `a shared support block is only restored when its last sign is removed`() {
        val support = world.getBlockAt(5, 64, 5).apply { type = Material.STONE }
        val top = world.getBlockAt(5, 65, 5).apply { type = Material.OAK_SIGN }
        val first = create(top)
        // Second sign on the same support block (recorded directly; wall sign facing isn't mockable)
        val side = world.getBlockAt(5, 64, 6).apply { type = Material.OAK_SIGN }
        val second = signs.setSupportBlock(signs.addSign("shop1", world, side.location), support.location, "STONE", "")!!
        assertEquals(first.support?.let { Triple(it.x, it.y, it.z) }, Triple(5, 64, 5))

        support.type = Material.DIRT
        manager.removeSign(first)
        assertEquals(Material.DIRT, support.type, "still holds sign #2")
        manager.removeSign(second)
        assertEquals(Material.STONE, support.type)
    }

    @Test
    fun `removeRegionSetup removes every sign and the registration`() {
        val blocks = (1..2).map { signBlock(it * 2) }
        blocks.forEach { create(it) }

        assertEquals(2, manager.removeRegionSetup("shop1", world))
        assertTrue(blocks.all { it.type == Material.AIR })
        assertTrue(!signs.isRegistered("shop1", world))
        assertEquals(-1, manager.removeRegionSetup("shop1", world))
    }

    @Test
    fun `an admin breaking a sign or its support block drops the affected signs`() {
        val a = signBlock(1)
        create(a)
        val b = signBlock(3)
        create(b)

        manager.onSignBroken(a.location, "Admin")
        assertNull(signs.getSign("shop1", world, 1))

        manager.onSupportBlockBroken(world.getBlockAt(3, 63, 0).location, "Admin")
        assertNull(signs.getSign("shop1", world, 2))
        assertTrue(signs.isRegistered("shop1", world))
    }

    @Test
    fun `rented regions without a registration are registered on load`() {
        every { rentalManager.allRentals } returns
            listOf(Rental.create("legacy", "world", UUID.randomUUID(), "Steve", System.currentTimeMillis() + 86_400_000, 100.0))
        manager.loadAllSigns()
        assertTrue(signs.isRegistered("legacy", world))
    }
}
