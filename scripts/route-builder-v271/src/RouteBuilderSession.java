package route.buildercl271;

import net.runelite.api.coords.WorldPoint;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * In-memory representation of a route being built.
 * Serialises itself to route-profile.json via toJson().
 */
public final class RouteBuilderSession {

    // ── Category / identity ───────────────────────────────────────────────────
    public String serverName     = "Reason";
    public String botType        = "Woodcutting";
    public String routeName      = "";
    public int    levelRequired  = 0;
    public String status         = "draft";

    // ── Captured tiles ────────────────────────────────────────────────────────
    public final List<CaptureTile> resourceTiles   = new ArrayList<>();
    public CaptureTile             bankTile         = null;
    public final List<CaptureTile> bankInsideTiles  = new ArrayList<>();
    public CaptureTile             bankStandTile    = null;
    public CaptureTile             returnAnchor     = null;

    // ── Path chains (waypoints for walking between areas) ─────────────────────
    public final List<WorldPoint> pathToBank        = new ArrayList<>();
    public final List<WorldPoint> pathToResource    = new ArrayList<>();

    // ── File path (set after first save) ──────────────────────────────────────
    public java.nio.file.Path filePath = null;

    // ── Validation ────────────────────────────────────────────────────────────

    public boolean isComplete() {
        return !routeName.isBlank()
                && !resourceTiles.isEmpty()
                && resourceTiles.stream().allMatch(t -> t.id > 0)
                && bankTile != null && bankTile.id > 0;
    }

    public List<String> missingFields() {
        List<String> missing = new ArrayList<>();
        if (routeName.isBlank())       missing.add("Route Name");
        if (resourceTiles.isEmpty())   missing.add("Resource tiles (none captured)");
        else if (resourceTiles.stream().anyMatch(t -> t.id <= 0))
            missing.add("Resource tile ID(s) — some are missing");
        if (bankTile == null)          missing.add("Bank tile");
        else if (bankTile.id <= 0)     missing.add("Bank tile ID");
        if (bankStandTile == null)     missing.add("Bank stand tile (where player stands)");
        return missing;
    }

    public void clear() {
        routeName = "";
        levelRequired = 0;
        status = "draft";
        resourceTiles.clear();
        bankTile = null;
        bankInsideTiles.clear();
        bankStandTile = null;
        returnAnchor = null;
        pathToBank.clear();
        pathToResource.clear();
        filePath = null;
    }

    // ── JSON serialisation ────────────────────────────────────────────────────

    public String toJson(String profilesRootPath) {
        String serverSlug = slugify(serverName);
        String botSlug    = slugify(botType);
        String routeSlug  = slugify(routeName);
        String today      = LocalDate.now().toString();
        String routeRoot  = profilesRootPath + "\\" + serverSlug + "\\" + botSlug + "\\" + routeSlug;

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"meta\": {\n");
        jf(sb, "serverName",    serverName,      4, true);
        jf(sb, "serverSlug",    serverSlug,      4, true);
        jf(sb, "botType",       botType,         4, true);
        jf(sb, "botTypeSlug",   botSlug,         4, true);
        jf(sb, "routeName",     routeName,       4, true);
        jf(sb, "routeSlug",     routeSlug,       4, true);
        jf(sb, "activityType",  botType.toLowerCase(), 4, true);
        jf(sb, "resourceType",  guessResourceType(), 4, true);
        jf(sb, "createdDate",   today,           4, true);
        jf(sb, "status",        status,          4, false);
        sb.append("  },\n");

        sb.append("  \"requirements\": {\n");
        jf(sb, "levelRequirement", levelRequired > 0 ? String.valueOf(levelRequired) : "", 4, true);
        sb.append("    \"notes\": []\n");
        sb.append("  },\n");

