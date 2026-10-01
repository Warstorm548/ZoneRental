# Getting Started

A walkthrough of the basic admin and player workflow. Commands assume the default `zr` prefix.

## 1. Set up a rentable region (admin)

1. Stand in the world where the rental should be.
2. Define a WorldGuard region: `/rg define shop1`
3. Place a sign (wall or standing) and look at it.
4. Run `/zrcreatesign shop1`.
   - The sign is stored in `signs.yml` under `world:shop1` (the world is the one the sign is in).
   - The block the sign hangs on (wall sign) or stands on (standing sign) is recorded as its **support block** and protected.
   - The sign shows the `signs.available-format` lines with the region's price and duration.
5. Optional: give the region its own price or duration with `/zroverride price world:shop1 500` (see [Overrides & groups](../features/overrides-and-groups.md)).

Only one sign per region is tracked. Running `/zrcreatesign` again for the same region moves the registration to the new sign.

Use the region name in the same case WorldGuard shows it (lower case). The plugin stores what you type, and mixed case can cause lookup mismatches; see [Known issues](../reference/known-issues.md).

## 2. Rent (player)

- **Right-click** an available sign. The plugin checks `zonerental.rent`, that the region exists, your rental count against `limits.max-rentals-per-player`, the region size against `async-scanning.max-rental-chunks`, and your balance.
- On success: money is withdrawn, the region is snapshotted with WorldEdit (if `restoration.enabled`), you're added as a WorldGuard **member**, and the sign switches to the rented format.
- **Right-click** a rented sign to see its info.

## 3. Extend (player)

- **Shift + right-click** your own sign, or run `/zrextend shop1`.
- Cost: region price × `extension.price-multiplier`. Adds `extension.extension-days` days. Limited by `extension.max-extensions`.

## 4. Share (player)

- `/zrmember add shop1 Friend` makes Friend a WorldGuard member, up to `members.max-members`.
- Owners and members can use `/zrtp shop1` to teleport in front of the sign.

## 5. Expiry

- Warnings at 24 h, 12 h, 6 h and 1 h before expiry (chat, title and sound, if online).
- At expiry: the owner and members are removed from the region, container contents and player-placed blocks are moved to storage, EzChestShop shops are removed, the region is restored from the snapshot, and the sign shows available again.
- The player gets the items back with `/zrretrieve`.

Details: [Rental lifecycle](../features/rental-lifecycle.md), [Storage & restoration](../features/storage-and-restoration.md).
