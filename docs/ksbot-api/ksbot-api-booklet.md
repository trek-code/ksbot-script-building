# KSBot API Booklet

Developer reference for building KSBot scripts in this workspace.

This booklet is based on two source layers:

1. The official KSBot documentation pages from `https://ksbot.org/docs/...`
2. Direct local signature verification against the installed API jar used in this workspace:
   - `D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar`

Where the official website is heavily client-rendered, this booklet favors:

- official page structure and guidance from the site
- local `javap` verification for exact method names and available hook surfaces

That means this document is optimized for coding correctly against KSBot, not for reproducing every sentence from the docs site.

The jar-derived companion reference lives here:

- `D:\Codex GPT\RSPS\KSBOT Script building\docs\ksbot-api\reference\README.md`

## Index

1. Scope
2. Core KSBot Mental Model
3. Script Structure
4. Script Manifest
5. Lifecycle
6. Configuration and GUI
7. Paint, Stats, and Debugging
8. Events and Input Listeners
9. Deployment and Loading
10. KSContext
11. Commons
12. Game APIs
13. Hook APIs
14. Queries
15. Wrappers
16. Canonical Patterns for Our Framework
17. Banking Pattern
18. Pathing Pattern
19. Object and NPC Interaction Pattern
20. Woodcutting Route Pattern
21. Example-Script Lessons
22. Website API Notes
23. Rules For Future Agents
24. Known Gaps and Verification Notes
25. Official Source Index

## 1. Scope

This booklet is for:

- KSBot local scripts
- route-driven RSPS bots
- Reason-focused development
- future reusable skilling framework work

It is especially meant to prevent guesswork when we have an official KSBot surface available.

## 2. Core KSBot Mental Model

The safest KSBot model is:

1. `onProcess()` runs often.
2. Each tick checks the current truth.
3. The script performs one meaningful action.
4. The next tick verifies the result.

This is a loop-driven framework, not a large custom sleep framework.

In practice, the truth should come from KSBot state:

- `ctx.bank.isOpen()`
- `ctx.inventory.isFull()`
- `ctx.players.getLocal().isIdle()`
- `ctx.players.getLocal().isMoving()`
- object availability and object actions from queries
- path availability and reachability from `ctx.pathing`

Timing should be a light throttle, not the main source of state truth.

## 3. Script Structure

The official quick-start path is:

- extend `Script`
- annotate the class with `@ScriptManifest`
- implement lifecycle methods such as `onStart()`, `onProcess()`, `onBreak()`, and `onStop()`

The quick-start example also demonstrates:

- using `ctx` directly inside the script
- querying game objects with `.query()`
- using `ctx.inventory.dropAll(...)`
- basic player-state checks like `ctx.players.getLocal().isIdle()`

For our framework, the right structure is:

- one script class per bot entrypoint
- one settings model
- one route/profile model
- one UI class
- minimal helper methods for pathing, banking, and interaction

## 4. Script Manifest

The manifest is the identity layer of a KSBot script.

Typical manifest fields include:

- `name`
- `author`
- `servers`
- `description`
- `version`
- `category`

The manifest decides:

- how the script appears in the client
- which servers it targets
- how it is categorized in the script list

Practical rule:

- treat manifest values as stable product metadata, not temporary debug labels

## 5. Lifecycle

The official lifecycle docs and examples imply this division:

- `onStart()`: initialize UI, counters, defaults, debug boot info
- `onProcess()`: main loop
- `onBreak()`: allow or deny KSBot-managed breaks depending on safe state
- `onStop()`: cleanup and final reporting

Our framework rules:

- `onStart()` should be cheap
- `onProcess()` should stay fast and fact-driven
- `onStop()` should not assume game state is still valid
- `onBreak()` should only allow breaks when the player is not in a fragile action state

## 6. Configuration and GUI

The official docs split script configuration into:

- basic config
- config GUI

That aligns well with our approach:

- settings object stores current route, mode, toggles, and stop conditions
- GUI edits settings
- script loop consumes a stable snapshot of settings