        sb.append("  \"routeAnchors\": {\n");
        sb.append("    \"teleportDestination\": [],\n");
        tileArray(sb, "returnTargetAnchor",
                returnAnchor != null ? List.of(returnAnchor)
                        : (resourceTiles.isEmpty() ? List.of() : List.of(resourceTiles.get(0))), true);
        sb.append("    \"bankDoorTiles\": [],\n");
        tileArray(sb, "bankInsideTiles", bankInsideTiles, true);
        tileArray(sb, "bankStandTile",
                bankStandTile != null ? List.of(bankStandTile) : List.of(), true);
        tileArray(sb, "bankObjectTile",
                bankTile != null ? List.of(bankTile) : List.of(), true);
        sb.append("    \"safeStandTiles\": []\n");
        sb.append("  },\n");

        // Resource targets
        sb.append("  \"resourceTargets\": {\n");
        List<String>  uniqueNames = resourceTiles.stream().map(t -> t.name).distinct().collect(Collectors.toList());
        List<Integer> uniqueIds   = resourceTiles.stream().map(t -> t.id).filter(i -> i > 0).distinct().collect(Collectors.toList());
        strArr(sb, "names", uniqueNames, true);
        intArr(sb, "objectIds", uniqueIds, true);
        sb.append("    \"npcIds\": [],\n");
        sb.append("    \"groundItemIds\": [],\n");
        tileArray(sb, "tiles", resourceTiles, false);
        sb.append("  },\n");

        // Bank targets
        sb.append("  \"bankTargets\": {\n");
        List<CaptureTile> bankList = bankTile != null ? List.of(bankTile) : List.of();
        List<Integer> bankIds = bankList.stream().map(t -> t.id).filter(i -> i > 0).distinct().collect(Collectors.toList());
        List<String> bankActions = bankList.stream()
                .flatMap(t -> t.actionList().stream()).distinct().collect(Collectors.toList());
        jf(sb, "type", "booth", 4, true);
        intArr(sb, "objectIds", bankIds, true);
        sb.append("    \"npcIds\": [],\n");
        strArr(sb, "actions", bankActions.isEmpty() ? List.of("Bank") : bankActions, true);
        tileArray(sb, "tiles", bankList, false);
        sb.append("  },\n");

        // Path chains
        sb.append("  \"pathChains\": {\n");
        waypointArray(sb, "toBank",     pathToBank,     true);
        waypointArray(sb, "toResource", pathToResource, true);
        sb.append("    \"alternatePaths\": []\n");
        sb.append("  },\n");

        // Boilerplate sections
        rawObj(sb, "interactionTargets",
                "    \"doors\": [],\n    \"gates\": [],\n    \"ladders\": [],\n    \"stairs\": [],\n    \"portals\": [],\n    \"obstacles\": []\n");
        rawObj(sb, "walkerHints",
                "    \"preferredScanRadius\": 16,\n    \"avoidTiles\": [],\n    \"blockedTileFlags\": [],\n    \"regionNotes\": [],\n    \"customServerWarnings\": []\n");
        rawObj(sb, "loot",
                "    \"defaultEnabled\": false,\n    \"suggestedItems\": [],\n    \"pickupStyle\": \"immediate\"\n");
        rawObj(sb, "sourceData",
                "    \"importedMapperExports\": [],\n    \"manualCaptureRows\": []\n");
        rawObj(sb, "testStatus",
                "    \"choppingOrInteraction\": \"\",\n    \"banking\": \"\",\n    \"powercutting\": \"\",\n    \"looting\": \"\",\n    \"knownIssues\": [],\n    \"nextFixes\": []\n");

        sb.append("  \"paths\": {\n");
        jf(sb, "routeRoot",      routeRoot,                   4, true);
        jf(sb, "capturesDir",    routeRoot + "\\captures",    4, true);
        jf(sb, "exportsDir",     routeRoot + "\\exports",     4, true);
        jf(sb, "screenshotsDir", routeRoot + "\\screenshots", 4, false);
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    private static void jf(StringBuilder sb, String key, String val, int indent, boolean comma) {
        spaces(sb, indent);
        sb.append("\"").append(key).append("\": \"").append(esc(val)).append("\"");
        if (comma) sb.append(",");
        sb.append("\n");
    }

