# KSBot Script Entrypoints

## Topic

Manifest requirements and script registration shape.

## Purpose

This note captures the minimum structure a KSBot script must expose to be recognized and described by the platform.

## Core Types and Methods

- Class or interface:
  - `Script`
  - `@ScriptManifest`
- Required manifest elements:
  - `name`
  - `author`
  - `servers`
- Optional manifest elements:
  - `description`
  - `version`
  - `category`
  - `uid`
  - `image`
  - `vip`

## Lifecycle / Usage Pattern

- The script class should be annotated with `@ScriptManifest`.
- The script then provides runtime behavior by overriding `onStart()`, `onProcess()`, and `onStop()`.
- `getManifest()` on the script exposes the manifest at runtime, which is useful for self-reporting and logging.

## Runtime Requirements

- `servers` is required, so every real bot should declare the RSPS it is built for.
- Category defaults to `OTHER` if not specified.
- Version defaults to `1.0`.

## Failure Modes

- Missing required manifest fields would likely make the script invalid for loading or classification.
- Generic `servers` values would make handoff confusing; keep them precise to the target RSPS.

## Reusability Decision

- Generic helper candidate:
  - a standard manifest pattern for internal templates
- Server-specific logic:
  - exact `servers` names
  - bot identity and scope text
- Template impact:
  - every starter file should ship with a clearly marked manifest block

## Human-Like Behavior Notes

- Put server identity directly in the manifest so the wrong script is less likely to be loaded on the wrong RSPS.

## Source Notes

- Link or citation:
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/scripts/ScriptManifest.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/scripts/Script.html>
- Open questions:
  - The docs do not yet show whether manifest metadata changes jar-loading or script library display inside KSBot.
