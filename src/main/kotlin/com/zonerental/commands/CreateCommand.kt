package com.zonerental.commands

import com.zonerental.ZoneRental
import com.zonerental.extensions.sendMiniMessage
import com.zonerental.util.WorldRegionParser
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * Registers a WorldGuard region as a rental space without placing a sign.
 * Signs are added afterwards with the createsign command.
 */
class CreateCommand(private val plugin: ZoneRental) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission("zonerental.admin.create")) {
            sender.sendMessage(plugin.configManager.getMessage("no-permission"))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMiniMessage("<red>Usage: /$label <world:region>")
            sender.sendMiniMessage("<yellow>Example: /$label world:shop1")
            return true
        }

        val parsed = WorldRegionParser.parse(args[0], sender) ?: run {
            sender.sendMiniMessage("<red>Invalid format! Console must use world:region format (e.g., world:shop1)")
            return true
        }
        val world = parsed.getWorld() ?: run {
            sender.sendMiniMessage("<red>World not found!")
            return true
        }

        // Store the region ID exactly as WorldGuard has it so "Shop1" and "shop1" can't register twice
        val region = plugin.worldGuardManager.getRegion(parsed.regionName, world) ?: run {
            sender.sendMessage(plugin.configManager.getMessage("region-not-found", "{region}", parsed.getCompositeKey()))
            return true
        }
        val regionKey = "${world.name}:${region.id}"

        if (!plugin.signsConfig.registerRegion(region.id, world)) {
            sender.sendMessage(plugin.configManager.getMessage("region-already-registered", "{region}", regionKey))
            return true
        }
        plugin.signsConfig.save()

        sender.sendMessage(plugin.configManager.getMessage("region-registered", "{region}", regionKey))
        plugin.logger.info("Admin ${sender.name} registered rental space $regionKey")
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (args.size != 1) return emptyList()
        val registered = plugin.signsConfig.getRegisteredRegions()
        return plugin.server.worlds.flatMap { world ->
            plugin.worldGuardManager.getRegionNames(world).map { "${world.name}:$it" }
        }
            .filter { it !in registered && it.startsWith(args[0], ignoreCase = true) }
            .sorted()
    }
}
