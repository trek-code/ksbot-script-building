# KSBot Deploy Paths

This file maps the current primary script lanes to their expected Windows KSBot deploy folders.

## Reason server root

- KSBot server root:
  - `C:\Users\<YOUR_USER>\.kreme\servers\Reason`

- API jar:
  - `C:\Users\<YOUR_USER>\.kreme\servers\Reason\rs.kreme.reason-api.jar`

- Client jar:
  - `C:\Users\<YOUR_USER>\.kreme\servers\Reason\ReasonRSPS-client.jar`

## Current primary scripts

- `PF (WBH) Runite ore v1.51`
  - Source: `live-src/pfwbh125`
  - Build script: `build/pf-wbh-v125/build-pf-wbh-v125.ps1`
  - Deploy folder:
    - `C:\Users\<YOUR_USER>\.kreme\servers\Reason\scripts\rs_perk_farmer`

- `Route Builder CL v2.71`
  - Source: `live-src/route/buildercl271`
  - Build script: `build/route-builder-v271/build-route-builder-v271.ps1`
  - Deploy folder:
    - `C:\Users\<YOUR_USER>\.kreme\servers\Reason\scripts\route_builder`

- `Mechanics Capture v1.0.0`
  - Source: `live-src/rsmechanicscapture`
  - Build script: `build/rs-mechanics-capture/build-rs-mechanics-capture.ps1`
  - Deploy folder:
    - `C:\Users\<YOUR_USER>\.kreme\servers\Reason\scripts\rs_mechanics_capture`

- `Cutter of wood v3.3`
  - Source: `live-src/reason/woodcutter`
  - Build script: `build/woodcutter-bot/build-woodcutter.ps1`
  - Deploy folder:
    - `C:\Users\<YOUR_USER>\.kreme\servers\Reason\scripts\woodcutter_bot`

- `Reason World Mapper v0.43`
  - Source: `live-src/reason/mapper`
  - Build script: `build/world-mapper/build-world-mapper.ps1`
  - Deploy folder:
    - `C:\Users\<YOUR_USER>\.kreme\servers\Reason\scripts\world_mapper`

- `Cutter of wood (Reference) v0.1`
  - Source: `live-src/reason/woodcutterreference`
  - Build script: `build/woodcutter-reference/build-woodcutter-reference.ps1`
  - Deploy folder:
    - `C:\Users\<YOUR_USER>\.kreme\servers\Reason\scripts\woodcutter_reference`

## Important note

Most build scripts in this workspace already try to deploy automatically after building.
On the laptop, if your username or folder layout changes, update the paths inside the build scripts or manually copy the built jar into the matching deploy folder above.
