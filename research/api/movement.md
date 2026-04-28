# KSBot Movement And Pathing

## Topic

Navigation, tile movement, reachability, and danger-aware path logic.

## Purpose

Movement is the backbone of any skilling bot, and KSBot exposes a fairly rich `Pathing` helper for route control.

## Core Types and Methods

- Class or interface: `rs.kreme.ksbot.api.game.Pathing`
- Important methods:
  - `walkPoint(LocalPoint)`
  - `walkPoint(WorldPoint)`
  - `walkPoint(int x, int y)`
  - `walkToTile(WorldPoint)`
  - `walkMiniMap(WorldPoint, double zoomDistance)`
  - `shortestPath(start, end)`
  - `walkPath(List<WorldPoint>)`
  - `walkPath(WorldPoint[] path, boolean reverse)`
  - `onTile(...)`
  - `walkable(...)`
  - `distanceTo(...)`
  - `inRegion(...)`, `getRegion()`, `inInstance()`
  - danger helpers such as `calculateDanger`, `countDangerousTiles`, `withinDanger`, and `withinReaction`

## Lifecycle / Usage Pattern

- Use query logic to choose the next target.
- Use `distanceTo`, `walkable`, and `onTile` before moving.
- Use `shortestPath` or a fixed path array when the route is stable.
- Use `walkPath` or `walkToTile` rather than spamming direct minimap clicks.

## Runtime Requirements

- The pathing helper is context-bound and appears to rely on loaded scene tiles.
- Some path data is cached for efficiency.
- World-point and local-point conversion is built in.

## Failure Modes

- Movement can fail if tiles are unloaded, unreachable, blocked, or out of scene.
- Shortest-path output still needs validation for RSPS-specific obstacles and instanced areas.
- A script should treat long navigation time or repeated no-progress as recovery triggers.

## Reusability Decision

- Generic helper candidate:
  - route timeout checks
  - path-following shell
  - distance validation
  - dangerous-tile wrappers
- Server-specific logic:
  - exact route nodes
  - special teleports
  - custom map blockers and instance transitions
- Template impact:
  - every skilling template should isolate path selection from business logic

## Human-Like Behavior Notes

- Prefer path helpers over repetitive direct clicks.
- Use distance and reachability checks before reissuing movement.
- Randomize dwell time around transitions instead of hard looping on movement failures.

## Source Notes

- Link or citation:
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/game/Pathing.html>
- Open questions:
  - The docs do not yet show an end-to-end server travel helper like a canonical web walker; we should confirm during runtime testing whether `walkToTile` is sufficient for long routes.
