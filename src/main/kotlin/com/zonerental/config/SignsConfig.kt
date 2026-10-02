package com.zonerental.config

import com.zonerental.ZoneRental
import com.zonerental.models.RentalSign
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.util.logging.Level

/**
 * signs.yml: registered rental spaces and their signs.
 *
 * ```
 * signs:
 *   world:shop1:        # registration, keyed by the REGION's world
 *     next-id: 3        # sign IDs are per region and never reused
 *     '1':
 *       world: lobby    # the SIGN's world (may differ from the region's)
 *       x: 100
 *       y: 64
 *       z: 200
 *       support-block: { x, y, z, original-type, original-data }
 * ```
 *
 * Data is held in memory and written by [save] (autosave via [saveIfDirty]).
 */
class SignsConfig(private val plugin: ZoneRental) {

    private val configFile: File = File(plugin.dataFolder, "signs.yml")

    private class RegionEntry(var nextId: Int, val signs: java.util.TreeMap<Int, RentalSign> = java.util.TreeMap())

    /** "world:region" -> registration */
    private val regions = linkedMapOf<String, RegionEntry>()

    /** Legacy entries that couldn't be migrated; written back unchanged so no data is lost. */
    private val unmigratedEntries = linkedMapOf<String, Map<String, Any?>>()

    // Indexes for O(1) lookups, rebuilt on load and kept in sync on every change
    private val signIndex = mutableMapOf<String, RentalSign>()                // "world:x:y:z" -> sign
    private val supportIndex = mutableMapOf<String, MutableSet<RentalSign>>() // "world:x:y:z" -> signs on that block
    private val chunkIndex = mutableMapOf<String, MutableSet<RentalSign>>()   // "world:cx:cz" -> signs in that chunk

    // Dirty tracking for optimized saves
    @Volatile
    var isDirty: Boolean = false
        private set

    init {
        createConfig()
        load()
    }

    private fun createConfig() {
        if (!configFile.exists()) {
            configFile.parentFile?.mkdirs()
            try {
                configFile.createNewFile()
                plugin.logger.info("Created signs.yml file")
            } catch (e: IOException) {
                plugin.logger.log(Level.SEVERE, "Could not create signs.yml!", e)
            }
        }
    }

    private fun load() {
        val config = YamlConfiguration.loadConfiguration(configFile)
        val migrated = migrate(config)
        parse(config)
        rebuildIndexes()
        isDirty = false
        if (migrated) save()
    }

    // ========== Migration ==========

    /**
     * Brings older signs.yml layouts up to date. Idempotent.
     * - Pre-multi-world: keys without "world:" are prefixed with their `world` field.
     * - Pre-3.3.0: one sign stored directly under the region becomes sign 1.
     * The original file is copied to signs.yml.pre-3.3.0.bak before anything changes.
     *
     * @return true if the file content changed and should be saved
     */
    private fun migrate(config: YamlConfiguration): Boolean {
        val signsSection = config.getConfigurationSection("signs") ?: return false
        val keys = signsSection.getKeys(false)
        val needsWorldPrefix = keys.filter { !it.contains(":") }
        val singleSign = keys.filter { isSingleSignLayout(signsSection.getConfigurationSection(it)) }
        if (needsWorldPrefix.isEmpty() && singleSign.isEmpty()) return false

        backupBeforeMigration()

        // Pre-multi-world: "shop1" -> "<world>:shop1"
        var prefixed = 0
        for (key in needsWorldPrefix) {
            val world = config.getString("signs.$key.world")
            if (world.isNullOrEmpty()) {
                plugin.logger.warning("Sign '$key' has no world field, skipping migration")
                continue
            }
            if (config.contains("signs.$world:$key")) {
                plugin.logger.warning("Sign '$key' can't be migrated: '$world:$key' already exists. " +
                    "Kept as is; merge or delete it by hand")
                continue
            }
            copySection(config, "signs.$key", "signs.$world:$key")
            config.set("signs.$key", null)
            prefixed++
        }
        if (prefixed > 0) plugin.logger.info("Migrated $prefixed sign(s) to world-aware format")

        // Pre-3.3.0: single sign directly under the region -> sign 1
        var numbered = 0
        for (key in config.getConfigurationSection("signs")?.getKeys(false).orEmpty()) {
            if (!key.contains(":")) continue // legacy entry that couldn't be prefixed
            val section = config.getConfigurationSection("signs.$key") ?: continue
            if (!isSingleSignLayout(section)) continue
            val values = section.getValues(true).filterValues { it !is ConfigurationSection }
            config.set("signs.$key", null)
            for ((field, value) in values) {
                config.set("signs.$key.1.$field", value)
            }
            config.set("signs.$key.next-id", 2)
            numbered++
        }
        if (numbered > 0) plugin.logger.info("Migrated $numbered region(s) to numbered signs (existing sign is #1)")

        return true
    }

