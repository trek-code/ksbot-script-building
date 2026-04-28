# KSBot API Index

Use this file to track documentation coverage and link to topic summaries.

## Coverage Tracker

| Topic | Summary File | Status | Notes |
| --- | --- | --- | --- |
| Bot lifecycle | `lifecycle.md` | Complete | Script subclass with `onStart`, `onProcess`, `onStop`, pause/break state helpers |
| Script entrypoints | `entrypoints.md` | Complete | `@ScriptManifest`, required abstract loop, config/storage methods |
| Required interfaces and classes | `core-types.md` | Complete | `Script`, `Paintable`, `KSContext`, query base, wrappers |
| Movement and pathing | `movement.md` | Complete | `Pathing` supports tile walking, path walking, reachability, danger helpers |
| Object, NPC, and player interactions | `interactions.md` | Complete | Query-first interaction model across hooks and wrappers |
| Inventory and banking | `inventory-banking.md` | Complete | `Inventory`, `Bank`, item use, deposit and withdraw flows |
| UI and widgets | `ui-widgets.md` | Complete | `Widgets`, `Dialog`, tab control, widget queries and interaction |
| Timing and sleeps | `timing.md` | Complete | `onProcess` return delay model plus `Random` and `Timer` helpers |
| Event hooks | `events.md` | Complete | Public docs appear loop/query centric; no first-class event bus surfaced in the main script API |
| Logging and debugging | `logging-debugging.md` | Complete | Paint overlays, config/storage files, and runtime-state logging guidance |
| Packaging and loading | `packaging-loading.md` | Complete | Manifest requirements confirmed; jar loading details still need runtime validation |
| Anti-detection constraints | `anti-detection.md` | Complete | `AntiBan`, query caching, conservative pathing and pause/break handling |

## Usage

- Create one summary file per topic using `api-summary-template.md`.
- Update the status as `Pending`, `In Progress`, or `Complete`.
- Add blockers when the docs are unclear or contradictory.
