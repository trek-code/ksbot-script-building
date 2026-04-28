# Release Note: Woodcutter Bot 0.6.0

## Release Metadata

- Script name: Woodcutter Bot
- Script ID: `woodcutter-bot`
- Version: `0.6.0`
- Date: 2026-04-11
- Status: In Progress
- GitHub: Not linked yet

## Summary

Moved the woodcutter from placeholder tree logic to real KSBot object-query scaffolding using the local API jar.

## Included In This Version

- Confirmed and used the real local KSBot API jar field names and query classes.
- Added object-query-based start-near-tree validation with the selected tree names.
- Added real nearby-tree capture for `Mark Tree Group` using world locations.
- Added target-tree selection that prefers either the closest tree or a random tree from the valid set.
- Added first-pass tree interaction calls using `Chop down` and `Chop` options.
- Added first-pass powercut drop logic that drops logs matching the selected tree type.

## Risks Or Follow-Ups

- The current `noProgressObserved()` logic is still simplistic and should be refined against live animation/inventory behavior.
- We still need live Near Reality testing to verify exact object names, action strings, and whether all tree types use the same chop option text.
