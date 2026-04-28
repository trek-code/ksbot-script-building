# Route Builder Stable Backup

This backup captures the current stable `Route Builder` state as of `2026-04-14`.

Contents:

- `src/`
  - source snapshot of the current Claude-imported `Route Builder`
- `packages/route-builder-reason-2.1.0.jar`
  - packaged jar built from this source snapshot
- `packages/route-builder-reason-latest.jar`
  - deployed latest jar copy from the active Reason scripts folder

Notes:

- The manifest version in source is `2.10`, which compiles to `2.1` in the deployed jar.
- This snapshot was saved because the current Route Builder behavior is considered the most stable rollback point so far.
