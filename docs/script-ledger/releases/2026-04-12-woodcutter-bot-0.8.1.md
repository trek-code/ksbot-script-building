# Release Note: Woodcutter Bot 0.8.1

## Release Metadata

- Script name: Woodcutter Bot
- Script ID: `woodcutter-bot`
- Version: `0.8.1`
- Date: 2026-04-12
- Status: In Progress
- GitHub: Not linked yet

## Summary

Refined live `Reason` behavior by reducing repeat clicks on trees and slowing powercut dropping just enough to feel less robotic.

## Included In This Version

- Added a short interaction cooldown after successful tree clicks.
- Added a short post-click settle pause before the bot reevaluates the same tree.
- Replaced instant drop-all behavior with paced per-log dropping and small natural micro-pauses.

## Risks Or Follow-Ups

- The new drop pacing is a first pass and may still need tuning after another live run.
- If the bot still double-clicks occasionally, we may need to increase the settle window slightly or key it to animation/start movement.
