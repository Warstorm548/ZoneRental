# Changelog

All notable changes to ZoneRental will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## IMPORTANT: Project Rename Notice

**As of version 3.0.0, this project has been renamed from RegionRental to ZoneRental.**

All historical entries below refer to the project under its previous name "RegionRental".
Going forward, all references use the new name "ZoneRental".

**Key changes in the rename:**
- Plugin name: RegionRental -> ZoneRental
- Commands: /rr* -> /zr* (e.g., /rrcreatesign -> /zrcreatesign)
- Permissions: regionrental.* -> zonerental.* (e.g., regionrental.admin -> zonerental.admin)
- Data folder: plugins/RegionRental/ -> plugins/ZoneRental/
- JAR file: RegionRental-X.X.X.jar -> ZoneRental-X.X.X.jar
- Package: com.regionrental -> com.zonerental

---

## [3.3.0] - Multiple Signs per Rental Space

### Added

- **Several signs per region.** A region can have more than one rental sign (`signs.max-per-region`, default `3`, `-1` = unlimited). Every sign gets a per-region ID (`#1`, `#2`, …) stored in `signs.yml`; IDs are never reused. All signs of a region are redrawn together.
- **Signs in other worlds.** `/zrcreatesign world:shop1` puts a sign for a region anywhere, for example in a lobby world. Clicking it rents the region in its own world.
- **`/zrcreate <world:region>`** registers a region as a rental space without a sign (permission `zonerental.admin.create`). `/zrcreatesign` still registers the region automatically.
- **Removing single signs.** `/zrremove` while looking at a rental sign, or `/zrremove <world:region> <id>`, removes only that sign. The rental space and any rental are kept.
- **Environment protection** (`signs.environment-protection`, default `true`): rental signs and their support blocks survive explosions (creepers, TNT, beds, respawn anchors…), pistons, fire, endermen, withers, falling blocks, leaf decay and fading blocks.
- **`signs.load-chunks-for-updates`** (default `true`): set to `false` to redraw signs in unloaded chunks when the chunk loads instead of loading it.
- `/zrverify` lists rental spaces with no signs, a missing WorldGuard region or a missing world.

### Changed

- **`/zrremove <region>` no longer removes anything.** The full teardown is now `/zrremove all <world:region>`, so a forgotten argument can't wipe a region. It also works when the WorldGuard region was already deleted.
- **`signs.yml` layout.** Signs are numbered under their region and keep their own `world` field. Existing files are converted on first start; the original is saved as `signs.yml.pre-3.3.0.bak`. Restore it if you downgrade, because older versions can't read the new layout.
- Standing and hanging signs show the text on both sides (wall signs on the front). Both sides are cleared when a sign is registered.
- `signs.protect-signs` is now listed in the default `config.yml`.
- `/zrtp` teleports to the region's oldest sign.
- `signs.yml` is saved right after `/zrcreate`, `/zrcreatesign` and `/zrremove`, so `/zrreload` no longer forgets new signs.
- New registrations use the region ID exactly as WorldGuard has it, so `Shop1` and `shop1` can't become two rental spaces.
- Message changes: `sign-created` and `sign-removed` now take `{id}` and `{region}`; new keys `sign-not-found`, `sign-already-registered`, `sign-limit-reached`, `region-registered`, `region-already-registered`, `region-not-registered`.

### Fixed

- Creating a second sign for a region overwrote the first: only the newest sign was redrawn, clickable and protected, and the old support block stayed protected until a restart.
- `/zrremove` left the physical sign behind; it now breaks the sign and restores its support block (only when no other rental sign uses that block).
- Hanging signs protected the wrong support block.
- Signs broken by admins stayed in `signs.yml`; they are now dropped immediately. Signs that disappear in other ways are dropped after two failed redraws, with a console warning.
- Clicking a rental sign rebuilt the full sign list; sign lookups now use an index.
- The `/zrremove` summary printed the `region-removed` message as raw object text.

---

## [3.2.0] - Adventure API Migration

### Changed

- **Complete migration from ChatColor to Adventure API** - Migrated 548 ChatColor usages across 21 source files to use Paper's native Adventure Component API with MiniMessage format
  - All commands now use `sendMiniMessage()` extension for inline messages
  - ConfigManager now returns `Component` from `getMessage()` instead of legacy strings
  - Sign formats updated to use MiniMessage tags (`<red>`, `<green>`, `<gold>`, etc.)
  - All config.yml messages migrated from `&` color codes to MiniMessage format

- **Deprecated Bukkit API fixes** - Updated 5 deprecated API usages:
  - `player.sendTitle()` → `player.showTitle()` with Adventure Title
  - `itemMeta.setDisplayName()` → `itemMeta.displayName(Component)`
  - `itemMeta.lore = List<String>` → `itemMeta.lore(List<Component>)`

### Added

- **AdventureExtensions.kt** - New extension functions for Adventure API:
  - `String.toComponent()` - Parse MiniMessage format
  - `String.legacyToComponent()` - Convert legacy `&` codes to Component
  - `String.toMiniMessage()` - Convert legacy codes to MiniMessage tags
  - `CommandSender.sendMiniMessage()` - Send MiniMessage-formatted text

### Breaking Changes

- **Config format change** - `config.yml` now uses MiniMessage format instead of `&` color codes
  - Old: `prefix: '&8[&6ZoneRental&8]&r '`
  - New: `prefix: '<dark_gray>[<gold>ZoneRental<dark_gray>]<reset> '`
  - Existing configs will need to be updated or regenerated

---

## [3.1.3] - Deprecated Code Cleanup

### Removed

- **Unused Rental factory methods** - Removed `fromLegacyStorage()` and `fromMigration()` from Rental.kt (never called, migration handled inline)
- **Deprecated non-world-aware methods** - Removed 7 methods that were replaced by world-aware versions:
  - `SignManager.updateSign(String)`
  - `WorldEditManager.captureRegion(String)`
  - `WorldEditManager.restoreRegion(String)`
  - `WorldEditManager.hasCapture(String)`
  - `WorldEditManager.deleteCapture(String)`
  - `StorageManager.collectItemsFromRegion(String)`
  - `EzChestShopManager.removeShopsInRegion(String)`
