# Release Note: Cutter of wood 0.9.3

## Release Metadata

- Script name: Cutter of wood
- Script ID: `woodcutter-bot`
- Version: `0.9.3`
- Date: 2026-04-12
- Status: In Progress
- GitHub: Not linked yet

## Summary

Expanded the Varrock West banking reference with a second confirmed booth tile and locked the current banking assumption to bank booths instead of banker targeting.

## Included In This Version

- Added the second confirmed Varrock West bank booth coordinate `3186, 3438`.
- Documented booth-first banking as the active assumption for the woodcutter route.
- Kept banker targeting deferred until live banker coordinates are available.

## Risks Or Follow-Ups

- If generic `openBank()` proves inconsistent, the next step is route-specific booth interaction using the confirmed booth coordinates.