    private fun isSingleSignLayout(section: ConfigurationSection?): Boolean =
        section != null && section.contains("x") && !section.isConfigurationSection("x")

    private fun copySection(config: YamlConfiguration, from: String, to: String) {
        val section = config.getConfigurationSection(from) ?: return
        for ((subKey, value) in section.getValues(true)) {
            if (value !is ConfigurationSection) config.set("$to.$subKey", value)
        }
    }

    private fun backupBeforeMigration() {
        val backup = File(plugin.dataFolder, "signs.yml.pre-3.3.0.bak")
        if (backup.exists()) return
        try {
            configFile.copyTo(backup)
            plugin.logger.info("Backed up signs.yml to ${backup.name} before migrating")
        } catch (e: IOException) {
            plugin.logger.log(Level.WARNING, "Could not back up signs.yml before migrating", e)
        }
    }

    // ========== Load / save ==========

    private fun parse(config: YamlConfiguration) {
        regions.clear()
        unmigratedEntries.clear()
        val signsSection = config.getConfigurationSection("signs") ?: return

        for (key in signsSection.getKeys(false)) {
            val section = signsSection.getConfigurationSection(key) ?: continue
            val regionWorld = key.substringBefore(":", "")
            val regionName = key.substringAfter(":", "")
            if (regionWorld.isEmpty() || regionName.isEmpty()) {
                plugin.logger.warning("Ignoring invalid signs.yml entry '$key' (expected world:region)")
                unmigratedEntries[key] = section.getValues(true).filterValues { it !is ConfigurationSection }
                continue
            }

            val entry = RegionEntry(section.getInt("next-id", 1))
            for (idKey in section.getKeys(false)) {
                val id = idKey.toIntOrNull() ?: continue
                val signSection = section.getConfigurationSection(idKey) ?: continue
                val signWorld = signSection.getString("world")
                if (signWorld == null) {
                    plugin.logger.warning("Sign $key #$id has no world field, skipping")
                    continue
                }
                val support = signSection.getConfigurationSection("support-block")?.let {
                    RentalSign.SupportBlock(
                        it.getInt("x"), it.getInt("y"), it.getInt("z"),
                        it.getString("original-type") ?: "STONE",
                        it.getString("original-data") ?: ""
                    )
                }
                entry.signs[id] = RentalSign(
                    regionWorld, regionName, id, signWorld,
                    signSection.getInt("x"), signSection.getInt("y"), signSection.getInt("z"),
                    support
                )
            }
            // Never hand out an ID that is already used, even if next-id was edited by hand
            entry.nextId = maxOf(entry.nextId, (entry.signs.keys.maxOrNull() ?: 0) + 1)
            regions[key] = entry
        }
    }

    private fun markDirty() {
        isDirty = true
    }

    /**
     * Saves only if there are unsaved changes
     */
    fun saveIfDirty() {
        if (isDirty) {
            save()
        }
    }

