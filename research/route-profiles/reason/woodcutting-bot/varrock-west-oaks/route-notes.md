# Varrock West Oaks Notes

## Identity

- Server: `Reason`
- Bot family: `woodcutting-bot`
- Activity: `woodcutting`
- Resource/target: `Oak Tree`
- Route slug: `varrock-west-oaks`
- Created: `2026-04-12`

## Goal

Describe what this route is meant to do and why it exists.

## What To Capture

- target tiles and object or NPC ids
- bank object tile and action text
- inside-bank stand tiles
- doorway or entry tiles
- useful waypoint chains
- obstacle notes
- any server-specific warnings

## Runtime Notes

- start conditions:
  - axe equipped or in inventory
  - minimap zoom forced on startup
- stop conditions:
  - no axe
  - user stop
  - configured level or runtime stop
- route risks:
  - Varrock booth resolution required live validation because the preferred booth shares the same line with other booth objects
- custom server mechanics:
  - preferred booth uses route target tile `3186,3436`
  - preferred stand tile is `3185,3436`
  - reliable opening requires KSBot custom bank registration with both object ids:
    - `10583`
    - `34810`
  - validated for both `Powercut` and `Bank` on `2026-04-13`

## Screenshots To Save

- one wide route overview
- one bank entrance screenshot
- one bank interaction screenshot
- one resource cluster screenshot

## Follow-Up

- missing ids: none for current live route
- missing tiles: none for current live route
- next live test: remaining routes should now be checked against the same shared mover/banker logic
