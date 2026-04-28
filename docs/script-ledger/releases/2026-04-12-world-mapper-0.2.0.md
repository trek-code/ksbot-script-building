# Reason World Mapper `0.2.0`

## Date

- 2026-04-12

## Status

- In Progress

## Summary

- added waypoint-chain capture for manual route building
- added blocked-tile scanning using KSBot pathing and reachability checks
- improved exports so route, obstacle, and action data are easier to consume when building a walker

## Notes

- the mapper still does not walk by itself in v1; waypoint chains are captured while the user walks manually
- object and NPC actions are exported, so right-click-style interactions stay visible in the session output
- blocked tiles are exported with tile flags for later obstacle and route debugging
