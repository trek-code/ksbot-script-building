# KSBot Server Targets

This index tracks which KSBot-supported servers are installed locally and which one is currently active for development.

## Current Priority

- Active server: `Reason`
- Deferred server: `Near-Reality`
- Other installed server: `OldschoolRSPS`

## Installed Local Paths

- `Reason`
  - `C:\Users\jonez\.kreme\servers\Reason`
- `Near-Reality`
  - `C:\Users\jonez\.kreme\servers\Near-Reality`
- `OldschoolRSPS`
  - `C:\Users\jonez\.kreme\servers\OldschoolRSPS`

## Workflow Rule

- Keep shared bot logic server-agnostic when possible.
- Put KSBot manifest targeting into tiny server-specific entry classes.
- Build and test one active server first, then add a new entry class when porting to another supported server.
