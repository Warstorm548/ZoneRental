package com.zonerental.commands

import com.zonerental.ZoneRental
import com.zonerental.extensions.asPlayerOrNull
import com.zonerental.extensions.sendMiniMessage
import com.zonerental.managers.SignManager.CreateResult
import com.zonerental.util.WorldRegionParser
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class CreateSignCommand(private val plugin: ZoneRental) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val player = sender.asPlayerOrNull() ?: run {
            sender.sendMiniMessage("<red>This command can only be used by players!")
            return true
        }

        if (!player.hasPermission("zonerental.admin.createsign")) {
            player.sendMessage(plugin.configManager.getMessage("no-permission"))
            return true
        }

        if (args.isEmpty()) {
            player.sendMiniMessage("<red>Usage: /$label <region> or /$label <world:region>")
            return true
        }

        // "shop1" = region in the player's world, "world:shop1" = region in another world
        val parsed = WorldRegionParser.parse(args[0], player) ?: run {
            player.sendMiniMessage("<red>World not found! Use world:region (e.g., world:shop1)")
            return true
        }
        val regionWorld = parsed.getWorld() ?: run {
            player.sendMiniMessage("<red>World not found!")
            return true
        }

        // Use the region ID exactly as WorldGuard has it so "Shop1" and "shop1" share one rental space
        val region = plugin.worldGuardManager.getRegion(parsed.regionName, regionWorld) ?: run {
            player.sendMessage(plugin.configManager.getMessage("region-not-found", "{region}", parsed.getCompositeKey()))
            return true
        }
        val regionName = region.id
        val regionKey = "${regionWorld.name}:$regionName"

        // Get the block the player is looking at
        val targetBlock = player.getTargetBlock(null, 5)

        when (val result = plugin.signManager.createSign(regionName, regionWorld, targetBlock)) {
            CreateResult.NotASign ->
                player.sendMiniMessage("<red>You must be looking at a sign!")

            is CreateResult.AlreadyRegistered ->
                player.sendMessage(plugin.configManager.getMessage("sign-already-registered",
                    "{id}", result.existing.id.toString(), "{region}", result.existing.regionKey))

            is CreateResult.LimitReached ->
                player.sendMessage(plugin.configManager.getMessage("sign-limit-reached",
                    "{region}", regionKey, "{max}", result.max.toString()))

            is CreateResult.Created -> {
                if (result.newlyRegistered) {
                    player.sendMessage(plugin.configManager.getMessage("region-registered", "{region}", regionKey))
                }
                player.sendMessage(plugin.configManager.getMessage("sign-created",
                    "{id}", result.sign.id.toString(), "{region}", regionKey))
                if (result.insideRegions.isNotEmpty()) {
                    player.sendMiniMessage("<yellow>Warning: this sign or its support block is inside rental space " +
                        "${result.insideRegions.joinToString(", ")}. Restoring that region on expiry will remove it.")
                }
                if (result.newlyRegistered) {
                    player.sendMiniMessage("<gray>Use /${plugin.activePrefix}override to set custom rental settings for this region.")
                }
            }
        }

        return true
    }
}
