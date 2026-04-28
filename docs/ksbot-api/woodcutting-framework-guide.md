# KSBot API Guide For Route-Based Skilling Bots

This guide is based on the local KSBot API jar used in this workspace:

- `D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar`

The goal is to document the real methods we can rely on for woodcutting and future skilling bots, instead of guessing.

## Core Pattern

For KSBot scripts in this workspace, the best baseline pattern is:

1. `onProcess()` runs often and stays light.
2. Query the world for the current truth.
3. Perform one action.
4. Let the next `onProcess()` tick verify the result.

Good examples of truth signals:

- local player tile
- player moving / idle / animating
- inventory full or not
- bank open or not
- object available or not
- object animation state

Avoid relying on long custom waits as the primary decision system.

## Pathing API

Class:

- `rs.kreme.ksbot.api.game.Pathing`

Useful methods:

- `distanceTo(WorldPoint)`
- `isTileLoaded(WorldPoint)`
- `walkable(WorldPoint)`
- `worldToMinimap(WorldPoint)`
- `walkMiniMap(WorldPoint, double)`
- `walkToTile(WorldPoint)`
- `walkTo(WorldPoint)`
- `walkPath(List<WorldPoint>)`
- `shortestPath(WorldPoint, WorldPoint)`
- `shortestSafePath(WorldPoint, WorldPoint, Collection<DangerousTile>)`
- `navThroughWorldPath(List<WorldPoint>)`
- `canReach(WorldPoint)`
- `isReachable(WorldPoint)`
- `onTile(WorldPoint)`
- `inMotion()`
- `toggleRun(boolean)`
- `getRunEnergy()`

Recommended use:

- For nearby exact positioning:
  - use `walkToTile(...)`
- For loaded minimap movement:
  - use `shortestPath(...)`
  - pick a visible step with `worldToMinimap(...)`
  - use `walkMiniMap(...)`
- For longer prebuilt paths:
  - use `navThroughWorldPath(...)`
- For safety checks:
  - use `walkable(...)`, `canReach(...)`, `isReachable(...)`

Notes:

- `walkTo(WorldPoint)` exists, but we should not assume it is a magical better long-range walker.
- For RSPS routes, explicit path segments and stand tiles are usually more reliable than vague destination walking.

## Bank API

Class:

- `rs.kreme.ksbot.api.hooks.Bank`

Useful methods:

- `isOpen()`
- `openBank()`
- `openBank(int)`
- `openBank(Object)`
- `waitForOpen(int)`
- `close()`
- `waitForClose(int)`
- `depositAll(String...)`
- `depositAll(int...)`
- `depositAllExcept(String...)`
- `depositInventory()`
- `depositEquipment()`
- `getClosestBank()`
- `addCustomBankObject(int...)`
- `addCustomBankObject(int, String)`
- `addCustomBankObject(String...)`
- `addCustomBankNPC(int...)`
- `addCustomBankNPC(int, String)`
- `addCustomBankNPC(String...)`
- `addCustomBankObjectWithAction(String, String)`
- `addCustomBankNPCWithAction(String, String)`
- `clearCustomBanks()`

Recommended use:

- For RSPS booths, chests, and custom bankers:
  - prefer `addCustomBankObject(...)` or `addCustomBankNPC(...)` first
  - then use `openBank()`
- Use raw exact object or NPC interaction as a fallback when debugging target resolution or when a specific route proves that direct interaction is more reliable
- After bank opens:
  - use `depositAll(...)` for the resource names you care about
- If the axe is equipped and inventory-only deposit is needed:
  - use `depositInventory()`

Recommended bank flow:

1. Walk to stand tile.
2. Use stand and entrance tiles only to get into reliable interaction range.
3. Register route bank IDs/actions with `addCustomBankObject(...)` or `addCustomBankNPC(...)`.
4. Prefer only true bank-open actions such as `Bank`, `Use`, or `Open`.
5. Use `openBank()`.
6. Always verify with `waitForOpen(...)` before treating the bank as open.
7. If that fails, try exact object interaction by ID / location / action as a route-specific fallback.
8. Deposit while `isOpen()` is true, then `close()` and `waitForClose(...)`.

## Inventory API

Class:

- `rs.kreme.ksbot.api.hooks.Inventory`

Useful methods:

- `isFull()` via inherited item hooks in live usage
- `dropAll(String...)`
- `dropAll(int...)`
- `dropAllExcept(String...)`
- `drop(String...)`
- `contains(...)` via inherited item hooks in live usage
- `getTotalQuantity(...)` via inherited item hooks in live usage
- `wear(...)`
- `useOn(...)`
- `itemOnObject(...)`
- `itemOnNpc(...)`

Recommended use:

- For powercutting:
  - prefer `dropAll(String...)`
- For partial keeps:
  - use `dropAllExcept(...)`
- For fast baseline skilling:
  - avoid manual one-by-one drop loops unless behavior explicitly requires it

## Object API

Class:

- `rs.kreme.ksbot.api.wrappers.KSObject`

