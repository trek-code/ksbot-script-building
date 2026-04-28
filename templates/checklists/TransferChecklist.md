# KSBot Transfer Checklist

Use this checklist before every manual transfer to KSBot.

## Artifact Package

- Source script is included if required.
- Jar is included if required.
- File names match the expected bot/version naming convention.
- Dependencies or support files are present.

## Runtime Readiness

- Target RSPS is named.
- Required client/build version is recorded.
- Required account state or inventory setup is recorded.
- Config defaults and stop conditions are documented.

## KSBot Readiness

- Expected load path or import flow is documented.
- Script entrypoint/manifest expectations are checked.
- Logging expectations are known.
- Recovery behavior is defined if the first action fails.

## Test Readiness

- A run note has been created from `TestRunTemplate.md`.
- Expected behavior for the first run is written down.
- Known risk points are listed.
- Next rollback or safe-stop action is clear.
