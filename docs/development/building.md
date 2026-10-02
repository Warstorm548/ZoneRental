# Building

## Prerequisites

- JDK 21 (the Gradle toolchain targets Java 21; Kotlin compiles to JVM 21)
- Nothing else. Gradle 9.2.0 comes through the wrapper (`gradle/wrapper/gradle-wrapper.properties`), and Kotlin through the Gradle plugin.

## Commands

```bash
./gradlew clean build      # default tasks; produces the shaded plugin JAR
./gradlew shadowJar        # JAR only
./gradlew compileKotlin    # compile Kotlin only
./gradlew compileJava      # compile Java only (depends on Kotlin)
```

Output: `build/libs/ZoneRental-<version>.jar`. The plain `jar` task is disabled; `build` depends on `shadowJar`.

`build.sh` runs `clean` + `build` but checks for a hard-coded `ZoneRental-3.0.0.jar`, so it **reports "Build FAILED" even when the build succeeds**. Use `./gradlew clean build` instead.

## Tests

```bash
./gradlew test       # unit suite: runs in CI, also part of `build`/`check`
./gradlew mockTest   # MockK + MockBukkit suite: local only, never run by CI
```

Both suites must pass locally before pushing. Details: [Automated tests](../testing/automated-tests.md).

## Build configuration (`build.gradle.kts`)

| Item | Value |
|---|---|
| Plugins | `java`, `kotlin("jvm") 2.2.20`, `com.gradleup.shadow 9.2.2` |
| Group / version | `com.zonerental` / `version = "…"` (line 8) |
| `compileOnly` | `paper-api:1.21.3-R0.1-SNAPSHOT`, `VaultAPI:1.7`, `worldguard-bukkit:7.0.14`, `worldedit-bukkit` + `worldedit-core:7.3.16`, `luckperms api:5.4` (declared but unused in code) |
| `implementation` (shaded) | Kotlin stdlib, `mccoroutine-bukkit-api` / `-core:2.21.0`, `kotlinx-coroutines-core:1.9.0` |
| Relocations | `kotlin` → `com.zonerental.shaded.kotlin`, `kotlinx.coroutines` → `com.zonerental.shaded.kotlinx.coroutines`, `com.github.shynixn.mccoroutine` → `com.zonerental.shaded.mccoroutine` |
| Forced versions | Guava 33.3.1-jre, Gson 2.11.0, fastutil 8.5.15, log4j-bom 2.24.1 (WorldGuard/WorldEdit transitive deps are also excluded) |
| Test dependencies (not shaded) | `src/test`: JUnit BOM 5.11.4, `kotlin("test-junit5")`. `src/mockTest` source set: + `mockbukkit-v1.21:4.26.0`, `mockk:1.14.11`. Both extend `compileOnly`. `-PrunKnownIssues` enables `@Disabled` tests. |
| Repositories | Maven Central, PaperMC, JitPack (Vault), EngineHub (WG/WE), Sonatype snapshots |

## CI (`.github/workflows/`)

| Workflow | Trigger | What it does |
|---|---|---|
| `gradle-ci.yml` | push / PR to `main`, `master`, `develop`; manual | Build (runs the unit tests), run the unit tests (**failures fail the check**), check that the JAR contains shaded Kotlin, upload the JAR artifact. Does not run `mockTest`. |
| `publish-modrinth.yml` | push to `main`, ignoring `**.md`, `docs/**`, `.gitignore`, `LICENSE` | Build and publish to Modrinth. Release notes are the `## [<version>]` section of **`CHANGELOG.md` (must stay in the repo root)** |

## Versioning (SemVer)

To bump the version, update:

1. `build.gradle.kts`: `version = "X.Y.Z"`
2. `src/main/resources/plugin.yml`: `version: X.Y.Z`. `processResources` runs `expand()` on `plugin.yml`, but the file has a literal version, so it must be edited by hand.
3. `CHANGELOG.md`: add a `## [X.Y.Z] - Title` section ending with `---` (the publish workflow extracts it).
4. `README.md`: the version line.
