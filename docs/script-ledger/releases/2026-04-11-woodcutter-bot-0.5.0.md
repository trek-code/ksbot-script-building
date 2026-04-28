# Release Note: Woodcutter Bot 0.5.0

## Release Metadata

- Script name: Woodcutter Bot
- Script ID: `woodcutter-bot`
- Version: `0.5.0`
- Date: 2026-04-11
- Status: In Progress
- GitHub: Not linked yet

## Summary

Added tree-group marking so the bot can be constrained to a chosen local cluster of trees.

## Included In This Version

- Added tree-group state storage in the woodcutter bot runtime.
- Added `Mark Tree Group` and `Clear Tree Group` controls to the single-window UI.
- Added tree-group status to the session snapshot and live UI display.
- Updated navigation status so the bot can distinguish between general anchoring and focused tree-group mode.
- Updated documentation and ledger entries to track the new focus-group behavior.

## Risks Or Follow-Ups

- Tree-group marking is currently a scaffold and still needs a real KSBot query that captures the nearby selected trees.
- The chop loop still needs live filtering logic so it only targets trees inside the marked group.
