# EzChestShop Integration

`EzChestShopManager`, an optional soft dependency with no compile-time dependency (reflection only).

## Detection

On startup (and on `/zrreload`), the plugin looks for a plugin named `EzChestShop`, `EzChestShopReborn`, `ecs`, `ChestShop` or `ezchestshop`. If it isn't enabled yet, it retries once after 1 second. It then looks up `me.deadlight.ezchestshop.data.ShopContainer` and its static `getShop(Location)` / `isShop(Location)` / `deleteShop(Location)` (or `removeShop(Location)`) methods. The integration is active if a lookup method is found. Removing shops also needs a delete method.

## On expiry / reset

If `integration.ezchestshop.enabled`, during `expireRental` / `expireRentalAsync`, **after** items are stored and **before** the snapshot is restored:

1. Every block in the region's bounding box is checked on the main thread. Chests, trapped chests and barrels that are shops are collected.
2. `deleteShop(location)` is called for each one, and removal is verified.
3. If any shops were removed and `notify-on-removal` is on, the owner gets `messages.ezchestshop-removed` (`{region}` placeholder).

`integration.ezchestshop.removal-message` is no longer read (since 3.2.0). Define `messages.ezchestshop-removed` instead.
