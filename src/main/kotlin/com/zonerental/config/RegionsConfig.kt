package com.zonerental.config

import com.zonerental.ZoneRental
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.util.logging.Level

class RegionsConfig(private val plugin: ZoneRental) {

    private val configFile: File = File(plugin.dataFolder, "regions.yml")
    private var config: YamlConfiguration

    @Volatile
    var isDirty: Boolean = false
        private set

    init {
        createConfig()
        config = YamlConfiguration.loadConfiguration(configFile)
        migrateToWorldAwareKeys()
    }

    private fun createConfig() {
        if (!configFile.exists()) {
            configFile.parentFile?.mkdirs()
            try {
                configFile.createNewFile()

                val newConfig = YamlConfiguration.loadConfiguration(configFile)
                newConfig.options().setHeader(listOf(
                    "ZoneRental - Per-Region Configuration",
                    "Use /zroverride commands to set custom values for specific regions.",
                    "Regions not listed here will use default settings from config.yml",
                    "",
                    "Format: world:region (e.g., world:shop1, world_nether:shop2)",
                    "",
                    "Commands:",
                    "  /zroverride price <region> <amount>",
                    "  /zroverride duration <region> <days>",
                    "  /zroverride maxextensions <region> <count>",
                    "  /zroverride extensionprice <region> <amount>",
                    "  /zroverride allowextensions <region> <true|false>",
                    "  /zroverride extensionduration <region> <days>",
                    "  /zroverride remove <region>              # Remove all overrides",
                    "  /zroverride list [region]                # View overrides"
                ))
                newConfig.createSection("regions")
                newConfig.save(configFile)

                plugin.logger.info("Created regions.yml file")
            } catch (e: IOException) {
                plugin.logger.log(Level.SEVERE, "Could not create regions.yml!", e)
            }
        }
    }

    private fun migrateToWorldAwareKeys() {
        val regionsSection = config.getConfigurationSection("regions") ?: return

        val defaultWorld = plugin.server.worlds.firstOrNull()?.name ?: return
        val migrations = mutableMapOf<String, Map<String, Any?>>()
        var migratedCount = 0

        for (key in regionsSection.getKeys(false)) {
            if (!key.contains(":")) {
                val newKey = "$defaultWorld:$key"
                val oldSection = config.getConfigurationSection("regions.$key")

                oldSection?.let { section ->
                    val data = mutableMapOf<String, Any?>()
                    for (subKey in section.getKeys(true)) {
                        data[subKey] = section.get(subKey)
                    }
                    migrations[newKey] = data
                }
                migratedCount++
            }
        }

        if (migrations.isNotEmpty()) {
            for ((compositeKey, data) in migrations) {
                for ((subKey, value) in data) {
                    config.set("regions.$compositeKey.$subKey", value)
                }
            }

            for (key in regionsSection.getKeys(false).toSet()) {
                if (!key.contains(":")) {
                    config.set("regions.$key", null)
                }
            }

            save()
            plugin.logger.info("Migrated $migratedCount region override(s) to world-aware format (defaulted to world: $defaultWorld)")
        }
    }

    private fun markDirty() {
        isDirty = true
    }

    fun saveIfDirty() {
        if (isDirty) save()
    }

    fun save() {
        try {
            config.save(configFile)
        } catch (e: IOException) {
            plugin.logger.log(Level.SEVERE, "Could not save regions.yml!", e)
            return
        }
        isDirty = false
    }

    fun reload() {
        config = YamlConfiguration.loadConfiguration(configFile)
        isDirty = false
    }

    // ========== Region Methods ==========

    fun removeRegion(region: String, world: World) {
        val compositeKey = "${world.name}:$region"
        if (!hasRegion(region, world)) return

        config.set("regions.$compositeKey", null)
        markDirty()

        if (plugin.configManager?.isDebug == true) {
            plugin.logger.info("Removed region '$compositeKey' from regions.yml")
        }
    }

    fun hasRegion(region: String, world: World): Boolean {
        val compositeKey = "${world.name}:$region"
        return config.contains("regions.$compositeKey")
    }

