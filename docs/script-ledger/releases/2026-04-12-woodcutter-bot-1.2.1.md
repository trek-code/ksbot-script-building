# Cutter of wood `1.2.1`

## Date

- 2026-04-12

## Status

- In Progress

## Summary

- tightened bank-entry and inside-bank standing tiles for Varrock West, Draynor, and the Woodcutting Guild
- removed the farther backup booth from Varrock West so booth interaction prefers the closest door-side target
- made `Fast` dropping much more aggressive and smoothed `Human-like` dropping so it no longer drops in a clunky 4-and-4 pattern
- shortened the return-to-cutting handoff after powercut dropping by clearing stale tree engagement and favoring bank-side tree anchors

## Notes

- Varrock West now uses the entrance tile and closest booth only
- Woodcutting Guild chest interaction now approaches from an explicit inside-building tile before attempting the chest
- return-to-tree pathing is still route-based, but target selection now favors the nearest valid tree instead of drifting deeper into the patch first
