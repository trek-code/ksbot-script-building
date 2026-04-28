# Release Note: Woodcutter Bot 0.4.0

## Release Metadata

- Script name: Woodcutter Bot
- Script ID: `woodcutter-bot`
- Version: `0.4.0`
- Date: 2026-04-11
- Status: In Progress
- GitHub: Not linked yet

## Summary

Simplified the woodcutter to the cleanest v1 flow: select tree type, start near the trees with an axe, anchor there, and powercut only.

## Included In This Version

- Removed location and bank-location selection from the UI and settings model.
- Removed banking-mode logic and standardized the script around powerwoodcutting only.
- Simplified tree metadata to tree-type search names plus a fixed 10-tile start requirement.
- Updated the control window to show selected tree and anchor mode instead of route and banking details.
- Updated the script state flow to focus on anchor, chop, drop, recover, pause, and stop behavior.

## Risks Or Follow-Ups

- The start-near-tree validation still needs a real KSBot tree query and distance check.
- The drop logic and cut detection still need live RSPS object IDs and interaction wiring.
