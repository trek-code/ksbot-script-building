# KSBot Interactions

## Topic

Finding and interacting with objects, NPCs, players, and ground entities.

## Purpose

Most skilling loops are “query, validate, interact, verify,” and the docs suggest KSBot is built around that model.

## Core Types and Methods

- `GameObjects`
  - `query()`
  - `findTileObject(...)`
  - object ID comparison helpers with impostor support
- `NPCs`
  - `query()`
  - `hasLOS(...)`
  - `getNearbyPlayerCount(...)`
  - `itemOnNpc(...)`
- Query package highlights:
  - `TileObjectQuery`
  - `NPCQuery`
  - `PlayerQuery`
  - `TileItemQuery`
  - generic `Query<T, Q>`

## Lifecycle / Usage Pattern

- Use `query()` first to narrow to valid visible entities.
- Validate line of sight, nearby player pressure, and distance before interacting.
- Keep object or NPC selection in small helper methods so they can be swapped per RSPS.

## Runtime Requirements

- Queries are cached per game tick for efficiency.
- Game objects cover wall, ground, decorative, and game objects in the visible scene.
- NPC queries operate on active NPCs only.

## Failure Modes

- Entity IDs may differ per RSPS or have impostor variants.
- Visibility and line of sight can change between selection and action.
- Busy targets, crowded areas, or nearby-player interference can make repeated interaction look robotic or fail often.

## Reusability Decision

- Generic helper candidate:
  - query pipeline shape
  - LOS and distance gating
  - “best target” selection pattern
- Server-specific logic:
  - names, IDs, action strings, and prioritization
- Template impact:
  - interaction code should not hardcode selection logic inside the main loop

## Human-Like Behavior Notes

- Nearby-player counts around an NPC are a useful signal for pacing or avoiding crowded targets.
- Impostor-aware object matching reduces bad clicks on morphing or transformed objects.

## Source Notes

- Link or citation:
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/hooks/GameObjects.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/hooks/NPCs.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/queries/package-summary.html>
- Open questions:
  - We still need to verify the best wrapper-level interaction methods for `KSObject` and `KSNPC` during runtime examples.
