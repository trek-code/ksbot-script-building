# Route Profile Creator

Two ways to create reusable route-profile folders:

## GUI App

Run:

```powershell
powershell -ExecutionPolicy Bypass -File "D:\Codex GPT\RSPS\KSBOT Script building\tools\route-profile-creator\RouteProfileCreatorApp.ps1"
```

This opens a small desktop-style form where you fill in:

- server name
- bot family
- route name
- activity type
- resource or target type

Then you can:

- create or refresh the route scaffold
- load an existing `route-profile.json`
- add coordinate rows in a structured table
- import the latest or a selected mapper export directly into rows
- import a clipboard capture line from the mapper into the grid
- save everything back into `route-profile.json`
- open the route, captures, exports, and screenshots folders directly

This is the best option for repeated daily use.

## Shell Script

Run:

```powershell
powershell -ExecutionPolicy Bypass -File "D:\Codex GPT\RSPS\KSBOT Script building\tools\route-profile-creator\New-RouteProfile.ps1"
```

Use the shell version if you want a quick command-line workflow or want to automate route folder creation later.
