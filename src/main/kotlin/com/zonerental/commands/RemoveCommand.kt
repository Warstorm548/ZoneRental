package com.zonerental.commands

import com.zonerental.ZoneRental
import com.zonerental.extensions.sendMiniMessage
import com.zonerental.models.RentalSign
import com.zonerental.util.WorldRegionParser
import org.bukkit.block.Sign
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Removes rental signs, or (with "all") the complete ZoneRental setup of a region:
 * signs, schematic, configuration data and the rental space registration.
 * Useful when a region needs to be repurposed or is no longer needed for rentals
 */
class RemoveCommand(private val plugin: ZoneRental) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission("zonerental.admin.remove")) {
            sender.sendMessage(plugin.configManager.getMessage("no-permission"))
            return true
        }

        when {
            // /zrremove (looking at a rental sign) -> remove that sign
            args.isEmpty() -> {
                val target = (sender as? Player)?.getTargetBlock(null, 5)
                val sign = target?.let { plugin.signsConfig.getSignAt(it.location) }
                when {
                    sign != null -> removeOneSign(sender, sign)
                    target?.state is Sign -> sender.sendMessage(plugin.configManager.getMessage("sign-not-rental"))
                    else -> sendUsage(sender, label)
                }
            }

            // /zrremove all world:shop1 -> full teardown
            args[0].equals("all", ignoreCase = true) -> {
                if (args.size < 2) sendUsage(sender, label) else removeRegionSetup(sender, args[1])
            }

            // /zrremove world:shop1 <id> -> remove that sign
            args.size >= 2 -> removeSignById(sender, label, args[0], args[1])

            // /zrremove world:shop1 alone does nothing, so a forgotten "all" can't wipe a region
            else -> sendUsage(sender, label)
        }
        return true
    }

    private fun sendUsage(sender: CommandSender, label: String) {
        sender.sendMiniMessage("<red>Usage:")
        sender.sendMiniMessage("<yellow>  /$label<gray> - remove the rental sign you are looking at")
        sender.sendMiniMessage("<yellow>  /$label <world:region> <id><gray> - remove one sign by its ID (see signs.yml)")
        sender.sendMiniMessage("<yellow>  /$label all <world:region><gray> - completely remove ZoneRental setup from the region")
        sender.sendMiniMessage("<gray>  Removing everything resets an active rental with a full refund.")
    }

    private fun removeSignById(sender: CommandSender, label: String, regionArg: String, idArg: String) {
        val id = idArg.toIntOrNull() ?: run {
            sendUsage(sender, label)
            return
        }
        val parsed = WorldRegionParser.parse(regionArg, sender) ?: run {
            sender.sendMiniMessage("<red>Invalid format! Console must use world:region format (e.g., world:shop1)")
            return
        }
        val world = parsed.getWorld() ?: run {
            sender.sendMiniMessage("<red>World not found!")
            return
        }
        if (!plugin.signsConfig.isRegistered(parsed.regionName, world)) {
            sender.sendMessage(plugin.configManager.getMessage("region-not-registered", "{region}", parsed.getCompositeKey()))
            return
        }
        val sign = plugin.signsConfig.getSign(parsed.regionName, world, id) ?: run {
            sender.sendMessage(plugin.configManager.getMessage("sign-not-found",
                "{region}", parsed.getCompositeKey(), "{id}", id.toString()))
            return
        }
        removeOneSign(sender, sign)
    }

    /** Removes one sign; the rental space and any rental stay as they are. */
    private fun removeOneSign(sender: CommandSender, sign: RentalSign) {
        plugin.signManager.removeSign(sign)
        sender.sendMessage(plugin.configManager.getMessage("sign-removed",
            "{id}", sign.id.toString(), "{region}", sign.regionKey))
        if (plugin.signsConfig.getSigns(sign.regionKey).isEmpty()) {
            sender.sendMiniMessage("<yellow>${sign.regionKey} has no signs left. It stays a rental space; " +
                "use /${plugin.activePrefix}createsign to add one or /${plugin.activePrefix}remove all ${sign.regionKey} to remove it.")
        }
        plugin.logger.info("Admin ${sender.name} removed rental sign ${sign.describe()}")
    }

    /** Full teardown: refund/reset, all signs, schematic, group membership, overrides, registration. */
    private fun removeRegionSetup(sender: CommandSender, regionArg: String) {
        // Parse region argument with world inference
        val parsed = WorldRegionParser.parse(regionArg, sender) ?: run {
            sender.sendMiniMessage("<red>Invalid format! Console must use world:region format (e.g., world:shop1)")
            return
        }

        val world = parsed.getWorld() ?: run {
            sender.sendMiniMessage("<red>World not found!")
            return
        }
        val regionName = parsed.regionName

        // Check the region exists (a registered space can be torn down after its WorldGuard region was deleted)
        if (!plugin.worldGuardManager.regionExists(regionName, world) && !plugin.signsConfig.isRegistered(regionName, world)) {
            sender.sendMessage(plugin.configManager.getMessage("region-not-found",
                "{region}", parsed.getCompositeKey()))
            return
        }

        // Check if region has an active rental
        val rental = plugin.rentalManager.getRental(regionName, world)
        if (rental != null) {
            // Reset the rental with net refund first (prevents double-refunds)
            val refundDetails = plugin.rentalManager.resetRentalWithRefund(regionName, world)

            if (refundDetails != null) {
                val playerName = refundDetails["playerName"] as String
                val refundAmount = refundDetails["refundAmount"] as Double
                val totalPaid = refundDetails["totalPaid"] as Double
                val alreadyRefunded = refundDetails["alreadyRefunded"] as Double

                val formattedAmount = String.format(plugin.configManager.currencyFormat, refundAmount)
                val formattedTotal = String.format(plugin.configManager.currencyFormat, totalPaid)
                val formattedAlready = String.format(plugin.configManager.currencyFormat, alreadyRefunded)

                sender.sendMiniMessage("<yellow>Active rental found. Player $playerName has been refunded $formattedAmount")

                // Show refund breakdown if any previous refunds exist
                if (alreadyRefunded > 0) {
                    sender.sendMiniMessage("<gray>  Total paid: <yellow>$formattedTotal")
                    sender.sendMiniMessage("<gray>  Already refunded: <yellow>$formattedAlready")
                    sender.sendMiniMessage("<gray>  Net refund: <green>$formattedAmount")
                }
            }
        }

        // Remove every rental sign and the rental space registration
        val signsRemoved = plugin.signManager.removeRegionSetup(regionName, world)

        // Delete WorldEdit schematic if it exists
        val schematicDeleted = if (plugin.worldEditManager.hasCapture(regionName, world)) {
            plugin.worldEditManager.deleteCapture(regionName, world)
            true
        } else {
            false
        }

        // Check if region is in a group and remove it
        val compositeKey = parsed.getCompositeKey()
        val groupName = plugin.groupsConfig.getRegionGroup(compositeKey)
        val groupRemoved = if (groupName != null) {
            plugin.groupsConfig.removeRegionsFromGroup(groupName, listOf(compositeKey))
        } else {
            false
        }

        // Only remove individual overrides if region was NOT in a group
        // (group membership already clears individual overrides when joining)
        val regionConfigRemoved = if (!groupRemoved && plugin.regionsConfig.hasRegion(regionName, world)) {
            plugin.regionsConfig.removeRegion(regionName, world)
            true
        } else {
            false
        }

        // Send comprehensive success message
        sender.sendMessage(plugin.configManager.getMessage("region-removed", "{region}", parsed.getCompositeKey()))
        val details = buildString {
            if (signsRemoved > 0) {
                append("\n<green>  ✓ $signsRemoved rental sign(s) removed")
            }

            if (schematicDeleted) {
                append("\n<green>  ✓ WorldEdit schematic deleted")
            }

            if (regionConfigRemoved) {
                append("\n<green>  ✓ Region configuration removed from regions.yml")
            }

            if (groupRemoved) {
                append("\n<green>  ✓ Removed from group '$groupName'")
            }

            if (rental != null) {
                append("\n<green>  ✓ Active rental reset with refund")
            }
        }

        if (details.isNotEmpty()) sender.sendMiniMessage(details.removePrefix("\n"))

        // Log the action
        plugin.logger.info("Admin ${sender.name} removed ZoneRental setup from region: ${parsed.getCompositeKey()}")
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (!sender.hasPermission("zonerental.admin.remove")) return emptyList()
        val registered = plugin.signsConfig.getRegisteredRegions()
        val options = when (args.size) {
            1 -> listOf("all") + registered
            2 -> if (args[0].equals("all", ignoreCase = true)) registered
                 else plugin.signsConfig.getSigns(args[0]).map { it.id.toString() }
            else -> emptyList()
        }
        return options.filter { it.startsWith(args.last(), ignoreCase = true) }.sorted()
    }
}
