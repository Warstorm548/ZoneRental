package com.zonerental.config

import com.zonerental.ZoneRental
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration

class ConfigManager(private val plugin: ZoneRental) {

    private var config: FileConfiguration = plugin.config

    // Cached values
    var prefix: String = ""
        private set
    var commandPrefix: String = "zr"
        private set
    var isDebug: Boolean = false
        private set
    var expirationCheckInterval: Int = 1
        private set

    // Economy settings
    var isEconomyEnabled: Boolean = true
        private set
    var defaultPrice: Double = 100.0
        private set
    var currencyFormat: String = "$%.2f"
        private set
    private var regionPrices: MutableMap<String, Double> = mutableMapOf()
    var permissionPrices: Map<String, Double> = emptyMap()
        private set

    // Durations
    var defaultDuration: Int = 7
        private set
    var extensionDuration: Int = 7
        private set
    var maxExtensions: Int = 10
        private set
    var availableDurations: List<Int> = listOf(1, 3, 7, 14, 30)
        private set

    // Limits
    var maxRentalsPerPlayer: Int = 3
        private set

    // Messages
    private var messages: MutableMap<String, String> = mutableMapOf()

    // Sign formats
    var availableSignFormat: List<String> = emptyList()
        private set
    var rentedSignFormat: List<String> = emptyList()
        private set

    // Features
    var isBlockRestoration: Boolean = true
        private set
    var isAutoDeleteSchematics: Boolean = true
        private set
    var isItemStorage: Boolean = true
        private set
    var isSignProtection: Boolean = true
        private set
    var isEnvironmentProtection: Boolean = true
        private set
    var isLoadChunksForSignUpdates: Boolean = true
        private set
    /** Maximum signs per rental space; -1 means unlimited. */
    var maxSignsPerRegion: Int = DEFAULT_MAX_SIGNS_PER_REGION
        private set

    // Async scanning settings
    var isAsyncScanningEnabled: Boolean = true
        private set
    var minChunksForAsync: Int = 10
        private set
    var tpsHealthyThreshold: Double = 19.5
        private set
    var tpsWarningThreshold: Double = 18.5
        private set
    var isDebugAsync: Boolean = false
        private set
    var maxRentalChunks: Int = 2000
        private set

    // Teleport settings
    var isTeleportEnabled: Boolean = true
        private set
    var teleportMaxSearchDistance: Int = 20
        private set
    var teleportForwardSearchDistance: Int = 5
        private set
    var teleportFloorSearchDown: Int = 20
        private set
    var teleportFloorSearchUp: Int = 20
        private set
    var teleportCooldown: Int = 30
        private set
    var isTeleportCrossWorldWarning: Boolean = true
        private set
    var isTeleportSoundEnabled: Boolean = true
        private set
    var isTeleportParticleEnabled: Boolean = true
        private set

    init {
        loadConfig()
    }

    fun reload() {
        plugin.reloadConfig()
        config = plugin.config
        loadConfig()
    }

