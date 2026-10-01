# Conventions & How-To

## Accessing managers

```kotlin
// Kotlin
plugin.rentalManager.getRental(regionName, world)
plugin.configManager.getPriceForRegion(regionName, world)
```
```java
// Java
ZoneRental plugin = ZoneRental.getInstance();
plugin.getRentalManager().getRental(regionName, world);
```

All region-scoped APIs take a `World`. There are no single-argument (world-less) variants.

## Common calls (current signatures)

```kotlin
// WorldGuard: always go through WorldGuardManager
worldGuardManager.regionExists(regionName, world)
worldGuardManager.addPlayerToRegion(regionName, world, uuid)
worldGuardManager.removePlayerFromRegion(regionName, world, uuid)

// WorldEdit snapshots
worldEditManager.captureRegion(regionName, world)
worldEditManager.restoreRegion(regionName, world)
worldEditManager.hasCapture(regionName, world)
worldEditManager.deleteCapture(regionName, world)

// Rentals
rentalManager.createRental(regionName, world, player, days, price)
rentalManager.resetRentalWithRefund(regionName, world)   // returns Map<String, Any>? (playerName, refundAmount, ...)
rentalManager.issueRefund(rental, amount, reason, adminName) // capped at rental.netRefundableAmount

// Config
configManager.getPriceForRegion(region, world)     // group → region → default
configManager.getDurationForRegion(region, world)
configManager.isBlockRestoration                    // Java: isBlockRestoration()
```

`Rental` is created only through factories: `Rental.create(...)` and `Rental.fromStorage(...)`. Useful properties: `compositeKey`, `isExpired`, `timeRemaining`, `daysRemaining`, `hoursRemaining`, `extensionCost`, `netRefundableAmount`.

## Persisting changes

Managers keep data in memory and mark it changed; autosave writes changed files every 5 minutes.

- `RentalManager.saveAllRentals()` **does nothing unless the manager is already marked changed**. Changing `rental.endDate` directly doesn't mark it. Go through a `RentalManager` method that calls `markDirty`, or add one.
- Config classes (`RegionsConfig`, `SignsConfig`, …) mark themselves changed in every setter.

## Player-facing text

- Configurable: add a key under `messages:` in `config.yml` **and** a default in `ConfigManager.loadMessages()`, then `sender.sendMessage(plugin.configManager.getMessage("key", "{region}", name))`.
- Fixed text: `sender.sendMiniMessage("<red>Something")` (Kotlin) or a `MiniMessage` instance (Java).
- Don't use `ChatColor` or `&` codes (removed in 3.2.0).

## Adding a command

1. Create `src/main/kotlin/com/zonerental/commands/XCommand.kt` implementing `CommandExecutor` (and `TabCompleter` if needed). Check permissions inside `onCommand` too.
2. Register it in `ZoneRental.registerCommands()` with `registerCommandWithPrefix(commandMap, activePrefix, "x", new XCommand(this), "zonerental.<perm>")`.
3. Add `"x"` to the `subcommands` array in `ZoneRental.checkPrefixConflicts()`.
4. Declare the permission in `plugin.yml` under `permissions:` (and in `zonerental.user.*` / `zonerental.admin.*` children). **Don't** add the command under `commands:`; only `zr` lives there.
5. Optionally add it to the `RRCommand` help pages.
6. Document it in [commands.md](../user-guide/commands.md) and [permissions.md](../user-guide/permissions.md).

## Adding a config option

1. Add the default to `src/main/resources/config.yml`.
2. Read it in `ConfigManager.kt`, either as a cached `var` loaded in `loadConfig()` (refreshed on `/zrreload`) or as a computed `val` reading `config` live.
3. Document it in [config-reference.md](../configuration/config-reference.md).

## Container types

The container list is hard-coded twice: `StorageManager.CONTAINER_TYPES` and `AsyncScanService.CONTAINER_TYPES`. Update both. The `storage.container-types` config key isn't read.

## Language

New code goes in Kotlin under `src/main/kotlin/com/zonerental/`. Java is limited to the main class and two listeners. Kotlin properties appear as getters in Java (`rental.getCompositeKey()`, `rental.isExpired()`).
