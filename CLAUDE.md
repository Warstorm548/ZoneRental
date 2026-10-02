# CLAUDE.md

Guidance for Claude Code (claude.ai/code) in this repository. This file is an **index**: it holds the tech stack and essential rules, and points to the detailed docs in `docs/`. Read the linked doc before working in that area.

## Project

**ZoneRental** is a Paper 1.21+ plugin for renting WorldGuard regions through clickable signs. It uses Vault for payments, time-based expiry, WorldEdit snapshots to restore regions, and item storage with a retrieval GUI. Current version: **3.3.0** (`build.gradle.kts`).

## Tech stack

| Area | Technology | Version | Scope |
|---|---|---|---|
| Language | Java (OpenJDK) | 21 (toolchain + `release 21`) | Main class + 2 listeners |
| Language | Kotlin (JVM) | 2.2.20 | Everything else (51 files) |
| Build | Gradle (Kotlin DSL, wrapper) | 9.2.0 | |
| Build | Shadow plugin (`com.gradleup.shadow`) | 9.2.2 | Shades and relocates Kotlin, coroutines, MCCoroutine |
| Server API | Paper API | 1.21.3-R0.1-SNAPSHOT | compileOnly; `api-version: '1.21'` |
| Text | Adventure + MiniMessage | bundled with Paper | All player-facing text since 3.2.0 |
| Regions | WorldGuard | 7.0.14 | compileOnly, hard dependency |
| Snapshots | WorldEdit (bukkit + core) | 7.3.16 | compileOnly, hard dependency |
| Economy | Vault API | 1.7 | compileOnly, hard dependency |
| Permissions | LuckPerms API | 5.4 | compileOnly, soft dependency, **not used in code** |
| Async | MCCoroutine (bukkit api + core) | 2.21.0 | Shaded; main class is `SuspendingJavaPlugin` |
| Async | kotlinx-coroutines-core | 1.9.0 | Shaded |
| Integration | EzChestShop / EzChestShopReborn | runtime reflection | Soft dependency |
| Testing | JUnit 5 (BOM) + kotlin-test | 5.11.4 | Unit suite `src/test`, runs in CI |
| Testing | MockBukkit (`mockbukkit-v1.21`) | 4.26.0 | Local-only `src/mockTest` suite; 4.26.x is the last line built for Paper 1.21.3 |
| Testing | MockK | 1.14.11 | Local-only `src/mockTest` suite |
| CI | GitHub Actions | `.github/workflows/` | Build + unit tests (gating) on `main`/`develop`; Modrinth publish on `main` |

## Build

```bash
./gradlew clean build          # → build/libs/ZoneRental-<version>.jar (also runs unit tests)
./gradlew test                 # unit suite, same as CI
./gradlew mockTest             # MockK/MockBukkit suite, local only (never run by CI)
./gradlew test mockTest -PrunKnownIssues   # also run @Disabled known-issue tests (expected to fail)
```

**Run `./gradlew test mockTest` locally and make sure both pass before any push or PR.** Don't rely on `build.sh` (it checks a hard-coded old JAR name). Details: [building.md](docs/development/building.md), [automated-tests.md](docs/testing/automated-tests.md)

## Documentation index

### User guide: `docs/user-guide/`
- [installation.md](docs/user-guide/installation.md): requirements, startup checks, 3.2.0 MiniMessage upgrade note
- [getting-started.md](docs/user-guide/getting-started.md): admin setup → rent → extend → expire walkthrough
- [commands.md](docs/user-guide/commands.md): every command, the dynamic prefix, `world:region` parsing, time format
- [permissions.md](docs/user-guide/permissions.md): permission nodes and what checks them

### Configuration: `docs/configuration/`
- [config-reference.md](docs/configuration/config-reference.md): every `config.yml` key, marked used/unused
- [messages.md](docs/configuration/messages.md): MiniMessage pipeline, message keys and their placeholders
- [data-files.md](docs/configuration/data-files.md): `rentals.yml`, `signs.yml`, `regions.yml`, `groups.yml`, `storage.yml`, `schematics/` formats

