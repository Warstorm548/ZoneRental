# Refunds & Payments

## What a rental tracks (`Rental.kt`)

| Field | Meaning |
|---|---|
| `initialPrice` | Price paid when renting |
| `totalPaid` | Initial price + extensions + charged admin additions |
| `totalRefunded` | Sum of all refunds |
| `refundHistory` | List of `RefundRecord(amount, timestamp, reason, adminName)` |
| `extensionCost` | `totalPaid - initialPrice` |
| `netRefundableAmount` | `max(0, totalPaid - totalRefunded)` |

Every refund goes through `RentalManager.issueRefund`, which caps the amount at `netRefundableAmount`. A player can never get back more than they paid in total.

## Refund sources

| Trigger | Amount | Reason code | Config |
|---|---|---|---|
| `/zrreset`, `/zrremove all` | `netRefundableAmount` | `admin_reset` (admin recorded as "Admin") | always |
| `/zrduration reset` | `extensionCost` (capped) | `duration_reset` | `extension.refund-on-duration-reset` |
| `/zrduration remove` | `(totalPaid - totalRefunded) × removedDays ÷ (endDate - startDate)`, whole days only | `time_removal` | `duration.refund-on-time-removal` |

Refunds are deposited through Vault with `depositPlayer(OfflinePlayer)`, so offline players are refunded directly by the economy plugin. If they're online, they're also notified.

## Charges

| Trigger | Amount |
|---|---|
| Renting | Region price (or a `permission-prices` value; see [config](../configuration/config-reference.md#permission-prices)) |
| Extending | Region price × `extension.price-multiplier` |
| `/zrduration add ... --charge` | `days × per-day price`, where per-day = `duration.add-price-per-day` if > 0, else region price ÷ region duration. Whole days only; the renter must be online and `duration.charge-for-add: true`. |

## Viewing history

`/zrrefundhistory <region>` shows records for **active** rentals only. Once a rental ends, its history is deleted along with the rental.
