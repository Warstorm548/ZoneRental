# Rental Lifecycle

## Sign interaction

`SignInteractListener` handles `PlayerInteractEvent` (right-click on a block) when the sign is registered in `signs.yml`:

| Action | Sign state | Result |
|---|---|---|
| Right-click | available | [Rent](#creation) |
| Right-click | rented | Show info (owner, expiry, time left, extensions used) |
| Shift + right-click | rented, yours | [Extend](#extension) |
| Shift + right-click | rented, not yours / available | Error message |

The region is looked up in the **clicking player's world**.

Sign protection (`signs.protect-signs`): breaking a registered sign, or its recorded support block, is cancelled unless the player has `zonerental.admin.breaksign`. Only `BlockBreakEvent` is handled. Explosions, pistons and other physics aren't.

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
| `/zrremove` | as `/zrreset`, then removes the sign, support block record, snapshot, group membership and overrides | same |

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