Good KSBot configuration principles:

- avoid letting UI widgets become the source of truth directly in the action loop
- convert GUI state into a settings object
- on update, replace settings cleanly

## 7. Paint, Stats, and Debugging

The official docs expose:

- KS Paint
- statistics-oriented helpers
- timing utilities

For our local framework, there are three useful debug layers:

1. UI session snapshot
2. live debug console
3. export-to-file trace

Best practice:

- use UI for current state
- use debug event log for transitions and failures
- use exportable text debug for reproducible route failures

## 8. Events and Input Listeners

The scripting docs include:

- events
- input listeners

This means KSBot supports more than pure polling. Even so, our current framework should still remain polling-first unless there is a specific event-driven gain:

- UI input events for controls
- optional game-event hooks when they add reliability

Do not overcomplicate routine skilling state with custom event webs if `onProcess()` can already express it cleanly.

## 9. Deployment and Loading

The official quick-start and deployment docs imply this standard flow:

1. compile against the correct server API jar
2. produce `.class` or `.jar`
3. load it through the KSBot script system

In this workspace, the practical deployment path is:

- build into `handoff/packages/`
- deploy into the local Reason scripts folder under `.kreme`

Practical rule:

- if a script does not appear in client, check manifest identity, package path, and class initialization safety before assuming the loader is broken

## 10. KSContext

The official docs present `ctx` as the central access point.

That matches local reality.

`ctx` is the script’s gateway to:

- bank
- inventory
- equipment
- players
- NPCs
- game objects / ground objects
- pathing
- widgets
- skills
- chat
- teleporter
- production
- statistics and other modules

Framework rule:

- if a workflow can be expressed through a KSBot hook on `ctx`, prefer that over reimplementing it manually

## 11. Commons

The official docs list commons modules such as:

- paint
- random
- timer

How to think about them:

- `Random`: jitter and lightweight pacing, not fake state logic
- `Timer`: runtime tracking and stop conditions
- `Paint`: overlays or lightweight render instrumentation when needed

Framework rule:

- never let random delays replace real KSBot state checks

## 12. Game APIs

The official game category includes:

- combat
- consumables
- discord
- game
- lootable
- pathing
- prayer
- presets
- statistics
- tiles
- vars

For our skilling framework, the most relevant are:

- `pathing`
- `statistics`
- `tiles`
- `vars`
- `presets` in servers where setup depends on them

### Pathing

The official pathing docs and local jar both support the idea that KSBot pathing is rich enough to avoid custom movement engines for normal route work.

Locally verified methods include:

- `walkPoint(WorldPoint)`
- `walkToTile(WorldPoint)`
- `walkTo(WorldPoint)`
- `walkMiniMap(WorldPoint, double)`
- `walkPath(List<WorldPoint>)`
- `shortestPath(WorldPoint, WorldPoint)`
- `navThroughWorldPath(List<WorldPoint>)`
- `canReach(WorldPoint)`
- `isReachable(WorldPoint)`
- `worldToMinimap(WorldPoint)`
- `distanceTo(...)`
- `onTile(WorldPoint)`
- `inMotion()`
- `toggleRun(boolean)`
- `getRunEnergy()`

Framework conclusion:

- use KSBot pathing as the base
- use route tiles to constrain target choice
- use stand tiles and approach tiles only where pathing alone is insufficient

## 13. Hook APIs

The official hooks section includes:

- bank
- chat
- dialog
- equipment
- friends
- game-objects
- graphics-objects
- inventory
- keyboard
- magic
- npcs
- players
- production
- shop
- skills
- teleporter
- trade
- widgets

### Bank

This is one of the most important hooks for our framework.

Locally verified methods include:

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
- `addCustomBankObject(int...)`
- `addCustomBankObject(int, String)`
- `addCustomBankObject(String...)`
- `addCustomBankObjectWithAction(String, String)`
- `addCustomBankNPC(int...)`
- `addCustomBankNPC(int, String)`
- `addCustomBankNPC(String...)`
- `addCustomBankNPCWithAction(String, String)`
- `clearCustomBanks()`
- `getClosestBank()`

