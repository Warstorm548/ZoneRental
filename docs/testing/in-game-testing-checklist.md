# In-Game Testing Checklist

Manual in-game test plan for ZoneRental (current: 3.2.0). Items marked **(known issue)** are expected to fail with the current code; see [Known issues](../reference/known-issues.md).

## Prerequisites

Before testing, ensure you have:
- [ ] Paper 1.21+ server running
- [ ] Vault plugin installed
- [ ] WorldGuard 7.0.14+ installed
- [ ] WorldEdit 7.3.16+ installed
- [ ] Economy plugin installed (EssentialsX, CMI, etc.)
- [ ] ZoneRental JAR in `plugins/` folder
- [ ] Server restarted after installation
- [ ] Test player account with funds

## Quick Start Test

Basic functionality verification:
1. [ ] Create WorldGuard region: `/rg define testshop`
2. [ ] Place a sign and look at it
3. [ ] Create rental sign: `/zrcreatesign testshop`
4. [ ] Right-click sign to rent
5. [ ] Verify player added to region members
6. [ ] Shift-click sign to extend
7. [ ] Run `/zrreset testshop` to reset with refund

---

## Core Rental System

### Sign Creation
- [ ] `/zrcreatesign <region>` creates a rental sign
- [ ] Sign displays correct format (AVAILABLE, price, region name)
- [ ] Sign redraws within 30 seconds of a status or override change
- [ ] Invalid region name shows error message

### Renting a Region
- [ ] Right-click available sign to rent
- [ ] Money deducted from player balance
- [ ] Player added to WorldGuard region members
- [ ] Sign updates to RENTED format (owner, expiry date)
- [ ] Rental confirmation message displayed
- [ ] Rental written to `rentals.yml` after autosave (5 min) or shutdown

### Extending a Rental
- [ ] Shift-click owned rental sign to extend
- [ ] Extension payment deducted
- [ ] Time added to rental
- [ ] Extension count incremented
- [ ] Extension limit enforced (global `extension.max-extensions`)
- [ ] Extension cost = region price × `extension.price-multiplier`
- [ ] Cannot extend someone else's rental

### Rental Expiration
- [ ] Expiration warnings sent at configured intervals (24h, 12h, 6h, 1h)
- [ ] Player removed from region on expiration
- [ ] Sign updates to AVAILABLE
- [ ] Items stored for retrieval
- [ ] Blocks restored (if enabled)

### Rental Limits
- [ ] `max-rentals-per-player` enforced
- [ ] Clear error message when limit reached
- [ ] Existing rentals count correctly

---

## Sign System

### Sign Protection
- [ ] Rental signs cannot be broken by regular players
- [ ] Admin with `zonerental.admin.breaksign` can break signs
- [ ] Breaking sign shows permission denied message

### Support Block Protection
- [ ] Wall sign: Block behind sign is protected
- [ ] Standing sign: Block below sign is protected
- [ ] Support block cannot be broken by players
- [ ] Support block restored on `/zrremove` (sign block stays in place)

### Sign Updates
- [ ] Sign updates when rental status changes
- [ ] `{days}`/`{hours}` on signs refresh only on state change **(known issue: they go stale)**
- [ ] Sign shows correct player name
- [ ] MiniMessage formatting displays correctly (legacy `&` codes show literally)

---

## Economy & Refunds

### Payments
- [ ] Rental payment deducted correctly
- [ ] Extension payment deducted correctly
- [ ] Insufficient funds shows error message
- [ ] Currency format displays correctly

### Refunds
- [ ] `/zrreset` refunds full amount (initial + extensions)
- [ ] `/zrremove` refunds if region was rented
- [ ] Refund message shows correct amount
- [ ] Player balance updated correctly
- [ ] Offline player refunded directly via Vault

