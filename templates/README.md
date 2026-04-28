# Template Library

This folder contains reusable starting points for KSBot bot development.

## Current Templates

- `java/SkillingBotTemplate.java`: starter bot structure with states, recovery hooks, and timing helpers
- `java/BotBuildNotesTemplate.md`: local build and packaging template
- `checklists/TransferChecklist.md`: pre-transfer handoff checklist
- `checklists/TestRunTemplate.md`: structured KSBot physical test note

## Template Standard

Every bot template should provide:

- clear state/task ownership
- configuration inputs
- interaction wrappers instead of ad hoc clicks
- conservative waits and retries
- stuck detection and safe shutdown paths
- structured logging that supports test review