- **Deprecated permission** - Removed `zonerental.admin.retime` (replaced by `zonerental.admin.duration`)

### Fixed

- **Dead code conditions** - Removed 3 conditions that were always true/false:
  - `CreateSignCommand.kt`: Redundant null check (getTargetBlock never returns null)
  - `SignManager.kt`: Two null checks in for loops (List<String> elements cannot be null)

### Changed

- **Documentation** - Updated CLAUDE.md WorldEdit Integration examples to use world-aware method signatures

---

## [3.1.2] - /zrremove Group Cleanup Fix

### Fixed

- **Regions not removed from groups when using `/zrremove`** - Fixed bug where removing a rental region did not remove it from any groups it belonged to
  - When `/zrremove <region>` is executed, the region is now automatically removed from its group (if any)
  - Prevents orphaned group memberships that could cause conflicts when re-adding regions
  - Individual region overrides are only removed if the region was NOT in a group (groups already clear individual overrides when joining)
  - Success message now shows group removal confirmation

---

## [3.1.1] - Fix Storage GUI Pagination Bugs

### Fixed

- **Storage retrieval GUI pagination broken on pages 2 and above** - Fixed critical bug where navigation buttons became moveable objects after navigating past page 1
  - **Root cause:** When opening a new page, `player.openInventory()` automatically closes the previous inventory, triggering `onInventoryClose` which removed the session from `activeGUISessions` before the new page could use it
  - **Fix:** Added `isTransitioning` flag to `StorageGUISession` that prevents the close handler from removing the session during page transitions
  - Navigation buttons now work correctly on all pages
  - Close button and item retrieval work on all pages

- **Items on pages 2+ lost when closing GUI** - Fixed critical bug where only items from the currently visible page were saved on close
  - **Root cause:** `getRemainingItemsFromGUI()` only read the 45 visible slots of the current page, ignoring items on other pages stored in `session.items`
  - **Fix:** Added `syncCurrentPageToSession()` method that syncs GUI state back to session before page transitions and on close
  - All remaining items across all pages now persist correctly when GUI is closed
  - Items consolidate automatically into fewer pages as items are taken

### Changed

- **StorageGUISession.kt**
  - Added `isTransitioning: Boolean` property for page transition tracking
  - Added `originalItemCount: Int` property for accurate "items taken" messaging

- **StorageManager.kt**
  - Added `syncCurrentPageToSession()` method to sync GUI state back to session
  - Updated navigation handlers to sync current page before changing pages
  - Updated `onInventoryClose()` to sync and save all items from session
  - Removed local `ITEMS_PER_PAGE` constant, now uses `StorageGUISession.ITEMS_PER_PAGE` as single source of truth
  - Removed unused `getRemainingItemsFromGUI()` method

---

## [3.1.0] - Async Region Scanning with MCCoroutine

Major performance update implementing async chunk-based region scanning for improved server performance during rental expiration.

### New Features

- **MCCoroutine 2.21.0 Integration** - Added Kotlin coroutine support for Bukkit
  - Plugin now extends `SuspendingJavaPlugin` for automatic coroutine lifecycle management
  - Enables non-blocking async operations during item/block scanning
  - TPS-aware throttling to prevent lag during large region scans

- **ChunkSnapshot Pre-filtering** - ~99% reduction in main thread `getBlock()` calls
  - Phase A: Scan `ChunkSnapshot` for container block types (async, off main thread)
  - Phase B: Access actual blocks only for found containers (main thread, minimal)
  - Dramatically reduces server impact for large rental regions

- **Dynamic Chunk Batching** - Batch size scales with region size
  - Tiny (<50 chunks): 15 chunks/batch, no delay
  - Small (<200 chunks): 10 chunks/batch, 25ms delay
  - Medium (<500 chunks): 8 chunks/batch, 50ms delay
  - Large (<1000 chunks): 4 chunks/batch, 100ms delay
  - Very Large (<2000 chunks): 2 chunks/batch, 150ms delay
  - Extreme (2000+ chunks): 1 chunk/batch, 200ms delay

- **Y-Level Aware Scanning** - Only scans actual region Y-bounds
  - Uses WorldGuard region min/max Y coordinates
  - Avoids scanning full world height (Y -64 to 320)
  - Significant performance improvement for regions with limited vertical extent

- **TPS Monitoring with Auto-Throttling** - Adaptive performance management
  - HEALTHY (TPS ≥ 19.5): Full speed scanning
  - WARNING (TPS ≥ 18.5): Reduced concurrency, doubled delays
  - CRITICAL (TPS < 18.5): Single chunk, 200ms delays, pause if TPS drops further

- **Region Size Limits** - Block rentals for excessively large regions
  - Configurable `max-rental-chunks` limit (default: 2000)
  - Prevents performance issues from extremely large regions
  - Clear error message when region exceeds limit

### New Files

- `src/main/kotlin/com/zonerental/async/ScanModels.kt` - Data classes for async scanning
  - `ScanRegion`, `ChunkCoord`, `BlockCoord`, `ChunkBatch`
  - `ScanStrategy` sealed class (Tiny/Small/Medium/Large/VeryLarge/Extreme)
  - `TpsLevel` enum and `AdjustedStrategy` for TPS-based throttling

- `src/main/kotlin/com/zonerental/async/TpsMonitor.kt` - TPS monitoring utility
  - Uses Paper's `Bukkit.getTPS()` API
  - Provides strategy adjustment based on current server load
  - Estimates scan time for large regions

- `src/main/kotlin/com/zonerental/async/AsyncScanService.kt` - Core async scan service
  - `createScanRegion()` - Creates scan region from WorldGuard bounds
  - `calculateChunkBatches()` - Splits region into TPS-friendly batches
  - `scanSnapshotForContainers()` - Finds container blocks in ChunkSnapshot
  - `scanSnapshotForChangedBlocks()` - Detects player-placed blocks

