# config.yml Reference

Source of truth: `src/main/resources/config.yml` (defaults) and `src/main/kotlin/com/zonerental/config/ConfigManager.kt` (what is read).

**Status column:**
- **Used**: read by the code.
- **Unused**: present in `config.yml` but never read. Changing it has no effect.

Values marked *cached* are read once on load or `/zrreload`. Others are read live from the config each time.

## general

| Key | Default | Status | Notes |
|---|---|---|---|
| `prefix` | `<dark_gray>[<gold>ZoneRental<dark_gray>]<reset> ` | Used (cached) | MiniMessage. Prepended to every `messages.*` value. |
| `debug` | `false` | Used (cached) | Extra console logging. |
| `expiration-check-interval` | `1` | Used (cached) | Minutes between expiry checks. Read at startup only (the scheduler is not rescheduled on reload). |
| `auto-save-interval` | `5` | **Unused** | Autosave is hard-coded to 5 minutes. |
| `max-storage-pages` | `10` | **Unused** | |

## commands

| Key | Default | Status | Notes |
|---|---|---|---|
| `prefix` | `zr` | Used (startup only) | See [Commands → prefix](../user-guide/commands.md#command-prefix). An invalid value falls back to `rr` (the comment says `zr`). |

## economy

| Key | Default | Status | Notes |
|---|---|---|---|
| `enabled` | `true` | **Unused** | Loaded but never checked; an economy is always required. |
| `default-price` | `100.0` | Used (cached) | Rental price when there's no override. |
| `currency-format` | `$%.2f` | Used (cached) | `String.format` pattern. |
| `refund-on-reset` | `false` | **Unused** | `/zrreset` always refunds the net amount. |
| `refund-percentage` | `0` | **Unused** | |

## durations

| Key | Default | Status | Notes |
|---|---|---|---|
| `default-days` | `7` | Used (cached) | Rental length when there's no override; also used by `/zrduration reset`. |
| `available-durations` | `[1,3,7,14,30]` | **Unused** | Loaded, never used. |
| `min-duration`, `max-duration`, `allow-custom-duration`, `time-format-help` | — | **Unused** | |

## extension

| Key | Default | Status | Notes |
|---|---|---|---|
| `enabled` | `true` | **Unused at runtime** | Only reached through `getAllowExtensionsForRegion`, which nothing calls. Extensions can't be disabled. |
| `extension-days` | `7` | Used (cached) | Days added per extension (global; region overrides are ignored). |
| `price-multiplier` | `1.0` | Used (live) | Extension cost = region rental price × multiplier. |
| `max-extensions` | `10` | Used (cached) | Global limit (region overrides are ignored). |
| `min-time-before-extension`, `max-time-before-extension` | — | **Unused** | |
| `refund-on-duration-reset` | `true` | Used (live) | `/zrduration reset` refunds `totalPaid - initialPrice`. |

## duration (admin `/zrduration` behaviour)

| Key | Default | Status | Notes |
|---|---|---|---|
| `refund-on-time-removal` | `true` | Used (live) | Proportional refund on `remove` (whole days only). |
| `charge-for-add` | `false` | Used (live) | Must be `true` for `--charge` to bill. |
| `add-bypass-extension-limit` | `true` | **Unused** | Charged additions never count as extensions anyway. |
| `add-price-per-day` | `0.0` | Used (live) | If > 0, the per-day price for `--charge`; otherwise region price ÷ region duration. |

## limits

| Key | Default | Status | Notes |
|---|---|---|---|
| `max-rentals-per-player` | `3` | Used (cached) | |
| `permission-limits` | vip/premium/unlimited | **Unused** | |
| `rental-cooldown` | `0` | **Unused** | |

## members

| Key | Default | Status |
|---|---|---|
| `enabled` | `true` | Used (live) |
| `max-members` | `5` | Used (live); `-1` = unlimited |

## teleport

| Key | Default | Status | Notes |
|---|---|---|---|
| `enabled` | `true` | Used | |
| `max-search-distance` | `20` | Read but not effective | Passed to the search but not used by the algorithm. |
| `forward-search-distance` | `5` | Used | Capped at 20. |
| `floor-search-down` | `20` | Used | Capped at 20. |
| `floor-search-up` | `20` | Used | |
| `cooldown` | `30` | Used | Seconds; `0` disables. |
| `cross-world-warning`, `sound-enabled`, `particle-enabled` | `true` | Used | |

## signs

| Key | Default | Status | Notes |
|---|---|---|---|
| `available-format` | 4 MiniMessage lines | Used (cached) | Placeholders: `{region}`, `{price}`, `{duration}` |
| `rented-format` | 4 MiniMessage lines | Used (cached) | Placeholders: `{region}`, `{owner}`, `{expires}` (MM/dd HH:mm, server timezone), `{days}`, `{hours}` (total hours) |
| `expiring-format` | — | **Unused** | There is no "expiring" sign state. |
| `protect-signs` | `true` | Used (cached) | Protects signs and support blocks from breaking. |
| `allow-break-with-permission`, `update-interval`, `expiring-threshold` | — | **Unused** | Sign refresh is fixed at 30 s and only redraws signs that changed. |

## storage

| Key | Default | Status | Notes |
|---|---|---|---|
| `enabled` | `true` | Used (cached) | Move container contents and player blocks to storage on expiry/reset. |
| `store-player-blocks` | `true` | Used (live) | Needs a snapshot to compare against. |
| `block-blacklist` | list | Used (live) | Merged with a built-in list (air variants, dirt, grass, stone, cobblestone, gravel, sand, sandstone, water, lava, bedrock). |
| `store-from-containers`, `container-types`, `max-storage-time`, `items-per-page` | — | **Unused** | Container types are hard-coded; storage never expires; the GUI uses 45 items per page. |

## restoration

> **`restoration:` is defined twice in the default `config.yml`.** Bukkit's YAML loader keeps the **second** block (enabled, save-on-rent, restore-on-expire, excluded-blocks). Keys only present in the first block are therefore missing and fall back to code defaults. Keep a single `restoration:` section in your file.

| Key | Default | Status | Notes |
|---|---|---|---|
| `enabled` | `true` | Used (cached) | Snapshot on rent, restore on expiry/reset. |
| `auto-delete-schematics` | `true` | Used (cached) | Delete the `.schem` after restoring. |
| `schematic-cache-size` | `20` | Used (startup) | In-memory LRU of snapshots. |
| `restore-entities`, `restore-biomes`, `auto-capture-on-rent`, `max-schematic-size`, `save-on-rent`, `restore-on-expire`, `excluded-blocks` | — | **Unused** | Entities are always captured and pasted; biomes never. |
| `enable-multi-page`, `gui-title`, `gui-title-single` | — | **Unused** | The GUI titles are hard-coded. |

## async-scanning

| Key | Default | Status | Notes |
|---|---|---|---|
| `enabled` | `true` | Used | Only affects the synchronous `/zrreset` and `/zrremove` path. |
| `min-chunks-for-async` | `10` | Used | Smaller regions use the simple block loop. |
| `tps-healthy-threshold`, `tps-warning-threshold` | `19.5`, `18.5` | Used | Throttling thresholds. |
| `debug-async` | `false` | Used | |
| `max-rental-chunks` | `2000` | Used | Larger regions can't be rented (`0` disables the check). |

## notifications

| Key | Default | Status | Notes |
|---|---|---|---|
| `title-enabled`, `sound-enabled` | `true` | Used (live) | Expiry warning title and sound. |
| `actionbar-enabled`, `warning-times`, `notification-sound`, `sound-volume`, `sound-pitch` | — | **Unused** | Warnings are hard-coded to 24/12/6/1 h with `ENTITY_EXPERIENCE_ORB_PICKUP`. |

## messages

See [Messages & formatting](messages.md).

## regions-config

| Key | Default | Status |
|---|---|---|
| `auto-verify-regions` | `true` | Used: logs orphaned overrides on startup/reload. |
| `enable-verify-command` | `true` | Used: gates `/zrverify`. |

## integration.ezchestshop

| Key | Default | Status | Notes |
|---|---|---|---|
| `enabled` | `true` | Used | Remove shops on expiry. |
| `notify-on-removal` | `true` | Used | |
| `removal-message` | — | **Unused since 3.2.0** | The notification now uses `messages.ezchestshop-removed`, which isn't in the default `config.yml`; add it under `messages:` to customise it. |

## permission-prices

```yaml
permission-prices:
  'zonerental.vip': 50.0
```

Intended to give players with a permission a different rental price. **Current behaviour** (`SignInteractListener.handleRentRegion`):

- The value **replaces** the region price outright (it isn't a percentage), even when the region price is lower.
- Bukkit reads dots in keys as nested paths, so `'zonerental.vip'` is stored as `zonerental → vip`. The plugin then checks the permission `zonerental` at the default price. **The feature does not work with real (dotted) permission nodes.**
- The section is commented out by default.

## Sections that are entirely unused

`debug-options.*`, `advanced.*` (including `advanced.database.*`).

## Legacy `regions:` section

If `config.yml` has a top-level `regions:` section (pre-`regions.yml` format), it's migrated into `regions.yml` once on startup and removed from `config.yml`. See [Data files](data-files.md#regionsyml).
