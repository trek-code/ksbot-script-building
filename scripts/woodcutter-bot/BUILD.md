# Woodcutter Build Guide

## Current Artifact

- Script: `Cutter of wood`
- Version: `2.0.0`
- Default target server: `Reason`
- Output jar: `D:\Codex GPT\RSPS\KSBOT Script building\handoff\packages\woodcutter-bot-reason-2.0.0.jar`

## One-Step Build Command

Run this from PowerShell:

```powershell
& "D:\Codex GPT\RSPS\KSBOT Script building\build\woodcutter-bot\build-woodcutter.ps1"
```

To target a different supported server:

```powershell
& "D:\Codex GPT\RSPS\KSBOT Script building\build\woodcutter-bot\build-woodcutter.ps1" -Server "Reason"
```

## What The Build Uses

- Java target: `JDK 11` / `--release 11`
- `C:\Users\jonez\.kreme\servers\Reason\rs.kreme.reason-api.jar`
- `C:\Users\jonez\.kreme\servers\Reason\ReasonRSPS-client.jar`

## What The Build Produces

- compiled classes in `D:\Codex GPT\RSPS\KSBOT Script building\build\woodcutter-bot\classes`
- transfer jar in `D:\Codex GPT\RSPS\KSBOT Script building\handoff\packages`

## First Live Test Checklist

- start Reason with an axe equipped or in inventory
- open the woodcutter UI
- choose a tree route
- choose `Powercut` or `Bank`
- click `Start Script`
- note whether the bot finds, chops, drops, and banks correctly

## Expected Feedback To Bring Back

- whether KSBot loaded the jar successfully
- which tree type and route were selected
- whether the live tree action was `Chop down`, `Chop`, or something else
- whether logs were dropped correctly on a full inventory in `Powercut`
- whether the bot reached the linked bank, deposited logs, and returned correctly in `Bank`
- whether the bot stalled, failed to re-click a tree promptly, or missed a bank target
