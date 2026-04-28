package local.reason.woodcutter;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.List;
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

        public List<String> treeNames() {
            return treeNames;
        }

        public List<Integer> treeObjectIds() {
            return treeObjectIds;
        }

        public List<String> logNames() {
            return logNames;
        }

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

    public static final class RouteProfile {
        private final String label;
        private final TreeType treeType;
        private final WorldArea treeArea;
        private final WorldPoint treeAnchor;
        private final String bankLabel;
        private final WorldArea bankArea;
        private final WorldPoint bankEntryAnchor;
        private final WorldPoint bankStandAnchor;
        private final WorldPoint bankAnchor;
        private final List<WorldPoint> bankApproachPath;
        private final List<Integer> bankObjectIds;
        private final List<WorldPoint> bankObjectLocations;
        private final List<String> bankActions;
        private final int minimumWoodcuttingLevel;
        private final boolean teleportBankSupported;
        private final WorldPoint returnTreeAnchor;

        public RouteProfile(
                String label,
                TreeType treeType,
                WorldArea treeArea,
                WorldPoint treeAnchor,
                String bankLabel,
                WorldArea bankArea,
                WorldPoint bankEntryAnchor,
                WorldPoint bankStandAnchor,
                WorldPoint bankAnchor,
                List<WorldPoint> bankApproachPath,
                List<Integer> bankObjectIds,
                List<WorldPoint> bankObjectLocations,
                List<String> bankActions,
                int minimumWoodcuttingLevel,
                boolean teleportBankSupported,
                WorldPoint returnTreeAnchor
        ) {
            this.label = label;
            this.treeType = treeType;
            this.treeArea = treeArea;
            this.treeAnchor = treeAnchor;
            this.bankLabel = bankLabel;
            this.bankArea = bankArea;
            this.bankEntryAnchor = bankEntryAnchor;
            this.bankStandAnchor = bankStandAnchor;
            this.bankAnchor = bankAnchor;
            this.bankApproachPath = bankApproachPath;
            this.bankObjectIds = bankObjectIds;
            this.bankObjectLocations = bankObjectLocations;
            this.bankActions = bankActions;
            this.minimumWoodcuttingLevel = minimumWoodcuttingLevel;
            this.teleportBankSupported = teleportBankSupported;
            this.returnTreeAnchor = returnTreeAnchor;
        }

        public String label() { return label; }
        public TreeType treeType() { return treeType; }
        public WorldArea treeArea() { return treeArea; }
        public WorldPoint treeAnchor() { return treeAnchor; }
        public String bankLabel() { return bankLabel; }
        public WorldArea bankArea() { return bankArea; }
        public WorldPoint bankEntryAnchor() { return bankEntryAnchor; }
        public WorldPoint bankStandAnchor() { return bankStandAnchor; }
        public WorldPoint bankAnchor() { return bankAnchor; }
        public List<WorldPoint> bankApproachPath() { return bankApproachPath; }
        public List<Integer> bankObjectIds() { return bankObjectIds; }
        public List<WorldPoint> bankObjectLocations() { return bankObjectLocations; }
        public List<String> bankActions() { return bankActions; }
        public int minimumWoodcuttingLevel() { return minimumWoodcuttingLevel; }
        public boolean teleportBankSupported() { return teleportBankSupported; }
        public WorldPoint returnTreeAnchor() { return returnTreeAnchor; }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final List<RouteProfile> ROUTES = List.of(
            new RouteProfile(
                    "Varrock West Trees",
                    TreeType.TREE,
                    new WorldArea(3158, 3404, 10, 13, 0),
                    new WorldPoint(3162, 3411, 0),
                    "Varrock West Bank",
                    new WorldArea(3183, 3434, 5, 5, 0),
                    new WorldPoint(3182, 3433, 0),
                    new WorldPoint(3184, 3435, 0),
                    new WorldPoint(3186, 3436, 0),
                    List.of(),
                    List.of(10583),
                    List.of(new WorldPoint(3186, 3436, 0)),
                    List.of("Bank"),
                    1,
                    false,
                    new WorldPoint(3162, 3411, 0)
            ),
            new RouteProfile(
                    "Varrock West Oaks",
                    TreeType.OAK_TREE,
                    new WorldArea(3160, 3410, 9, 11, 0),
                    new WorldPoint(3167, 3420, 0),
                    "Varrock West Bank",
                    new WorldArea(3183, 3434, 5, 5, 0),
                    new WorldPoint(3182, 3433, 0),
                    new WorldPoint(3184, 3435, 0),
                    new WorldPoint(3186, 3436, 0),
                    List.of(),
                    List.of(10583),
                    List.of(new WorldPoint(3186, 3436, 0)),
                    List.of("Bank"),
                    15,
                    false,
                    new WorldPoint(3167, 3420, 0)
            ),
            new RouteProfile(
                    "Draynor Willows",
                    TreeType.WILLOW_TREE,
                    new WorldArea(3082, 3227, 7, 11, 0),
                    new WorldPoint(3086, 3231, 0),
                    "Draynor Bank",
                    new WorldArea(3090, 3241, 3, 7, 0),
                    new WorldPoint(3092, 3246, 0),
                    new WorldPoint(3092, 3245, 0),
                    new WorldPoint(3091, 3245, 0),
                    List.of(
                            new WorldPoint(3092, 3246, 0),
                            new WorldPoint(3092, 3245, 0)
                    ),
                    List.of(10355, 10527),
                    List.of(
                            new WorldPoint(3091, 3245, 0),
                            new WorldPoint(3091, 3243, 0),
                            new WorldPoint(3091, 3242, 0),
                            new WorldPoint(3091, 3241, 0)
                    ),
                    List.of("Bank"),
                    30,
                    false,
                    new WorldPoint(3088, 3234, 0)
            ),
            new RouteProfile(
                    "Seers Village Maples",
                    TreeType.MAPLE_TREE,
                    new WorldArea(2721, 3499, 12, 4, 0),
                    new WorldPoint(2727, 3501, 0),
                    "Seers Village Bank",
                    new WorldArea(2726, 3491, 4, 4, 0),
                    new WorldPoint(2726, 3489, 0),
                    new WorldPoint(2727, 3492, 0),
                    new WorldPoint(2727, 3493, 0),
                    List.of(),
                    List.of(25808),
                    List.of(new WorldPoint(2727, 3493, 0)),
                    List.of("Bank"),
                    45,
                    false,
                    new WorldPoint(2727, 3501, 0)
            ),
            new RouteProfile(
                    "Woodcutting Guild Yews",
                    TreeType.YEW_TREE,
                    new WorldArea(1582, 3478, 14, 15, 0),
                    new WorldPoint(1589, 3482, 0),
                    "Woodcutting Guild Bank Chest",
                    new WorldArea(1590, 3473, 4, 4, 0),
                    new WorldPoint(1591, 3479, 0),
                    new WorldPoint(1591, 3477, 0),
                    new WorldPoint(1592, 3475, 0),
                    List.of(
                            new WorldPoint(1591, 3479, 0),
                            new WorldPoint(1591, 3478, 0),
                            new WorldPoint(1591, 3477, 0),
                            new WorldPoint(1591, 3476, 0)
                    ),
                    List.of(REASON_WOODCUTTING_GUILD_BANK_CHEST_ID),
                    List.of(new WorldPoint(1592, 3475, 0)),
                    List.of("Use", "Bank"),
                    60,
                    false,
                    new WorldPoint(1590, 3481, 0)
            ),
            new RouteProfile(
                    "Woodcutting Guild Magics",
                    TreeType.MAGIC_TREE,
                    new WorldArea(1576, 3481, 6, 6, 0),
                    new WorldPoint(1579, 3483, 0),
                    "Woodcutting Guild Bank Chest",
                    new WorldArea(1590, 3473, 4, 4, 0),
                    new WorldPoint(1591, 3479, 0),
                    new WorldPoint(1591, 3477, 0),
                    new WorldPoint(1592, 3475, 0),
                    List.of(
                            new WorldPoint(1591, 3479, 0),
                            new WorldPoint(1591, 3478, 0),
                            new WorldPoint(1591, 3477, 0),
                            new WorldPoint(1591, 3476, 0)
                    ),
                    List.of(REASON_WOODCUTTING_GUILD_BANK_CHEST_ID),
                    List.of(new WorldPoint(1592, 3475, 0)),
                    List.of("Use", "Bank"),
                    75,
                    false,
                    new WorldPoint(1580, 3482, 0)
            )
    );

    public static List<RouteProfile> routesFor(TreeType treeType) {
        return ROUTES.stream()
                .filter(route -> route.treeType() == treeType)
                .collect(Collectors.toList());
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

    private WoodcuttingProfile() {
    }
}
