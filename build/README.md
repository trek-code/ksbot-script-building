# Build Workspace

Use this folder for local build outputs and packaging notes that should not replace source templates.

## Intended Contents

- temporary compiled artifacts
- jar outputs before handoff
- local verification notes
- packaging experiments while KSBot build rules are still being learned

## Current Woodcutter Compile Inputs

- `D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar`
- `D:\Codex GPT\RSPS\KSBOT Script building\near-reality\NearReality-client.jar`

The current `woodcutter-bot` source compiles locally when both jars are present on the Java classpath.

Keep the final transfer-ready result in `handoff/` after validation.