### Modified Files

- **ZoneRental.java** - Now extends `SuspendingJavaPlugin` for MCCoroutine
- **StorageManager.kt** - Added async ChunkSnapshot-based scanning methods
- **WorldEditManager.kt** - Added async file I/O for schematic operations
- **RentalManager.kt** - Added `expireRentalAsync()` suspend function
- **ExpirationManager.kt** - Uses `plugin.launch {}` for async expiration
- **ConfigManager.kt** - Added async-scanning configuration properties

### New Configuration

```yaml
async-scanning:
  enabled: true              # Enable async scanning for large regions
  min-chunks-for-async: 10   # Minimum chunks before using async
  tps-healthy-threshold: 19.5
  tps-warning-threshold: 18.5
  debug-async: false         # Enable debug logging for async operations
  max-rental-chunks: 2000    # Block rentals for regions larger than this
```

### Technical Details

- **Dependencies added:**
  - `mccoroutine-bukkit-api:2.21.0`
  - `mccoroutine-bukkit-core:2.21.0`
  - `kotlinx-coroutines-core:1.9.0`
- All new dependencies are relocated via ShadowJar to avoid conflicts
- Backward compatible - small regions still use synchronous scanning
- No changes to data formats or API

---

## [3.0.5] - Fix Retrieval GUI Navigation Buttons

### Fixed

- **Retrieval GUI missing navigation elements** - Close button and page indicator are now always visible
  - Previously, navigation buttons only appeared when there were more than 45 items (multiple pages)
  - Single-page GUIs now show page indicator (slot 49) and close button (slot 50)
  - Navigation arrows still only appear when there are multiple pages to navigate

### Changed

- **Consolidated StorageGUISession classes** - Removed duplicate inner class
  - Replaced `StorageManager.StorageGUISession` inner class with external model
  - Now uses `com.zonerental.models.StorageGUISession` for session tracking
  - Improved code organization and maintainability

---

## [3.0.4] - Complete Command Kotlin Migration

Internal refactoring: Migrated the final four Java command classes to Kotlin.

### Kotlin Conversions

- **RRCommand.kt** - Migrated from Java (467 → 444 lines, 5% reduction)
  - Multi-page help system with `when` expressions
  - String templates for dynamic prefix interpolation
  - Tab completion with filtered list operations

- **RetrieveCommand.kt** - Migrated from Java (37 → 28 lines, 24% reduction)
  - Kotlin smart cast for Player type check
  - Property access for plugin managers

- **RemoveCommand.kt** - Migrated from Java (132 → 124 lines, 6% reduction)
  - `buildString {}` instead of StringBuilder
  - Null-safe property access with `?.let {}`
  - World-aware region parsing preserved

- **ReloadCommand.kt** - Migrated from Java (27 → 20 lines, 26% reduction)
  - Simple command with idiomatic Kotlin patterns
  - Property access for config manager

### Technical Notes

- **All commands now in Kotlin** - Zero Java command classes remain
- **Total line reduction:** 663 lines Java → 616 lines Kotlin (7% reduction)
- No user-facing changes - all commands work identically
- `src/main/java/com/zonerental/commands/` directory is now empty

---

## [3.0.3] - Merge Member Commands

### Changed

- **Consolidated `/zrmembers` into `/zrmember list`** - Reduced command redundancy
  - `/zrmember add <region> <username>` - Add a member (unchanged)
  - `/zrmember remove <region> <username>` - Remove a member (unchanged)
  - `/zrmember list <region>` - View members (previously `/zrmembers <region>`)

### Kotlin Migration

- **MemberCommand.kt** - Migrated from Java and merged with MembersCommand
  - Combined MemberCommand.java (204 lines) + MembersCommand.java (142 lines) → MemberCommand.kt (268 lines)
  - World-aware parser support for all subcommands
  - Unified tab completion for all subcommands

### Removed

- **MemberCommand.java** - Replaced by Kotlin implementation
- **MembersCommand.java** - Merged into `/zrmember list` subcommand
- **`/zrmembers` command** - Use `/zrmember list` instead

### Notes

- Both permissions preserved for backward compatibility:
  - `zonerental.member` - For `add` and `remove` subcommands
  - `zonerental.members` - For `list` subcommand

---

## [3.0.2] - Fix Duplicate Command Registration

### Fixed

- **Command Registration Bug** - Fixed issue where `/zr` command was being registered twice
  - Plugin's own PluginCommand from plugin.yml was incorrectly detected as a conflict
  - This caused fallback to `/zr1` prefix instead of `/zr`
  - `checkPrefixConflicts()` now recognizes the plugin's own PluginCommand and skips it
  - `registerCommandWithPrefix()` now uses existing PluginCommand for main `/zr` command

- **Missing Subcommands in Conflict Check** - Added `member`, `members`, `tp`, `group` to the subcommands array in conflict detection

---

## [3.0.1] - Tier 3 Command Kotlin Migration

Internal refactoring: Migrated four command classes from Java to Kotlin.

### Kotlin Conversions

- **CreateSignCommand.kt** - Migrated from Java (65 → 53 lines, 18% reduction)
  - Player validation using `asPlayerOrNull()` extension
  - String templates for messages
  - Property access for plugin managers

- **InfoCommand.kt** - Migrated from Java (76 → 70 lines, 8% reduction)
  - String templates for cleaner output formatting
  - Property access for Rental fields
  - Elvis operator for null handling

- **ResetCommand.kt** - Migrated from Java (87 → 77 lines, 11% reduction)
  - Map indexing with type casting for refund details
  - String templates for messages and logging
  - Conditional logging with string interpolation

- **ExtendCommand.kt** - Migrated from Java (107 → 98 lines, 8% reduction)
  - Player validation using `asPlayerOrNull()` extension
  - Property access for Rental fields and config values
  - Vault economy integration preserved with null safety

