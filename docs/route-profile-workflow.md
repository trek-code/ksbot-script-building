# Route Profile Workflow

## Goal

Create one reusable route-profile workflow that works for woodcutting now and other bot families later.

## Best Workflow

1. Create a route scaffold with the route-profile workbench.
2. Add coordinate rows in the capture table.
3. Import the latest or selected mapper exports if they help.
4. Save screenshots into the route `screenshots/` folder.
5. Save the route JSON and notes in one batch.
6. Use the route intake template when sending grouped updates back for code changes.

## Route Creator

### GUI App

Run:

```powershell
powershell -ExecutionPolicy Bypass -File "D:\Codex GPT\RSPS\KSBOT Script building\tools\route-profile-creator\RouteProfileCreatorApp.ps1"
```

Use this when you want the most user-friendly flow. The GUI now includes:

- server, bot-family, activity, and route presets
- `+ New` and `Open` flows for separate create vs load behavior
- structured coordinate-entry table
- direct folder open buttons
- mapper-export import buttons that can populate rows
- clipboard-capture import from the mapper
- `route-profile.json` writing from the UI

### Shell Script

Run:

```powershell
powershell -ExecutionPolicy Bypass -File "D:\Codex GPT\RSPS\KSBOT Script building\tools\route-profile-creator\New-RouteProfile.ps1"
```

The creator will ask for:

- server name
- bot family
- route name
- activity type
- resource or target type

It creates this structure:

```text
research/route-profiles/<server>/<bot-family>/<route-slug>/
  route-profile.json
  route-notes.md
  test-checklist.md
  route-intake.md
  captures/
  exports/
  screenshots/
```

## Why This Saves Tokens

- grouped route bundles are cheaper than one-coordinate-at-a-time changes
- route JSON becomes long-term memory inside the workspace
- screenshots and exports stay attached to the exact route
- future bots can reuse the same structure with different activity fields
- the GUI workbench lowers friction enough that route capture is more likely to happen consistently

## Recommended Pattern

- collect all route data first
- patch code second
- test multiple routes in one pass
- send one grouped feedback message
