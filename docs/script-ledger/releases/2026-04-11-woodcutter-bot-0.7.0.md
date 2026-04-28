# Release Note: Woodcutter Bot 0.7.0

## Release Metadata

- Script name: Woodcutter Bot
- Script ID: `woodcutter-bot`
- Version: `0.7.0`
- Date: 2026-04-11
- Status: In Progress
- GitHub: Not linked yet

## Summary

Hardened the woodcutter around real client object IDs and better progress detection so the first live KSBot tests have a stronger target-selection baseline.

## Included In This Version

- Added explicit tree object ID sets for normal, oak, willow, maple, yew, and magic trees.
- Updated tree queries to require both supported names and supported object IDs.
- Improved runtime progress tracking by watching inventory log quantity changes.
- Tightened recovery detection so the bot escalates only after both interaction time and progress time stall.
- Reset and synchronize observed log counts when starting and after dropping logs.

## Risks Or Follow-Ups

- The chosen object ID sets are a strong first pass, but Near Reality may still use extra custom IDs in some zones.
- We still need a live KSBot run to verify the best timeout values for chopping, recovering, and tree respawn behavior.
