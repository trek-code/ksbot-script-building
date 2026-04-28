package route.builder;

import net.runelite.api.coords.WorldPoint;
import rs.kreme.ksbot.api.scripts.Script;
import rs.kreme.ksbot.api.wrappers.KSObject;

import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;
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
        if (panel == null) ensurePanel();

        refreshLiveSnapshot();

        if (recordingPath) {
            autoRecordWaypoint();
        }

        panel.onLiveUpdate(liveSnapshot);
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

        if (ctx.players.getLocal() != null) {
            WorldPoint tile = ctx.players.getLocal().getWorldLocation();
            if (tile != null) {
                snap.playerX     = tile.getX();
                snap.playerY     = tile.getY();
                snap.playerPlane = tile.getPlane();
                snap.playerValid = true;
            }
        }

        KSObject nearest = ctx.groundObjects.query().withinDistance(16).nearestToPlayer();
        if (nearest != null && nearest.getWorldLocation() != null) {
            snap.nearestName    = nearest.getName() != null ? nearest.getName() : "Unknown";
            snap.nearestId      = nearest.getId();
            snap.nearestX       = nearest.getWorldLocation().getX();
            snap.nearestY       = nearest.getWorldLocation().getY();
            snap.nearestPlane   = nearest.getWorldLocation().getPlane();
            snap.nearestValid   = true;
            snap.nearestActions = joinActions(nearest.getActions());
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
        panel.onPathWaypointAdded(session.pathToBank.size(), session.pathToResource.size());
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

    void onStartPathRecording(boolean toBank) {
        recordingToBank = toBank;
        lastPathTile = null;
        recordingPath = true;
        if (toBank) session.pathToBank.clear();
        else        session.pathToResource.clear();
    }

    void onStopPathRecording() {
        recordingPath = false;
    }

    void onClearPath(boolean toBank) {
        if (toBank) session.pathToBank.clear();
        else        session.pathToResource.clear();
        panel.onPathWaypointAdded(session.pathToBank.size(), session.pathToResource.size());
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
            panel.appendLog("Loaded \u2192 " + profileFile);
        } catch (Exception e) {
            panel.appendLog("Failed to load: " + e.getMessage());
        }
    }

    void onSave() {
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

        if (session.filePath == null) {
            Path dir = profilesRoot
                    .resolve(RouteBuilderSession.slugify(session.serverName))
                    .resolve(RouteBuilderSession.slugify(session.botType))
                    .resolve(RouteBuilderSession.slugify(session.routeName));
            try {
                Files.createDirectories(dir);
            } catch (Exception e) {
                panel.appendLog("Could not create directory: " + e.getMessage());
                return;
            }
            session.filePath = dir.resolve("route-profile.json");
        }

        try {
            String json = session.toJson(profilesRoot.toString());
            Files.writeString(session.filePath, json, java.nio.charset.StandardCharsets.UTF_8);
            panel.applySession(session);
            panel.appendLog("Saved \u2192 " + session.filePath);
        } catch (Exception e) {
            panel.appendLog("Save failed: " + e.getMessage());
        }
    }

    /** Export markdown: one file per route + an index.md. */
    void onExport() {
        try {
            Path exportDir = profilesRoot.resolve("_exports");
            Files.createDirectories(exportDir);

            panel.applyToSession(session);

            List<Path> profiles = new java.util.ArrayList<>();
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
        Path root = Path.of(System.getProperty("user.home"), ".kreme", "route-profiles");
        try { Files.createDirectories(root); } catch (Exception ignored) {}
        return root;
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

        boolean pathRecording;
        boolean recordingToBank;
        int     pathWaypointCount;
    }
}
