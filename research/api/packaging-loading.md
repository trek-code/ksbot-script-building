# KSBot Packaging And Loading

## Topic

What the public docs currently confirm about script identity, config files, storage, and deployment.

## Purpose

We need a local handoff process that matches what KSBot actually expects.

## Core Types and Methods

- `@ScriptManifest`
  - required: `name`, `author`, `servers`
  - optional: `description`, `version`, `category`, `uid`, `image`, `vip`
- `Script`
  - `getConfigFile()`
  - `getStorageDirectory()`
  - `loadConfig()`
- `DynamicConfigManager`
  - `setConfigFile(File)`
  - `load(target)`
  - `reload(target)`
  - `save(target)`
  - `openEditor()`
  - `close()`
- `ConfigManager`
  - typed config reads such as `asInt`, `asString`, `asBoolean`, arrays, and doubles

## Lifecycle / Usage Pattern

- A script is identified by manifest metadata and can access a config file and storage directory at runtime.
- Dynamic config support exists for saving annotated fields to JSON and reloading them into a target object.

## Runtime Requirements

- `DynamicConfigManager` only saves and loads annotated fields.
- Config file selection can be set explicitly through `setConfigFile(File)`.

## Failure Modes

- The docs reviewed do not yet show the actual external loader rules for “source script versus jar,” so packaging must still be validated on the real KSBot platform.
- Config models without annotations will not load through the dynamic config manager.

## Reusability Decision

- Generic helper candidate:
  - config object pattern
  - standardized manifest metadata
  - storage layout conventions
- Server-specific logic:
  - config defaults and server-specific route/item data
- Template impact:
  - every bot should have a named manifest, a clear config object, and a handoff note that records whether KSBot used source or jar

## Human-Like Behavior Notes

- Persisting config and route data cleanly will help us tune behavior over time instead of hardcoding every change.

## Source Notes

- Link or citation:
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/scripts/ScriptManifest.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/scripts/Script.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/scripts/config/ConfigManager.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/scripts/config/DynamicConfigManager.html>
- Open questions:
  - We still need a real KSBot load test to confirm the exact transfer rules for jars, directories, and any manifest discovery edge cases.