    /**
     * Get region price or fallback to default
     * Lookup order: group override → region override → default
     */
    fun getRegionPrice(region: String, world: World, defaultPrice: Double): Double {
        val compositeKey = "${world.name}:$region"

        // Check if region is in a group and get group override
        plugin.groupsConfig?.let { groupsConfig ->
            if (groupsConfig.isRegionInGroup(compositeKey)) {
                val groupName = groupsConfig.getRegionGroup(compositeKey)
                if (groupName != null && config.contains("groups.$groupName.price")) {
                    return getGroupPrice(groupName, defaultPrice)
                }
            }
        }

        // Check region-specific override
        val path = "regions.$compositeKey.price"
        if (config.contains(path)) {
            return config.getDouble(path, defaultPrice)
        }

        return defaultPrice
    }

    fun getRegionDuration(region: String, world: World, defaultDuration: Int): Int {
        val compositeKey = "${world.name}:$region"

        plugin.groupsConfig?.let { groupsConfig ->
            if (groupsConfig.isRegionInGroup(compositeKey)) {
                val groupName = groupsConfig.getRegionGroup(compositeKey)
                if (groupName != null && config.contains("groups.$groupName.duration")) {
                    return getGroupDuration(groupName, defaultDuration)
                }
            }
        }

        val path = "regions.$compositeKey.duration"
        if (config.contains(path)) {
            return config.getInt(path, defaultDuration)
        }

        return defaultDuration
    }

    fun getRegionMaxExtensions(region: String, world: World, defaultMax: Int): Int {
        val compositeKey = "${world.name}:$region"

        plugin.groupsConfig?.let { groupsConfig ->
            if (groupsConfig.isRegionInGroup(compositeKey)) {
                val groupName = groupsConfig.getRegionGroup(compositeKey)
                if (groupName != null && config.contains("groups.$groupName.max-extensions")) {
                    return getGroupMaxExtensions(groupName, defaultMax)
                }
            }
        }

        val path = "regions.$compositeKey.max-extensions"
        if (config.contains(path)) {
            return config.getInt(path, defaultMax)
        }

        return defaultMax
    }

    fun getRegionExtensionPrice(region: String, world: World, defaultPrice: Double): Double {
        val compositeKey = "${world.name}:$region"

        plugin.groupsConfig?.let { groupsConfig ->
            if (groupsConfig.isRegionInGroup(compositeKey)) {
                val groupName = groupsConfig.getRegionGroup(compositeKey)
                if (groupName != null && config.contains("groups.$groupName.extension-price")) {
                    val price = getGroupExtensionPrice(groupName, defaultPrice)
                    return if (price > 0) price else defaultPrice
                }
            }
        }

        val path = "regions.$compositeKey.extension-price"
        if (config.contains(path)) {
            val price = config.getDouble(path, defaultPrice)
            return if (price > 0) price else defaultPrice
        }

        return defaultPrice
    }

    fun getRegionAllowExtensions(region: String, world: World, defaultAllow: Boolean): Boolean {
        val compositeKey = "${world.name}:$region"

        plugin.groupsConfig?.let { groupsConfig ->
            if (groupsConfig.isRegionInGroup(compositeKey)) {
                val groupName = groupsConfig.getRegionGroup(compositeKey)
                if (groupName != null && config.contains("groups.$groupName.allow-extensions")) {
                    return getGroupAllowExtensions(groupName, defaultAllow)
                }
            }
        }

        val path = "regions.$compositeKey.allow-extensions"
        if (config.contains(path)) {
            return config.getBoolean(path, defaultAllow)
        }

        return defaultAllow
    }

    fun getRegionExtensionDuration(region: String, world: World, defaultDuration: Int): Int {
        val compositeKey = "${world.name}:$region"

        plugin.groupsConfig?.let { groupsConfig ->
            if (groupsConfig.isRegionInGroup(compositeKey)) {
                val groupName = groupsConfig.getRegionGroup(compositeKey)
                if (groupName != null && config.contains("groups.$groupName.extension-duration")) {
                    return getGroupExtensionDuration(groupName, defaultDuration)
                }
            }
        }

        val path = "regions.$compositeKey.extension-duration"
        if (config.contains(path)) {
            return config.getInt(path, defaultDuration)
        }

        return defaultDuration
    }

    // Region setters
    fun setRegionPrice(region: String, world: World, price: Double) {
        config.set("regions.${world.name}:$region.price", price)
        markDirty()
    }

    fun setRegionDuration(region: String, world: World, days: Int) {
        config.set("regions.${world.name}:$region.duration", days)
        markDirty()
    }

    fun setRegionMaxExtensions(region: String, world: World, maxExtensions: Int) {
        config.set("regions.${world.name}:$region.max-extensions", maxExtensions)
        markDirty()
    }

