package com.zonerental.managers

import com.zonerental.ZoneRental
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.Bukkit
import org.bukkit.World
import java.util.UUID

/**
 * Manages WorldGuard region operations for the rental system.
 */
class WorldGuardManager(private val plugin: ZoneRental) {

    private var worldGuard: WorldGuardPlugin? = null

    init {
        setupWorldGuard()
    }

    private fun setupWorldGuard() {
        val wgPlugin = plugin.server.pluginManager.getPlugin("WorldGuard")

        if (wgPlugin == null || wgPlugin !is WorldGuardPlugin) {
            plugin.logger.severe("WorldGuard not found!")
            return
        }

        worldGuard = wgPlugin
        plugin.logger.info("WorldGuard integration enabled")
    }

    fun regionExists(regionName: String, world: World): Boolean {
        if (worldGuard == null) return false

        val regionManager = WorldGuard.getInstance().platform
            .regionContainer.get(BukkitAdapter.adapt(world))
            ?: return false

        return regionManager.hasRegion(regionName)
    }

    fun addPlayerToRegion(regionName: String, world: World, playerUUID: UUID) {
        val regionManager = WorldGuard.getInstance().platform
            .regionContainer.get(BukkitAdapter.adapt(world))

        if (regionManager == null) {
            plugin.logger.warning("No RegionManager found for world: ${world.name}")
            return
        }

        val region = regionManager.getRegion(regionName)
        if (region != null) {
            region.members.addPlayer(playerUUID)
            plugin.logger.info("Added player $playerUUID to region $regionName in world ${world.name}")
        } else {
            plugin.logger.warning("Region $regionName not found in world ${world.name}")
        }
    }

    fun removePlayerFromRegion(regionName: String, world: World, playerUUID: UUID) {
        val regionManager = WorldGuard.getInstance().platform
            .regionContainer.get(BukkitAdapter.adapt(world))

        if (regionManager == null) {
            plugin.logger.warning("No RegionManager found for world: ${world.name}")
            return
        }

        val region = regionManager.getRegion(regionName)
        if (region != null) {
            region.members.removePlayer(playerUUID)
            plugin.logger.info("Removed player $playerUUID from region $regionName in world ${world.name}")
        } else {
            plugin.logger.warning("Region $regionName not found in world ${world.name}")
        }
    }

    fun getRegion(regionName: String, world: World): ProtectedRegion? {
        if (worldGuard == null) return null

        val regionManager = WorldGuard.getInstance().platform
            .regionContainer.get(BukkitAdapter.adapt(world))
            ?: return null

        return regionManager.getRegion(regionName)
    }

    /**
     * Get the region names of one world for tab completion.
     */
    fun getRegionNames(world: World): Set<String> {
        if (worldGuard == null) return emptySet()

        val regionManager = WorldGuard.getInstance().platform
            .regionContainer.get(BukkitAdapter.adapt(world))
            ?: return emptySet()

        return regionManager.regions.keys
    }

    /**
     * Get all region names across all worlds for tab completion.
     */
    val allRegionNames: Set<String>
        get() {
            val regionNames = mutableSetOf<String>()

            for (world in Bukkit.getWorlds()) {
                val regionManager = WorldGuard.getInstance().platform
                    .regionContainer.get(BukkitAdapter.adapt(world))
                    ?: continue

                regionNames.addAll(regionManager.regions.keys)
            }

            return regionNames
        }
}
