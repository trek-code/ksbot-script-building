package route.buildercl271;

import net.runelite.api.coords.WorldPoint;
import rs.kreme.ksbot.api.scripts.Script;
import rs.kreme.ksbot.api.wrappers.KSNPC;
import rs.kreme.ksbot.api.wrappers.KSObject;

import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Route Builder — dedicated KSBot script for capturing route data.
 *
 * This script never interacts with the game; it only READS game state
 * (player tile, nearby objects) and publishes that to the UI panel.
 * All actual game control is left to the user.
 */
public class RouteBuilderBot extends Script {

    private static final int POLL_MS = 300;

    private final RouteBuilderSession session = new RouteBuilderSession();

    private volatile LiveSnapshot liveSnapshot = new LiveSnapshot();

    // Path recording state
    private volatile boolean recordingPath   = false;
    private volatile boolean recordingToBank = true;
    private volatile WorldPoint lastPathTile  = null;
    static final int PATH_STEP_DISTANCE = 5;

    private volatile RouteBuilderPanel panel;

    Path profilesRoot;

    // ── Script lifecycle ──────────────────────────────────────────────────────

    @Override
    public boolean onStart() {
        profilesRoot = findProfilesRoot();
        ensurePanel();
        panel.showPanel();
        return true;
    }

    @Override
    public int onProcess() {
        try {
            if (panel == null) ensurePanel();

            refreshLiveSnapshot();

            if (recordingPath) {
                autoRecordWaypoint();
            }

            panel.onLiveUpdate(liveSnapshot);
        } catch (Exception e) {
            if (panel != null) {
                panel.appendLog("Live update failure: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
        return POLL_MS;
    }

    @Override
    public void onStop() {
        if (panel != null) {
            panel.closePanel();
        }
    }

    // ── Live snapshot ─────────────────────────────────────────────────────────

    private void refreshLiveSnapshot() {
        LiveSnapshot snap = new LiveSnapshot();

        try {
            if (ctx.players.getLocal() != null) {
                WorldPoint tile = ctx.players.getLocal().getWorldLocation();
                if (tile != null) {
                    snap.playerX     = tile.getX();
                    snap.playerY     = tile.getY();
                    snap.playerPlane = tile.getPlane();
                    snap.playerValid = true;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            KSObject nearest = nearestCaptureObject(snap.playerValid
                    ? new WorldPoint(snap.playerX, snap.playerY, snap.playerPlane)
                    : null);
            if (nearest != null) {
                WorldPoint nearestTile = nearest.getWorldLocation();
                if (nearestTile != null) {
                    snap.nearestName    = nearest.getName() != null ? nearest.getName() : "Unknown";
                    snap.nearestId      = nearest.getId();
                    snap.nearestX       = nearestTile.getX();
                    snap.nearestY       = nearestTile.getY();
                    snap.nearestPlane   = nearestTile.getPlane();
                    snap.nearestValid   = true;
                    snap.nearestActions = joinActions(nearest.getActions());
                }
            }
        } catch (Exception ignored) {
        }

        try {
            KSObject nearestBank = nearestBankObject(snap.playerValid
                    ? new WorldPoint(snap.playerX, snap.playerY, snap.playerPlane)
                    : null);
            if (nearestBank != null) {
                WorldPoint bankTile = nearestBank.getWorldLocation();
                if (bankTile != null) {
                    snap.nearestBankName = nearestBank.getName() != null ? nearestBank.getName() : "Unknown";
                    snap.nearestBankId = nearestBank.getId();
                    snap.nearestBankX = bankTile.getX();
                    snap.nearestBankY = bankTile.getY();
                    snap.nearestBankPlane = bankTile.getPlane();
                    snap.nearestBankValid = true;
                    snap.nearestBankActions = joinActions(nearestBank.getActions());
                }
            }
        } catch (Exception ignored) {
        }

        try {
            KSNPC nearestNpc = nearestCaptureNpc();
            if (nearestNpc != null) {
                WorldPoint npcTile = nearestNpc.getWorldLocation();
                if (npcTile != null) {
                    snap.nearestNpcName = nearestNpc.getName() != null ? nearestNpc.getName() : "Unknown";
                    snap.nearestNpcId = nearestNpc.getId();
                    snap.nearestNpcX = npcTile.getX();
                    snap.nearestNpcY = npcTile.getY();
                    snap.nearestNpcPlane = npcTile.getPlane();
                    snap.nearestNpcValid = true;
                    snap.nearestNpcActions = joinActions(nearestNpc.getActions());
                }
            }
        } catch (Exception ignored) {
        }

        try {
            KSObject nearestResource = nearestResourceObject(snap.playerValid
                    ? new WorldPoint(snap.playerX, snap.playerY, snap.playerPlane)
                    : null);
            if (nearestResource != null) {
                WorldPoint resourceTile = nearestResource.getWorldLocation();
                if (resourceTile != null) {
                    snap.nearestResourceName = nearestResource.getName() != null ? nearestResource.getName() : "Unknown";
                    snap.nearestResourceId = nearestResource.getId();
                    snap.nearestResourceX = resourceTile.getX();
                    snap.nearestResourceY = resourceTile.getY();
                    snap.nearestResourcePlane = resourceTile.getPlane();
                    snap.nearestResourceValid = true;
                    snap.nearestResourceActions = joinActions(nearestResource.getActions());
                }
            }
        } catch (Exception ignored) {
        }

        snap.pathRecording  = recordingPath;
        snap.recordingToBank = recordingToBank;
        snap.pathWaypointCount = recordingToBank
                ? session.pathToBank.size()
                : session.pathToResource.size();

        liveSnapshot = snap;
    }

    void onRefreshNearest() {
        refreshLiveSnapshot();
        if (panel != null) {
            panel.onLiveUpdate(liveSnapshot);
            panel.appendLog(liveSnapshot.nearestValid
                    ? "Refreshed nearest game object."
                    : "Refreshed nearest game object: no valid object in range.");
        }
    }

    void onRefreshNearestNpc() {
        refreshLiveSnapshot();
        if (panel != null) {
            panel.onLiveUpdate(liveSnapshot);
            panel.appendLog(liveSnapshot.nearestNpcValid
                    ? "Refreshed nearest NPC."
                    : "Refreshed nearest NPC: no valid NPC in range.");
        }
    }

    void onRefreshNearestBank() {
        refreshLiveSnapshot();
        if (panel != null) {
            panel.onLiveUpdate(liveSnapshot);
            panel.appendLog(liveSnapshot.nearestBankValid
                    ? "Refreshed nearest bank target."
                    : "Refreshed nearest bank target: no valid bank object in range.");
        }
    }

    void onRefreshNearestResource() {
        refreshLiveSnapshot();
        if (panel != null) {
            panel.onLiveUpdate(liveSnapshot);
            panel.appendLog(liveSnapshot.nearestResourceValid
                    ? "Refreshed nearest resource target."
                    : "Refreshed nearest resource target: no valid resource object in range.");
        }
    }

    private void autoRecordWaypoint() {
        if (!liveSnapshot.playerValid) return;
        WorldPoint current = new WorldPoint(liveSnapshot.playerX, liveSnapshot.playerY, liveSnapshot.playerPlane);

        if (lastPathTile != null && current.distanceTo(lastPathTile) < PATH_STEP_DISTANCE) return;

        if (recordingToBank) {
            session.pathToBank.add(current);
        } else {
            session.pathToResource.add(current);
        }
        lastPathTile = current;
        panel.onPathWaypointAdded(
                List.copyOf(session.pathToBank),
                List.copyOf(session.pathToResource),
                recordingPath,
                recordingToBank);
    }

    // ── Panel callbacks ───────────────────────────────────────────────────────

    /** Snapshot the player's current tile into a new table row tagged "waypoint". */
    void onAddMyTileRow() {
        LiveSnapshot snap = liveSnapshot;
        if (!snap.playerValid) {
            panel.appendLog("Player tile not available.");
            return;
        }
        panel.addRowFromMyTile(snap.playerX, snap.playerY, snap.playerPlane);
    }

    /** Snapshot the nearest interactable object into a new table row. */
    void onAddNearestRow() {
        LiveSnapshot snap = liveSnapshot;
        if (!snap.nearestValid) {
            panel.appendLog("No nearby object in range.");
            return;
        }
        panel.addRowFromNearest(
                snap.nearestName, snap.nearestId,
                snap.nearestX, snap.nearestY, snap.nearestPlane,
                snap.nearestActions);
    }

    void onAddNearestNpcRow() {
        LiveSnapshot snap = liveSnapshot;
        if (!snap.nearestNpcValid) {
            panel.appendLog("No nearby NPC in range.");
            return;
        }
        panel.addRowFromNearestNpc(
                snap.nearestNpcName, snap.nearestNpcId,
                snap.nearestNpcX, snap.nearestNpcY, snap.nearestNpcPlane,
                snap.nearestNpcActions);
    }

    void onAddNearestBankRow() {
        LiveSnapshot snap = liveSnapshot;
        if (!snap.nearestBankValid) {
            panel.appendLog("No nearby bank target in range.");
            return;
        }
        panel.addRowFromNearestBank(
                snap.nearestBankName, snap.nearestBankId,
                snap.nearestBankX, snap.nearestBankY, snap.nearestBankPlane,
                snap.nearestBankActions);
    }

    void onAddNearestResourceRow() {
        LiveSnapshot snap = liveSnapshot;
        if (!snap.nearestResourceValid) {
            panel.appendLog("No nearby resource target in range.");
            return;
        }
        panel.addRowFromNearestResource(
                snap.nearestResourceName, snap.nearestResourceId,
                snap.nearestResourceX, snap.nearestResourceY, snap.nearestResourcePlane,
                snap.nearestResourceActions);
    }

    void onStartPathRecording(boolean toBank) {
        recordingToBank = toBank;
        lastPathTile = null;
        recordingPath = true;
        if (toBank) session.pathToBank.clear();
        else        session.pathToResource.clear();
        panel.onPathWaypointAdded(
                List.copyOf(session.pathToBank),
                List.copyOf(session.pathToResource),
                recordingPath,
                recordingToBank);
    }

    void onStopPathRecording() {
        recordingPath = false;
        panel.onPathWaypointAdded(
                List.copyOf(session.pathToBank),
                List.copyOf(session.pathToResource),
                recordingPath,
                recordingToBank);
    }

    void onClearPath(boolean toBank) {
        if (toBank) session.pathToBank.clear();
        else        session.pathToResource.clear();
        panel.onPathWaypointAdded(
                List.copyOf(session.pathToBank),
                List.copyOf(session.pathToResource),
                recordingPath,
                recordingToBank);
    }

    /** Clear the editor to start a brand-new route. */
    void onNewRoute() {
        session.clear();
        panel.applySession(session);
        panel.appendLog("New route: fill in Server / Bot Type / Route Name, then click Save.");
    }

    void onLoadRoute(Path profileFile) {
        try {
            String json = Files.readString(profileFile);
            RouteBuilderSession.loadInto(session, json);
            session.filePath = profileFile;
            panel.applySession(session);
            panel.refreshRecentRouteInfo();
            panel.appendLog("Loaded \u2192 " + profileFile);
        } catch (Exception e) {
            panel.appendLog("Failed to load: " + e.getMessage());
        }
    }

    void onSave(Path targetFile) {
        panel.applyToSession(session);

        if (session.routeName == null || session.routeName.isBlank()) {
            panel.appendLog("Cannot save: Route Name is required (top bar).");
            return;
        }
        if (session.serverName == null || session.serverName.isBlank()) {
            panel.appendLog("Cannot save: Server is required (top bar).");
            return;
        }
        if (session.botType == null || session.botType.isBlank()) {
            panel.appendLog("Cannot save: Bot Type is required (top bar).");
            return;
        }

        if (targetFile != null) {
            session.filePath = normalizeRouteFile(targetFile);
        } else if (session.filePath == null) {
            session.filePath = defaultRouteFile();
        }

        try {
            Files.createDirectories(session.filePath.getParent());
            String json = session.toJson(profilesRoot.toString());
            Files.writeString(session.filePath, json, java.nio.charset.StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            panel.applySession(session);
            panel.refreshRecentRouteInfo();
            panel.appendLog("Saved \u2192 " + session.filePath);
        } catch (Exception e) {
            panel.appendLog("Save failed: " + e.getMessage());
        }
    }

    void onForceExport(Path targetFile) {
        panel.applyToSession(session);

        if (targetFile == null) {
            panel.appendLog("Export cancelled.");
            return;
        }

        Path exportFile = normalizeJsonFile(targetFile);

        try {
            Files.createDirectories(exportFile.getParent());
            String json = session.toJson(profilesRoot.toString());
            Files.writeString(exportFile, json, java.nio.charset.StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            panel.appendLog("Force exported current route \u2192 " + exportFile);
        } catch (Exception e) {
            panel.appendLog("Export failed: " + e.getMessage());
        }
    }

    void onExportAllMarkdown() {
        try {
            Path exportDir = profilesRoot.resolve("_exports");
            Files.createDirectories(exportDir);

            List<Path> profiles = new ArrayList<>();
            Files.walk(profilesRoot, 4)
                    .filter(p -> p.getFileName().toString().equals("route-profile.json"))
                    .forEach(profiles::add);

            StringBuilder index = new StringBuilder();
            index.append("# RSPS Route Index\n\n");
            index.append("_Generated by Route Builder. One entry per `route-profile.json` under `")
                    .append(profilesRoot).append("`._\n\n");
            index.append("| Route | Server | Bot Type | Level | File |\n");
            index.append("|-------|--------|----------|-------|------|\n");

            int count = 0;
            for (Path p : profiles) {
                RouteBuilderSession tmp = new RouteBuilderSession();
                try {
                    String json = Files.readString(p);
                    RouteBuilderSession.loadInto(tmp, json);
                    tmp.filePath = p;
                } catch (Exception ex) {
                    panel.appendLog("Skip " + p + ": " + ex.getMessage());
                    continue;
                }

                String slug = RouteBuilderSession.slugify(tmp.serverName) + "_"
                        + RouteBuilderSession.slugify(tmp.botType) + "_"
                        + RouteBuilderSession.slugify(tmp.routeName);
                Path mdFile = exportDir.resolve(slug + ".md");
                Files.writeString(mdFile, renderRouteMarkdown(tmp),
                        java.nio.charset.StandardCharsets.UTF_8);

                index.append("| ").append(safe(tmp.routeName))
                        .append(" | ").append(safe(tmp.serverName))
                        .append(" | ").append(safe(tmp.botType))
                        .append(" | ").append(tmp.levelRequired > 0 ? tmp.levelRequired : "-")
                        .append(" | [`").append(slug).append(".md`](./").append(slug).append(".md) |\n");
                count++;
            }

            Path indexFile = exportDir.resolve("index.md");
            Files.writeString(indexFile, index.toString(),
                    java.nio.charset.StandardCharsets.UTF_8);
            panel.appendLog("Exported " + count + " route(s) \u2192 " + exportDir);
        } catch (Exception e) {
            panel.appendLog("Export failed: " + e.getMessage());
        }
    }

    private String renderRouteMarkdown(RouteBuilderSession s) {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(safe(s.routeName)).append("\n\n");
        md.append("- **Server:** ").append(safe(s.serverName)).append("\n");
        md.append("- **Bot Type:** ").append(safe(s.botType)).append("\n");
        if (s.levelRequired > 0) md.append("- **Min Level:** ").append(s.levelRequired).append("\n");
        if (s.filePath != null)  md.append("- **Profile File:** `").append(s.filePath).append("`\n");
        md.append("\n");

        md.append("## Resource Targets\n\n");
        if (s.resourceTiles.isEmpty()) md.append("_None recorded._\n\n");
        else {
            md.append("| # | Name | ID | X | Y | Plane | Actions |\n");
            md.append("|---|------|----|---|---|-------|---------|\n");
            for (int i = 0; i < s.resourceTiles.size(); i++) {
                RouteBuilderSession.CaptureTile t = s.resourceTiles.get(i);
                md.append("| ").append(i + 1)
                  .append(" | ").append(safe(t.name))
                  .append(" | ").append(t.id)
                  .append(" | ").append(t.x)
                  .append(" | ").append(t.y)
                  .append(" | ").append(t.plane)
                  .append(" | ").append(safe(t.actions)).append(" |\n");
            }
            md.append("\n");
        }

        md.append("## Bank\n\n");
        if (s.bankTile != null) {
            RouteBuilderSession.CaptureTile t = s.bankTile;
            md.append("- **Booth:** ").append(safe(t.name))
              .append(" (id ").append(t.id).append(")")
              .append(" @ ").append(t.x).append(", ").append(t.y).append(", ").append(t.plane)
              .append(" \u2014 actions: ").append(safe(t.actions)).append("\n");
        } else md.append("- **Booth:** _not set_\n");

        if (s.bankStandTile != null) {
            RouteBuilderSession.CaptureTile t = s.bankStandTile;
            md.append("- **Stand Tile:** ").append(t.x).append(", ").append(t.y).append(", ").append(t.plane).append("\n");
        } else md.append("- **Stand Tile:** _not set_\n");

        if (!s.bankInsideTiles.isEmpty()) {
            md.append("- **Inside Tiles:**\n");
            for (int i = 0; i < s.bankInsideTiles.size(); i++) {
                RouteBuilderSession.CaptureTile t = s.bankInsideTiles.get(i);
                md.append("  ").append(i + 1).append(". ").append(t.x).append(", ").append(t.y).append(", ").append(t.plane).append("\n");
            }
        }
        md.append("\n");

        md.append("## Return Anchor\n\n");
        if (s.returnAnchor != null) {
            RouteBuilderSession.CaptureTile t = s.returnAnchor;
            md.append("- ").append(safe(t.name)).append(" (id ").append(t.id).append(") @ ")
              .append(t.x).append(", ").append(t.y).append(", ").append(t.plane).append("\n\n");
        } else md.append("_Not set._\n\n");

        md.append("## Path Chains\n\n");
        md.append("- **To Bank:** ").append(s.pathToBank.size()).append(" waypoints\n");
        md.append("- **To Resource:** ").append(s.pathToResource.size()).append(" waypoints\n\n");

        if (!s.pathToBank.isEmpty()) {
            md.append("### Waypoints \u2192 Bank\n\n");
            for (int i = 0; i < s.pathToBank.size(); i++) {
                WorldPoint w = s.pathToBank.get(i);
                md.append(i + 1).append(". ").append(w.getX()).append(", ").append(w.getY()).append(", ").append(w.getPlane()).append("\n");
            }
            md.append("\n");
        }
        if (!s.pathToResource.isEmpty()) {
            md.append("### Waypoints \u2192 Resource\n\n");
            for (int i = 0; i < s.pathToResource.size(); i++) {
                WorldPoint w = s.pathToResource.get(i);
                md.append(i + 1).append(". ").append(w.getX()).append(", ").append(w.getY()).append(", ").append(w.getPlane()).append("\n");
            }
            md.append("\n");
        }
        return md.toString();
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|").replace("\n", " ");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void ensurePanel() {
        if (panel != null) return;

        RouteBuilderBot self = this;
        Runnable create = () -> {
            panel = new RouteBuilderPanel(self);
            panel.setProfilesRoot(profilesRoot);
        };

        if (SwingUtilities.isEventDispatchThread()) {
            create.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(create);
            } catch (Exception e) {
                throw new IllegalStateException("Could not create Route Builder panel", e);
            }
        }
    }

    private Path findProfilesRoot() {
        List<Path> candidates = List.of(
                Paths.get("D:\\Code Agents\\Codex GPT\\RSPS\\KSBOT Script building\\research\\route-profiles"),
                Paths.get("D:\\Codex GPT\\RSPS\\KSBOT Script building\\research\\route-profiles"),
                Path.of(System.getProperty("user.home"), ".kreme", "route-profiles")
        );
        Path root = candidates.get(candidates.size() - 1);
        for (Path candidate : candidates) {
            if (Files.exists(candidate.getParent() != null ? candidate.getParent() : candidate) || candidate.equals(root)) {
                root = candidate;
                break;
            }
        }
        try { Files.createDirectories(root); } catch (Exception ignored) {}
        return root;
    }

    private Path defaultRouteFile() {
        return profilesRoot
                .resolve(RouteBuilderSession.slugify(session.serverName))
                .resolve(RouteBuilderSession.slugify(session.botType))
                .resolve(RouteBuilderSession.slugify(session.routeName))
                .resolve("route-profile.json");
    }

    private Path normalizeRouteFile(Path input) {
        Path normalized = normalizeJsonFile(input);
        if (normalized.getFileName() == null || !normalized.getFileName().toString().equalsIgnoreCase("route-profile.json")) {
            if (Files.isDirectory(normalized) || !normalized.toString().toLowerCase().endsWith(".json")) {
                return normalized.resolve("route-profile.json");
            }
        }
        return normalized;
    }

    private Path normalizeJsonFile(Path input) {
        String raw = input.toString();
        if (!raw.toLowerCase().endsWith(".json")) {
            return input.resolveSibling(input.getFileName().toString() + ".json");
        }
        return input;
    }

    private KSObject nearestCaptureObject(WorldPoint playerTile) {
        if (playerTile == null) {
            return null;
        }

        try {
            KSObject direct = ctx.groundObjects.query().withinDistance(16).nearestToPlayer();
            if (isUsableCaptureObject(direct)) {
                return direct;
            }
        } catch (Exception ignored) {
        }

        try {
            KSObject broader = ctx.groundObjects.query().withinDistance(24).nearestToPlayer();
            if (isUsableCaptureObject(broader)) {
                return broader;
            }
        } catch (Exception ignored) {
        }

        try {
            KSObject named = ctx.groundObjects.getClosest(
                    "Bank booth", "Bank chest", "Tree", "Oak tree", "Willow tree",
                    "Maple tree", "Yew", "Yew tree", "Magic tree", "Teak", "Mahogany");
            if (isUsableCaptureObject(named) && objectInRange(named, playerTile, 32)) {
                return named;
            }
        } catch (Exception ignored) {
        }

        KSObject nearest = chooseBestCaptureObject(safeObjectList(12), playerTile);
        if (nearest == null) {
            nearest = chooseBestCaptureObject(safeObjectList(24), playerTile);
        }
        if (nearest == null) {
            nearest = chooseBestCaptureObject(safeObjectList(40), playerTile);
        }
        return nearest;
    }

    private KSNPC nearestCaptureNpc() {
        try {
            KSNPC direct = ctx.npcs.query().withinDistance(16).closest();
            if (isUsableCaptureNpc(direct)) {
                return direct;
            }
        } catch (Exception ignored) {
        }

        try {
            List<KSNPC> npcs = ctx.npcs.query().withinDistance(16).list();
            KSNPC best = null;
            int bestScore = Integer.MAX_VALUE;
            for (KSNPC npc : npcs) {
                if (!isUsableCaptureNpc(npc)) continue;
                int score = npcCapturePriority(npc);
                if (best == null || score < bestScore) {
                    best = npc;
                    bestScore = score;
                }
            }
            return best;
        } catch (Exception ignored) {
            return null;
        }
    }

    private KSObject nearestBankObject(WorldPoint playerTile) {
        if (playerTile == null) {
            return null;
        }

        try {
            KSObject direct = ctx.groundObjects.query()
                    .withOption("Bank", "Use", "Open", "Collect")
                    .withinDistance(24)
                    .nearestToPlayer();
            if (isUsableBankObject(direct)) {
                return direct;
            }
        } catch (Exception ignored) {
        }

        try {
            List<KSObject> objects = ctx.groundObjects.query()
                    .withOption("Bank", "Use", "Open", "Collect")
                    .withinDistance(40)
                    .list();
            KSObject best = null;
            int bestDistance = Integer.MAX_VALUE;
            for (KSObject object : objects) {
                if (!isUsableBankObject(object)) continue;
                WorldPoint tile = object.getWorldLocation();
                if (tile == null) continue;
                int distance = playerTile.distanceTo(tile);
                if (best == null || distance < bestDistance) {
                    best = object;
                    bestDistance = distance;
                }
            }
            if (best != null) {
                return best;
            }
        } catch (Exception ignored) {
        }

        try {
            KSObject named = ctx.groundObjects.getClosest("Bank booth", "Bank chest", "Bank Chest", "Deposit box");
            if (isUsableBankObject(named) && objectInRange(named, playerTile, 40)) {
                return named;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private KSObject chooseBestCaptureObject(List<KSObject> objects, WorldPoint playerTile) {
        KSObject best = null;
        int bestPriority = Integer.MAX_VALUE;
        int bestDistance = Integer.MAX_VALUE;

        for (KSObject object : objects) {
            try {
                if (object == null) {
                    continue;
                }
                WorldPoint tile = object.getWorldLocation();
                if (tile == null) {
                    continue;
                }
                int priority = capturePriority(object);
                int distance = playerTile.distanceTo(tile);
                if (best == null || priority < bestPriority || (priority == bestPriority && distance < bestDistance)) {
                    best = object;
                    bestPriority = priority;
                    bestDistance = distance;
                }
            } catch (Exception ignored) {
            }
        }
        return best;
    }

    private List<KSObject> safeObjectList(int distance) {
        try {
            return ctx.groundObjects.query().withinDistance(distance).list();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private int capturePriority(KSObject object) {
        int score = 0;
        String name = object.getName();
        if (name == null || name.isBlank() || "null".equalsIgnoreCase(name)) {
            score += 1000;
        }
        if (joinActions(object.getActions()).isBlank()) {
            score += 100;
        }
        return score;
    }

    private boolean isUsableCaptureObject(KSObject object) {
        if (object == null) {
            return false;
        }
        try {
            WorldPoint tile = object.getWorldLocation();
            return tile != null && tile.getX() > 0 && tile.getY() > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean objectInRange(KSObject object, WorldPoint playerTile, int maxDistance) {
        if (object == null || playerTile == null) {
            return false;
        }
        try {
            WorldPoint tile = object.getWorldLocation();
            return tile != null && playerTile.distanceTo(tile) <= maxDistance;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isUsableBankObject(KSObject object) {
        if (!isUsableCaptureObject(object)) {
            return false;
        }
        try {
            if (object.hasAction("Bank") || object.hasAction("Use") || object.hasAction("Open") || object.hasAction("Collect")) {
                return true;
            }
            String name = object.getName();
            if (name == null) {
                return false;
            }
            String lower = name.toLowerCase();
            return lower.contains("bank") || lower.contains("deposit");
        } catch (Exception ignored) {
            return false;
        }
    }

    private static final String[] RESOURCE_ACTIONS = {
            "Chop down", "Chop", "Mine", "Fish", "Net", "Bait", "Cage", "Harpoon", "Lure",
            "Pick", "Pick-lock", "Harvest", "Cut", "Catch", "Steal-from", "Gather",
            "Chop-down", "Chop-Down"
    };

    /** Public list of resource filter categories (what the dropdown shows). */
    public static final String[] RESOURCE_FILTER_OPTIONS = {
            "All", "Tree", "Ore / Rocks", "Fishing spot", "Thieving", "Farming", "Hunter", "Chest / Door"
    };

    /** Default filter = "All". Updated from the panel via {@link #setResourceFilter(String)}. */
    private volatile String resourceFilter = "All";

    public void setResourceFilter(String filter) {
        this.resourceFilter = (filter == null || filter.isBlank()) ? "All" : filter;
        // Trigger an immediate refresh so the live labels update without waiting for the next poll.
        onRefreshNearestResource();
    }

    public String getResourceFilter() {
        return resourceFilter;
    }

    private String[] actionsForFilter() {
        switch (resourceFilter) {
            case "Tree":          return new String[]{"Chop down", "Chop-down", "Chop-Down", "Chop", "Cut"};
            case "Ore / Rocks":   return new String[]{"Mine"};
            case "Fishing spot":  return new String[]{"Fish", "Net", "Bait", "Cage", "Harpoon", "Lure"};
            case "Thieving":      return new String[]{"Steal-from", "Pick-lock", "Pickpocket"};
            case "Farming":       return new String[]{"Harvest", "Pick", "Gather"};
            case "Hunter":        return new String[]{"Catch", "Check-trap", "Dismantle", "Lay", "Investigate"};
            case "Chest / Door":  return new String[]{"Open", "Search", "Pick-lock", "Unlock"};
            default:              return RESOURCE_ACTIONS;
        }
    }

    private String[] nameKeywordsForFilter() {
        switch (resourceFilter) {
            case "Tree":          return new String[]{"tree"};
            case "Ore / Rocks":   return new String[]{"rocks", "ore", "ore vein", "vein"};
            case "Fishing spot":  return new String[]{"fishing spot"};
            case "Chest / Door":  return new String[]{"chest", "door", "gate"};
            default:              return new String[0];
        }
    }

    private KSObject nearestResourceObject(WorldPoint playerTile) {
        if (playerTile == null) {
            return null;
        }

        String[] actions = actionsForFilter();

        try {
            KSObject direct = ctx.groundObjects.query()
                    .withOption(actions)
                    .withinDistance(24)
                    .nearestToPlayer();
            if (isUsableResourceObject(direct)) {
                return direct;
            }
        } catch (Exception ignored) {
        }

        try {
            List<KSObject> objects = ctx.groundObjects.query()
                    .withOption(actions)
                    .withinDistance(40)
                    .list();
            KSObject best = null;
            int bestDistance = Integer.MAX_VALUE;
            for (KSObject object : objects) {
                if (!isUsableResourceObject(object)) continue;
                WorldPoint tile = object.getWorldLocation();
                if (tile == null) continue;
                int distance = playerTile.distanceTo(tile);
                if (best == null || distance < bestDistance) {
                    best = object;
                    bestDistance = distance;
                }
            }
            if (best != null) {
                return best;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private boolean isUsableResourceObject(KSObject object) {
        if (!isUsableCaptureObject(object)) {
            return false;
        }
        try {
            // Action match against filtered subset.
            for (String action : actionsForFilter()) {
                if (object.hasAction(action)) {
                    return true;
                }
            }
            // Name-keyword fallback so "Tree" still matches a generic "Tree" whose
            // right-click action is exotic (e.g., "Inspect") on some servers.
            String[] keywords = nameKeywordsForFilter();
            if (keywords.length > 0) {
                String name = object.getName();
                if (name != null) {
                    String lower = name.toLowerCase();
                    for (String kw : keywords) {
                        if (lower.contains(kw)) return true;
                    }
                }
            }
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private int npcCapturePriority(KSNPC npc) {
        int score = 0;
        String name = npc.getName();
        if (name == null || name.isBlank() || "null".equalsIgnoreCase(name)) {
            score += 1000;
        }
        if (joinActions(npc.getActions()).isBlank()) {
            score += 100;
        }
        return score;
    }

    private boolean isUsableCaptureNpc(KSNPC npc) {
        if (npc == null) {
            return false;
        }
        try {
            return npc.getWorldLocation() != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String joinActions(String[] actions) {
        if (actions == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String a : actions) {
            if (a != null && !a.isBlank() && !"null".equalsIgnoreCase(a)) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(a.trim());
            }
        }
        return sb.toString();
    }

    // ── Live data snapshot ────────────────────────────────────────────────────

    static final class LiveSnapshot {
        boolean playerValid;
        int     playerX, playerY, playerPlane;

        boolean nearestValid;
        String  nearestName    = "";
        int     nearestId;
        int     nearestX, nearestY, nearestPlane;
        String  nearestActions = "";

        boolean nearestBankValid;
        String  nearestBankName = "";
        int     nearestBankId;
        int     nearestBankX, nearestBankY, nearestBankPlane;
        String  nearestBankActions = "";

        boolean nearestNpcValid;
        String  nearestNpcName = "";
        int     nearestNpcId;
        int     nearestNpcX, nearestNpcY, nearestNpcPlane;
        String  nearestNpcActions = "";

        boolean nearestResourceValid;
        String  nearestResourceName = "";
        int     nearestResourceId;
        int     nearestResourceX, nearestResourceY, nearestResourcePlane;
        String  nearestResourceActions = "";

        boolean pathRecording;
        boolean recordingToBank;
        int     pathWaypointCount;
    }
}
