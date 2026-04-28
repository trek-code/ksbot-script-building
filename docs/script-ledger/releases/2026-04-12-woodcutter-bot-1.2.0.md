# Cutter of wood `1.2.0`

## Date

- 2026-04-12

## Status

- In Progress

## Summary

- improved bank routing by preferring ordered live booth targets instead of whichever nearby booth the helper opens first
- adjusted Woodcutting Guild banking to approach from a safer inside-bank stand tile before chest interaction
- added configurable powercut drop behavior with `drop all` and `drop some random` modes plus `fast` and `human-like` drop speed options
- added a reusable looting tab framework with enabled toggle, looting style selection, and available-vs-active loot lists

## Notes

- Seers Village and Varrock West now respect the preferred booth order captured in live route data
- Draynor keeps its existing inside-bank stand point and also benefits from the explicit booth-target ordering
- powercut no longer exits the dropping state just because inventory stopped being full after the first couple of drops
- looting UI and settings are now ready for reuse across future bots, even though this pass focused on the framework rather than a full loot-pickup runtime
