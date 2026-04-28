Mechanics Capture

Separate imported KSBot observer script for recording boss mechanics.

Purpose:
- capture nearby NPC IDs, animation IDs, graphics IDs, and chat into session files
- provide a live tile-map and deduplicated mechanics table while the user fights normally
- keep the script isolated as its own KSBot lane inside the shared source root

Source root:
- `live-src\\rsmechanicscapture`

Build:
- `build\\rs-mechanics-capture\\build-rs-mechanics-capture.ps1`

Deploy folder:
- `C:\\Users\\jonez\\.kreme\\servers\\Reason\\scripts\\rs_mechanics_capture`
