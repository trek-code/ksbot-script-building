# KSBot Logging And Debugging

## Topic

Paint overlays, runtime files, and debugging surfaces.

## Purpose

Good debugging tools matter because physical KSBot testing will happen outside this workspace.

## Core Types and Methods

- `Script`
  - `getConfigFile()`
  - `getStorageDirectory()`
- `Paintable`
  - default paint methods for graphics and panel components
- `Paint`
  - `renderArea(...)`
  - `renderNpcs(...)`
  - `renderObject(...)`
  - `renderPath(...)`
  - `renderTile(...)`
  - `valuePerHour(...)`
- `Timer`
  - elapsed and rate tracking methods for overlays or log output

## Lifecycle / Usage Pattern

- Use the script storage/config paths for durable run artifacts or saved config state.
- Use paint overlays to show routes, targets, work areas, and state diagnostics.
- Use timers and counters to report gains per hour and time spent in a state.

## Runtime Requirements

- Paint is optional but built into the script contract through `Paintable`.
- The current docs do not expose a dedicated logger type in the reviewed pages, so standard script-side logging may still be needed.

## Failure Modes

- Debugging only through console output will slow down physical testing.
- Too much paint or noisy logs can hide the actual failure point.

## Reusability Decision

- Generic helper candidate:
  - standard log prefixes
  - state-aware debug overlay
  - timeout and gain counters
- Server-specific logic:
  - which object IDs or areas should be visualized
- Template impact:
  - every serious bot should be able to show current state, target, and last recovery reason

## Human-Like Behavior Notes

- Logging should explain why the bot chose an action, not only what action it took.

## Source Notes

- Link or citation:
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/scripts/Script.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/interfaces/Paintable.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/game/Paint.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/commons/Timer.html>
- Open questions:
  - We still need to confirm what logs are visible inside KSBot itself during real runs.
