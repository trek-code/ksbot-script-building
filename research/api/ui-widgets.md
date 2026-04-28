# KSBot UI And Widgets

## Topic

Tabs, widgets, dialogs, and user-interface interaction.

## Purpose

Private servers often change UI flow, so widget and dialog handling should be documented early.

## Core Types and Methods

- `Game`
  - `currentTab()`
  - `openTab(Game.Tab)`
  - `setCompass(Game.Direction)`
  - `performEmote(Emote)`
- `Widgets`
  - `query()`
  - `getWidget(componentId)`
  - `getWidget(groupId, childId)`
  - `getWidget(groupId, childId, index)`
  - `getWidget(WidgetInfo)`
  - `getChildContains(parent, text)`
  - `getChildWithOffset(...)`
  - `getItems(widget)`
  - `isUsable(...)`
  - preferred `interact(KSWidget, option, index)`
- `Dialog`
  - `isOpen()`
  - `canContinue()`
  - `continueSpace()`
  - `chooseOption(...)`
  - `hasOption(...)`
  - `enterText(...)`
  - `enterAmount(...)`
  - `getText()`
  - `getName()`

## Lifecycle / Usage Pattern

- Use tab checks to keep the expected UI open before interacting.
- Query or fetch widgets, validate usability, then interact.
- Treat dialogs as a separate state when they can interrupt the main skilling loop.

## Runtime Requirements

- Widgets can be addressed by packed component ID, group/child ID, or `WidgetInfo`.
- The docs deprecate older widget interaction overloads, so new scripts should use the non-deprecated widget interaction signature.

## Failure Modes

- Custom RSPS widgets may not match OSRS assumptions.
- A widget may exist but not be usable if hidden or blocked.
- Dialog input and options can appear unexpectedly and stall loops.

## Reusability Decision

- Generic helper candidate:
  - “ensure tab open”
  - “safe widget fetch and usability check”
  - “dialog resolver” shell
- Server-specific logic:
  - widget IDs
  - option text
  - custom dialog structure
- Template impact:
  - all bots should have a UI recovery branch

## Human-Like Behavior Notes

- Avoid rapid-fire widget calls; validate visibility and then interact once.
- Dialog recognition is important for preventing stuck, obviously robotic loops.

## Source Notes

- Link or citation:
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/game/Game.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/hooks/Widgets.html>
  - <https://ksbotxyz.github.io/rs/kreme/ksbot/api/hooks/widgets/Dialog.html>
  - <https://ksbotxyz.github.io/deprecated-list.html>
- Open questions:
  - We need runtime examples to map target-RSPS widget IDs and confirm dialog patterns.
