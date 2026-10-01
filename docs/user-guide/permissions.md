# Permissions

Defined in `src/main/resources/plugin.yml`.

## Groups

| Node | Default | Children |
|---|---|---|
| `zonerental.*` | op | `zonerental.admin.*`, `zonerental.user.*` |
| `zonerental.admin.*` | op | every `zonerental.admin.<x>` node below |
| `zonerental.user.*` | true | `rent`, `extend`, `retrieve`, `info`, `list`, `member`, `members`, `tp` |

## Player nodes (default: true)

| Node | Used by |
|---|---|
| `zonerental.user` | `/zr` help command |
| `zonerental.rent` | Right-clicking an available sign |
| `zonerental.extend` | Shift-right-click on your sign, `/zrextend` |
| `zonerental.retrieve` | `/zrretrieve` |
| `zonerental.info` | `/zrinfo` |
| `zonerental.list` | `/zrlist` |
| `zonerental.member` | `/zrmember add\|remove` |
| `zonerental.members` | `/zrmember list` |
| `zonerental.tp` | `/zrtp` |

## Admin nodes (default: op)

| Node | Used by |
|---|---|
| `zonerental.admin.reload` | `/zrreload`, `/zr reload` |
| `zonerental.admin.createsign` | `/zrcreatesign` |
| `zonerental.admin.reset` | `/zrreset` |
| `zonerental.admin.remove` | `/zrremove` |
| `zonerental.admin.duration` | `/zrduration` |
| `zonerental.admin.override` | `/zroverride` |
| `zonerental.admin.group` | `/zrgroup` |
| `zonerental.admin.refundhistory` | `/zrrefundhistory` |
| `zonerental.admin.verify` | `/zrverify` |
| `zonerental.admin.list.others` | `/zrlist <player>` |
| `zonerental.admin.breaksign` | Breaking rental signs and their support blocks |
| `zonerental.admin.bypass` | Skips the `/zrtp` cooldown. **This is its only effect.** |

## Undeclared nodes checked by the code

| Node | Where | Effect |
|---|---|---|
| `zonerental.admin` | `RRCommand` help pages 2–3 and tab completion of `reload` | Not declared in `plugin.yml`, so only ops (or players given it explicitly) see the admin help pages. Granting `zonerental.admin.*` alone does **not** include it. |
| keys under `permission-prices` | `SignInteractListener.handleRentRegion` | See [config reference](../configuration/config-reference.md#permission-prices). Currently broken for any permission containing a dot. |

`limits.permission-limits` in `config.yml` (e.g. `zonerental.vip: 5`) is **not read by the code**. Every player gets `limits.max-rentals-per-player`.
