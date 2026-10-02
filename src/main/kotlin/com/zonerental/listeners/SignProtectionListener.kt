package com.zonerental.listeners

import com.zonerental.ZoneRental
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.world.ChunkLoadEvent

/**
 * Keeps rental signs and their support blocks from being destroyed by the environment
 * (signs.environment-protection), and redraws signs that waited for their chunk to load
 * (signs.load-chunks-for-updates: false).
 */
class SignProtectionListener(private val plugin: ZoneRental) : Listener {

    private val enabled: Boolean
        get() = plugin.configManager.isEnvironmentProtection

    private fun isProtected(block: Block): Boolean = plugin.signsConfig.isProtectedBlock(block.location)

    /** Creepers, TNT, ghast fireballs, withers, end crystals, TNT minecarts... */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        if (enabled) event.blockList().removeIf(::isProtected)
    }

    /** Beds in the Nether/End, respawn anchors */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        if (enabled) event.blockList().removeIf(::isProtected)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        if (!enabled) return
        // Moved blocks, the spaces they move into (a sign there would be crushed) and the piston head
        val affected = event.blocks.flatMap { listOf(it, it.getRelative(event.direction)) } +
            event.block.getRelative(event.direction)
        if (affected.any(::isProtected)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        if (enabled && event.blocks.any(::isProtected)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBurn(event: BlockBurnEvent) {
        if (enabled && isProtected(event.block)) event.isCancelled = true
    }

    /** Endermen picking blocks up, withers breaking blocks, sand/gravel starting to fall */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        if (enabled && isProtected(event.block)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onLeavesDecay(event: LeavesDecayEvent) {
        if (enabled && isProtected(event.block)) event.isCancelled = true
    }

    /** Ice melting, snow fading, coral dying under a sign */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onFade(event: BlockFadeEvent) {
        if (enabled && isProtected(event.block)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChunkLoad(event: ChunkLoadEvent) {
        if (!plugin.configManager.isLoadChunksForSignUpdates) {
            plugin.signManager.onChunkLoad(event.chunk)
        }
    }
}
