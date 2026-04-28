# Active Scripts

Use this folder for bot implementations that move beyond templates.

## Suggested Layout

- One folder per bot when the first real implementation begins
- A short bot-specific README with target server assumptions
- Build notes copied from `templates/java/BotBuildNotesTemplate.md`
- A matching script ledger file in `docs/script-ledger/scripts/`

## Current Status

`woodcutter-bot` is now the active first live bot implementation. It has a tracked ledger entry, a single-window control UI, a route-based tree and bank model for `Reason`, and a local compile path against the `Reason` KSBot API jar plus the `Reason` client jar.

`world-mapper` is the second active script. It is a manual-guided area scanner for `Reason` that captures anchors, waypoint chains, nearby objects, nearby NPCs, blocked tiles, and action data for future walker work.

## Version Tracking Rule

Whenever a new script is created:

- add an entry to `docs/script-ledger/index.md`
- create a per-script history file in `docs/script-ledger/scripts/`
- add a release note entry in `docs/script-ledger/releases/` for each meaningful version
