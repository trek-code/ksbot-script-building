# KSBot Events

## Topic

Public event hooks versus loop/query polling.

## Purpose

This note documents what the current public docs do and do not expose, so we do not over-design for APIs that may not exist in the script layer.

## Core Types and Methods

- Confirmed public script hooks:
  - `onStart()`
  - `onProcess()`
  - `onStop()`
  - `onBreak()`
- Related packages exposed in the docs:
  - `rs.kreme.ksbot.api.hooks`
  - `rs.kreme.ksbot.api.hooks.widgets`

## Lifecycle / Usage Pattern

- The reviewed docs strongly center on polling and loop processing, not on a first-class event-bus model for scripts.
- Queries are cached per game tick in several hooks, which reinforces the polling model.

## Runtime Requirements

- Scripts should be written to tolerate state discovery through repeated query checks.
- Dialog, widget, and area changes should be modeled as loop-detected transitions.

## Failure Modes

- Designing around assumed listener callbacks could waste time or create the wrong abstraction layer.
- Missing a poll-time check for dialog, staff presence, or full inventory will cause brittle behavior.

## Reusability Decision

- Generic helper candidate:
  - loop-driven transition guards
  - per-tick validation helpers
- Server-specific logic:
  - which signals matter most for the target RSPS
- Template impact:
  - favor deterministic polling helpers over an event-driven architecture until runtime testing proves otherwise

## Human-Like Behavior Notes

- Polling with validation and jittered cadence is safer than hammering the same condition every minimal cycle.

## Source Notes

- Link or citation:
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/scripts/Script.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/queries/package-summary.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/hooks/package-summary.html>
- Open questions:
- This "loop-centric" conclusion is an inference from the public docs reviewed, not an explicit statement from KSBot. We should validate it against runtime examples if you have them.
