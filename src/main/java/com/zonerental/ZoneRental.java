package com.zonerental;

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin;
import com.zonerental.commands.*;
import com.zonerental.config.ConfigManager;
import com.zonerental.config.GroupsConfig;
import com.zonerental.config.RegionsConfig;
import com.zonerental.config.SignsConfig;
import com.zonerental.config.StorageConfig;
import com.zonerental.listeners.GroupChatListener;
import com.zonerental.listeners.SignInteractListener;
import com.zonerental.listeners.SignProtectionListener;
import com.zonerental.managers.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Main plugin class for ZoneRental.
 * Extends SuspendingJavaPlugin for MCCoroutine async support.
 */
public class ZoneRental extends SuspendingJavaPlugin {

    private static ZoneRental instance;
    
    // Core managers
    private ConfigManager configManager;
    private SignsConfig signsConfig;
    private StorageConfig storageConfig;
    private RegionsConfig regionsConfig;
    private GroupsConfig groupsConfig;
    private RentalManager rentalManager;
    private SignManager signManager;
    private StorageManager storageManager;
    private ExpirationManager expirationManager;
    private WorldGuardManager worldGuardManager;
    private WorldEditManager worldEditManager;
    private EzChestShopManager ezChestShopManager;
    private TeleportCooldownManager teleportCooldownManager;

    // Commands (for Phase 2 chat listener access)
    private GroupCommand groupCommand;

    // Listeners
    private GroupChatListener groupChatListener;

    // Economy
    private Economy economy;