    private static void rawObj(StringBuilder sb, String key, String body) {
        sb.append("  \"").append(key).append("\": {\n").append(body).append("  },\n");
    }

    private static void tileArray(StringBuilder sb, String key, List<CaptureTile> tiles, boolean comma) {
        spaces(sb, 4); sb.append("\"").append(key).append("\": [");
        if (!tiles.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < tiles.size(); i++) {
                CaptureTile t = tiles.get(i);
                sb.append("      {\n");
                jf(sb, "label",   t.name.isBlank() ? key + " " + (i + 1) : t.name, 8, true);
                sb.append("        \"x\": ").append(t.x).append(",\n");
                sb.append("        \"y\": ").append(t.y).append(",\n");
                sb.append("        \"plane\": ").append(t.plane).append(",\n");
                sb.append("        \"id\": ").append(t.id).append(",\n");
                strArr(sb, "actions", t.actionList(), true);
                jf(sb, "notes", t.notes, 8, false);
                sb.append("      }");
                if (i < tiles.size() - 1) sb.append(",");
                sb.append("\n");
            }
            spaces(sb, 4);
        }
        sb.append("]");
        if (comma) sb.append(",");
        sb.append("\n");
    }

    private static void waypointArray(StringBuilder sb, String key, List<WorldPoint> points, boolean comma) {
        spaces(sb, 4); sb.append("\"").append(key).append("\": [");
        if (!points.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < points.size(); i++) {
                WorldPoint p = points.get(i);
                sb.append("      {\"x\": ").append(p.getX())
                  .append(", \"y\": ").append(p.getY())
                  .append(", \"plane\": ").append(p.getPlane()).append("}");
                if (i < points.size() - 1) sb.append(",");
                sb.append("\n");
            }
            spaces(sb, 4);
        }
        sb.append("]");
        if (comma) sb.append(",");
        sb.append("\n");
    }

    private static void strArr(StringBuilder sb, String key, List<String> vals, boolean comma) {
        spaces(sb, 4); sb.append("\"").append(key).append("\": [");
        if (!vals.isEmpty()) {
            for (int i = 0; i < vals.size(); i++) {
                sb.append("\"").append(esc(vals.get(i))).append("\"");
                if (i < vals.size() - 1) sb.append(", ");
            }
        }
        sb.append("]");
        if (comma) sb.append(",");
        sb.append("\n");
    }

    private static void intArr(StringBuilder sb, String key, List<Integer> vals, boolean comma) {
        spaces(sb, 4); sb.append("\"").append(key).append("\": [");
        for (int i = 0; i < vals.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(vals.get(i));
        }
        sb.append("]");
        if (comma) sb.append(",");
        sb.append("\n");
    }

    private static void spaces(StringBuilder sb, int n) {
        for (int i = 0; i < n; i++) sb.append(' ');
    }

    private static String esc(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String guessResourceType() {
        for (CaptureTile t : resourceTiles) {
            String lower = t.name.toLowerCase();
            if (lower.contains("magic")) return "Magic Tree";
            if (lower.contains("yew"))   return "Yew Tree";
            if (lower.contains("maple")) return "Maple Tree";
            if (lower.contains("willow"))return "Willow Tree";
            if (lower.contains("oak"))   return "Oak Tree";
            if (lower.contains("tree"))  return "Tree";
            if (!t.name.isBlank())       return t.name;
        }
        return botType + " Target";
    }

    // ── JSON deserialisation (minimal hand-rolled parser — no external lib) ──

    /**
     * Parse a route-profile.json string and populate the given session.
     * Uses simple regex/string scanning so we don't need javax.json or Gson.
     */
    public static void loadInto(RouteBuilderSession s, String json) {
        s.clear();

        // ── Meta ─────────────────────────────────────────────────────────────
        s.serverName    = jStr(json, "serverName",  s.serverName);
        s.botType       = jStr(json, "botType",     s.botType);
        s.routeName     = jStr(json, "routeName",   s.routeName);
        s.status        = jStr(json, "status",      s.status);
        String lvl      = jStr(json, "levelRequirement", "");
        if (!lvl.isBlank()) {
            try { s.levelRequired = Integer.parseInt(lvl.trim()); } catch (NumberFormatException ignored) {}
        }

        // ── Resource targets ─────────────────────────────────────────────────
        String resSec = extractSection(json, "resourceTargets");
        if (resSec != null) {
            for (String obj : extractObjects(extractArray(resSec, "tiles"))) {
                s.resourceTiles.add(parseTile(obj));
            }
        }

        // ── Bank targets ─────────────────────────────────────────────────────
        String bankSec = extractSection(json, "bankTargets");
        if (bankSec != null) {
            List<String> bankObjs = extractObjects(extractArray(bankSec, "tiles"));
            if (!bankObjs.isEmpty()) {
                s.bankTile = parseTile(bankObjs.get(0));
            }
        }

        // ── Route anchors ────────────────────────────────────────────────────
        String anchorSec = extractSection(json, "routeAnchors");
        if (anchorSec != null) {
            // bankStandTile
            List<String> standObjs = extractObjects(extractArray(anchorSec, "bankStandTile"));
            if (!standObjs.isEmpty()) s.bankStandTile = parseTile(standObjs.get(0));

            // bankInsideTiles
            for (String obj : extractObjects(extractArray(anchorSec, "bankInsideTiles"))) {
                s.bankInsideTiles.add(parseTile(obj));
            }

            // returnTargetAnchor
            List<String> retObjs = extractObjects(extractArray(anchorSec, "returnTargetAnchor"));
            if (!retObjs.isEmpty()) s.returnAnchor = parseTile(retObjs.get(0));
        }

        // ── Path chains ──────────────────────────────────────────────────────
        String pathSec = extractSection(json, "pathChains");
        if (pathSec != null) {
            for (String obj : extractObjects(extractArray(pathSec, "toBank"))) {
                s.pathToBank.add(parseWaypoint(obj));
            }
            for (String obj : extractObjects(extractArray(pathSec, "toResource"))) {
                s.pathToResource.add(parseWaypoint(obj));
            }
        }
    }

    // ── Mini JSON helpers (read-only, regex-based) ───────────────────────────

    /** Extract a quoted string value for a given key. */
    private static String jStr(String json, String key, String fallback) {
        // Match "key": "value" or "key" : "value"
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return fallback;
        int colon = json.indexOf(':', idx + key.length() + 2);
        if (colon < 0) return fallback;
        int qStart = json.indexOf('"', colon + 1);
        if (qStart < 0) return fallback;
        int qEnd = findClosingQuote(json, qStart + 1);
        if (qEnd < 0) return fallback;
        return unescape(json.substring(qStart + 1, qEnd));
    }

    /** Extract an integer value for a given key. */
    private static int jInt(String json, String key, int fallback) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return fallback;
        int colon = json.indexOf(':', idx + key.length() + 2);
        if (colon < 0) return fallback;
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) start++;
        int end = start;
        if (end < json.length() && json.charAt(end) == '-') end++;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end == start) return fallback;
        try { return Integer.parseInt(json.substring(start, end)); } catch (NumberFormatException e) { return fallback; }
    }

    /** Find closing quote, respecting backslash escapes. */
    private static int findClosingQuote(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (s.charAt(i) == '\\') { i++; continue; }
            if (s.charAt(i) == '"') return i;
        }
        return -1;
    }

    private static String unescape(String s) {
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    /** Extract the content of a top-level or nested JSON object section by key name. */
    private static String extractSection(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int braceStart = json.indexOf('{', idx);
        if (braceStart < 0) return null;
        int depth = 0;
        for (int i = braceStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') { i = findClosingQuote(json, i + 1); if (i < 0) break; continue; }
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) return json.substring(braceStart, i + 1); }
        }
        return null;
    }

    /** Extract the raw content of a JSON array by key name within a section string. */
    private static String extractArray(String section, String key) {
        int idx = section.indexOf("\"" + key + "\"");
        if (idx < 0) return "[]";
        int bracketStart = section.indexOf('[', idx);
        if (bracketStart < 0) return "[]";
        int depth = 0;
        for (int i = bracketStart; i < section.length(); i++) {
            char c = section.charAt(i);
            if (c == '"') { i = findClosingQuote(section, i + 1); if (i < 0) break; continue; }
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) return section.substring(bracketStart, i + 1); }
        }
        return "[]";
    }

    /** Split a JSON array string into individual object strings ({...}). */
    private static List<String> extractObjects(String arrayStr) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int objStart = -1;
        for (int i = 0; i < arrayStr.length(); i++) {
            char c = arrayStr.charAt(i);
            if (c == '"') { i = findClosingQuote(arrayStr, i + 1); if (i < 0) break; continue; }
            if (c == '{') { if (depth == 0) objStart = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0 && objStart >= 0) { result.add(arrayStr.substring(objStart, i + 1)); objStart = -1; } }
        }
        return result;
    }

    /** Parse a tile object JSON string into a CaptureTile. */
    private static CaptureTile parseTile(String obj) {
        CaptureTile t = new CaptureTile();
        t.name  = jStr(obj, "label", jStr(obj, "name", ""));
        t.id    = jInt(obj, "id",    0);
        t.x     = jInt(obj, "x",     0);
        t.y     = jInt(obj, "y",     0);
        t.plane = jInt(obj, "plane", 0);
        t.notes = jStr(obj, "notes", "");

        // Parse actions array into comma-separated string
        String actArr = extractArray(obj, "actions");
        if (actArr != null && actArr.length() > 2) {
            List<String> acts = new ArrayList<>();
            int idx = 0;
            while (true) {
                int q1 = actArr.indexOf('"', idx);
                if (q1 < 0) break;
                int q2 = findClosingQuote(actArr, q1 + 1);
                if (q2 < 0) break;
                acts.add(unescape(actArr.substring(q1 + 1, q2)));
                idx = q2 + 1;
            }
            t.actions = String.join(", ", acts);
        }
        return t;
    }

    /** Parse a waypoint object ({x, y, plane}) into a WorldPoint. */
    private static WorldPoint parseWaypoint(String obj) {
        return new WorldPoint(jInt(obj, "x", 0), jInt(obj, "y", 0), jInt(obj, "plane", 0));
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    public static String slugify(String raw) {
        if (raw == null || raw.isBlank()) return "unnamed";
        return raw.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    public static final class CaptureTile {
        public String name    = "";
        public int    id, x, y, plane;
        public String actions = "";
        public String notes   = "";

        public CaptureTile() {}

        public CaptureTile(String name, int id, int x, int y, int plane, String actions) {
            this.name    = name;
            this.id      = id;
            this.x       = x;
            this.y       = y;
            this.plane   = plane;
            this.actions = actions;
        }

        public List<String> actionList() {
            List<String> result = new ArrayList<>();
            if (actions == null || actions.isBlank()) return result;
            for (String a : actions.split("[,|]")) {
                String t = a.trim();
                if (!t.isEmpty()) result.add(t);
            }
            return result;
        }

        public String coordString()  { return x + ", " + y + ", " + plane; }
        public String idCoordString(){ return id + " @ " + coordString(); }

        @Override public String toString() {
            return (name.isBlank() ? "tile" : name) + " [" + x + "," + y + "]" + (id > 0 ? " id=" + id : "");
        }
    }
}