### Features: `docs/features/`
- [rental-lifecycle.md](docs/features/rental-lifecycle.md): sign interaction, create/extend/expire, scheduled tasks
- [overrides-and-groups.md](docs/features/overrides-and-groups.md): `/zroverride`, `/zrgroup`, lookup priority (group → region → default)
- [refunds.md](docs/features/refunds.md): payment tracking, net-capped refunds, charges
- [storage-and-restoration.md](docs/features/storage-and-restoration.md): WorldEdit snapshots, item/block storage, async scanning, retrieval GUI
- [members-and-teleport.md](docs/features/members-and-teleport.md): `/zrmember`, `/zrtp`
- [multi-world.md](docs/features/multi-world.md): `world:region` keys and migrations
- [ezchestshop-integration.md](docs/features/ezchestshop-integration.md): reflection-based shop removal

### Development: `docs/development/`
- [architecture.md](docs/development/architecture.md): managers, lifecycle, command registration, threading model, message pipeline
- [source-layout.md](docs/development/source-layout.md): file tree, which files are dead code
- [conventions.md](docs/development/conventions.md): current API signatures, persistence rules, adding commands/config options
- [building.md](docs/development/building.md): Gradle config, shading, CI workflows, version bumps

### Testing: `docs/testing/`
- [automated-tests.md](docs/testing/automated-tests.md): unit vs local mock suite, commands, known-issue test convention, regression test map
- [in-game-testing-checklist.md](docs/testing/in-game-testing-checklist.md): manual test plan

### Reference: `docs/reference/`
- [known-issues.md](docs/reference/known-issues.md): confirmed bugs, settings that don't take effect, performance caveats. **Check this before assuming a feature works as configured.**

### Archive: `docs/archive/`
- [refund-system-implementation.md](docs/archive/refund-system-implementation.md): historical notes (RegionRental era)

### Root files
- [README.md](README.md): user-facing overview
- [CHANGELOG.md](CHANGELOG.md): release history. **Must stay at the repo root**: `publish-modrinth.yml` extracts the `## [<version>]` section up to the next `---` as release notes.

## Essential rules (summary; see linked docs)

- **Language:** new code goes in Kotlin under `src/main/kotlin/com/zonerental/`.
- **Keys:** everything is keyed by `world:region`. All region APIs take a `World`. Parse user input with `WorldRegionParser`.
- **WorldGuard:** only through `WorldGuardManager`. **WorldEdit:** only through `WorldEditManager`.
- **Text:** MiniMessage only. Configurable text goes through `ConfigManager.getMessage(key, "{ph}", value)` (key in `config.yml` + default in `loadMessages()`); fixed text through `sendMiniMessage("<red>…")`. No `ChatColor` or `&` codes.
- **Persistence:** in-memory with change tracking and a 5-minute autosave. `RentalManager.saveAllRentals()` is a no-op unless the rentals are marked changed; editing `Rental` fields directly doesn't mark them ([conventions](docs/development/conventions.md#persisting-changes)).
- **Commands:** registered dynamically with a configurable prefix (default `zr`). New commands go in `ZoneRental.registerCommands()` **and** `checkPrefixConflicts()`, not under `plugin.yml` `commands:` ([how-to](docs/development/conventions.md#adding-a-command)).
- **Refunds:** use `RentalManager.issueRefund` / `resetRentalWithRefund(regionName, world)`; they cap at `netRefundableAmount`.
- **Tests:** logic with no server → `src/test` (CI). Needs Bukkit objects or `ZoneRental` → `src/mockTest`, extending `MockPluginTest`. Bug fixes get a regression test, and fixing a known issue means enabling its `@Disabled` test or removing its allowlist entry.
- **Threading:** scheduled tasks and the expiry coroutine run on the main thread; only file I/O uses `Dispatchers.IO` ([architecture](docs/development/architecture.md#threading-model)).

## Versioning (SemVer)

Bump in `build.gradle.kts` (line 8), `src/main/resources/plugin.yml` (line 2), `README.md` (version line), and add a `CHANGELOG.md` section. See [building.md](docs/development/building.md#versioning-semver).

## Keeping docs in sync

When changing behaviour, update the matching doc above. When fixing something in [known-issues.md](docs/reference/known-issues.md), remove it from that list.