    // Track command prefix
    private static final String DEFAULT_PREFIX = "zr";
    private String activePrefix;
    private boolean usingFallback = false;
    private List<String> registeredCommands = new ArrayList<>();

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize configurations
        if (!initializeConfigs()) {
            getLogger().severe("Failed to initialize configurations! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Check dependencies
        if (!checkDependencies()) {
            getLogger().severe("Missing required dependencies! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Setup economy
        if (!setupEconomy()) {
            getLogger().severe("Failed to setup economy! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize managers
        initializeManagers();
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Start tasks
        startTasks();

        getLogger().info("ZoneRental has been enabled successfully!");
        getLogger().info("Commands start with /" + activePrefix);
        getLogger().info("Example: /" + activePrefix + "info, /" + activePrefix + "extend, /" + activePrefix + "list");
    }
    
    @Override
    public void onDisable() {
        // Save all data
        if (rentalManager != null) {
            rentalManager.saveAllRentals();
        }
        if (signsConfig != null) {
            signsConfig.save();
        }
        if (storageConfig != null) {
            storageConfig.save();
        }
        if (regionsConfig != null) {
            regionsConfig.save();
        }
        if (groupsConfig != null) {
            groupsConfig.save();
        }

        // Cancel tasks
        Bukkit.getScheduler().cancelTasks(this);

        getLogger().info("ZoneRental has been disabled!");
    }
    
    private boolean initializeConfigs() {
        try {
            saveDefaultConfig();
            configManager = new ConfigManager(this);
            signsConfig = new SignsConfig(this);
            storageConfig = new StorageConfig(this);
            regionsConfig = new RegionsConfig(this);
            groupsConfig = new GroupsConfig(this);

            // Migrate regions from config.yml to regions.yml (one-time)
            regionsConfig.migrateFromMainConfig(getConfig());

            // Verify regions.yml is in sync with signs.yml (if auto-verify enabled)
            if (configManager.isAutoVerifyRegions()) {
                regionsConfig.verifyAndRepairRegions();
            }

            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize configurations", e);
            return false;
        }
    }
    
    private boolean checkDependencies() {
        // Check for Vault
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault is not installed!");
            return false;
        }

        // Check for WorldGuard
        if (getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().severe("WorldGuard is not installed!");
            return false;
        }

        // Check for WorldEdit
        if (getServer().getPluginManager().getPlugin("WorldEdit") == null) {
            getLogger().severe("WorldEdit is not installed!");
            return false;
        }

        return true;
    }
    
    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
    
    private void initializeManagers() {
        // Initialize WorldGuard and WorldEdit managers first
        worldGuardManager = new WorldGuardManager(this);
        worldEditManager = new WorldEditManager(this);

        // Initialize integration managers
        ezChestShopManager = new EzChestShopManager(this);

        // Initialize teleport cooldown manager
        teleportCooldownManager = new TeleportCooldownManager(configManager.getTeleportCooldown());

        // Initialize other managers
        rentalManager = new RentalManager(this);
        signManager = new SignManager(this);
        storageManager = new StorageManager(this);
        expirationManager = new ExpirationManager(this);

        // Load data
        rentalManager.loadAllRentals();
        signManager.loadAllSigns();
    }
    
    private void registerCommands() {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            getLogger().severe("Could not access CommandMap! Commands will not be registered.");
            return;
        }

        // Get configured prefix from config
        String configuredPrefix = configManager.getCommandPrefix();

        // Determine which prefix to use based on conflicts
        activePrefix = determineActivePrefix(commandMap, configuredPrefix);

        getLogger().info("Registering commands with prefix: '" + activePrefix + "'");

        // Register all commands with determined prefix
        registerCommandWithPrefix(commandMap, activePrefix, "", new RRCommand(this), "zonerental.user");
        registerCommandWithPrefix(commandMap, activePrefix, "reload", new ReloadCommand(this), "zonerental.admin.reload");
        registerCommandWithPrefix(commandMap, activePrefix, "create", new CreateCommand(this), "zonerental.admin.create");
        registerCommandWithPrefix(commandMap, activePrefix, "createsign", new CreateSignCommand(this), "zonerental.admin.createsign");
        registerCommandWithPrefix(commandMap, activePrefix, "reset", new ResetCommand(this), "zonerental.admin.reset");
        registerCommandWithPrefix(commandMap, activePrefix, "retrieve", new RetrieveCommand(this), "zonerental.retrieve");
        registerCommandWithPrefix(commandMap, activePrefix, "info", new InfoCommand(this), "zonerental.info");
        registerCommandWithPrefix(commandMap, activePrefix, "list", new ListCommand(this), "zonerental.list");
        registerCommandWithPrefix(commandMap, activePrefix, "extend", new ExtendCommand(this), "zonerental.extend");
        registerCommandWithPrefix(commandMap, activePrefix, "member", new MemberCommand(this), "zonerental.member");
        registerCommandWithPrefix(commandMap, activePrefix, "tp", new TpCommand(this), "zonerental.tp");

        DurationCommand durationCommand = new DurationCommand(this);
        registerCommandWithPrefix(commandMap, activePrefix, "duration", durationCommand, "zonerental.admin.duration");

        registerCommandWithPrefix(commandMap, activePrefix, "remove", new RemoveCommand(this), "zonerental.admin.remove");
        registerCommandWithPrefix(commandMap, activePrefix, "refundhistory", new RefundHistoryCommand(this), "zonerental.admin.refundhistory");
        registerCommandWithPrefix(commandMap, activePrefix, "verify", new VerifyCommand(this), "zonerental.admin.verify");

        OverrideCommand overrideCommand = new OverrideCommand(this);
        registerCommandWithPrefix(commandMap, activePrefix, "override", overrideCommand, "zonerental.admin.override");

        groupCommand = new GroupCommand(this);
        registerCommandWithPrefix(commandMap, activePrefix, "group", groupCommand, "zonerental.admin.group");

        // Log final status
        logPrefixStatus(configuredPrefix);

        // Update default command description to show active prefix
        updateHelpCommandDescription();
    }

    /**
     * Determines the active prefix to use based on conflicts
     */
    private String determineActivePrefix(CommandMap commandMap, String configuredPrefix) {
        // If configured prefix has no conflicts, use it
        if (!checkPrefixConflicts(commandMap, configuredPrefix)) {
            return configuredPrefix;
        }

        // Configured prefix has conflicts, log warning
        getLogger().warning("Custom prefix '" + configuredPrefix + "' conflicts with existing commands!");
        getLogger().warning("Falling back to default '" + DEFAULT_PREFIX + "' prefix due to conflicts...");
        usingFallback = true;

        // Check if default prefix has conflicts
        if (!checkPrefixConflicts(commandMap, DEFAULT_PREFIX)) {
            return DEFAULT_PREFIX;
        }

        // Both configured and default have conflicts, generate suffixed version
        getLogger().warning("Fallback prefix '" + DEFAULT_PREFIX + "' also has conflicts!");
        getLogger().warning("Generating auto-suffixed prefix...");
        return generateSuffixedPrefix(commandMap, DEFAULT_PREFIX);
    }

    /**
     * Checks if a prefix would conflict with existing commands
     */
    private boolean checkPrefixConflicts(CommandMap commandMap, String prefix) {
        // Check main command
        Command existingCmd = commandMap.getCommand(prefix);
        if (existingCmd != null) {
            // Check if this is our own command from plugin.yml - not a conflict
            if (existingCmd instanceof org.bukkit.command.PluginCommand) {
                org.bukkit.command.PluginCommand pluginCmd = (org.bukkit.command.PluginCommand) existingCmd;
                if (pluginCmd.getPlugin() != this) {
                    return true; // Conflict with another plugin
                }
                // This is our own command, skip conflict check for main command
            } else {
                return true; // Conflict with non-plugin command
            }
        }

        // Check all subcommands
        String[] subcommands = {
            "reload", "create", "createsign", "reset", "retrieve", "info", "list",
            "extend", "duration", "remove", "refundhistory", "verify", "override",
            "member", "tp", "group"
        };

        for (String sub : subcommands) {
            String fullCommand = prefix + sub;
            if (commandMap.getCommand(fullCommand) != null) {
                getLogger().warning("  Conflict detected: /" + fullCommand);
                return true;
            }
        }

        return false;
    }

    /**
     * Generates a suffix-based prefix (rr1, rr2, etc.) until no conflicts exist
     */
    private String generateSuffixedPrefix(CommandMap commandMap, String basePrefix) {
        for (int i = 1; i <= 99; i++) {
            String suffixedPrefix = basePrefix + i;
            if (!checkPrefixConflicts(commandMap, suffixedPrefix)) {
                getLogger().info("Using auto-generated prefix: '" + suffixedPrefix + "'");
                return suffixedPrefix;
            }
        }

        // Fallback to a random string if all numbers are taken (very unlikely)
        String randomPrefix = basePrefix + System.currentTimeMillis() % 1000;
        getLogger().warning("Using randomized prefix: '" + randomPrefix + "'");
        return randomPrefix;
    }

    /**
     * Registers a single command with the given prefix
     */
    private void registerCommandWithPrefix(CommandMap commandMap, String prefix, String subcommand,
                                           org.bukkit.command.CommandExecutor executor, String permission) {
        String commandName = subcommand.isEmpty() ? prefix : prefix + subcommand;

        // For main command with default prefix, use existing PluginCommand from plugin.yml
        if (subcommand.isEmpty() && prefix.equals(DEFAULT_PREFIX)) {
            org.bukkit.command.PluginCommand existingCmd = getCommand(DEFAULT_PREFIX);
            if (existingCmd != null) {
                existingCmd.setExecutor(executor);
                existingCmd.setPermission(permission);
                registeredCommands.add(commandName);
                if (configManager.isDebug()) {
                    getLogger().info("  Using existing PluginCommand: /" + commandName);
                }
                return;
            } else {
                getLogger().warning("Expected PluginCommand '" + DEFAULT_PREFIX +
                    "' from plugin.yml was not found. Falling back to DynamicCommand.");
            }
        }

        // For subcommands or custom prefix, use DynamicCommand
        DynamicCommand cmd = new DynamicCommand(commandName, this);
        cmd.setExecutor(executor);
        cmd.setPermission(permission);

        // Only set description for main command to appear in /help
        // Subcommands get no description so they don't clutter /help menu
        if (subcommand.isEmpty()) {
            // Main help command - show in /help with active prefix
            cmd.setDescription("ZoneRental (prefix: " + prefix + ") - Type /" + prefix + " for help");
            cmd.setUsage("/" + prefix);
        } else {
            // Subcommands - empty description keeps them out of /help
            cmd.setDescription("");
            cmd.setUsage("/" + commandName);
        }

        // Register with CommandMap
        if (commandMap.register(getName(), cmd)) {
            registeredCommands.add(commandName);
            if (configManager.isDebug()) {
                getLogger().info("  Registered: /" + commandName);
            }
        } else {
            getLogger().warning("  Failed to register: /" + commandName);
        }
    }

    /**
     * Logs the final prefix status to console
     */
    private void logPrefixStatus(String configuredPrefix) {
        if (activePrefix.equals(configuredPrefix) && !usingFallback) {
            getLogger().info("✓ Using custom prefix '" + activePrefix + "' for all commands");
        } else if (usingFallback && activePrefix.equals(DEFAULT_PREFIX)) {
            getLogger().warning("⚠ Custom prefix '" + configuredPrefix + "' had conflicts");
            getLogger().info("✓ Using fallback prefix '" + DEFAULT_PREFIX + "'");
        } else {
            getLogger().warning("⚠ Both custom prefix '" + configuredPrefix + "' and '" + DEFAULT_PREFIX + "' had conflicts");
            getLogger().info("✓ Using auto-generated prefix '" + activePrefix + "'");
        }

        getLogger().info("Commands are now available! Example: /" + activePrefix + "info");
    }

    /**
     * Updates the default command description to show the active prefix
     * This ensures /help displays the correct prefix players should use
     */
    private void updateHelpCommandDescription() {
        org.bukkit.command.PluginCommand defaultCommand = getCommand(DEFAULT_PREFIX);
        if (defaultCommand != null) {
            String description;
            if (activePrefix.equals(DEFAULT_PREFIX)) {
                // Using default prefix
                description = "ZoneRental - Rent WorldGuard regions. Type /" + DEFAULT_PREFIX + " for help";
            } else {
                // Using custom prefix - update description to show it
                description = "ZoneRental (active prefix: /" + activePrefix + ") - Type /" + activePrefix + " for help";
            }
            defaultCommand.setDescription(description);

            if (configManager.isDebug()) {
                getLogger().info("Updated /" + DEFAULT_PREFIX + " command description: " + description);
            }
        }
    }

    /**
     * Returns the currently active command prefix
     */
    public String getActivePrefix() {
        return activePrefix != null ? activePrefix : DEFAULT_PREFIX;
    }

    private CommandMap getCommandMap() {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (Exception e) {
            return null;
        }
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new SignInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new SignProtectionListener(this), this);

        // Register GroupChatListener for hybrid command prompts (Phase 2)
        groupChatListener = new GroupChatListener(this);
        getServer().getPluginManager().registerEvents(groupChatListener, this);

        // StorageManager also registers itself as a listener
    }
    
    private void startTasks() {
        // Start expiration checker task (runs every minute)
        long checkInterval = configManager.getExpirationCheckInterval() * 20L * 60L; // Convert minutes to ticks
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            expirationManager.checkExpiredRentals();
        }, checkInterval, checkInterval);
        
        // Start sign update task (runs every 30 seconds)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            signManager.updateAllSigns();
        }, 600L, 600L); // 30 seconds

        // Cleanup expired group action prompts (runs every 30 seconds)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (groupCommand != null) {
                groupCommand.cleanupExpiredActions();
            }
        }, 600L, 600L); // 30 seconds

        // Auto-save task (runs every 5 minutes) - uses dirty tracking to avoid unnecessary I/O
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            rentalManager.saveIfDirty();
            signsConfig.saveIfDirty();
            storageConfig.saveIfDirty();
            regionsConfig.saveIfDirty();
            groupsConfig.saveIfDirty();
            if (configManager.isDebug()) {
                getLogger().info("Auto-save check completed (dirty configs saved)");
            }
        }, 6000L, 6000L); // 5 minutes

        // Cleanup expired teleport cooldowns (runs every 10 minutes) - prevents memory leaks
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (teleportCooldownManager != null) {
                int removed = teleportCooldownManager.cleanupExpired();
                if (removed > 0 && configManager.isDebug()) {
                    getLogger().info("Cleaned up " + removed + " expired teleport cooldown(s)");
                }
            }
        }, 12000L, 12000L); // 10 minutes
    }
    
    public void reloadPlugin() {
        // Reload configurations
        reloadConfig();
        configManager.reload();
        signsConfig.reload();
        storageConfig.reload();
        regionsConfig.reload();

        // Reload integration managers
        if (ezChestShopManager != null) {
            ezChestShopManager.reload();
        }

        // Clear and reinitialize teleport cooldown manager with new config values
        if (teleportCooldownManager != null) {
            teleportCooldownManager.clearAll();
        }
        teleportCooldownManager = new TeleportCooldownManager(configManager.getTeleportCooldown());

        // Reload data
        rentalManager.loadAllRentals();
        signManager.loadAllSigns();

        // Verify regions if auto-verify enabled
        if (configManager.isAutoVerifyRegions()) {
            regionsConfig.verifyAndRepairRegions();
        }

        // Update default command description in case prefix changed
        updateHelpCommandDescription();

        getLogger().info("Plugin reloaded successfully!");
    }
    
    // Getters
    public static ZoneRental getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public SignsConfig getSignsConfig() {
        return signsConfig;
    }
    
    public StorageConfig getStorageConfig() {
        return storageConfig;
    }

    public RegionsConfig getRegionsConfig() {
        return regionsConfig;
    }

    public GroupsConfig getGroupsConfig() {
        return groupsConfig;
    }

    public GroupCommand getGroupCommand() {
        return groupCommand;
    }

    public RentalManager getRentalManager() {
        return rentalManager;
    }
    
    public SignManager getSignManager() {
        return signManager;
    }
    
    public StorageManager getStorageManager() {
        return storageManager;
    }
    
    public ExpirationManager getExpirationManager() {
        return expirationManager;
    }
    
    public WorldGuardManager getWorldGuardManager() {
        return worldGuardManager;
    }

    public WorldEditManager getWorldEditManager() {
        return worldEditManager;
    }

    public EzChestShopManager getEzChestShopManager() {
        return ezChestShopManager;
    }

    public TeleportCooldownManager getTeleportCooldownManager() {
        return teleportCooldownManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    // Inner class for dynamic command registration
    private static class DynamicCommand extends Command {
        private final ZoneRental plugin;
        private org.bukkit.command.CommandExecutor executor;

        public DynamicCommand(String name, ZoneRental plugin) {
            super(name);
            this.plugin = plugin;
        }

        public void setExecutor(org.bukkit.command.CommandExecutor executor) {
            this.executor = executor;
        }

        @Override
        public boolean execute(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
            if (executor != null) {
                return executor.onCommand(sender, this, commandLabel, args);
            }
            return false;
        }

        @Override
        public java.util.List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args)
                throws IllegalArgumentException {
            if (executor instanceof org.bukkit.command.TabCompleter) {
                return ((org.bukkit.command.TabCompleter) executor).onTabComplete(sender, this, alias, args);
            }
            return super.tabComplete(sender, alias, args);
        }
    }
}
