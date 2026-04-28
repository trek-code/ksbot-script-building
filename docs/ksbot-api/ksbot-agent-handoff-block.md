# KSBot Agent Handoff Block

Give the following block to another coding agent when you want them to inherit the KSBot framework knowledge from this session.

```text
You are continuing work in a KSBot RSPS scripting workspace.

Treat the following as hard-won framework knowledge, not optional style:

1. This project should be coded against KSBot’s own API surfaces first, not custom reimplementations.
2. Use the installed local API jar as the truth source for exact method names when needed:
   D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar
3. The local KSBot reference booklet is here:
   D:\Codex GPT\RSPS\KSBOT Script building\docs\ksbot-api\ksbot-api-booklet.md
4. The shorter route/banking framework guide is here:
   D:\Codex GPT\RSPS\KSBOT Script building\docs\ksbot-api\woodcutting-framework-guide.md
5. The full jar-derived API reference index is here:
   D:\Codex GPT\RSPS\KSBOT Script building\docs\ksbot-api\reference\README.md

Core KSBot mental model:

- onProcess() should be loop-driven and cheap
- each tick should observe current truth, perform one action, and return
- real state should come from KSBot hooks such as:
  - ctx.bank.isOpen()
  - ctx.inventory.isFull()
  - ctx.players.getLocal().isIdle()
  - ctx.players.getLocal().isMoving()
  - object / NPC query results
  - ctx.pathing reachability and pathing helpers
- do not build large fake timing frameworks when KSBot already knows the state

Most important API usage rules:

Banking:
- prefer custom bank registration for RSPS banking
- use:
  - ctx.bank.clearCustomBanks()
  - ctx.bank.addCustomBankObject(...)
  - ctx.bank.addCustomBankNPC(...)
  - ctx.bank.openBank()
  - ctx.bank.waitForOpen(...)
  - ctx.bank.depositAll(...)
  - ctx.bank.depositInventory() when appropriate
  - ctx.bank.close()
  - ctx.bank.waitForClose(...)
- movement tiles are only there to get into reliable interaction range
- once bank is open, bank state is truth; do not let stand-tile logic override an already-open bank
- only register real bank-open actions, typically:
  - Bank
  - Use
  - Open
- avoid treating actions like Collect as the primary bank-open action unless the route truly requires it

Pathing:
- use KSBot pathing, not a homemade walker
- common tools:
  - ctx.pathing.walkToTile(...)
  - ctx.pathing.walkMiniMap(...)
  - ctx.pathing.shortestPath(...)
  - ctx.pathing.navThroughWorldPath(...)
  - ctx.pathing.worldToMinimap(...)
  - ctx.pathing.distanceTo(...)
  - ctx.pathing.canReach(...)
  - ctx.pathing.isReachable(...)
- use exact stand tiles and helper waypoints only where route geometry is awkward
- route data should constrain movement choices; pathing engine should stay shared

Queries:
- use hook.query() chains instead of vague proximity assumptions
- for route-critical targets, selection order should be:
  1. exact route tile
  2. route area
  3. nearby fallback

Wrappers:
- KSObject is a primary runtime unit for trees, booths, chests, and route objects
- rely on:
  - getId()
  - getName()
  - getActions()
  - hasAction(...)
  - interact(...)
  - getWorldLocation()
  - isAnimating()

Framework rules:
- prefer facts over waits
- prefer hooks over manual UI simulation
- prefer route data over broad “closest thing nearby”
- verify uncertain method names against the local jar
- export debug traces whenever behavior is ambiguous

Current project-specific state:
- Varrock West Oaks pathing and banking were stabilized after aligning route data and bank logic
- minimap zoom experimentation showed that in this environment, 2.0 behaved as the fully zoomed-out minimap value for the script’s startup adjustment
- the woodcutter now has a debug export path and live debug console
- route profiles exist under:
  D:\Codex GPT\RSPS\KSBOT Script building\research\route-profiles\reason\woodcutting-bot\

If you need to continue framework work:
- read the booklet first
- inspect the actual live source second
- prefer KSBot-native fixes before inventing custom behavior

Important limitation:
- the official ksbot.org docs are heavily client-rendered; a local booklet was created from official site structure plus direct local jar verification
- use the booklet and the jar together as your source of truth
```

## What This Block Does Not Do

It does not create persistent model memory by itself.

It does give the next agent:

- the local reference files
- the workspace conventions
- the verified KSBot-first coding rules
