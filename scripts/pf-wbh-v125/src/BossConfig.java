package pfwbh125;

import net.runelite.api.coords.WorldPoint;

/**
 * Data record for a single Reason world-event boss. Immutable once built.
 * <p>
 * Each boss defines:
 * <ul>
 *   <li>Detection — substring(s) that appear in the chat notification</li>
 *   <li>Transport — server command to teleport, arrival tile, waypoints to boss</li>
 *   <li>Combat — NPC name / ID, prayers to activate</li>
 *   <li>Return — teleport command back + which bank-preset to reload</li>
 * </ul>
 */
public final class BossConfig {

    // ── Fields ────────────────────────────────────────────────────────────
    public final String      displayName;
    /** Lower-case substring(s) to look for in chat messages. */
    public final String[]    chatTriggers;
    /** Server command WITHOUT the "::" prefix (e.g. "sb", "vb", "gb"). */
    public final String      teleportCommand;
    /** Tile where the teleport drops you. Used to detect arrival. */
    public final WorldPoint  arrivalTile;
    /** Walk path from arrival to the boss spawn area (may be empty). */
    public final WorldPoint[] waypoints;
    /** NPC name to look for (case-insensitive). */
    public final String      npcName;
    /** NPC ID — 0 means "match by name only". */
    public final int         npcId;
    /** How long (ms) to wait near the spawn before giving up. */
    public final long        spawnTimeoutMs;
    /** How long (ms) the fight may last before we bail. */
    public final long        fightTimeoutMs;

    // ── AOE dodge config (empty arrays = no dodge logic runs) ────────────
    /** Boss animation IDs that signal an incoming AOE (e.g. Stone Poltegeist 1842). */
    public final int[]       dangerousAnimationIds;
    /** Graphics-object IDs that mark a live AOE on the ground (e.g. 305). */
    public final int[]       dangerousAoeGraphicIds;
    /** Radius (in tiles) of the AOE footprint centered on its target.
     *  5x5 = radius 2, 7x7 = radius 3, etc. */
    public final int         aoeFootprintRadius;
    /** How many tiles to step away when dodging. */
    public final int         dodgeDistance;

    // ── Low-player bail config ───────────────────────────────────────────
    /** Minimum players in radius to commit to this fight (0 disables check). */
    public final int         minPlayersForFight;
    /** Radius (tiles) to count players for the min-player check. */
    public final int         playerCheckRadius;
    /** Minimum tiles to stay from boss (passive — avoid pathing closer). */
    public final int         preferredBossDistanceMin;

    // ── Pre-built configs ─────────────────────────────────────────────────

    /** Vote Boss — NPC 8262, teleport ::vb, Protect from Magic + Eagle Eye.
     *  Only triggers on the 60-second countdown message, not vote-count updates. */
    public static final BossConfig VOTE_BOSS = new BossConfig(
            "Vote Boss",
            new String[]{"vote boss will spawn in 60", "vote boss spawning in 60",
                          "vote boss will spawn in 1 minute"},
            "vb",
            new WorldPoint(1439, 5329, 1),
            new WorldPoint[]{
                    new WorldPoint(1439, 5329, 1),
                    new WorldPoint(1439, 5335, 1),
                    new WorldPoint(1440, 5341, 1),
            },
            "Vote Boss", 8262,
            120_000, 180_000,
            new int[0], new int[0], 0, 0,          // no AOE data yet
            5, 16, 0                                // bail if <5 nearby, 16-tile radius
    );

    /** Blood Reaper (Global boss variant A) — NPC 11895, teleport ::gb.
     *  Shares ::gb with Stone Poltegeist; distinguish by boss name in chat.
     *  Only triggers on the 60-second countdown message. */
    public static final BossConfig BLOOD_REAPER = new BossConfig(
            "Blood Reaper",
            new String[]{"blood reaper will spawn in 60", "blood reaper spawning in 60",
                          "blood reaper will spawn in 1 minute"},
            "gb",
            new WorldPoint(3489, 11992, 0),
            new WorldPoint[]{
                    new WorldPoint(3489, 11992, 0),
                    new WorldPoint(3489, 11998, 0),
                    new WorldPoint(3489, 12004, 0),
            },
            "Blood Reaper", 11895,
            120_000, 180_000,
            new int[0], new int[0], 0, 0,          // no AOE data yet
            5, 16, 0
    );

