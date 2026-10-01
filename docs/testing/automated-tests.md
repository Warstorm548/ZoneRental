# Automated Tests

ZoneRental has two JUnit 5 test suites with different jobs.

| Suite | Location | Libraries | Runs in |
|---|---|---|---|
| **Unit** | `src/test/kotlin` | JUnit 5 (5.11.4), kotlin-test | GitHub CI (gates PRs and Modrinth releases) + local |
| **Mock** | `src/mockTest/kotlin` | JUnit 5, MockK 1.14.11, MockBukkit `mockbukkit-v1.21` 4.26.0 | **Local only** |

The mock suite is kept out of CI on purpose. `mockTest` is a separate source set and Gradle task, not part of `check`/`build`, so CI's `./gradlew clean build` and `./gradlew test` never run it. MockBukkit and MockK are not on the unit test classpath, and neither is shaded into the plugin JAR.

**Run both suites locally before pushing or opening a PR.**

## Commands

```bash
./gradlew test                       # unit suite (same as CI)
./gradlew mockTest                   # local mock suite
./gradlew test mockTest              # both
./gradlew mockTest --tests '*RentalManagerTest'          # one class
./gradlew mockTest --tests '*RentalManagerTest.issueRefund*'
./gradlew test mockTest -PrunKnownIssues                 # also run @Disabled known-issue tests (expected to FAIL)
```

Reports: `build/reports/tests/test/index.html` and `build/reports/tests/mockTest/index.html`.

## Unit suite (CI)

Pure logic plus consistency checks that read project files from the repo root (`ProjectFiles`).

| Area | Tests |
|---|---|
| Pure logic | `TimeUtilsTest`, `WorldRegionParserTest`, `RentalTest`, `OverrideSettingTest`, `ScanModelsTest` |
| `config.yml` | `ConfigYamlConsistencyTest`: no duplicate keys, all messages and sign formats are valid MiniMessage with no legacy `&` codes |
| Messages | `MessageUsageConsistencyTest`: every `getMessage("key", …)` key exists, and every `{placeholder}` in the text is supplied by the call |
| `plugin.yml` | `PluginYmlConsistencyTest`: every permission checked in code is declared, children exist, only `zr` is under `commands:` |
| Commands | `CommandRegistrationConsistencyTest`: every subcommand registered in `ZoneRental.registerCommands()` is in `checkPrefixConflicts()` |
| Release | `VersionConsistencyTest`: `build.gradle.kts` = `plugin.yml` = latest `CHANGELOG.md` heading, and the section ends with `---` |

## Mock suite (local)

`MockPluginTest` starts a MockBukkit server with worlds `world` and `world_nether`, uses a `@TempDir` as the data folder, and mocks `ZoneRental` with MockK. The real plugin is never enabled, because it needs Vault, WorldGuard and WorldEdit. Real config classes (`ConfigManager`, `RegionsConfig`, `GroupsConfig`, `SignsConfig`, `StorageConfig`) run against temp files. WorldGuard, WorldEdit, Vault and the other managers are MockK mocks. `CommandTestSupport` adds wiring for command and listener tests.

Covers:
- the config classes
- `RentalManager` (lifecycle, indexes, persistence, refunds)
- `WorldEditManager` file handling
- the retrieval GUI
- `/zrremove`, `/zrgroup` and duration parsing
- a robustness sweep over every command
- `SignInteractListener`
- container-type consistency

**MockBukkit gaps:**
- `FailOnUnimplementedExtension` turns MockBukkit's "Not implemented" (which JUnit would otherwise report as *skipped*) into a failure. The only skipped tests should be `@Disabled` known issues.
- MockBukkit 4.26 doesn't implement `getTargetBlock` or `Material.isItem` for legacy materials. Tests stub or avoid these. Don't change production code to work around MockBukkit.
- Stay on MockBukkit 4.26.x while the plugin compiles against Paper 1.21.3; 4.27+ targets 1.21.4.

## Regression tests

Tests named for a past fix carry a KDoc like `/** Regression 3.1.1: … */`. Current coverage:

| Fix | Test |
|---|---|
| 3.1.2 `/zrremove` group cleanup | `RemoveCommandTest` |
| 3.1.1 GUI pagination and item loss | `StorageManagerGuiTest` |
| 3.0.5 GUI navigation items | `StorageManagerGuiTest` |
| 3.0.2 conflict-check list | `CommandRegistrationConsistencyTest` |
| 2.9.1 schematic deletion | `WorldEditManagerTest` |
| 2.5.1 group cache invalidation, save pattern | `GroupCommandTest`, `StorageConfigTest` |
| 2.5.0 indexes, GUI session leak | `SignsConfigTest`, `RentalManagerTest`, `StorageManagerGuiTest` |
| 2.2.1 sign redraw on group changes | `GroupCommandTest` |
| 2.0.1 composite-key sign lookup, crash on bad args | `WorldRegionParserTest`, `SignInteractListenerTest`, `CommandRobustnessTest` |
| Double refund | `RentalTest`, `RentalManagerTest` |

## Known issues in tests

Open bugs from [Known issues](../reference/known-issues.md) are written as tests of the **correct** behaviour, so they're ready the moment a fix lands:

- **Behaviour tests** use `@Disabled("Known issue <ID>: …")`. `-PrunKnownIssues` runs them; they must fail until the bug is fixed.
- **Consistency tests** use a `KNOWN_*` allowlist constant (for example `ConfigYamlConsistencyTest.KNOWN_DUPLICATE_KEYS`). The test asserts the detected problems **equal** the allowlist. A new problem fails the test, and so does a fixed problem still listed.

When you fix a known issue: remove the `@Disabled` or the allowlist entry, make sure the test passes, and remove the entry from `known-issues.md`.

## Adding tests

- Logic that needs no Bukkit server goes in `src/test` (it runs in CI).
- Anything needing `ItemStack`, players, worlds, inventories or `ZoneRental` goes in `src/mockTest` and extends `MockPluginTest` (or `CommandTestSupport`).
- When fixing a bug, add a regression test named after the behaviour, with a `/** Regression x.y.z: … */` KDoc.
