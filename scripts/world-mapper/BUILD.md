# World Mapper Build Guide

## Current Artifact

- Script: `Reason World Mapper`
- Version: `0.4.1`
- Default target server: `Reason`
- Output jar: `D:\Codex GPT\RSPS\KSBOT Script building\handoff\packages\world-mapper-reason-0.4.1.jar`

## One-Step Build Command

Run this from PowerShell:

```powershell
& "D:\Codex GPT\RSPS\KSBOT Script building\build\world-mapper\build-world-mapper.ps1"
```

## What The Build Uses

- Java target: `JDK 11` / `--release 11`
- `C:\Users\jonez\.kreme\servers\Reason\rs.kreme.reason-api.jar`
- `C:\Users\jonez\.kreme\servers\Reason\ReasonRSPS-client.jar`

## What The Build Produces

- compiled classes in `D:\Codex GPT\RSPS\KSBOT Script building\build\world-mapper\classes`
- transfer jar in `D:\Codex GPT\RSPS\KSBOT Script building\handoff\packages`
