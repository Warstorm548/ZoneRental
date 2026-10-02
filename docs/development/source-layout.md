# Source Layout

**3 Java files + 51 Kotlin files** under `src/main/`. New code should be written in Kotlin.

Tests live in `src/test/kotlin` (unit suite, runs in CI) and `src/mockTest/kotlin` (MockK + MockBukkit, local only). See [Automated tests](../testing/automated-tests.md).

```
src/main/
├── java/com/zonerental/
│   ├── ZoneRental.java                 # Main class (SuspendingJavaPlugin), command registration, tasks
│   └── listeners/
│       ├── SignInteractListener.java   # Sign rent/info/extend + break protection
│       └── GroupChatListener.java      # /zrgroup chat prompts
├── kotlin/com/zonerental/
│   ├── async/
│   │   ├── AsyncScanService.kt         # ChunkSnapshot scanning, CONTAINER_TYPES (copy #2)
│   │   ├── ScanModels.kt               # ScanRegion, ChunkCoord, BlockCoord, ScanStrategy, TpsLevel, ScanResult
│   │   └── TpsMonitor.kt               # TPS-based throttling
│   ├── commands/                       # One executor per command
│   │   ├── RRCommand.kt                # /zr help (+ reload; list/info are stubs)
│   │   ├── CreateCommand.kt  CreateSignCommand.kt  DurationCommand.kt  ExtendCommand.kt  GroupCommand.kt
│   │   ├── InfoCommand.kt  ListCommand.kt  MemberCommand.kt  OverrideCommand.kt
│   │   ├── RefundHistoryCommand.kt  ReloadCommand.kt  RemoveCommand.kt  ResetCommand.kt
│   │   ├── RetrieveCommand.kt  TpCommand.kt  VerifyCommand.kt
│   │   └── DurationAction.kt           # UNUSED sealed classes (planned refactor)
│   ├── config/
│   │   ├── ConfigManager.kt            # config.yml + messages (MiniMessage → Component)
│   │   ├── RegionsConfig.kt            # regions.yml (region + group overrides)
│   │   ├── GroupsConfig.kt             # groups.yml
│   │   ├── SignsConfig.kt              # signs.yml: rental spaces, numbered signs, location/support/chunk indexes
│   │   ├── StorageConfig.kt            # storage.yml
│   │   ├── RegionOverride.kt           # UNUSED
│   │   └── MessageFormatter.kt         # UNUSED
│   ├── extensions/
│   │   ├── AdventureExtensions.kt      # toComponent(), legacyToComponent(), toMiniMessage()
│   │   ├── PlayerExtensions.kt         # asPlayerOrNull(), checkPermission(), sendMiniMessage()
│   │   ├── StringExtensions.kt         # color() (deprecated), withPlaceholders()
│   │   ├── LocationExtensions.kt       # mostly unused
│   │   └── CollectionExtensions.kt     # unused
│   ├── listeners/
│   │   └── SignProtectionListener.kt   # explosions/pistons/fire/... protection + chunk-load redraws
│   ├── managers/
│   │   ├── RentalManager.kt  Rental.kt  SignManager.kt  StorageManager.kt
│   │   ├── WorldEditManager.kt  WorldGuardManager.kt  ExpirationManager.kt
│   │   ├── EzChestShopManager.kt  TeleportCooldownManager.kt
│   │   └── ManagerExtensions.kt        # unused helper extensions for rental collections
│   ├── models/
│   │   ├── ParsedRegion.kt             # used by OverrideCommand
│   │   ├── StorageGUISession.kt        # retrieval GUI pages (ITEMS_PER_PAGE = 45)
│   │   ├── RentalSign.kt               # one sign in signs.yml (region world/name, ID, sign world, support block)
│   │   ├── RefundRecord.kt             # UNUSED (Rental.RefundRecord is used instead)
│   │   └── SupportBlockData.kt         # UNUSED
│   └── util/
│       ├── TimeUtils.kt                # ms constants, formatDuration ("7 days 3 hours"), Int.days
│       └── WorldRegionParser.kt        # "region" / "world:region" parsing
└── resources/
    ├── plugin.yml                      # zr command + permissions
    └── config.yml                      # default config (MiniMessage)
```

## Dead code

Never referenced from live code paths: `RegionOverride.kt`, `MessageFormatter.kt`, `DurationAction.kt`, `models/RefundRecord.kt`, `models/SupportBlockData.kt`, most of `CollectionExtensions.kt`, `LocationExtensions.kt` and `ManagerExtensions.kt`, both `findWorldForRegion` helpers (`StorageManager`, `EzChestShopManager`), `EzChestShopManager.notifyPlayer`, `StorageConfig.cleanupOldStorage`, the four `ConfigManager.get*ForRegion` extension helpers, and `TimeUtils.parseTimeString`. Don't copy patterns from these files as if they were current conventions.
