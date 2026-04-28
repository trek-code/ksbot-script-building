# Release Note: Woodcutter Bot 0.2.0

## Release Metadata

- Script name: Woodcutter Bot
- Script ID: `woodcutter-bot`
- Version: `0.2.0`
- Date: 2026-04-11
- Status: In Progress
- GitHub: Not linked yet

## Summary

Created the first live implementation scaffold for the woodcutting bot.

## Included In This Version

- Added a dedicated `scripts/woodcutter-bot/` workspace.
- Added a single-window control UI with Start, Pause, Stop, and Update Settings buttons.
- Added a typed settings model for tree selection, location, banking mode, bank location, safety options, and runtime limit.
- Added predefined location profiles for the first supported tree spots.
- Added a KSBot `Script` subclass scaffold with runtime state handling, startup axe validation, and placeholder behavior for navigation, chopping, banking, dropping, and recovery.

## Risks Or Follow-Ups

- Real tree IDs, area anchors, bank routes, and object interaction logic still need target-RSPS mapping.
- The Swing-based control window is a practical first scaffold, but we still need to validate how it behaves inside the live KSBot runtime.
