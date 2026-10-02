package com.zonerental.commands

import com.zonerental.ZoneRental
import com.zonerental.extensions.sendMiniMessage
import com.zonerental.util.WorldRegionParser
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.WallSign
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Command to teleport players to their rented regions.
 * Supports owners and members.
 */
class TpCommand(private val plugin: ZoneRental) : CommandExecutor, TabCompleter {

    companion object {
        private val DANGEROUS_MATERIALS = setOf(
            Material.LAVA, Material.FIRE, Material.SOUL_FIRE,
            Material.CACTUS, Material.MAGMA_BLOCK, Material.WITHER_ROSE,
            Material.SWEET_BERRY_BUSH, Material.POWDER_SNOW
        )

        private val PASSABLE_MATERIALS = setOf(Material.WATER, Material.SNOW)

        private val PASSABLE_PATTERNS = listOf(
            "SIGN", "TORCH", "FLOWER", "GRASS", "TALL_GRASS", "SEAGRASS",
            "KELP", "RAIL", "CARPET", "PRESSURE_PLATE", "BUTTON", "TRIPWIRE"
        )
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        // Player-only command
        val player = sender as? Player ?: run {
            sender.sendMiniMessage("<red>This command can only be used by players!")
            return true
        }

        // Permission check
        if (!player.hasPermission("zonerental.tp")) {
            player.sendMessage(plugin.configManager.getMessage("no-permission"))
            return true
        }

        // Check if teleport is enabled
        if (!plugin.configManager.isTeleportEnabled) {
            player.sendMiniMessage("<red>Teleportation is currently disabled!")
            return true
        }

        // Validate arguments
        if (args.isEmpty()) {
            player.sendMiniMessage("<red>Usage: /$label <region>")
            player.sendMiniMessage("<yellow>Example: /$label shop1")
            player.sendMiniMessage("<yellow>Example: /$label world:shop1")
            return true
        }

        // Parse region with world awareness
        val parsed = WorldRegionParser.parse(args[0], player) ?: run {
            player.sendMiniMessage("<red>Invalid region format!")
            player.sendMiniMessage("<yellow>Use: <region> or world:<region>")
            return true
        }

        val world = parsed.getWorld() ?: run {
            player.sendMiniMessage("<red>World '${parsed.worldName}' is not loaded!")
            return true
        }
        val regionName = parsed.regionName

        // Check if region exists in WorldGuard
        if (!plugin.worldGuardManager.regionExists(regionName, world)) {
            player.sendMessage(plugin.configManager.getMessage("region-not-found",
                "{region}", parsed.getCompositeKey()))
            return true
        }

        // Get rental
        val rental = plugin.rentalManager.getRental(regionName, world) ?: run {
            player.sendMessage(plugin.configManager.getMessage("tp-not-rented",
                "{region}", parsed.getCompositeKey()))
            return true
        }

        // Check if player is owner or member
        val isOwner = rental.playerUUID == player.uniqueId
        val isMember = rental.hasMember(player.uniqueId)

        if (!isOwner && !isMember) {
            player.sendMessage(plugin.configManager.getMessage("tp-no-permission",
                "{region}", parsed.getCompositeKey()))
            return true
        }

        // Check cooldown (bypass for admins)
        if (!player.hasPermission("zonerental.admin.bypass")) {
            if (plugin.teleportCooldownManager.isOnCooldown(player)) {
                val remaining = plugin.teleportCooldownManager.getRemainingCooldown(player)
                player.sendMessage(plugin.configManager.getMessage("tp-cooldown",
                    "{time}", remaining.toString()))
                return true
            }
        }

        // Teleport to the oldest sign (lowest ID) whose world is loaded
        val signLocation = plugin.signsConfig.getSigns(regionName, world)
            .firstNotNullOfOrNull { it.location(plugin.server) } ?: run {
            player.sendMiniMessage("<red>No rental sign found for region ${parsed.getCompositeKey()}")
            player.sendMiniMessage("<yellow>Please contact an administrator.")
            return true
        }

        // Find safe location
        val signBlock = signLocation.block
        val maxSearchDistance = plugin.configManager.teleportMaxSearchDistance
        val safeLocation = findSafeLocation(signLocation, signBlock, player, maxSearchDistance) ?: run {
            player.sendMessage(plugin.configManager.getMessage("tp-no-safe-location",
                "{region}", parsed.getCompositeKey()))
            return true
        }

        // Show cross-world warning if applicable
        if (plugin.configManager.isTeleportCrossWorldWarning && player.world != safeLocation.world) {
            player.sendMessage(plugin.configManager.getMessage("tp-cross-world-warning",
                "{region}", parsed.getCompositeKey(),
                "{world}", safeLocation.world.name))
        }

        // Perform teleport
        val success = player.teleport(safeLocation)

        if (success) {
            // Play sound effect
            if (plugin.configManager.isTeleportSoundEnabled) {
                player.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)
            }

            // Spawn particles
            if (plugin.configManager.isTeleportParticleEnabled) {
                player.world.spawnParticle(Particle.PORTAL, safeLocation, 50, 0.5, 1.0, 0.5, 0.1)
            }

            // Success message
            player.sendMessage(plugin.configManager.getMessage("tp-success",
                "{region}", parsed.getCompositeKey()))

            // Start cooldown (bypass for admins)
            if (!player.hasPermission("zonerental.admin.bypass")) {
                plugin.teleportCooldownManager.startCooldown(player)
            }
        } else {
            player.sendMiniMessage("<red>Teleport failed! Please try again.")
        }

