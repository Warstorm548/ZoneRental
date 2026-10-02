# ZoneRental

**Let players rent WorldGuard regions with a sign.**

Version: 3.3.0
Minecraft: Paper 1.21+
Languages: Java 21, Kotlin 2.2.20

ZoneRental turns any WorldGuard region into a shop, plot or apartment that players can rent with in-game money. Put up a sign and players right-click it to rent. They build for as long as they pay, and when the rental ends the plugin returns their items and resets the region to how it was.

---

## Why server owners use it

- **One-click renting.** Players right-click a sign to rent and shift-right-click to extend. No commands needed.
- **Hands-off expiry.** Rentals end on schedule. Players get warnings 24, 12, 6 and 1 hour before.
- **Regions reset themselves.** The region is snapshotted with WorldEdit when rented and restored when the rental ends, so the next renter gets a clean plot.
- **Players keep their stuff.** Items in chests, barrels, furnaces and other containers, plus blocks the player placed, are saved. Players collect them with `/zrretrieve`.
- **Vault economy.** Works with EssentialsX, CMI or any Vault economy. Admin resets refund players automatically, with a refund history per rental.
- **Shared builds.** Renters can add friends as members (`/zrmember`), and both can teleport to the rental (`/zrtp`).
- **Custom pricing.** Set a price and duration per region, or for a whole group of regions at once.
- **Multi-world.** Works in the overworld, nether, end and custom worlds; the same region name can be rented separately in each.
- **Several signs per region.** Put up to 3 signs (configurable) for the same region, even in other worlds such as a lobby. They always show the same thing.
- **Protected signs.** Rental signs and the blocks they hang on can't be broken by players, blown up, burned or moved by pistons.
- **EzChestShop support.** Chest shops in a region are removed automatically when its rental ends.
- **Customisable text.** Every message and sign line can be changed and styled with [MiniMessage](https://docs.advntr.dev/minimessage/format.html).

---

## Requirements

| Required | Version |
|---|---|
| [Paper](https://papermc.io/) | 1.21+ |
| Java | 21+ |
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | 1.7+ |
| An economy plugin | e.g. EssentialsX, CMI |
| [WorldGuard](https://enginehub.org/worldguard) | 7.0.14+ |
| [WorldEdit](https://enginehub.org/worldedit) | 7.3.16+ |

| Optional | Purpose |
|---|---|
| LuckPerms (or any permissions plugin) | Manage who can rent, extend, teleport, etc. |
| EzChestShop / EzChestShopReborn | Automatic shop removal when a rental ends |

---

## Installation

1. Install Vault, an economy plugin, WorldGuard and WorldEdit.
2. Drop `ZoneRental-3.2.0.jar` into your `plugins/` folder.
3. Restart the server (avoid `/reload`).
4. Adjust `plugins/ZoneRental/config.yml` (prices, durations, limits, messages), then restart or run `/zrreload`.

**Upgrading from 3.2.x or older?** 3.3.0 converts `signs.yml` to a new layout on first start and keeps a copy as `signs.yml.pre-3.3.0.bak`; restore that copy if you go back to an older version. `/zrremove <region>` now needs `all` to remove a whole setup: `/zrremove all world:shop1`.

**Upgrading from 3.1.x or older?** Since 3.2.0, messages and sign text use MiniMessage (`<red>`, `<gold>`) instead of `&` colour codes. Regenerate your `config.yml`, or convert the `messages`, `signs` and `general.prefix` values, otherwise the old codes will show as plain text.

---

## Setting up your first rental

```
/rg define shop1          # 1. Create a WorldGuard region
                          # 2. Place a sign near it and look at it
/zrcreatesign shop1       # 3. Turn it into a rental sign
```

The sign now shows the region as available with its price and duration. Optionally give it its own price or length:

```
/zroverride price world:shop1 500
/zroverride duration world:shop1 14
```

**For players:**

| Action | Result |
|---|---|
| Right-click an available sign | Rent the region |
| Right-click a rented sign | See who rents it and when it expires |
| Shift + right-click your sign | Extend your rental |
| `/zrretrieve` | Collect items from an ended rental |

---

## Commands

All commands start with `/zr` by default. You can change the prefix with `commands.prefix` in `config.yml` (needs a restart). Region arguments accept `shop1` (your current world) or `world_nether:shop1`; the console must always use the `world:region` form.

**Players**

| Command | Description |
|---|---|
| `/zr help` | Help menu |
| `/zrinfo <region>` | Rental status, price, owner and expiry |
| `/zrlist` | Your active rentals |
| `/zrextend <region>` | Extend your rental |
| `/zrretrieve` | Collect stored items |
| `/zrmember add\|remove\|list <region> [player]` | Manage members of your rental |
| `/zrtp <region>` | Teleport to a rental you own or are a member of |

**Admins**

| Command | Description |
|---|---|
| `/zrcreate <world:region>` | Register a region as a rental space (signs can be added later) |
| `/zrcreatesign <region>` | Turn the sign you're looking at into a rental sign (`world:region` for a region in another world) |
| `/zrreset <region>` | End a rental with a full refund (keeps the sign) |
| `/zrremove` / `/zrremove <world:region> <id>` | Remove one rental sign (the one you're looking at, or by ID) |
| `/zrremove all <world:region>` | Remove a region's rental setup completely |
| `/zrduration add\|remove\|set\|reset <region> [time]` | Change a rental's remaining time (e.g. `2d 3h`) |
| `/zroverride <setting> <region\|group:name> <value>` | Per-region or per-group price and duration |
| `/zrgroup create\|edit\|delete\|list\|view` | Group regions for shared settings |
| `/zrlist <player>` | Another player's rentals |
| `/zrrefundhistory <region>` | Refunds issued for a rental |
| `/zrverify` | Check which regions use default or custom settings |
| `/zrreload` | Reload configuration |

Full details: [Commands guide](docs/user-guide/commands.md)

---

## Permissions

| Permission | Default | Grants |
|---|---|---|
| `zonerental.user.*` | everyone | Renting, extending, retrieving, info, list, members, teleport |
| `zonerental.admin.*` | op | All admin commands |
| `zonerental.admin.breaksign` | op | Break rental signs and their support blocks |
| `zonerental.admin.bypass` | op | Skip the `/zrtp` cooldown |
| `zonerental.admin.list.others` | op | `/zrlist <player>` |

Full list: [Permissions guide](docs/user-guide/permissions.md)

---

## Configuration

Key settings in `config.yml`:

| Setting | Default | What it controls |
|---|---|---|
| `economy.default-price` | `100.0` | Rental price when a region has no override |
| `durations.default-days` | `7` | Rental length |
| `extension.extension-days` / `max-extensions` / `price-multiplier` | `7` / `10` / `1.0` | Extensions |
| `limits.max-rentals-per-player` | `3` | Rentals per player |
| `members.max-members` | `5` | Members per rental (`-1` = unlimited) |
| `teleport.cooldown` | `30` | Seconds between `/zrtp` uses |
| `restoration.enabled` | `true` | Snapshot and reset regions |
| `storage.enabled` | `true` | Save items when a rental ends |
| `async-scanning.max-rental-chunks` | `2000` | Largest rentable region, in chunks |

Every option, including which ones are not active yet: [Configuration reference](docs/configuration/config-reference.md) · [Messages](docs/configuration/messages.md)

---

## Known limitations

Please read these before running ZoneRental on a live server:

- **Prefer a restart over `/zrreload`.** Data is saved every 5 minutes and on shutdown. `/zrreload` discards changes from the last few minutes (new rentals, stored items, duration changes).
- **Duration edits may not persist.** Changes made with `/zrduration` can be lost on restart.
- **Some settings aren't applied yet:**
  - Per-region `maxextensions`, `extensionprice`, `allowextensions` and `extensionduration` overrides, and `extension.enabled`. Extensions always use the global `extension` settings.
  - `permission-prices` (VIP pricing), `limits.permission-limits`, custom `notifications.warning-times`, and storage expiry (`max-storage-time`).
- **Item storage gaps:**
  - Player-placed shulker boxes are not saved and are lost when the region resets.
  - Two-block items such as doors and beds are returned twice.
  - Items stored for the same region are replaced if that region's rental ends again before the player collects them.
- **Large regions** can briefly lag the server when an admin runs `/zrreset` or `/zrremove` on them. Regions over 2000 chunks can't be rented.
- **Signs:** place the sign before running `/zrcreatesign`, attached to a block.
- **Windows servers:** region snapshots use `world:region.schem` filenames, which Windows doesn't allow.

Full, up-to-date list: [Known issues](docs/reference/known-issues.md)

---

## Troubleshooting

| Problem | Fix |
|---|---|
| "Vault / WorldGuard / WorldEdit is not installed!" | Install the missing plugin and restart. |
| "Failed to setup economy!" | Install a Vault-compatible economy plugin. |
| Messages show `&a` as plain text | Your config uses the old `&` format; see *Upgrading* above. |
| Commands are `/zr1…` instead of `/zr…` | Another plugin uses the same command names; ZoneRental picks a free prefix. Set `commands.prefix` and restart. |
| Admin help pages say no permission | Admin help pages need `zonerental.admin` (ops have it). |

---

## More documentation

- [Getting started](docs/user-guide/getting-started.md)
- [Installation](docs/user-guide/installation.md)
- [How rentals work](docs/features/rental-lifecycle.md)
- [Overrides & groups](docs/features/overrides-and-groups.md)
- [Item storage & restoration](docs/features/storage-and-restoration.md)
- [Refunds](docs/features/refunds.md)
- [Changelog](CHANGELOG.md)
- [Building from source](docs/development/building.md)

---

## License

ZoneRental is licensed under the **GNU Affero General Public License v3.0**. See [LICENSE](LICENSE).

## Credits

- **Author:** Jean-Luc
- Built for [Paper](https://papermc.io/) with [Adventure / MiniMessage](https://docs.advntr.dev/)
- Region management: [WorldGuard](https://enginehub.org/worldguard) · Block restoration: [WorldEdit](https://enginehub.org/worldedit) by EngineHub
- Economy: [Vault](https://github.com/MilkBowl/VaultAPI)
- Async scheduling: [MCCoroutine](https://github.com/Shynixn/MCCoroutine) and [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines)
- Optional integrations: LuckPerms, EzChestShop
