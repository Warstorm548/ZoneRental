# Data Files

All files live in `plugins/ZoneRental/`. Keys use the composite format **`world:region`** (for example `world:shop1`).

| File | Owner class | Purpose | Saved |
|---|---|---|---|
| `config.yml` | `ConfigManager` | Settings ([reference](config-reference.md)) | Never written, except once to remove a migrated `regions:` section |
| `rentals.yml` | `RentalManager` | Active rentals | Autosave (5 min, only if changed) + shutdown |
| `signs.yml` | `SignsConfig` | Sign + support-block locations | Autosave + shutdown |
| `regions.yml` | `RegionsConfig` | Region and group overrides | Autosave + shutdown |
| `groups.yml` | `GroupsConfig` | Group membership | Autosave + shutdown |
| `storage.yml` | `StorageConfig` | Items from ended rentals | Autosave + shutdown |
| `schematics/*.schem` | `WorldEditManager` | Region snapshots | Written on rent, deleted after restore (if `auto-delete-schematics`) |

Changes are held in memory and written by the 5-minute autosave or on shutdown. A crash, or a `/zrreload` (which re-reads the files), loses changes made since the last save. See [Known issues](../reference/known-issues.md#data-loss).

## rentals.yml

```yaml
rentals:
  world:shop1:
    region-name: shop1
    world: world
    player-uuid: 8667ba71-b85a-4004-af54-457a9734eed7
    player-name: Steve
    start-date: 1767225600000      # epoch millis
    end-date: 1767830400000
    extension-count: 1
    total-paid: 200.0
    initial-price: 100.0
    total-refunded: 0.0
    refund-history:
      - amount: 50.0
        timestamp: 1767300000000
        reason: time_removal       # admin_reset | duration_reset | time_removal
        admin: Admin               # resets always record "Admin"
    members:
      - 069a79f4-44e9-4726-a5be-fca90e38aaf5
```

Entries with no `world` field (single-world format) are assigned the server's first world on load.

## signs.yml

```yaml
signs:
  world:shop1:
    world: world
    x: 100
    y: 64
    z: 200
    support-block:
      x: 100
      y: 64
      z: 201
      original-type: STONE_BRICKS
      original-data: minecraft:stone_bricks
```

Only one sign per region. Old entries keyed by region name only are migrated to `world:region` using their `world` field.

## regions.yml

```yaml
regions:
  world:shop1:
    price: 500.0
    duration: 14
    max-extensions: 20          # stored, not enforced (see Overrides)
    extension-price: 0.0        # stored, not enforced
    allow-extensions: true      # stored, not enforced
    extension-duration: 7       # stored, not enforced
groups:
  shop_group:                   # group overrides live here, not in groups.yml
    price: 1000.0
    duration: 30
```

- Keys without a world are migrated to `<first world>:<key>` on startup.
- A legacy `regions:` section in `config.yml` is copied here once. It's written **without** a world prefix and only becomes world-aware on the next restart.

## groups.yml

```yaml
groups:
  shop_group:
    regions:
      - world:shop1
      - world_nether:shop3
```

## storage.yml

```yaml
storage:
  <player-uuid>:
    shop1:                      # region name only, no world
      timestamp: 1767830400000
      items: [ ...serialized ItemStacks... ]
      blocks: [ ...serialized ItemStacks... ]
    partial_retrieval:          # leftovers after closing the GUI with items remaining
      items: [ ... ]
      blocks: []
```

Each new store for the same player and region name **replaces** the previous entry. Entries are never cleaned up automatically.

## schematics/

One Sponge `.schem` file per rented region, named `<world>:<region>.schem`. The `:` is not a valid filename character on Windows. Legacy `.dat` files from v2.5.1 are empty and are deleted on startup when `auto-delete-schematics` is on.
