# Release Note: Cutter of wood 1.1.1

## Release Metadata

- Script name: Cutter of wood
- Script ID: `woodcutter-bot`
- Version: `1.1.1`
- Date: 2026-04-12
- Status: In Progress
- GitHub: Not linked yet

## Summary

Extended the chop engagement window so a successful tree click holds much longer before the bot considers clicking again.

## Included In This Version

- Increased the post-click chopping engagement window from a short tick-based delay to a longer activity window.
- Tied same-tree re-engage behavior more strongly to the last successful tree click and recent cutting activity.
- Kept the “move to next closest tree when clearly done” behavior intact.

## Risks Or Follow-Ups

- If Reason has very short or inconsistent chopping animation pulses on some trees, we may still need one more live tune on the grace duration.
