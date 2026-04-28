# Release Note: Cutter of wood 1.1.0

## Release Metadata

- Script name: Cutter of wood
- Script ID: `woodcutter-bot`
- Version: `1.1.0`
- Date: 2026-04-12
- Status: In Progress
- GitHub: Not linked yet

## Summary

Finished the next route expansion pass by bringing Woodcutting Guild Yews and Magics into the live bot and tightening the same-tree click behavior.

## Included In This Version

- Added `Woodcutting Guild Yews` to the live route list.
- Added `Woodcutting Guild Magics` to the live route list.
- Added route-level woodcutting minimums so guild routes refuse to start below their required level.
- Added a short same-tree re-engage grace so the bot does not hammer the same tree tile immediately after clicking it.

## Risks Or Follow-Ups

- Guild banking still uses generic bank opening against the bank chest, so if that route snags the next step is explicit chest interaction.
- The tree-click grace may still need one more tune if Reason has longer idle gaps on some trees.
