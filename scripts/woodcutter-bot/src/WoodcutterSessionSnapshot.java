package reason.woodcutter;

public final class WoodcutterSessionSnapshot {
    private final String status;
    private final String routeLabel;
    private final String bankLabel;
    private final String runtimeText;
    private final int logsCut;
    private final String lastAction;
    private final int currentLevel;
    private final int totalXp;
    private final int gainedXp;
    private final int xpPerHour;
    private final int xpToNextLevel;
    private final String playerTile;

    public WoodcutterSessionSnapshot(
            String status,
            String routeLabel,
            String bankLabel,
            String runtimeText,
            int logsCut,
            String lastAction,
            int currentLevel,
            int totalXp,
            int gainedXp,
            int xpPerHour,
            int xpToNextLevel,
            String playerTile
    ) {
        this.status = status;
        this.routeLabel = routeLabel;
        this.bankLabel = bankLabel;
        this.runtimeText = runtimeText;
        this.logsCut = logsCut;
        this.lastAction = lastAction;
        this.currentLevel = currentLevel;
        this.totalXp = totalXp;
        this.gainedXp = gainedXp;
        this.xpPerHour = xpPerHour;
        this.xpToNextLevel = xpToNextLevel;
        this.playerTile = playerTile;
    }

    public String status() { return status; }
    public String routeLabel() { return routeLabel; }
    public String bankLabel() { return bankLabel; }
    public String runtimeText() { return runtimeText; }
    public int logsCut() { return logsCut; }
    public String lastAction() { return lastAction; }
    public int currentLevel() { return currentLevel; }
    public int totalXp() { return totalXp; }
    public int gainedXp() { return gainedXp; }
    public int xpPerHour() { return xpPerHour; }
    public int xpToNextLevel() { return xpToNextLevel; }
    public String playerTile() { return playerTile; }

    public static WoodcutterSessionSnapshot idle(WoodcutterSettings settings) {
        return new WoodcutterSessionSnapshot(
                "Idle",
                settings.routeProfile().label(),
                settings.routeProfile().bankLabel(),
                "00:00:00",
                0,
                "waiting",
                0,
                0,
                0,
                0,
                0,
                "-"
        );
    }
}
