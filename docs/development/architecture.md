# Architecture

## Entry point

`ZoneRental.java` extends `SuspendingJavaPlugin` (MCCoroutine). It's a singleton (`ZoneRental.getInstance()`) and exposes every manager through getters, which are Kotlin properties when called from Kotlin (`plugin.rentalManager`).

`onEnable` order: configs → dependency check → Vault economy → managers (WorldGuard, WorldEdit, EzChestShop, TeleportCooldown, Rental, Sign, Storage, Expiration) → load rentals and signs → register commands → register listeners → start tasks.

`onDisable` saves rentals, signs, storage, regions and groups, then cancels tasks.

`reloadPlugin()` (`/zrreload`) re-reads `config.yml`, `signs.yml`, `storage.yml` and `regions.yml` from disk **without saving first**, rebuilds the EzChestShop hook and teleport cooldowns, then reloads rentals from the last saved snapshot and redraws signs. `groups.yml` is not reloaded.

## Components

| Component | File | Responsibility |
|---|---|---|
| `RentalManager` | `managers/RentalManager.kt` | Rentals in a `ConcurrentHashMap<compositeKey, Rental>` plus owner and member indexes. Create, extend, expire (sync + `expireRentalAsync`), refunds, members, `rentals.yml` persistence with change tracking. |
| `Rental` | `managers/Rental.kt` | Rental data (factories `Rental.create` / `Rental.fromStorage`), refund history, members, in-memory warning flags. |
| `SignManager` | `managers/SignManager.kt` | Sign creation, support-block detection, `removeRegionSetup`, redrawing signs that changed. |
| `WorldGuardManager` | `managers/WorldGuardManager.kt` | Region lookup and member add/remove. Always go through it rather than calling the WorldGuard API directly. |
| `WorldEditManager` | `managers/WorldEditManager.kt` | Snapshot capture/restore, `.schem` I/O, LRU cache. |
| `StorageManager` | `managers/StorageManager.kt` | Container and player-block collection (sync + async), retrieval GUI and its inventory listeners. Registers itself as a listener. |
| `ExpirationManager` | `managers/ExpirationManager.kt` | Periodic expiry check and warnings. |
| `EzChestShopManager` | `managers/EzChestShopManager.kt` | Reflection-based shop removal. |
| `TeleportCooldownManager` | `managers/TeleportCooldownManager.kt` | In-memory `/zrtp` cooldowns. |
| `AsyncScanService`, `TpsMonitor`, `ScanModels` | `async/` | ChunkSnapshot scanning, batch strategy, TPS-based throttling. |
| `ConfigManager` | `config/ConfigManager.kt` | Reads `config.yml`; `getMessage()` returns an Adventure `Component`. |
| `RegionsConfig` / `GroupsConfig` / `SignsConfig` / `StorageConfig` | `config/` | YAML data files with change tracking (`saveIfDirty`). `SignsConfig` keeps an index of support-block locations. |
| `SignInteractListener` | `listeners/` (Java) | Sign clicks (rent/info/extend) and sign / support-block break protection. |
| `GroupChatListener` | `listeners/` (Java) | Chat input for `/zrgroup` prompts. |
| Commands | `commands/` | One `CommandExecutor` (most also `TabCompleter`) per command. |

## Command registration

Only `zr` is declared in `plugin.yml`. `ZoneRental.registerCommands()` takes the `CommandMap` by reflection and registers a `DynamicCommand` named `<prefix><sub>` for each executor, with a permission set. The sub-command names are also listed in `checkPrefixConflicts()` for conflict detection. See [Commands → prefix](../user-guide/commands.md#command-prefix).

## Data keys

`world:region` composite keys everywhere (rentals, signs, regions, groups, schematics), except `storage.yml`, which uses the region name only. Use `WorldRegionParser` (`util/`) to parse user input and to split keys.

## Threading model

- Bukkit scheduled tasks (`runTaskTimer`) all run on the **main thread**: the expiry check, sign redraw, autosave, cooldown cleanup and group-prompt cleanup.
- Expiry launches `plugin.launch { expireRentalAsync(...) }`, a coroutine on MCCoroutine's **main-thread dispatcher**. Scanning is split up by `delay()` rather than moved to another thread. `withContext(Dispatchers.IO)` is used for `storage.yml` writes and `.schem` load/save/delete.
- `/zrreset` and `/zrremove` use the synchronous path. For large regions it calls the suspend scanners through `runBlocking` on the main thread.
- `ConcurrentHashMap` is used for rentals and indexes. `YamlConfiguration` objects are not thread-safe.

## Message pipeline

Since 3.2.0 all player-facing text is Adventure `Component`s:

- Configurable text: `ConfigManager.getMessage(key, "{ph}", value, ...)` → prefix + placeholder replacement → `MiniMessage.deserialize`.
- Inline text: `Audience.sendMiniMessage("<red>...")` / `String.toComponent()` (`extensions/AdventureExtensions.kt`, `PlayerExtensions.kt`). Java classes use a `MiniMessage` instance directly.
- `String.color()` / legacy helpers remain but are `@Deprecated`.
