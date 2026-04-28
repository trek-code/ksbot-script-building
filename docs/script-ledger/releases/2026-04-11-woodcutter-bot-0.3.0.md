# Release Note: Woodcutter Bot 0.3.0

## Release Metadata

- Script name: Woodcutter Bot
- Script ID: `woodcutter-bot`
- Version: `0.3.0`
- Date: 2026-04-11
- Status: In Progress
- GitHub: Not linked yet

## Summary

Strengthened the first live woodcutter scaffold with better validation and runtime feedback.

## Included In This Version

- Added richer route metadata, including expected tree names and bank support.
- Added a session snapshot model for pushing runtime status into the control window.
- Added UI fields for current profile, banking mode, runtime, logs cut, and last action.
- Added validation for missing axe, missing route, missing bank selection, and bank-route mismatch.
- Improved runtime status publishing so the control window reflects the bot's current state more clearly.

## Risks Or Follow-Ups

- The bot still needs target-RSPS tree IDs, coordinates, and interaction logic before it can woodcut live.
- `logsCut` is wired into the session model but still needs real increment logic from successful tree-cut detection.