    /** Stone Poltegeist (Global boss variant B) — NPC 11896, teleport ::gb.
     *  Shares ::gb with Blood Reaper; distinguish by boss name in chat. */
    public static final BossConfig STONE_POLTEGEIST = new BossConfig(
            "Stone Poltegeist",
            new String[]{"stone poltegeist will spawn in 60", "stone poltegeist spawning in 60",
                          "stone poltegeist will spawn in 1 minute",
                          "stone poltergeist will spawn in 60", "stone poltergeist spawning in 60",
                          "stone poltergeist will spawn in 1 minute"},
            "gb",
            new WorldPoint(4015, 12258, 1),
            new WorldPoint[]{
                    new WorldPoint(4015, 12258, 1),
                    new WorldPoint(4007, 12258, 1),
                    new WorldPoint(4001, 12258, 1),
                    new WorldPoint(3996, 12258, 1),
                    new WorldPoint(3988, 12258, 1),
            },
            "Stone Poltegeist", 11896,
            120_000, 180_000,
            new int[]{1842},       // boss animation 1842 = AOE cast
            new int[]{305},        // graphic 305 = AOE marker (5x5 around target)
            2,                     // 5x5 footprint = radius 2
            4,                     // dodge 4 tiles
            5, 16, 5               // min 5 players, 16-tile radius, stay ≥5 tiles from boss
    );

    /** Donation Boss — NPC 1787, teleport ::db.
     *  Only triggers on the 60-second countdown message. */
    public static final BossConfig DONATION_BOSS = new BossConfig(
            "Donation Boss",
            new String[]{"donation boss will spawn in 60", "donation boss spawning in 60",
                          "donation boss will spawn in 1 minute"},
            "db",
            new WorldPoint(1236, 5727, 0),
            new WorldPoint[]{
                    new WorldPoint(1236, 5727, 0),
                    new WorldPoint(1242, 5727, 0),
                    new WorldPoint(1248, 5727, 0),
                    new WorldPoint(1254, 5726, 0),
            },
            "Donation Boss", 1787,
            120_000, 180_000,
            new int[0], new int[0], 0, 0,
            5, 16, 0
    );

    /**
     * Spring Boss — NPC name and ID unknown, teleport ::sb.
     * Enabled once the user captures the NPC info.
     */
    public static final BossConfig SPRING_BOSS = new BossConfig(
            "Spring Boss",
            new String[]{"spring boss will spawn in 60", "spring boss spawning in 60",
                          "spring boss will spawn in 1 minute"},
            "sb",
            new WorldPoint(3870, 6123, 0),
            new WorldPoint[]{
                    new WorldPoint(3870, 6123, 0),
                    new WorldPoint(3870, 6129, 0),
                    new WorldPoint(3870, 6135, 0),
            },
            "Big Chungus", 17189,
            120_000, 180_000,
            new int[0], new int[0], 0, 0,
            5, 16, 0
    );

    /** Waypoints from ::dz landing tile back to the Donator Zone bank area.
     *  Used by the world-boss handler's RETURN phase.
     *  Two mid-path waypoints only — bank chest click handles the final step. */
    public static final WorldPoint[] DZ_RETURN_WAYPOINTS = {
            new WorldPoint(1257, 2601, 2),   // first leg: head SE from landing
            new WorldPoint(1268, 2585, 2),   // second leg: approach bank area
    };

    // ── Constructor ───────────────────────────────────────────────────────
    public BossConfig(String displayName, String[] chatTriggers,
                      String teleportCommand, WorldPoint arrivalTile,
                      WorldPoint[] waypoints, String npcName, int npcId,
                      long spawnTimeoutMs, long fightTimeoutMs,
                      int[] dangerousAnimationIds, int[] dangerousAoeGraphicIds,
                      int aoeFootprintRadius, int dodgeDistance,
                      int minPlayersForFight, int playerCheckRadius,
                      int preferredBossDistanceMin) {
        this.displayName     = displayName;
        this.chatTriggers    = chatTriggers;
        this.teleportCommand = teleportCommand;
        this.arrivalTile     = arrivalTile;
        this.waypoints       = waypoints;
        this.npcName         = npcName;
        this.npcId           = npcId;
        this.spawnTimeoutMs  = spawnTimeoutMs;
        this.fightTimeoutMs  = fightTimeoutMs;
        this.dangerousAnimationIds  = dangerousAnimationIds  == null ? new int[0] : dangerousAnimationIds;
        this.dangerousAoeGraphicIds = dangerousAoeGraphicIds == null ? new int[0] : dangerousAoeGraphicIds;
        this.aoeFootprintRadius     = aoeFootprintRadius;
        this.dodgeDistance          = dodgeDistance;
        this.minPlayersForFight     = minPlayersForFight;
        this.playerCheckRadius      = playerCheckRadius <= 0 ? 16 : playerCheckRadius;
        this.preferredBossDistanceMin = preferredBossDistanceMin;
    }

    /** True if any trigger substring appears in the (lower-cased) message. */
    public boolean matchesChat(String lowerMsg) {
        for (String trigger : chatTriggers) {
            if (lowerMsg.contains(trigger)) return true;
        }
        return false;
    }
}
