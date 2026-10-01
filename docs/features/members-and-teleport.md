# Members & Teleportation

## Members (`/zrmember`)

`MemberCommand` + `RentalManager.addMemberToRental` / `removeMemberFromRental`:

- Only the rental **owner** can add or remove members. Anyone with `zonerental.members` can list the members of any rented region.
- A member is added as a WorldGuard region member and saved in `rentals.yml` (`members:`).
- Limit: `members.max-members` (`-1` = unlimited). The whole feature can be turned off with `members.enabled`.
- You can't add yourself or someone who's already a member.
- Player names are resolved with `Bukkit.getOfflinePlayer(name)`, so a name that has never joined still resolves to a UUID and can be added.
- Added and removed players are notified if online.
- When the rental ends (expiry, `/zrreset`, `/zrremove`), all members are removed from the WorldGuard region.
- Members can teleport with `/zrtp`. They don't receive stored items.

## Teleport (`/zrtp <region>`)

`TpCommand`:

1. Needs `teleport.enabled`, `zonerental.tp`, and the player must be the **owner or a member** of an active rental of that region.
2. Cooldown: `teleport.cooldown` seconds (`TeleportCooldownManager`, in memory, reset on reload). `zonerental.admin.bypass` skips it.
3. Destination search, starting from the region's **sign**:
   - Walk 1 … `forward-search-distance` blocks in the direction the sign faces.
   - At each step, search down `floor-search-down` blocks, then up `floor-search-up` blocks, for a solid, non-dangerous floor with passable feet and head blocks.
   - The player faces away from the sign.
   - Dangerous blocks: lava, fire, soul fire, cactus, magma, wither rose, sweet berry bush, powder snow, campfires.
4. With `cross-world-warning`, a warning is shown when the region is in another world.
5. Optional sound (`ENTITY_ENDERMAN_TELEPORT`) and portal particles.

If the region has no registered sign, teleporting isn't possible.
