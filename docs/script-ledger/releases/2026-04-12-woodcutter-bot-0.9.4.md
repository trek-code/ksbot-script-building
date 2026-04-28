# Release Note: Cutter of wood 0.9.4

## Release Metadata

- Script name: Cutter of wood
- Script ID: `woodcutter-bot`
- Version: `0.9.4`
- Date: 2026-04-12
- Status: In Progress
- GitHub: Not linked yet

## Summary

Completed the Varrock West banker pair in the coordinate registry and documented the preferred two-oak starter cluster for the active route.

## Included In This Version

- Added banker tile `3187, 3436`.
- Added banker tile `3187, 3438`.
- Documented the booth row at `x=3186` and banker row at `x=3187`.
- Marked the two-oak starter focus as `3167, 3420` and `3165, 3411`.

## Risks Or Follow-Ups

- The code still banks booth-first; explicit banker targeting is now possible later if needed.
- If the two-oak route feels too sparse in live tests, we can widen back to the third nearby oak without needing new discovery work.
