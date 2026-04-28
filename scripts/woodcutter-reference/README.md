# Woodcutter Reference

Separate KSBot-first woodcutter scaffold.

This script is intentionally separate from the live `woodcutter-bot` so we can keep the working bot intact while rebuilding from a cleaner reference baseline.

## Goals

- use KSBot hooks and queries directly
- keep the state machine small
- keep route data explicit
- prefer KSBot banking and pathing helpers over custom behavior

## Current Scope

- one validated route scaffold: `Varrock West Oaks`
- bank mode baseline
- exact route tiles for trees and bank
- custom bank registration plus `openBank()` and `waitForOpen(...)`
- short, loop-driven `onProcess()` flow

## Files

- `ReasonWoodcutterReferenceBot.java`: KSBot entrypoint
- `ReferenceWoodcutterBot.java`: shared script logic
- `ReferenceWoodcuttingProfile.java`: route and tile data
