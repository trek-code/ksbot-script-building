# Release Note: Cutter of wood 0.9.1

## Release Metadata

- Script name: Cutter of wood
- Script ID: `woodcutter-bot`
- Version: `0.9.1`
- Date: 2026-04-12
- Status: In Progress
- GitHub: Not linked yet

## Summary

Tuned the first bankable oak route so the bot can actually see the Varrock West oak cluster and recover if a route box is slightly off.

## Included In This Version

- Shifted and widened the `Varrock West Oaks` route area and tree anchor.
- Added a nearby-tree fallback query when the route-local tree query returns nothing.
- Added the same nearby fallback to `Mark Tree Group` so marking still works when a route boundary is slightly imperfect.
- Reverified Java 11 compilation against the local Reason API and client jars.

## Risks Or Follow-Ups

- The exact oak route still needs one more live pass on Reason to confirm it lands on the cleanest cluster.
- If the bank path is still awkward, the next step is route-specific walking points instead of one anchor per route.