    fun setRegionExtensionPrice(region: String, world: World, price: Double) {
        config.set("regions.${world.name}:$region.extension-price", price)
        markDirty()
    }

    fun setRegionAllowExtensions(region: String, world: World, allow: Boolean) {
        config.set("regions.${world.name}:$region.allow-extensions", allow)
        markDirty()
    }

    fun setRegionExtensionDuration(region: String, world: World, days: Int) {
        config.set("regions.${world.name}:$region.extension-duration", days)
        markDirty()
    }

    fun getRegionOverrides(region: String, world: World): Map<String, Any> {
        if (!hasRegion(region, world)) return emptyMap()

        val compositeKey = "${world.name}:$region"
        val section = config.getConfigurationSection("regions.$compositeKey") ?: return emptyMap()

        return section.getKeys(false).associateWith { section.get(it)!! }
    }

    val allRegions: Set<String>
        get() = config.getConfigurationSection("regions")?.getKeys(false) ?: emptySet()

    fun migrateFromMainConfig(mainConfig: FileConfiguration) {
        val regionsSection = mainConfig.getConfigurationSection("regions") ?: return
        val regionKeys = regionsSection.getKeys(false)
        if (regionKeys.isEmpty()) return

        var migratedCount = 0
        val defaultWorld = plugin.server.worlds.firstOrNull()?.name ?: return
        val world = plugin.server.getWorld(defaultWorld) ?: return

        for (region in regionKeys) {
            if (hasRegion(region, world)) continue

            val sourcePath = "regions.$region"
            val destPath = "regions.$region"

            mainConfig.getDouble("$sourcePath.price").takeIf { mainConfig.contains("$sourcePath.price") }
                ?.let { config.set("$destPath.price", it) }
            mainConfig.getInt("$sourcePath.duration").takeIf { mainConfig.contains("$sourcePath.duration") }
                ?.let { config.set("$destPath.duration", it) }
            mainConfig.getInt("$sourcePath.max-extensions").takeIf { mainConfig.contains("$sourcePath.max-extensions") }
                ?.let { config.set("$destPath.max-extensions", it) }
            mainConfig.getDouble("$sourcePath.extension-price").takeIf { mainConfig.contains("$sourcePath.extension-price") }
                ?.let { config.set("$destPath.extension-price", it) }
            mainConfig.getBoolean("$sourcePath.allow-extensions").takeIf { mainConfig.contains("$sourcePath.allow-extensions") }
                ?.let { config.set("$destPath.allow-extensions", it) }
            mainConfig.getInt("$sourcePath.extension-duration").takeIf { mainConfig.contains("$sourcePath.extension-duration") }
                ?.let { config.set("$destPath.extension-duration", it) }

            migratedCount++
        }

        if (migratedCount > 0) {
            save()
            plugin.logger.info("Migrated $migratedCount region(s) from config.yml to regions.yml")

            mainConfig.set("regions", null)
            try {
                mainConfig.save(File(plugin.dataFolder, "config.yml"))
                plugin.logger.info("Removed migrated regions from config.yml")
            } catch (e: IOException) {
                plugin.logger.log(Level.WARNING, "Could not remove regions from config.yml", e)
            }
        }
    }

    fun verifyAndRepairRegions() {
        val rentalSpaces = plugin.signsConfig.getRegisteredRegions()

        if (rentalSpaces.isEmpty()) {
            if (plugin.configManager?.isDebug == true) {
                plugin.logger.info("Region verification: No rental spaces found")
            }
            return
        }

        val configuredRegions = allRegions
        val orphanedConfigs = configuredRegions.filter { it !in rentalSpaces }

        if (orphanedConfigs.isNotEmpty()) {
            plugin.logger.info("Region verification: Found ${orphanedConfigs.size} orphaned config(s) (custom overrides exist but no rental space):")
            orphanedConfigs.forEach { plugin.logger.info("  - $it") }
            plugin.logger.info("These configs are safe but may be unused. Use /zroverride remove <region> to clean up.")
        } else if (plugin.configManager?.isDebug == true) {
            plugin.logger.info("Region verification: No orphaned configs found")
        }

        val usingDefaults = rentalSpaces.size - configuredRegions.size
        if (usingDefaults > 0 && plugin.configManager?.isDebug == true) {
            plugin.logger.info("Region verification: $usingDefaults region(s) using default settings")
            plugin.logger.info("Use /zroverride commands to set custom values for specific regions")
        }
    }

