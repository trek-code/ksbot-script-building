# KSBot Timing And Sleeps

## Topic

Loop timing, delay control, and time-based tracking.

## Purpose

A human-like script on KSBot should shape timing through the loop model instead of hardcoded spammy action bursts.

## Core Types and Methods

- `Script.onProcess()`
  - returns the next loop delay
- `Random`
  - `nextInt()`
  - `nextInt(min, max)`
  - `nextBool()`
- `Timer`
  - `getElapsedTime()`
  - `getRemaining()`
  - `getRemainingTime()`
  - `isFinished()`
  - `isRunning()`
  - `reset()`
  - `restart()`
  - `getPerHour(...)`

## Lifecycle / Usage Pattern

- Use `onProcess()` return values as the main pacing control.
- Use `Random` to create bounded variance between loops or retries.
- Use `Timer` for run metrics, timeout handling, or stop conditions.

## Runtime Requirements

- Timing is script-managed rather than exposed as a dedicated wait-until helper in the pages reviewed so far.
- Per-hour helper methods make it easier to report rates in paint or logs.

## Failure Modes

- Fixed delays will make the script feel robotic.
- Over-randomization without state validation can make scripts slow and inconsistent.
- Timers should be reset on state changes, not only at script start.

## Reusability Decision

- Generic helper candidate:
  - timing profile object
  - per-state timeout logic
  - rate-tracking helper
- Server-specific logic:
  - cadence ranges for specific activities
  - any custom server delays or animation timings
- Template impact:
  - every bot should centralize timing constants instead of scattering sleeps

## Human-Like Behavior Notes

- Return delays from `onProcess()` based on what just happened.
- Use separate ranges for idle, interaction, failure, and recovery timing.

## Source Notes

- Link or citation:
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/scripts/Script.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/commons/Random.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/commons/Timer.html>
- Open questions:
  - The docs reviewed do not yet show a canonical conditional wait helper, so timing gates will likely need to be script-authored.
