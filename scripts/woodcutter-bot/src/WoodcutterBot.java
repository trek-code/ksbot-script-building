package reason.woodcutter;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import rs.kreme.ksbot.api.API;
import rs.kreme.ksbot.api.commons.Random;
import rs.kreme.ksbot.api.queries.TileObjectQuery;
import rs.kreme.ksbot.api.scripts.Script;
import rs.kreme.ksbot.api.wrappers.KSObject;
import rs.kreme.ksbot.api.wrappers.KSPlayer;

import javax.swing.SwingUtilities;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class WoodcutterBot extends Script {
    private static final int LOOP_DELAY_MIN_MS = 35;
    private static final int LOOP_DELAY_MAX_MS = 65;
    private static final int MOVE_DELAY_MIN_MS = 50;
    private static final int MOVE_DELAY_MAX_MS = 80;
    private static final int TREE_CLICK_COOLDOWN_MS = 220;
    private static final int TREE_ENGAGEMENT_WINDOW_MS = 900;
    private static final int MOVE_REISSUE_COOLDOWN_MS = 700;
    private static final int BANK_ACTION_COOLDOWN_MS = 250;
    private static final int DROP_ACTION_COOLDOWN_MS = 60;
    private static final int BANK_OPEN_TIMEOUT_MS = 1200;
    private static final int BANK_CLOSE_TIMEOUT_MS = 800;
    private static final int PATH_SEGMENT_LENGTH = 10;
    private static final int MAX_DEBUG_EVENTS = 1200;
    private static final int MINIMAP_ZOOM_RETRY_MS = 1500;
    private static final int MOVE_STALL_RETRY_MS = 1800;
    private static final double MINIMAP_PATH_ZOOM = 2.0d;
    private static final DateTimeFormatter DEBUG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String[] AXE_NAMES = {
            "Bronze axe", "Iron axe", "Steel axe", "Black axe", "Mithril axe",
            "Adamant axe", "Rune axe", "Dragon axe", "Crystal axe", "3rd age axe"
    };

    private volatile WoodcutterSettings activeSettings = WoodcutterSettings.defaults();
    private volatile BotState state = BotState.IDLE;
    private volatile boolean runningRequested;
    private volatile boolean pausedRequested;

    private Instant runStartedAt;
    private Instant stateEnteredAt = Instant.now();
    private Instant lastTreeClickAt;
    private Instant lastTreeGainAt;
    private Instant lastMoveAt;
    private Instant lastBankActionAt;
    private Instant lastDropActionAt;

    private int logsCut;
    private int initialWoodcuttingXp;
    private int lastObservedLogQuantity;
    private int bankApproachIndex;
    private WoodcuttingProfile.RouteProfile lastRegisteredBankRoute;

    private WorldPoint lastMoveTarget;
    private WorldPoint currentTargetTreeLocation;
    private KSObject currentTargetTree;
    private WorldPoint lastBankTargetLocation;
    private String lastBankCandidates = "-";
    private WorldPoint lastPathDestination;
    private String lastPathFailure = "-";
    private int lastPathLength = -1;
    private double lastKnownMinimapZoom = -1d;
    private String lastAction = "boot";
    private Instant lastMinimapZoomAttemptAt;
    private WorldPoint lastObservedPlayerTile;
    private Instant lastObservedPlayerTileAt;
    private int repeatedMoveTargetCount;
    private final List<String> debugEvents = new ArrayList<>();

    private WoodcutterControlPanel controlPanel;

    @Override
    public boolean onStart() {
        ensureControlPanel();
        controlPanel.updateSession(WoodcutterSessionSnapshot.idle(activeSettings));
        controlPanel.showPanel();
        lastAction = "ui opened";
        addDebugEvent("BOOT", "Woodcutter UI opened.");
        return true;
    }

    @Override
    public int onProcess() {
        ensureControlPanel();

        if (isPaused() || isOnBreak()) {
            publishSession("Paused by KSBot");
            return 250;
        }

        if (!runningRequested) {
            state = BotState.IDLE;
            publishSession("Idle");
            return 200;
        }

        if (pausedRequested) {
            state = BotState.PAUSED;
            publishSession("Paused");
            return 200;
        }

        updateLogProgress();
        trackPlayerMovement();

        String validationError = validateSettings();
        if (validationError != null) {
            runningRequested = false;
            state = BotState.STOPPED;
            lastAction = validationError;
            publishSession(validationError);
            return 300;
        }

        if (stopLevelReached()) {
            runningRequested = false;
            state = BotState.STOPPED;
            lastAction = "target level reached";
            publishSession("Stopped: target level reached");
            return 300;
        }

        if (runtimeExceeded()) {
            runningRequested = false;
            state = BotState.STOPPED;
            lastAction = "runtime reached";
            publishSession("Stopped: runtime limit reached");
            return 300;
        }

        updateState();
        executeState();
        publishSession(labelFor(state));
        return nextLoopDelay();
    }

    @Override
    public void onStop() {
        if (controlPanel != null) {
            publishSession("Stopped");
            controlPanel.closePanel();
        }
    }

    private void ensureControlPanel() {
        if (controlPanel != null) {
            return;
        }

        Runnable build = () -> controlPanel = new WoodcutterControlPanel(activeSettings, new WoodcutterControlPanel.Listener() {
            @Override
            public void onStartRequested(WoodcutterSettings settings) {
                activeSettings = settings;
                runningRequested = true;
                pausedRequested = false;
                state = BotState.NAVIGATE_TO_TREES;
                runStartedAt = Instant.now();
                stateEnteredAt = Instant.now();
                lastTreeClickAt = null;
                lastTreeGainAt = null;
                lastMoveAt = null;
                lastBankActionAt = null;
                lastDropActionAt = null;
                logsCut = 0;
                initialWoodcuttingXp = currentWoodcuttingExperience();
                lastObservedLogQuantity = currentLogQuantity();
                bankApproachIndex = 0;
                lastMoveTarget = null;
                currentTargetTreeLocation = null;
                currentTargetTree = null;
                lastBankTargetLocation = null;
                lastBankCandidates = "-";
                lastPathDestination = null;
                lastPathFailure = "-";
                lastPathLength = -1;
                lastKnownMinimapZoom = readMinimapZoom();
                lastMinimapZoomAttemptAt = null;
                lastObservedPlayerTile = localPlayerLocation();
                lastObservedPlayerTileAt = Instant.now();
                repeatedMoveTargetCount = 0;
                lastAction = "start requested";
                clearDebugEvents();
                addDebugEvent("START", "Route=" + settings.routeProfile().label() + ", mode=" + settings.mode());
                forceMinimapZoom("script start");
                publishSession("Starting");
            }

            @Override
            public void onPauseRequested() {
                pausedRequested = !pausedRequested;
                lastAction = pausedRequested ? "paused" : "pause released";
                addDebugEvent("CONTROL", pausedRequested ? "Pause requested" : "Pause released");
                publishSession(pausedRequested ? "Paused" : labelFor(state));
            }

            @Override
            public void onStopRequested() {
                runningRequested = false;
                pausedRequested = false;
                state = BotState.STOPPED;
                lastAction = "manual stop";
                addDebugEvent("CONTROL", "Manual stop requested");
                publishSession("Stopped");
            }

            @Override
            public void onUpdateRequested(WoodcutterSettings settings) {
                activeSettings = settings;
                controlPanel.applySettings(settings);
                lastAction = "settings updated";
                forceMinimapZoom("settings updated");
                addDebugEvent("CONTROL", "Settings updated for " + settings.routeProfile().label());
                publishSession(labelFor(state));
            }

            @Override
            public String onDebugSnapshotRequested() {
                return buildDebugSnapshot();
            }
        });

        if (SwingUtilities.isEventDispatchThread()) {
            build.run();
            return;
        }

        try {
            SwingUtilities.invokeAndWait(build);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create woodcutter UI.", exception);
        }
    }

    private void updateState() {
        switch (state) {
            case IDLE:
            case STOPPED:
                transitionTo(BotState.NAVIGATE_TO_TREES);
                break;
            case NAVIGATE_TO_TREES:
                if (inventoryFull()) {
                    transitionTo(activeSettings.bankingEnabled() ? BotState.NAVIGATE_TO_BANK : BotState.DROPPING);
                } else if (canStartChoppingNow()) {
                    transitionTo(BotState.CHOPPING);
                }
                break;
            case CHOPPING:
                if (inventoryFull()) {
                    clearChopMemory();
                    transitionTo(activeSettings.bankingEnabled() ? BotState.NAVIGATE_TO_BANK : BotState.DROPPING);
                }
                break;
            case DROPPING:
                if (!inventoryHasTreeLogs()) {
                    clearChopMemory();
                    transitionTo(canStartChoppingNow() ? BotState.CHOPPING : BotState.NAVIGATE_TO_TREES);
                }
                break;
            case NAVIGATE_TO_BANK:
                if (ctx.bank.isOpen() || bankInteractionReady()) {
                    transitionTo(BotState.BANKING);
                }
                break;
            case BANKING:
                if (!inventoryHasTreeLogs() && !ctx.bank.isOpen()) {
                    clearChopMemory();
                    transitionTo(BotState.RETURN_TO_TREES);
                }
                break;
            case RETURN_TO_TREES:
                if (canStartChoppingNow()) {
                    transitionTo(BotState.CHOPPING);
                }
                break;
            case PAUSED:
            default:
                break;
        }
    }

    private void executeState() {
        switch (state) {
            case NAVIGATE_TO_TREES:
                navigateToTrees();
                break;
            case CHOPPING:
                chopTree();
                break;
            case DROPPING:
                dropLogs();
                break;
            case NAVIGATE_TO_BANK:
                navigateToBank();
                break;
            case BANKING:
                bankLogs();
                break;
            case RETURN_TO_TREES:
                returnToTrees();
                break;
            case PAUSED:
                lastAction = "paused";
                break;
            case STOPPED:
                lastAction = "stopped";
                break;
            case IDLE:
            default:
                lastAction = "idle";
                break;
        }
    }

    private void navigateToTrees() {
        if (inventoryFull()) {
            transitionTo(activeSettings.bankingEnabled() ? BotState.NAVIGATE_TO_BANK : BotState.DROPPING);
            return;
        }

        if (tryDirectTreeInteraction()) {
            transitionTo(BotState.CHOPPING);
            return;
        }

        walkRoutePoint(nextTreeApproachPoint(), "walking to trees");
    }

    private void chopTree() {
        if (inventoryFull()) {
            clearChopMemory();
            transitionTo(activeSettings.bankingEnabled() ? BotState.NAVIGATE_TO_BANK : BotState.DROPPING);
            return;
        }

        if (isActivelyChopping()) {
            lastAction = "cutting";
            return;
        }

        if (treeClickOnCooldown()) {
            lastAction = "readying next tree";
            return;
        }

        KSObject tree = findTargetTree();
        if (tree == null) {
            lastAction = "no tree found";
            if (!inTreeArea()) {
                transitionTo(BotState.NAVIGATE_TO_TREES);
            }
            return;
        }

        currentTargetTree = tree;
        currentTargetTreeLocation = tree.getWorldLocation();
        if (interactWithTree(tree)) {
            return;
        }

        lastAction = "tree click failed";
    }

    private void dropLogs() {
        if (!inventoryHasTreeLogs()) {
            clearChopMemory();
            transitionTo(canStartChoppingNow() ? BotState.CHOPPING : BotState.NAVIGATE_TO_TREES);
            return;
        }

        if (dropActionOnCooldown()) {
            lastAction = "dropping logs";
            return;
        }

        ctx.inventory.dropAll(WoodcuttingProfile.allKnownLogNames().toArray(new String[0]));
        lastDropActionAt = Instant.now();
        lastAction = "drop all logs";
        addDebugEvent("DROP", "Dropped all known logs.");
        clearChopMemory();
    }

    private void navigateToBank() {
        if (ctx.bank.isOpen()) {
            transitionTo(BotState.BANKING);
            return;
        }

        if (bankInteractionReady()) {
            transitionTo(BotState.BANKING);
            return;
        }

        if (canAttemptDirectBankOpen()) {
            if (bankActionOnCooldown()) {
                lastAction = "opening bank from range";
                return;
            }
            if (openRouteBank()) {
                lastAction = "open bank from range";
                transitionTo(BotState.BANKING);
                return;
            }
        }

        WorldPoint approach = nextBankApproachPoint();
        if (approach != null) {
            walkRoutePoint(approach, "walking bank approach");
            return;
        }

        WorldPoint bankDestination = activeSettings.routeProfile().bankStandAnchor();
        if (bankDestination == null) {
            bankDestination = activeSettings.routeProfile().bankAnchor();
        }
        walkRoutePoint(bankDestination, "walking to bank");
    }

    private boolean canAttemptDirectBankOpen() {
        WoodcuttingProfile.RouteProfile route = activeSettings.routeProfile();
        if (route.insideBankRequired()) {
            if (inBankArea()) {
                return true;
            }
            WorldPoint standAnchor = route.bankStandAnchor();
            return standAnchor != null && localPlayerLocation().distanceTo(standAnchor) <= 4;
        }
        for (WorldPoint location : route.bankTargetLocations()) {
            if (location != null && localPlayerLocation().distanceTo(location) <= 14) {
                return true;
            }
        }
        return false;
    }

    private void bankLogs() {
        if (ctx.bank.isOpen()) {
            if (!inventoryHasTreeLogs()) {
                if (!bankActionOnCooldown()) {
                    ctx.bank.close();
                    ctx.bank.waitForClose(BANK_CLOSE_TIMEOUT_MS);
                    lastBankActionAt = Instant.now();
                    lastAction = "close bank";
                } else {
                    lastAction = "closing bank";
                }
                return;
            }

            if (bankActionOnCooldown()) {
                lastAction = "depositing logs";
                return;
            }
            ctx.bank.depositAll(WoodcuttingProfile.allKnownLogNames().toArray(new String[0]));
            lastBankActionAt = Instant.now();
            lastAction = "deposit logs";
            return;
        }

        if (bankActionOnCooldown()) {
            lastAction = "opening bank";
            return;
        }

        if ((bankInteractionReady() || canAttemptDirectBankOpen()) && openRouteBank()) {
            lastAction = "open bank";
            return;
        }

        transitionTo(BotState.NAVIGATE_TO_BANK);
        lastAction = "reposition bank stand";
    }

    private void returnToTrees() {
        if (tryDirectTreeInteraction()) {
            transitionTo(BotState.CHOPPING);
            return;
        }

        walkRoutePoint(nextTreeApproachPoint(), "returning to trees");
    }

    private boolean canStartChoppingNow() {
        return inTreeArea() || nearbyTreeAvailable();
    }

    private boolean nearbyTreeAvailable() {
        return !baseTreeQuery().withinDistance(10).list().isEmpty();
    }

    private boolean tryDirectTreeInteraction() {
        if (isActivelyChopping() || inventoryFull() || treeClickOnCooldown()) {
            return false;
        }

        KSObject tree = findTargetTree();
        if (tree == null) {
            return false;
        }

        currentTargetTree = tree;
        currentTargetTreeLocation = tree.getWorldLocation();
        return interactWithTree(tree);
    }

    private boolean isActivelyChopping() {
        KSPlayer player = ctx.players.getLocal();
        if (player == null) {
            return false;
        }

        if (player.isAnimating()) {
            return true;
        }

        if (lastTreeGainAt != null && Duration.between(lastTreeGainAt, Instant.now()).toMillis() < TREE_ENGAGEMENT_WINDOW_MS) {
            return true;
        }

        if (lastTreeClickAt == null) {
            return false;
        }

        long sinceClick = Duration.between(lastTreeClickAt, Instant.now()).toMillis();
        if (sinceClick > TREE_ENGAGEMENT_WINDOW_MS) {
            return false;
        }

        if (player.isMoving() || player.inMotion() || !player.isIdle()) {
            return true;
        }

        return currentTargetTree != null
                && currentTargetTreeLocation != null
                && currentTargetTreeLocation.equals(currentTargetTree.getWorldLocation())
                && currentTargetTree.isAnimating();
    }

    private boolean treeClickOnCooldown() {
        return lastTreeClickAt != null
                && Duration.between(lastTreeClickAt, Instant.now()).toMillis() < TREE_CLICK_COOLDOWN_MS;
    }

    private TileObjectQuery baseTreeQuery() {
        return ctx.groundObjects.query()
                .withId(activeSettings.treeType().treeObjectIds())
                .withName(activeSettings.treeType().treeNames())
                .withOption("Chop down", "Chop");
    }

    private KSObject findTargetTree() {
        List<KSObject> candidates = new ArrayList<>();

        candidates.addAll(baseTreeQuery().atLocation(activeSettings.routeProfile().resourceTiles()).list());
        if (candidates.isEmpty()) {
            candidates.addAll(baseTreeQuery().inArea(activeSettings.routeProfile().treeArea()).list());
        }
        if (candidates.isEmpty() && inTreeArea()) {
            candidates.addAll(baseTreeQuery().withinDistance(8).list());
        }
        if (candidates.isEmpty()) {
            candidates.addAll(baseTreeQuery().withinDistance(12).list());
        }
        if (candidates.isEmpty()) {
            candidates.addAll(baseTreeQuery().withinDistance(18).list());
        }

        candidates.removeIf(candidate -> candidate == null || candidate.getWorldLocation() == null);
        if (candidates.isEmpty()) {
            return null;
        }

        WorldPoint reference = localPlayerLocation();
        Comparator<KSObject> comparator = Comparator
                .comparingInt((KSObject object) -> reference.distanceTo(object.getWorldLocation()))
                .thenComparingInt(object -> activeSettings.routeProfile().returnTreeAnchor().distanceTo(object.getWorldLocation()));

        if (!activeSettings.useClosestTree()) {
            candidates.sort(comparator);
            return candidates.get(Random.nextInt(0, candidates.size()));
        }

        candidates.sort(comparator);
        return candidates.get(0);
    }

    private boolean interactWithTree(KSObject tree) {
        String action = tree.hasAction("Chop down") ? "Chop down" : tree.hasAction("Chop") ? "Chop" : null;
        if (action == null) {
            return false;
        }

        boolean interacted = tree.interact(action);
        if (interacted) {
            lastTreeClickAt = Instant.now();
            lastAction = "click tree";
        }
        return interacted;
    }

    private boolean openRouteBank() {
        WoodcuttingProfile.RouteProfile route = activeSettings.routeProfile();
        List<String> openActions = resolveBankOpenActions(route);
        List<KSObject> bankTargets = ctx.groundObjects.query()
                .withId(route.bankObjectIds())
                .withinDistance(8)
                .list();
        bankTargets.removeIf(target -> target == null
                || target.getWorldLocation() == null
                || !hasAnyAction(target, openActions));

        lastBankCandidates = describeBankCandidates(bankTargets);

        List<KSObject> prioritizedTargets = prioritizeBankTargets(route, bankTargets);

        if (!route.bankObjectIds().isEmpty() && !openActions.isEmpty()) {
            if (lastRegisteredBankRoute != route) {
                ctx.bank.clearCustomBanks();
                for (Integer objectId : route.bankObjectIds()) {
                    for (String action : openActions) {
                        ctx.bank.addCustomBankObject(objectId, action);
                    }
                }
                lastRegisteredBankRoute = route;
            }
            boolean opened = ctx.bank.openBank();
            if (opened) {
                opened = ctx.bank.waitForOpen(BANK_OPEN_TIMEOUT_MS);
            }
            if (opened) {
                lastBankActionAt = Instant.now();
                if (!prioritizedTargets.isEmpty()) {
                    lastBankTargetLocation = prioritizedTargets.get(0).getWorldLocation();
                }
                lastAction = "open bank via custom ids";
                addDebugEvent("BANK", "Opened bank via custom object ids " + route.bankObjectIds() + " actions " + openActions);
                return true;
            }
        }

        for (KSObject bankTarget : prioritizedTargets) {
            for (String action : openActions) {
                if (bankTarget.hasAction(action) && bankTarget.interact(action) && ctx.bank.waitForOpen(BANK_OPEN_TIMEOUT_MS)) {
                    lastBankActionAt = Instant.now();
                    lastBankTargetLocation = bankTarget.getWorldLocation();
                    lastAction = "open bank target " + bankTarget.getWorldLocation() + " id=" + bankTarget.getId();
                    addDebugEvent("BANK", "Fallback object interaction at " + bankTarget.getWorldLocation() + " id=" + bankTarget.getId());
                    return true;
                }
            }
        }

        if (route.bankTargetLocations().isEmpty()) {
            boolean opened = ctx.bank.openBank();
            if (opened) {
                opened = ctx.bank.waitForOpen(BANK_OPEN_TIMEOUT_MS);
            }
            if (opened) {
                lastBankActionAt = Instant.now();
                lastAction = "open bank generic";
                addDebugEvent("BANK", "Opened bank via generic bank hook.");
                return true;
            }
        }

        return false;
    }

    private List<String> resolveBankOpenActions(WoodcuttingProfile.RouteProfile route) {
        List<String> preferred = new ArrayList<>();
        for (String action : route.bankActions()) {
            if (action == null) {
                continue;
            }
            String normalized = action.trim();
            if (normalized.equalsIgnoreCase("Bank")
                    || normalized.equalsIgnoreCase("Use")
                    || normalized.equalsIgnoreCase("Open")) {
                preferred.add(normalized);
            }
        }
        if (!preferred.isEmpty()) {
            return preferred.stream().distinct().collect(Collectors.toList());
        }
        return route.bankActions().stream()
                .filter(action -> action != null && !action.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<KSObject> prioritizeBankTargets(WoodcuttingProfile.RouteProfile route, List<KSObject> bankTargets) {
        if (bankTargets.isEmpty()) {
            return bankTargets;
        }

        WorldPoint preferredLocation = route.bankAnchor();
        WorldPoint standAnchor = route.bankStandAnchor();
        List<KSObject> prioritized = new ArrayList<>(bankTargets);
        prioritized.sort(Comparator
                .comparingInt((KSObject target) -> preferredLocation != null && target.getWorldLocation().equals(preferredLocation) ? 0 : 1)
                .thenComparingInt(target -> preferredLocation == null ? 0 : target.getWorldLocation().distanceTo(preferredLocation))
                .thenComparingInt(target -> standAnchor == null ? 0 : target.getWorldLocation().distanceTo(standAnchor))
                .thenComparingInt(KSObject::getDistance));
        return prioritized;
    }

    private boolean hasAnyAction(KSObject object, List<String> actions) {
        for (String action : actions) {
            if (object.hasAction(action)) {
                return true;
            }
        }
        return false;
    }

    private String describeBankCandidates(List<KSObject> bankTargets) {
        if (bankTargets.isEmpty()) {
            return "-";
        }
        List<String> parts = new ArrayList<>();
        for (KSObject target : bankTargets) {
            parts.add(target.getId() + "@" + target.getWorldLocation());
        }
        return String.join(", ", parts);
    }

    private void walkRoutePoint(WorldPoint point, String action) {
        if (point == null) {
            return;
        }

        ensureMinimapZoomedOut(action);

        if (waitingForRouteStepToSettle(point)) {
            lastAction = action;
            return;
        }

        lastPathDestination = point;
        lastPathFailure = "-";

        if (ctx.pathing.distanceTo(point) <= 6) {
            ctx.pathing.walkToTile(point);
            recordMoveAttempt(point);
            lastAction = action;
            addDebugEvent("MOVE", action + " -> exact tile " + point);
            return;
        }

        List<WorldPoint> path = ctx.pathing.shortestPath(localPlayerLocation(), point);
        lastPathLength = path == null ? -1 : path.size();
        if (path != null && path.size() > 1) {
            if (shouldForcePathFallback(point)) {
                boolean moved = ctx.pathing.navThroughWorldPath(path);
                if (moved) {
                    recordMoveAttempt(point);
                    lastAction = action;
                    addDebugEvent("MOVE", action + " -> forced navThroughWorldPath after stall (path length " + path.size() + ")");
                    return;
                }
                lastPathFailure = "Forced navThroughWorldPath failed for " + point + " while " + action;
                addDebugEvent("PATH", lastPathFailure);
            }

            WorldPoint visibleStep = furthestVisiblePathStep(path, PATH_SEGMENT_LENGTH);
            boolean moved = false;

            if (visibleStep != null && !visibleStep.equals(localPlayerLocation())) {
                moved = ctx.pathing.walkMiniMap(visibleStep, MINIMAP_PATH_ZOOM);
                if (moved) {
                    addDebugEvent("MOVE", action + " -> visible step " + visibleStep + " (path length " + path.size() + ")");
                }
            }

            if (!moved) {
                moved = ctx.pathing.navThroughWorldPath(path);
                if (moved) {
                    addDebugEvent("MOVE", action + " -> navThroughWorldPath (path length " + path.size() + ")");
                }
            }

            if (!moved) {
                lastAction = "path unavailable";
                lastPathFailure = "No minimap-visible step for " + point + " while " + action;
                addDebugEvent("PATH", lastPathFailure);
                return;
            }
        } else if (ctx.pathing.distanceTo(point) > 1) {
            boolean moved = ctx.pathing.walkMiniMap(point, MINIMAP_PATH_ZOOM);
            if (moved) {
                addDebugEvent("MOVE", action + " -> direct minimap " + point);
            } else {
                lastAction = "path unavailable";
                lastPathFailure = "No path/minimap point for " + point + " while " + action;
                addDebugEvent("PATH", lastPathFailure);
                return;
            }
        }

        recordMoveAttempt(point);
        lastAction = action;
    }

    private void recordMoveAttempt(WorldPoint point) {
        lastMoveAt = Instant.now();
        if (lastMoveTarget != null && lastMoveTarget.equals(point)) {
            repeatedMoveTargetCount++;
        } else {
            repeatedMoveTargetCount = 1;
        }
        lastMoveTarget = point;
    }

    private boolean shouldForcePathFallback(WorldPoint point) {
        if (lastMoveTarget == null || !lastMoveTarget.equals(point) || lastObservedPlayerTileAt == null) {
            return false;
        }
        if (repeatedMoveTargetCount < 3) {
            return false;
        }
        return Duration.between(lastObservedPlayerTileAt, Instant.now()).toMillis() >= MOVE_STALL_RETRY_MS;
    }

    private boolean waitingForRouteStepToSettle(WorldPoint point) {
        if (lastMoveAt == null || lastMoveTarget == null || !lastMoveTarget.equals(point)) {
            return false;
        }
        long elapsed = Duration.between(lastMoveAt, Instant.now()).toMillis();
        if (elapsed >= MOVE_REISSUE_COOLDOWN_MS) {
            return false;
        }
        KSPlayer player = ctx.players.getLocal();
        return player != null && (player.isMoving() || player.inMotion());
    }

    private WorldPoint furthestVisiblePathStep(List<WorldPoint> path, int maxSteps) {
        if (path == null || path.size() <= 1) {
            return null;
        }

        int maxIndex = Math.min(path.size() - 1, Math.max(1, maxSteps));
        for (int index = maxIndex; index >= 1; index--) {
            WorldPoint candidate = path.get(index);
            if (ctx.pathing.worldToMinimap(candidate) != null) {
                return candidate;
            }
        }

        return null;
    }

    private void ensureMinimapZoomedOut(String reason) {
        if (lastMinimapZoomAttemptAt != null
                && Duration.between(lastMinimapZoomAttemptAt, Instant.now()).toMillis() < MINIMAP_ZOOM_RETRY_MS) {
            return;
        }

        double currentZoom = readMinimapZoom();
        lastKnownMinimapZoom = currentZoom;
        if (Math.abs(currentZoom - MINIMAP_PATH_ZOOM) < 0.01d) {
            return;
        }

        forceMinimapZoom(reason);
    }

    private void forceMinimapZoom(String reason) {
        double beforeZoom = readMinimapZoom();
        lastMinimapZoomAttemptAt = Instant.now();
        API.runOnClientThread(() -> API.getClient().setMinimapZoom(MINIMAP_PATH_ZOOM));
        lastKnownMinimapZoom = readMinimapZoom();
        addDebugEvent("PATH", "Minimap zoom " + beforeZoom + " -> " + lastKnownMinimapZoom + " for " + reason);
    }

    private double readMinimapZoom() {
        try {
            return API.getFromClientThread(() -> API.getClient().getMinimapZoom());
        } catch (Exception ignored) {
            return -1d;
        }
    }

    private WorldPoint nextBankApproachPoint() {
        List<WorldPoint> path = activeSettings.routeProfile().bankApproachPath();
        if (path.isEmpty()) {
            return null;
        }
        while (bankApproachIndex < path.size() && localPlayerLocation().distanceTo(path.get(bankApproachIndex)) <= 6) {
            bankApproachIndex++;
        }
        if (bankApproachIndex >= path.size()) {
            return null;
        }
        return path.get(bankApproachIndex);
    }

    private boolean insideBankForInteraction() {
        WorldPoint standAnchor = activeSettings.routeProfile().bankStandAnchor();
        return standAnchor != null && localPlayerLocation().distanceTo(standAnchor) <= 2;
    }

    private boolean bankInteractionReady() {
        WorldPoint here = localPlayerLocation();
        WorldPoint standAnchor = activeSettings.routeProfile().bankStandAnchor();
        if (standAnchor != null) {
            return here.distanceTo(standAnchor) <= 2;
        }

        WorldPoint bankAnchor = activeSettings.routeProfile().bankAnchor();
        return bankAnchor != null && here.distanceTo(bankAnchor) <= 2;
    }

    private WorldPoint nextTreeApproachPoint() {
        WorldPoint anchor = activeSettings.routeProfile().returnTreeAnchor();
        return anchor != null ? anchor : activeSettings.routeProfile().treeAnchor();
    }

    private void updateLogProgress() {
        int currentLogs = currentLogQuantity();
        if (currentLogs > lastObservedLogQuantity) {
            logsCut += currentLogs - lastObservedLogQuantity;
            lastTreeGainAt = Instant.now();
            lastAction = "log gained";
            addDebugEvent("LOG", "Gained " + (currentLogs - lastObservedLogQuantity) + " log(s).");
        }
        lastObservedLogQuantity = currentLogs;
    }

    private void trackPlayerMovement() {
        WorldPoint playerTile = localPlayerLocation();
        if (lastObservedPlayerTile == null || !lastObservedPlayerTile.equals(playerTile)) {
            lastObservedPlayerTile = playerTile;
            lastObservedPlayerTileAt = Instant.now();
            repeatedMoveTargetCount = 0;
        }
    }

    private int currentLogQuantity() {
        int total = 0;
        for (String logName : WoodcuttingProfile.allKnownLogNames()) {
            total += ctx.inventory.getTotalQuantity(logName);
        }
        return total;
    }

    private boolean inventoryHasTreeLogs() {
        return currentLogQuantity() > 0;
    }

    private boolean inventoryFull() {
        return ctx.inventory.isFull();
    }

    private boolean inTreeArea() {
        return activeSettings.routeProfile().treeArea().contains(localPlayerLocation());
    }

    private boolean inBankArea() {
        return activeSettings.routeProfile().bankArea().contains(localPlayerLocation());
    }

    private WorldPoint localPlayerLocation() {
        KSPlayer player = ctx.players.getLocal();
        return player == null ? new WorldPoint(0, 0, 0) : player.getWorldLocation();
    }

    private boolean hasUsableAxe() {
        return ctx.inventory.contains(AXE_NAMES) || ctx.equipment.contains(AXE_NAMES);
    }

    private String validateSettings() {
        if (activeSettings.routeProfile() == null) {
            return "No route selected";
        }
        if (activeSettings.requireAxe() && !hasUsableAxe()) {
            return "No axe found";
        }
        if (currentWoodcuttingLevel() < activeSettings.routeProfile().minimumWoodcuttingLevel()) {
            return "Level too low for route";
        }
        return null;
    }

    private boolean stopLevelReached() {
        return activeSettings.hasStopAtLevelGoal() && currentWoodcuttingLevel() >= activeSettings.stopAtLevel();
    }

    private boolean runtimeExceeded() {
        return runStartedAt != null
                && activeSettings.runtimeLimit().minutes() > 0
                && Duration.between(runStartedAt, Instant.now()).toMinutes() >= activeSettings.runtimeLimit().minutes();
    }

    private int currentWoodcuttingLevel() {
        return ctx.skills.getLevel(Skill.WOODCUTTING);
    }

    private int currentWoodcuttingExperience() {
        return ctx.skills.getExperience(Skill.WOODCUTTING);
    }

    private boolean bankActionOnCooldown() {
        return lastBankActionAt != null
                && Duration.between(lastBankActionAt, Instant.now()).toMillis() < BANK_ACTION_COOLDOWN_MS;
    }

    private boolean dropActionOnCooldown() {
        return lastDropActionAt != null
                && Duration.between(lastDropActionAt, Instant.now()).toMillis() < DROP_ACTION_COOLDOWN_MS;
    }

    private void clearChopMemory() {
        currentTargetTree = null;
        currentTargetTreeLocation = null;
        lastTreeClickAt = null;
        lastTreeGainAt = null;
    }

    private void transitionTo(BotState nextState) {
        if (state != nextState) {
            BotState previous = state;
            state = nextState;
            stateEnteredAt = Instant.now();
            if (nextState == BotState.NAVIGATE_TO_BANK || nextState == BotState.RETURN_TO_TREES) {
                bankApproachIndex = 0;
            }
            addDebugEvent("STATE", previous + " -> " + nextState);
        }
    }

    private long stateDurationMs() {
        return Duration.between(stateEnteredAt, Instant.now()).toMillis();
    }

    private int nextLoopDelay() {
        switch (state) {
            case NAVIGATE_TO_TREES:
            case NAVIGATE_TO_BANK:
            case RETURN_TO_TREES:
                return randomBetween(MOVE_DELAY_MIN_MS, MOVE_DELAY_MAX_MS);
            case CHOPPING:
            case DROPPING:
            case BANKING:
                return randomBetween(LOOP_DELAY_MIN_MS, LOOP_DELAY_MAX_MS);
            case PAUSED:
            case IDLE:
            case STOPPED:
            default:
                return 200;
        }
    }

    private int randomBetween(int minInclusive, int maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        return Random.nextInt(minInclusive, maxInclusive + 1);
    }

    private void publishSession(String status) {
        if (controlPanel != null) {
            controlPanel.updateSession(buildSnapshot(status));
        }
    }

    private WoodcutterSessionSnapshot buildSnapshot(String status) {
        int currentXp = currentWoodcuttingExperience();
        int gainedXp = Math.max(0, currentXp - initialWoodcuttingXp);
        long runtimeSeconds = runStartedAt == null ? 0 : Math.max(1, Duration.between(runStartedAt, Instant.now()).getSeconds());
        int xpPerHour = runStartedAt == null ? 0 : (int) ((gainedXp * 3600L) / runtimeSeconds);
        return new WoodcutterSessionSnapshot(
                status,
                activeSettings.routeProfile().label(),
                activeSettings.routeProfile().bankLabel(),
                runtimeText(),
                logsCut,
                lastAction,
                currentWoodcuttingLevel(),
                currentXp,
                gainedXp,
                xpPerHour,
                ctx.skills.getExperienceToNextLevel(Skill.WOODCUTTING),
                localPlayerLocation().toString()
        );
    }

    private String buildDebugSnapshot() {
        StringBuilder builder = new StringBuilder();
        builder.append("State: ").append(state).append(System.lineSeparator());
        builder.append("Status: ").append(labelFor(state)).append(System.lineSeparator());
        builder.append("Route: ").append(activeSettings.routeProfile().label()).append(System.lineSeparator());
        builder.append("Tree: ").append(activeSettings.treeType()).append(System.lineSeparator());
        builder.append("Mode: ").append(activeSettings.mode()).append(System.lineSeparator());
        builder.append("Player Tile: ").append(localPlayerLocation()).append(System.lineSeparator());
        builder.append("Last Action: ").append(lastAction).append(System.lineSeparator());
        builder.append("Logs Cut: ").append(logsCut).append(System.lineSeparator());
        builder.append("XP: ").append(currentWoodcuttingExperience()).append(System.lineSeparator());
        builder.append("Time Since Tree Click (ms): ")
                .append(lastTreeClickAt == null ? "-" : Duration.between(lastTreeClickAt, Instant.now()).toMillis())
                .append(System.lineSeparator());
        builder.append("Time Since Tree Gain (ms): ")
                .append(lastTreeGainAt == null ? "-" : Duration.between(lastTreeGainAt, Instant.now()).toMillis())
                .append(System.lineSeparator());
        builder.append("Time Since Move (ms): ")
                .append(lastMoveAt == null ? "-" : Duration.between(lastMoveAt, Instant.now()).toMillis())
                .append(System.lineSeparator());
        builder.append("Time In State (ms): ").append(stateDurationMs()).append(System.lineSeparator());
        builder.append("Bank Path Index: ").append(bankApproachIndex).append(System.lineSeparator());
        builder.append("Current Target Tree: ").append(currentTargetTreeLocation == null ? "-" : currentTargetTreeLocation).append(System.lineSeparator());
        builder.append("Last Bank Target: ").append(lastBankTargetLocation == null ? "-" : lastBankTargetLocation).append(System.lineSeparator());
        builder.append("Last Bank Candidates: ").append(lastBankCandidates).append(System.lineSeparator());
        builder.append("Last Move Target: ").append(lastMoveTarget == null ? "-" : lastMoveTarget).append(System.lineSeparator());
        builder.append("Last Path Destination: ").append(lastPathDestination == null ? "-" : lastPathDestination).append(System.lineSeparator());
        builder.append("Last Path Length: ").append(lastPathLength < 0 ? "-" : lastPathLength).append(System.lineSeparator());
        builder.append("Last Path Failure: ").append(lastPathFailure).append(System.lineSeparator());
        builder.append("Minimap Zoom: ").append(lastKnownMinimapZoom < 0 ? "-" : lastKnownMinimapZoom).append(System.lineSeparator());
        builder.append("Last Observed Tile: ").append(lastObservedPlayerTile == null ? "-" : lastObservedPlayerTile).append(System.lineSeparator());
        builder.append("Time On Observed Tile (ms): ")
                .append(lastObservedPlayerTileAt == null ? "-" : Duration.between(lastObservedPlayerTileAt, Instant.now()).toMillis())
                .append(System.lineSeparator());
        builder.append("Repeated Move Target Count: ").append(repeatedMoveTargetCount).append(System.lineSeparator());
        builder.append("Bank Stand Anchor: ").append(activeSettings.routeProfile().bankStandAnchor() == null ? "-" : activeSettings.routeProfile().bankStandAnchor()).append(System.lineSeparator());
        builder.append("Bank Ready: ").append(bankInteractionReady()).append(System.lineSeparator());
        builder.append("In Tree Area: ").append(inTreeArea()).append(System.lineSeparator());
        builder.append("In Bank Area: ").append(inBankArea()).append(System.lineSeparator());
        builder.append("Inventory Full: ").append(inventoryFull()).append(System.lineSeparator());
        builder.append(System.lineSeparator()).append("Event Log").append(System.lineSeparator());
        for (String event : snapshotDebugEvents()) {
            builder.append("- ").append(event).append(System.lineSeparator());
        }
        return builder.toString().trim();
    }

    private void clearDebugEvents() {
        synchronized (debugEvents) {
            debugEvents.clear();
        }
    }

    private void addDebugEvent(String category, String message) {
        synchronized (debugEvents) {
            debugEvents.add(LocalTime.now().format(DEBUG_TIME) + " [" + category + "] " + message);
            if (debugEvents.size() > MAX_DEBUG_EVENTS) {
                debugEvents.remove(0);
            }
        }
    }

    private List<String> snapshotDebugEvents() {
        synchronized (debugEvents) {
            return new ArrayList<>(debugEvents);
        }
    }

    private String runtimeText() {
        if (runStartedAt == null) {
            return "00:00:00";
        }
        long seconds = Duration.between(runStartedAt, Instant.now()).getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    private String labelFor(BotState currentState) {
        switch (currentState) {
            case NAVIGATE_TO_TREES:
                return "Navigating to trees";
            case CHOPPING:
                return "Chopping";
            case DROPPING:
                return "Powercutting";
            case NAVIGATE_TO_BANK:
                return "Navigating to bank";
            case BANKING:
                return "Banking";
            case RETURN_TO_TREES:
                return "Returning to trees";
            case PAUSED:
                return "Paused";
            case STOPPED:
                return "Stopped";
            case IDLE:
            default:
                return "Idle";
        }
    }

    private enum BotState {
        IDLE,
        NAVIGATE_TO_TREES,
        CHOPPING,
        DROPPING,
        NAVIGATE_TO_BANK,
        BANKING,
        RETURN_TO_TREES,
        PAUSED,
        STOPPED
    }
}
