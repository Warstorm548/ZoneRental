# Rental Lifecycle

## Sign interaction

`SignInteractListener` handles `PlayerInteractEvent` (right-click on a block) when the sign is registered in `signs.yml`:

| Action | Sign state | Result |
|---|---|---|
| Right-click | available | [Rent](#creation) |
| Right-click | rented | Show info (owner, expiry, time left, extensions used) |
| Shift + right-click | rented, yours | [Extend](#extension) |
| Shift + right-click | rented, not yours / available | Error message |

The region is looked up in the **region's world** from the sign's `world:region` key, so a sign in a lobby world rents a region elsewhere.

A rental space can have several signs. They are redrawn together: any change marks the space for redraw, and every one of its signs is updated in the next pass.

Sign protection (`signs.protect-signs`): breaking a registered sign, or its recorded support block, is cancelled unless the player has `zonerental.admin.breaksign`. When such a break goes through, the affected signs are dropped from `signs.yml` immediately. Environment protection (`signs.environment-protection`, `SignProtectionListener`) keeps explosions, pistons, fire, endermen, withers, falling blocks, leaf decay and fading blocks away from signs and support blocks.

## Creation

`SignInteractListener.handleRentRegion` → `RentalManager.createRental`:

1. Check `zonerental.rent`, that the WorldGuard region exists, and `limits.max-rentals-per-player`.
2. Price = region price ([overrides](overrides-and-groups.md)), then replaced by the first matching `permission-prices` entry ([currently broken](../configuration/config-reference.md#permission-prices)).
3. Check the balance, then **withdraw**.
4. `createRental` checks the region size (`async-scanning.max-rental-chunks`), snapshots the region if `restoration.enabled`, creates the `Rental` (end = now + region duration in days), adds the player as a WorldGuard member and marks the sign for redraw.
5. If step 4 fails, the money is deposited back.

If the snapshot fails, the rental still goes ahead (logged only in debug mode). Nothing will be restored at expiry.

## Extension

Same logic in the sign handler and `/zrextend` (`ExtendCommand`):

- Owner only. Limited by the global `extension.max-extensions`.
- Cost = region rental price × `extension.price-multiplier`.
- Adds the global `extension.extension-days`; `extensionCount` +1; `totalPaid` += cost.
- **Not applied:** the region/group overrides `maxextensions`, `extensionprice`, `allowextensions`, `extensionduration`, and `extension.enabled`.

## Expiry warnings

`ExpirationManager.checkExpiredRentals` runs every `general.expiration-check-interval` minutes. For each active rental it sends **one** warning per check at the 24, 12, 6 and 1 hour thresholds (hard-coded): chat `rental-expiring-soon`, plus a title and a sound if enabled. Sent warnings are tracked in memory only, so they repeat after a restart or reload.

## Expiration

When `endDate` has passed, `ExpirationManager` messages the owner (`rental-expired`) and launches `RentalManager.expireRentalAsync` in an MCCoroutine:

1. Remove the owner and members from the WorldGuard region.
2. If `storage.enabled`: move container contents and player-placed blocks to `storage.yml` ([details](storage-and-restoration.md)).
3. If EzChestShop is enabled: remove shops in the region ([details](ezchestshop-integration.md)).
4. If `restoration.enabled`: paste the snapshot back and optionally delete it.
5. Remove the rental and mark the sign for redraw (it shows available within 30 s).

The rental stays in memory until step 5, and nothing stops a later check from starting a second expiry for it (see [Known issues](../reference/known-issues.md)).

## Admin-triggered endings

| Command | Path | Refund |
|---|---|---|
| `/zrreset` | `RentalManager.resetRentalWithRefund` → synchronous `expireRental` (same steps as above) | `totalPaid - totalRefunded` |
| `/zrremove all <world:region>` | as `/zrreset`, then breaks every sign, restores their support blocks, unregisters the rental space and removes the snapshot, group membership and overrides | same |

See [Refunds](refunds.md).

## Scheduled tasks

All run on the main thread (`ZoneRental.startTasks`):

| Task | Interval |
|---|---|
| Expiry check | `general.expiration-check-interval` minutes (default 1) |
| Redraw signs that changed | 30 s |
| Expire pending `/zrgroup` chat prompts | 30 s |
| Autosave of changed data files | 5 min |
| Teleport cooldown cleanup | 10 min |

Sign text is only redrawn when the rental state or overrides change. Placeholders like `{days}`/`{hours}` on a rented sign don't count down by themselves.

Standing and hanging signs get the text on both sides; wall signs on the front only.

During a redraw, a sign whose world isn't loaded is skipped. A sign in an unloaded chunk is loaded and redrawn when `signs.load-chunks-for-updates` is `true`, otherwise it is redrawn when its chunk loads. A sign whose block is loaded but isn't a sign any more is checked again on the next pass and dropped from `signs.yml` (with a warning) if it is still missing.
