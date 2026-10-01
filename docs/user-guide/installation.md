# Installation

## Requirements

| Requirement | Version | Notes |
|---|---|---|
| Server | Paper 1.21+ (`api-version: '1.21'`) | Built against Paper API 1.21.3. Messages use Paper's Adventure/MiniMessage API, so plain Spigot is not a supported target. |
| Java | 21+ | |
| Vault | 1.7+ | Hard dependency (`depend` in `plugin.yml`). |
| An economy plugin | any Vault-compatible | EssentialsX, CMI, etc. The plugin disables itself if no Vault economy provider is registered. |
| WorldGuard | 7.0.14+ | Hard dependency. |
| WorldEdit | 7.3.16+ | Hard dependency (used for region snapshots/restoration). |
| LuckPerms | optional | Listed as a soft dependency. The plugin only uses normal Bukkit permission checks, so any permissions plugin works. |
| EzChestShop / EzChestShopReborn | optional | Detected at runtime; see [EzChestShop integration](../features/ezchestshop-integration.md). |

## Steps

1. Build the JAR (see [Building](../development/building.md)) or download a release.
2. Copy `ZoneRental-<version>.jar` into the server's `plugins/` folder.
3. Install Vault, an economy plugin, WorldGuard and WorldEdit.
4. Restart the server. Avoid `/reload`.
5. Edit `plugins/ZoneRental/config.yml` (see [config.yml reference](../configuration/config-reference.md)), then run `/zrreload` or restart.

## Startup checks

On enable, `ZoneRental.onEnable()`:

1. Saves the default `config.yml`, creates `signs.yml`, `storage.yml`, `regions.yml`, `groups.yml` and runs the one-time migrations.
2. Checks that Vault, WorldGuard and WorldEdit are present. If any is missing, the plugin disables itself.
3. Hooks the Vault economy. If there is no provider, the plugin disables itself ("Failed to setup economy!").
4. Creates the managers, loads `rentals.yml` and `signs.yml`, registers commands and listeners, and starts the scheduled tasks.

## Upgrading to 3.2.0

3.2.0 switched every message and sign format from legacy `&` colour codes to **MiniMessage** (`<red>`, `<gold>`, ...). An existing `config.yml` that still uses `&` codes shows those codes as literal text. Regenerate the file, or convert each `messages.*`, `signs.*-format` and `general.prefix` value. See [Messages & formatting](../configuration/messages.md).