    fun save() {
        val config = YamlConfiguration()
        config.createSection("signs")
        for ((key, entry) in regions) {
            val path = "signs.$key"
            config.set("$path.next-id", entry.nextId)
            for ((id, sign) in entry.signs) {
                config.set("$path.$id.world", sign.signWorld)
                config.set("$path.$id.x", sign.x)
                config.set("$path.$id.y", sign.y)
                config.set("$path.$id.z", sign.z)
                sign.support?.let { support ->
                    config.set("$path.$id.support-block.x", support.x)
                    config.set("$path.$id.support-block.y", support.y)
                    config.set("$path.$id.support-block.z", support.z)
                    config.set("$path.$id.support-block.original-type", support.originalType)
                    config.set("$path.$id.support-block.original-data", support.originalData)
                }
            }
        }

        for ((key, values) in unmigratedEntries) {
            for ((field, value) in values) config.set("signs.$key.$field", value)
        }

        try {
            config.save(configFile)
        } catch (e: IOException) {
            plugin.logger.log(Level.SEVERE, "Could not save signs.yml!", e)
            return
        }
        isDirty = false
    }

    fun reload() {
        load()
    }

    // ========== Indexes ==========

    private fun rebuildIndexes() {
        signIndex.clear()
        supportIndex.clear()
        chunkIndex.clear()
        regions.values.forEach { entry -> entry.signs.values.forEach(::index) }

        if (plugin.configManager?.isDebug == true) {
            plugin.logger.info("Indexed ${signIndex.size} rental sign(s) in ${regions.size} rental space(s)")
        }
    }

    private fun index(sign: RentalSign) {
        signIndex[locationKey(sign.signWorld, sign.x, sign.y, sign.z)] = sign
        chunkIndex.getOrPut(chunkKey(sign.signWorld, sign.x shr 4, sign.z shr 4)) { mutableSetOf() } += sign
        sign.support?.let {
            supportIndex.getOrPut(locationKey(sign.signWorld, it.x, it.y, it.z)) { mutableSetOf() } += sign
        }
    }

    private fun unindex(sign: RentalSign) {
        signIndex.remove(locationKey(sign.signWorld, sign.x, sign.y, sign.z))
        removeFromSetIndex(chunkIndex, chunkKey(sign.signWorld, sign.x shr 4, sign.z shr 4), sign)
        sign.support?.let {
            removeFromSetIndex(supportIndex, locationKey(sign.signWorld, it.x, it.y, it.z), sign)
        }
    }

    private fun removeFromSetIndex(index: MutableMap<String, MutableSet<RentalSign>>, key: String, sign: RentalSign) {
        val set = index[key] ?: return
        set.remove(sign)
        if (set.isEmpty()) index.remove(key)
    }

    private fun locationKey(worldName: String, x: Int, y: Int, z: Int): String = "$worldName:$x:$y:$z"

    private fun locationKey(loc: Location): String = locationKey(loc.world.name, loc.blockX, loc.blockY, loc.blockZ)

    private fun chunkKey(worldName: String, chunkX: Int, chunkZ: Int): String = "$worldName:$chunkX:$chunkZ"

    private fun regionKey(regionName: String, world: World): String = "${world.name}:$regionName"

    // ========== Registration ==========

    /**
     * Registers a region as a rental space (no sign needed).
     * @return true if newly registered, false if it already was
     */
    fun registerRegion(regionName: String, world: World): Boolean {
        val key = regionKey(regionName, world)
        if (key in regions) return false
        regions[key] = RegionEntry(1)
        markDirty()
        return true
    }

    fun isRegistered(regionName: String, world: World): Boolean = regionKey(regionName, world) in regions

    fun isRegistered(compositeKey: String): Boolean = compositeKey in regions

    /**
     * Removes the registration and every sign of a region from signs.yml.
     * Blocks in the world are not touched.
     *
     * @return the signs that were registered
     */
    fun unregisterRegion(regionName: String, world: World): List<RentalSign> {
        val entry = regions.remove(regionKey(regionName, world)) ?: return emptyList()
        entry.signs.values.forEach(::unindex)
        markDirty()
        return entry.signs.values.toList()
    }