---

## [3.0.0] - Project Rename to ZoneRental

Major version bump marking the complete rename of the project from RegionRental to ZoneRental.

### Breaking Changes

- **Plugin Renamed** - RegionRental is now ZoneRental
- **Commands Renamed** - All commands now use `/zr` prefix instead of `/rr`
  - `/rrcreatesign` -> `/zrcreatesign`
  - `/rrreset` -> `/zrreset`
  - `/rrremove` -> `/zrremove`
  - `/rrinfo` -> `/zrinfo`
  - `/rrlist` -> `/zrlist`
  - `/rrextend` -> `/zrextend`
  - `/rrretrieve` -> `/zrretrieve`
  - `/rrmember` -> `/zrmember`
  - `/rrmembers` -> `/zrmembers`
  - `/rrtp` -> `/zrtp`
  - `/rrduration` -> `/zrduration`
  - `/rroverride` -> `/zroverride`
  - `/rrgroup` -> `/zrgroup`
  - `/rrverify` -> `/zrverify`
  - `/rrreload` -> `/zrreload`
  - `/rrrefundhistory` -> `/zrrefundhistory`
- **Permissions Renamed** - All permissions now use `zonerental.` prefix instead of `regionrental.`
  - `regionrental.rent` -> `zonerental.rent`
  - `regionrental.extend` -> `zonerental.extend`
  - `regionrental.admin.*` -> `zonerental.admin.*`
  - (and all other permissions)
- **Data Folder Renamed** - Plugin data folder changed from `plugins/RegionRental/` to `plugins/ZoneRental/`
- **JAR File Renamed** - Output JAR is now `ZoneRental-3.0.0.jar`
- **Package Renamed** - Java/Kotlin package changed from `com.regionrental` to `com.zonerental`

### Migration Notes

- **Manual Migration Required** - Move your data folder from `plugins/RegionRental/` to `plugins/ZoneRental/`
- **Permission Updates Required** - Update all permissions in your permissions plugin from `regionrental.*` to `zonerental.*`
- **Command Aliases** - Update any command aliases or scripts that reference `/rr*` commands to use `/zr*`
- **Configuration Files** - Configuration files (config.yml, regions.yml, etc.) are compatible and do not need modification

---

## [2.9.3] - Tier 2 Command Kotlin Migration

Internal refactoring: Migrated three additional command classes from Java to Kotlin.

### Kotlin Conversions

- **RefundHistoryCommand.kt** - Migrated from Java (130 → 109 lines, 16% reduction)
  - Switch statement → Kotlin when expression
  - Loop with index → forEachIndexed
  - String templates for cleaner formatting

- **ListCommand.kt** - Migrated from Java (79 → 76 lines, 4% reduction)
  - Complex if/else chain → when expression with destructuring
  - Smart cast for Player type checking
  - Nullable handling with Pair returns

- **VerifyCommand.kt** - Migrated from Java (94 → 83 lines, 12% reduction)
  - Unchecked casts → safe casts with filterIsInstance
  - For loops → forEach
  - Type-safe map access with null defaults

### Technical Notes

- No user-facing changes - all commands work identically
- Total line reduction: ~12% across migrated commands
- Improved null safety and type handling

---

## [2.9.2] - Tier 1 Command Kotlin Migration

Internal refactoring: Migrated the three largest command classes from Java to Kotlin.

### Kotlin Conversions

- **DurationCommand.kt** - Migrated from Java (439 → 329 lines, 25% reduction)
  - Leverages existing DurationAction/DurationResult sealed classes
  - When expressions for action handling
  - TimeUtils integration for duration parsing

- **GroupCommand.kt** - Migrated from Java (777 → 599 lines, 23% reduction)
  - Data classes for PendingGroupAction and ParseResult
  - Collection functions for region parsing
  - Simplified tab completion with caching

- **TpCommand.kt** - Migrated from Java (463 → 310 lines, 33% reduction)
  - Smart casts for block data types
  - Companion object with material sets for block checks
  - Streamlined 3D safe location algorithm

### Technical Notes

- No user-facing changes - all commands work identically
- Total line reduction: ~26% across migrated commands
- Improved type safety and null handling

---

## [2.9.1] - Schematic Deletion Bug Fix

### Fixed

- **Schematic deletion bug in /rrremove command** - Fixed issue where schematic files were not being deleted when removing rental setup from a region. The `deleteCapture()` method now correctly uses the composite key format for schematic file lookup.

---

## [2.9.0] - Tier 2 Kotlin Migration

Major update completing the Kotlin migration by converting all remaining manager classes and utilities to Kotlin.

### Kotlin Conversions (Tier 2)

**Phase 1 - Utilities:**
- **WorldRegionParser.kt** - Migrated from Java
  - Idiomatic Kotlin with data classes and sealed results
  - Pattern matching for region parsing
- **ExpirationManager.kt** - Migrated from Java
  - Kotlin coroutine-style task scheduling
  - Safe operators for null handling
- **WorldGuardManager.kt** - Migrated from Java
  - Properties for region queries
  - Extension functions for WorldGuard API

**Phase 2 - Plugin Managers:**
- **EzChestShopManager.kt** - Migrated from Java
  - Reflection API wrapped with Kotlin safe calls
  - Cleaner API with extension functions
- **SignManager.kt** - Migrated from Java
  - Functional operations for sign updates
  - Support block handling with sealed classes

**Phase 3 - Storage Managers:**
- **WorldEditManager.kt** - Migrated from Java
  - Schematic operations with safe file handling
  - LRU cache implementation in Kotlin
- **StorageManager.kt** - Migrated from Java
  - Container scanning with functional operations
  - GUI session management with data classes

**Phase 4 - Core Manager:**
- **RentalManager.kt** - Migrated from Java
  - Central rental lifecycle in idiomatic Kotlin
  - ConcurrentHashMap with Kotlin extensions
  - Computed properties for rental queries

