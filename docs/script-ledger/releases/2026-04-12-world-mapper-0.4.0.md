# Reason World Mapper `0.4.0`

## Date

- 2026-04-12

## Summary

Added practical backend capture helpers so route building no longer depends on reading the KSBot debug overlay manually.

## Changes

- added nearest object capture
- added nearest NPC capture
- added nearest ground-item capture
- added nearby ground-item scans
- added automatic route recording while the player walks
- added clipboard-friendly capture lines for fast route intake
- kept quick survey, blocked-tile scanning, and JSON export flow

## Notes

- the mapper still does not walk on its own
- exact hovered/selected object capture is not implemented yet because the confirmed backend hooks we checked are strongest for nearby-query capture
- exports remain workspace-first and are designed to help the route workbench and future walker work
