package reason.mapper;

import net.runelite.api.coords.WorldPoint;
import rs.kreme.ksbot.api.scripts.Script;
import rs.kreme.ksbot.api.wrappers.KSGroundItem;
import rs.kreme.ksbot.api.wrappers.KSNPC;
import rs.kreme.ksbot.api.wrappers.KSObject;

import javax.swing.SwingUtilities;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WorldMapperBot extends Script {
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private volatile WorldMapperControlPanel controlPanel;
    private volatile WorldMapperSession session;
    private volatile boolean recorderActive;
    private volatile Path lastExportPath;
    private WorldPoint lastRecordedWaypoint;
    private String recorderLabelPrefix = "route";
    private int recorderStepIndex = 1;

    @Override
    public boolean onStart() {
        ensurePanel();
        session = new WorldMapperSession("reason-route-capture", "unassigned-zone");
        controlPanel.updateSummary(0, 0, 0, 0, 0, 0);
        controlPanel.updateRecorderState(false);
        controlPanel.appendLog("Mapper ready. Backend capture is active.");
        controlPanel.showPanel();
        return true;
    }

    @Override
    public int onProcess() {
        if (controlPanel == null) {
            ensurePanel();
        }

        if (session != null) {
            session.updateMetadata(controlPanel.sessionName(), controlPanel.zoneName());
        }

        if (recorderActive) {
            autoRecordWaypointIfMoved();
        }

        publishSummary();
        return 250;
    }

    @Override
    public void onStop() {
        if (controlPanel != null) {
            controlPanel.updateRecorderState(false);
            controlPanel.closePanel();
        }
    }

    private void ensurePanel() {
        if (controlPanel != null) {
            return;
        }

        Runnable createPanel = () -> controlPanel = new WorldMapperControlPanel(new WorldMapperControlPanel.Listener() {
            @Override
            public void onCaptureCurrentTile() {
                captureCurrentTile();
            }

            @Override
            public void onCaptureAnchor() {
                captureAnchor();
            }

            @Override
            public void onCaptureNearestObject() {
                captureNearestObject();
            }

            @Override
            public void onCaptureNearestBankTarget() {
                captureNearestBankTarget();
            }

            @Override
            public void onCaptureNearestTreeTarget() {
                captureNearestTreeTarget();
            }

            @Override
            public void onCaptureNearestNpc() {
                captureNearestNpc();
            }

            @Override
            public void onCaptureNearestGroundItem() {
                captureNearestGroundItem();
            }

            @Override
            public void onScanNearbyObjects() {
                scanNearbyObjects();
            }

            @Override
            public void onScanNearbyNpcs() {
                scanNearbyNpcs();
            }

            @Override
            public void onScanNearbyGroundItems() {
                scanNearbyGroundItems();
            }

            @Override
            public void onScanBlockedTiles() {
                scanBlockedTiles();
            }

            @Override
            public void onQuickSurveyAndExport() {
                quickSurveyAndExport();
            }

            @Override
            public void onToggleRouteRecorder() {
                toggleRouteRecorder();
            }

            @Override
            public void onExportSession() {
                exportSession();
            }

            @Override
            public void onCopyLastExportPath() {
                copyLastExportPath();
            }

            @Override
            public void onClearSession() {
                clearSession();
            }
        });

        if (SwingUtilities.isEventDispatchThread()) {
            createPanel.run();
            return;
        }

        try {
            SwingUtilities.invokeAndWait(createPanel);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create world mapper panel.", exception);
        }
    }

    private void captureCurrentTile() {
        WorldPoint playerTile = localPlayerTile();
        if (playerTile == null) {
            log("No player tile available.");
            return;
        }
        session.captureAnchor("player-tile", playerTile);
        copyCaptureLine("tile", "player-tile", 0, playerTile, new String[0]);
        controlPanel.appendLog("Captured current tile at " + format(playerTile) + ".");
        publishSummary();
    }

    private void captureAnchor() {
        WorldPoint playerTile = localPlayerTile();
        if (playerTile == null) {
            log("No player tile available.");
            return;
        }
        String label = controlPanel.anchorLabel();
        session.captureAnchor(label, playerTile);
        controlPanel.appendLog("Captured anchor '" + label + "' at " + format(playerTile) + ".");
        publishSummary();
    }

    private void captureNearestObject() {
        KSObject object = ctx.groundObjects.query().withinDistance(controlPanel.scanRadius()).closest();
        if (object == null) {
            log("No nearby object found.");
            return;
        }
        session.captureObjects(List.of(object));
        copyCaptureLine("object", object.getName(), object.getId(), object.getWorldLocation(), object.getActions());
        controlPanel.appendLog("Captured nearest object: " + object.getName() + " (" + object.getId() + ") at " + format(object.getWorldLocation()) + ".");
        publishSummary();
    }

    private void captureNearestBankTarget() {
        WorldPoint playerTile = localPlayerTile();
        if (playerTile == null) {
            log("No player tile available.");
            return;
        }

        List<KSObject> nearbyObjects = ctx.groundObjects.query().withinDistance(controlPanel.scanRadius()).list();
        List<KSNPC> nearbyNpcs = ctx.npcs.query().withinDistance(controlPanel.scanRadius()).list();

        KSObject bankObject = nearestBankObject(nearbyObjects, playerTile);
        KSNPC bankNpc = nearestBankNpc(nearbyNpcs, playerTile);

        if (bankObject == null && bankNpc == null) {
            log("No nearby bank target found.");
            return;
        }

        if (bankNpc != null && (bankObject == null || distanceTo(playerTile, bankNpc.getWorldLocation()) < distanceTo(playerTile, bankObject.getWorldLocation()))) {
            session.captureNpcs(List.of(bankNpc));
            copyCaptureLine("npc", bankNpc.getName(), bankNpc.getId(), bankNpc.getWorldLocation(), bankNpc.getActions());
            controlPanel.appendLog("Captured nearest bank target NPC: " + bankNpc.getName() + " (" + bankNpc.getId() + ") at " + format(bankNpc.getWorldLocation()) + ".");
        } else {
            session.captureObjects(List.of(bankObject));
            copyCaptureLine("object", bankObject.getName(), bankObject.getId(), bankObject.getWorldLocation(), bankObject.getActions());
            controlPanel.appendLog("Captured nearest bank target object: " + bankObject.getName() + " (" + bankObject.getId() + ") at " + format(bankObject.getWorldLocation()) + ".");
        }
        publishSummary();
    }

    private void captureNearestTreeTarget() {
        WorldPoint playerTile = localPlayerTile();
        if (playerTile == null) {
            log("No player tile available.");
            return;
        }

        KSObject treeTarget = nearestTreeObject(ctx.groundObjects.query().withinDistance(controlPanel.scanRadius()).list(), playerTile);
        if (treeTarget == null) {
            log("No nearby tree target found.");
            return;
        }

        session.captureObjects(List.of(treeTarget));
        copyCaptureLine("object", treeTarget.getName(), treeTarget.getId(), treeTarget.getWorldLocation(), treeTarget.getActions());
        controlPanel.appendLog("Captured nearest tree target: " + treeTarget.getName() + " (" + treeTarget.getId() + ") at " + format(treeTarget.getWorldLocation()) + ".");
        publishSummary();
    }

    private void captureNearestNpc() {
        KSNPC npc = ctx.npcs.query().withinDistance(controlPanel.scanRadius()).closest();
        if (npc == null) {
            log("No nearby NPC found.");
            return;
        }
        session.captureNpcs(List.of(npc));
        copyCaptureLine("npc", npc.getName(), npc.getId(), npc.getWorldLocation(), npc.getActions());
        controlPanel.appendLog("Captured nearest NPC: " + npc.getName() + " (" + npc.getId() + ") at " + format(npc.getWorldLocation()) + ".");
        publishSummary();
    }

    private void captureNearestGroundItem() {
        KSGroundItem item = ctx.groundItems.query().withinDistance(controlPanel.scanRadius()).closest();
        if (item == null) {
            log("No nearby ground item found.");
            return;
        }
        session.captureGroundItems(List.of(item));
        copyCaptureLine("groundItem", item.getName(), item.getId(), item.getWorldLocation(), item.getActions());
        controlPanel.appendLog("Captured nearest ground item: " + item.getName() + " (" + item.getId() + ") x" + item.getQuantity() + " at " + format(item.getWorldLocation()) + ".");
        publishSummary();
    }

    private void scanNearbyObjects() {
        List<KSObject> objects = ctx.groundObjects.query().withinDistance(controlPanel.scanRadius()).list();
        session.captureObjects(objects);
        controlPanel.appendLog("Scanned " + objects.size() + " nearby objects.");
        publishSummary();
    }

    private void scanNearbyNpcs() {
        List<KSNPC> npcs = ctx.npcs.query().withinDistance(controlPanel.scanRadius()).list();
        session.captureNpcs(npcs);
        controlPanel.appendLog("Scanned " + npcs.size() + " nearby NPCs.");
        publishSummary();
    }

    private void scanNearbyGroundItems() {
        List<KSGroundItem> items = ctx.groundItems.query().withinDistance(controlPanel.scanRadius()).list();
        session.captureGroundItems(items);
        controlPanel.appendLog("Scanned " + items.size() + " nearby ground items.");
        publishSummary();
    }

    private void scanBlockedTiles() {
        WorldPoint playerTile = localPlayerTile();
        if (playerTile == null) {
            log("No player tile available.");
            return;
        }

        int radius = controlPanel.scanRadius();
        int captured = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                WorldPoint point = new WorldPoint(playerTile.getX() + dx, playerTile.getY() + dy, playerTile.getPlane());
                if (!ctx.pathing.isTileLoaded(point)) {
                    continue;
                }
                if (ctx.pathing.walkable(point)) {
                    continue;
                }
                session.captureBlockedTile(point, ctx.pathing.getTileFlag(point));
                captured++;
            }
        }

        controlPanel.appendLog("Scanned blocked tiles and captured " + captured + " blocked locations.");
        publishSummary();
    }

    private void quickSurveyAndExport() {
        WorldPoint playerTile = localPlayerTile();
        if (playerTile == null) {
            log("No player tile available.");
            return;
        }
        session.captureAnchor("survey-center", playerTile);
        scanNearbyObjects();
        scanNearbyNpcs();
        scanNearbyGroundItems();
        scanBlockedTiles();
        exportSession();
    }

    private void toggleRouteRecorder() {
        recorderActive = !recorderActive;
        if (recorderActive) {
            recorderLabelPrefix = normalizeRecorderLabel(controlPanel.routeRecorderLabel());
            recorderStepIndex = 1;
            lastRecordedWaypoint = localPlayerTile();
            session.captureWaypoint(recorderLabelPrefix + " - start", lastRecordedWaypoint, false);
            controlPanel.appendLog("Route recorder started for '" + recorderLabelPrefix + "' at " + format(lastRecordedWaypoint) + ".");
        } else {
            controlPanel.appendLog("Route recorder stopped for '" + recorderLabelPrefix + "'.");
        }
        controlPanel.updateRecorderState(recorderActive);
        publishSummary();
    }

    private void autoRecordWaypointIfMoved() {
        WorldPoint playerTile = localPlayerTile();
        if (playerTile == null) {
            return;
        }
        if (lastRecordedWaypoint == null) {
            lastRecordedWaypoint = playerTile;
            return;
        }
        if (playerTile.distanceTo(lastRecordedWaypoint) < controlPanel.waypointDistance()) {
            return;
        }
        session.captureWaypoint(recorderLabelPrefix + " - step " + recorderStepIndex++, playerTile, true);
        lastRecordedWaypoint = playerTile;
        controlPanel.appendLog("Auto-recorded waypoint at " + format(playerTile) + ".");
    }

    private void exportSession() {
        try {
            Path exportDir = exportDirectory();
            Files.createDirectories(exportDir);
            String fileName = sanitize(session.sessionName()) + "-" + sanitize(session.zoneName()) + "-" + FILE_TIMESTAMP.format(LocalDateTime.now()) + ".json";
            Path exportPath = exportDir.resolve(fileName);
            Files.writeString(exportPath, session.toJson(localPlayerTile()), StandardCharsets.UTF_8);
            lastExportPath = exportPath;
            controlPanel.updateLastExport(exportPath.toString());
            controlPanel.appendLog("Exported session to " + exportPath + ".");
        } catch (IOException exception) {
            log("Export failed: " + exception.getMessage());
        }
    }

    private void copyLastExportPath() {
        if (lastExportPath == null) {
            log("No export path available to copy.");
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(lastExportPath.toString()), null);
        controlPanel.appendLog("Copied export path to clipboard.");
    }

    private void clearSession() {
        session.clear();
        recorderActive = false;
        lastRecordedWaypoint = null;
        lastExportPath = null;
        controlPanel.updateRecorderState(false);
        controlPanel.updateLastExport("-");
        controlPanel.appendLog("Session cleared.");
        publishSummary();
    }

    private void publishSummary() {
        if (session == null || controlPanel == null) {
            return;
        }
        controlPanel.updateSummary(
                session.anchorCount(),
                session.waypointCount(),
                session.objectCount(),
                session.npcCount(),
                session.groundItemCount(),
                session.blockedTileCount()
        );
    }

    private WorldPoint localPlayerTile() {
        return ctx.players.getLocal() == null ? null : ctx.players.getLocal().getWorldLocation();
    }

    private String normalizeRecorderLabel(String rawLabel) {
        if (rawLabel == null || rawLabel.isBlank()) {
            return "route";
        }
        return rawLabel.trim();
    }

    private KSObject nearestBankObject(List<KSObject> objects, WorldPoint playerTile) {
        KSObject best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (KSObject object : objects) {
            if (object == null || object.getWorldLocation() == null || !isBankTarget(object.getName(), object.getActions())) {
                continue;
            }
            int distance = distanceTo(playerTile, object.getWorldLocation());
            if (distance < bestDistance) {
                best = object;
                bestDistance = distance;
            }
        }
        return best;
    }

    private KSNPC nearestBankNpc(List<KSNPC> npcs, WorldPoint playerTile) {
        KSNPC best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (KSNPC npc : npcs) {
            if (npc == null || npc.getWorldLocation() == null || !isBankTarget(npc.getName(), npc.getActions())) {
                continue;
            }
            int distance = distanceTo(playerTile, npc.getWorldLocation());
            if (distance < bestDistance) {
                best = npc;
                bestDistance = distance;
            }
        }
        return best;
    }

    private KSObject nearestTreeObject(List<KSObject> objects, WorldPoint playerTile) {
        KSObject best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (KSObject object : objects) {
            if (object == null || object.getWorldLocation() == null || !isTreeTarget(object.getName(), object.getActions())) {
                continue;
            }
            int distance = distanceTo(playerTile, object.getWorldLocation());
            if (distance < bestDistance) {
                best = object;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean isBankTarget(String name, String[] actions) {
        String nameText = name == null ? "" : name.toLowerCase();
        String actionsText = joinActions(actions);
        return nameText.contains("bank") || nameText.contains("banker") || nameText.contains("booth") || nameText.contains("chest")
                || actionsText.contains("bank") || actionsText.contains("collect");
    }

    private boolean isTreeTarget(String name, String[] actions) {
        String nameText = name == null ? "" : name.toLowerCase();
        String actionsText = joinActions(actions);
        return nameText.contains("tree") || nameText.contains("oak") || nameText.contains("willow") || nameText.contains("maple")
                || nameText.contains("yew") || nameText.contains("magic") || nameText.contains("redwood")
                || actionsText.contains("chop");
    }

    private String joinActions(String[] actions) {
        if (actions == null || actions.length == 0) {
            return "";
        }
        List<String> normalized = new ArrayList<>();
        for (String action : actions) {
            if (action != null && !action.isBlank()) {
                normalized.add(action.toLowerCase());
            }
        }
        return String.join(" ", normalized);
    }

    private int distanceTo(WorldPoint a, WorldPoint b) {
        if (a == null || b == null) {
            return Integer.MAX_VALUE;
        }
        return a.distanceTo(b);
    }

    private void copyCaptureLine(String type, String name, int id, WorldPoint point, String[] actions) {
        String actionText = "";
        if (actions != null) {
            List<String> cleaned = new ArrayList<>();
            for (String action : actions) {
                if (action == null || action.isBlank() || "null".equalsIgnoreCase(action)) {
                    continue;
                }
                cleaned.add(action);
            }
            actionText = String.join("|", cleaned);
        }
        String payload = type + "," + safe(name) + "," + id + "," + point.getX() + "," + point.getY() + "," + point.getPlane() + "," + actionText;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(payload), null);
    }

    private Path exportDirectory() {
        Path preferred = Path.of("D:\\Codex GPT\\RSPS\\KSBOT Script building\\research\\mapping-sessions\\reason");
        if (Files.exists(preferred.getParent())) {
            return preferred;
        }
        return Path.of(System.getProperty("user.home"), ".kreme", "servers", "Reason", "mapping-exports");
    }

    private String sanitize(String raw) {
        String fallback = raw == null || raw.isBlank() ? "route-capture" : raw.trim();
        return fallback.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private String safe(String raw) {
        return raw == null ? "Unknown" : raw.replace(",", " ").trim();
    }

    private String format(WorldPoint point) {
        if (point == null) {
            return "unknown";
        }
        return point.getX() + "," + point.getY() + "," + point.getPlane();
    }

    private void log(String message) {
        if (controlPanel != null) {
            controlPanel.appendLog(message);
        }
        System.out.println("[MAPPER] " + message);
    }
}
