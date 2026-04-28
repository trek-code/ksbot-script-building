# Reason Server Notes

## Server Identity

- Server name: Reason
- Client/build version: local KSBot Reason client
- Account type used for testing: user-provided live test account
- Relevant launcher or setup notes: local development runs through `ksbot.Loader` with `-Dserver.name=Reason`

## Bot Scope

- First bot category: woodcutting
- First skill/activity: route-based woodcutting with optional banking
- Allowed locations:
  - Varrock West Trees
  - Varrock West Oaks
  - Draynor Willows
  - Edgeville custom woodcutting area documented for later routes
  - Reason Woodcutting Guild documented for later high-level routes
- Banking/restock route:
  - booth-first banking for Varrock West and Draynor
  - no banker dependency required for the current Reason routes
  - Woodcutting Guild uses a bank chest instead of a normal bank booth
- Stop conditions:
  - no axe
  - staff nearby
  - runtime limit reached
  - stop-at-level reached
  - recovery timeout

## Server-Specific Data

- Coordinates / regions:
  - stored in `docs/coordinates/reason-coordinate-registry.md`
- NPC IDs:
  - Varrock West banker `2897`
- Object IDs:
  - regular tree `1276`
  - oak tree `10820`
  - willow trees `10838`, `10829`, `10831`, `10819`, `10817`
  - yew tree `10822`
  - magic tree `10834`
  - Varrock West bank booth `10583`
  - Draynor bank booths `10355`, `10527`
  - closed Draynor booth `10528`
  - Edgeville bank booth `1096`
  - Woodcutting Guild bank chest `1592, 3475`
- Item IDs:
  - not captured yet
- Widget/dialog IDs:
  - not captured yet
- World or channel assumptions:
  - Reason has custom area layouts, especially around Edgeville, so coordinates must stay server-specific
  - Reason also has server-specific teleport commands that should be tracked separately from portable route logic

## Behavioral Notes

- Login flow: standard KSBot local run
- Anti-idle expectations: still tuning from live behavior
- Custom mechanics:
  - Edgeville woodcutting area is custom to Reason
  - Edgeville banking is booth-only in the captured setup
  - `::home` leads to a custom Reason hub area, so visual landmarks and travel assumptions should be documented server-by-server
  - `::wcguild` teleports the player into the Woodcutting Guild when the `60` woodcutting requirement is met
- Random events or interruptions:
  - not fully profiled yet
- Dangerous areas or states:
  - Edgeville route is near the wilderness ditch, so route safety should be reviewed before enabling unattended use
  - the open-field layout near the ditch means pathing should avoid drifting too far north or east without explicit route bounds

## Walker Notes

- KSBot already exposes strong pathing primitives through `ctx.pathing`, including:
  - `walkTo(...)`
  - `walkPath(...)`
  - `shortestPath(...)`
  - `shortestSafePath(...)`
  - `navThroughWorldPath(...)`
  - `reachable`
- This looks like a solid low-level pathing toolkit, but not a fully modeled RSPS-aware world walker with custom city knowledge, doors, bank interiors, teleports, and server-specific shortcuts already solved for us.
- For Reason, the best long-term approach is to build a layered walker:
  - common city and skilling-area support first
  - route and waypoint data per city/area
  - bank-entry and interior stand tiles for awkward buildings
  - optional server-specific teleports integrated later
- Because Reason is a private server, we do not need to model the entire world immediately.
- Best expansion path:
  - start with common cities and skilling hubs
  - expand one city or area at a time
  - keep custom Reason areas explicitly server-specific
- add teleport command support later when larger bossing or restock bots need it
- the KSBot backend is a better source of route data than the on-screen debug overlay for repeatable tooling:
  - `ctx.groundObjects` for object ids, names, actions, and world tiles
  - `ctx.npcs` for NPC ids, names, actions, and world tiles
  - `ctx.groundItems` for item ids, names, actions, quantities, and world tiles
  - `ctx.players.getLocal()` for current player world tile and motion state
  - `ctx.pathing` for walkable-vs-blocked tile checks and tile flags

## Validation Notes

- What must always be true before a run starts:
  - axe equipped or in inventory
  - selected route matches the intended tree type
- What causes a safe shutdown:
  - staff detection
  - explicit stop
  - level goal hit
  - runtime goal hit
  - recovery timeout
- What must be logged during test runs:
  - route chosen
  - tree action text
  - whether tree group marking worked
  - whether booth banking worked
  - any route drift or stuck points
