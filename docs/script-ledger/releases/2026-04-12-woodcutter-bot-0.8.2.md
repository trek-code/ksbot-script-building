# Release Note: Woodcutter Bot 0.8.2

## Release Metadata

- Script name: Woodcutter Bot
- Script ID: `woodcutter-bot`
- Version: `0.8.2`
- Date: 2026-04-12
- Status: In Progress
- GitHub: Not linked yet

## Summary

Added a level-based stop goal so the bot can stop at a chosen woodcutting level instead of only by manual stop or runtime.

## Included In This Version

- Added a `Stop At Level` field to the single-window settings UI.
- Accepts blank as disabled and clamps entered values to the 1-99 range.
- Wired the stop condition to the live woodcutting level through `ctx.skills`.
- Kept `Max Runtime` with `No Limit` available as an optional fallback stop rule.

## Risks Or Follow-Ups

- We still need one live run to confirm the stop triggers at the exact desired level and not one tick late.
- The build guide still has a stale checklist line mentioning Near Reality and should be cleaned up in a later pass.