### Per-Region Pricing
- [ ] Default price used when no override
- [ ] Region-specific price override works
- [ ] Group price override works
- [ ] Permission-based pricing **(known issue: dotted permission keys don't work)**

---

## Block Restoration

### Capture
- [ ] Region state captured when rental starts
- [ ] `.schem` file created in `schematics/` folder (Sponge format)
- [ ] Schematic file is not empty (> 1KB)
- [ ] Entities captured (always on)

### Restoration
- [ ] Blocks restored on rental expiration
- [ ] Blocks restored on `/zrreset`
- [ ] Blocks restored on `/zrremove`
- [ ] Entities restored, not duplicated **(verify: see known issue X3)**
- [ ] Schematic deleted after restoration (if configured)

### Configuration
- [ ] `restoration.enabled` toggle works
- [ ] `restoration.auto-delete-schematics` toggle works
- [ ] `restoration.schematic-cache-size` limits memory usage

---

## Item Storage

### Item Collection
- [ ] Items collected from chests on expiration
- [ ] Items collected from barrels
- [ ] Player-placed shulker boxes preserved **(known issue D4: they are lost)**
- [ ] Doors/beds stored as one item **(known issue X1: stored per half)**
- [ ] Items collected from hoppers, dispensers, droppers
- [ ] Items collected from furnaces, blast furnaces, smokers
- [ ] Items collected from brewing stands

### Item Retrieval
- [ ] `/zrretrieve` opens GUI
- [ ] Stored items displayed in GUI
- [ ] Taking an item moves it to the player inventory
- [ ] Placing items into the GUI is blocked (shift-click, number key, cursor)
- [ ] Multiple pages work for large collections
- [ ] Items removed from storage after retrieval

---

## Member Management

### Adding Members
- [ ] `/zrmember add <region> <player>` adds member
- [ ] Member added to WorldGuard region
- [ ] Member can build in rented region
- [ ] Member limit enforced (`members.max-members`)
- [ ] Cannot add yourself as member
- [ ] Cannot add duplicate members
- [ ] Only renter can add members

### Removing Members
- [ ] `/zrmember remove <region> <player>` removes member
- [ ] Member removed from WorldGuard region
- [ ] Only renter can remove members

### Listing Members
- [ ] `/zrmember list <region>` shows member list
- [ ] Shows correct member count

### Member Cleanup
- [ ] Members removed when rental expires
- [ ] Members removed when rental reset
- [ ] Member data cleared from `rentals.yml`

---

## Teleportation

### Basic Teleportation
- [ ] `/zrtp <region>` teleports to rented region
- [ ] Works for rental owner
- [ ] Works for members
- [ ] Cannot teleport to non-owned regions
- [ ] Error message for invalid regions

### Safe Location Detection
- [ ] Finds safe location in front of sign
- [ ] Avoids dangerous blocks (lava, fire, cactus, etc.)
- [ ] Finds solid floor
- [ ] Ensures 2-block head clearance
- [ ] Works for wall signs
- [ ] Works for standing signs

### Cooldown System
- [ ] Cooldown enforced between teleports
- [ ] Cooldown message shows remaining time
- [ ] Admin bypass permission works

### Cross-World Teleportation
- [ ] Teleports to regions in other worlds
- [ ] Cross-world warning displayed (if enabled)
- [ ] World name shown in confirmation

### Effects
- [ ] Teleport sound plays (if enabled)
- [ ] Particle effects display (if enabled)

---

## Region Grouping

### Creating Groups
- [ ] `/zrgroup create <name>` creates group
- [ ] `/zrgroup create <name> <regions>` with inline regions
- [ ] Chat prompt for regions works
- [ ] Group names validated (2-30 chars, alphanumeric + underscore)
- [ ] Reserved names blocked ("all", "none", "default")

### Managing Groups
- [ ] `/zrgroup edit <name> add <regions>` adds regions
- [ ] `/zrgroup edit <name> remove <regions>` removes regions
- [ ] Regions can only be in one group
- [ ] Region existence validated

### Viewing Groups
- [ ] `/zrgroup list` shows all groups
- [ ] `/zrgroup view <name>` shows group details

### Deleting Groups
- [ ] `/zrgroup delete <name>` requires confirmation
- [ ] `/zrgroup delete <name> confirm` deletes group
- [ ] Group overrides removed on deletion

### Group Overrides
- [ ] `/zroverride price group:<name> <value>` sets group price
- [ ] All regions in group use group override
- [ ] Signs update when group override set
- [ ] Individual overrides cleared when added to group

---

## Override System

### Region Overrides
- [ ] `/zroverride price <region> <value>` works
- [ ] `/zroverride duration <region> <value>` works
- [ ] `/zroverride maxextensions <region> <value>` is saved **(known issue: not applied)**
- [ ] `/zroverride extensionprice <region> <value>` is saved **(known issue: not applied)**
- [ ] `/zroverride allowextensions <region> <value>` is saved **(known issue: not applied)**
- [ ] `/zroverride extensionduration <region> <value>` is saved **(known issue: not applied)**
- [ ] Region in a group rejects individual overrides
- [ ] `/zroverride list <region>` shows overrides

### Override Priority
- [ ] Group override takes precedence over individual
- [ ] Individual override takes precedence over default
- [ ] Default used when no overrides

### Verification
- [ ] `/zrverify` reports orphaned configs
- [ ] `/zrverify` lists regions using defaults vs overrides

---

## Multi-World Support

### Cross-World Regions
- [ ] Create rental sign in different world
- [ ] Same region name in different worlds works
- [ ] World prefix format: `world:region`

### World-Aware Commands
- [ ] Commands accept `world:region` format
- [ ] Commands default to player's current world
- [ ] Tab completion includes world prefixes

### Data Storage
- [ ] Rentals stored with world name
- [ ] Signs stored with world name
- [ ] Schematics stored with world prefix

---

## Admin Commands

### Reset Command
- [ ] `/zrreset <region>` resets rental
- [ ] Full refund issued
- [ ] Player removed from region
- [ ] Sign updated to AVAILABLE
- [ ] Rental setup preserved (sign, schematic)

### Remove Command
- [ ] `/zrremove <region>` removes setup
- [ ] Refund issued (if rented)
- [ ] Sign config removed
- [ ] Region removed from its group
- [ ] Support block restored
- [ ] Schematic deleted

### Duration Command
- [ ] `/zrduration add <region> <time>` adds time
- [ ] `/zrduration remove <region> <time>` removes time
- [ ] `/zrduration set <region> <time>` sets exact time
- [ ] `/zrduration reset <region>` resets to `durations.default-days`
- [ ] Duration changes survive a restart **(known issue D2)**
- [ ] `2d 3h` parses; `1d12h` **(known issue: parses as 12h)**
- [ ] Extension refund on reset (if configured)

### Info Command
- [ ] `/zrinfo <region>` shows rental details
- [ ] Shows renter, time remaining, extensions

### List Command
- [ ] `/zrlist` shows your rentals
- [ ] `/zrlist <player>` shows that player's rentals (needs `zonerental.admin.list.others`)

### Reload Command
- [ ] `/zrreload` reloads configuration
- [ ] Changes take effect immediately
- [ ] No data loss on reload **(known issue D1: unsaved changes are discarded)**

---

## Configuration

### Config Reload
- [ ] Changes to `config.yml` applied on reload
- [ ] Messages updated on reload
- [ ] Prices updated on reload

### Data Persistence
- [ ] Rentals persist across restarts
- [ ] Signs persist across restarts
- [ ] Groups persist across restarts
- [ ] Stored items persist across restarts

### Auto-Save
- [ ] Data auto-saved every 5 minutes
- [ ] Dirty tracking prevents unnecessary saves

---

## Edge Cases

### Error Handling
- [ ] Invalid region name shows error
- [ ] Invalid player name shows error
- [ ] Invalid command syntax shows usage
- [ ] Offline player handling works

### Concurrent Operations
- [ ] Multiple players renting simultaneously
- [ ] Rapid sign clicks handled correctly
- [ ] Extension during expiration check

### Data Integrity
- [ ] Server crash recovery
- [ ] Corrupted data handling
- [ ] Missing schematic handling

---

## Notes

**Test Environment:**
- Server version: _______________
- Plugin version: _______________
- Date tested: _______________
- Tester: _______________

**Issues Found:**
1.
2.
3.