### Bug Fixes

- **Fixed nullable World in chargeDurationAdd method** - Proper null handling for world parameter
- **Fixed StorageGUIHolder getInventory return type** - Corrected interface implementation
- **Fixed SignManager Kotlin compilation errors** - Resolved type mismatches
- **Fixed ParsedRegion Java field access** - Added `@JvmField` annotations for Java interop
- **Fixed JVM signature clash in WorldGuardManager** - Resolved overload conflicts
- **Fixed WorldGuardManager property access** - Added proper Kotlin accessors

### Technical Details

- **10 manager classes migrated** from Java to Kotlin
- **Full Java interoperability** maintained via JVM annotations
- **Zero breaking changes** to API or data formats
- **Build verified** with Kotlin stdlib verification in CI

---

## [2.8.2] - Kotlin Runtime Fix (Complete)

### Fixed

- **Kotlin stdlib not bundled in JAR** - Fixed shadowJar configuration to explicitly include runtime dependencies. Added `configurations = listOf(project.configurations.runtimeClasspath.get())` to ensure Kotlin stdlib is properly shaded. Also disabled the default jar task to prevent conflicts.

---

## [2.8.1] - Kotlin Runtime Fix (Incomplete)

### Fixed

- Attempted fix for Kotlin stdlib bundling with `relocate` - insufficient on its own.

---

## [2.8.0] - Tier 1 Kotlin Migration

Continues the Kotlin migration by converting all configuration managers and the teleport cooldown manager.

### Kotlin Conversions (Tier 1)

- **ConfigManager.kt** - Migrated from Java (394 lines → 387 lines)
  - Properties with private setters replacing getter methods
  - Kotlin string templates for config paths

- **TeleportCooldownManager.kt** - Migrated from Java (129 lines → 81 lines, 37% reduction)
  - `mutableMapOf<UUID, Long>()` for cooldowns
  - Simplified with Elvis operator and safe calls

- **SignsConfig.kt** - Migrated from Java (367 lines → 301 lines, 18% reduction)
  - String templates for YAML paths
  - Safe operators for null handling

- **RegionsConfig.kt** - Migrated from Java (818 lines → 509 lines, 38% reduction)
  - Properties for query methods (`allRegions`, `allGroupsWithOverrides`)
  - Functional operations (`filter`, `associateWith`)

- **GroupsConfig.kt** - Migrated from Java (442 lines → 276 lines, 38% reduction)
  - Companion object for constants
  - Kotlin `Regex` instead of `Pattern`
  - `when` expression for validation

- **StorageConfig.kt** - Migrated from Java (208 lines → 169 lines, 19% reduction)
  - Safe cast with `@Suppress` for generic lists
  - `emptyList()` instead of `ArrayList()`

### Summary

- **6 files migrated** from Java to Kotlin
- **Total reduction:** 2,358 lines → 1,723 lines (27% reduction)
- Full Java interoperability maintained via generated getters

---

## [2.7.0] - Kotlin Migration

Major update introducing Kotlin for improved code quality, type safety, and reduced boilerplate.

### Kotlin Conversions

- **Rental.kt** - Full conversion from Java (300 lines → ~170 lines)
  - Data class with computed properties replacing 10+ getter methods
  - Factory methods (`Rental.create()`, `Rental.fromStorage()`) replacing 5 Java constructors
  - `@JvmStatic` and `@JvmOverloads` annotations for full Java interoperability
  - Nested `RefundRecord` data class for audit trail

- **OverrideCommand.kt** - Full rewrite with sealed classes (863 lines → ~350 lines, 60% reduction)
  - `OverrideSetting<T>` sealed class with 6 type-safe setting variants
  - Single generic handler replaces 6 nearly identical Java methods
  - `ParseResult<T>` sealed class for type-safe parsing results

### New Kotlin Files

**Extensions (5 files):**
- `StringExtensions.kt` - `color()`, `withPlaceholders()`, `parseCompositeKey()`
- `LocationExtensions.kt` - `Location.toKey()`, `String.toLocation()`, `String.toWorld()`
- `PlayerExtensions.kt` - `asPlayerOrNull()`, `requirePlayer()`, `requirePermission()`
- `CollectionExtensions.kt` - Filtering and mapping helpers

**Utilities:**
- `TimeUtils.kt` - Duration formatting, time parsing, `Int.days`/`Int.hours` extensions

**Data Models (4 files):**
- `RefundRecord.kt` - Standalone refund transaction model
- `ParsedRegion.kt` - World:region parsing with composite key support
- `StorageGUISession.kt` - Paginated GUI session for item retrieval
- `SupportBlockData.kt` - Sign support block capture/restore

**Configuration (2 files):**
- `RegionOverride.kt` - Type-safe override container with `merge()` and `withDefaults()`
- `MessageFormatter.kt` - DSL for message formatting with fluent API

**Commands:**
- `DurationAction.kt` - Sealed class for duration command actions

**Manager Extensions:**
- `ManagerExtensions.kt` - Collection extensions for rental operations

### Deleted Java Files

- `src/main/java/com/regionrental/managers/Rental.java` (replaced by Kotlin)
- `src/main/java/com/regionrental/commands/OverrideCommand.java` (replaced by Kotlin)

### Technical Details

- **15 Kotlin files created** across extensions, models, config, commands, and managers
- **2 Java files replaced** (Rental.java, OverrideCommand.java)
- **~1,100 lines of Java removed**, ~800 lines of Kotlin added (net reduction)
- **Full Java interoperability** maintained via `@JvmStatic`, `@JvmOverloads`, `@JvmField`
- **Zero breaking changes** to API or data formats
- **Build verified** on GitHub Actions

### Migration Notes

- Java code calling `new Rental(...)` should use `Rental.create()` or `Rental.fromStorage()`
- `rental.getExtensionCost()` → `rental.extensionCost` (Kotlin property)
- `rental.isExpired()` → `rental.isExpired` (Kotlin property)
- All existing data files (rentals.yml, etc.) remain compatible

