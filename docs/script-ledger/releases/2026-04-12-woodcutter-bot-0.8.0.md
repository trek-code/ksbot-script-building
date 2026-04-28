# Release Note: Woodcutter Bot 0.8.0

## Release Metadata

- Script name: Woodcutter Bot
- Script ID: `woodcutter-bot`
- Version: `0.8.0`
- Date: 2026-04-12
- Status: In Progress
- GitHub: Not linked yet

## Summary

Reorganized the woodcutter for multi-server growth by separating shared woodcutting logic from the active server-specific entry script and moving the current target to `Reason`.

## Included In This Version

- Removed the KSBot manifest from the shared `WoodcutterBot` logic class.
- Added `ReasonWoodcutterBot` as the active KSBot entry point for the `Reason` server.
- Updated the build script to target supported servers explicitly and default to `Reason`.
- Updated handoff and build docs to point to the new `Reason` artifact path.

## Risks Or Follow-Ups

- We still need the first live `Reason` test to confirm tree names, actions, and object IDs.
- Near Reality is no longer the active target, but the shared structure now makes future server-specific entry classes straightforward.