Framework conclusion:

- for RSPS booths, chests, and custom bankers, custom bank registration plus `openBank()` is the intended primary flow
- `waitForOpen(...)` and `waitForClose(...)` should be treated as truth gates
- raw object interaction should be fallback/debug behavior, not the default engine

### Inventory

The inventory hook should handle:

- fullness checks
- item quantity checks
- dropping
- keeping exceptions
- gear usage or interactions when needed

For skilling:

- prefer `dropAll(...)` or `depositAll(...)`
- avoid hand-written inventory loops unless the API cannot express the behavior

### Players

Player hooks matter for:

- idle checks
- motion checks
- animation checks
- local player location

Framework rule:

- use player state to decide whether to issue the next action
- do not infer busy state from a fixed timer when KSBot already knows whether the player is moving or idle

### Game Objects

Game-object hooks plus object queries are the backbone of:

- chopping trees
- interacting with booths and chests
- identifying route-critical objects

### NPCs

NPC hooks matter for:

- banker fallback routes
- special production or shop routes
- random-event or event-object interactions where the server uses NPCs instead of objects

## 14. Queries

The official query docs split by entity type:

- item
- NPC
- player
- object
- ground item
- widget
- chat
- equipment
- friend
- graphics

The query mental model is:

1. start from the hook’s `.query()`
2. chain filters
3. resolve with `closest()`, `nearestToPlayer()`, `list()`, `first()`, or similar

For our object work, the most useful object-query style is:

- `withId(...)`
- `withName(...)`
- `withOption(...)`
- `withinDistance(...)`
- `atLocation(...)`
- `inArea(...)`
- `closest()`
- `nearestToPlayer()`
- `list()`
- `first()`
- `count()`

Framework rule:

- prefer exact route tiles first
- then route area
- only then broader nearby fallbacks

## 15. Wrappers

The official wrapper docs include:

- `KSGroundItem`
- `KSItem`
- `KSNPC`
- `KSObject`
- `KSPlayer`
- `KSWidget`

For our framework, three wrappers matter most:

### KSObject

Locally verified methods include:

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

### KSPlayer

We rely on:

- idle state
- moving state
- animation state
- world location

### KSNPC

Relevant for:

- banker routes
- special NPC interactions
- combat or event systems in future scripts

## 16. Canonical Patterns for Our Framework

These are the patterns we should keep reusing.

### State machine pattern

Keep states small:

- move to target
- interact
- verify
- transition

### One-action-per-tick pattern

Each `onProcess()` pass should:

- observe
- act once
- return

### Route-first selection pattern

For resource and bank targets:

1. exact route tiles
2. route area
3. constrained nearby fallback

## 17. Banking Pattern

This is the current preferred KSBot-native banking pattern for RSPS.

1. Move into reliable interaction range using route entrance/inside/stand tiles when needed.
2. Register the route’s bank object or NPC with:
   - `addCustomBankObject(...)`
   - or `addCustomBankNPC(...)`
3. Prefer only real bank-open actions:
   - `Bank`
   - `Use`
   - `Open`
4. Call `openBank()`.
5. Verify with `waitForOpen(...)`.
6. If open, deposit with:
   - `depositAll(...)`
   - or `depositInventory()` if appropriate
7. Verify there are no remaining route logs.
8. Close and verify with `waitForClose(...)`.

Why this matters:

- movement and stand tiles are there to improve interaction reliability
- they are not a replacement for KSBot’s bank hook

## 18. Pathing Pattern

Use pathing in this order:

1. exact close-range positioning:
   - `walkToTile(...)`
2. loaded-route stepping:
   - `shortestPath(...)`
   - choose visible minimap steps
   - `walkMiniMap(...)`
3. longer prebuilt path fallback:
   - `navThroughWorldPath(...)`

Additional pathing rules:

