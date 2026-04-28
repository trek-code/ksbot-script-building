# Release Note: Cutter of wood 0.9.0

## Release Metadata

- Script name: Cutter of wood
- Script ID: `woodcutter-bot`
- Version: `0.9.0`
- Date: 2026-04-12
- Status: In Progress
- GitHub: Not linked yet

## Summary

Reworked the woodcutter from a start-anchor script into a route-based tree and banking bot for Reason.

## Included In This Version

- Added route profiles for `Lumbridge Trees`, `Varrock West Oaks`, and `Draynor Willows`.
- Replaced anchor-based startup flow with route-based navigation to tree areas.
- Added `Powercut` and `Bank` modes to the single-window settings UI.
- Linked each starter route to a bank area and return path anchor.
- Added the first banking state flow: navigate to bank, open bank, deposit matching logs, and return to trees.
- Kept `Stop At Level`, `Max Runtime`, and tree-group marking in the new route model.
- Kept Java 11 compatibility and verified the refactor compiles against the local Reason API and client jars.

## Risks Or Follow-Ups

- The starter route coordinates still need live walking validation on Reason.
- Bank-open behavior may need route-specific adjustments if a route wants a custom bank booth or banker target.
- Teleport banking is scaffolded in settings, but not enabled for the starter routes yet.
