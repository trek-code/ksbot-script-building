# Reason World Mapper

Manual-guided local KSBot script for capturing structured area data while the player walks around.

## Current Version

- `0.4.3`

## Scope

- one-window manual mapping UI
- one-click quick survey and export
- backend nearest-target capture for objects, NPCs, and ground items
- dedicated nearest bank-target capture
- dedicated nearest tree-target capture
- nearby object scans
- nearby NPC scans
- nearby ground-item scans
- current-tile and named-anchor capture
- ordered waypoint-chain capture
- automatic route recorder that adds waypoints while the player walks
- custom route-recorder labels so exported waypoint chains can be named per path
- action/menu capture from object and NPC wrappers
- clipboard-friendly capture lines for quick route entry
- current-tile capture now copies a clipboard-ready capture line too
- blocked-tile scanning using KSBot pathing and reachability signals
- structured JSON session export for later walker work
- workspace-first export path with a script-storage fallback

## Notes

- the mapper does not walk by itself in v1
- the mapper still does not walk by itself; waypoint chains are captured while the user walks manually
- the user manually guides the character through the target area
- the debug overlay is optional; object ids, names, actions, and player tiles are captured from KSBot backend hooks directly
- the goal is to speed up route creation, bank mapping, walker planning, and custom RSPS area documentation
- exports prefer `research/mapping-sessions/reason/` inside this workspace
- if the workspace path is unavailable at runtime, exports fall back to the script storage directory
- object and NPC actions are exported so right-click-style interactions like `Bank`, `Collect`, `Chop down`, and `Talk-to` are preserved in the data
- exports now include a `summary` block so noisy scans can be judged quickly before being turned into walker data