    private fun loadConfig() {
        // General settings
        prefix = config.getString("general.prefix", "<dark_gray>[<gold>ZoneRental<dark_gray>]<reset> ") ?: ""
        isDebug = config.getBoolean("general.debug", false)
        expirationCheckInterval = config.getInt("general.expiration-check-interval", 1)

        // Command prefix setting
        val configuredPrefix = config.getString("commands.prefix", "rr") ?: "rr"
        commandPrefix = validatePrefix(configuredPrefix)

        // Economy settings
        isEconomyEnabled = config.getBoolean("economy.enabled", true)
        defaultPrice = config.getDouble("economy.default-price", 100.0)
        currencyFormat = config.getString("economy.currency-format", "$%.2f") ?: "$%.2f"

        // Load per-region prices
        regionPrices = mutableMapOf()
        config.getConfigurationSection("regions")?.getKeys(false)?.forEach { region ->
            val price = config.getDouble("regions.$region.price", defaultPrice)
            regionPrices[region] = price
        }

        // Load permission-based prices
        val permPrices = mutableMapOf<String, Double>()
        config.getConfigurationSection("permission-prices")?.getKeys(false)?.forEach { perm ->
            val price = config.getDouble("permission-prices.$perm", defaultPrice)
            permPrices[perm] = price
        }
        permissionPrices = permPrices

        // Duration settings
        defaultDuration = config.getInt("durations.default-days", 7)
        extensionDuration = config.getInt("extension.extension-days", 7)
        maxExtensions = config.getInt("extension.max-extensions", 10)
        availableDurations = config.getIntegerList("durations.available-durations").ifEmpty {
            listOf(1, 3, 7, 14, 30)
        }

        // Limits
        maxRentalsPerPlayer = config.getInt("limits.max-rentals-per-player", 3)

        // Load messages
        loadMessages()

        // Sign formats
        availableSignFormat = config.getStringList("signs.available-format").ifEmpty {
            listOf(
                "<dark_green>[AVAILABLE]",
                "<gold>{region}",
                "<yellow>{price}",
                "<gray>{duration} days"
            )
        }

        rentedSignFormat = config.getStringList("signs.rented-format").ifEmpty {
            listOf(
                "<red>[RENTED]",
                "<gold>{region}",
                "<yellow>{owner}",
                "<gray>Exp: {expires}"
            )
        }

        // Features
        isBlockRestoration = config.getBoolean("restoration.enabled", true)
        isAutoDeleteSchematics = config.getBoolean("restoration.auto-delete-schematics", true)
        isItemStorage = config.getBoolean("storage.enabled", true)
        isSignProtection = config.getBoolean("signs.protect-signs", true)
        isEnvironmentProtection = config.getBoolean("signs.environment-protection", true)
        isLoadChunksForSignUpdates = config.getBoolean("signs.load-chunks-for-updates", true)
        val maxSigns = config.getInt("signs.max-per-region", DEFAULT_MAX_SIGNS_PER_REGION)
        maxSignsPerRegion = if (maxSigns == -1 || maxSigns >= 1) maxSigns else {
            plugin.logger.warning("signs.max-per-region must be -1 (unlimited) or at least 1, got $maxSigns. " +
                "Using $DEFAULT_MAX_SIGNS_PER_REGION")
            DEFAULT_MAX_SIGNS_PER_REGION
        }

        // Async scanning settings
        isAsyncScanningEnabled = config.getBoolean("async-scanning.enabled", true)
        minChunksForAsync = config.getInt("async-scanning.min-chunks-for-async", 10)
        tpsHealthyThreshold = config.getDouble("async-scanning.tps-healthy-threshold", 19.5)
        tpsWarningThreshold = config.getDouble("async-scanning.tps-warning-threshold", 18.5)
        isDebugAsync = config.getBoolean("async-scanning.debug-async", false)
        maxRentalChunks = config.getInt("async-scanning.max-rental-chunks", 2000)

        // Teleport settings
        isTeleportEnabled = config.getBoolean("teleport.enabled", true)
        teleportMaxSearchDistance = config.getInt("teleport.max-search-distance", 20)
        teleportForwardSearchDistance = minOf(config.getInt("teleport.forward-search-distance", 5), 20)
        teleportFloorSearchDown = minOf(config.getInt("teleport.floor-search-down", 20), 20)
        teleportFloorSearchUp = config.getInt("teleport.floor-search-up", 20)
        teleportCooldown = config.getInt("teleport.cooldown", 30)
        isTeleportCrossWorldWarning = config.getBoolean("teleport.cross-world-warning", true)
        isTeleportSoundEnabled = config.getBoolean("teleport.sound-enabled", true)
        isTeleportParticleEnabled = config.getBoolean("teleport.particle-enabled", true)
    }