        return true
    }

    /**
     * Finds a safe teleport location using 3D search algorithm.
     * Searches forward from sign, then down/up for floor at each position.
     */
    private fun findSafeLocation(signLocation: Location, signBlock: Block, player: Player, maxSearchDistance: Int): Location? {
        val facing = getSignFacing(signBlock)
        val config = plugin.configManager
        val forwardSearchDistance = config.teleportForwardSearchDistance
        val floorSearchDown = config.teleportFloorSearchDown
        val floorSearchUp = config.teleportFloorSearchUp
        val yaw = getYawFromBlockFace(facing)

        for (forward in 1..forwardSearchDistance) {
            val forwardLocation = signLocation.clone().add(
                facing.modX * forward.toDouble(),
                0.0,
                facing.modZ * forward.toDouble()
            )

            // Search downward first
            findFloorLocation(forwardLocation, floorSearchDown, false)?.let { floorLocation ->
                val spawnLocation = floorLocation.clone().add(0.0, 1.0, 0.0)
                if (isSafeLocation(spawnLocation)) {
                    spawnLocation.yaw = yaw
                    spawnLocation.pitch = 0f
                    return centerLocation(spawnLocation)
                }
            }

            // Then search upward
            findFloorLocation(forwardLocation, floorSearchUp, true)?.let { floorLocation ->
                val spawnLocation = floorLocation.clone().add(0.0, 1.0, 0.0)
                if (isSafeLocation(spawnLocation)) {
                    spawnLocation.yaw = yaw
                    spawnLocation.pitch = 0f
                    return centerLocation(spawnLocation)
                }
            }
        }

        return null
    }

    /**
     * Gets the facing direction of a sign block.
     */
    private fun getSignFacing(signBlock: Block): BlockFace {
        return when (val blockData = signBlock.blockData) {
            is WallSign -> blockData.facing
            is org.bukkit.block.data.type.Sign -> blockData.rotation
            else -> BlockFace.NORTH
        }
    }

    /**
     * Searches for solid floor starting from a location.
     */
    private fun findFloorLocation(startLocation: Location, maxDistance: Int, searchUp: Boolean): Location? {
        for (offset in 0..maxDistance) {
            val yOffset = if (searchUp) offset else -offset
            val testLocation = startLocation.clone().add(0.0, yOffset.toDouble(), 0.0)
            val block = testLocation.block

            if (block.type.isSolid && !isDangerousBlock(block)) {
                return testLocation
            }
        }
        return null
    }

    /**
     * Convert BlockFace to player yaw angle.
     */
    private fun getYawFromBlockFace(face: BlockFace): Float = when (face) {
        BlockFace.SOUTH -> 0.0f
        BlockFace.WEST -> 90.0f
        BlockFace.NORTH -> 180.0f
        BlockFace.EAST -> 270.0f
        else -> 0.0f
    }

    /**
     * Check if a location is safe for teleportation.
     */
    private fun isSafeLocation(location: Location): Boolean {
        val feetBlock = location.block
        val headBlock = location.clone().add(0.0, 1.0, 0.0).block
        val floorBlock = location.clone().subtract(0.0, 1.0, 0.0).block

        // Floor must be solid and not dangerous
        if (!floorBlock.type.isSolid || isDangerousBlock(floorBlock)) {
            return false
        }

        // Feet and head must be passable and not dangerous
        if (!isPassableBlock(feetBlock) || !isPassableBlock(headBlock)) {
            return false
        }

        if (isDangerousBlock(feetBlock) || isDangerousBlock(headBlock)) {
            return false
        }

        return true
    }

    /**
     * Centers a location within its block.
     */
    private fun centerLocation(location: Location): Location {
        val centered = location.clone().add(0.5, 0.0, 0.5)
        centered.yaw = location.yaw
        centered.pitch = location.pitch
        return centered
    }

    /**
     * Check if a block is passable (player can stand in it).
     */
    private fun isPassableBlock(block: Block): Boolean {
        val type = block.type

        if (type.isAir) return true

        return type in PASSABLE_MATERIALS ||
               PASSABLE_PATTERNS.any { type.name.contains(it) }
    }

    /**
     * Check if a block is dangerous to stand in/on.
     */
    private fun isDangerousBlock(block: Block): Boolean {
        val type = block.type
        return type in DANGEROUS_MATERIALS || type.name.contains("CAMPFIRE")
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (args.size != 1 || sender !is Player) return emptyList()

        val playerRentals = plugin.rentalManager.getPlayerRentals(sender.uniqueId)
        val memberRentals = plugin.rentalManager.getRentalsWhereMember(sender.uniqueId)

        return (playerRentals + memberRentals)
            .flatMap { listOf(it.regionName, it.compositeKey) }
            .filter { it.lowercase().startsWith(args[0].lowercase()) }
            .distinct()
            .sorted()
    }
}
