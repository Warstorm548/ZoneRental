# Commands

## Command prefix

Commands are registered at runtime as `/<prefix><name>` (for example `/zrinfo`). They are not declared in `plugin.yml`; only the bare `/zr` help command is.

- The prefix comes from `commands.prefix` in `config.yml` (default `zr`).
- Validation (`ConfigManager.validatePrefix`): the prefix is lower-cased and must be 2–10 characters of `a-z0-9`. **An invalid or empty prefix falls back to `rr`** in the code, even though the `config.yml` comment says `zr`.
- Conflict handling (`ZoneRental.determineActivePrefix`): if any `/<prefix><sub>` command already exists, the plugin falls back to `zr`. If that also conflicts, it tries `zr1` … `zr99`.
- The prefix is only applied at startup. Changing it needs a server restart; `/zrreload` does not re-register commands.

The examples below assume the default `zr` prefix.

## Region arguments

Commands that take a `<region>` use `WorldRegionParser.parse`:

- `shop1`: the region in the **player's current world**.
- `world_nether:shop1`: explicit world. The world must be loaded, otherwise the command reports an invalid format.
- The console must always use `world:region`.

## Player commands

| Command | Permission | What it does |
|---|---|---|
| `/zr`, `/zr help [page\|command]` | `zonerental.user` | Help. Page 1 lists player commands. Pages 2–3 (admin) check the permission `zonerental.admin`, which is **not defined in `plugin.yml`** (ops have it implicitly). The help pages don't list `member`, `tp` or `group`. |
| `/zr reload` | `zonerental.admin.reload` | Same as `/zrreload`. |
| `/zr list`, `/zr info` | — | **Stubs: they print usage or do nothing.** Use `/zrlist` and `/zrinfo`. |
| `/zrinfo <region>` | `zonerental.info` | Status, price and duration if available; owner, expiry, time left, extensions used and total paid if rented. |
| `/zrlist [player]` | `zonerental.list` (+ `zonerental.admin.list.others` for another player) | Lists a player's active rentals. The console must give a player name. |
| `/zrextend <region>` | `zonerental.extend` | Same as shift-right-clicking your sign (see [Rental lifecycle](../features/rental-lifecycle.md#extension)). |
| `/zrretrieve` | `zonerental.retrieve` | Opens the stored-items GUI (player only). |
| `/zrmember add <region> <player>` | `zonerental.member` | Adds a member (owner only). |
| `/zrmember remove <region> <player>` | `zonerental.member` | Removes a member (owner only). |
| `/zrmember list <region>` | `zonerental.members` | Lists the members of any rented region. |
| `/zrtp <region>` | `zonerental.tp` | Teleports an owner or member near the region's sign. |

## Admin commands

| Command | Permission | What it does |
|---|---|---|
| `/zrreload` | `zonerental.admin.reload` | Reloads config, signs, storage and regions files. **Read the warning in [Known issues](../reference/known-issues.md#data-loss)**: unsaved data is discarded and `groups.yml` is not reloaded. |
| `/zrcreatesign <region>` | `zonerental.admin.createsign` | Registers the sign you are looking at (within 5 blocks) for `<region>` in your current world and protects its support block. |
| `/zrreset <region>` | `zonerental.admin.reset` | Ends an active rental with a net refund (`totalPaid - totalRefunded`). Stores the renter's items, restores the region, and keeps the sign and setup. |
| `/zrremove <region>` | `zonerental.admin.remove` | Resets the rental if active, restores the support block, unregisters the sign, deletes the snapshot, removes the region from its group and removes its overrides (only if it was not in a group). The physical sign is **not** broken when a support block was recorded. |
| `/zrduration add <region> <time> [--charge]` | `zonerental.admin.duration` | Adds time. `--charge` only charges if `duration.charge-for-add: true` **and** the renter is online; otherwise the time is added for free. |
| `/zrduration remove <region> <time>` | `zonerental.admin.duration` | Removes time and refunds proportionally if `duration.refund-on-time-removal`. Refuses if the rental would expire immediately. |
| `/zrduration set <region> <time>` | `zonerental.admin.duration` | Sets the expiry to now + `<time>`. |
| `/zrduration reset <region>` | `zonerental.admin.duration` | Sets the expiry to now + `durations.default-days` (global default; region overrides are ignored). Refunds extension costs if `extension.refund-on-duration-reset`. |
| `/zroverride <setting> <target> <value>` | `zonerental.admin.override` | See [Overrides & groups](../features/overrides-and-groups.md). Settings: `price`, `duration`, `maxextensions`, `extensionprice`, `allowextensions`, `extensionduration`. |
| `/zroverride remove <target>` | `zonerental.admin.override` | Removes all overrides for a region or `group:<name>`. |
| `/zroverride list [target]` | `zonerental.admin.override` | Lists overrides. |
| `/zrgroup create <name> [regions]` | `zonerental.admin.group` | Creates a group. With no regions, the player is prompted in chat (60 s timeout, `cancel` to abort). |
| `/zrgroup edit <name> add\|remove [regions]` | `zonerental.admin.group` | Changes group membership. |
| `/zrgroup delete <name> confirm` | `zonerental.admin.group` | Deletes the group and its overrides. |
| `/zrgroup list` / `view <name>` | `zonerental.admin.group` | Shows groups. |
| `/zrrefundhistory <region>` | `zonerental.admin.refundhistory` | Shows the refund records of an **active** rental. |
| `/zrverify` | `zonerental.admin.verify` | Takes no arguments. Reports which signed regions use defaults or overrides, and which overrides have no sign. Can be disabled with `regions-config.enable-verify-command`. |

## Time format (`/zrduration`)

Parsed by `DurationCommand.parseTimeString`:

- Units: `day(s)`, `hour(s)`/`hr(s)`, `minute(s)`/`min(s)`, plus the short forms `d`, `h`, `m` separated by spaces: `2d 3h 30m`, `2 days 3 hours`.
- Joined short forms such as `1d12h` **are not parsed correctly**: only `12h` is read. Put a space between units.
- `--charge` only bills **whole days** and needs at least 1 day.
