# KSBot Core Types

## Topic

The foundational classes and interfaces used by most scripts.

## Purpose

These are the base abstractions we should build around before writing real bot logic.

## Core Types and Methods

- `Script`
  - base class for all scripts
  - provides `ctx`, config file access, storage directory access, and runtime-state checks
- `Paintable`
  - default `paint(Graphics)`
  - default `paint(PanelComponent)`
- `Query<T, Q>`
  - `filter(...)`, `first()`, `exists()`, `empty()`, `count()`
- Wrappers surfaced through the docs:
  - `KSItem`
  - `KSNPC`
  - `KSObject`
  - `KSPlayer`
  - `KSWidget`

## Lifecycle / Usage Pattern

- `ctx` appears to be the primary dependency container exposed by `Script`.
- Queries should be the default way to discover game entities before interaction.
- Paint is optional but available for debugging overlays and path visualization.

## Runtime Requirements

- The docs show many services are constructed with `KSContext`, which strongly suggests `ctx` is the central gateway for script capabilities.
- Queries are fluent and intended for incremental filtering before selecting a target.

## Failure Modes

- Direct raw interactions without query validation will be brittle.
- Assuming `ctx` field names without checking runtime examples is risky; keep wrapper/helper layers thin until we test on KSBot.

## Reusability Decision

- Generic helper candidate:
  - query wrappers
  - state machine scaffolding
  - paint helpers
- Server-specific logic:
  - entity IDs, names, area assumptions, and action priorities
- Template impact:
  - scripts should separate entity selection from action execution

## Human-Like Behavior Notes

- Queries make it easier to validate before clicking, which supports less robotic behavior.

## Source Notes

- Link or citation:
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/scripts/Script.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/interfaces/Paintable.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/queries/Query.html>
  - <https://ksbotxyz.github.io/allclasses.html>
- Open questions:
  - We still need a direct `KSContext` class page or sample script to confirm the public field names exposed on `ctx`.
