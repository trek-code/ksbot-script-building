package reason.woodcutter;

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

        public int minutes() { return minutes; }

        @Override
        public String toString() {
            return label;
        }
    }

    private final WoodcuttingProfile.TreeType treeType;
    private final WoodcuttingProfile.RouteProfile routeProfile;
    private final WoodcuttingProfile.Mode mode;
    private final boolean requireAxe;
    private final boolean useClosestTree;
    private final int stopAtLevel;
    private final RuntimeLimit runtimeLimit;

    public WoodcutterSettings(
            WoodcuttingProfile.TreeType treeType,
            WoodcuttingProfile.RouteProfile routeProfile,
            WoodcuttingProfile.Mode mode,
            boolean requireAxe,
            boolean useClosestTree,
            int stopAtLevel,
            RuntimeLimit runtimeLimit
    ) {
        this.treeType = Objects.requireNonNull(treeType, "treeType");
        this.routeProfile = Objects.requireNonNull(routeProfile, "routeProfile");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.requireAxe = requireAxe;
        this.useClosestTree = useClosestTree;
        this.stopAtLevel = normalizeStopAtLevel(stopAtLevel);
        this.runtimeLimit = Objects.requireNonNull(runtimeLimit, "runtimeLimit");
    }

    public static WoodcutterSettings defaults() {
        WoodcuttingProfile.TreeType treeType = defaultTreeType();
        WoodcuttingProfile.RouteProfile routeProfile = Objects.requireNonNull(
                WoodcuttingProfile.defaultRouteFor(treeType),
                "defaultRoute"
        );
        return new WoodcutterSettings(
                treeType,
                routeProfile,
                WoodcuttingProfile.Mode.POWERCUT,
                true,
                true,
                0,
                RuntimeLimit.NO_LIMIT
        );
    }

    private static WoodcuttingProfile.TreeType defaultTreeType() {
        for (WoodcuttingProfile.TreeType treeType : WoodcuttingProfile.TreeType.values()) {
            if (WoodcuttingProfile.defaultRouteFor(treeType) != null) {
                return treeType;
            }
        }
        throw new IllegalStateException("No woodcutting routes are available.");
    }

    private static int normalizeStopAtLevel(int value) {
        if (value <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(99, value));
    }

    public WoodcuttingProfile.TreeType treeType() { return treeType; }
    public WoodcuttingProfile.RouteProfile routeProfile() { return routeProfile; }
    public WoodcuttingProfile.Mode mode() { return mode; }
    public boolean requireAxe() { return requireAxe; }
    public boolean useClosestTree() { return useClosestTree; }
    public int stopAtLevel() { return stopAtLevel; }
    public boolean hasStopAtLevelGoal() { return stopAtLevel > 0; }
    public RuntimeLimit runtimeLimit() { return runtimeLimit; }
    public boolean bankingEnabled() { return mode == WoodcuttingProfile.Mode.BANK; }
}