---

## [2.6.0] - Schematic Format Migration

Major update fixing critical WorldEdit schematic serialization bug and migrating to industry-standard Sponge format.

### Critical Bug Fix
- **Fixed Schematic Serialization** - Replaced broken Java serialization with Sponge Schematic format
  - **Previous Bug:** `SerializableClipboard` class had `transient` clipboard field, resulting in empty .dat files
  - **Impact:** All .dat schematics from v2.5.1 and earlier contain NO block data
  - Restoration only worked if clipboard remained in memory cache
  - Server restarts caused silent data loss

### Changes
- **Sponge Schematic Format** - Now uses WorldEdit's official `.schem` format
  - Industry-standard format used by WorldEdit, FastAsyncWorldEdit, and other tools
  - Properly persists blocks, entities, and metadata across server restarts
  - Files can be imported/exported to other WorldEdit-compatible tools
  - Better compression and smaller file sizes

### Migration
- **Automatic Migration** - Handles legacy .dat files gracefully
  - Detects and warns about empty .dat files at startup
  - Auto-deletes legacy files if `restoration.auto-delete-schematics: true`
  - Clear logging explains the bug and migration process
  - Regions will be automatically re-captured on next rental

### Technical Details
- Added imports: `ClipboardFormat`, `ClipboardFormats`, `ClipboardReader`, `ClipboardWriter`, `BuiltInClipboardFormat`
- Removed: Broken `SerializableClipboard` wrapper class
- Updated methods: `saveSchematic()`, `loadSchematic()`, `indexSchematics()`, `deleteSchematic()`
- File extension: `.dat` → `.schem`

### Upgrade Notes
- **Existing Installations:**
  - All existing .dat files are empty and will be ignored
  - Regions will be re-captured automatically when next rented
  - Set `restoration.auto-delete-schematics: true` to clean up legacy .dat files
- **No Config Changes Required:** Migration is automatic
- **No Data Loss:** The .dat files were already empty, so nothing is lost

---

## [2.5.1] - PR Review Fixes

Patch release addressing code review feedback with bug fixes, thread safety improvements, and code quality enhancements.

### Bug Fixes
- **Cache Invalidation** - Fixed `invalidateGroupCache()` never being called after group modifications
  - Added calls to: `processCreateGroup`, `handleDelete`, `processAddRegions`, `processRemoveRegions`
  - Tab completion now immediately reflects group changes

### Thread Safety Improvements
- **WorldEditManager** - Made collections thread-safe for potential concurrent access
  - `knownSchematics`: Changed from `HashSet` to `ConcurrentHashMap.newKeySet()`
  - `clipboardCache`: Wrapped `LinkedHashMap` with `Collections.synchronizedMap()`

### Code Quality
- **Cleaner Save Pattern** - Refactored save methods across 5 files
  - Moved `isDirty = false` outside try block with early return on failure
  - More explicit success-only flag reset
  - Files: StorageConfig, RegionsConfig, SignsConfig, GroupsConfig, RentalManager

### Technical Details
- Files modified: 7 files
- ~43 lines changed (+31/-12)
- Backward compatible

---

## [2.5.0] - Performance Optimization

Minor update with comprehensive performance optimizations to reduce disk I/O, memory usage, and CPU overhead.

### Optimizations
- **Dirty Tracking** - Saves only happen when data actually changes (80-90% disk I/O reduction)
  - Applied to: RentalManager, SignsConfig, StorageConfig, RegionsConfig, GroupsConfig
  - `markDirty()` replaces immediate `save()` calls
  - `saveIfDirty()` called during auto-save task
- **Lazy Schematic Loading** - Schematics loaded on-demand with LRU cache (major RAM reduction)
  - Startup only indexes schematic files, doesn't load them
  - LRU cache evicts least-recently-used schematics when full
  - Configurable cache size: `restoration.schematic-cache-size: 20`
- **O(1) Support Block Lookups** - HashMap index instead of O(n) scan on block breaks
  - `supportBlockIndex` maps "world:x:y:z" → "world:region"
  - Rebuilt on sign config load
- **O(1) Player/Member Lookups** - Secondary indexes for rental queries
  - `playerRentalIndex`: UUID → Set of composite keys
  - `memberRentalIndex`: UUID → Set of composite keys
  - `getPlayerRentals()` and `getRentalsWhereMember()` now O(1)
- **Memory Leak Fixes** - GUI sessions cleaned on disconnect, cooldown cleanup task
  - `PlayerQuitEvent` handler removes GUI sessions
  - Periodic task cleans expired teleport cooldowns (every 10 minutes)
- **EnumSet for Container Types** - O(1) contains() instead of O(n) ArrayList
- **Tab Completion Caching** - 5-second TTL cache for group names in commands

### New Configuration
```yaml
restoration:
  schematic-cache-size: 20  # Max schematics in memory (LRU eviction)
```

### Technical Summary
- Files modified: 10+ files across managers, configs, and commands
- ~500 lines of optimization code
- Backward compatible with existing data
- Zero breaking changes

---

## [2.4.1] - Teleportation Safe Location Bug Fix

Patch release fixing critical teleportation bugs with enhanced 3D search algorithm.

### Bug Fixes
- **Wall Sign Teleportation** - Fixed "no safe location found" error for wall-mounted signs
  - Original algorithm only searched upward from sign level
  - New 3D search algorithm searches forward, down, and up
- **Standing Sign Direction** - Fixed standing signs using player's facing instead of sign rotation

### Enhanced 3D Search Algorithm
- **Forward Search** - Searches 1-20 blocks in front of sign (configurable)
- **Downward Floor Search** - Searches up to 20 blocks down for solid floor
- **Upward Floor Search** - Searches up to 20 blocks up as fallback

### New Configuration
```yaml
teleport:
  forward-search-distance: 5   # How many blocks forward to search
  floor-search-down: 20        # How many blocks down to search for floor
  floor-search-up: 20          # How many blocks up to search for floor
```

