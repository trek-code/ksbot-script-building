# Research System

This folder is the durable local reference system for KSBot and the target RSPS.

## Files

- `api/index.md`: master map of KSBot concepts and doc coverage
- `api/api-summary-template.md`: format for each KSBot API topic summary
- `api/api-decision-map.md`: what should be abstracted, what stays server-specific, and runtime constraints
- `servers/server-notes-template.md`: format for one target RSPS profile
- `servers/reason.md`: active Reason-specific notes and captured runtime assumptions

## Research Standard

Each research note should answer:

- What the KSBot API can do
- What assumptions it makes
- What data or game state it needs
- What can fail at runtime
- Whether the logic is generic or RSPS-specific
- What a bot template should expose because of it
