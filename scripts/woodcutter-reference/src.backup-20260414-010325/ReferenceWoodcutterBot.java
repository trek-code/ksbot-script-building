package reason.woodcutterreference;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import rs.kreme.ksbot.api.API;
import rs.kreme.ksbot.api.commons.Random;
import rs.kreme.ksbot.api.queries.TileObjectQuery;
import rs.kreme.ksbot.api.scripts.Script;
import rs.kreme.ksbot.api.wrappers.KSObject;
import rs.kreme.ksbot.api.wrappers.KSPlayer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ReferenceWoodcutterBot extends Script {
    private static final int LOOP_DELAY_MIN_MS = 40;
    private static final int LOOP_DELAY_MAX_MS = 80;
    private static final int BANK_OPEN_TIMEOUT_MS = 1200;
    private static final int BANK_CLOSE_TIMEOUT_MS = 800;
    private static final int BANK_ACTION_COOLDOWN_MS = 250;
    private static final int TREE_CLICK_COOLDOWN_MS = 250;
    private static final double MINIMAP_ZOOM = 2.0d;
    private static final String[] AXE_NAMES = {
            "Bronze axe", "Iron axe", "Steel axe", "Black axe", "Mithril axe",
            "Adamant axe", "Rune axe", "Dragon axe", "Crystal axe", "3rd age axe"
    };

    private final ReferenceWoodcuttingProfile.RouteProfile route = ReferenceWoodcuttingProfile.VARROCK_WEST_OAKS;
    private BotState state = BotState.NAVIGATE_TO_TREES;
    private Instant lastBankActionAt;
    private Instant lastTreeClickAt;

    private enum BotState {
        NAVIGATE_TO_TREES,
        CHOPPING,
        NAVIGATE_TO_BANK,
        BANKING,
        RETURN_TO_TREES
    }

    @Override
    public boolean onStart() {
        forceMinimapZoom();
        setStatus("Reference route starting");
        ctx.log("Starting reference woodcutter on route: " + route.label());
        return true;
    }

    @Override
    public int onProcess() {
        if (!hasUsableAxe()) {
            setStatus("No axe found");
            return 300;
        }

        if (ctx.skills.getLevel(Skill.WOODCUTTING) < 15) {
            setStatus("Level too low");
            return 300;
        }

        switch (state) {
            case NAVIGATE_TO_TREES:
                if (ctx.inventory.isFull()) {
                    state = BotState.NAVIGATE_TO_BANK;
                    break;
                }
                if (tryInteractWithRouteTree()) {
                    state = BotState.CHOPPING;
                    break;
                }
                walkRoutePoint(route.returnTreeAnchor(), "Walking to trees");
                break;
            case CHOPPING:
                if (ctx.inventory.isFull()) {
                    state = BotState.NAVIGATE_TO_BANK;
                    break;
                }
                if (!isActivelyChopping()) {
                    if (!tryInteractWithRouteTree()) {
                        state = BotState.NAVIGATE_TO_TREES;
                    }
                }
                setStatus("Chopping");
                break;
            case NAVIGATE_TO_BANK:
                if (ctx.bank.isOpen()) {
                    state = BotState.BANKING;
                    break;
                }
                if (openRouteBank()) {
                    state = BotState.BANKING;
                    break;
                }
                WorldPoint approach = nextBankApproachPoint();
                walkRoutePoint(approach != null ? approach : route.bankStandAnchor(), "Walking to bank");
                setStatus("Navigating to bank");
                break;
            case BANKING:
                if (!ctx.bank.isOpen()) {
                    if (bankActionOnCooldown()) {
                        setStatus("Opening bank");
                        break;
                    }
                    if (!openRouteBank()) {
                        state = BotState.NAVIGATE_TO_BANK;
                    }
                    break;
                }

                if (inventoryHasLogs()) {
                    if (bankActionOnCooldown()) {
                        setStatus("Depositing");
                        break;
                    }
                    ctx.bank.depositAll(route.logNames().toArray(new String[0]));
                    lastBankActionAt = Instant.now();
                    setStatus("Depositing logs");
                    break;
                }

                ctx.bank.close();
                ctx.bank.waitForClose(BANK_CLOSE_TIMEOUT_MS);
                lastBankActionAt = Instant.now();
                state = BotState.RETURN_TO_TREES;
                setStatus("Returning to trees");
                break;
            case RETURN_TO_TREES:
                if (tryInteractWithRouteTree()) {
                    state = BotState.CHOPPING;
                    break;
                }
                walkRoutePoint(route.returnTreeAnchor(), "Returning to trees");
                setStatus("Returning to trees");
                break;
            default:
                break;
        }

        return Random.nextInt(LOOP_DELAY_MIN_MS, LOOP_DELAY_MAX_MS + 1);
    }

    private boolean tryInteractWithRouteTree() {
        if (treeClickOnCooldown() || ctx.inventory.isFull()) {
            return false;
        }

        KSObject tree = findRouteTree();
        if (tree == null) {
            return false;
        }

        String action = tree.hasAction("Chop down") ? "Chop down" : tree.hasAction("Chop") ? "Chop" : null;
        if (action == null) {
            return false;
        }

        boolean interacted = tree.interact(action);
        if (interacted) {
            lastTreeClickAt = Instant.now();
            setStatus("Clicked tree");
        }
        return interacted;
    }

    private KSObject findRouteTree() {
        TileObjectQuery query = ctx.groundObjects.query()
                .withId(route.treeIds())
                .withName(route.treeNames())
                .withOption("Chop down", "Chop");

        List<KSObject> candidates = new ArrayList<>(query.atLocation(route.resourceTiles()).list());
        if (candidates.isEmpty()) {
            candidates.addAll(query.inArea(route.treeArea()).list());
        }
        if (candidates.isEmpty()) {
            candidates.addAll(query.withinDistance(10).list());
        }

        candidates.removeIf(candidate -> candidate == null || candidate.getWorldLocation() == null);
        if (candidates.isEmpty()) {
            return null;
        }

        WorldPoint reference = localPlayerLocation();
        candidates.sort(Comparator.comparingInt(object -> reference.distanceTo(object.getWorldLocation())));
        return candidates.get(0);
    }

    private boolean openRouteBank() {
        List<String> openActions = route.bankOpenActions();
        if (openActions.isEmpty()) {
            return false;
        }

        ctx.bank.clearCustomBanks();
        for (Integer objectId : route.bankObjectIds()) {
            for (String action : openActions) {
                ctx.bank.addCustomBankObject(objectId, action);
            }
        }

        boolean opened = ctx.bank.openBank();
        if (opened) {
            opened = ctx.bank.waitForOpen(BANK_OPEN_TIMEOUT_MS);
        }
        if (opened) {
            lastBankActionAt = Instant.now();
            setStatus("Opened bank");
            return true;
        }

        List<KSObject> candidates = ctx.groundObjects.query()
                .withId(route.bankObjectIds())
                .withinDistance(8)
                .list();
        candidates.removeIf(target -> target == null || target.getWorldLocation() == null);
        candidates.sort(Comparator.comparingInt(target -> route.bankTargetAnchor().distanceTo(target.getWorldLocation())));

        for (KSObject target : candidates) {
            for (String action : openActions) {
                if (target.hasAction(action) && target.interact(action) && ctx.bank.waitForOpen(BANK_OPEN_TIMEOUT_MS)) {
                    lastBankActionAt = Instant.now();
                    setStatus("Opened bank");
                    return true;
                }
            }
        }

        return false;
    }

    private WorldPoint nextBankApproachPoint() {
        for (WorldPoint point : route.bankApproachPath()) {
            if (!ctx.pathing.onTile(point)) {
                return point;
            }
        }
        return route.bankStandAnchor();
    }

    private void walkRoutePoint(WorldPoint point, String status) {
        if (point == null) {
            return;
        }

        if (ctx.pathing.distanceTo(point) <= 6) {
            ctx.pathing.walkToTile(point);
            setStatus(status);
            return;
        }

        List<WorldPoint> path = ctx.pathing.shortestPath(localPlayerLocation(), point);
        if (path != null && path.size() > 1) {
            WorldPoint visible = furthestVisibleStep(path, 10);
            if (visible != null && ctx.pathing.walkMiniMap(visible, MINIMAP_ZOOM)) {
                setStatus(status);
                return;
            }
            if (ctx.pathing.navThroughWorldPath(path)) {
                setStatus(status);
                return;
            }
        }

        ctx.pathing.walkToTile(point);
        setStatus(status);
    }

    private WorldPoint furthestVisibleStep(List<WorldPoint> path, int maxIndex) {
        int lastIndex = Math.min(path.size() - 1, maxIndex);
        for (int index = lastIndex; index >= 1; index--) {
            WorldPoint candidate = path.get(index);
            if (ctx.pathing.worldToMinimap(candidate) != null) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isActivelyChopping() {
        KSPlayer player = ctx.players.getLocal();
        return player != null && (player.isAnimating() || player.isMoving() || !player.isIdle());
    }

    private boolean inventoryHasLogs() {
        for (String logName : route.logNames()) {
            if (ctx.inventory.getTotalQuantity(logName) > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean bankActionOnCooldown() {
        return lastBankActionAt != null
                && Duration.between(lastBankActionAt, Instant.now()).toMillis() < BANK_ACTION_COOLDOWN_MS;
    }

    private boolean treeClickOnCooldown() {
        return lastTreeClickAt != null
                && Duration.between(lastTreeClickAt, Instant.now()).toMillis() < TREE_CLICK_COOLDOWN_MS;
    }

    private boolean hasUsableAxe() {
        return ctx.inventory.contains(AXE_NAMES) || ctx.equipment.contains(AXE_NAMES);
    }

    private void forceMinimapZoom() {
        API.runOnClientThread(() -> API.getClient().setMinimapZoom(MINIMAP_ZOOM));
    }

    private WorldPoint localPlayerLocation() {
        KSPlayer player = ctx.players.getLocal();
        return player == null ? new WorldPoint(0, 0, 0) : player.getWorldLocation();
    }
}
