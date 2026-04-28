# Woodcutter Bot History

## Script Identity

- Script name: Cutter of wood
- Script ID: `woodcutter-bot`
- Current version: `1.2.2`
- Status: In Progress
- Target RSPS: Reason
- GitHub: Not linked yet
- First tracked: 2026-04-11
- Owner: Codex + User

## Purpose

Provide a simple, safe baseline KSBot woodcutting script for one private server, with a polished control UI and a route-based state machine that can grow into safe banking support.

## Planned V1 Scope

- one target RSPS
- route-based tree and bank locations
- basic sleek control UI
- Start Script button
- Stop Script button
- Pause Script button
- Update Settings button
- `Powercut` and `Bank` modes
- tree-type and route dropdowns
- startup axe validation against inventory or equipment
- optional tree-group marking to focus one chosen local cluster
- linked bank location per route

## Version Timeline

| Version | Date | Status | Summary |
| --- | --- | --- | --- |
| `0.1.0` | 2026-04-11 | Planned | Script concept created, KSBot research linked, and initial scope defined. |
| `0.2.0` | 2026-04-11 | In Progress | Added the first live scaffold with a single-window UI, settings model, location profiles, and script state machine shell. |
| `0.3.0` | 2026-04-11 | In Progress | Added route metadata, stronger startup/settings validation, and richer session feedback in the control window. |
| `0.4.0` | 2026-04-11 | In Progress | Simplified the bot to tree-type selection, start-near-tree anchoring, and powercutting-only behavior. |
| `0.5.0` | 2026-04-11 | In Progress | Added tree-group marking controls and bot-side focus state for constraining the bot to a chosen tree cluster. |
| `0.6.0` | 2026-04-11 | In Progress | Replaced key tree placeholders with real KSBot object queries for start-range validation, target selection, and marked-group capture. |
| `0.7.0` | 2026-04-11 | In Progress | Added explicit Near Reality/OSRS tree object IDs and inventory-based progress tracking to harden live tree targeting and stuck detection. |
| `0.7.1` | 2026-04-11 | In Progress | Fixed the script manifest metadata to target `Near-Reality` explicitly and added a stable local script UID for discovery. |
| `0.8.0` | 2026-04-12 | In Progress | Split the woodcutter into shared core logic plus a `Reason` server entry script and switched the active build target to `Reason`. |
| `0.8.1` | 2026-04-12 | In Progress | Added a short post-click interaction cooldown and slowed powercut dropping to feel more human during live Reason tests. |
| `0.8.2` | 2026-04-12 | In Progress | Added `Stop At Level` to the settings window and wired it to the live woodcutting level while keeping optional runtime stopping. |
| `0.8.3` | 2026-04-12 | In Progress | Renamed the bot to `Cutter of wood`, changed the author to `MindMyLogic`, and expanded the control panel layout for better spacing. |
| `0.9.0` | 2026-04-12 | In Progress | Replaced start-anchor logic with route-based tree locations and added the first linked banking flow for Reason starter routes. |
| `0.9.1` | 2026-04-12 | In Progress | Corrected the Varrock West oak route and added a nearby-tree fallback so route misses do not blind tree targeting or tree-group marking. |
| `0.9.2` | 2026-04-12 | In Progress | Replaced Lumbridge with live-coordinate Varrock West tree routes, added real Reason oak IDs, and created a reusable coordinate registry for future bots. |
| `0.9.3` | 2026-04-12 | In Progress | Added a second confirmed Varrock West bank booth coordinate and documented booth-first banking as the active route assumption. |
| `0.9.4` | 2026-04-12 | In Progress | Added both confirmed Varrock West banker tiles to the coordinate registry and documented the preferred two-oak starter cluster. |
| `0.9.5` | 2026-04-12 | In Progress | Added live Reason willow IDs and Draynor bank booth coordinates, and restored Draynor Willows as a real selectable route. |
| `0.9.6` | 2026-04-12 | In Progress | Documented the custom Reason Edgeville woodcutting area, booth-only banking, and created a dedicated Reason server-notes file for future route expansion. |
| `0.9.7` | 2026-04-12 | In Progress | Added visual layout notes for the custom Reason Edgeville woodcutting zone and documented `::home` as a server-specific landmark. |
| `0.9.8` | 2026-04-12 | In Progress | Switched chopping patience toward local-player activity, added a Draynor inside-bank entry tile, and introduced safe deposit-method variety. |
| `0.9.9` | 2026-04-12 | In Progress | Added Reason Woodcutting Guild guild-bank and yew/magic data, and started a dedicated Reason-specific teleport reference with `::wcguild`. |
| `1.0.0` | 2026-04-12 | In Progress | Promoted Seers Village Maples into the live route list with booth-only banking and live Reason maple coordinates. |
| `1.1.0` | 2026-04-12 | In Progress | Added Woodcutting Guild Yew and Magic routes to the live bot, enforced route level requirements, and tightened same-tree re-engage behavior. |
| `1.1.1` | 2026-04-12 | In Progress | Extended the chop engagement window so one click should hold until cutting clearly stops, instead of re-clicking every few seconds. |
| `1.1.2` | 2026-04-12 | In Progress | Switched route banking toward explicit booth targets, corrected the Seers bank standing point, and made banking deposit every known woodcutting log type so mid-run setting changes do not leave logs behind. |
| `1.2.0` | 2026-04-12 | In Progress | Added configurable powercut drop behavior, upgraded booth/chest banking approach logic, and introduced a reusable looting tab framework with available-vs-active loot lists. |
| `1.2.1` | 2026-04-12 | In Progress | Tightened Varrock, Draynor, and Guild bank approach tiles, removed the farther Varrock backup booth, made fast and human-like drop cycles smoother, and biased returns toward the bank-side tree first. |
| `1.2.2` | 2026-04-12 | In Progress | Replaced key bank entry and inside-bank stand tiles with the newest live Draynor, Varrock West, Seers, and Guild coordinates for smoother route behavior. |

## Open Questions

- How closely do the starter `Reason` routes line up with real walking and bank-open behavior in live tests?
- Do the live Reason tree objects expose stable `Chop down` or `Chop` actions for every selected tree type?
- Does Reason use any extra custom tree object IDs beyond the common OSRS sets now baked into the profile?
- Which route should get teleport banking first once the walking bank loop is stable?
