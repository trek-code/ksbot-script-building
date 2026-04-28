# Release Note: Cutter of wood 0.9.8

## Release Metadata

- Script name: Cutter of wood
- Script ID: `woodcutter-bot`
- Version: `0.9.8`
- Date: 2026-04-12
- Status: In Progress
- GitHub: Not linked yet

## Summary

Reduced tree spam-clicking by trusting the local player's chopping state more heavily and improved Draynor banking by entering the bank interior before trying to open the booth.

## Included In This Version

- Chopping now waits on local-player activity signals before retrying tree clicks.
- Added a recent-progress grace window to avoid reclicking oaks too aggressively.
- Added a Draynor inside-bank entry tile at `3091, 3247` before booth interaction.
- Banking now occasionally uses `depositInventory()` for variety, but only when the axe is equipped.

## Risks Or Follow-Ups

- The local-player activity check may still need tuning if Reason reports idle/animation changes differently on other tree types.
- If Draynor banking still snags, the next step is route-specific booth clicking rather than generic `openBank()`.
