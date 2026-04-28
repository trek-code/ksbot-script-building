# Release Note: Woodcutter Bot 0.7.1

## Release Metadata

- Script name: Woodcutter Bot
- Script ID: `woodcutter-bot`
- Version: `0.7.1`
- Date: 2026-04-11
- Status: In Progress
- GitHub: Not linked yet

## Summary

Adjusted the script manifest metadata to match the live Near Reality KSBot environment so the local woodcutter can be discovered and loaded correctly.

## Included In This Version

- Changed the manifest server target from the placeholder value to `Near-Reality`.
- Added a stable local script UID: `local-woodcutter-bot`.
- Rebuilt the local handoff jar target for the corrected manifest version.

## Risks Or Follow-Ups

- If KSBot still does not show the script after this manifest fix, the next check is the exact folder placement or whether KSBot expects a different jar layout.
- Once the script loads, we still need the first in-game behavior test to confirm tree actions and tree-group behavior.
