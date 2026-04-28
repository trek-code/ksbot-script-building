# Reason Coordinate Registry

This file stores live coordinates and IDs gathered during Reason testing so routes can be built from real server data instead of guesses.

## Varrock West Woodcutting

### Normal Tree Cluster

- Tree, id `1276`, `3160, 3414`
- Tree, id `1276`, `3159, 3416`
- Tree, id `1276`, `3160, 3414`
- Tree, id `1276`, `3162, 3414`
- Tree, id `1276`, `3161, 3410`
- Tree, id `1276`, `3163, 3410`
- Tree, id `1276`, `3166, 3406`

Notes:
- this is now the active starter regular-tree route for the woodcutter
- nearest linked bank is Varrock West Bank

### Oak Tree Cluster

- Oak tree, id `10820`, `3167, 3420`
- Oak tree, id `10820`, `3161, 3416`
- Oak tree, id `10820`, `3165, 3411`

Notes:
- this is now the active starter oak route for the woodcutter
- keep the route focused on the two confirmed core oaks first: `3167, 3420` and `3165, 3411`
- `3161, 3416` remains documented as part of the nearby oak cluster if we widen the route later

### Banking

- Bank booth, id `10583`, `3186, 3436`
- Bank booth, id `10583`, `3186, 3438`
- Banker, id `2897`, `3187, 3436`
- Banker, id `2897`, `3187, 3438`
- Banker, id `2897`, stands behind the Varrock West bank booth line

Notes:
- current bank route anchors use bank booth coordinates instead of banker targeting
- the two confirmed Varrock West booth tiles are enough to keep booth-based banking as the default for now
- the bankers appear to stand one tile behind the booths, which supports staying booth-first unless banker targeting becomes necessary
- if banking feels awkward later, capture 1-2 more booth tiles around the same bank

## Draynor Willow Woodcutting

### Willow Tree Cluster

- Willow tree, id `10838`, `3083, 3237`
- Willow tree, id `10829`, `3085, 3235`
- Willow tree, id `10829`, `3087, 3231`
- Willow tree, id `10831`, `3088, 3227`
- Willow tree, id `10819`, `3088, 3234`

Notes:
- the active willow route should focus on the 4-willow Draynor cluster
- one screenshot also confirmed a nearby willow at `3088, 3234`, which is documented as part of the local cluster data

### Banking

- Bank booth, id `10355`, `3091, 3245`
- Bank booth, id `10355`, `3091, 3243`
- Bank booth, id `10355`, `3091, 3242`
- Bank booth, id `10527`, `3091, 3241`
- Closed bank booth, id `10528`, `3091, 3244`

Notes:
- `3091, 3245` is the closest confirmed booth to the door and should be treated as the primary Draynor bank anchor
- the closed bank booth at `3091, 3244` should not be used for route targeting
- banker coordinates have not been captured yet

## Edgeville Woodcutting (Reason Custom)

This Edgeville area is Reason-specific and should not be assumed to match Near Reality or base OSRS layouts.

### Visual Layout Notes

- The custom woodcutting area sits near the wilderness ditch and the bridge before the Grand Exchange side.
- The area includes mixed low and high level trees in one open field, making it a strong future multi-tree route zone.
- The nearby custom bank uses booths instead of bankers.
- The user-provided `::home` screenshots show that Reason's home hub is a custom social/commercial area and should be treated as a server-specific landmark, not an OSRS default assumption.

### Banking

- Bank booth, id `1096`, `3096, 3440`
- Bank booth, id `1096`, `3096, 3420`

Notes:
- Reason Edgeville banking here uses booths only; there are no bankers in this custom setup
- booth-first banking should remain the default assumption for this area
- screenshots confirm the booths sit inside the nearby custom bank building next to the woodcutting zone

### Normal Trees

- Tree, id `1276`, `3124, 3509`
- Tree, id `1278`, `3126, 3512`
- Tree, id `1278`, `3125, 3505`
- Tree, id `1276`, `3125, 3504`

Notes:
- this appears to be the custom woodcutting area near the wilderness ditch and the bridge before the Grand Exchange

### Willow Trees

- Willow tree, id `10831`, `3121, 3502`
- Willow tree, id `10817`, `3118, 3500`
- Willow tree, id `10819`, `3118, 3500`
- Willow tree, id `10831`, `3116, 3447`

Notes:
- one willow coordinate was captured as `3116, 3447`, which may be correct for Reason's custom area or may need a second verification pass before we turn it into a live route

### Oak Trees

- Oak tree, id `10820`, `3114, 3506`

### Yew Trees

- Yew tree, id `10822`, `3110, 3503`
- Yew tree, id `10822`, `3105, 3507`
- Yew tree, id `10822`, `3111, 3515`

### Magic Trees

- Magic tree, id `10834`, `3114, 3510`
- Magic tree, id `10834`, `3109, 3509`
- Magic tree, id `10834`, `3103, 3508`

Notes:
- these Edgeville yew and magic coordinates are strong candidates for future high-level woodcutting routes once the basic banking flow is fully stable

## Woodcutting Guild (Reason)

This section tracks the Reason Woodcutting Guild separately because it may not match Near Reality or base OSRS exactly.

### Access Notes

- minimum woodcutting level: `60`
- Reason teleport command: `::wcguild`
- teleport result: places the player inside the Woodcutting Guild when the level requirement is met

### Banking

- Bank chest, `1592, 3475`

Notes:
- this is a bank chest, not a standard bank booth
- future banking logic for the guild should support chest interaction as a separate bank type

### Yew Trees

- Yew tree, id `10822`, `1585, 3479`
- Yew tree, id `10822`, `1583, 3481`
- Yew tree, id `10822`, `1595, 3481`
- Yew tree, id `10822`, `1595, 3480`
- Yew tree, id `10822`, `1545, 3491`
- Yew tree, id `10822`, `1590, 3486`
- Yew tree, id `10822`, `1590, 3492`

### Magic Trees

- Magic tree, id `10834`, `1580, 3482`
- Magic tree, id `10834`, `1580, 3485`
- Magic tree, id `10834`, `1577, 3482`
- Magic tree, id `10834`, `1577, 3485`

Notes:
- the guild is a strong candidate for future high-level `Yew`, `Magic`, and `Redwood` routes
- redwood data still needs to be captured and documented

## Reason-Specific Teleports

This section is for server-specific teleport commands that can later be wired into route logic.

### Confirmed Commands

- `::wcguild`
  - destination: Reason Woodcutting Guild
  - requirement: `60` woodcutting
  - notes: places the player inside the guild when the requirement is met

### Pending Commands

- additional Reason teleports to be added as they are discovered

## Seers Village Maple Woodcutting

### Maple Tree Cluster

- Maple tree, id `10832`, `2730, 3501`
- Maple tree, id `10832`, `2732, 3499`
- Maple tree, id `10832`, `2727, 3501`
- Maple tree, id `10832`, `2721, 3501`

Notes:
- these are the full confirmed maple coordinates for the current Seers Village route
- the route should treat this as a booth-bank setup, not a banker setup

### Banking

- Bank booth, id `25808`, `2727, 3494`
- Bank booth, id `25808`, `2728, 3494`

Notes:
- no bankers are needed for this route
- booth-first banking remains the intended model here
- these two booth tiles are the active Seers banking targets for the live bot
