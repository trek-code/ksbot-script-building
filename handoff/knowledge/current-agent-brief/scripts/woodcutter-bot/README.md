# Cutter of wood

This is the first live KSBot script scaffold in the workspace.

## Current Version

- `1.3.0`

## Scope

- single-window control UI
- dedicated `Main`, `Stats`, and `Looting` tabs
- tree-type dropdown plus route-based `Tree Location`
- `Powercut` and `Bank` modes
- linked bank location per supported route
- optional teleport-banking toggle scaffolded per route
- startup axe validation
- optional anti-ban next-action delay range
- top-right debug tool for live bot diagnostics and failsafe reporting
- real KSBot object queries for route-local trees
- object ID filtering layered on top of tree-name checks
- log progress tracking from inventory changes to support lighter watchdog checks instead of heavy recovery loops
- runtime session feedback for status, runtime, selected tree, route, bank location, last action, and woodcutting XP stats
- route-based state machine for navigate, chop, drop, bank, return, pause, and stop
- shared core logic plus server-specific entry scripts
- reusable looting configuration framework for future bot families

## Notes

- The UI is intentionally one window with focused tabs instead of a crowded single panel.
- The user should start with an axe in inventory or equipped.
- The bot now uses named route profiles instead of start-near-tree anchoring.
- The current starter routes are `Varrock West Trees`, `Varrock West Oaks`, `Draynor Willows`, `Seers Village Maples`, `Woodcutting Guild Yews`, and `Woodcutting Guild Magics`.
- These Varrock West routes are now driven by live Reason coordinates captured in testing instead of guessed OSRS-only areas.
- `Bank` mode walks to the linked route bank, opens the configured booth or chest when route data exists, deposits all known woodcutting logs, and returns to the tree area.
- Teleport banking is scaffolded in the settings model, but the starter routes currently use walking only.
- Tree detection uses explicit tree object IDs plus tree names and live action filtering.
- The timing loop now favors lighter action-ready checks and watchdog reroutes instead of frequent recovery stalls.
- Chopping now waits on player activity and recent log progress rather than long fixed recover windows.
- Draynor banking now uses an inside-bank entry tile before trying to interact with the booth.
- Seers banking now uses an inside-bank entry tile plus the two confirmed Seers booth tiles instead of relying on a loose bank-area guess.
- booth routes now prefer ordered explicit booth targets instead of whichever nearby booth the generic helper picks first.
- Woodcutting Guild banking now approaches from an inside-bank standing tile instead of trying to click the chest from a risky exterior angle.
- Varrock West banking now prefers the closest booth by the entrance and no longer targets the farther backup booth.
- Draynor banking now uses a stronger inside-bank standing tile before booth interaction.
- Woodcutting Guild banking now uses an explicit inside-building stand tile before chest interaction.
- Varrock West, Draynor, Seers, and Guild banking tiles are now aligned to the latest live coordinates captured during testing.
- Guild routes now enforce minimum woodcutting requirements before the run starts.
- `WoodcutterBot.java` now holds the shared woodcutter logic.
- `ReasonWoodcutterBot.java` is the active KSBot entry script for `Reason`.
- Future supported KSBot servers should get their own tiny entry class with a server-specific manifest while reusing the shared bot logic.
- Tree clicks respect a short settle window to reduce double-click behavior.
- Log dropping is paced to look more human while staying efficient.
- Powercut now supports configurable drop type and drop speed, and partial dropping stays in the drop cycle instead of immediately running back after only a couple of logs.
- `Fast` drop speed now behaves much more aggressively for RSPS-style speed runs, while `Human-like` still drops smoothly without the old 4-and-4 rhythm.
- Powercut return-to-tree timing is shorter now because drop completion clears stale chopping engagement and routes back toward the bank-side tree anchor.
- Looting now has its own UI tab with enabled/disabled mode, looting style, and reusable available-vs-active loot lists for future bot reuse.
- The settings panel supports `Stop At Level` while still keeping `Max Runtime` as an optional fallback.
- A lightweight debug manager now records failsafes, bank retries, and loop events for live troubleshooting.
- The active public bot name is `Cutter of wood` by `MindMyLogic`.
