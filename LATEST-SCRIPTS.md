# KSBot Latest Scripts

This file lists the current primary script lanes worth using on a fresh laptop clone.

## Current Primary Scripts

- `PF (WBH) Runite ore v1.51`
  - Source: `live-src/pfwbh125`
  - Build: `build/pf-wbh-v125/build-pf-wbh-v125.ps1`
  - Notes: `scripts/pf-wbh-v125/README.txt`
  - Purpose: latest active perk farmer lane

- `Route Builder CL v2.71`
  - Source: `live-src/route/buildercl271`
  - Build: `build/route-builder-v271/build-route-builder-v271.ps1`
  - Notes: `scripts/route-builder-v271/README.md`
  - Purpose: route capture, nearest target capture, save/load/export workflow

- `Mechanics Capture v1.0.0`
  - Source: `live-src/rsmechanicscapture`
  - Build: `build/rs-mechanics-capture/build-rs-mechanics-capture.ps1`
  - Notes: `scripts/rs-mechanics-capture/README.txt`
  - Purpose: mechanics observation and capture helper

- `Cutter of wood v3.3`
  - Source: `live-src/reason/woodcutter`
  - Build: `build/woodcutter-bot/build-woodcutter.ps1`
  - Notes: `scripts/woodcutter-bot/README.md`
  - Purpose: main active woodcutting bot

- `Reason World Mapper v0.43`
  - Source: `live-src/reason/mapper`
  - Build: `build/world-mapper/build-world-mapper.ps1`
  - Notes: `scripts/world-mapper/README.md`
  - Purpose: mapper and waypoint capture utility

- `Cutter of wood (Reference) v0.1`
  - Source: `live-src/reason/woodcutterreference`
  - Build: `build/woodcutter-reference/build-woodcutter-reference.ps1`
  - Notes: `scripts/woodcutter-reference/README.md`
  - Purpose: KSBot-first reference scaffold

## Secondary / Archived-But-Kept Lanes

- `RS Perk Farmer CL v1.00`
  - Source: `live-src/rsperkfarmer`

- `RS Perk Farmer v1.03`
  - Source: `live-src/rsperkfarmerv103`

- `Perk Farmer CL Refined v2.2`
  - Source: `live-src/rsperkfarmerclrefinedv2`

- `Perk Farmer CL (World-Boss-Handler Test) v1.24`
  - Source: `live-src/rsperkfarmerclworldbosshandlertest`

## Laptop Workflow

1. Clone the repo.
2. Open the project in IntelliJ.
3. Mark `live-src` as a Sources Root if needed.
4. Use the build script for the script lane you want.
5. Copy the built jar to the KSBot script folder on the laptop, or adjust the build script deploy path to your laptop environment.
