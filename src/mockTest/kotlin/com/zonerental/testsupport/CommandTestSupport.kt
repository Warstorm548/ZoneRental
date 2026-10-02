package com.zonerental.testsupport

import com.zonerental.config.GroupsConfig
import com.zonerental.config.RegionsConfig
import com.zonerental.managers.RentalManager
import com.zonerental.managers.SignManager
import com.zonerental.managers.WorldGuardManager
import io.mockk.every
import io.mockk.mockk
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.mockbukkit.mockbukkit.command.MessageTarget

/**
 * Shared wiring for command tests: real config/groups/regions, mocked managers
 * with realistic "nothing rented" defaults.
 */
abstract class CommandTestSupport : MockPluginTest() {

    protected lateinit var groups: GroupsConfig
    protected lateinit var regions: RegionsConfig
    protected lateinit var rentalManager: RentalManager
    protected lateinit var worldGuard: WorldGuardManager
    protected lateinit var signManager: SignManager
    protected lateinit var signs: com.zonerental.config.SignsConfig
    protected val command: Command = mockk(relaxed = true)

    protected fun wireCommandDependencies() {
        useRealConfigManager()
        groups = GroupsConfig(plugin)
        every { plugin.groupsConfig } returns groups
        regions = RegionsConfig(plugin)
        every { plugin.regionsConfig } returns regions

        rentalManager = mockk(relaxed = true)
        every { rentalManager.getRental(any(), any()) } returns null
        every { rentalManager.getPlayerRentals(any()) } returns emptyList()
        every { rentalManager.getRentalsWhereMember(any()) } returns emptyList()
        every { rentalManager.allRentals } returns emptyList()
        every { rentalManager.resetRentalWithRefund(any(), any()) } returns null
        every { plugin.rentalManager } returns rentalManager

        worldGuard = mockk(relaxed = true)
        every { worldGuard.regionExists(any(), any()) } returns true
        every { worldGuard.allRegionNames } returns setOf("shop1", "shop2")
        every { plugin.worldGuardManager } returns worldGuard

        signManager = mockk(relaxed = true)
        every { signManager.removeRegionSetup(any(), any()) } returns -1
        every { plugin.signManager } returns signManager

        signs = com.zonerental.config.SignsConfig(plugin)
        every { plugin.signsConfig } returns signs
        every { plugin.worldEditManager.hasCapture(any(), any()) } returns false
    }

    /** Drains and returns every chat message the sender has received so far (plain text). */
    protected fun messages(sender: CommandSender): List<String> {
        val target = sender as MessageTarget
        return generateSequence { target.nextMessage() }.toList()
    }

    protected fun CommandSender.run(executor: org.bukkit.command.CommandExecutor, vararg args: String): Boolean =
        executor.onCommand(this, command, "zr", arrayOf(*args))
}
