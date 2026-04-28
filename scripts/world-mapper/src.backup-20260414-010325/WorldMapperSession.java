package reason.mapper;

import net.runelite.api.coords.WorldPoint;
import rs.kreme.ksbot.api.wrappers.KSGroundItem;
import rs.kreme.ksbot.api.wrappers.KSNPC;
import rs.kreme.ksbot.api.wrappers.KSObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WorldMapperSession {
    private final Instant createdAt = Instant.now();
    private final Map<String, AnchorRecord> anchors = new LinkedHashMap<>();
    private final Map<String, WaypointRecord> waypoints = new LinkedHashMap<>();
    private final Map<String, ObjectRecord> objects = new LinkedHashMap<>();
    private final Map<String, NpcRecord> npcs = new LinkedHashMap<>();
    private final Map<String, GroundItemRecord> groundItems = new LinkedHashMap<>();
    private final Map<String, BlockedTileRecord> blockedTiles = new LinkedHashMap<>();
    private final List<String> importedExportPaths = new ArrayList<>();

    private String sessionName;
    private String zoneName;
    private int autoWaypointIndex = 1;

    WorldMapperSession(String sessionName, String zoneName) {
        this.sessionName = emptyFallback(sessionName, "reason-session");
        this.zoneName = emptyFallback(zoneName, "unassigned-zone");
    }

    void updateMetadata(String sessionName, String zoneName) {
        this.sessionName = emptyFallback(sessionName, "reason-session");
        this.zoneName = emptyFallback(zoneName, "unassigned-zone");
    }

    String sessionName() {
        return sessionName;
    }

    String zoneName() {
        return zoneName;
    }

    int anchorCount() {
        return anchors.size();
    }

    int waypointCount() {
        return waypoints.size();
    }

    int objectCount() {
        return objects.size();
    }

    int npcCount() {
        return npcs.size();
    }

    int groundItemCount() {
        return groundItems.size();
    }

    int blockedTileCount() {
        return blockedTiles.size();
    }

    void addImportedExportPath(String path) {
        if (path == null || path.isBlank() || importedExportPaths.contains(path)) {
            return;
        }
        importedExportPaths.add(path);
    }

    void captureAnchor(String label, WorldPoint point) {
        if (point == null) {
            return;
        }
        String normalizedLabel = emptyFallback(label, "anchor-" + (anchors.size() + 1));
        anchors.put(key(normalizedLabel, point), new AnchorRecord(normalizedLabel, point));
    }

    void captureWaypoint(String label, WorldPoint point, boolean autoRecorded) {
        if (point == null) {
            return;
        }
        String normalizedLabel = emptyFallback(label,
                (autoRecorded ? "auto-waypoint-" : "waypoint-") + autoWaypointIndex++);
        String recordKey = normalizedLabel + "@" + point.getX() + "," + point.getY() + "," + point.getPlane();
        if (waypoints.containsKey(recordKey)) {
            return;
        }
        waypoints.put(recordKey, new WaypointRecord(normalizedLabel, point, autoRecorded, waypoints.size() + 1));
    }

    void captureObjects(Collection<KSObject> scanObjects) {
        if (scanObjects == null) {
            return;
        }
        for (KSObject object : scanObjects) {
            if (object == null || object.getWorldLocation() == null) {
                continue;
            }
            WorldPoint point = object.getWorldLocation();
            String recordKey = object.getId() + "@" + point.getX() + "," + point.getY() + "," + point.getPlane();
            objects.put(recordKey, new ObjectRecord(
                    normalizeName(object.getName()),
                    object.getId(),
                    point,
                    normalizeActions(object.getActions()),
                    object.isAnimating(),
                    object.getAnimation()
            ));
        }
    }

    void captureNpcs(Collection<KSNPC> scanNpcs) {
        if (scanNpcs == null) {
            return;
        }
        for (KSNPC npc : scanNpcs) {
            if (npc == null || npc.getWorldLocation() == null) {
                continue;
            }
            WorldPoint point = npc.getWorldLocation();
            String recordKey = npc.getId() + "@" + point.getX() + "," + point.getY() + "," + point.getPlane();
            npcs.put(recordKey, new NpcRecord(
                    normalizeName(npc.getName()),
                    npc.getId(),
                    point,
                    normalizeActions(npc.getActions()),
                    npc.isAnimating(),
                    npc.getAnimation()
            ));
        }
    }

    void captureGroundItems(Collection<KSGroundItem> scanItems) {
        if (scanItems == null) {
            return;
        }
        for (KSGroundItem item : scanItems) {
            if (item == null || item.getWorldLocation() == null) {
                continue;
            }
            WorldPoint point = item.getWorldLocation();
            String recordKey = item.getId() + "@" + point.getX() + "," + point.getY() + "," + point.getPlane();
            groundItems.put(recordKey, new GroundItemRecord(
                    normalizeName(item.getName()),
                    item.getId(),
                    point,
                    item.getQuantity(),
                    normalizeActions(item.getActions())
            ));
        }
    }

    void captureBlockedTile(WorldPoint point, int tileFlag) {
        if (point == null) {
            return;
        }
        String recordKey = point.getX() + "," + point.getY() + "," + point.getPlane();
        blockedTiles.put(recordKey, new BlockedTileRecord(point, tileFlag));
    }

    void clear() {
        anchors.clear();
        waypoints.clear();
        objects.clear();
        npcs.clear();
        groundItems.clear();
        blockedTiles.clear();
        importedExportPaths.clear();
        autoWaypointIndex = 1;
    }

    String toJson(WorldPoint playerTile) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        appendJsonField(json, "sessionName", sessionName, 2, true);
        appendJsonField(json, "zoneName", zoneName, 2, true);
        appendJsonField(json, "createdAt", createdAt.toString(), 2, true);
        json.append("  \"summary\": {\n");
        appendJsonNumberField(json, "anchorCount", anchorCount(), 4, true);
        appendJsonNumberField(json, "waypointCount", waypointCount(), 4, true);
        appendJsonNumberField(json, "objectCount", objectCount(), 4, true);
        appendJsonNumberField(json, "npcCount", npcCount(), 4, true);
        appendJsonNumberField(json, "groundItemCount", groundItemCount(), 4, true);
        appendJsonNumberField(json, "blockedTileCount", blockedTileCount(), 4, false);
        json.append("  },\n");
        json.append("  \"playerSnapshot\": ");
        appendWorldPoint(json, playerTile, 2);
        json.append(",\n");
        appendStringArray(json, "importedMapperExports", importedExportPaths, 2, true);
        appendAnchorArray(json, "anchors", new ArrayList<>(anchors.values()), 2, true);
        appendWaypointArray(json, "waypoints", new ArrayList<>(waypoints.values()), 2, true);
        appendObjectArray(json, "objects", new ArrayList<>(objects.values()), 2, true);
        appendNpcArray(json, "npcs", new ArrayList<>(npcs.values()), 2, true);
        appendGroundItemArray(json, "groundItems", new ArrayList<>(groundItems.values()), 2, true);
        appendBlockedTileArray(json, "blockedTiles", new ArrayList<>(blockedTiles.values()), 2, true);
        json.append("  \"walkerHints\": {\n");
        appendAnchorReferenceArray(json, "bankAnchors", new ArrayList<>(anchors.values()), "bank", 4, true);
        appendAnchorReferenceArray(json, "resourceAnchors", new ArrayList<>(anchors.values()), "resource", 4, true);
        appendWaypointReferenceArray(json, "waypointChain", new ArrayList<>(waypoints.values()), 4, false);
        json.append("  }\n");
        json.append("}\n");
        return json.toString();
    }

    private void appendAnchorArray(StringBuilder json, String name, List<AnchorRecord> records, int indent, boolean trailingComma) {
        indent(json, indent).append("\"").append(name).append("\": [\n");
        for (int i = 0; i < records.size(); i++) {
            AnchorRecord record = records.get(i);
            indent(json, indent + 2).append("{\n");
            appendJsonField(json, "label", record.label, indent + 4, true);
            indent(json, indent + 4).append("\"tile\": ");
            appendWorldPoint(json, record.point, indent + 4);
            json.append("\n");
            indent(json, indent + 2).append("}");
            if (i < records.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        indent(json, indent).append("]");
        if (trailingComma) {
            json.append(",");
        }
        json.append("\n");
    }

    private void appendWaypointArray(StringBuilder json, String name, List<WaypointRecord> records, int indent, boolean trailingComma) {
        indent(json, indent).append("\"").append(name).append("\": [\n");
        for (int i = 0; i < records.size(); i++) {
            WaypointRecord record = records.get(i);
            indent(json, indent + 2).append("{\n");
            appendJsonField(json, "label", record.label, indent + 4, true);
            appendJsonNumberField(json, "order", record.order, indent + 4, true);
            appendJsonBooleanField(json, "autoRecorded", record.autoRecorded, indent + 4, true);
            indent(json, indent + 4).append("\"tile\": ");
            appendWorldPoint(json, record.point, indent + 4);
            json.append("\n");
            indent(json, indent + 2).append("}");
            if (i < records.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        indent(json, indent).append("]");
        if (trailingComma) {
            json.append(",");
        }
        json.append("\n");
    }

    private void appendObjectArray(StringBuilder json, String name, List<ObjectRecord> records, int indent, boolean trailingComma) {
        indent(json, indent).append("\"").append(name).append("\": [\n");
        for (int i = 0; i < records.size(); i++) {
            ObjectRecord record = records.get(i);
            indent(json, indent + 2).append("{\n");
            appendJsonField(json, "name", record.name, indent + 4, true);
            appendJsonNumberField(json, "id", record.id, indent + 4, true);
            appendStringArray(json, "actions", record.actions, indent + 4, true);
            appendJsonBooleanField(json, "animating", record.animating, indent + 4, true);
            appendJsonNumberField(json, "animation", record.animation, indent + 4, true);
            indent(json, indent + 4).append("\"tile\": ");
            appendWorldPoint(json, record.point, indent + 4);
            json.append("\n");
            indent(json, indent + 2).append("}");
            if (i < records.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        indent(json, indent).append("]");
        if (trailingComma) {
            json.append(",");
        }
        json.append("\n");
    }

    private void appendNpcArray(StringBuilder json, String name, List<NpcRecord> records, int indent, boolean trailingComma) {
        indent(json, indent).append("\"").append(name).append("\": [\n");
        for (int i = 0; i < records.size(); i++) {
            NpcRecord record = records.get(i);
            indent(json, indent + 2).append("{\n");
            appendJsonField(json, "name", record.name, indent + 4, true);
            appendJsonNumberField(json, "id", record.id, indent + 4, true);
            appendStringArray(json, "actions", record.actions, indent + 4, true);
            appendJsonBooleanField(json, "animating", record.animating, indent + 4, true);
            appendJsonNumberField(json, "animation", record.animation, indent + 4, true);
            indent(json, indent + 4).append("\"tile\": ");
            appendWorldPoint(json, record.point, indent + 4);
            json.append("\n");
            indent(json, indent + 2).append("}");
            if (i < records.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        indent(json, indent).append("]");
        if (trailingComma) {
            json.append(",");
        }
        json.append("\n");
    }

    private void appendGroundItemArray(StringBuilder json, String name, List<GroundItemRecord> records, int indent, boolean trailingComma) {
        indent(json, indent).append("\"").append(name).append("\": [\n");
        for (int i = 0; i < records.size(); i++) {
            GroundItemRecord record = records.get(i);
            indent(json, indent + 2).append("{\n");
            appendJsonField(json, "name", record.name, indent + 4, true);
            appendJsonNumberField(json, "id", record.id, indent + 4, true);
            appendJsonNumberField(json, "quantity", record.quantity, indent + 4, true);
            appendStringArray(json, "actions", record.actions, indent + 4, true);
            indent(json, indent + 4).append("\"tile\": ");
            appendWorldPoint(json, record.point, indent + 4);
            json.append("\n");
            indent(json, indent + 2).append("}");
            if (i < records.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        indent(json, indent).append("]");
        if (trailingComma) {
            json.append(",");
        }
        json.append("\n");
    }

    private void appendBlockedTileArray(StringBuilder json, String name, List<BlockedTileRecord> records, int indent, boolean trailingComma) {
        indent(json, indent).append("\"").append(name).append("\": [\n");
        for (int i = 0; i < records.size(); i++) {
            BlockedTileRecord record = records.get(i);
            indent(json, indent + 2).append("{\n");
            appendJsonNumberField(json, "tileFlag", record.tileFlag, indent + 4, true);
            indent(json, indent + 4).append("\"tile\": ");
            appendWorldPoint(json, record.point, indent + 4);
            json.append("\n");
            indent(json, indent + 2).append("}");
            if (i < records.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        indent(json, indent).append("]");
        if (trailingComma) {
            json.append(",");
        }
        json.append("\n");
    }

    private void appendAnchorReferenceArray(StringBuilder json, String name, List<AnchorRecord> records, String keyword, int indent, boolean trailingComma) {
        List<String> matches = new ArrayList<>();
        for (AnchorRecord record : records) {
            if (record.label.toLowerCase().contains(keyword.toLowerCase())) {
                matches.add(record.label);
            }
        }
        appendStringArray(json, name, matches, indent, trailingComma);
    }

    private void appendWaypointReferenceArray(StringBuilder json, String name, List<WaypointRecord> records, int indent, boolean trailingComma) {
        List<String> labels = new ArrayList<>();
        for (WaypointRecord record : records) {
            labels.add(record.label);
        }
        appendStringArray(json, name, labels, indent, trailingComma);
    }

    private void appendStringArray(StringBuilder json, String name, List<String> values, int indent, boolean trailingComma) {
        indent(json, indent).append("\"").append(name).append("\": [");
        if (!values.isEmpty()) {
            json.append("\n");
            for (int i = 0; i < values.size(); i++) {
                indent(json, indent + 2).append("\"").append(escape(values.get(i))).append("\"");
                if (i < values.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            indent(json, indent);
        }
        json.append("]");
        if (trailingComma) {
            json.append(",");
        }
        json.append("\n");
    }

    private void appendJsonField(StringBuilder json, String name, String value, int indent, boolean trailingComma) {
        indent(json, indent).append("\"").append(name).append("\": \"").append(escape(value)).append("\"");
        if (trailingComma) {
            json.append(",");
        }
        json.append("\n");
    }

    private void appendJsonNumberField(StringBuilder json, String name, int value, int indent, boolean trailingComma) {
        indent(json, indent).append("\"").append(name).append("\": ").append(value);
        if (trailingComma) {
            json.append(",");
        }
        json.append("\n");
    }

    private void appendJsonBooleanField(StringBuilder json, String name, boolean value, int indent, boolean trailingComma) {
        indent(json, indent).append("\"").append(name).append("\": ").append(value);
        if (trailingComma) {
            json.append(",");
        }
        json.append("\n");
    }

    private void appendWorldPoint(StringBuilder json, WorldPoint point, int indent) {
        if (point == null) {
            json.append("null");
            return;
        }
        json.append("{\n");
        appendJsonNumberField(json, "x", point.getX(), indent + 2, true);
        appendJsonNumberField(json, "y", point.getY(), indent + 2, true);
        appendJsonNumberField(json, "plane", point.getPlane(), indent + 2, false);
        indent(json, indent).append("}");
    }

    private StringBuilder indent(StringBuilder builder, int spaces) {
        for (int i = 0; i < spaces; i++) {
            builder.append(' ');
        }
        return builder;
    }

    private String normalizeName(String raw) {
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw.trim())) {
            return "Unknown";
        }
        return raw.trim();
    }

    private List<String> normalizeActions(String[] rawActions) {
        List<String> actions = new ArrayList<>();
        if (rawActions == null) {
            return actions;
        }
        for (String action : rawActions) {
            if (action == null || action.isBlank() || "null".equalsIgnoreCase(action.trim())) {
                continue;
            }
            actions.add(action.trim());
        }
        return actions;
    }

    private String escape(String raw) {
        return raw
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String emptyFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String key(String label, WorldPoint point) {
        return label + "@" + point.getX() + "," + point.getY() + "," + point.getPlane();
    }

    private static final class AnchorRecord {
        private final String label;
        private final WorldPoint point;

        private AnchorRecord(String label, WorldPoint point) {
            this.label = label;
            this.point = point;
        }
    }

    private static final class WaypointRecord {
        private final String label;
        private final WorldPoint point;
        private final boolean autoRecorded;
        private final int order;

        private WaypointRecord(String label, WorldPoint point, boolean autoRecorded, int order) {
            this.label = label;
            this.point = point;
            this.autoRecorded = autoRecorded;
            this.order = order;
        }
    }

    private static final class ObjectRecord {
        private final String name;
        private final int id;
        private final WorldPoint point;
        private final List<String> actions;
        private final boolean animating;
        private final int animation;

        private ObjectRecord(String name, int id, WorldPoint point, List<String> actions, boolean animating, int animation) {
            this.name = name;
            this.id = id;
            this.point = point;
            this.actions = actions;
            this.animating = animating;
            this.animation = animation;
        }
    }

    private static final class NpcRecord {
        private final String name;
        private final int id;
        private final WorldPoint point;
        private final List<String> actions;
        private final boolean animating;
        private final int animation;

        private NpcRecord(String name, int id, WorldPoint point, List<String> actions, boolean animating, int animation) {
            this.name = name;
            this.id = id;
            this.point = point;
            this.actions = actions;
            this.animating = animating;
            this.animation = animation;
        }
    }

    private static final class GroundItemRecord {
        private final String name;
        private final int id;
        private final WorldPoint point;
        private final int quantity;
        private final List<String> actions;

        private GroundItemRecord(String name, int id, WorldPoint point, int quantity, List<String> actions) {
            this.name = name;
            this.id = id;
            this.point = point;
            this.quantity = quantity;
            this.actions = actions;
        }
    }

    private static final class BlockedTileRecord {
        private final WorldPoint point;
        private final int tileFlag;

        private BlockedTileRecord(WorldPoint point, int tileFlag) {
            this.point = point;
            this.tileFlag = tileFlag;
        }
    }
}
