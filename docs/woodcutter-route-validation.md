# Woodcutter Route Validation

## Validated

- `Varrock West Oaks`
  - powercutting: validated
  - banking: validated
  - minimap zoom startup: validated
  - bank opening: validated through KSBot custom bank object registration

## Ready For Validation

- `Draynor Willows`
  - validate bank approach path
  - validate booth open
  - validate return-to-willows path

- `Seers Village Maples`
  - validate booth open
  - validate tree return selection

- `Yews in Seers village`
  - validate booth open
  - validate nearest-yew return logic

- `Catherby Oak Trees`
  - validate booth open
  - validate route smoothness

- `Woodcutting Guild Yews`
  - validate inside-bank approach
  - validate chest open
  - validate return path

- `Woodcutting Guild Magics`
  - validate inside-bank approach
  - validate chest open
  - validate return path

## Shared Route Engine

All supported routes now use the same shared woodcutter movement and banking engine:

- minimap zoom forced at startup
- route approach tiles
- stand-tile banking
- KSBot custom bank registration plus `ctx.bank.openBank()`
- shared pathing stack:
  - `shortestPath(...)`
  - `walkMiniMap(...)`
  - `walkToTile(...)`
  - `navThroughWorldPath(...)`

## Next Test Order

1. `Draynor Willows`
2. `Seers Village Maples`
3. `Yews in Seers village`
4. `Catherby Oak Trees`
5. `Woodcutting Guild Yews`
6. `Woodcutting Guild Magics`
