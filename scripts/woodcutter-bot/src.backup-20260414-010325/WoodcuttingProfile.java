package reason.woodcutter;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class WoodcuttingProfile {
    public static final int REASON_TREE_ID = 1276;
    public static final int REASON_OAK_TREE_ID = 10820;
    public static final int REASON_WILLOW_TREE_ID_A = 10838;
    public static final int REASON_WILLOW_TREE_ID_B = 10829;
    public static final int REASON_WILLOW_TREE_ID_C = 10831;
    public static final int REASON_WILLOW_TREE_ID_D = 10819;
    public static final int REASON_MAPLE_TREE_ID = 10832;
    public static final int REASON_YEW_TREE_ID = 10822;
    public static final int REASON_MAGIC_TREE_ID = 10834;
    public static final int REASON_WOODCUTTING_GUILD_BANK_CHEST_ID = 28861;

    public enum TreeType {
        TREE("Tree", List.of("Tree"), List.of(REASON_TREE_ID, 1277, 1278, 1279, 1280, 4532, 4539), List.of("Logs")),
        OAK_TREE("Oak Tree", List.of("Oak tree"), List.of(REASON_OAK_TREE_ID, 4533, 4540, 42395, 42831, 51772, 55913, 58539), List.of("Oak logs")),
        WILLOW_TREE("Willow Tree", List.of("Willow tree"), List.of(REASON_WILLOW_TREE_ID_A, REASON_WILLOW_TREE_ID_B, REASON_WILLOW_TREE_ID_C, REASON_WILLOW_TREE_ID_D, 4534, 4541), List.of("Willow logs")),
        MAPLE_TREE("Maple Tree", List.of("Maple tree"), List.of(REASON_MAPLE_TREE_ID, 4535, 4674, 5126, 40754, 40755), List.of("Maple logs")),
        YEW_TREE("Yew Tree", List.of("Yew tree"), List.of(REASON_YEW_TREE_ID, 4536, 5121, 40756, 42391, 42427, 57790), List.of("Yew logs")),
        MAGIC_TREE("Magic Tree", List.of("Magic tree"), List.of(REASON_MAGIC_TREE_ID, 4537, 5127, 8396), List.of("Magic logs"));

        private final String label;
        private final List<String> treeNames;
        private final List<Integer> treeObjectIds;
        private final List<String> logNames;

        TreeType(String label, List<String> treeNames, List<Integer> treeObjectIds, List<String> logNames) {
            this.label = label;
            this.treeNames = treeNames;
            this.treeObjectIds = treeObjectIds;
            this.logNames = logNames;
        }

        public List<String> treeNames() { return treeNames; }
        public List<Integer> treeObjectIds() { return treeObjectIds; }
        public List<String> logNames() { return logNames; }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Mode {
        POWERCUT("Powercut"),
        BANK("Bank");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum BankType {
        BOOTH,
        CHEST
    }

    public static final class TileTarget {
        private final WorldPoint location;
        private final int objectId;
        private final List<String> actions;

        public TileTarget(WorldPoint location, int objectId, List<String> actions) {
            this.location = Objects.requireNonNull(location, "location");
            this.objectId = objectId;
            this.actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        }

        public WorldPoint location() { return location; }
        public int objectId() { return objectId; }
        public List<String> actions() { return actions; }
    }

    public static final class RouteProfile {
        private final String label;
        private final TreeType treeType;
        private final List<WorldPoint> resourceTiles;
        private final WorldArea treeArea;
        private final WorldPoint treeAnchor;
        private final WorldPoint returnTreeAnchor;
        private final String bankLabel;
        private final BankType bankType;
        private final List<WorldPoint> bankDoorTiles;
        private final List<WorldPoint> bankInsideTiles;
        private final List<WorldPoint> bankStandTiles;
        private final List<TileTarget> bankTargets;
        private final WorldArea bankArea;
        private final int minimumWoodcuttingLevel;
        private final boolean insideBankRequired;

        public RouteProfile(
                String label,
                TreeType treeType,
                List<WorldPoint> resourceTiles,
                WorldPoint treeAnchor,
                WorldPoint returnTreeAnchor,
                String bankLabel,
                BankType bankType,
                List<WorldPoint> bankDoorTiles,
                List<WorldPoint> bankInsideTiles,
                List<WorldPoint> bankStandTiles,
                List<TileTarget> bankTargets,
                int minimumWoodcuttingLevel,
                boolean insideBankRequired
        ) {
            this.label = label;
            this.treeType = treeType;
            this.resourceTiles = List.copyOf(resourceTiles);
            this.treeArea = paddedArea(this.resourceTiles, 2);
            this.treeAnchor = treeAnchor != null ? treeAnchor : this.resourceTiles.get(0);
            this.returnTreeAnchor = returnTreeAnchor != null ? returnTreeAnchor : this.treeAnchor;
            this.bankLabel = bankLabel;
            this.bankType = bankType;
            this.bankDoorTiles = List.copyOf(bankDoorTiles);
            this.bankInsideTiles = List.copyOf(bankInsideTiles);
            this.bankStandTiles = List.copyOf(bankStandTiles);
            this.bankTargets = List.copyOf(bankTargets);
            this.bankArea = paddedArea(bankAreaPoints(), 1);
            this.minimumWoodcuttingLevel = minimumWoodcuttingLevel;
            this.insideBankRequired = insideBankRequired;
        }

        private List<WorldPoint> bankAreaPoints() {
            List<WorldPoint> points = new ArrayList<>();
            points.addAll(bankDoorTiles);
            points.addAll(bankInsideTiles);
            points.addAll(bankStandTiles);
            for (TileTarget target : bankTargets) {
                points.add(target.location());
            }
            return points;
        }

        public String label() { return label; }
        public TreeType treeType() { return treeType; }
        public List<WorldPoint> resourceTiles() { return resourceTiles; }
        public WorldArea treeArea() { return treeArea; }
        public WorldPoint treeAnchor() { return treeAnchor; }
        public WorldPoint returnTreeAnchor() { return returnTreeAnchor; }
        public String bankLabel() { return bankLabel; }
        public BankType bankType() { return bankType; }
        public List<WorldPoint> bankDoorTiles() { return bankDoorTiles; }
        public List<WorldPoint> bankInsideTiles() { return bankInsideTiles; }
        public List<WorldPoint> bankStandTiles() { return bankStandTiles; }
        public List<TileTarget> bankTargets() { return bankTargets; }
        public WorldArea bankArea() { return bankArea; }
        public int minimumWoodcuttingLevel() { return minimumWoodcuttingLevel; }
        public boolean insideBankRequired() { return insideBankRequired; }

        public WorldPoint bankStandAnchor() {
            if (!bankStandTiles.isEmpty()) {
                return bankStandTiles.get(0);
            }
            if (!bankInsideTiles.isEmpty()) {
                return bankInsideTiles.get(0);
            }
            if (!bankTargets.isEmpty()) {
                return bankTargets.get(0).location();
            }
            return null;
        }

        public WorldPoint bankAnchor() {
            return !bankTargets.isEmpty() ? bankTargets.get(0).location() : bankStandAnchor();
        }

        public List<WorldPoint> bankApproachPath() {
            if (!insideBankRequired) {
                return Collections.emptyList();
            }
            List<WorldPoint> path = new ArrayList<>();
            path.addAll(bankDoorTiles);
            path.addAll(bankInsideTiles);
            path.addAll(bankStandTiles);
            return path.stream().distinct().collect(Collectors.toList());
        }

        public List<Integer> bankObjectIds() {
            return bankTargets.stream().map(TileTarget::objectId).distinct().collect(Collectors.toList());
        }

        public List<WorldPoint> bankTargetLocations() {
            return bankTargets.stream().map(TileTarget::location).collect(Collectors.toList());
        }

        public List<String> bankActions() {
            return bankTargets.stream().flatMap(target -> target.actions().stream()).distinct().collect(Collectors.toList());
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final List<RouteProfile> ROUTES = List.of(
            new RouteProfile("Varrock West Oaks", TreeType.OAK_TREE,
                    points(point(3167, 3420), point(3161, 3416), point(3165, 3411)),
                    point(3167, 3420), point(3170, 3424), "Varrock West Bank", BankType.BOOTH,
                    points(point(3183, 3433), point(3182, 3433)),
                    points(point(3183, 3435), point(3182, 3435), point(3182, 3434), point(3183, 3434)),
                    points(point(3185, 3436)),
                    List.of(
                            new TileTarget(point(3186, 3436), 10583, List.of("Bank")),
                            new TileTarget(point(3186, 3436), 34810, List.of("Bank"))
                    ),
                    15, true),
            new RouteProfile("Draynor Willows", TreeType.WILLOW_TREE,
                    points(point(3083, 3237), point(3085, 3235), point(3087, 3231), point(3088, 3227), point(3088, 3234)),
                    point(3088, 3234), point(3088, 3234), "Draynor Bank", BankType.BOOTH,
                    points(point(3092, 3246)),
                    points(point(3092, 3246), point(3092, 3245)),
                    points(point(3092, 3245)),
                    List.of(new TileTarget(point(3091, 3245), 10355, List.of("Bank"))),
                    30, true),
            new RouteProfile("Seers Village Maples", TreeType.MAPLE_TREE,
                    points(point(2727, 3501), point(2721, 3501), point(2730, 3501), point(2732, 3499)),
                    point(2727, 3501), point(2727, 3501), "Seers Village Bank", BankType.BOOTH,
                    List.of(),
                    points(point(2726, 3489), point(2727, 3491), point(2727, 3492)),
                    points(point(2727, 3492)),
                    List.of(new TileTarget(point(2727, 3494), 25808, List.of("Bank"))),
                    45, true),
            new RouteProfile("Yews in Seers village", TreeType.YEW_TREE,
                    points(point(2714, 3459), point(2705, 3458), point(2705, 3464)),
                    point(2714, 3459), point(2714, 3459), "Seers Village Bank", BankType.BOOTH,
                    List.of(),
                    points(point(2726, 3489), point(2727, 3491), point(2727, 3492)),
                    points(point(2727, 3492)),
                    List.of(new TileTarget(point(2727, 3494), 25808, List.of("Bank"))),
                    60, true),
            new RouteProfile("Catherby Oak Trees", TreeType.OAK_TREE,
                    points(point(2787, 3434), point(2786, 3438), point(2787, 3444)),
                    point(2787, 3434), point(2787, 3434), "Catherby Bank", BankType.BOOTH,
                    points(point(2808, 3438), point(2809, 3438)),
                    points(point(2808, 3438), point(2809, 3438)),
                    points(point(2807, 3441)),
                    List.of(new TileTarget(point(2807, 3442), 10355, List.of("Bank", "Collect"))),
                    15, false),
            new RouteProfile("Woodcutting Guild Yews", TreeType.YEW_TREE,
                    points(point(1585, 3479), point(1583, 3481), point(1595, 3481), point(1595, 3480), point(1590, 3486), point(1590, 3492)),
                    point(1590, 3481), point(1590, 3481), "Woodcutting Guild Bank Chest", BankType.CHEST,
                    points(point(1591, 3479)),
                    points(point(1591, 3479), point(1591, 3478), point(1591, 3477), point(1591, 3476)),
                    points(point(1591, 3477), point(1592, 3476)),
                    List.of(new TileTarget(point(1592, 3475), REASON_WOODCUTTING_GUILD_BANK_CHEST_ID, List.of("Use", "Bank"))),
                    60, true),
            new RouteProfile("Woodcutting Guild Magics", TreeType.MAGIC_TREE,
                    points(point(1580, 3482), point(1580, 3485), point(1577, 3482), point(1577, 3485)),
                    point(1580, 3482), point(1580, 3482), "Woodcutting Guild Bank Chest", BankType.CHEST,
                    points(point(1591, 3479)),
                    points(point(1591, 3479), point(1591, 3478), point(1591, 3477), point(1591, 3476)),
                    points(point(1591, 3477), point(1592, 3476)),
                    List.of(new TileTarget(point(1592, 3475), REASON_WOODCUTTING_GUILD_BANK_CHEST_ID, List.of("Use", "Bank"))),
                    75, true)
    );

    public static List<RouteProfile> routesFor(TreeType treeType) {
        return ROUTES.stream().filter(route -> route.treeType() == treeType).collect(Collectors.toList());
    }

    public static RouteProfile defaultRouteFor(TreeType treeType) {
        List<RouteProfile> routes = routesFor(treeType);
        return routes.isEmpty() ? null : routes.get(0);
    }

    public static List<String> allKnownLogNames() {
        return Arrays.stream(TreeType.values())
                .flatMap(treeType -> treeType.logNames().stream())
                .distinct()
                .collect(Collectors.toList());
    }

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

        return new WorldArea(minX - padding, minY - padding, (maxX - minX) + 1 + (padding * 2), (maxY - minY) + 1 + (padding * 2), plane);
    }

    private static List<WorldPoint> points(WorldPoint... points) {
        return List.of(points);
    }

    private static WorldPoint point(int x, int y) {
        return new WorldPoint(x, y, 0);
    }
}
