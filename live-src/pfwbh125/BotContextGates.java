package pfwbh125;

/**
 * Server-agnostic "is it safe to break right now?" checks consumed by
 * {@link BreakManager}. Each per-server bot (Reason, Near Reality, ...)
 * implements this against its own state machine.
 * <p>
 * Breaks should only fire when the bot is in a state the user can safely
 * resume from. These gates model "don't break mid-fight", "don't break with
 * a bag of loot undeposited", "don't log out next to a staff member", etc.
 */
public interface BotContextGates {

    /** True if actively in combat / taking damage. Break would lose progress. */
    boolean isInCombat();

    /** True if inventory is full / we need to bank before stopping.
     *  A break here would look like the player vanished mid-pile. */
    boolean isInventoryFullOfLoot();

    /** True if a perk-task (or equivalent server task) is mid-progress. */
    boolean isMidTask();

    /** True if the bot is in a safe, resumable state (idle / walking / banking). */
    boolean isInSafeState();

    /** True if a known staff member is within the default proximity radius. */
    boolean isStaffNearby();

    /** True if player HP is below the configured low-HP threshold. */
    boolean isLowHp();
}