Useful methods:

- `getId()`
- `getName()`
- `getActions()`
- `hasAction(String)`
- `interact(String)`
- `interact(String...)`
- `getWorldLocation()`
- `getDistance()`
- `getAnimation()`
- `isAnimating()`

Recommended use:

- For trees:
  - query by `withId(...)`, `withName(...)`, and `withOption("Chop down", "Chop")`
- For bank objects:
  - query by exact IDs plus matching action like `Bank` or `Use`
- When multiple objects are near each other:
  - store exact stand tile
  - store exact target tile
  - sort candidates by target tile distance and stand tile distance

## Tile Object Query API

Class:

- `rs.kreme.ksbot.api.queries.TileObjectQuery`

Useful methods:

- `withId(...)`
- `withName(...)`
- `withOption(...)`
- `withinDistance(int)`
- `atLocation(WorldPoint...)`
- `atLocation(List<WorldPoint>)`
- `inArea(WorldArea)`
- `notAtLocation(...)`
- `closest()`
- `nearestToPlayer()`
- `nearestToPoint(WorldPoint)`
- `list()`
- `first()`
- `count()`
- `animating()`
- `idle()`

Recommended use:

- Broad object lookup:
  - `withId(...)`
  - `withName(...)`
  - `withOption(...)`
- Tight route lookup:
  - `atLocation(...)`
- Tree selection:
  - nearest object in route area
- Debugging:
  - capture candidate list with ID and world tile

## NPC API

Classes:

- `rs.kreme.ksbot.api.hooks.NPCs`
- `rs.kreme.ksbot.api.wrappers.KSNPC`
- `rs.kreme.ksbot.api.queries.NPCQuery`

Useful hook methods:

- `ctx.npcs.getClosest(int...)`
- `ctx.npcs.getClosest(String...)`
- `ctx.npcs.query()`

Useful wrapper methods:

- `getId()`
- `getName()`
- `getActions()`
- `interact(String)`
- `getWorldLocation()`
- `isAlive()`
- `isAnimating()`
- `getInteracting()`

Useful query methods:

- `withId(...)`
- `withName(...)`
- `withinDistance(int)`
- `atLocation(WorldPoint)`
- `closest()`
- `alive()`
- `animating()`
- `idle()`
- `interacting()`
- `interactingWithLocal()`
- `inArea(WorldArea)`

Recommended use:

- For banker-only routes:
  - query banker by exact ID if possible
  - otherwise use `withName(...)` + `withOption(...)`
- For event handling:
  - use `withinDistance(...)` + `closest()`

## Player API

Class:

- `rs.kreme.ksbot.api.wrappers.KSPlayer`

Useful methods:

- `getWorldLocation()`
- `isMoving()`
- `inMotion()`
- `isIdle()`
- `isAnimating()`
- `getAnimation()`
- `getInteracting()`

Recommended use:

- For skilling engagement:
  - do not rely on only one signal
  - combine:
    - `isAnimating()`
    - `isIdle()`
    - `isMoving()`
    - recent inventory gain
    - target object `isAnimating()`

## Query Base API

Class:

- `rs.kreme.ksbot.api.queries.Query`

Useful generic methods:

- `withId(...)`
- `withName(...)`
- `withOption(...)`
- `withoutId(...)`
- `withoutName(...)`
- `withoutOption(...)`
- `filter(...)`
- `omit(...)`
- `exists()`
- `empty()`
- `count()`
- `list()`
- `first()`
- `last()`
- `closest()` through subclass support
- `random()`

Recommended use:

- Build narrow queries first.
- Avoid over-querying giant candidate sets when exact route data exists.
- For RSPS routes, combine exact IDs and exact tiles whenever possible.

## Practical Framework Rules

For future bots in this workspace:

1. Routes should store:
   - target object IDs
   - target object tiles
   - bank stand tiles
   - bank target IDs
   - bank target tiles
   - optional door and inside-bank tiles

2. Movement should use:
   - `shortestPath(...)`
   - `walkMiniMap(...)`
   - `walkToTile(...)`
   - `navThroughWorldPath(...)`

3. Banking should use:
   - `addCustomBankObject(...)` or `addCustomBankNPC(...)` first for RSPS routes
   - exact target lookup as a fallback or debug path
   - `openBank()`
   - `depositAll(...)`

4. Tree interaction should use:
   - `TileObjectQuery`
   - exact tree IDs
   - exact route tiles
   - route area fallback

5. Debugging should always log:
   - player tile
   - move target
   - path destination
   - path failure
   - bank stand tile
   - bank target candidates
   - actual target interacted with

## Current Varrock West Lesson

The recent Varrock West issue is a good example of why this guide matters:

- movement to the stand tile was correct
- bank interaction still failed
- the root cause was target resolution on a private-server booth with multiple possible object IDs

So the correct fix pattern is:

1. exact stand tile
2. exact target tile
3. allow all known booth IDs for that target
4. log all candidate booth IDs and locations
5. fall back to KSBot custom bank registration if raw object interaction fails
