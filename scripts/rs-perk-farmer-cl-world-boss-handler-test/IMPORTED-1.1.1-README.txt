RS Perk Farmer — KSBot script (Reason)
======================================

What it does
------------
Automates the Donator Zone perk-task grind:
  1. Zooms the minimap to 2.0 (max).
  2. Checks inventory + equipment for any pickaxe; force-stops if none.
  3. Walks to the Perk Master, right-click Get-task.
  4. Dialog: Skilling -> Elite -> Runite ore (always chooses runite,
     regardless of whether it's option 1 or 2; ignores amethyst).
  5. If an existing perk-task is already active, skips straight to mining.
  6. Walks to runite rock, mines until inventory full.
  7. Walks to bank chest, deposits everything except the pickaxe.
  8. Returns to Perk Master, repeats.
Spring Creature random event is intercepted inline while skilling.

Layout
------
  src/rsperkfarmer/
    RsPerkFarmerBot.java    Script entry point, state machine, KSBot glue
    RsPerkFarmerPanel.java  Swing UI (Main + Debug tabs, status bar)
    BotState.java           State enum + human-readable action strings
    RouteProfile.java       Tile coords / NPC id / pickaxe list
    DebugDumper.java        Writes timestamped debug dump files
  route-profile/
    donator-zone-mining-rune.json   Route profile this script was generated from
  build-rs-perk-farmer.ps1          Compile + deploy

Build
-----
  powershell -ExecutionPolicy Bypass -File .\build-rs-perk-farmer.ps1 `
      -Version 1.0.0 -Server Reason

The jar is deployed to:
  C:\Users\jonez\.kreme\servers\Reason\scripts\rs_perk_farmer\

Bump -Version on each build so KSBot's Reload Scripts sees the new filename.

UI
--
Main tab:
  Buttons: Start | Pause | Stop | Update Script
  (Start/Stop proxy to KSBot's own run state; Pause/Resume toggles the
  built-in Script.pause()/resume(); Update Script resets counters and
  re-runs the equipment check + minimap zoom.)
  Stats: tasks completed, ores mined, springs caught, session uptime.

Debug tab:
  Dump State to File  - writes a full diagnostic text file to
                        <storageDir>\debug-dumps\. Auto-fires on fault.
  Copy Log            - dumps the in-panel log to the clipboard.
  Open Dump Folder    - opens the dumps folder in Explorer.
  Clear Log           - clears the scroll area.
  Panel shows: current state, last fault reason, live action log.

Status bar (bottom of both tabs):
  Now: <current action>   Next: <next action>
  In:  <countdown to next action>   Uptime: <session uptime>

Requirements
------------
- A pickaxe anywhere in inventory or equipment (bronze -> crystal all
  recognized).
- Runite mining level (done by the account, not the bot).
- Reason server, standing anywhere near the Donator Zone teleport.