---

## [2.4.0] - Teleportation System

Minor update adding teleportation feature for rental owners and members.

### New Features
- **Teleport Command** - `/rrtp <region>` allows teleporting to rented regions
  - Works for rental owners and members
  - Cross-world teleportation support
  - Safe location detection algorithm
- **Cooldown System** - Prevents spam teleporting (configurable, default: 30s)
- **Effects & Feedback** - Enderman teleport sound and portal particles (configurable)

### Configuration
```yaml
teleport:
  enabled: true
  max-search-distance: 20
  cooldown: 30
  cross-world-warning: true
  sound-enabled: true
  particle-enabled: true
```

---

## [2.3.0] - Member Management System

Minor update adding complete member management for rented regions.

### New Features
- **Member Management Commands**
  - `/rrmember add <region> <username>` - Add member to rented region
  - `/rrmember remove <region> <username>` - Remove member from rented region
  - `/rrmembers <region>` - List all members of a region
- **WorldGuard Integration** - Members added to WorldGuard region members
- **Configurable Limits** - `members.max-members: 5` (-1 for unlimited)
- **Access Control** - Only renter can add/remove members

### Configuration
```yaml
members:
  enabled: true
  max-members: 5  # -1 for unlimited
```

---

## [2.2.1] - Sign Update Bug Fix

Patch release fixing missing sign updates when regions are removed from groups.

### Bug Fixes
- **Sign Update Edge Cases** - Signs now update immediately when group membership changes
  - Fixed: Signs not updating when regions are removed from groups (`/rrgroup edit <name> remove`)
  - Fixed: Signs not updating when groups are deleted (`/rrgroup delete <name>`)
  - Fixed: Signs not updating when regions are added to groups with existing overrides
  - Added: `bulkMarkSignsDirty()` calls in 4 group command methods

### Technical Details
- Modified `GroupCommand.java` to mark signs as dirty after group membership changes
- Signs now update within 30 seconds (next update cycle) instead of showing stale data
- No performance impact (dirty tracking uses efficient HashSet operations)
- Ensures override lookup priority works correctly: Group → Region → Default

---

## [2.2.0] - Region Grouping System

Minor update adding comprehensive region grouping and mass override management.

### New Features
- **Region Grouping System** - Group multiple regions together for unified configuration
  - `/rrgroup create <name> [regions]` - Create region groups
  - `/rrgroup edit <name> add/remove [regions]` - Modify group membership
  - `/rrgroup delete <name>` - Delete groups with confirmation
  - `/rrgroup list` and `/rrgroup view <name>` - Browse groups
  - Multi-world support (regions from different worlds in same group)
  - Hybrid command interface (inline arguments OR chat prompts with 60s timeout)

- **Mass Override Operations** - Set overrides once for entire group
  - `group:` prefix parser resolves naming conflicts (`/rroverride price group:shops 1000`)
  - Override lookup priority: Group → Region → Default
  - Bulk sign updates (all group signs update together)
  - Automatic individual override cleanup when adding to groups

