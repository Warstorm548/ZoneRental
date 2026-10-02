package com.zonerental.managers

import com.zonerental.ZoneRental
import com.zonerental.extensions.toComponent
import com.zonerental.models.RentalSign
import net.kyori.adventure.text.Component
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.block.data.type.HangingSign
import org.bukkit.block.data.type.WallHangingSign
import org.bukkit.block.data.type.WallSign
import org.bukkit.block.sign.Side

/**
 * Manages rental signs. A rental space ("world:region") can have several signs, possibly in
 * other worlds; they are always redrawn together.
 */
class SignManager(private val plugin: ZoneRental) {

    /** Outcome of [createSign]. */
    sealed interface CreateResult {
        data class Created(val sign: RentalSign, val newlyRegistered: Boolean, val insideRegions: List<String>) : CreateResult
        data class AlreadyRegistered(val existing: RentalSign) : CreateResult
        data class LimitReached(val max: Int) : CreateResult
        data object NotASign : CreateResult
    }

    private val dirtySigns = mutableSetOf<String>() // "world:region" keys of spaces whose signs need a redraw

    /** Signs found missing once; removed if still missing on the next check. Keyed by [token]. */
    private val missingOnce = mutableSetOf<String>()

    /** Signs waiting for their chunk to load (load-chunks-for-updates: false). Keyed by [token]. */
    private val waitingForChunk = mutableSetOf<String>()

    private fun token(sign: RentalSign) = "${sign.regionKey}#${sign.id}"

    fun loadAllSigns() {
        missingOnce.clear()
        waitingForChunk.clear()

        // Rentals from before rental spaces were registered must not become orphans
        registerRentedRegions()

        // Migrate existing signs to include support block data
        migrateSupportBlocks()

        // Mark all signs as dirty to update them on startup
        markAllSignsDirty()

        // Update all dirty signs
        updateAllSigns()
    }

    private fun registerRentedRegions() {
        var registered = 0
        for (rental in plugin.rentalManager.allRentals) {
            val world = plugin.server.getWorld(rental.worldName) ?: continue
            if (plugin.signsConfig.registerRegion(rental.regionName, world)) registered++
        }
        if (registered > 0) {
            plugin.logger.info("Registered $registered rented region(s) that had no rental space entry")
            plugin.signsConfig.save()
        }
    }

    /**
     * Marks all signs as dirty for initial update or full refresh.
     */
    private fun markAllSignsDirty() {
        dirtySigns.addAll(plugin.signsConfig.getRegisteredRegions())
    }

    /**
     * Migrates existing signs to include support block data.
     * This is for backward compatibility with signs created before this feature.
     */
    private fun migrateSupportBlocks() {
        var migratedCount = 0

        for (sign in plugin.signsConfig.getAllSigns()) {
            if (sign.support != null) continue

            val signLoc = sign.location(plugin.server) ?: continue
            if (!isChunkReady(signLoc)) continue
            val signBlock = signLoc.block

            if (signBlock.state !is Sign) {
                plugin.logger.warning("Rental sign ${sign.describe()} no longer exists at location")
                continue
            }

            val supportBlock = getSupportBlock(signBlock)
            if (supportBlock != null) {
                plugin.signsConfig.setSupportBlock(sign, supportBlock.location, supportBlock.type.name, supportBlock.blockData.asString)
                migratedCount++
                plugin.logger.info("Migrated support block for rental sign ${sign.describe()} (Type: ${supportBlock.type.name})")
            } else {
                plugin.logger.warning("Could not detect support block for rental sign ${sign.describe()}")
            }
        }

        if (migratedCount > 0) {
            plugin.logger.info("Migrated $migratedCount sign(s) with support block protection")
        }
    }

