package reason.woodcutterreference;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReferenceWoodcuttingProfile {
    public static final int OAK_TREE_ID = 10820;

    public static final class TileTarget {
        private final WorldPoint location;
        private final int objectId;
        private final List<String> actions;

        public TileTarget(WorldPoint location, int objectId, List<String> actions) {
            this.location = location;
            this.objectId = objectId;
            this.actions = List.copyOf(actions);
        }

        public WorldPoint location() { return location; }
        public int objectId() { return objectId; }
        public List<String> actions() { return actions; }
    }

    public static final class RouteProfile {
        private final String label;
        private final List<Integer> treeIds;
        private final List<String> treeNames;
        private final List<String> logNames;
        private final List<WorldPoint> resourceTiles;
        private final WorldPoint returnTreeAnchor;
        private final List<WorldPoint> bankDoorTiles;
        private final List<WorldPoint> bankInsideTiles;
        private final List<WorldPoint> bankStandTiles;
        private final List<TileTarget> bankTargets;
        private final boolean insideBankRequired;

        public RouteProfile(
                String label,
                List<Integer> treeIds,
                List<String> treeNames,
                List<String> logNames,
                List<WorldPoint> resourceTiles,
                WorldPoint returnTreeAnchor,
                List<WorldPoint> bankDoorTiles,
                List<WorldPoint> bankInsideTiles,
                List<WorldPoint> bankStandTiles,
                List<TileTarget> bankTargets,
                boolean insideBankRequired
        ) {
            this.label = label;
            this.treeIds = List.copyOf(treeIds);
            this.treeNames = List.copyOf(treeNames);
            this.logNames = List.copyOf(logNames);
            this.resourceTiles = List.copyOf(resourceTiles);
            this.returnTreeAnchor = returnTreeAnchor;
            this.bankDoorTiles = List.copyOf(bankDoorTiles);
            this.bankInsideTiles = List.copyOf(bankInsideTiles);
            this.bankStandTiles = List.copyOf(bankStandTiles);
            this.bankTargets = List.copyOf(bankTargets);
            this.insideBankRequired = insideBankRequired;
        }

        public String label() { return label; }
        public List<Integer> treeIds() { return treeIds; }
        public List<String> treeNames() { return treeNames; }
        public List<String> logNames() { return logNames; }
        public List<WorldPoint> resourceTiles() { return resourceTiles; }
        public WorldPoint returnTreeAnchor() { return returnTreeAnchor; }
        public List<WorldPoint> bankDoorTiles() { return bankDoorTiles; }
        public List<WorldPoint> bankInsideTiles() { return bankInsideTiles; }
        public List<WorldPoint> bankStandTiles() { return bankStandTiles; }
        public List<TileTarget> bankTargets() { return bankTargets; }
        public boolean insideBankRequired() { return insideBankRequired; }

        public WorldArea treeArea() {
            return paddedArea(resourceTiles, 2);
        }

        public WorldArea bankArea() {
            List<WorldPoint> points = new ArrayList<>();
            points.addAll(bankDoorTiles);
            points.addAll(bankInsideTiles);
            points.addAll(bankStandTiles);
            for (TileTarget target : bankTargets) {
                points.add(target.location());
            }
            return paddedArea(points, 1);
        }

        public WorldPoint bankStandAnchor() {
            return bankStandTiles.isEmpty() ? null : bankStandTiles.get(0);
        }

        public WorldPoint bankTargetAnchor() {
            return bankTargets.isEmpty() ? bankStandAnchor() : bankTargets.get(0).location();
        }

        public List<Integer> bankObjectIds() {
            List<Integer> ids = new ArrayList<>();
            for (TileTarget target : bankTargets) {
                if (!ids.contains(target.objectId())) {
                    ids.add(target.objectId());
                }
            }
            return ids;
        }

        public List<String> bankOpenActions() {
            List<String> actions = new ArrayList<>();
            for (TileTarget target : bankTargets) {
                for (String action : target.actions()) {
                    if (action != null
                            && (action.equalsIgnoreCase("Bank")
                            || action.equalsIgnoreCase("Use")
                            || action.equalsIgnoreCase("Open"))
                            && !actions.contains(action)) {
                        actions.add(action);
                    }
                }
            }
            return actions;
        }

        public List<WorldPoint> bankApproachPath() {
            if (!insideBankRequired) {
                return Collections.emptyList();
            }
            List<WorldPoint> path = new ArrayList<>();
            path.addAll(bankDoorTiles);
            path.addAll(bankInsideTiles);
            path.addAll(bankStandTiles);
            return path;
        }
    }

    public static final RouteProfile VARROCK_WEST_OAKS = new RouteProfile(
            "Varrock West Oaks",
            List.of(OAK_TREE_ID),
            List.of("Oak tree"),
            List.of("Oak logs"),
            List.of(point(3167, 3420), point(3161, 3416), point(3165, 3411)),
            point(3170, 3424),
            List.of(point(3183, 3433), point(3182, 3433)),
            List.of(point(3183, 3435), point(3182, 3435), point(3182, 3434), point(3183, 3434)),
            List.of(point(3185, 3436)),
            List.of(
                    new TileTarget(point(3186, 3436), 10583, List.of("Bank")),
                    new TileTarget(point(3186, 3436), 34810, List.of("Bank"))
            ),
            true
    );

    private static WorldArea paddedArea(List<WorldPoint> points, int padding) {
        if (points.isEmpty()) {
            return new WorldArea(0, 0, 1, 1, 0);
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int plane = points.get(0).getPlane();

        for (WorldPoint point : points) {
            minX = Math.min(minX, point.getX());
            minY = Math.min(minY, point.getY());
            maxX = Math.max(maxX, point.getX());
            maxY = Math.max(maxY, point.getY());
        }

        return new WorldArea(
                minX - padding,
                minY - padding,
                (maxX - minX) + 1 + (padding * 2),
                (maxY - minY) + 1 + (padding * 2),
                plane
        );
    }

    private static WorldPoint point(int x, int y) {
        return new WorldPoint(x, y, 0);
    }

    private ReferenceWoodcuttingProfile() {
    }
}