    /** All registered rental spaces as "world:region" keys. */
    fun getRegisteredRegions(): Set<String> = regions.keys.toSet()

    // ========== Signs ==========

    /**
     * Adds a sign to a region, registering the region if needed.
     * The sign gets the region's next ID; IDs are never reused.
     */
    fun addSign(regionName: String, regionWorld: World, signLocation: Location): RentalSign {
        val key = regionKey(regionName, regionWorld)
        val entry = regions.getOrPut(key) { RegionEntry(1) }
        val sign = RentalSign(
            regionWorld.name, regionName, entry.nextId, signLocation.world.name,
            signLocation.blockX, signLocation.blockY, signLocation.blockZ, null
        )
        entry.signs[sign.id] = sign
        entry.nextId++
        index(sign)
        markDirty()
        return sign
    }

    /**
     * Records the support block of a sign.
     * @return the updated sign, or null if the sign no longer exists
     */
    fun setSupportBlock(sign: RentalSign, supportLoc: Location, blockType: String, blockData: String): RentalSign? {
        val entry = regions[sign.regionKey] ?: return null
        val current = entry.signs[sign.id] ?: return null
        val updated = current.copy(
            support = RentalSign.SupportBlock(supportLoc.blockX, supportLoc.blockY, supportLoc.blockZ, blockType, blockData)
        )
        unindex(current)
        entry.signs[sign.id] = updated
        index(updated)
        markDirty()
        return updated
    }

    /**
     * Removes one sign from signs.yml. The region stays registered.
     * @return the removed sign, or null if it didn't exist
     */
    fun removeSign(compositeKey: String, id: Int): RentalSign? {
        val sign = regions[compositeKey]?.signs?.remove(id) ?: return null
        unindex(sign)
        markDirty()
        return sign
    }

    fun removeSign(regionName: String, world: World, id: Int): RentalSign? = removeSign(regionKey(regionName, world), id)

    /** Signs of a region ordered by ID (oldest first). */
    fun getSigns(compositeKey: String): List<RentalSign> = regions[compositeKey]?.signs?.values?.toList() ?: emptyList()

    fun getSigns(regionName: String, world: World): List<RentalSign> = getSigns(regionKey(regionName, world))

    fun getSign(regionName: String, world: World, id: Int): RentalSign? = regions[regionKey(regionName, world)]?.signs?.get(id)

    fun getSignCount(regionName: String, world: World): Int = regions[regionKey(regionName, world)]?.signs?.size ?: 0

    fun getAllSigns(): List<RentalSign> = regions.values.flatMap { it.signs.values }

    /** O(1): the rental sign at this block, if any. */
    fun getSignAt(location: Location): RentalSign? = signIndex[locationKey(location)]

    /** O(1): every rental sign using this block as its support block. */
    fun getSignsOnSupportBlock(location: Location): List<RentalSign> =
        supportIndex[locationKey(location)]?.toList() ?: emptyList()

    /** O(1): every rental sign inside the given chunk. */
    fun getSignsInChunk(worldName: String, chunkX: Int, chunkZ: Int): List<RentalSign> =
        chunkIndex[chunkKey(worldName, chunkX, chunkZ)]?.toList() ?: emptyList()

    /** True if the block is a rental sign or the support block of one. */
    fun isProtectedBlock(location: Location): Boolean {
        val key = locationKey(location)
        return key in signIndex || key in supportIndex
    }

    /**
     * Get composite key (world:region) by sign location
     */
    fun getRegionByLocation(location: Location): String? = getSignAt(location)?.regionKey

    /**
     * Get composite key (world:region) for a support block location
     */
    fun getSupportBlockByLocation(location: Location): String? =
        supportIndex[locationKey(location)]?.firstOrNull()?.regionKey
}