    /**
     * Registers [signBlock] as a sign of the region (registering the region if needed),
     * records its support block, clears both sides and draws it. Saves signs.yml.
     */
    fun createSign(regionName: String, regionWorld: World, signBlock: Block): CreateResult {
        if (signBlock.state !is Sign) return CreateResult.NotASign

        plugin.signsConfig.getSignAt(signBlock.location)?.let { return CreateResult.AlreadyRegistered(it) }

        val max = plugin.configManager.maxSignsPerRegion
        if (max != -1 && plugin.signsConfig.getSignCount(regionName, regionWorld) >= max) {
            return CreateResult.LimitReached(max)
        }

        val newlyRegistered = !plugin.signsConfig.isRegistered(regionName, regionWorld)
        var sign = plugin.signsConfig.addSign(regionName, regionWorld, signBlock.location)

        val supportBlock = getSupportBlock(signBlock)
        if (supportBlock != null) {
            val blockType = supportBlock.type.name
            sign = plugin.signsConfig.setSupportBlock(sign, supportBlock.location, blockType, supportBlock.blockData.asString) ?: sign
            plugin.logger.info(
                "Stored support block for rental sign ${sign.describe()} " +
                "(Type: $blockType at ${supportBlock.x},${supportBlock.y},${supportBlock.z})"
            )
        } else {
            plugin.logger.warning("Could not detect support block for rental sign ${sign.describe()}")
        }

        // Start from blank sides so text from an earlier use of this sign doesn't linger
        (signBlock.state as? Sign)?.let { state ->
            for (side in Side.entries) {
                for (line in 0 until 4) state.getSide(side).line(line, Component.empty())
            }
            state.update(true)
        }

        updateSign(regionName, regionWorld)
        plugin.signsConfig.save()

        plugin.logger.info("Created rental sign ${sign.describe()}")
        return CreateResult.Created(sign, newlyRegistered, registeredRegionsContaining(sign))
    }

    /**
     * Registered rental spaces (including the sign's own) whose WorldGuard region contains the
     * sign or its support block. Those blocks would be overwritten when that region is restored.
     */
    private fun registeredRegionsContaining(sign: RentalSign): List<String> {
        val positions = listOfNotNull(
            Triple(sign.x, sign.y, sign.z),
            sign.support?.let { Triple(it.x, it.y, it.z) }
        )
        val world = plugin.server.getWorld(sign.signWorld) ?: return emptyList()
        return plugin.signsConfig.getRegisteredRegions()
            .filter { it.startsWith("${sign.signWorld}:") }
            .filter { key ->
                val region = plugin.worldGuardManager.getRegion(key.substringAfter(":"), world) ?: return@filter false
                positions.any { (x, y, z) -> region.contains(x, y, z) }
            }
    }

    /**
     * Gets the support block for a sign (the block it's attached to, placed on or hanging from).
     */
    private fun getSupportBlock(signBlock: Block): Block? {
        return when (val blockData = signBlock.blockData) {
            // Wall signs are attached to the block opposite to their facing direction
            is WallSign -> signBlock.getRelative(blockData.facing.oppositeFace)
            // Wall hanging signs hang from a bracket on either side, along the sign's plane
            is WallHangingSign -> {
                val right = signBlock.getRelative(rotateClockwise(blockData.facing))
                val left = signBlock.getRelative(rotateClockwise(blockData.facing).oppositeFace)
                if (!right.type.isAir) right else if (!left.type.isAir) left else null
            }
            // Ceiling hanging signs hang from the block above
            is HangingSign -> signBlock.getRelative(BlockFace.UP)
            // Otherwise it's a standing sign - support block is below
            else -> if (signBlock.type.name.contains("SIGN")) signBlock.getRelative(BlockFace.DOWN) else null
        }
    }

    private fun rotateClockwise(face: BlockFace): BlockFace = when (face) {
        BlockFace.NORTH -> BlockFace.EAST
        BlockFace.EAST -> BlockFace.SOUTH
        BlockFace.SOUTH -> BlockFace.WEST
        BlockFace.WEST -> BlockFace.NORTH
        else -> face
    }

    /**
     * Removes one sign through /zrremove: breaks the sign block and restores its support block
     * (unless another rental sign still uses it). The rental space stays registered.
     */
    fun removeSign(sign: RentalSign) {
        plugin.signsConfig.removeSign(sign.regionKey, sign.id)
        clearPhysicalSign(sign)
        plugin.signsConfig.save()
        plugin.logger.info("Removed rental sign ${sign.describe()}")
    }

    /**
     * Completely removes ZoneRental setup from a region: every sign (block broken, support
     * blocks restored) and the registration itself. Saves signs.yml.
     *
     * @return the number of signs removed, or -1 if the region was not registered
     */
    fun removeRegionSetup(regionName: String, world: World): Int {
        if (!plugin.signsConfig.isRegistered(regionName, world)) return -1

        val removed = plugin.signsConfig.unregisterRegion(regionName, world)
        removed.forEach(::clearPhysicalSign)
        plugin.signsConfig.save()
        dirtySigns.remove("${world.name}:$regionName")

        plugin.logger.info("Removed ZoneRental setup from region ${world.name}:$regionName (${removed.size} sign(s))")
        return removed.size
    }

