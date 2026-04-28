# KSBot Anti-Detection

## Topic

Staff detection, pacing discipline, and safer loop design.

## Purpose

The public docs show that KSBot exposes anti-staff helpers, but safe behavior still depends heavily on how we structure scripts.

## Core Types and Methods

- `AntiBan`
  - `getStaff()`
  - `isStaffMember(...)`
  - `nearbyStaff()`
  - `staffNearby()`
- `NPCs`
  - `getNearbyPlayerCount(...)`
  - `hasLOS(...)`
- `Random`
  - bounded randomization for delay shaping
- `Script`
  - `isPaused()`
  - `isOnBreak()`

## Lifecycle / Usage Pattern

- Treat staff detection as a first-class guard, not a late-stage add-on.
- Check for nearby staff before risky repeated loops and during recovery.
- Use player-density signals and line-of-sight checks to avoid crowded or suspicious interactions.

## Runtime Requirements

- `AntiBan` loads a staff list from the server and can query nearby staff within a default or custom distance.
- Public docs suggest several hooks cache data per game tick, which is good for lightweight periodic checks.

## Failure Modes

- Staff lists alone are not enough; private servers may have custom moderation patterns.
- Human-like delays without validation still look bot-like if the script repeats impossible actions.
- Recovery loops that keep forcing the same target are suspicious even with random timing.

## Reusability Decision

- Generic helper candidate:
  - staff-presence guard
  - crowded-target avoidance
  - conservative retry policy
- Server-specific logic:
  - risky zones
  - safe-stop behavior
  - which activities are staff-sensitive on the target RSPS
- Template impact:
  - every bot should expose anti-staff and safe-stop checks from day one

## Human-Like Behavior Notes

- The best anti-detection behavior is “validate, then act,” not “spam with random sleeps.”
- Pause, break, and safe-stop paths should be part of normal runtime design.

## Source Notes

- Link or citation:
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/hooks/AntiBan.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/hooks/NPCs.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/commons/Random.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/scripts/Script.html>
- Open questions:
  - We need to validate how reliable the staff list is on the target RSPS and what the safest automatic response should be when staff are nearby.
