package com.zonerental.listeners

import com.zonerental.config.SignsConfig
import com.zonerental.testsupport.MockPluginTest
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignProtectionListenerTest : MockPluginTest() {

    private lateinit var signs: SignsConfig
    private lateinit var listener: SignProtectionListener
    private lateinit var sign: Block
    private lateinit var support: Block
    private lateinit var other: Block

    @BeforeEach
    fun setUp() {
        useRealConfigManager()
        signs = SignsConfig(plugin)
        every { plugin.signsConfig } returns signs

        support = world.getBlockAt(10, 63, 10).apply { type = Material.OAK_PLANKS }
        sign = world.getBlockAt(10, 64, 10).apply { type = Material.OAK_SIGN }
        other = world.getBlockAt(12, 63, 10).apply { type = Material.STONE }
        signs.setSupportBlock(signs.addSign("shop1", world, sign.location), support.location, "OAK_PLANKS", "")

        listener = SignProtectionListener(plugin)
    }

    private fun explosion(): EntityExplodeEvent {
        val blocks = mutableListOf(sign, support, other)
        return mockk(relaxed = true) { every { blockList() } returns blocks }
    }

    @Test
    fun `explosions skip rental signs and their support blocks`() {
        val event = explosion()
        listener.onEntityExplode(event)
        assertEquals(listOf(other), event.blockList())
    }

    @Test
    fun `turning environment protection off lets explosions through`() {
        bukkitConfig.set("signs.environment-protection", false)
        useRealConfigManager()
        val event = explosion()
        listener.onEntityExplode(event)
        assertEquals(3, event.blockList().size)
    }

    @Test
    fun `pistons can't move a support block`() {
        var cancelled = false
        val event = mockk<BlockPistonExtendEvent>(relaxed = true) {
            every { blocks } returns listOf(support)
            every { direction } returns BlockFace.EAST
            every { block } returns world.getBlockAt(9, 63, 10)
            every { isCancelled = any() } answers { cancelled = firstArg() }
        }
        listener.onPistonExtend(event)
        assertTrue(cancelled)
    }

    @Test
    fun `pistons can't push a block into a rental sign`() {
        var cancelled = false
        val pushed = world.getBlockAt(9, 64, 10).apply { type = Material.STONE }
        val event = mockk<BlockPistonExtendEvent>(relaxed = true) {
            every { blocks } returns listOf(pushed)
            every { direction } returns BlockFace.EAST
            every { block } returns world.getBlockAt(8, 64, 10)
            every { isCancelled = any() } answers { cancelled = firstArg() }
        }
        listener.onPistonExtend(event)
        assertTrue(cancelled)
    }

    @Test
    fun `pistons elsewhere are not affected`() {
        var cancelled = false
        val event = mockk<BlockPistonExtendEvent>(relaxed = true) {
            every { blocks } returns listOf(other)
            every { direction } returns BlockFace.EAST
            every { block } returns world.getBlockAt(11, 63, 10)
            every { isCancelled = any() } answers { cancelled = firstArg() }
        }
        listener.onPistonExtend(event)
        assertFalse(cancelled)
    }

    @Test
    fun `fire can't burn a support block`() {
        val event = BlockBurnEvent(support, null)
        listener.onBurn(event)
        assertTrue(event.isCancelled)
    }
}
