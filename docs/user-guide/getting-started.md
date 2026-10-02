# Getting Started

A walkthrough of the basic admin and player workflow. Commands assume the default `zr` prefix.

## 1. Set up a rentable region (admin)

1. Stand in the world where the rental should be.
2. Define a WorldGuard region: `/rg define shop1`
3. Place a sign (wall, standing or hanging) and look at it.
4. Run `/zrcreatesign shop1`.
   - The region is registered as a **rental space** under `world:shop1` in `signs.yml` (if it wasn't already), and the sign is stored under it as sign `#1`.
   - The block the sign hangs on, stands on or hangs from is recorded as its **support block** and protected.
   - The sign shows the `signs.available-format` lines with the region's price and duration.
   - To place the sign in another world (for example a lobby), run `/zrcreatesign world:shop1` there.
5. Optional: give the region its own price or duration with `/zroverride price world:shop1 500` (see [Overrides & groups](../features/overrides-and-groups.md)).

A region can have several signs (3 by default, `signs.max-per-region`). Run `/zrcreatesign shop1` on each one; they get IDs `#2`, `#3`, … and always show the same text. Remove one with `/zrremove` while looking at it, or `/zrremove world:shop1 <id>`.

You can also register the rental space first with `/zrcreate world:shop1` and add signs later; nobody can rent it until it has a sign.

`/zrcreate` and `/zrcreatesign` store the region name the way WorldGuard has it, so `Shop1` and `shop1` are the same rental space.

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
