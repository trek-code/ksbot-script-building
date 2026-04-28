import rs.kreme.ksbot.api.commons.Random;
import rs.kreme.ksbot.api.scripts.Category;
import rs.kreme.ksbot.api.scripts.Script;
import rs.kreme.ksbot.api.scripts.ScriptManifest;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/**
 * KSBot skilling starter that matches the documented script lifecycle.
 *
 * Replace the placeholder decisions with real ctx-driven logic after the target
 * RSPS and KSBot behaviors are mapped.
 */
@ScriptManifest(
        name = "Skilling Bot Template",
        author = "Codex",
        servers = {"TARGET_RSPS"},
        description = "Starter KSBot skilling template with states, retries, and recovery.",
        version = 1.0,
        category = Category.OTHER
)
public class SkillingBotTemplate extends Script {
    private final BotConfig config = new BotConfig();
    private final Map<BotState, Integer> stateFailures = new EnumMap<>(BotState.class);

    private BotState state = BotState.INITIALIZE;
    private Instant stateEnteredAt = Instant.now();
    private String lastAction = "boot";
    private int stuckChecks;

    @Override
    public boolean onStart() {
        log("INFO", "Starting " + getManifest().name() + " for " + String.join(",", getManifest().servers()));
        transitionTo(BotState.INITIALIZE);
        return true;
    }

    @Override
    public int onProcess() {
        try {
            if (isPaused() || isOnBreak()) {
                return randomBetween(600, 1200);
            }

            updateState();
            executeState();
            return randomBetween(config.minLoopDelayMs, config.maxLoopDelayMs);
        } catch (RuntimeException ex) {
            registerFailure("Loop exception: " + ex.getMessage());
            recoverOrStop("Unhandled loop exception");
            return randomBetween(config.minFailureDelayMs, config.maxFailureDelayMs);
        }
    }

    @Override
    public void onStop() {
        log("INFO", "Stopping bot. Last action=" + lastAction + ", state=" + state);
    }

    private void updateState() {
        if (shouldStop()) {
            transitionTo(BotState.STOP);
            return;
        }

        switch (state) {
            case INITIALIZE:
                transitionTo(BotState.NAVIGATE);
                break;
            case NAVIGATE:
                if (isAtWorkArea()) {
                    transitionTo(BotState.INTERACT);
                }
                break;
            case INTERACT:
                if (inventoryFull()) {
                    transitionTo(BotState.BANK);
                } else if (needsRecoveryCheck()) {
                    transitionTo(BotState.RECOVER);
                }
                break;
            case BANK:
                if (!inventoryFull()) {
                    transitionTo(BotState.NAVIGATE);
                }
                break;
            case RECOVER:
                if (recovered()) {
                    transitionTo(BotState.NAVIGATE);
                }
                break;
            case STOP:
                break;
        }
    }

    private void executeState() {
        switch (state) {
            case INITIALIZE:
                lastAction = "prepare-runtime";
                ensureRequirements();
                break;
            case NAVIGATE:
                lastAction = "walk-to-work-area";
                performNavigation();
                break;
            case INTERACT:
                lastAction = "skill-action";
                performInteraction();
                break;
            case BANK:
                lastAction = "banking";
                handleBanking();
                break;
            case RECOVER:
                lastAction = "recover";
                attemptRecovery();
                break;
            case STOP:
                lastAction = "shutdown";
                stopBot("Stop condition reached");
                break;
        }
    }

    private void transitionTo(BotState newState) {
        if (state != newState) {
            log("STATE", state + " -> " + newState);
            state = newState;
            stateEnteredAt = Instant.now();
        }
    }

    private void ensureRequirements() {
        // Validate startup assumptions here:
        // - account is in the correct location
        // - required inventory or equipment is present
        // - route data and object IDs match the target RSPS
    }

    private void performNavigation() {
        // Use ctx.pathing once the target route is mapped. KSBot documents:
        // - walkPoint(WorldPoint)
        // - walkToTile(WorldPoint)
        // - shortestPath(start, end)
        // - walkPath(path)
        if (stateDuration().toSeconds() > config.maxNavigateSeconds) {
            registerFailure("Navigation timeout");
            transitionTo(BotState.RECOVER);
        }
    }

    private void performInteraction() {
        // Keep interaction logic query-first:
        // - ctx.gameObjects.query()
        // - ctx.npcs.query()
        // - ctx.inventory.query()
        if (noProgressObserved()) {
            stuckChecks++;
            if (stuckChecks >= config.maxStuckChecks) {
                registerFailure("No interaction progress");
                transitionTo(BotState.RECOVER);
            }
        } else {
            stuckChecks = 0;
        }
    }

    private void handleBanking() {
        // Typical KSBot bank flow from the docs:
        // 1. Open/validate bank widget state.
        // 2. depositInventory() / depositAllExcept(...)
        // 3. withdraw(item, amount, withdrawMode)
        // 4. verify inventory after each action.
    }

    private void attemptRecovery() {
        // Recovery ideas:
        // - reopen the inventory or skill tab with ctx.game.openTab(...)
        // - re-query the target object or NPC
        // - re-path to the work area
        // - stop if staff are nearby or the state keeps failing
        if (stateDuration().toSeconds() > config.maxRecoverSeconds) {
            stopBot("Recovery timeout");
        }
    }

    private void recoverOrStop(String reason) {
        if (failuresForState(state) >= config.maxFailuresPerState) {
            stopBot(reason);
        } else {
            transitionTo(BotState.RECOVER);
        }
    }

    private void registerFailure(String reason) {
        stateFailures.merge(state, 1, Integer::sum);
        log("WARN", reason + " (state failures=" + failuresForState(state) + ")");
    }

    private int failuresForState(BotState currentState) {
        return stateFailures.getOrDefault(currentState, 0);
    }

    private Duration stateDuration() {
        return Duration.between(stateEnteredAt, Instant.now());
    }

    private boolean shouldStop() {
        return false;
    }

    private boolean isAtWorkArea() {
        return false;
    }

    private boolean inventoryFull() {
        return false;
    }

    private boolean needsRecoveryCheck() {
        return false;
    }

    private boolean recovered() {
        return false;
    }

    private boolean noProgressObserved() {
        return false;
    }

    private void stopBot(String reason) {
        log("INFO", "Stop requested: " + reason);
    }

    private void log(String level, String message) {
        System.out.println("[" + level + "] " + message);
    }

    private int randomBetween(int min, int max) {
        return Random.nextInt(min, max);
    }

    private enum BotState {
        INITIALIZE,
        NAVIGATE,
        INTERACT,
        BANK,
        RECOVER,
        STOP
    }

    public static class BotConfig {
        public String profileName = "default";
        public int maxNavigateSeconds = 45;
        public int maxRecoverSeconds = 30;
        public int maxStuckChecks = 3;
        public int maxFailuresPerState = 3;
        public int minLoopDelayMs = 350;
        public int maxLoopDelayMs = 700;
        public int minFailureDelayMs = 800;
        public int maxFailureDelayMs = 1400;
    }
}
