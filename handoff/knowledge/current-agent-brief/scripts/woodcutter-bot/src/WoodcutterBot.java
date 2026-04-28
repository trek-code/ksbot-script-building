package local.reason.woodcutter;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import rs.kreme.ksbot.api.commons.Random;
import rs.kreme.ksbot.api.queries.TileObjectQuery;
import rs.kreme.ksbot.api.scripts.Script;
import rs.kreme.ksbot.api.wrappers.KSNPC;
import rs.kreme.ksbot.api.wrappers.KSObject;
import rs.kreme.ksbot.api.wrappers.KSPlayer;

import javax.swing.SwingUtilities;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class WoodcutterBot extends Script {
    private static final int DEFAULT_ACTION_DELAY_MIN_MS = 90;
    private static final int DEFAULT_ACTION_DELAY_MAX_MS = 180;
    private static final int NAVIGATION_DELAY_MIN_MS = 120;
    private static final int NAVIGATION_DELAY_MAX_MS = 220;
    private static final int TREE_CLICK_COOLDOWN_MS = 800;
    private static final int TREE_PROGRESS_GRACE_MS = 2200;
    private static final int TREE_WALK_GRACE_MS = 1700;
    private static final int ROUTE_RECLICK_DELAY_MS = 850;
    private static final int WATCHDOG_TREE_STALL_MS = 6500;
    private static final int WATCHDOG_ROUTE_STALL_MS = 8000;
    private static final int WATCHDOG_BANK_STALL_MS = 5000;
    private static final int SPRING_EVENT_RADIUS = 12;
    private static final String[] SPRING_EVENT_KEYWORDS = {"spring"};
    private static final String[] SPRING_EVENT_ACTIONS = {"Catch", "Pick-up", "Take", "Interact"};

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
    private Instant lastProgressAt = Instant.now();
    private Instant lastInteractionAt;
    private Instant lastRouteStepAt;

    private int logsCut;
    private int lastObservedLogQuantity;
    private int initialWoodcuttingXp;
    private int bankApproachIndex;
    private int dropLogsRemaining;
    private boolean dropPlanInitialized;

    private WorldPoint lastTargetTreeLocation;
    private WorldPoint lastRouteTarget;
    private KSObject currentTargetTree;
    private String lastAction = "boot";

    private WoodcutterControlPanel controlPanel;
    private final WoodcutterDebugManager debugManager = new WoodcutterDebugManager();

    @Override
    public boolean onStart() {
        ensureControlPanel();
        controlPanel.updateSession(buildSnapshot("Idle"));
        controlPanel.showPanel();
        debugManager.record("BOOT", "Cutter of wood UI opened.");
        return true;
    }

    @Override
    public int onProcess() {
        if (controlPanel == null) {
            ensureControlPanel();
        }

        if (isPaused() || isOnBreak()) {
            publishSession("Paused by KSBot");
            return randomBetween(450, 800);
        }

        if (!runningRequested) {
            transitionTo(pausedRequested ? BotState.PAUSED : BotState.IDLE, "waiting");
            publishSession(labelFor(state));
            return randomBetween(250, 450);
        }

        if (pausedRequested) {
            transitionTo(BotState.PAUSED, "manual pause");
            publishSession("Paused");
            return randomBetween(350, 600);
        }

        try {
            if (handleDedicatedSpringEvent()) {
                publishSession("Handling spring event");
                return nextLoopDelay();
            }

            String validationError = validateActiveSettings();
            if (validationError != null) {
                stopWithReason(validationError, "WARN");
                return randomBetween(600, 900);
            }

            if (activeSettings.safeStopOnStaff() && staffNearby()) {
                stopWithReason("Stopped: staff nearby", "WARN");
                return randomBetween(700, 1000);
            }

            if (stopLevelReached()) {
                stopWithReason("Stopped: target level reached", "INFO");
                return randomBetween(700, 1000);
            }

            if (runtimeExceeded()) {
                stopWithReason("Stopped: runtime limit reached", "INFO");
                return randomBetween(700, 1000);
            }

            updateLogProgress();
            runWatchdog();
            updateState();
            executeState();
            publishSession(labelFor(state));
            return nextLoopDelay();
        } catch (RuntimeException ex) {
            debugManager.increment("loopExceptions");
            debugManager.record("ERROR", "Loop exception: " + ex.getMessage());
            publishSession("Loop exception");
            return randomBetween(300, 550);
        }
    }

    @Override
    public void onStop() {
        if (controlPanel != null) {
            publishSession("Stopped");
            controlPanel.closePanel();
        }
        debugManager.record("STOP", "Stopped with state=" + state + ", lastAction=" + lastAction);
    }

    private void ensureControlPanel() {
        if (controlPanel != null) {
            return;
        }

        Runnable createPanel = () -> controlPanel = new WoodcutterControlPanel(activeSettings, new WoodcutterControlPanel.Listener() {
            @Override
            public void onStartRequested(WoodcutterSettings settings) {
                activeSettings = settings;
                runningRequested = true;
                pausedRequested = false;
                runStartedAt = Instant.now();
                stateEnteredAt = Instant.now();
                lastProgressAt = Instant.now();
                lastInteractionAt = null;
                lastRouteStepAt = null;
                lastRouteTarget = null;
                lastTargetTreeLocation = null;
                currentTargetTree = null;
                bankApproachIndex = 0;
                dropLogsRemaining = 0;
                dropPlanInitialized = false;
                logsCut = 0;
                initialWoodcuttingXp = currentWoodcuttingExperience();
                lastObservedLogQuantity = currentLogQuantity();
                lastAction = "start-requested";
                transitionTo(BotState.NAVIGATE_TO_TREES, "start requested");
                debugManager.record("START", "Route=" + settings.routeProfile().label() + ", mode=" + settings.mode());
                publishSession("Starting");
            }

            @Override
            public void onPauseRequested() {
                pausedRequested = !pausedRequested;
                debugManager.record("CONTROL", pausedRequested ? "Pause requested" : "Pause released");
                publishSession(pausedRequested ? "Paused" : labelFor(state));
            }

            @Override
            public void onStopRequested() {
                runningRequested = false;
                pausedRequested = false;
                transitionTo(BotState.STOPPED, "manual stop");
                debugManager.record("CONTROL", "Stop requested from UI");
                publishSession("Stopped");
            }

            @Override
            public void onUpdateRequested(WoodcutterSettings settings) {
                activeSettings = settings;
                controlPanel.applySettings(settings);
                debugManager.record("CONTROL", "Settings updated for " + settings.routeProfile().label());
                publishSession(runningRequested ? labelFor(state) : "Settings Updated");
            }

            @Override
            public String onDebugSnapshotRequested() {
                return buildDebugSnapshot();
            }
        });

        if (SwingUtilities.isEventDispatchThread()) {
            createPanel.run();
            return;
        }

        try {
            SwingUtilities.invokeAndWait(createPanel);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create woodcutter control panel.", exception);
        }
    }

    private void updateState() {
        switch (state) {
            case IDLE:
            case STOPPED:
                transitionTo(BotState.NAVIGATE_TO_TREES, "resume from idle");
                break;
            case NAVIGATE_TO_TREES:
                if (inTreeArea()) {
                    transitionTo(BotState.CHOPPING, "arrived at trees");
                }
                break;
            case CHOPPING:
                if (inventoryFull()) {
                    transitionTo(activeSettings.bankingEnabled() ? BotState.NAVIGATE_TO_BANK : BotState.DROPPING, "inventory full");
                }
                break;
            case DROPPING:
                if (dropCycleFinished()) {
                    transitionTo(BotState.NAVIGATE_TO_TREES, "drop cycle finished");
                }
                break;
            case NAVIGATE_TO_BANK:
                if (inBankArea() || insideBankForInteraction()) {
                    transitionTo(BotState.BANKING, "arrived at bank");
                }
                break;
            case BANKING:
                if (!inventoryHasTreeLogs()) {
                    transitionTo(BotState.RETURN_TO_TREES, "inventory banked");
                }
                break;
            case RETURN_TO_TREES:
                if (inTreeArea()) {
                    transitionTo(BotState.CHOPPING, "returned to trees");
                }
                break;
            case PAUSED:
                break;
        }
    }

    private void executeState() {
        switch (state) {
            case IDLE:
                lastAction = "idle";
                break;
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
        }
    }

    private void navigateToTrees() {
        WorldPoint target = nextTreeApproachPoint();
        if (target == null) {
            return;
        }
        if (inTreeArea()) {
            transitionTo(BotState.CHOPPING, "tree area confirmed");
            return;
        }
        walkRoutePoint(target, "walking to trees", false);
    }

    private void navigateToBank() {
        if (inBankArea() || insideBankForInteraction()) {
            transitionTo(BotState.BANKING, "bank area confirmed");
            return;
        }

        WorldPoint nextApproachPoint = nextBankApproachPoint();
        if (nextApproachPoint != null) {
            walkRoutePoint(nextApproachPoint, "walking bank approach", false);
            return;
        }

        walkRoutePoint(activeSettings.routeProfile().bankAnchor(), "walking to bank", false);
    }

    private void returnToTrees() {
        resetBankApproach();
        if (inTreeArea()) {
            transitionTo(BotState.CHOPPING, "back at trees");
            return;
        }
        walkRoutePoint(nextTreeApproachPoint(), "returning to trees", false);
    }

    private void chopTree() {
        if (inventoryFull()) {
            transitionTo(activeSettings.bankingEnabled() ? BotState.NAVIGATE_TO_BANK : BotState.DROPPING, "inventory full mid chop");
            return;
        }

        if (isTreeEngagementActive()) {
            lastAction = "cutting";
            return;
        }

        if (waitingForTreeClickCooldown()) {
            lastAction = "waiting click cooldown";
            return;
        }

        currentTargetTree = findTargetTree();
        if (currentTargetTree == null) {
            lastAction = "waiting for next tree";
            return;
        }

        if (interactWithTree(currentTargetTree)) {
            ctx.sleepUntil(this::isTreeEngagementActive, 80, 900);
            return;
        }

        debugManager.increment("treeInteractRetries");
        debugManager.record("TREE", "Tree interact failed at " + currentTargetTree.getWorldLocation());
    }

    private void dropLogs() {
        initializeDropPlanIfNeeded();
        if (dropCycleFinished()) {
            resetTreeEngagement();
            return;
        }

        int batchSize = batchSizeFor(activeSettings.dropSpeed());
        int droppedThisLoop = 0;

        for (var item : ctx.inventory.query().withName(WoodcuttingProfile.allKnownLogNames()).list()) {
            if (dropLogsRemaining <= 0 || droppedThisLoop >= batchSize) {
                break;
            }

            if (item.interact("Drop")) {
                droppedThisLoop++;
                dropLogsRemaining--;
                sleepForDrop(activeSettings.dropSpeed());
            }
        }

        lastObservedLogQuantity = currentLogQuantity();
        lastProgressAt = Instant.now();
        lastAction = "dropping logs";

        if (dropCycleFinished()) {
            resetTreeEngagement();
        }
    }

    private void bankLogs() {
        WoodcuttingProfile.RouteProfile route = activeSettings.routeProfile();
        if (!ctx.bank.isOpen()) {
            if (!route.bankApproachPath().isEmpty() && !insideBankForInteraction()) {
                navigateToBank();
                return;
            }

            if (openRouteBank()) {
                ctx.bank.waitForOpen(2500);
                lastAction = "opening bank";
                return;
            }

            debugManager.increment("bankOpenRetries");
            debugManager.record("BANK", "Bank open retry on " + route.bankLabel());
            if (!route.bankApproachPath().isEmpty()) {
                resetBankApproach();
                WorldPoint nextPoint = nextBankApproachPoint();
                if (nextPoint != null) {
                    walkRoutePoint(nextPoint, "repositioning for bank", true);
                }
            } else {
                walkRoutePoint(route.bankStandAnchor() != null ? route.bankStandAnchor() : route.bankAnchor(), "repositioning for bank", true);
            }
            return;
        }

        if (inventoryHasTreeLogs()) {
            if (shouldUseDepositInventory()) {
                ctx.bank.depositInventory();
                lastAction = "deposit inventory";
            } else {
                ctx.bank.depositAll(WoodcuttingProfile.allKnownLogNames().toArray(new String[0]));
                lastAction = "deposit all logs";
            }
            ctx.sleepUntil(() -> !inventoryHasTreeLogs(), 80, 1600);
            return;
        }

        ctx.bank.close();
        ctx.bank.waitForClose(1500);
        resetBankApproach();
        resetTreeEngagement();
        lastObservedLogQuantity = currentLogQuantity();
        lastProgressAt = Instant.now();
        transitionTo(BotState.RETURN_TO_TREES, "banking finished");
    }

    private void runWatchdog() {
        KSPlayer localPlayer = ctx.players.getLocal();
        if (localPlayer == null) {
            return;
        }

        long stateMillis = stateDuration().toMillis();
        boolean idle = localPlayer.isIdle() && !localPlayer.isMoving() && !localPlayer.inMotion();

        if (state == BotState.CHOPPING && stateMillis > WATCHDOG_TREE_STALL_MS && idle && !progressRecentlyObserved(TREE_PROGRESS_GRACE_MS)) {
            debugManager.increment("treeWatchdogResets");
            debugManager.record("FAILSAFE", "Resetting tree engagement after stall.");
            safeIncrementStatistic("treeWatchdogResets");
            resetTreeEngagement();
            transitionTo(BotState.NAVIGATE_TO_TREES, "tree watchdog reroute");
            return;
        }

        if ((state == BotState.NAVIGATE_TO_BANK || state == BotState.RETURN_TO_TREES || state == BotState.NAVIGATE_TO_TREES)
                && stateMillis > WATCHDOG_ROUTE_STALL_MS && idle) {
            debugManager.increment("routeFailsafes");
            debugManager.record("FAILSAFE", "Refreshing route path after idle stall in " + state);
            safeIncrementStatistic("routeFailsafes");
            lastRouteStepAt = null;
            lastRouteTarget = null;
            if (state == BotState.NAVIGATE_TO_BANK) {
                resetBankApproach();
            }
            stateEnteredAt = Instant.now();
            return;
        }

        if (state == BotState.BANKING && stateMillis > WATCHDOG_BANK_STALL_MS && !ctx.bank.isOpen()) {
            debugManager.increment("bankLoopBreaks");
            debugManager.record("FAILSAFE", "Breaking bank loop and retrying bank approach.");
            safeIncrementStatistic("bankLoopBreaks");
            resetBankApproach();
            transitionTo(BotState.NAVIGATE_TO_BANK, "bank watchdog retry");
        }
    }

    private String validateActiveSettings() {
        if (activeSettings.requireAxe() && !hasUsableAxe()) {
            return "No axe found";
        }
        if (activeSettings.routeProfile() == null) {
            return "No route selected";
        }
        if (currentWoodcuttingLevel() < activeSettings.routeProfile().minimumWoodcuttingLevel()) {
            return "Woodcutting level too low for " + activeSettings.routeProfile().label();
        }
        if (activeSettings.bankingEnabled() && activeSettings.useTeleportToBank()
                && !activeSettings.routeProfile().teleportBankSupported()) {
            return "Teleport banking is not configured for this route yet";
        }
        return null;
    }

    private boolean handleDedicatedSpringEvent() {
        KSNPC springNpc = findNearestSpringNpc();
        if (springNpc != null && interactWithSpringNpc(springNpc)) {
            return true;
        }

        KSObject springObject = findNearestSpringObject();
        return springObject != null && interactWithSpringObject(springObject);
    }

    private KSNPC findNearestSpringNpc() {
        List<KSNPC> candidates = ctx.npcs.query().alive().withinDistance(SPRING_EVENT_RADIUS).list();
        KSNPC nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (KSNPC candidate : candidates) {
            if (candidate == null || !matchesSpringKeyword(candidate.getName()) || !hasSpringAction(candidate.getActions())) {
                continue;
            }
            WorldPoint location = candidate.getWorldLocation();
            if (location == null) {
                continue;
            }
            int distance = localPlayerLocation().distanceTo(location);
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private KSObject findNearestSpringObject() {
        List<KSObject> candidates = ctx.groundObjects.query().withinDistance(SPRING_EVENT_RADIUS).list();
        KSObject nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (KSObject candidate : candidates) {
            if (candidate == null || !matchesSpringKeyword(candidate.getName()) || !hasSpringAction(candidate.getActions())) {
                continue;
            }
            WorldPoint location = candidate.getWorldLocation();
            if (location == null) {
                continue;
            }
            int distance = localPlayerLocation().distanceTo(location);
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private boolean interactWithSpringNpc(KSNPC springNpc) {
        for (String action : SPRING_EVENT_ACTIONS) {
            if (hasAction(springNpc.getActions(), action) && springNpc.interact(action)) {
                onSpringEventHandled("npc:" + springNpc.getName());
                return true;
            }
        }
        return false;
    }

    private boolean interactWithSpringObject(KSObject springObject) {
        for (String action : SPRING_EVENT_ACTIONS) {
            if (hasAction(springObject.getActions(), action) && springObject.interact(action)) {
                onSpringEventHandled("object:" + springObject.getName());
                return true;
            }
        }
        return false;
    }

    private void onSpringEventHandled(String detail) {
        lastAction = "spring event";
        lastProgressAt = Instant.now();
        lastInteractionAt = Instant.now();
        debugManager.increment("springEventsHandled");
        debugManager.record("SPRING", "Handled " + detail);
        safeIncrementStatistic("springEventsHandled");
        resetTreeEngagement();
        ctx.sleepUntil(() -> ctx.players.getLocal() != null && !ctx.players.getLocal().isMoving(), 100, 1200);
    }

    private boolean matchesSpringKeyword(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.toLowerCase();
        for (String keyword : SPRING_EVENT_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSpringAction(String[] actions) {
        for (String action : SPRING_EVENT_ACTIONS) {
            if (hasAction(actions, action)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAction(String[] actions, String expected) {
        if (actions == null || expected == null) {
            return false;
        }
        for (String action : actions) {
            if (action != null && action.equalsIgnoreCase(expected)) {
                return true;
            }
        }
        return false;
    }

    private void walkRoutePoint(WorldPoint point, String action, boolean force) {
        if (point == null) {
            return;
        }
        if (!force && waitingForRouteStepToSettle(point)) {
            return;
        }

        List<WorldPoint> path = ctx.pathing.shortestPath(localPlayerLocation(), point);
        if (path != null && path.size() > 1 && path.size() <= 24) {
            ctx.pathing.walkPath(path);
        } else {
            ctx.pathing.walkTo(point);
        }

        lastAction = action;
        lastRouteStepAt = Instant.now();
        lastRouteTarget = point;
    }

    private boolean waitingForRouteStepToSettle(WorldPoint point) {
        if (lastRouteStepAt == null || lastRouteTarget == null) {
            return false;
        }
        if (!lastRouteTarget.equals(point)) {
            return false;
        }
        KSPlayer localPlayer = ctx.players.getLocal();
        if (localPlayer == null) {
            return false;
        }
        long elapsed = Duration.between(lastRouteStepAt, Instant.now()).toMillis();
        return elapsed < ROUTE_RECLICK_DELAY_MS && (localPlayer.isMoving() || localPlayer.inMotion());
    }

    private boolean isTreeEngagementActive() {
        KSPlayer localPlayer = ctx.players.getLocal();
        if (localPlayer == null || lastInteractionAt == null) {
            return false;
        }

        long sinceClick = Duration.between(lastInteractionAt, Instant.now()).toMillis();
        if (sinceClick < TREE_CLICK_COOLDOWN_MS) {
            return true;
        }

        if (localPlayer.isAnimating()) {
            return true;
        }

        if (progressRecentlyObserved(TREE_PROGRESS_GRACE_MS)) {
            return true;
        }

        if (localPlayer.isMoving() || localPlayer.inMotion()) {
            return sinceClick < TREE_WALK_GRACE_MS;
        }

        if (lastTargetTreeLocation != null && localPlayerLocation().distanceTo(lastTargetTreeLocation) <= 2) {
            return sinceClick < 1200;
        }

        return false;
    }

    private boolean waitingForTreeClickCooldown() {
        return lastInteractionAt != null
                && Duration.between(lastInteractionAt, Instant.now()).toMillis() < TREE_CLICK_COOLDOWN_MS;
    }

    private TileObjectQuery baseTreeQuery() {
        return ctx.groundObjects.query()
                .withId(activeSettings.treeType().treeObjectIds())
                .withName(activeSettings.treeType().treeNames())
                .withOption("Chop down", "Chop");
    }

    private TileObjectQuery routeTreeQuery() {
        return baseTreeQuery().inArea(activeSettings.routeProfile().treeArea());
    }

    private TileObjectQuery nearbyTreeQuery() {
        return baseTreeQuery().withinDistance(18);
    }

    private KSObject findTargetTree() {
        List<KSObject> candidates = routeTreeQuery().list();
        if (candidates.isEmpty()) {
            candidates = nearbyTreeQuery().list();
        }

        if (candidates.isEmpty()) {
            return null;
        }

        if (activeSettings.useClosestTree()) {
            return nearestAvailableTree(candidates);
        }

        return candidates.get(randomBetween(0, candidates.size()));
    }

    private KSObject nearestAvailableTree(List<KSObject> candidates) {
        WorldPoint referencePoint = localPlayerLocation();
        if (state == BotState.RETURN_TO_TREES || state == BotState.NAVIGATE_TO_TREES) {
            WorldPoint returnAnchor = activeSettings.routeProfile().returnTreeAnchor();
            if (returnAnchor != null) {
                referencePoint = returnAnchor;
            }
        }

        KSObject nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (KSObject candidate : candidates) {
            if (candidate == null || candidate.getWorldLocation() == null) {
                continue;
            }

            WorldPoint location = candidate.getWorldLocation();
            if (lastTargetTreeLocation != null && lastTargetTreeLocation.equals(location) && waitingForTreeClickCooldown()) {
                continue;
            }

            int distance = referencePoint.distanceTo(location);
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private boolean interactWithTree(KSObject tree) {
        String action = tree.hasAction("Chop down") ? "Chop down" : tree.hasAction("Chop") ? "Chop" : null;
        if (action == null) {
            return false;
        }

        boolean interacted = tree.interact(action);
        if (interacted) {
            lastInteractionAt = Instant.now();
            lastTargetTreeLocation = tree.getWorldLocation();
            lastAction = "click tree";
            debugManager.record("TREE", "Clicked " + tree.getName() + " at " + tree.getWorldLocation());
        }
        return interacted;
    }

    private boolean openRouteBank() {
        WoodcuttingProfile.RouteProfile route = activeSettings.routeProfile();
        if (!route.bankObjectIds().isEmpty() && !route.bankObjectLocations().isEmpty()) {
            for (WorldPoint location : route.bankObjectLocations()) {
                KSObject bankTarget = ctx.groundObjects.query()
                        .withId(route.bankObjectIds())
                        .atLocation(List.of(location))
                        .closest();

                if (bankTarget == null) {
                    continue;
                }

                for (String action : route.bankActions()) {
                    if (bankTarget.hasAction(action) && bankTarget.interact(action)) {
                        lastAction = "bank target interact";
                        debugManager.record("BANK", "Interacted with bank target at " + location);
                        return true;
                    }
                }
            }
        }

        boolean opened = ctx.bank.openBank();
        if (opened) {
            lastAction = "bank helper open";
            debugManager.record("BANK", "Opened bank via helper.");
        }
        return opened;
    }

    private WorldPoint nextBankApproachPoint() {
        List<WorldPoint> approachPath = activeSettings.routeProfile().bankApproachPath();
        if (approachPath.isEmpty()) {
            return null;
        }

        while (bankApproachIndex < approachPath.size()
                && localPlayerLocation().distanceTo(approachPath.get(bankApproachIndex)) <= 1) {
            bankApproachIndex++;
        }

        if (bankApproachIndex >= approachPath.size()) {
            return null;
        }

        return approachPath.get(bankApproachIndex);
    }

    private void resetBankApproach() {
        bankApproachIndex = 0;
    }

    private boolean insideBankForInteraction() {
        WorldPoint standAnchor = activeSettings.routeProfile().bankStandAnchor();
        return inBankArea() || (standAnchor != null && localPlayerLocation().distanceTo(standAnchor) <= 1);
    }

    private WorldPoint nextTreeApproachPoint() {
        WorldPoint preferred = activeSettings.routeProfile().returnTreeAnchor();
        return preferred != null ? preferred : activeSettings.routeProfile().treeAnchor();
    }

    private void initializeDropPlanIfNeeded() {
        if (dropPlanInitialized) {
            return;
        }

        int currentLogs = currentLogQuantity();
        if (currentLogs <= 0) {
            dropLogsRemaining = 0;
            dropPlanInitialized = true;
            return;
        }

        if (activeSettings.dropType() == WoodcutterSettings.DropType.DROP_ALL) {
            dropLogsRemaining = currentLogs;
        } else {
            dropLogsRemaining = Math.min(currentLogs, randomBetween(8, 25));
        }
        dropPlanInitialized = true;
    }

    private boolean dropCycleFinished() {
        return dropLogsRemaining <= 0 || currentLogQuantity() <= 0;
    }

    private int batchSizeFor(WoodcutterSettings.DropSpeed speed) {
        if (speed == WoodcutterSettings.DropSpeed.FAST) {
            return 28;
        }
        return randomBetween(10, 17);
    }

    private void sleepForDrop(WoodcutterSettings.DropSpeed speed) {
        if (speed == WoodcutterSettings.DropSpeed.FAST) {
            ctx.sleep(randomBetween(4, 12));
            return;
        }

        ctx.sleep(randomBetween(28, 60));
        if (Random.nextInt(0, 100) < 10) {
            ctx.sleep(randomBetween(55, 100));
        }
    }

    private void updateLogProgress() {
        int currentLogQuantity = currentLogQuantity();
        if (currentLogQuantity > lastObservedLogQuantity) {
            logsCut += currentLogQuantity - lastObservedLogQuantity;
            lastProgressAt = Instant.now();
            lastAction = "log gained";
            debugManager.increment("logsGainedBursts");
            safeIncrementStatistic("logsGainedBursts");
        } else if (currentLogQuantity < lastObservedLogQuantity) {
            lastProgressAt = Instant.now();
        }
        lastObservedLogQuantity = currentLogQuantity;
    }

    private int currentLogQuantity() {
        int total = 0;
        for (String logName : WoodcuttingProfile.allKnownLogNames()) {
            total += ctx.inventory.getTotalQuantity(logName);
        }
        return total;
    }

    private boolean progressRecentlyObserved(int windowMs) {
        return Duration.between(lastProgressAt, Instant.now()).toMillis() < windowMs;
    }

    private boolean inventoryFull() {
        return ctx.inventory.isFull();
    }

    private boolean inventoryHasTreeLogs() {
        return currentLogQuantity() > 0;
    }

    private boolean inTreeArea() {
        return activeSettings.routeProfile().treeArea().contains(localPlayerLocation());
    }

    private boolean inBankArea() {
        return activeSettings.routeProfile().bankArea().contains(localPlayerLocation());
    }

    private WorldPoint localPlayerLocation() {
        KSPlayer localPlayer = ctx.players.getLocal();
        return localPlayer == null ? new WorldPoint(0, 0, 0) : localPlayer.getWorldLocation();
    }

    private boolean hasUsableAxe() {
        return ctx.inventory.contains(AXE_NAMES) || ctx.equipment.contains(AXE_NAMES);
    }

    private boolean axeEquipped() {
        return ctx.equipment.contains(AXE_NAMES);
    }

    private boolean shouldUseDepositInventory() {
        return axeEquipped() && Random.nextInt(0, 100) < 35;
    }

    private boolean staffNearby() {
        return ctx.antiBan.staffNearby();
    }

    private boolean stopLevelReached() {
        return activeSettings.hasStopAtLevelGoal() && currentWoodcuttingLevel() >= activeSettings.stopAtLevel();
    }

    private int currentWoodcuttingLevel() {
        return ctx.skills.getLevel(Skill.WOODCUTTING);
    }

    private int currentWoodcuttingExperience() {
        return ctx.skills.getExperience(Skill.WOODCUTTING);
    }

    private boolean runtimeExceeded() {
        return runStartedAt != null
                && activeSettings.runtimeLimit().minutes() > 0
                && Duration.between(runStartedAt, Instant.now()).toMinutes() >= activeSettings.runtimeLimit().minutes();
    }

    private void resetTreeEngagement() {
        lastInteractionAt = null;
        lastTargetTreeLocation = null;
        currentTargetTree = null;
    }

    private Duration stateDuration() {
        return Duration.between(stateEnteredAt, Instant.now());
    }

    private void transitionTo(BotState nextState, String reason) {
        if (state != nextState) {
            state = nextState;
            stateEnteredAt = Instant.now();
            debugManager.record("STATE", nextState + " <- " + reason);
        }

        if (nextState == BotState.DROPPING) {
            dropPlanInitialized = false;
            dropLogsRemaining = 0;
        }

        if (nextState == BotState.NAVIGATE_TO_BANK) {
            resetBankApproach();
        }
    }

    private void stopWithReason(String reason, String level) {
        runningRequested = false;
        transitionTo(BotState.STOPPED, reason);
        debugManager.record(level, reason);
        publishSession(reason);
    }

    private int nextLoopDelay() {
        if (activeSettings.antiBanEnabled()) {
            return randomBetween(activeSettings.antiBanDelayMinMs(), activeSettings.antiBanDelayMaxMs() + 1);
        }

        if (state == BotState.NAVIGATE_TO_BANK || state == BotState.NAVIGATE_TO_TREES || state == BotState.RETURN_TO_TREES) {
            return randomBetween(NAVIGATION_DELAY_MIN_MS, NAVIGATION_DELAY_MAX_MS);
        }

        return randomBetween(DEFAULT_ACTION_DELAY_MIN_MS, DEFAULT_ACTION_DELAY_MAX_MS);
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
                activeSettings.treeType().toString(),
                activeSettings.routeProfile().label(),
                activeSettings.routeProfile().bankLabel(),
                runtimeText(),
                logsCut,
                lastAction,
                currentWoodcuttingLevel(),
                currentXp,
                gainedXp,
                xpPerHour,
                ctx.skills.getExperienceToNextLevel(Skill.WOODCUTTING)
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
        builder.append("Time Since Progress (ms): ").append(Duration.between(lastProgressAt, Instant.now()).toMillis()).append(System.lineSeparator());
        builder.append("Time Since Tree Click (ms): ").append(lastInteractionAt == null ? "-" : Duration.between(lastInteractionAt, Instant.now()).toMillis()).append(System.lineSeparator());
        builder.append("Time In State (ms): ").append(stateDuration().toMillis()).append(System.lineSeparator());
        builder.append("Bank Path Index: ").append(bankApproachIndex).append(System.lineSeparator());
        builder.append("Anti-ban Enabled: ").append(activeSettings.antiBanEnabled()).append(System.lineSeparator());
        builder.append(System.lineSeparator()).append(debugManager.dump());
        return builder.toString();
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

    private void safeIncrementStatistic(String key) {
        try {
            ctx.statistics.increment("woodcutter." + key, 1);
        } catch (RuntimeException ignored) {
            // Best-effort only.
        }
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

    private int randomBetween(int min, int maxExclusive) {
        return Random.nextInt(min, maxExclusive);
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