- use `worldToMinimap(...)` to confirm visibility before minimap click logic
- use `distanceTo(...)` and observed tile changes to detect stalls
- use route helper waypoints for awkward interiors or custom RSPS geometry

## 19. Object and NPC Interaction Pattern

For objects:

1. query exact ID/name/action
2. filter by exact route tile if available
3. sort by route preference
4. interact by real action text

For NPCs:

1. query by ID/name/action
2. constrain by route area or tile if possible
3. interact by true menu action

Do not rely on “closest thing that looks about right” when route data exists.

## 20. Woodcutting Route Pattern

A good woodcutting route contains:

- tree object IDs
- exact resource tiles
- preferred return tree anchor
- bank target IDs
- exact bank stand tile
- inside-bank tiles if building entry is tricky
- entrance tiles if doors or narrow bank entry matter
- optional path helper waypoints for long or awkward routes

Execution pattern:

1. walk toward tree area
2. if a valid tree is already interactable, chop immediately
3. when full:
   - bank mode: route to bank and use bank flow
   - powercut mode: use inventory drop flow
4. return using the route’s return anchor

## 21. Example-Script Lessons

The official example pages are useful for:

- simple lifecycle structure
- basic query usage
- GUI and paint examples
- reference patterns for banking or altar interactions

Takeaways:

- examples are good for idioms
- examples should not override local route-specific realities
- production scripts should be stricter than example scripts

## 22. Website API Notes

The website API docs are not the same thing as the in-client script API.

They appear to cover:

- git source
- scripts
- script users
- bug reports

Use them for publishing, remote management, or ecosystem integration, not as a replacement for the runtime scripting API.

## 23. Rules For Future Agents

These are the non-negotiable KSBot framework rules from this session.

1. Prefer KSBot hooks over reimplementing core systems manually.
2. Treat `ctx` as the primary integration surface.
3. Use route data to constrain behavior before using broad proximity logic.
4. Use `ctx.bank.openBank()` with custom bank registration as the primary RSPS bank-opening flow.
5. Always verify bank open/close state with `waitForOpen(...)` and `waitForClose(...)` when banking reliability matters.
6. Use queries for selection, not raw guessing.
7. Use object and player state as truth, not long fixed delays.
8. Keep `onProcess()` loop-driven and cheap.
9. Use debug exports whenever route behavior is unclear.
10. If a method name matters, verify against the installed jar before assuming.

## 24. Known Gaps and Verification Notes

These are the honest limitations of this booklet.

1. The official docs site is heavily client-rendered.
   - I was able to extract page structure and substantial page content from the official site payloads.
   - I did not manually transcribe every visible sentence from every page.

2. For exact callable method names, local jar verification is the stronger source.

3. Some categories listed on the docs site were not all deeply expanded line-by-line in this booklet.
   - Instead, they are organized into a framework reference that is more useful for implementation work.

4. This document is not stored in a model memory system.
   - It is stored on disk for reuse by future agents.

## 25. Official Source Index

### Script API Overview and Setup

- `https://ksbot.org/docs/script-api/installation`
- `https://ksbot.org/docs/script-api/quick-start`
- `https://ksbot.org/docs/script-api/scripting/config`
- `https://ksbot.org/docs/script-api/scripting/config-gui`
- `https://ksbot.org/docs/script-api/scripting/ks-paint`
- `https://ksbot.org/docs/script-api/scripting/deployment`
- `https://ksbot.org/docs/script-api/scripting/events`
- `https://ksbot.org/docs/script-api/scripting/input-listeners`
- `https://ksbot.org/docs/script-api/scripting/lifecycle`
- `https://ksbot.org/docs/script-api/scripting/manifest`

### Core API

- `https://ksbot.org/docs/script-api/api/kscontext`

### Commons

- `https://ksbot.org/docs/script-api/api/commons/paint`
- `https://ksbot.org/docs/script-api/api/commons/random`
- `https://ksbot.org/docs/script-api/api/commons/timer`

### Game APIs

