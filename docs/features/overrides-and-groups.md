# Overrides & Groups

## Per-region overrides (`/zroverride`)

```
/zroverride <setting> <world:region|region|group:<name>> <value>
/zroverride remove <target>
/zroverride list [target]
```

Stored in `regions.yml` ([format](../configuration/data-files.md#regionsyml)). The region must exist in WorldGuard. A region that belongs to a group can't get individual overrides; use `group:<name>`.

| Setting | Value | Stored key | **Applied by the code?** |
|---|---|---|---|
| `price` | ≥ 0 | `price` | ✅ Rental price, signs, `/zrinfo`, extension cost base |
| `duration` | > 0 days | `duration` | ✅ Rental length, signs, `/zrinfo` |
| `maxextensions` | ≥ 0 | `max-extensions` | ❌ Stored only; the global `extension.max-extensions` applies |
| `extensionprice` | ≥ 0 (0 = auto) | `extension-price` | ❌ Stored only |
| `allowextensions` | true/false | `allow-extensions` | ❌ Stored only |
| `extensionduration` | > 0 days | `extension-duration` | ❌ Stored only; the global `extension.extension-days` applies |

`ConfigManager` has `get*ForRegion` helpers for the four unapplied settings, but no code calls them.

Setting an override marks the sign for redraw (within 30 s).

## Groups (`/zrgroup`)

Groups apply one set of overrides to many regions.

- Membership: `groups.yml`. Group overrides: `regions.yml` under `groups.<name>`.
- Group names: 2–30 characters of `[A-Za-z0-9_]`; `all`, `none` and `default` are reserved.
- A region can be in only **one** group.
- **Adding a region to a group deletes its individual overrides.**
- Regions are given as a comma-separated list: `shop1,shop2,world_nether:shop3`. Names without a world use the player's world; the console needs `world:region`.
- If no regions are given, a player is prompted in chat (`GroupChatListener`): 60 s timeout, `cancel`/`stop` to abort.
- `/zrgroup delete <name> confirm` removes the group and its overrides; members' individual overrides are **not** restored.
- `/zrremove` removes the region from its group.

The `/zrgroup view` footer suggests `/zroverride <setting> <group> <value>`; the correct target is `group:<group>`.

## Lookup priority

For each setting (`RegionsConfig.getRegion*`):

1. **Group override**, if the region is in a group and the group sets it
2. **Region override**
3. **Global default** from `config.yml`

For `extension-price`, a value ≤ 0 falls through to the default.

## Verification

- On startup/reload (if `regions-config.auto-verify-regions`), overrides for regions with no sign are logged as "orphaned".
- `/zrverify` lists signed regions using defaults or overrides, and orphaned overrides. Group overrides aren't included in this report.