    private fun loadMessages() {
        messages = mutableMapOf(
            "no-permission" to "<red>You don't have permission to do that!",
            "region-not-found" to "<red>Region <yellow>{region}<red> not found!",
            "already-rented" to "<red>This region is already rented!",
            "rental-success" to "<green>You have successfully rented <yellow>{region}<green> for <yellow>{days}<green> days!",
            "rental-expired" to "<red>Your rental of <yellow>{region}<red> has expired!",
            "not-enough-money" to "<red>You don't have enough money! Need <yellow>{amount}",
            "max-rentals-reached" to "<red>You have reached the maximum number of rentals!",
            "rental-extended" to "<green>Rental extended for <yellow>{days}<green> days!",
            "max-extensions-reached" to "<red>Maximum extensions reached for this rental!",
            "items-stored" to "<green>Your items from <yellow>{region}<green> have been stored!",
            "items-retrieved" to "<green>You have retrieved your stored items!",
            "no-stored-items" to "<red>You have no stored items!",
            "sign-created" to "<green>Rental sign <yellow>#{id}<green> created for <yellow>{region}<green>!",
            "sign-removed" to "<green>Rental sign <yellow>#{id}<green> removed from <yellow>{region}<green>.",
            "sign-not-found" to "<red>Rental space <yellow>{region}<red> has no sign <yellow>#{id}<red>!",
            "sign-not-rental" to "<red>This is not a rental sign!",
            "sign-already-registered" to "<red>This sign is already rental sign <yellow>#{id}<red> of <yellow>{region}<red>!",
            "sign-limit-reached" to "<red>Rental space <yellow>{region}<red> already has the maximum of <yellow>{max}<red> signs!",
            "region-registered" to "<green>Rental space <yellow>{region}<green> created. Add signs with the createsign command.",
            "region-already-registered" to "<red>Rental space <yellow>{region}<red> is already registered!",
            "region-not-registered" to "<red>Region <yellow>{region}<red> is not a rental space!",
            "rental-reset" to "<green>Rental for <yellow>{region}<green> has been reset!",
            "admin-reset-success" to "<green>Successfully reset rental for <yellow>{region}<green>. Player <yellow>{player}<green> has been refunded <yellow>{amount}<green>.",
            "rental-reset-refund" to "<green>Your rental of <yellow>{region}<green> has been reset by an admin. You have been refunded <yellow>{amount}<green>.",
            "region-removed" to "<green>ZoneRental setup completely removed from <yellow>{region}<green>:",
            "rental-info" to "<gold>=== Rental Info for {region} ===",
            "config-reloaded" to "<green>Configuration reloaded!",
            // Refund tracking messages
            "refund-issued" to "<green>Refund issued to <yellow>{player}<green>: <yellow>{amount}<green> (Reason: {reason})",
            "refund-already-given" to "<red>Cannot refund. Total refunded (<yellow>{refunded}<red>) would exceed total paid (<yellow>{paid}<red>).",
            "refund-history-header" to "<green><bold>Refund History for <yellow>{region}",
            "refund-partial" to "<green>Proportional refund for <yellow>{days}<green> days removed: <yellow>{amount}",
            // Duration command messages
            "duration-add-charged" to "<green>Added <yellow>{days}<green> days to <yellow>{region}<green>. Player <yellow>{player}<green> was charged <yellow>{amount}<green>.",
            "duration-add-free" to "<green>Added <yellow>{days}<green> to <yellow>{region}<green> (no charge).",
            "duration-remove-refunded" to "<green>Removed <yellow>{days}<green> days from <yellow>{region}<green>. Player <yellow>{player}<green> was refunded <yellow>{amount}<green>.",
            "duration-remove-no-refund" to "<green>Removed <yellow>{days}<green> days from <yellow>{region}<green> (no refund issued).",
            // Region size messages
            "region-too-large" to "<red>Region <yellow>{region}<red> is too large to rent! (<yellow>{chunks}<red> chunks, max: <yellow>{max}<red>)",
            // EzChestShop integration messages
            "ezchestshop-removed" to "<yellow>Chest shops in <gold>{region}<yellow> have been removed due to rental expiration."
        )

        // Override with config values
        config.getConfigurationSection("messages")?.getKeys(false)?.forEach { key ->
            config.getString("messages.$key")?.let { messages[key] = it }
        }
    }

    companion object {
        private val MINI_MESSAGE = MiniMessage.miniMessage()
        const val DEFAULT_MAX_SIGNS_PER_REGION = 3
    }

    fun getMessage(key: String, vararg replacements: String): Component {
        var message = messages.getOrDefault(key, "<red>Missing message: $key")
        message = prefix + message

        // Replace placeholders
        var i = 0
        while (i < replacements.size - 1) {
            message = message.replace(replacements[i], replacements[i + 1])
            i += 2
        }

        return MINI_MESSAGE.deserialize(message)
    }

    /**
     * Validates and sanitizes the command prefix.
     * Rules:
     * - Lowercase only (auto-converts)
     * - Alphanumeric only (a-z, 0-9)
     * - Length: 2-10 characters
     * - Defaults to "rr" if invalid
     */
    private fun validatePrefix(prefix: String): String {
        if (prefix.isEmpty()) {
            plugin.logger.warning("Command prefix is empty! Defaulting to 'rr'")
            return "rr"
        }

        val validated = prefix.lowercase()

        if (validated.length < 2 || validated.length > 10) {
            plugin.logger.warning("Command prefix '$prefix' is invalid (must be 2-10 characters). Defaulting to 'rr'")
            return "rr"
        }

        if (!validated.matches(Regex("[a-z0-9]+"))) {
            plugin.logger.warning("Command prefix '$prefix' is invalid (must be lowercase letters/numbers only). Defaulting to 'rr'")
            return "rr"
        }

        if (validated != prefix) {
            plugin.logger.info("Command prefix '$prefix' converted to lowercase: '$validated'")
        }

        return validated
    }

