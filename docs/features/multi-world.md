# Multi-World Support

Every rental, sign, override, group entry and snapshot is keyed by **`world:region`** (for example `world:shop1`, `world_nether:shop1`). The same WorldGuard region name can be rented separately in different worlds.

## Which world is used

| Context | World |
|---|---|
| Clicking a sign | The region's world stored for that sign (`world:region` key), which can differ from the sign's and the player's world |
| `/zrcreatesign <region>` | The player's world; `/zrcreatesign world:region` registers a sign for a region in another world |
| `/zrtp <region>` | Teleports to the region's oldest sign, in whatever world that sign is |
| Commands with `<region>` | The player's world, unless written as `world:region` |
| Console commands | Must use `world:region` |
| `/zrgroup` region lists | `region` = player's world; `world:region` = explicit |

## Exceptions

- **`storage.yml`** keys stored items by region name only (no world). See [Data files](../configuration/data-files.md#storageyml).
- Tab completion for `/zroverride` and `/zrgroup` suggests region names from **all** worlds.

## Migration from single-world data

On load, entries without a world are given the server's **first** world (`server.worlds[0]`):

| File | Behaviour |
|---|---|
| `rentals.yml` | Entries without `world:` get the first world. The re-save is skipped when nothing else has changed, so the conversion is written on the next normal save. |
| `signs.yml` | Old `region:` keys become `<sign's world>:region`. Each sign keeps its own `world` field, so a sign can live in a different world from its region. |
| `regions.yml` | Old `region:` keys become `<first world>:region`. |