    fun getVerificationReport(): Map<String, Any> {
        val regionsWithSigns = plugin.signsConfig.getRegisteredRegions()
        val configuredRegions = allRegions

        val missingConfigs = regionsWithSigns.filter { it !in configuredRegions }
        val orphanedConfigs = configuredRegions.filter { it !in regionsWithSigns }

        return mapOf(
            "totalSigns" to regionsWithSigns.size,
            "totalConfigs" to configuredRegions.size,
            "missingConfigs" to missingConfigs,
            "orphanedConfigs" to orphanedConfigs,
            "regionsWithSigns" to regionsWithSigns.toList(),
            "configuredRegions" to configuredRegions.toList()
        )
    }

    // ========== Group Override Methods ==========

    fun setGroupPrice(groupName: String, price: Double) {
        config.set("groups.$groupName.price", price)
        markDirty()
    }

    fun setGroupDuration(groupName: String, days: Int) {
        config.set("groups.$groupName.duration", days)
        markDirty()
    }

    fun setGroupMaxExtensions(groupName: String, maxExtensions: Int) {
        config.set("groups.$groupName.max-extensions", maxExtensions)
        markDirty()
    }

    fun setGroupExtensionPrice(groupName: String, price: Double) {
        config.set("groups.$groupName.extension-price", price)
        markDirty()
    }

    fun setGroupAllowExtensions(groupName: String, allow: Boolean) {
        config.set("groups.$groupName.allow-extensions", allow)
        markDirty()
    }

    fun setGroupExtensionDuration(groupName: String, days: Int) {
        config.set("groups.$groupName.extension-duration", days)
        markDirty()
    }

    fun getGroupPrice(groupName: String, defaultPrice: Double): Double {
        val path = "groups.$groupName.price"
        return if (config.contains(path)) config.getDouble(path, defaultPrice) else defaultPrice
    }

    fun getGroupDuration(groupName: String, defaultDuration: Int): Int {
        val path = "groups.$groupName.duration"
        return if (config.contains(path)) config.getInt(path, defaultDuration) else defaultDuration
    }

    fun getGroupMaxExtensions(groupName: String, defaultMax: Int): Int {
        val path = "groups.$groupName.max-extensions"
        return if (config.contains(path)) config.getInt(path, defaultMax) else defaultMax
    }

    fun getGroupExtensionPrice(groupName: String, defaultPrice: Double): Double {
        val path = "groups.$groupName.extension-price"
        if (config.contains(path)) {
            val price = config.getDouble(path, defaultPrice)
            return if (price > 0) price else defaultPrice
        }
        return defaultPrice
    }

    fun getGroupAllowExtensions(groupName: String, defaultAllow: Boolean): Boolean {
        val path = "groups.$groupName.allow-extensions"
        return if (config.contains(path)) config.getBoolean(path, defaultAllow) else defaultAllow
    }

    fun getGroupExtensionDuration(groupName: String, defaultDuration: Int): Int {
        val path = "groups.$groupName.extension-duration"
        return if (config.contains(path)) config.getInt(path, defaultDuration) else defaultDuration
    }

    fun hasGroupOverrides(groupName: String): Boolean = config.contains("groups.$groupName")

    fun getGroupOverrides(groupName: String): Map<String, Any> {
        if (!hasGroupOverrides(groupName)) return emptyMap()

        val section = config.getConfigurationSection("groups.$groupName") ?: return emptyMap()
        return section.getKeys(false).associateWith { section.get(it)!! }
    }

    fun removeGroupOverrides(groupName: String) {
        if (!hasGroupOverrides(groupName)) return

        config.set("groups.$groupName", null)
        markDirty()

        if (plugin.configManager?.isDebug == true) {
            plugin.logger.info("Removed overrides for group '$groupName' from regions.yml")
        }
    }

    val allGroupsWithOverrides: Set<String>
        get() = config.getConfigurationSection("groups")?.getKeys(false) ?: emptySet()

    fun hasRegionOverrides(compositeKey: String): Boolean {
        val path = "regions.$compositeKey"
        return config.contains(path) && config.isConfigurationSection(path)
    }

    fun removeRegionOverrides(compositeKey: String) {
        if (!hasRegionOverrides(compositeKey)) return

        config.set("regions.$compositeKey", null)
        markDirty()

        if (plugin.configManager?.isDebug == true) {
            plugin.logger.info("Removed individual overrides for region '$compositeKey' from regions.yml")
        }
    }
}
