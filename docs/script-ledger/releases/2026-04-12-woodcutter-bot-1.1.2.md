# Cutter of wood `1.1.2`

## Date

- 2026-04-12

## Status

- In Progress

## Summary

- switched route banking toward explicit bank-booth targets where live booth coordinates are known
- corrected the Seers Village banking stand point and locked the route to the two confirmed Seers booth tiles
- changed log banking to deposit all known woodcutting logs so mid-run route or tree-type changes do not leave old logs behind

## Notes

- Seers Village now uses the two confirmed booth tiles at `2727, 3494` and `2728, 3494`
- explicit booth targeting is now available for the Varrock West, Draynor, and Seers booth routes
- Woodcutting Guild routes still fall back to the generic bank helper because the live chest id has not been captured yet
