# KSBot RSPS Bot Lab

This workspace is organized to study the KSBot API, build reusable bot templates, and produce transfer-ready artifacts for physical testing on the KSBot platform.

## Workflow

1. Add KSBot docs and notes to `docs/` and `research/api/`.
2. Convert doc findings into the local reference system in `research/`.
3. Build or adapt script templates from `templates/`.
4. Place active bot work in `scripts/`.
5. Stage transfer-ready artifacts and checklists in `handoff/`.
6. Record every physical KSBot run in `test-notes/runs/`.
7. Record every script version and update in `docs/script-ledger/`.

## Directory Guide

- `docs/`: raw links, copied notes, and external documentation summaries
  includes the script ledger for versions and release notes
- `research/`: normalized KSBot knowledge base and server-specific notes
- `templates/`: reusable bot, checklist, and packaging templates
- `scripts/`: active bot implementations and examples
- `build/`: local build notes and generated outputs that should stay outside source templates
- `handoff/`: transfer-ready packages and bot-specific delivery notes
- `test-notes/`: physical KSBot validation logs and follow-up findings

## Working Rules

- Optimize for one RSPS first before abstracting for more servers.
- Prefer reliable, human-like behavior over fast iteration.
- Every bot should have clear states, recovery logic, and transfer notes.
- Every KSBot test should create a written run record, even if it fails early.
- Every shipped change should update the script ledger before physical testing.

## Next Input Needed

Add the KSBot documentation links to [`docs/ksbot-doc-sources.md`](docs/ksbot-doc-sources.md) so the API study phase can begin.
