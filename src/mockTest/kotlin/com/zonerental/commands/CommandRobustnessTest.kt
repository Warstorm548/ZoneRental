package com.zonerental.commands

import com.zonerental.testsupport.CommandTestSupport
import io.mockk.every
import io.mockk.spyk
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertTrue

/**
 * Regression 2.0.1: missing or malformed arguments crashed commands with
 * IndexOutOfBounds / NullPointer exceptions. Every executor must answer with a message instead.
 */
class CommandRobustnessTest : CommandTestSupport() {

    @BeforeEach
    fun setUp() {
        wireCommandDependencies()
        // Real storage so /zrretrieve answers "no stored items" instead of a silent stub
        every { plugin.storageConfig } returns com.zonerental.config.StorageConfig(plugin)
        every { plugin.storageManager } returns com.zonerental.managers.StorageManager(plugin)
    }

    private val argumentSets = listOf(
        emptyArray(),
        arrayOf(""),
        arrayOf("x"),
        arrayOf("add"),
        arrayOf("world:shop1"),
        arrayOf("nope:shop1"),
        arrayOf("add", "world:shop1"),
        arrayOf("remove", "world:shop1", "Nobody"),
        arrayOf("help", "999"),
    )

    private fun executors(): Map<String, CommandExecutor> = mapOf(
        "zr" to RRCommand(plugin),
        "reload" to ReloadCommand(plugin),
        "createsign" to CreateSignCommand(plugin),
        "reset" to ResetCommand(plugin),
        "retrieve" to RetrieveCommand(plugin),
        "info" to InfoCommand(plugin),
        "list" to ListCommand(plugin),
        "extend" to ExtendCommand(plugin),
        "member" to MemberCommand(plugin),
        "tp" to TpCommand(plugin),
        "duration" to DurationCommand(plugin),
        "remove" to RemoveCommand(plugin),
        "refundhistory" to RefundHistoryCommand(plugin),
        "verify" to VerifyCommand(plugin),
        "override" to OverrideCommand(plugin),
        "group" to GroupCommand(plugin),
    )

    @Test
    fun `every command handles bad input from the console`() {
        val console = server.consoleSender
        assertAll(executors().flatMap { (name, executor) ->
            argumentSets.map { args ->
                {
                    console.run(executor, *args)
                    assertTrue(messages(console).isNotEmpty(), "/$name ${args.joinToString(" ")} sent no reply to console")
                }
            }
        })
    }

    @Test
    fun `every command handles bad input from an op player`() {
        // MockBukkit 4.26 doesn't implement getTargetBlock (used by /zrcreatesign); stub it with a spy
        val player = spyk(server.addPlayer().apply { isOp = true })
        every { player.getTargetBlock(null, 5) } returns world.getBlockAt(0, 100, 0)
        assertAll(executors().flatMap { (name, executor) ->
            argumentSets.map { args ->
                {
                    player.run(executor, *args)
                    assertTrue(messages(player).isNotEmpty(), "/$name ${args.joinToString(" ")} sent no reply to player")
                }
            }
        })
    }

    @Test
    fun `tab completion never throws`() {
        val player = server.addPlayer().apply { isOp = true }
        assertAll(executors().values.filterIsInstance<TabCompleter>().flatMap { completer ->
            // Bukkit always passes at least one (possibly empty) argument when tab-completing
            (argumentSets.filter { it.isNotEmpty() } + listOf(arrayOf("edit", ""), arrayOf("price", ""), arrayOf("add", "shop1", ""))).map { args ->
                { completer.onTabComplete(player, command, "zr", args) ; Unit }
            }
        })
    }

    @Test
    fun `admin commands refuse players without permission`() {
        val player = server.addPlayer()
        val admin = listOf("reload", "createsign", "reset", "duration", "remove", "refundhistory", "verify", "override", "group")
        val executors = executors()
        assertAll(admin.map { name ->
            {
                player.run(executors.getValue(name), "world:shop1")
                val reply = messages(player).joinToString(" ").lowercase()
                assertTrue("permission" in reply, "/$name should deny a non-op player, got: $reply")
            }
        })
    }
}