    /** Breaks a removed sign's block and restores its support block if no other rental sign uses it. */
    private fun clearPhysicalSign(sign: RentalSign) {
        forget(sign)
        val signLoc = sign.location(plugin.server) ?: return

        val block = signLoc.block
        if (block.state is Sign) {
            block.type = Material.AIR
        }

        val support = sign.support ?: return
        val supportLoc = sign.supportLocation(plugin.server) ?: return
        if (plugin.signsConfig.getSignsOnSupportBlock(supportLoc).isNotEmpty()) return // still holds another sign

        try {
            val supportBlock = supportLoc.block
            supportBlock.type = Material.valueOf(support.originalType)
            if (support.originalData.isNotEmpty()) {
                try {
                    supportBlock.blockData = plugin.server.createBlockData(support.originalData)
                } catch (e: IllegalArgumentException) {
                    plugin.logger.warning("Could not restore block data for support block: ${e.message}")
                }
            }
            plugin.logger.info("Restored support block for rental sign ${sign.describe()} to ${support.originalType}")
        } catch (e: IllegalArgumentException) {
            plugin.logger.warning(
                "Could not restore support block for rental sign ${sign.describe()}: " +
                "Invalid material type - ${e.message}"
            )
        }
    }

    /**
     * A rental sign at [location] was broken by an admin (zonerental.admin.breaksign).
     * Drops it from signs.yml; the rental space stays registered.
     */
    fun onSignBroken(location: Location, breakerName: String) {
        val sign = plugin.signsConfig.getSignAt(location) ?: return
        dropSign(sign, "broken by $breakerName")
    }

    /**
     * The support block at [location] was broken by an admin. Every sign on it pops off,
     * so they are dropped from signs.yml.
     */
    fun onSupportBlockBroken(location: Location, breakerName: String) {
        plugin.signsConfig.getSignsOnSupportBlock(location).forEach {
            dropSign(it, "support block broken by $breakerName")
        }
    }

    /** Removes a sign entry without touching any blocks, logs why, and saves. */
    private fun dropSign(sign: RentalSign, reason: String) {
        plugin.signsConfig.removeSign(sign.regionKey, sign.id)
        forget(sign)
        plugin.signsConfig.save()
        plugin.logger.warning("Removed rental sign ${sign.describe()} from signs.yml: $reason")
        if (plugin.signsConfig.getSigns(sign.regionKey).isEmpty()) {
            plugin.logger.warning("Rental space ${sign.regionKey} has no signs left (still registered)")
        }
    }

    private fun forget(sign: RentalSign) {
        missingOnce.remove(token(sign))
        waitingForChunk.remove(token(sign))
    }

    /**
     * Redraws every sign of a rental space. Prices, durations and the rental are looked up in
     * the region's [world]; the signs themselves may be in any world.
     */
    fun updateSign(regionName: String, world: World) {
        val signs = plugin.signsConfig.getSigns(regionName, world)
        if (signs.isEmpty()) return

        val rental = plugin.rentalManager.getRental(regionName, world)
        val lines = if (rental == null) availableLines(regionName, world) else rentedLines(rental)
        signs.forEach { drawSign(it, lines) }
    }

    private fun drawSign(sign: RentalSign, lines: List<Component>?) {
        val location = sign.location(plugin.server) ?: return // sign's world not loaded: keep and retry later

        if (!isChunkReady(location)) {
            waitingForChunk += token(sign)
            return
        }
        waitingForChunk -= token(sign)

        val block = location.block
        val state = block.state as? Sign
        if (state == null) {
            handleMissingSign(sign)
            return
        }
        missingOnce -= token(sign)

        if (lines == null) return
        val sides = if (block.blockData is WallSign) listOf(Side.FRONT) else listOf(Side.FRONT, Side.BACK)
        for (side in sides) {
            val signSide = state.getSide(side)
            for (line in 0 until 4) {
                signSide.line(line, lines.getOrElse(line) { Component.empty() })
            }
        }
        state.update(true)
    }

    /** True if the sign's chunk is loaded, or may be loaded (signs.load-chunks-for-updates). */
    private fun isChunkReady(location: Location): Boolean =
        plugin.configManager.isLoadChunksForSignUpdates ||
            location.world.isChunkLoaded(location.blockX shr 4, location.blockZ shr 4)

