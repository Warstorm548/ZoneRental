package com.zonerental.commands

import com.zonerental.ZoneRental
import com.zonerental.extensions.sendMiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class RRCommand(private val plugin: ZoneRental) : CommandExecutor, TabCompleter {

    private val prefix: String
        get() = plugin.activePrefix

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        // Handle help command with various options
        if (args.isEmpty()) {
            showHelp(sender, 1)
            return true
        }

        if (args[0].equals("help", ignoreCase = true)) {
            if (args.size == 1) {
                // /rr help - show page 1
                showHelp(sender, 1)
                return true
            }

            // Check if argument is a page number
            val pageNum = args[1].toIntOrNull()
            if (pageNum != null) {
                if (pageNum in 1..3) {
                    showHelp(sender, pageNum)
                } else {
                    sender.sendMiniMessage("<red>Invalid page number. Valid pages: 1-3")
                }
                return true
            }

            // Not a number, treat as command name for detailed help
            showDetailedHelp(sender, args[1].lowercase())
            return true
        }

        // Delegate to specific commands based on first argument
        when (args[0].lowercase()) {
            "reload" -> {
                if (!sender.hasPermission("zonerental.admin.reload")) {
                    sender.sendMessage(plugin.configManager.getMessage("no-permission"))
                    return true
                }
                plugin.reloadPlugin()
                sender.sendMessage(plugin.configManager.getMessage("config-reloaded"))
            }

            "list" -> {
                if (args.size == 1) {
                    sender.sendMiniMessage("<red>Usage: /$prefix list <player>")
                    return true
                }
                // Handle list command
            }

            "info" -> {
                if (args.size < 2) {
                    sender.sendMiniMessage("<red>Usage: /$prefix info <region>")
                    return true
                }
                // Handle info command
            }

            else -> {
                sender.sendMiniMessage("<red>Unknown command. Use /$prefix help for help.")
            }
        }

        return true
    }

    private fun showHelp(sender: CommandSender, page: Int) {
        sender.sendMiniMessage("<gold>=== ZoneRental Help ===")
        sender.sendMiniMessage("")

        when (page) {
            1 -> {
                // Page 1: User Commands
                sender.sendMiniMessage("<gold>Player Commands:")
                sender.sendMiniMessage("<yellow>/$prefix help<gray> - Show this help menu")
                sender.sendMiniMessage("<yellow>/${prefix}info<gray> - View rental information for a region")
                sender.sendMiniMessage("<yellow>/${prefix}list<gray> - List your active rentals")
                sender.sendMiniMessage("<yellow>/${prefix}extend<gray> - Extend your rental duration")
                sender.sendMiniMessage("<yellow>/${prefix}retrieve<gray> - Retrieve items from expired rentals")
            }

            2 -> {
                if (!sender.hasPermission("zonerental.admin")) {
                    sender.sendMiniMessage("<red>You don't have permission to view admin commands.")
                    sender.sendMiniMessage("<gray>Use <yellow>/$prefix help 1<gray> to view player commands.")
                    return
                }
                // Page 2: Admin Commands Part 1
                sender.sendMiniMessage("<gold>Admin Commands (Part 1):")
                sender.sendMiniMessage("<yellow>/${prefix}reload<gray> - Reload plugin configuration")
                sender.sendMiniMessage("<yellow>/${prefix}create<gray> - Register a region as a rental space")
                sender.sendMiniMessage("<yellow>/${prefix}createsign<gray> - Add a rental sign to a region")
                sender.sendMiniMessage("<yellow>/${prefix}reset<gray> - Reset a rental with full refund")
                sender.sendMiniMessage("<yellow>/${prefix}remove<gray> - Remove a rental sign, or all setup from a region")
            }

            3 -> {
                if (!sender.hasPermission("zonerental.admin")) {
                    sender.sendMiniMessage("<red>You don't have permission to view admin commands.")
                    sender.sendMiniMessage("<gray>Use <yellow>/$prefix help 1<gray> to view player commands.")
                    return
                }
                // Page 3: Admin Commands Part 2
                sender.sendMiniMessage("<gold>Admin Commands (Part 2):")
                sender.sendMiniMessage("<yellow>/${prefix}duration<gray> - Modify rental duration (add/remove/set/reset)")
                sender.sendMiniMessage("<yellow>/${prefix}override<gray> - Set per-region custom settings")
                sender.sendMiniMessage("<yellow>/${prefix}verify<gray> - Verify region configurations")
                sender.sendMiniMessage("<yellow>/${prefix}refundhistory<gray> - View refund transaction history")
            }
        }

        // Footer with navigation instructions
        sender.sendMiniMessage("")
        sender.sendMiniMessage("<dark_aqua>Page $page/3")
        sender.sendMiniMessage("<aqua>Usage: <white>/$prefix help [page]<gray> - View help page")
        sender.sendMiniMessage("<aqua>Usage: <white>/$prefix help <command><gray> - Detailed command help")
        sender.sendMiniMessage("<gold>========================")
    }

    private fun showDetailedHelp(sender: CommandSender, commandName: String) {
        sender.sendMiniMessage("<gold>=== Detailed Command Help ===")
        sender.sendMiniMessage("")

        when (commandName) {
            "help" -> {
                sender.sendMiniMessage("<yellow>Command: <white>/$prefix help")
                sender.sendMiniMessage("<gray>Display help information for ZoneRental commands.")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Syntax:")
                sender.sendMiniMessage("<white>  /$prefix help [page]")
                sender.sendMiniMessage("<white>  /$prefix help <command>")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Permission: <white>zonerental.user")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Examples:")
                sender.sendMiniMessage("<green>  /$prefix help<gray> - Show page 1 of help")
                sender.sendMiniMessage("<green>  /$prefix help 2<gray> - Show page 2 of help")
                sender.sendMiniMessage("<green>  /$prefix help info<gray> - Show detailed help for /${prefix}info command")
            }

            "info" -> {
                sender.sendMiniMessage("<yellow>Command: <white>/${prefix}info")
                sender.sendMiniMessage("<gray>View detailed information about a rental region including price, owner, and expiration time.")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Syntax:")
                sender.sendMiniMessage("<white>  /${prefix}info <region>")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Permission: <white>zonerental.info")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Examples:")
                sender.sendMiniMessage("<green>  /${prefix}info shop1<gray> - View info for region 'shop1'")
                sender.sendMiniMessage("<green>  /${prefix}info apartment_5<gray> - Check if apartment_5 is available")
            }

            "list" -> {
                sender.sendMiniMessage("<yellow>Command: <white>/${prefix}list")
                sender.sendMiniMessage("<gray>List all your active rentals or view another player's rentals (admin only).")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Syntax:")
                sender.sendMiniMessage("<white>  /${prefix}list [player]")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Permissions:")
                sender.sendMiniMessage("<white>  zonerental.list <gray>- View your own rentals")
                sender.sendMiniMessage("<white>  zonerental.admin.list.others <gray>- View other players' rentals")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Examples:")
                sender.sendMiniMessage("<green>  /${prefix}list<gray> - List all your rentals")
                sender.sendMiniMessage("<green>  /${prefix}list Notch<gray> - List Notch's rentals (admin only)")
            }

            "extend" -> {
                sender.sendMiniMessage("<yellow>Command: <white>/${prefix}extend")
                sender.sendMiniMessage("<gray>Extend the duration of your rental. Costs money and has a maximum extension limit.")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Syntax:")
                sender.sendMiniMessage("<white>  /${prefix}extend <region>")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Permission: <white>zonerental.extend")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Examples:")
                sender.sendMiniMessage("<green>  /${prefix}extend shop1<gray> - Extend your rental for shop1")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Notes:")
                sender.sendMiniMessage("<gray>  - You can only extend rentals you own")
                sender.sendMiniMessage("<gray>  - Extensions have a cost (configurable per region)")
                sender.sendMiniMessage("<gray>  - Maximum extension limit applies (check config)")
            }

            "retrieve" -> {
                sender.sendMiniMessage("<yellow>Command: <white>/${prefix}retrieve")
                sender.sendMiniMessage("<gray>Open a GUI to retrieve items stored from your expired rentals.")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Syntax:")
                sender.sendMiniMessage("<white>  /${prefix}retrieve")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Permission: <white>zonerental.retrieve")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Examples:")
                sender.sendMiniMessage("<green>  /${prefix}retrieve<gray> - Open item retrieval GUI")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Notes:")
                sender.sendMiniMessage("<gray>  - Items are stored when your rental expires")
                sender.sendMiniMessage("<gray>  - Only shows items from your expired rentals")
            }

            "reload" -> {
                if (!sender.hasPermission("zonerental.admin.reload")) {
                    sender.sendMiniMessage("<red>You don't have permission to view this command's help.")
                    return
                }
                sender.sendMiniMessage("<yellow>Command: <white>/${prefix}reload")
                sender.sendMiniMessage("<gray>Reload all plugin configuration files without restarting the server.")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Syntax:")
                sender.sendMiniMessage("<white>  /${prefix}reload")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Permission: <white>zonerental.admin.reload")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Examples:")
                sender.sendMiniMessage("<green>  /${prefix}reload<gray> - Reload configuration")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Notes:")
                sender.sendMiniMessage("<gray>  - Reloads config.yml, regions.yml, signs.yml, and storage.yml")
                sender.sendMiniMessage("<gray>  - Active rentals are not affected")
            }

            "create" -> {
                if (!sender.hasPermission("zonerental.admin.create")) {
                    sender.sendMiniMessage("<red>You don't have permission to view this command's help.")
                    return
                }
                sender.sendMiniMessage("<yellow>Command: <white>/${prefix}create")
                sender.sendMiniMessage("<gray>Register a WorldGuard region as a rental space without placing a sign.")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Syntax:")
                sender.sendMiniMessage("<white>  /${prefix}create <world:region>")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Permission: <white>zonerental.admin.create")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Examples:")
                sender.sendMiniMessage("<green>  /${prefix}create world:shop1<gray> - Register shop1 as a rental space")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Notes:")
                sender.sendMiniMessage("<gray>  - Players rent through signs: add one with /${prefix}createsign")
                sender.sendMiniMessage("<gray>  - /${prefix}createsign registers the region automatically if needed")
            }

            "createsign" -> {
                if (!sender.hasPermission("zonerental.admin.createsign")) {
                    sender.sendMiniMessage("<red>You don't have permission to view this command's help.")
                    return
                }
                sender.sendMiniMessage("<yellow>Command: <white>/${prefix}createsign")
                sender.sendMiniMessage("<gray>Turn the sign you're looking at into a rental sign for a region.")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Syntax:")
                sender.sendMiniMessage("<white>  /${prefix}createsign <region>")
                sender.sendMiniMessage("<white>  /${prefix}createsign <world:region>")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Permission: <white>zonerental.admin.createsign")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Examples:")
                sender.sendMiniMessage("<green>  /${prefix}createsign shop1<gray> - Sign for shop1 in your world")
                sender.sendMiniMessage("<green>  /${prefix}createsign world:shop1<gray> - Sign here for shop1 in 'world'")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Notes:")
                sender.sendMiniMessage("<gray>  - Look at an existing sign block before running")
                sender.sendMiniMessage("<gray>  - A region can have several signs (signs.max-per-region), in any world")
                sender.sendMiniMessage("<gray>  - Each sign gets an ID (#1, #2...) shown in signs.yml")
                sender.sendMiniMessage("<gray>  - Sign will use default settings until overrides are set")
                sender.sendMiniMessage("<gray>  - Support block is automatically protected")
            }

            "reset" -> {
                if (!sender.hasPermission("zonerental.admin.reset")) {
                    sender.sendMiniMessage("<red>You don't have permission to view this command's help.")
                    return
                }
                sender.sendMiniMessage("<yellow>Command: <white>/${prefix}reset")
                sender.sendMiniMessage("<gray>Reset an active rental with full refund. Keeps rental setup intact.")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Syntax:")
                sender.sendMiniMessage("<white>  /${prefix}reset <region>")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Permission: <white>zonerental.admin.reset")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Examples:")
                sender.sendMiniMessage("<green>  /${prefix}reset shop1<gray> - Reset shop1 rental")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Notes:")
                sender.sendMiniMessage("<gray>  - Player receives 100% refund (initial + all extensions)")
                sender.sendMiniMessage("<gray>  - Player is removed from WorldGuard region")
                sender.sendMiniMessage("<gray>  - Region blocks are restored to original state")
                sender.sendMiniMessage("<gray>  - Sign, schematic, and configs remain (use /${prefix}remove to delete)")
            }

            "remove" -> {
                if (!sender.hasPermission("zonerental.admin.remove")) {
                    sender.sendMiniMessage("<red>You don't have permission to view this command's help.")
                    return
                }
                sender.sendMiniMessage("<yellow>Command: <white>/${prefix}remove")
                sender.sendMiniMessage("<gray>Remove one rental sign, or all ZoneRental setup from a region.")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Syntax:")
                sender.sendMiniMessage("<white>  /${prefix}remove<gray> (looking at a rental sign)")
                sender.sendMiniMessage("<white>  /${prefix}remove <world:region> <id>")
                sender.sendMiniMessage("<white>  /${prefix}remove all <world:region>")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Permission: <white>zonerental.admin.remove")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Examples:")
                sender.sendMiniMessage("<green>  /${prefix}remove world:shop1 2<gray> - Remove sign #2 of shop1")
                sender.sendMiniMessage("<green>  /${prefix}remove all world:shop1<gray> - Remove all setup from shop1")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Notes:")
                sender.sendMiniMessage("<gray>  - Removing a sign keeps the rental space and any rental")
                sender.sendMiniMessage("<gray>  - 'all' resets an active rental with full refund")
                sender.sendMiniMessage("<gray>  - 'all' removes every sign and restores support blocks")
                sender.sendMiniMessage("<gray>  - 'all' deletes the WorldEdit schematic and regions.yml entry")
                sender.sendMiniMessage("<gray>  - Use 'all' to completely repurpose a region")
            }

            "duration" -> {
                if (!sender.hasPermission("zonerental.admin.duration")) {
                    sender.sendMiniMessage("<red>You don't have permission to view this command's help.")
                    return
                }
                sender.sendMiniMessage("<yellow>Command: <white>/${prefix}duration")
                sender.sendMiniMessage("<gray>Modify the duration of an active rental.")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Syntax:")
                sender.sendMiniMessage("<white>  /${prefix}duration add <region> <time> [--charge]")
                sender.sendMiniMessage("<white>  /${prefix}duration remove <region> <time>")
                sender.sendMiniMessage("<white>  /${prefix}duration set <region> <time>")
                sender.sendMiniMessage("<white>  /${prefix}duration reset <region>")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Permission: <white>zonerental.admin.duration")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Examples:")
                sender.sendMiniMessage("<green>  /${prefix}duration add shop1 7d<gray> - Add 7 days (free)")
                sender.sendMiniMessage("<green>  /${prefix}duration add shop1 3d --charge<gray> - Add 3 days (charge player)")
                sender.sendMiniMessage("<green>  /${prefix}duration remove shop1 2d<gray> - Remove 2 days (auto-refund)")
                sender.sendMiniMessage("<green>  /${prefix}duration set shop1 14d<gray> - Set to exactly 14 days remaining")
                sender.sendMiniMessage("<green>  /${prefix}duration reset shop1<gray> - Reset to default duration")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Time Format:")
                sender.sendMiniMessage("<gray>  Use 'd' for days, 'h' for hours, 'm' for minutes")
                sender.sendMiniMessage("<gray>  Examples: 7d, 12h, 30m, 1d12h")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Notes:")
                sender.sendMiniMessage("<gray>  - 'reset' refunds extension costs if configured")
                sender.sendMiniMessage("<gray>  - 'remove' provides automatic refund if configured")
                sender.sendMiniMessage("<gray>  - '--charge' flag charges player for added time")
            }

            "override" -> {
                if (!sender.hasPermission("zonerental.admin.override")) {
                    sender.sendMiniMessage("<red>You don't have permission to view this command's help.")
                    return
                }
                sender.sendMiniMessage("<yellow>Command: <white>/${prefix}override")
                sender.sendMiniMessage("<gray>Set per-region custom settings (price, duration, extensions, etc.)")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Syntax:")
                sender.sendMiniMessage("<white>  /${prefix}override price <region> <amount>")
                sender.sendMiniMessage("<white>  /${prefix}override duration <region> <days>")
                sender.sendMiniMessage("<white>  /${prefix}override maxextensions <region> <count>")
                sender.sendMiniMessage("<white>  /${prefix}override extensionprice <region> <amount>")
                sender.sendMiniMessage("<white>  /${prefix}override allowextensions <region> <true|false>")
                sender.sendMiniMessage("<white>  /${prefix}override extensionduration <region> <days>")
                sender.sendMiniMessage("<white>  /${prefix}override remove <region>")
                sender.sendMiniMessage("<white>  /${prefix}override list [region]")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Permission: <white>zonerental.admin.override")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Examples:")
                sender.sendMiniMessage("<green>  /${prefix}override price shop1 500<gray> - Set custom price")
                sender.sendMiniMessage("<green>  /${prefix}override duration shop1 14<gray> - Set custom duration")
                sender.sendMiniMessage("<green>  /${prefix}override maxextensions shop1 20<gray> - Set max extensions")
                sender.sendMiniMessage("<green>  /${prefix}override extensionprice shop1 0<gray> - Auto-calculate price")
                sender.sendMiniMessage("<green>  /${prefix}override allowextensions shop1 false<gray> - Disable extensions")
                sender.sendMiniMessage("<green>  /${prefix}override remove shop1<gray> - Remove all overrides")
                sender.sendMiniMessage("<green>  /${prefix}override list shop1<gray> - View shop1 settings")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Notes:")
                sender.sendMiniMessage("<gray>  - Overrides saved to regions.yml")
                sender.sendMiniMessage("<gray>  - Signs without overrides use config.yml defaults")
                sender.sendMiniMessage("<gray>  - Sign updates automatically when override is set")
            }

            "verify" -> {
                if (!sender.hasPermission("zonerental.admin.verify")) {
                    sender.sendMiniMessage("<red>You don't have permission to view this command's help.")
                    return
                }
                sender.sendMiniMessage("<yellow>Command: <white>/${prefix}verify")
                sender.sendMiniMessage("<gray>Verify region configurations and check for orphaned configs.")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Syntax:")
                sender.sendMiniMessage("<white>  /${prefix}verify")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Permission: <white>zonerental.admin.verify")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Examples:")
                sender.sendMiniMessage("<green>  /${prefix}verify<gray> - Run configuration verification")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Notes:")
                sender.sendMiniMessage("<gray>  - Reports regions using default vs custom settings")
                sender.sendMiniMessage("<gray>  - Identifies orphaned configs (no sign exists)")
                sender.sendMiniMessage("<gray>  - Helps maintain clean configuration")
            }

            "refundhistory" -> {
                if (!sender.hasPermission("zonerental.admin.refundhistory")) {
                    sender.sendMiniMessage("<red>You don't have permission to view this command's help.")
                    return
                }
                sender.sendMiniMessage("<yellow>Command: <white>/${prefix}refundhistory")
                sender.sendMiniMessage("<gray>View all refund transactions for a rental.")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Syntax:")
                sender.sendMiniMessage("<white>  /${prefix}refundhistory <region>")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Permission: <white>zonerental.admin.refundhistory")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Examples:")
                sender.sendMiniMessage("<green>  /${prefix}refundhistory shop1<gray> - View refund history for shop1")
                sender.sendMiniMessage("")
                sender.sendMiniMessage("<aqua>Notes:")
                sender.sendMiniMessage("<gray>  - Shows refund amounts, timestamps, and reasons")
                sender.sendMiniMessage("<gray>  - Useful for tracking admin actions")
            }

            else -> {
                sender.sendMiniMessage("<red>Unknown command: $commandName")
                sender.sendMiniMessage("<gray>Use <yellow>/$prefix help<gray> to see available commands.")
                return
            }
        }

        sender.sendMiniMessage("")
        sender.sendMiniMessage("<gold>========================")
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.size == 1) {
            val subCommands = mutableListOf("help", "info", "list")

            if (sender.hasPermission("zonerental.admin")) {
                subCommands.add("reload")
            }

            val partial = args[0].lowercase()
            subCommands.filterTo(completions) { it.startsWith(partial) }
        }

        return completions
    }
}
