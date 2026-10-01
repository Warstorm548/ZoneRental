# Known Issues & Limitations

Behaviour of the current code that differs from what users would expect or from the older docs. These came from a code review; items marked *(verify)* follow from the code but haven't been confirmed in game. Keep this list in sync when fixing issues.

## Data loss

| # | Issue | Where |
|---|---|---|
| D1 | **`/zrreload` drops unsaved data.** Rentals, signs, stored items and overrides changed since the last autosave (up to 5 min) are reloaded from disk / the last save without saving first. A new rental can vanish after the player has paid (they stay a WorldGuard member). `groups.yml` is not reloaded at all. | `ZoneRental.reloadPlugin`, `RentalManager.loadAllRentals`, `StorageConfig.reload` |
| D2 | **`/zrduration` changes may not be saved.** Setting `endDate` directly doesn't mark the rentals file as changed, so `saveAllRentals()` returns early. The change is lost on restart unless something else marked rentals as changed. The one-time multi-world migration save has the same problem. | `DurationCommand`, `RentalManager.saveAllRentals` |
| D3 | **Stored items are overwritten** when the same player's rental of the same region name ends again before they retrieve them. Storage keys have no world. | `StorageConfig.storeItems` |
| D4 | **Player-placed shulker boxes are deleted** with their contents: they're skipped by both scans, then removed by the restore. | `StorageManager`, `AsyncScanService` |
| D5 | Closing the retrieval GUI replaces the player's whole storage with what's left in the GUI, wiping anything stored while it was open. Items dragged into the GUI's bottom row are deleted (drag events aren't handled). | `StorageManager.onInventoryClose` |

## Duplication

| # | Issue | Where |
|---|---|---|
| X1 | Two-block objects (doors, beds, tall plants) are stored as one item per half. Blocks with no item form produce invalid items or an exception mid-expiry *(verify)*. | `StorageManager.blockToItemStack` |
| X2 | An expiry can be started twice: the rental stays in memory until the async expiry finishes, and the next check starts another. | `ExpirationManager.handleExpiration` |
| X3 | Snapshot entities are pasted without removing existing entities, so item frames and armor stands may multiply each cycle *(verify)*. | `WorldEditManager` restore |
| X4 | Items already inside containers at rent time go to the renter's storage and are also put back by the restore. If the snapshot failed, every container block is also stored as an item while the block stays in the world. | `StorageManager` |

## Settings that don't take effect

- Region/group overrides `maxextensions`, `extensionprice`, `allowextensions`, `extensionduration`, and `extension.enabled`, are not applied ([details](../features/overrides-and-groups.md)).
- `permission-prices` doesn't work with dotted permission keys, and its values replace the price rather than discount it ([details](../configuration/config-reference.md#permission-prices)).
- `config.yml` defines `restoration:` twice; only the second block is used.
- About 40 keys are unused (marked in the [config reference](../configuration/config-reference.md)), including `limits.permission-limits`, `notifications.warning-times`, `storage.max-storage-time` (storage never expires), `signs.expiring-format`, `restoration.restore-entities` / `restore-biomes`, and `integration.ezchestshop.removal-message` (replaced by `messages.ezchestshop-removed`).
- `max-rentals-reached` and `max-extensions-reached` show `{current}/{max}` literally.
- An invalid `commands.prefix` falls back to `rr`, while `config.yml` says `zr`.

## Commands

- `/zr list` and `/zr info` are stubs.
- `/zr help` admin pages need the undeclared `zonerental.admin` permission and leave out member/tp/group.
- `/zrduration`: `1d12h` parses as 12 h; `--charge` bills whole days only and fails for less than one day; `reset` ignores region duration overrides.
- `/zrremove` leaves the physical sign (with stale text, now unprotected) when a support block was recorded.
- `/zrgroup view` suggests `/zroverride <setting> <group>` (missing `group:`); `/zroverride maxextensions` tab completion suggests `unlimited`, which is rejected.
- Region names are stored in the case typed. WorldGuard IDs are lower case, so mixed case can split lookups.
- Re-running `/zrcreatesign` for a region leaves the old support block protected until restart.
- Reset refunds record the admin as "Admin" instead of the actual name.

## Performance

- `/zrreset` and `/zrremove` on regions with ≥ 10 chunks run the throttled scanner inside `runBlocking` on the main thread, so its pauses freeze the server.
- EzChestShop removal checks every block of the region on the main thread.
- The "async" expiry scan runs on the main thread, split up by pauses. `storage.yml` is written from an IO thread while autosave may write it too.
- Every sign click rebuilds the full sign location list; every price lookup scans all groups.

## Other

- Expiry warnings are hard-coded (24/12/6/1 h) and repeat after a restart or reload.
- Signs only redraw when something changes, so `{days}`/`{hours}` on rented signs go stale.
- Snapshot filenames contain `:`, which is invalid on Windows.
- Members are resolved by `getOfflinePlayer(name)`, so names that never joined can be added.
- `build.sh` checks for `ZoneRental-3.0.0.jar` and reports failure on success.
- Placeholder values are inserted before MiniMessage parsing, so values containing `<tags>` are interpreted.
- Sign protection only handles direct block breaking (not explosions or pistons).
- No automated tests.
