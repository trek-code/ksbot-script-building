package local.reason.woodcutter;

import java.util.List;
import java.util.Objects;

public final class WoodcutterSettings {
    public enum RuntimeLimit {
        NO_LIMIT("No Limit", 0),
        THIRTY_MINUTES("30 Minutes", 30),
        ONE_HOUR("1 Hour", 60),
        TWO_HOURS("2 Hours", 120);

        private final String label;
        private final int minutes;

        RuntimeLimit(String label, int minutes) {
            this.label = label;
            this.minutes = minutes;
        }

        public int minutes() {
            return minutes;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum DropType {
        DROP_ALL("Drop all logs from inventory when full"),
        DROP_SOME_RANDOM("Drop some logs (8-24 logs randomised)");

        private final String label;

        DropType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum DropSpeed {
        FAST("Fast"),
        HUMAN_LIKE("Human-like");

        private final String label;

        DropSpeed(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum LootingStyle {
        IMMEDIATE("Immediate"),
        STACK("Stack");

        private final String label;

        LootingStyle(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final WoodcuttingProfile.TreeType treeType;
    private final WoodcuttingProfile.RouteProfile routeProfile;
    private final WoodcuttingProfile.Mode mode;
    private final boolean useTeleportToBank;
    private final boolean requireAxe;
    private final boolean useClosestTree;
    private final boolean safeStopOnStaff;
    private final int stopAtLevel;
    private final RuntimeLimit runtimeLimit;
    private final DropType dropType;
    private final DropSpeed dropSpeed;
    private final boolean antiBanEnabled;
    private final int antiBanDelayMinMs;
    private final int antiBanDelayMaxMs;
    private final boolean lootingEnabled;
    private final LootingStyle lootingStyle;
    private final List<String> availableLootNames;
    private final List<String> activeLootNames;

    public WoodcutterSettings(
            WoodcuttingProfile.TreeType treeType,
            WoodcuttingProfile.RouteProfile routeProfile,
            WoodcuttingProfile.Mode mode,
            boolean useTeleportToBank,
            boolean requireAxe,
            boolean useClosestTree,
            boolean safeStopOnStaff,
            int stopAtLevel,
            RuntimeLimit runtimeLimit,
            DropType dropType,
            DropSpeed dropSpeed,
            boolean antiBanEnabled,
            int antiBanDelayMinMs,
            int antiBanDelayMaxMs,
            boolean lootingEnabled,
            LootingStyle lootingStyle,
            List<String> availableLootNames,
            List<String> activeLootNames
    ) {
        this.treeType = Objects.requireNonNull(treeType, "treeType");
        this.routeProfile = Objects.requireNonNull(routeProfile, "routeProfile");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.useTeleportToBank = useTeleportToBank;
        this.requireAxe = requireAxe;
        this.useClosestTree = useClosestTree;
        this.safeStopOnStaff = safeStopOnStaff;
        this.stopAtLevel = normalizeStopAtLevel(stopAtLevel);
        this.runtimeLimit = Objects.requireNonNull(runtimeLimit, "runtimeLimit");
        this.dropType = Objects.requireNonNull(dropType, "dropType");
        this.dropSpeed = Objects.requireNonNull(dropSpeed, "dropSpeed");
        this.antiBanEnabled = antiBanEnabled;
        this.antiBanDelayMinMs = normalizeDelayMin(antiBanDelayMinMs);
        this.antiBanDelayMaxMs = normalizeDelayMax(this.antiBanDelayMinMs, antiBanDelayMaxMs);
        this.lootingEnabled = lootingEnabled;
        this.lootingStyle = Objects.requireNonNull(lootingStyle, "lootingStyle");
        this.availableLootNames = List.copyOf(Objects.requireNonNull(availableLootNames, "availableLootNames"));
        this.activeLootNames = List.copyOf(Objects.requireNonNull(activeLootNames, "activeLootNames"));
    }

    public static WoodcutterSettings defaults() {
        WoodcuttingProfile.TreeType treeType = WoodcuttingProfile.TreeType.TREE;
        return new WoodcutterSettings(
                treeType,
                Objects.requireNonNull(WoodcuttingProfile.defaultRouteFor(treeType), "defaultRoute"),
                WoodcuttingProfile.Mode.POWERCUT,
                false,
                true,
                true,
                true,
                0,
                RuntimeLimit.NO_LIMIT,
                DropType.DROP_ALL,
                DropSpeed.HUMAN_LIKE,
                false,
                250,
                750,
                false,
                LootingStyle.IMMEDIATE,
                List.of(),
                List.of()
        );
    }

    private static int normalizeStopAtLevel(int stopAtLevel) {
        if (stopAtLevel <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(99, stopAtLevel));
    }

    private static int normalizeDelayMin(int value) {
        return Math.max(50, value);
    }

    private static int normalizeDelayMax(int min, int value) {
        return Math.max(min, value);
    }

    public WoodcuttingProfile.TreeType treeType() { return treeType; }
    public WoodcuttingProfile.RouteProfile routeProfile() { return routeProfile; }
    public WoodcuttingProfile.Mode mode() { return mode; }
    public boolean useTeleportToBank() { return useTeleportToBank; }
    public boolean requireAxe() { return requireAxe; }
    public boolean useClosestTree() { return useClosestTree; }
    public boolean safeStopOnStaff() { return safeStopOnStaff; }
    public int stopAtLevel() { return stopAtLevel; }
    public boolean hasStopAtLevelGoal() { return stopAtLevel > 0; }
    public RuntimeLimit runtimeLimit() { return runtimeLimit; }
    public DropType dropType() { return dropType; }
    public DropSpeed dropSpeed() { return dropSpeed; }
    public boolean antiBanEnabled() { return antiBanEnabled; }
    public int antiBanDelayMinMs() { return antiBanDelayMinMs; }
    public int antiBanDelayMaxMs() { return antiBanDelayMaxMs; }
    public boolean lootingEnabled() { return lootingEnabled; }
    public LootingStyle lootingStyle() { return lootingStyle; }
    public List<String> availableLootNames() { return availableLootNames; }
    public List<String> activeLootNames() { return activeLootNames; }
    public boolean bankingEnabled() { return mode == WoodcuttingProfile.Mode.BANK; }
}
