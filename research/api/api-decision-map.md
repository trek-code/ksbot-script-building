# KSBot API Decision Map

Use this document to decide what belongs in reusable helpers and what must remain tied to the target RSPS.

## Safe To Abstract

- Script state machine structure
- Timing and randomization helper interfaces
- Logging format
- Retry and stuck-detection policies
- Test-note and handoff contracts

## Likely Server-Specific

- Area coordinates and routing assumptions
- Object, NPC, and item identifiers
- Banking and shop interaction patterns
- Skill-loop stop conditions
- Any UI flow that depends on custom RSPS widgets or dialogs

## Unknown Until Docs Arrive

- Required script registration or manifest format
- Jar packaging expectations
- Which APIs expose polling versus event-driven control
- Whether the platform supports overlays, custom config UIs, or runtime settings screens

## Runtime Constraints To Track

- Main loop cadence and sleep model
- Allowed blocking operations
- Threading expectations
- Script reload behavior
- Logging visibility in KSBot
- Failure handling when game state changes unexpectedly

## Decision Rule

Do not move behavior into shared helpers until it has been proven stable across at least one working RSPS bot and does not depend on custom widget IDs, area data, or server-specific interaction order.