- `https://ksbot.org/docs/script-api/api/game/combat`
- `https://ksbot.org/docs/script-api/api/game/consumables`
- `https://ksbot.org/docs/script-api/api/game/discord`
- `https://ksbot.org/docs/script-api/api/game/game`
- `https://ksbot.org/docs/script-api/api/game/lootable`
- `https://ksbot.org/docs/script-api/api/game/pathing`
- `https://ksbot.org/docs/script-api/api/game/prayer`
- `https://ksbot.org/docs/script-api/api/game/presets`
- `https://ksbot.org/docs/script-api/api/game/statistics`
- `https://ksbot.org/docs/script-api/api/game/tiles`
- `https://ksbot.org/docs/script-api/api/game/vars`

### Hooks

- `https://ksbot.org/docs/script-api/api/hooks`
- `https://ksbot.org/docs/script-api/api/hooks/bank`
- `https://ksbot.org/docs/script-api/api/hooks/chat`
- `https://ksbot.org/docs/script-api/api/hooks/dialog`
- `https://ksbot.org/docs/script-api/api/hooks/equipment`
- `https://ksbot.org/docs/script-api/api/hooks/friends`
- `https://ksbot.org/docs/script-api/api/hooks/game-objects`
- `https://ksbot.org/docs/script-api/api/hooks/graphics-objects`
- `https://ksbot.org/docs/script-api/api/hooks/inventory`
- `https://ksbot.org/docs/script-api/api/hooks/keyboard`
- `https://ksbot.org/docs/script-api/api/hooks/magic`
- `https://ksbot.org/docs/script-api/api/hooks/npcs`
- `https://ksbot.org/docs/script-api/api/hooks/players`
- `https://ksbot.org/docs/script-api/api/hooks/production`
- `https://ksbot.org/docs/script-api/api/hooks/shop`
- `https://ksbot.org/docs/script-api/api/hooks/skills`
- `https://ksbot.org/docs/script-api/api/hooks/teleporter`
- `https://ksbot.org/docs/script-api/api/hooks/trade`
- `https://ksbot.org/docs/script-api/api/hooks/widgets`

### Queries

- `https://ksbot.org/docs/script-api/api/queries`
- `https://ksbot.org/docs/script-api/api/queries/chat`
- `https://ksbot.org/docs/script-api/api/queries/equipment`
- `https://ksbot.org/docs/script-api/api/queries/friend`
- `https://ksbot.org/docs/script-api/api/queries/graphics`
- `https://ksbot.org/docs/script-api/api/queries/item`
- `https://ksbot.org/docs/script-api/api/queries/npc`
- `https://ksbot.org/docs/script-api/api/queries/player`
- `https://ksbot.org/docs/script-api/api/queries/ground-item`
- `https://ksbot.org/docs/script-api/api/queries/object`
- `https://ksbot.org/docs/script-api/api/queries/widget`

### Wrappers

- `https://ksbot.org/docs/script-api/api/wrappers/ksgrounditem`
- `https://ksbot.org/docs/script-api/api/wrappers/ksitem`
- `https://ksbot.org/docs/script-api/api/wrappers/ksnpc`
- `https://ksbot.org/docs/script-api/api/wrappers/ksobject`
- `https://ksbot.org/docs/script-api/api/wrappers/ksplayer`
- `https://ksbot.org/docs/script-api/api/wrappers/kswidget`

### Examples

- `https://ksbot.org/docs/script-api/examples/argentavis`
- `https://ksbot.org/docs/script-api/examples/balance-elemental`
- `https://ksbot.org/docs/script-api/examples/dz-prayer-altar`
- `https://ksbot.org/docs/script-api/examples/simple-cooking`
- `https://ksbot.org/docs/script-api/examples/skotizo`

### Website API

- `https://ksbot.org/docs/website-api/git-source`
- `https://ksbot.org/docs/website-api`
- `https://ksbot.org/docs/website-api/scripts`
- `https://ksbot.org/docs/website-api/script-users`
- `https://ksbot.org/docs/website-api/bug-reports`
