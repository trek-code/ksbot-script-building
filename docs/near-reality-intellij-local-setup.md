# Near Reality KSBot Local Setup

This follows the KSBot owner workflow, adapted to the exact Near Reality paths on this machine.

## Required JDK

- Use `JDK 11`

## Near Reality Local Files

- API jar:
  `C:\Users\jonez\.kreme\servers\Near-Reality\rs.kreme.nearreality-api.jar`
- Client jar:
  `C:\Users\jonez\.kreme\servers\Near-Reality\NearReality-client.jar`

## IntelliJ Project Setup

1. Create a new Java project.
2. Untick sample code.
3. Make sure the project SDK is `JDK 11`.
4. Open `File -> Project Structure -> Libraries`.
5. Add the API jar first:
   `C:\Users\jonez\.kreme\servers\Near-Reality\rs.kreme.nearreality-api.jar`
6. Add the client jar second:
   `C:\Users\jonez\.kreme\servers\Near-Reality\NearReality-client.jar`

## Run Configuration

Create an `Application` run configuration with:

- Main class:
  `ksbot.Loader`
- VM options:
  `-Dserver.name=Near-Reality`

## Local Script Notes

- The script manifest should target `Near-Reality`.
- The current local UID for the woodcutter is:
  `local-woodcutter-bot`
- If you test from IntelliJ local dev, prefer this flow over dropping jars into the scripts folder first.

## Current Woodcutter Source

- Main class:
  `D:\Codex GPT\RSPS\KSBOT Script building\scripts\woodcutter-bot\src\WoodcutterBot.java`

## Packaging Rule For Local Scripts

- Put local script source in a real Java package, not the default package.
- For the current woodcutter layout, use:
  `local.reason.woodcutter`

## Why This Matters

- The earlier jar attempt used the wrong server placeholder first, which blocked discovery.
- The build is now also pinned to Java 11 output so it matches the owner’s expected local environment.