    fun getPriceForRegion(region: String, world: World): Double {
        plugin.regionsConfig?.let {
            return it.getRegionPrice(region, world, defaultPrice)
        }
        return regionPrices.getOrDefault(region, defaultPrice)
    }

    fun getDurationForRegion(region: String, world: World): Int {
        plugin.regionsConfig?.let {
            return it.getRegionDuration(region, world, defaultDuration)
        }
        return config.getInt("regions.$region.duration", defaultDuration)
    }

    fun getMaxExtensionsForRegion(region: String, world: World): Int {
        plugin.regionsConfig?.let {
            return it.getRegionMaxExtensions(region, world, maxExtensions)
        }
        return maxExtensions
    }

    fun getExtensionPriceForRegion(region: String, world: World): Double {
        val calculatedPrice = getExtensionPrice(region, world)
        plugin.regionsConfig?.let {
            return it.getRegionExtensionPrice(region, world, calculatedPrice)
        }
        return calculatedPrice
    }

    fun getAllowExtensionsForRegion(region: String, world: World): Boolean {
        val defaultAllow = config.getBoolean("extension.enabled", true)
        plugin.regionsConfig?.let {
            return it.getRegionAllowExtensions(region, world, defaultAllow)
        }
        return defaultAllow
    }

    fun getExtensionDurationForRegion(region: String, world: World): Int {
        plugin.regionsConfig?.let {
            return it.getRegionExtensionDuration(region, world, extensionDuration)
        }
        return extensionDuration
    }

    // Config-backed properties (read directly from config)
    val isRefundOnDurationReset: Boolean
        get() = config.getBoolean("extension.refund-on-duration-reset", true)

    fun getSchematicCacheSize(defaultSize: Int): Int =
        config.getInt("restoration.schematic-cache-size", defaultSize)

    val isBlockStorage: Boolean
        get() = config.getBoolean("storage.store-player-blocks", true)

    val blockBlacklist: List<String>
        get() = config.getStringList("storage.block-blacklist")

    // Member management settings
    val isMemberManagementEnabled: Boolean
        get() = config.getBoolean("members.enabled", true)

    val maxMembers: Int
        get() = config.getInt("members.max-members", 5)

    // Duration-related refund and charge settings
    val isRefundOnTimeRemoval: Boolean
        get() = config.getBoolean("duration.refund-on-time-removal", true)

    val isChargeForDurationAdd: Boolean
        get() = config.getBoolean("duration.charge-for-add", false)

    val isDurationAddBypassExtensionLimit: Boolean
        get() = config.getBoolean("duration.add-bypass-extension-limit", true)

    val durationAddPricePerDay: Double
        get() = config.getDouble("duration.add-price-per-day", 0.0)

    // Extension price calculation helper
    fun getExtensionPrice(region: String, world: World): Double {
        val pricePerDay = durationAddPricePerDay
        if (pricePerDay > 0) {
            return pricePerDay
        }

        val regionPrice = getPriceForRegion(region, world)
        val duration = getDurationForRegion(region, world)

        if (duration <= 0) {
            return regionPrice
        }

        return regionPrice / duration
    }

    // Region verification settings
    val isAutoVerifyRegions: Boolean
        get() = config.getBoolean("regions-config.auto-verify-regions", true)

    val isEnableVerifyCommand: Boolean
        get() = config.getBoolean("regions-config.enable-verify-command", true)

    // EzChestShop integration settings
    val isEzChestShopEnabled: Boolean
        get() = config.getBoolean("integration.ezchestshop.enabled", true)

    val isEzChestShopNotifyOnRemoval: Boolean
        get() = config.getBoolean("integration.ezchestshop.notify-on-removal", true)

    val ezChestShopRemovalMessage: String
        get() = config.getString("integration.ezchestshop.removal-message",
            "&eChest shops in &6{region}&e have been removed due to rental expiration.") ?: ""

    // Aliases for enhanced EzChestShop manager
    val isEzChestShopRemovalEnabled: Boolean
        get() = isEzChestShopEnabled

    val isEzChestShopNotifyEnabled: Boolean
        get() = isEzChestShopNotifyOnRemoval

    val isDebugMode: Boolean
        get() = isDebug
}
