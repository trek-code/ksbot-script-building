# Release Note: Cutter of wood 0.9.2

## Release Metadata

- Script name: Cutter of wood
- Script ID: `woodcutter-bot`
- Version: `0.9.2`
- Date: 2026-04-12
- Status: In Progress
- GitHub: Not linked yet

## Summary

Switched the active starter routes to live-coordinate Varrock West trees and oaks and created a reusable coordinate registry for future bot work.

## Included In This Version

- Removed `Lumbridge Trees` from the active route list.
- Added `Varrock West Trees` as the active starter normal-tree route.
- Updated `Varrock West Oaks` to use the live Reason oak cluster coordinates and the live oak object id `10820`.
- Kept both Varrock West routes linked to the nearby bank at `3186, 3436`.
- Added a dedicated coordinate registry under `docs/coordinates/` for Reason route data.

## Risks Or Follow-Ups

- More oak coordinates from the same cluster will make the route area even tighter.
- The banker coordinates still need to be captured fully if we want route-specific banker interactions later.
