# World Mapper History

## Script Identity

- Script name: Reason World Mapper
- Script ID: `world-mapper`
- Current version: `0.4.1`
- Status: In Progress
- Target RSPS: Reason
- GitHub: Not linked yet
- First tracked: 2026-04-12
- Owner: Codex + User

## Purpose

Provide a manual-guided KSBot mapping utility that captures anchors, objects, NPCs, and action data so future walkers and route systems can be built from structured exports instead of screenshots alone.

## Current Scope

- manual-guided area scanning
- one-click quick survey and export
- current-tile capture
- named-anchor capture
- ordered waypoint-chain capture
- automatic route recording while the player walks
- nearest object, NPC, and ground-item capture
- nearby object scans
- nearby NPC scans
- nearby ground-item scans
- object and NPC action capture
- clipboard-friendly capture lines for fast route intake
- blocked-tile scanning
- JSON session export into the workspace when available

## Version Timeline

| Version | Date | Status | Summary |
| --- | --- | --- | --- |
| `0.1.0` | 2026-04-12 | In Progress | Added the first live manual-guided world mapper for Reason with object/NPC/action scanning, anchor capture, and JSON session export. |
| `0.2.0` | 2026-04-12 | In Progress | Added waypoint-chain capture, blocked-tile scanning, and more walker-friendly structured exports for manual city and route mapping. |
| `0.3.0` | 2026-04-12 | In Progress | Added one-click quick survey/export and export summaries so nearby area snapshots are easier to collect and review for walker work. |
| `0.4.0` | 2026-04-12 | In Progress | Added backend nearest-target capture, nearby ground-item scans, clipboard-ready capture lines, and an automatic route recorder for manual route walking. |
| `0.4.1` | 2026-04-12 | In Progress | Added clipboard-ready current-tile capture and aligned the built artifact/versioning with the latest backend-capture helper workflow. |

## Open Questions

- Do we want explicit selected-object capture if KSBot exposes hovered or selected entities later?
- Do we want a route-marking mode with start/end anchor pairs and waypoint chains?
- Should the mapper eventually support auto-grouping exports by city, district, and building?
