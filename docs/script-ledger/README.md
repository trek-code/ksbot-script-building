# Script Ledger

This folder is the master tracking system for every KSBot script built in this workspace.

## Purpose

Use the ledger to track:

- script names
- current versions
- release dates
- update summaries
- status of each bot
- per-version change notes
- GitHub repository and file links where available

## Structure

- `index.md`: master index of all scripts
- `scripts/`: one history file per script
- `releases/`: one release note file per versioned update

## Rules

- Every new script gets a script history file before implementation starts.
- Every meaningful update gets a release note.
- The current version in the script history must match the latest release note.
- Physical testing should reference the version from the ledger.
- Add GitHub links once the workspace is connected to a repo.
