# Item Storage & Block Restoration

## Snapshots (WorldEdit)

`WorldEditManager`:

- **Capture** (`captureRegion`, on rent if `restoration.enabled`): copies the region's cuboid bounds (blocks + entities, no biomes) into a `BlockArrayClipboard`, caches it in an LRU of `restoration.schematic-cache-size` entries, and writes `schematics/<world>:<region>.schem` (Sponge format).
- **Restore** (`restoreRegion` / `restoreRegionAsync`, on expiry or reset): pastes the clipboard at its origin, including air. Entities from the snapshot are pasted too; entities already in the region are not removed first. If `auto-delete-schematics`, the cache entry and file are deleted afterwards.
- Snapshots are loaded from disk on demand when they're not cached.

## Storing items when a rental ends

Runs before the restore, if `storage.enabled`. Items are filed under the **owner** (members get nothing).

### Containers

Scanned types (hard-coded `CONTAINER_TYPES` in both `StorageManager` and `AsyncScanService`): chest, trapped chest, barrel, hopper, dropper, dispenser, furnace, blast furnace, smoker, brewing stand, and all shulker box colours.

For each container found:
- **Shulker boxes are skipped completely**: not emptied and not stored as an item.
- Every other container's contents are copied to storage and the container is emptied.
- If the container block differs from the snapshot (player-placed), one container item is also stored.

### Player-placed blocks

If `storage.store-player-blocks` and a snapshot exists: every block whose material differs from the snapshot, and isn't in the blacklist or a container type, is stored as **one item of that block's material**. Things this rule produces:

- Two-block objects (doors, beds, tall flowers) give one item per half.
- Blocks without an item form (wall torches, crops, fire, …) are converted as-is.

### Where the items go

`storage.yml` under `storage.<uuid>.<region>` (no world in the key). A later store for the same player and region name replaces the earlier one. The owner is told how many items and blocks were stored, if online.

## Async scanning

For regions with at least `async-scanning.min-chunks-for-async` (10) chunks, the scan (`StorageManager.*Async`, `AsyncScanService`, `TpsMonitor`) works in two phases:

- **Phase A**: takes a `ChunkSnapshot` per chunk and finds containers or changed blocks, in batches sized by region size (`ScanStrategy`: Tiny < 50 chunks … Extreme ≥ 2000). Pauses are added between batches and lengthened when TPS drops (`TpsLevel`: healthy ≥ 19.5, warning ≥ 18.5, critical below).
- **Phase B**: touches only the blocks found in phase A.

Threading, as implemented:

- **Expiry** runs in an MCCoroutine launched with `plugin.launch`, which uses the main-thread dispatcher. The scan therefore runs **on the main thread, split up by suspending pauses**, not on a worker thread. Only `storage.yml` writes and snapshot file I/O switch to `Dispatchers.IO`.
- **`/zrreset` and `/zrremove`** use the synchronous wrappers. For large regions these call the async code inside `runBlocking`, so the throttling pauses **block the main thread**.
- `async-scanning.enabled: false` only affects those synchronous wrappers.

Regions larger than `async-scanning.max-rental-chunks` (default 2000) can't be rented.

## Retrieval GUI (`/zrretrieve`)

`StorageManager.openRetrievalGUI`:

- Combines every stored entry (items + blocks, all regions) for the player into a 54-slot GUI: 45 item slots, plus navigation in the bottom row (45 previous, 49 page info, 50 close, 53 next).
- Players can take items out. Putting items in by shift-click, number key or cursor is blocked. Dragging isn't handled.
- On close, everything left over is saved as one `partial_retrieval` entry. If nothing is left, the player's storage is cleared.
- Leaving the server while the GUI is open discards the session.
