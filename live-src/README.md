# Live Source Root

This folder is the shared source root for active KSBot scripts in this workspace.

## What this does

- Keeps one real copy of the Java source files.
- Lets IntelliJ and Codex edit the same files.
- Leaves the old `scripts\<name>\src` paths working by turning them into junctions.

## Current package folders

- `reason\woodcutter`
- `reason\mapper`
- `reason\woodcutterreference`
- `route\builder`
- `rsperkfarmer`
- `rsperkfarmerv103`
- `rsperkfarmerclrefinedv2`
- `rsperkfarmerclworldbosshandlertest`
- `pfwbh125`
- `rsmechanicscapture`

## IntelliJ setup

1. Open `D:\Code Agents\Codex GPT\RSPS\KSBOT Script building`.
2. In the Project view, locate `D:\Code Agents\Codex GPT\RSPS\KSBOT Script building\live-src`.
3. Right-click `live-src` and mark it as a `Sources Root` if IntelliJ has not already done so.
4. Open your Java package folders from `live-src\reason\...`, `live-src\route\...`, `live-src\rsperkfarmer`, `live-src\rsperkfarmerv103`, `live-src\rsperkfarmerclrefinedv2`, `live-src\rsperkfarmerclworldbosshandlertest`, `live-src\pfwbh125`, and `live-src\rsmechanicscapture` instead of working from copied files elsewhere.
5. Keep using the existing build scripts. They still point at `scripts\<name>\src`, and those paths are now junctions back into this shared root.

## Setup command

Run this once if the shared root is not wired up yet:

```powershell
powershell -ExecutionPolicy Bypass -File "D:\Code Agents\Codex GPT\RSPS\KSBOT Script building\tools\live-source-root\Setup-LiveSourceRoot.ps1"
```

## Safety

- The setup script creates a timestamped backup of each old `src` folder before linking it.
- Backups live beside each script folder as `src.backup-YYYYMMDD-HHMMSS`.
