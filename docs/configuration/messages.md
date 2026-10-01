# Messages & Formatting

## Format: MiniMessage (since 3.2.0)

All configurable text (`messages.*`, `general.prefix`, `signs.available-format`, `signs.rented-format`) is parsed with Adventure **MiniMessage**:

```yaml
rental-success: '<green>You rented <yellow>{region}<green> for <yellow>{days}<green> days!'
```

Legacy `&a` codes are **not** translated; they show up as literal text. Tag reference: https://docs.advntr.dev/minimessage/format.html

## How a message is built

`ConfigManager.getMessage(key, "{placeholder}", value, ...)`:

1. Looks up `messages.<key>` from `config.yml`, falling back to a built-in default in `ConfigManager.loadMessages()`, then to `<red>Missing message: <key>`.
2. Prepends `general.prefix`. There is no `{prefix}` placeholder; the prefix is always added.
3. Does plain string replacement of each placeholder pair.
4. Parses the result with MiniMessage and returns a `Component`.

Placeholder values are inserted **before** MiniMessage parsing, so a value containing `<...>` is interpreted as tags.

Many hard-coded command outputs (usage lines, info screens, the help menu) don't come from `messages.*` and can't be changed in config.

## Message keys the code uses

Only the placeholders listed here are filled in. Any other `{...}` in your text is shown as-is.

| Key | Placeholders supplied |
|---|---|
| `no-permission` | — |
| `region-not-found` | `{region}` |
| `region-too-large` | `{region}`, `{chunks}`, `{max}` |
| `rental-success` | `{region}`, `{days}`, `{price}` |
| `rental-expired` | `{region}` |
| `rental-expiring-soon` | `{region}`, `{time}` |
| `rental-extended` | `{region}`, `{days}`, `{price}` |
| `not-enough-money` | `{amount}` |
| `max-rentals-reached` | **none**: the default text's `{current}/{max}` is shown literally |
| `max-extensions-reached` | **none**: the default text's `{current}/{max}` is shown literally |
| `items-stored` | `{region}`, `{count}` |
| `items-retrieved`, `no-stored-items` | — |
| `sign-created` | `{region}` |
| `sign-protected`, `sign-support-protected` | — |
| `admin-reset-success` | `{region}`, `{player}`, `{amount}` |
| `rental-reset-refund` | `{region}`, `{amount}` |
| `region-removed` | `{region}` |
| `config-reloaded` | — |
| `refund-issued` | `{player}`, `{amount}`, `{reason}` |
| `refund-history-header` | `{region}` |
| `duration-add-charged` | `{days}`, `{region}`, `{player}`, `{amount}` |
| `duration-add-free` | `{days}` (formatted duration text), `{region}` |
| `duration-remove-refunded` | `{days}`, `{region}`, `{player}`, `{amount}` |
| `duration-remove-no-refund` | `{days}`, `{region}` |
| `ezchestshop-removed` | `{region}`. Not in the default `config.yml`; add it to customise. |
| `member-added`, `member-removed` | `{member}`, `{region}` |
| `member-added-notify`, `member-removed-notify` | `{region}`, `{owner}` |
| `member-already-added`, `member-not-found` | `{member}` |
| `member-limit-reached` | `{current}`, `{max}` |
| `member-management-disabled`, `cannot-add-self`, `not-rental-owner` | — |
| `members-list-header` | `{region}` |
| `members-list-entry` | `{member}` |
| `members-list-empty` | — |
| `members-list-footer` | `{count}`, `{max}` |
| `tp-success`, `tp-not-rented`, `tp-no-permission`, `tp-no-safe-location` | `{region}` |
| `tp-cooldown` | `{time}` (seconds) |
| `tp-cross-world-warning` | `{region}`, `{world}` |

## Keys in config.yml that nothing reads

`region-not-setup`, `invalid-region-format`, `region-not-found-in-world`, `already-rented`, `not-your-rental`, `economy-disabled`, `refund-given`, `cooldown-active`, `extension-too-early`, `extension-disabled`, `duration-added`, `duration-removed`, `duration-set`, `duration-invalid`, `duration-too-short`, `refund-already-given`, `refund-partial`, `items-stored-pages`, `storage-expired`, `sign-removed`, `sign-not-rental`, `rental-reset`, `rental-info`, `aliases-enabled`, `aliases-disabled`, `help-header`, `help-footer`.
