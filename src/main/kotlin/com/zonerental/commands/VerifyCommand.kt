package com.zonerental.commands

import com.zonerental.ZoneRental
import com.zonerental.extensions.sendMiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

/**
 * Command to verify region configuration integrity.
 * Shows regions using defaults and orphaned configurations.
 */
class VerifyCommand(private val plugin: ZoneRental) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission("zonerental.admin.verify")) {
            sender.sendMessage(plugin.configManager.getMessage("no-permission"))
            return true
        }

        if (!plugin.configManager.isEnableVerifyCommand) {
            sender.sendMiniMessage("<red>The verify command is disabled in config.yml")
            sender.sendMiniMessage("<gray>Enable it with: regions-config.enable-verify-command: true")
            return true
        }

        sender.sendMiniMessage("<gold><bold>=== Region Configuration Verification ===")
        sender.sendMiniMessage("")

        // Get verification report with safe casts
        val report = plugin.regionsConfig.getVerificationReport()
        val totalSigns = report["totalSigns"] as? Int ?: 0
        val missingConfigs = (report["missingConfigs"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val orphanedConfigs = (report["orphanedConfigs"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        val usingDefaults = missingConfigs.size
        val withCustomSettings = totalSigns - usingDefaults

        // Display summary
        sender.sendMiniMessage("<aqua>Summary:")
        sender.sendMiniMessage("<gray>  Total rental spaces:      <white>$totalSigns")
        sender.sendMiniMessage("<gray>  Total rental signs:       <white>${plugin.signsConfig.getAllSigns().size}")
        sender.sendMiniMessage("<gray>  Using custom overrides:   <white>$withCustomSettings")
        sender.sendMiniMessage("<gray>  Using default settings:   <white>$usingDefaults")
        sender.sendMiniMessage("")

        // Display regions using defaults
        if (missingConfigs.isNotEmpty()) {
            sender.sendMiniMessage("<green>Regions Using Defaults (${missingConfigs.size}):")
            sender.sendMiniMessage("<gray>  These regions use default values from config.yml:")
            missingConfigs.forEach { region ->
                sender.sendMiniMessage("<gray>    - <white>$region")
            }
            sender.sendMiniMessage("<yellow>  Use /zroverride to set custom values for these regions")
            sender.sendMiniMessage("")
        } else {
            sender.sendMiniMessage("<yellow>All rental spaces have custom overrides configured!")
            sender.sendMiniMessage("")
        }

        // Display orphaned configs (potential issue)
        if (orphanedConfigs.isNotEmpty()) {
            sender.sendMiniMessage("<gold>Orphaned Configurations (${orphanedConfigs.size}):")
            sender.sendMiniMessage("<gray>  These custom configs exist but have no rental space:")
            orphanedConfigs.forEach { region ->
                sender.sendMiniMessage("<gray>    - <white>$region")
            }
            sender.sendMiniMessage("<yellow>  Use /zroverride remove <region> to clean up unused configs")
            sender.sendMiniMessage("")
        }

        val spaceProblems = showRentalSpaceProblems(sender)

        // Summary message
        if (orphanedConfigs.isEmpty() && spaceProblems == 0) {
            sender.sendMiniMessage("<green>✓ No issues found - all configs are linked to rental spaces")
        } else {
            if (orphanedConfigs.isNotEmpty()) {
                sender.sendMiniMessage("<yellow>⚠ Found ${orphanedConfigs.size} orphaned config(s) - consider cleanup")
            }
            if (spaceProblems > 0) {
                sender.sendMiniMessage("<yellow>⚠ Found $spaceProblems rental space problem(s) - see above")
            }
        }

        sender.sendMiniMessage("")
        sender.sendMiniMessage("<gold><bold>=================================")

        return true
    }

    /**
     * Lists rental spaces that can't work as configured: missing world, missing WorldGuard
     * region, or no signs (nobody can rent it).
     *
     * @return number of problems listed
     */
    private fun showRentalSpaceProblems(sender: CommandSender): Int {
        val missingWorld = mutableListOf<String>()
        val missingRegion = mutableListOf<String>()
        val noSigns = mutableListOf<String>()

        for (key in plugin.signsConfig.getRegisteredRegions().sorted()) {
            val world = plugin.server.getWorld(key.substringBefore(":"))
            when {
                world == null -> missingWorld += key
                !plugin.worldGuardManager.regionExists(key.substringAfter(":"), world) -> missingRegion += key
                plugin.signsConfig.getSigns(key).isEmpty() -> noSigns += key
            }
        }

        val prefix = plugin.activePrefix
        showList(sender, "World not loaded or deleted", missingWorld,
            "Load the world and run /${prefix}remove all <world:region>, or delete the entry from signs.yml while the server is stopped")
        showList(sender, "WorldGuard region missing", missingRegion, "Use /${prefix}remove all <world:region> to clean up")
        showList(sender, "Rental spaces without signs (can't be rented)", noSigns, "Use /${prefix}createsign <world:region> to add one")
        return missingWorld.size + missingRegion.size + noSigns.size
    }

    private fun showList(sender: CommandSender, title: String, entries: List<String>, hint: String) {
        if (entries.isEmpty()) return
        sender.sendMiniMessage("<gold>$title (${entries.size}):")
        entries.forEach { sender.sendMiniMessage("<gray>    - <white>$it") }
        sender.sendMiniMessage("<yellow>  $hint")
        sender.sendMiniMessage("")
    }
}