- **Enhanced Tab Completion** - Smart suggestions for all commands
  - `group:` prefix suggestions in override commands
  - Region name completion from WorldGuard
  - World prefix completion (`world:`, `world_nether:`)
  - Context-aware suggestions (player's world vs explicit world)

### Validation & Edge Cases
- Exclusive group membership (regions can only be in one group)
- Duplicate prevention (cannot add if already in another group)
- Group name validation (2-30 chars, alphanumeric + underscore, reserved names blocked)
- Region/world existence checks before adding
- Automatic cleanup on group deletion (removes group overrides from regions.yml)

### Technical Summary
- **Files created:** 3 (GroupsConfig.java, GroupCommand.java, GroupChatListener.java)
- **Files modified:** 5 (RegionRental.java, OverrideCommand.java, SignManager.java, RegionsConfig.java, CLAUDE.md)
- **Lines added:** ~2000+ lines across 5 implementation phases
- **Data files:** groups.yml (new), regions.yml (extended with group section)
- **Documentation:** Comprehensive grouping system docs added to CLAUDE.md

---

## [2.1.0] - Command Prefix Registration Cleanup

Minor update cleaning up command prefix registration behavior.

### Changes
- **Command Prefix Fix** - Removed duplicate /rr command registration when custom prefix is configured
  - Custom prefix users will only have their configured prefix registered (no more /rr fallback)
  - Default prefix users (`prefix: 'rr'`) are unaffected
  - Conflict resolution still works (falls back to 'rr' or auto-generated suffix)
- **Console Message Cleanup** - Removed misleading log messages about fallback /rr registration
  - Removed: "Fallback 'rr' prefix also registered for backward compatibility" (line 352)
  - Removed: "Fallback 'rr' will be registered when conflicts resolve" (line 360)

### Technical Summary
- **Files modified:** 1 file (RegionRental.java)
- **Lines removed:** 20 lines (18 from registration block + 2 misleading messages)
- **Breaking change:** Users who configured custom prefix but used /rr commands must switch to custom prefix
- **Benefits:** Cleaner command registration, less namespace pollution, more intuitive behavior

---

## [2.0.1] - Critical Bug Fixes and Performance Optimizations

### Bug Fixes
- **Fixed rental sign interaction error**: Resolved "region world:shop1 not found" bug
  - Properly parses composite keys to extract region names
  - Sign interactions now work correctly across all worlds
- **Prevented IndexOutOfBoundsException crashes**: Added comprehensive bounds checking
  - WorldRegionParser null and bounds validation
  - Command argument validation (RRCommand, DurationCommand)
  - Storage manager GUI pagination bounds checking
- **Prevented NullPointerException errors**: Added null safety checks
  - Sign format list validation before iteration
  - Individual format line null checks

### Performance Improvements
- **WorldGuard operations: 3x-10x faster**
  - Removed 9 deprecated methods with multi-world iteration (133 lines)
  - Direct world-specific lookups instead of scanning all worlds
- **Rental lookups: 100x-1000x faster**
  - Removed 8 deprecated O(n) search methods (82 lines)
  - Direct hash map access using composite keys for O(1) performance
- **Sign updates: 90%+ overhead reduction**
  - Dirty tracking system updates only changed signs
  - No longer updates all signs every 30 seconds
- **Thread safety improvements**
  - Synchronized storage manager GUI pagination
  - Prevents concurrent modification exceptions

### Technical Details
- Total: ~215+ lines of deprecated code removed
- 15 potential crash/error scenarios eliminated
- 7 commits with comprehensive testing
- All optimizations maintain backward compatibility
- Zero breaking changes to API or data formats

---

## [2.0.0] - Multi-World Support

### Complete Multi-World System
- **World-Aware Rentals**: Each rental now tracks and operates in its specific world
- **Fixed Critical Bug**: WorldEdit operations now use correct world instead of hardcoded first world
- **Composite Key System**: Rentals identified by `worldName:regionName` format for uniqueness
- **Automatic Migration**: Existing rentals seamlessly upgraded to multi-world format on first load
- **Performance Improvements**: Direct world-specific lookups eliminate inefficient world scanning
- **Zero Breaking Changes**: Fully backward compatible with existing rental data

### Updated Components
- **Rental Class**: Added `worldName` field and composite key support
- **RentalManager**: All CRUD operations now world-aware with deprecated fallback methods
- **WorldEditManager**: Fixed hardcoded world bug, now captures/restores in correct world
- **WorldGuardManager**: Added direct world-specific methods for O(1) performance
- **SignManager**: Sign updates now world-aware based on rental's world
- **SignInteractListener**: Player interactions use player's current world automatically
- **CreateSignCommand**: Validates regions in player's current world

### Data Migration
- Existing `rentals.yml` automatically detected and migrated
- Old rentals default to first world (usually "world")
- Migration count logged on startup for transparency
- Data immediately re-saved in new composite key format
- No manual intervention or downtime required

### Example Use Cases
- Set up identical rental shops in overworld and nether
- Different rental regions per dimension (mining claims in end, shops in overworld)
- WorldEdit captures/restores blocks in correct world
- No more world collisions or unexpected behavior

---

## [1.3.3] - EzChestShop Integration

### EzChestShop Plugin Support
- **Automatic shop removal** on rental expiration for seamless cleanup
- **Runtime detection** using reflection-based API (no compile-time dependency required)
- **Smart cleanup approach** using block break/replace for reliable shop and hologram removal
- **Conditional restoration** - Only restores chest blocks if WorldEdit restoration is disabled
- **Configurable notifications** - Optional player notifications when shops are removed
- **Perfect timing** - Shops removed after storage scan but before WorldEdit restoration
- **Compatible with EzChestShop 1.9.2+** with automatic version detection

### Technical Implementation
- **Reflection API** for accessing EzChestShop's internal ShopContainer class
- **Block break/replace pattern** triggers EzChestShop's BlockBreakEvent for automatic cleanup
- **No hologram glitches** - EzChestShop's event handler removes all holograms automatically
- **Inventory protection** - Clears chest inventory before breaking (items already saved)
- **3-tick delay** for chest restoration when WorldEdit is disabled
- **Configuration options** in config.yml under `integration.ezchestshop`

### Configuration Options
```yaml
integration:
  ezchestshop:
    enabled: true                    # Enable/disable integration
    notify-on-removal: true          # Notify player when shops removed
    removal-message: '&eChest shops in &6{region}&e have been removed.'
```

---

## [1.1.0] - Command-Based Override System

### Region Override Command System
- **New command** `/rroverride` for setting per-region custom rental settings
- **8 subcommands**: price, duration, maxextensions, extensionprice, allowextensions, extensionduration, remove, list
- **In-game configuration** - No manual file editing required
- **Defaults-first approach** - Regions not in regions.yml use config.yml defaults
- **Immediate updates** - Signs automatically update when overrides are changed
- **Tab completion** - Full tab completion support for all subcommands and region names
- **WorldGuard validation** - Prevents setting overrides for non-existent regions

### Updated Verification System
- **Enhanced `/rrverify`** now shows breakdown of defaults vs custom overrides
- **Reports orphaned configs** - Configs without signs (can be cleaned up)
- **No longer auto-repairs** - Only reports status, admins manage via commands

### Removed Auto-Population
- **Simplified workflow** - `/rrcreatesign` no longer auto-populates regions.yml
- **Cleaner files** - regions.yml only contains intentional overrides
- **Better clarity** - Clear which regions use defaults vs custom settings

---

## [1.0.0] - Initial Release

### Per-Region Configuration System (regions.yml)
- **Config file** `regions.yml` for managing per-region override settings
- **Migration system** automatically moves old `regions:` data from config.yml
- **Partial overrides** - Only specify settings you want to change

### Command Consolidation - Duration Management
- **Removed** `RetimeCommand` - functionality merged into `DurationCommand`
- **Enhanced** `/rrduration` command now supports:
  - `add` - Add time to rental
  - `remove` - Remove time from rental
  - `set` - Set absolute duration
  - `reset` - Reset to default duration with optional extension refund
- **Extension refund system** - Optionally refunds extension costs when resetting duration
- **Config option** `extension.refund-on-duration-reset` controls refund behavior
- **Simplified interface** - No longer requires player name, only region name

### Refund History Tracking
- **New command** `/rrrefundhistory <region>` to view complete refund transaction history
- **Automatic tracking** of all refunds (resets, duration changes, removals)
- **Detailed logging** with timestamps, amounts, and reasons
- **Double-refund prevention** system tracks what has been refunded

### Enhanced Support Block Protection
- **Auto-migration** of existing signs to include support block protection
- **Improved data storage** for original block type and orientation
- **Restoration system** properly restores support blocks when using `/rrremove`
