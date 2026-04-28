# Cutter of wood

This is the first live KSBot script scaffold in the workspace.

## Current Version

- `2.0.0`

## Scope

- single-window control UI
- dedicated `Main` and `Stats` tabs
- planner-backed route selection by tree type
- `Powercut` and `Bank` modes
- startup axe validation
- optional anti-ban next-action delay range
- top-right debug tool for live bot diagnostics and failsafe reporting
- exact route-tile targeting for trees and bank interaction points
- direct booth/chest banking with route-specific inside-bank handling only where needed
- short blocking waits after real interactions instead of long recovery-state timing
- runtime session feedback for status, runtime, selected tree, route, bank location, last action, and woodcutting XP stats
- shared core logic plus server-specific entry scripts

## Notes

- The UI is intentionally one window with focused tabs instead of a crowded single panel.
- The user should start with an axe in inventory or equipped.
- The bot now runs from planner-backed route data instead of start-near-tree anchoring.
- Current live routes: `Varrock West Trees`, `Varrock West Oaks`, `Draynor Willows`, `Seers Village Maples`, `Yews in Seers village`, `Catherby Oak Trees`, `Woodcutting Guild Yews`, and `Woodcutting Guild Magics`.
- `Bank` mode walks directly to the linked route bank unless the route explicitly requires an inside-bank step.
- Draynor and Woodcutting Guild are the current inside-bank routes.
- Tree detection uses explicit route tiles, tree object IDs, tree names, and live action filtering.
- Chopping now waits on player movement, animation, and recent log gain instead of a separate recovery state.
- Powercut supports `Drop all logs` and `Drop some logs (8-24)` with `Fast` or `Human-like` speeds.
- When the axe is equipped, banking can still vary with `depositInventory()` for occasional deposit-all behavior.
- The settings panel supports `Stop At Level` while still keeping `Max Runtime` as an optional fallback.
- A lightweight debug manager records failsafes, bank retries, and loop events for live troubleshooting.
- `WoodcutterBot.java` holds the shared bot logic, and `ReasonWoodcutterBot.java` is the active KSBot entry script for `Reason`.
