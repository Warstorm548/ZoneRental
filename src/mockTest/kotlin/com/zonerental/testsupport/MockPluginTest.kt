package com.zonerental.testsupport

import com.zonerental.ZoneRental
import com.zonerental.config.ConfigManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import java.io.File
import java.util.logging.Logger

/**
 * Base class for local-only mock tests.
 *
 * Starts a MockBukkit server with worlds "world" and "world_nether", and provides a MockK
 * [ZoneRental] (relaxed) whose data folder is a temp directory. The real plugin is never
 * enabled because onEnable() requires Vault, WorldGuard and WorldEdit.
 */
@ExtendWith(FailOnUnimplementedExtension::class)
abstract class MockPluginTest {

    protected lateinit var server: ServerMock
    protected lateinit var world: WorldMock
    protected lateinit var nether: WorldMock
    protected lateinit var plugin: ZoneRental
    protected lateinit var bukkitConfig: YamlConfiguration

    @TempDir
    protected lateinit var dataFolder: File

    @BeforeEach
    fun setUpMockServer() {
        server = MockBukkit.mock()
        world = server.addSimpleWorld("world")
        nether = server.addSimpleWorld("world_nether")

        bukkitConfig = YamlConfiguration.loadConfiguration(File("src/main/resources/config.yml"))

        plugin = mockk(relaxed = true)
        every { plugin.dataFolder } returns dataFolder
        every { plugin.logger } returns Logger.getLogger("ZoneRentalTest")
        every { plugin.server } returns server
        every { plugin.config } returns bukkitConfig
        every { plugin.name } returns "ZoneRental"
        every { plugin.isEnabled } returns true
        every { plugin.activePrefix } returns "zr"
    }

    @AfterEach
    fun tearDownMockServer() {
        MockBukkit.unmock()
        unmockkAll()
    }

    /** Creates a real ConfigManager from the bundled config.yml and wires it into the plugin mock. */
    protected fun useRealConfigManager(): ConfigManager {
        val configManager = ConfigManager(plugin)
        every { plugin.configManager } returns configManager
        return configManager
    }
}
