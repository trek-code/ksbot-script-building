# KSBot Inventory And Banking

## Topic

Inventory management, item usage, and bank/deposit flows.

## Purpose

This is the core of any skilling loop that gathers resources, restocks, or handles full inventory states.

## Core Types and Methods

- `Inventory`
  - inherited `Items` helpers such as `contains`, `containsAll`, `getCount`, `getEmptySlots`, `getItem`, `isEmpty`, `isFull`, `query`, `size`
  - `drop`, `dropAll`, `dropAllExcept`
  - `wear`
  - `itemOnItem`
  - `itemOnNpc`
  - `itemOnObject`
- `Bank`
  - `bankDistance`
  - `close`
  - `currentTab`
  - `deposit`, `depositAll`, `depositAllExcept`
  - `depositEquipment`, `depositInventory`, `depositLootingBag`
  - `withdraw(filter, amount, withdrawMode)`
  - `withdraw(id, amount, withdrawMode)`
  - `withdraw(name, amount, withdrawMode)`
  - `setWithdrawMode(boolean noted)`
  - inner inventory and deposit-box support

## Lifecycle / Usage Pattern

- Detect full inventory through `Inventory.isFull()` or item count checks.
- Enter a bank state only after validating route and bank availability.
- Deposit in explicit steps, then withdraw and verify inventory content after each critical action.

## Runtime Requirements

- Bank logic depends on the bank or deposit box being open.
- Withdraws support item-vs-note modes.
- The item system is queryable, so selection should stay filter-based where possible.

## Failure Modes

- Bank widget not open
- wrong bank tab selected
- withdraw mode mismatch
- partially filled inventory after a restock
- RSPS-specific bank widget or deposit-box behavior

## Reusability Decision

- Generic helper candidate:
  - bank validation shell
  - deposit/withdraw verification
  - loadout assertion helpers
- Server-specific logic:
  - item IDs and names
  - restock counts
  - custom bank flows or alternate storage systems
- Template impact:
  - bank state should be its own state machine node, not inline logic

## Human-Like Behavior Notes

- Verify each material bank step instead of firing a large batch of actions back-to-back.
- Use `depositAllExcept` and precise withdraws to avoid chaotic inventory shaping.

## Source Notes

- Link or citation:
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/hooks/Inventory.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/hooks/Bank.html>
- Open questions:
  - We still need runtime confirmation of the best way to open the bank on the target RSPS before calling these methods.
