package com.zonerental.listeners;

import com.zonerental.ZoneRental;
import com.zonerental.managers.Rental;
import com.zonerental.util.WorldRegionParser;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignInteractListener implements Listener {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    private final ZoneRental plugin;

    public SignInteractListener(ZoneRental plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Check if player clicked on a sign
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        if (event.getClickedBlock() == null || 
            !(event.getClickedBlock().getState() instanceof Sign)) {
            return;
        }
        
        Location signLoc = event.getClickedBlock().getLocation();
        String compositeKey = plugin.getSignManager().getRegionFromSign(signLoc);

        if (compositeKey == null) {
            // Not a rental sign
            return;
        }

        // Parse composite key "world:region". The world is the REGION's world, which can
        // differ from the world the sign (and the player) is in.
        String regionName = WorldRegionParser.extractRegionName(compositeKey);
        String regionWorldName = WorldRegionParser.extractWorldName(compositeKey);

        if (regionName == null || regionWorldName == null) {
            plugin.getLogger().warning("Invalid composite key format from sign: " + compositeKey);
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        World regionWorld = plugin.getServer().getWorld(regionWorldName);
        if (regionWorld == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("region-not-found",
                "{region}", compositeKey));
            return;
        }

        // Check if player is shift-clicking (extend rental)
        if (player.isSneaking()) {
            handleExtendRental(player, regionName, regionWorld);
        } else {
            // Regular click - rent or show info
            handleRentOrInfo(player, regionName, regionWorld);
        }
    }
    
    private void handleRentOrInfo(Player player, String regionName, World world) {
        Rental rental = plugin.getRentalManager().getRental(regionName, world);

        if (rental == null) {
            // Region is available - attempt to rent
            handleRentRegion(player, regionName, world);
        } else {
            // Region is rented - show info
            showRentalInfo(player, rental);
        }
    }
    
    private void handleRentRegion(Player player, String regionName, World world) {
        // Check permission
        if (!player.hasPermission("zonerental.rent")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        // Check if region exists in WorldGuard (in the region's world, not the player's)
        if (!plugin.getWorldGuardManager().regionExists(regionName, world)) {
            player.sendMessage(plugin.getConfigManager().getMessage("region-not-found",
                "{region}", regionName));
            return;
        }
        
        // Check player rental limit
        int currentRentals = plugin.getRentalManager().getPlayerRentals(player.getUniqueId()).size();
        int maxRentals = plugin.getConfigManager().getMaxRentalsPerPlayer();
        
        if (currentRentals >= maxRentals) {
            player.sendMessage(plugin.getConfigManager().getMessage("max-rentals-reached"));
            return;
        }
        
        // Get price
        double price = plugin.getConfigManager().getPriceForRegion(regionName, world);

        // Check for permission-based pricing
        for (String perm : plugin.getConfigManager().getPermissionPrices().keySet()) {
            if (player.hasPermission(perm)) {
                price = plugin.getConfigManager().getPermissionPrices().get(perm);
                break;
            }
        }
        
        // Check economy
        Economy economy = plugin.getEconomy();
        if (economy == null) {
            player.sendMessage(MINI_MESSAGE.deserialize("<red>Economy system not available!"));
            return;
        }
        
        if (!economy.has(player, price)) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-enough-money",
                "{amount}", String.format(plugin.getConfigManager().getCurrencyFormat(), price)));
            return;
        }
        
        // Withdraw money
        economy.withdrawPlayer(player, price);
        
        // Create rental
        int days = plugin.getConfigManager().getDurationForRegion(regionName, world);
        if (plugin.getRentalManager().createRental(regionName, world, player, days, price)) {
            player.sendMessage(plugin.getConfigManager().getMessage("rental-success",
                "{region}", regionName,
                "{days}", String.valueOf(days),
                "{price}", String.format(plugin.getConfigManager().getCurrencyFormat(), price)));

            // Play sound
            player.playSound(player.getLocation(),
                org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            // Refund if rental failed
            economy.depositPlayer(player, price);
            player.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to create rental!"));
        }
    }
    
    private void handleExtendRental(Player player, String regionName, World world) {
        // Check permission
        if (!player.hasPermission("zonerental.extend")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        Rental rental = plugin.getRentalManager().getRental(regionName, world);

        if (rental == null) {
            player.sendMessage(MINI_MESSAGE.deserialize("<red>This region is not rented!"));
            return;
        }

        // Check if player owns this rental
        if (!rental.getPlayerUUID().equals(player.getUniqueId())) {
            player.sendMessage(MINI_MESSAGE.deserialize("<red>You don't own this rental!"));
            return;
        }

        // Check extension limit
        if (rental.getExtensionCount() >= plugin.getConfigManager().getMaxExtensions()) {
            player.sendMessage(plugin.getConfigManager().getMessage("max-extensions-reached"));
            return;
        }

        // Get extension price (same as rental price by default)
        double price = plugin.getConfigManager().getPriceForRegion(regionName, world);
        double multiplier = plugin.getConfig().getDouble("extension.price-multiplier", 1.0);
        price = price * multiplier;

        // Check economy
        Economy economy = plugin.getEconomy();
        if (economy == null) {
            player.sendMessage(MINI_MESSAGE.deserialize("<red>Economy system not available!"));
            return;
        }

        if (!economy.has(player, price)) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-enough-money",
                "{amount}", String.format(plugin.getConfigManager().getCurrencyFormat(), price)));
            return;
        }

        // Withdraw money
        economy.withdrawPlayer(player, price);

        // Extend rental
        int days = plugin.getConfigManager().getExtensionDuration();
        if (plugin.getRentalManager().extendRental(regionName, world, player, days, price)) {
            player.sendMessage(plugin.getConfigManager().getMessage("rental-extended",
                "{region}", regionName,
                "{days}", String.valueOf(days),
                "{price}", String.format(plugin.getConfigManager().getCurrencyFormat(), price)));

            // Play sound
            player.playSound(player.getLocation(),
                org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        } else {
            // Refund if extension failed
            economy.depositPlayer(player, price);
            player.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to extend rental!"));
        }
    }
    
    private void showRentalInfo(Player player, Rental rental) {
        player.sendMessage(MINI_MESSAGE.deserialize("<gold>=== Rental Info ==="));
        player.sendMessage(MINI_MESSAGE.deserialize("<yellow>Region: <white>" + rental.getRegionName()));
        player.sendMessage(MINI_MESSAGE.deserialize("<yellow>Owner: <white>" + rental.getPlayerName()));
        player.sendMessage(MINI_MESSAGE.deserialize("<yellow>Expires: <white>" + rental.getFormattedEndDate()));
        player.sendMessage(MINI_MESSAGE.deserialize("<yellow>Time Remaining: <white>" +
            rental.getDaysRemaining() + " days, " +
            (rental.getHoursRemaining() % 24) + " hours"));
        player.sendMessage(MINI_MESSAGE.deserialize("<yellow>Extensions Used: <white>" +
            rental.getExtensionCount() + "/" + plugin.getConfigManager().getMaxExtensions()));

        if (rental.getPlayerUUID().equals(player.getUniqueId())) {
            player.sendMessage(MINI_MESSAGE.deserialize("<green>Shift+Right-Click to extend your rental!"));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        // Protect rental signs if configured
        if (!plugin.getConfigManager().isSignProtection()) {
            return;
        }

        Location blockLoc = event.getBlock().getLocation();

        // Check if the block being broken is the sign itself
        if (event.getBlock().getState() instanceof Sign) {
            if (plugin.getSignManager().isRentalSign(blockLoc)) {
                // Check if player has admin permission
                if (!event.getPlayer().hasPermission("zonerental.admin.breaksign")) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("sign-protected"));
                }
            }
            return;
        }

        // Check if the block being broken is a support block for a rental sign
        String regionName = plugin.getSignManager().getSupportBlockRegion(blockLoc);
        if (regionName != null) {
            // This block supports a rental sign - protect it
            if (!event.getPlayer().hasPermission("zonerental.admin.breaksign")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("sign-support-protected"));
            }
        }
    }

    /**
     * A rental sign or support block was actually broken (not cancelled by us or another
     * plugin): drop the affected signs from signs.yml right away.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBroken(BlockBreakEvent event) {
        Location blockLoc = event.getBlock().getLocation();
        String breaker = event.getPlayer().getName();
        plugin.getSignManager().onSignBroken(blockLoc, breaker);
        // Signs on a broken support block pop off
        plugin.getSignManager().onSupportBlockBroken(blockLoc, breaker);
    }
}
