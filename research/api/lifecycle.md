# KSBot Lifecycle

## Topic

Bot lifecycle and script runtime flow.

## Purpose

This defines the main control model every KSBot script has to follow.

## Core Types and Methods

- Class or interface: `rs.kreme.ksbot.api.scripts.Script`
- Important methods:
  - `boolean onStart()`
  - `abstract int onProcess()`
  - `void onStop()`
  - `boolean onBreak()`
  - `boolean loadConfig()`
  - `boolean isRunning()`, `isPaused()`, `isStopped()`, `isOnBreak()`
- Required inputs:
  - subclass of `Script`
  - `@ScriptManifest` on the script class
- Expected outputs:
  - `onStart()` returns whether startup succeeded
  - `onProcess()` returns the next loop delay in milliseconds

## Lifecycle / Usage Pattern

- `onStart()` is the setup hook for validation and early state initialization.
- `onProcess()` is the main loop and should be treated as the primary control point.
- `onStop()` is the cleanup hook for final logging, shutdown, or persistence.
- `onBreak()` exists as a break-specific hook, which suggests scripts should tolerate platform-driven pause or break states.

## Runtime Requirements

- Scripts run as a loop-based model, not as a callback-only event model.
- The base script exposes `ctx`, `getConfigFile()`, and `getStorageDirectory()` for runtime assets.
- The script instance can check whether it is paused, running, stopped, or on break before taking actions.

## Failure Modes

- Startup should fail early if required account state, location, or config is missing.
- `onProcess()` exceptions should be caught inside the script so the bot can recover or stop safely.
- Pause and break states should suppress interaction instead of forcing the loop onward.

## Reusability Decision

- Generic helper candidate:
  - state machine shell
  - startup validation pattern
  - guarded `onProcess()` loop with retry and recovery
- Server-specific logic:
  - stop conditions
  - startup state checks
  - what “progress” means during skilling
- Template impact:
  - all templates should be `Script` subclasses with a guarded loop and explicit states

## Human-Like Behavior Notes

- Use `onProcess()` delay returns to avoid rigid action cadence.
- Respect pause and break state instead of trying to bypass them.
- Retry through recovery states before repeating direct interactions.

## Source Notes

- Link or citation:
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/scripts/Script.html>
- Open questions:
  - The docs confirm lifecycle hooks but not the scheduler internals behind break handling.