    /**
     * The sign's block is loaded but isn't a sign. The first time, check again on the next
     * update pass (a region restore may be replacing it); the second time, drop the entry.
     */
    private fun handleMissingSign(sign: RentalSign) {
        if (missingOnce.add(token(sign))) {
            plugin.logger.warning("Rental sign ${sign.describe()} is not a sign! Checking again on the next update")
            dirtySigns += sign.regionKey
            return
        }
        dropSign(sign, "the block is no longer a sign")
    }

    /** Redraws signs that were waiting for this chunk to load. */
    fun onChunkLoad(chunk: Chunk) {
        val waiting = plugin.signsConfig.getSignsInChunk(chunk.world.name, chunk.x, chunk.z)
            .filter { token(it) in waitingForChunk }
        if (waiting.isEmpty()) return
        dirtySigns.addAll(waiting.map { it.regionKey })
        // Next tick: don't edit block states while the chunk is still being loaded
        plugin.server.scheduler.runTask(plugin, Runnable { updateAllSigns() })
    }

    private fun availableLines(regionName: String, world: World): List<Component>? {
        val price = plugin.configManager.getPriceForRegion(regionName, world)
        val duration = plugin.configManager.getDurationForRegion(regionName, world)
        val formattedPrice = String.format(plugin.configManager.currencyFormat, price)

        val formatList = plugin.configManager.availableSignFormat

        // Null/empty check to prevent NPE
        if (formatList.isNullOrEmpty()) {
            plugin.logger.warning("Available sign format is null or empty for region $regionName")
            return null
        }

        return formatList.take(4).map { format ->
            format
                .replace("{region}", regionName)
                .replace("{price}", formattedPrice)
                .replace("{duration}", duration.toString())
                .toComponent()
        }
    }

    private fun rentedLines(rental: Rental): List<Component>? {
        val formatList = plugin.configManager.rentedSignFormat

        // Null/empty check to prevent NPE
        if (formatList.isNullOrEmpty()) {
            plugin.logger.warning("Rented sign format is null or empty for region ${rental.regionName}")
            return null
        }

        return formatList.take(4).map { format ->
            format
                .replace("{region}", rental.regionName)
                .replace("{owner}", rental.playerName)
                .replace("{expires}", rental.formattedEndDate)
                .replace("{days}", rental.daysRemaining.toString())
                .replace("{hours}", rental.hoursRemaining.toString())
                .toComponent()
        }
    }

    /**
     * Marks a sign as dirty (needs update).
     * Uses composite key format "world:region".
     */
    fun markSignDirty(regionName: String, world: World) {
        val compositeKey = "${world.name}:$regionName"
        dirtySigns.add(compositeKey)
    }

    /**
     * Marks multiple signs as dirty using composite keys (world:region format).
     * Used for bulk operations like group overrides.
     */
    fun bulkMarkSignsDirty(compositeKeys: List<String>) {
        dirtySigns.addAll(compositeKeys)
    }

    /**
     * Updates all dirty signs and clears the dirty set.
     * This is much more efficient than updating ALL signs every 30 seconds.
     */
    fun updateAllSigns() {
        if (dirtySigns.isEmpty()) {
            return // Nothing to update
        }

        // Take a snapshot: drawing can mark spaces dirty again for the next pass
        val batch = dirtySigns.toList()
        dirtySigns.clear()

        for (compositeKey in batch) {
            if (!plugin.signsConfig.isRegistered(compositeKey)) continue // Space was removed
            val world = plugin.server.getWorld(compositeKey.substringBefore(":")) ?: continue
            updateSign(compositeKey.substringAfter(":"), world)
        }
    }

    fun isRentalSign(location: Location): Boolean {
        return plugin.signsConfig.getSignAt(location) != null
    }

    /**
     * @return Composite key "world:region" of the rental space this sign belongs to, or null
     */
    fun getRegionFromSign(location: Location): String? {
        return plugin.signsConfig.getRegionByLocation(location)
    }

    /**
     * Gets the region name if the given location is a protected support block.
     * Uses O(1) index lookup instead of O(n) linear scan for better performance.
     *
     * @return Composite key "world:region" if this is a support block, null otherwise
     */
    fun getSupportBlockRegion(location: Location): String? {
        // O(1) lookup via support block index
        return plugin.signsConfig.getSupportBlockByLocation(location)
    }
}
