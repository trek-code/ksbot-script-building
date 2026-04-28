package rsperkfarmerclworldbosshandlertest;

import net.runelite.api.coords.WorldPoint;
import rs.kreme.ksbot.api.game.Prayer;
import rs.kreme.ksbot.api.wrappers.KSGroundItem;
import rs.kreme.ksbot.api.wrappers.KSItem;
import rs.kreme.ksbot.api.wrappers.KSNPC;
import rs.kreme.ksbot.api.wrappers.KSObject;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Reusable world-boss event handler for Reason scripts.
 * <p>
 * Watches chat for boss notifications, handles the full lifecycle
 * (preset swap, teleport, potion, prayer, fight, loot, return, re-gear),
 * then yields control back to the host script.
 */
public final class ReasonWorldBossHandler {

    // ── Sub-states ────────────────────────────────────────────────────────
    public enum Phase {
        IDLE,            // No boss event active
        BANK_PRESET,     // Load "world boss" preset from bank
        TELEPORT,        // Type ::XX teleport command
        WALK_TO_BOSS,    // Walk waypoints to boss spawn
        WAIT_SPAWN,      // Idle near spawn until NPC appears
        DRINK_POTION,    // Drink Superior Potion
        PRAY_UP,         // Activate Protect from Magic + Eagle Eye
        FIGHT,           // Attack boss until dead
        LOOT,            // Grab ground items
        RETURN,          // ::dz back to Donator Zone
        RETURN_WALK,     // Walk from ::dz landing to bank chest
        REGEAR,          // Load "mining" preset from bank
        DONE             // Hand back to host
    }

    /** Superior Potion (Reason custom overload for range, doesn't damage player). */
    private static final int SUPERIOR_POTION_ID = 30594;
    private static final String SUPERIOR_POTION_NAME = "Superior potion";

    /** Command to return to the Donator Zone after the kill. */
    private static final String RETURN_COMMAND = "dz";

    /** Bank-chest ID in the Donator Zone (same as RouteProfile). */
    private static final int BANK_CHEST_ID = RouteProfile.BANK_CHEST_ID;

    private static final int MAX_LOOT_SWEEPS = 6;
    /** Wait this long (ms) after boss dies before first loot scan, so ground items appear. */
    private static final long LOOT_SETTLE_DELAY_MS = 3000;
    /** Boss loot drops on the player's tile — anything further than this is
     *  NOT our loot (old arrows, other players' drops, etc.) and must be
     *  ignored to avoid picking up junk left behind on the ground. */
    private static final int LOOT_MAX_DISTANCE = 2;
    private static final int MAX_FAILS       = 8;
    /** Minimum prayer points before we sip a super restore mid-fight. */
    private static final int PRAYER_LOW_THRESHOLD = 20;

    // ── Configuration ─────────────────────────────────────────────────────
    private final RsPerkFarmerBot bot;
    private final Map<String, BossConfig> registeredBosses = new LinkedHashMap<>();
    private final Set<String> enabledBossKeys = new HashSet<>();
    private volatile boolean globalEnabled = false;
    /** Which preset slot (1-5) to load before heading to the boss. Default = 1. */
    private volatile int bossPresetSlot = 1;

    // ── Loot tracking ─────────────────────────────────────────────────────
    /** Item name → total quantity picked up across all boss kills this session. */
    private final Map<String, Integer> sessionLoot =
            Collections.synchronizedMap(new LinkedHashMap<>());

    // ── Per-boss stats ────────────────────────────────────────────────────
    public static final class BossStats {
        public volatile int attempted = 0;
        public volatile int killed    = 0;
        public volatile int bailed    = 0;
        public volatile int deaths    = 0;
        public volatile int aoeDodges = 0;
    }
    /** Boss displayName → stats. Also totals under key "__total__". */
    private final Map<String, BossStats> bossStats =
            Collections.synchronizedMap(new LinkedHashMap<>());
    /** Live nearby-player count (updated every tick the script runs). */
    private volatile int nearbyPlayerCount = 0;

    // ── Runtime state ─────────────────────────────────────────────────────
    private volatile Phase       phase       = Phase.IDLE;
    private volatile BossConfig  activeBoss  = null;
    private volatile long        phaseStartMs = 0L;
    private volatile int         waypointIdx  = 0;
    private volatile int         returnWpIdx  = 0;
    private volatile int         consecutiveFails = 0;
    private volatile int         lootSweeps   = 0;
    /** Sub-step within BANK_PRESET / REGEAR:
     *  0 = need to open preset UI, 1 = need to load slot, 2 = need to close */
    private volatile int         presetStep   = 0;
    /** Which preset slot the current BANK_PRESET/REGEAR phase is targeting. */
    private volatile int         presetSlotTarget = 0;
    /** Which Phase to enter after preset completes. */
    private volatile Phase       presetNextPhase  = Phase.DONE;
    /** Inventory snapshot taken right before LOOT phase so we can diff. */
    private volatile Map<String, Integer> preLootInventory = new HashMap<>();

    // ── Fight-phase tracking (for bail + dodge) ──────────────────────────
    /** When FIGHT phase started (ms epoch). 0 = not in FIGHT. */
    private volatile long fightStartedAt = 0L;
    /** Random grace period before low-player bail (14-28 game ticks * 600 ms). */
    private volatile long bailGraceMs = 0L;
    /** True once we've bailed this fight — skip loot on the way out. */
    private volatile boolean bailedThisFight = false;
    /** Throttle AOE dodges to avoid spam-walking every tick we're in the marker. */
    private volatile long lastDodgeAtMs = 0L;
    private static final long DODGE_COOLDOWN_MS = 1500;

    // ── Pre-spawn prep (pray + pot before boss appears) ───────────────────
    /** True once we've prayed + potted this spawn cycle. */
    private volatile boolean preSpawnPrepped = false;
    /** Time (epoch ms) after which we do the pre-spawn prep. Set on WAIT_SPAWN entry.
     *  Random 10-15s after arriving — human-like "settle in then prepare". */
    private volatile long preSpawnPrepAtMs = 0L;

    // ── Construction + registration ───────────────────────────────────────
    public ReasonWorldBossHandler(RsPerkFarmerBot bot) {
        this.bot = bot;
    }

    public ReasonWorldBossHandler register(BossConfig cfg) {
        String key = cfg.displayName.toLowerCase();
        registeredBosses.put(key, cfg);
        enabledBossKeys.add(key);
        bossStats.putIfAbsent(cfg.displayName, new BossStats());
        return this;
    }

    /** Per-boss stats map (displayName -> stats). */
    public Map<String, BossStats> getBossStats() {
        synchronized (bossStats) { return new LinkedHashMap<>(bossStats); }
    }

    private BossStats statsFor(BossConfig cfg) {
        return bossStats.computeIfAbsent(cfg.displayName, k -> new BossStats());
    }

    /** Live count of players within the active boss's check radius (or 16 when idle). */
    public int getNearbyPlayerCount() { return nearbyPlayerCount; }

    /** Recompute the nearby player count. Called from the bot every tick. */
    public void refreshNearbyPlayerCount() {
        int radius = activeBoss != null && activeBoss.playerCheckRadius > 0
                ? activeBoss.playerCheckRadius : 16;
        try {
            int count = 0;
            try {
                java.util.List<?> players = bot.ctx.players.query()
                        .withinDistance(radius).list();
                count = players == null ? 0 : players.size();
            } catch (Throwable ignored) {}
            // Subtract the local player (queries usually include self).
            if (count > 0) count -= 1;
            nearbyPlayerCount = Math.max(0, count);
        } catch (Throwable ignored) {}
    }

    // ── Global + per-boss enable/disable ──────────────────────────────────
    public boolean isGlobalEnabled()            { return globalEnabled; }
    public void    setGlobalEnabled(boolean on)  { this.globalEnabled = on; }

    public int  getBossPresetSlot()        { return bossPresetSlot; }
    public void setBossPresetSlot(int slot) {
        if (slot < 1) slot = 1;
        if (slot > 5) slot = 5;
        this.bossPresetSlot = slot;
    }

    public boolean isBossEnabled(String displayName) {
        return enabledBossKeys.contains(displayName.toLowerCase());
    }
    public void setBossEnabled(String displayName, boolean on) {
        String key = displayName.toLowerCase();
        if (on) enabledBossKeys.add(key); else enabledBossKeys.remove(key);
    }

    public Collection<BossConfig> getRegisteredBosses() {
        return registeredBosses.values();
    }

    public Map<String, Integer> getSessionLoot() {
        synchronized (sessionLoot) {
            return new LinkedHashMap<>(sessionLoot);
        }
    }

    public void clearSessionLoot() { sessionLoot.clear(); }

    // ── Chat event hook ──────────────────────────────────────────────────
    public void onChatMessage(String cleanLower) {
        if (!globalEnabled || phase != Phase.IDLE) return;
        for (BossConfig cfg : registeredBosses.values()) {
            String key = cfg.displayName.toLowerCase();
            if (!enabledBossKeys.contains(key)) continue;
            if (cfg.matchesChat(cleanLower)) {
                activeBoss   = cfg;
                phase        = Phase.BANK_PRESET;
                phaseStartMs = System.currentTimeMillis();
                waypointIdx  = 0;
                returnWpIdx  = 0;
                consecutiveFails = 0;
                lootSweeps   = 0;
                presetStep   = 0;
                presetSlotTarget = bossPresetSlot; // user-chosen boss preset slot
                presetNextPhase  = Phase.TELEPORT;
                bot.log("[world-boss] Notification detected: " + cfg.displayName
                        + " — starting boss routine.");
                return;
            }
        }
    }

    // ── Query methods for the host ────────────────────────────────────────
    public boolean shouldInterrupt() { return phase != Phase.IDLE && phase != Phase.DONE; }
    public boolean isDone()          { return phase == Phase.DONE; }

    public void acknowledge() {
        phase = Phase.IDLE;
        activeBoss = null;
        fightStartedAt = 0L;
        bailedThisFight = false;
        preSpawnPrepped = false;
        preSpawnPrepAtMs = 0L;
    }

    public Phase      getPhase()      { return phase; }
    public BossConfig getActiveBoss() { return activeBoss; }

    public String statusText() {
        if (phase == Phase.IDLE) return "—";
        String boss = activeBoss != null ? activeBoss.displayName : "?";
        switch (phase) {
            case BANK_PRESET:   return "Loading boss preset";
            case TELEPORT:      return "Teleporting to " + boss;
            case WALK_TO_BOSS:  return "Walking to " + boss + " spawn";
            case WAIT_SPAWN:    return "Waiting for " + boss + " to spawn";
            case DRINK_POTION:  return "Drinking Superior Potion";
            case PRAY_UP:       return "Activating prayers";
            case FIGHT:         return "Fighting " + boss;
            case LOOT:          return "Looting";
            case RETURN:        return "Teleporting to Donator Zone";
            case RETURN_WALK:   return "Walking to bank chest";
            case REGEAR:        return "Loading mining preset";
            case DONE:          return "Boss done";
            default:            return phase.name();
        }
    }

    // ── Tick ──────────────────────────────────────────────────────────────
    public int tick() {
        if (activeBoss == null) { phase = Phase.DONE; return 600; }
        try {
            switch (phase) {
                case BANK_PRESET:   return tickBankPreset();
                case TELEPORT:      return tickTeleport();
                case WALK_TO_BOSS:  return tickWalkToBoss();
                case WAIT_SPAWN:    return tickWaitSpawn();
                case DRINK_POTION:  return tickDrinkPotion();
                case PRAY_UP:       return tickPrayUp();
                case FIGHT:         return tickFight();
                case LOOT:          return tickLoot();
                case RETURN:        return tickReturn();
                case RETURN_WALK:   return tickReturnWalk();
                case REGEAR:        return tickBankPreset();
                default:            phase = Phase.DONE; return 600;
            }
        } catch (Throwable t) {
            bot.log("[world-boss] Error in phase " + phase + ": " + t.getMessage());
            consecutiveFails++;
            if (consecutiveFails >= MAX_FAILS) {
                bot.log("[world-boss] Too many failures — aborting.");
                phase = Phase.DONE;
            }
            return 1200;
        }
    }

    // ── Phase: BANK_PRESET / REGEAR ──────────────────────────────────────
    // Multi-step:
    //   step 0 → walk to bank if needed, right-click chest "Presets" to open UI
    //   step 1 → once open, call presets.load(slot); try 0-indexed and 1-indexed
    //   step 2 → close interfaces, advance to next phase
    private int tickBankPreset() {
        WorldPoint me = playerTile();
        if (me != null && me.distanceTo(RouteProfile.BANK_CHEST_TILE) > 15) {
            bot.ctx.pathing.walkTo(RouteProfile.BANK_CHEST_TILE);
            return 900;
        }

        switch (presetStep) {
            case 0: // Open preset UI
                closeInterfaces();
                sleep(400);
                // Use location-verified lookup so we never interact with the
                // wrong chest in a nearby building.
                KSObject chest = findBankChest();
                if (chest == null) { fail("Cannot find bank chest at known tile"); return 1200; }
                boolean ok = safeInteract(() -> chest.interact("Presets"));
                if (!ok) { fail("Failed to interact Presets on chest"); return 1200; }
                sleep(2000);
                // Check if preset UI opened
                if (bot.ctx.presets.isOpen()) {
                    presetStep = 1;
                    consecutiveFails = 0;
                    return 600;
                }
                fail("Preset UI did not open");
                return 1200;

            case 1: // Select + load
                if (!bot.ctx.presets.isOpen()) {
                    presetStep = 0; // go back and re-open
                    return 600;
                }
                // Slots are 1-based: 1 = "world boss", 2 = "mining".
                boolean loaded = false;
                try { loaded = bot.ctx.presets.load(presetSlotTarget); } catch (Throwable ignored) {}
                if (!loaded) {
                    fail("presets.load failed for slot " + presetSlotTarget);
                    return 1200;
                }
                sleep(1200);
                presetStep = 2;
                consecutiveFails = 0;
                return 400;

            case 2: // Close + advance
                closeInterfaces();
                bot.log("[world-boss] Preset loaded (slot " + presetSlotTarget + ").");
                presetStep = 0;
                setPhase(presetNextPhase);
                return 600;

            default:
                presetStep = 0;
                return 600;
        }
    }

    // ── Phase: TELEPORT ──────────────────────────────────────────────────
    private int tickTeleport() {
        bot.ctx.chat.sendCommand(activeBoss.teleportCommand);
        bot.log("[world-boss] Sent ::" + activeBoss.teleportCommand);
        sleep(2500);
        if (activeBoss.waypoints != null && activeBoss.waypoints.length > 0) {
            setPhase(Phase.WALK_TO_BOSS);
        } else {
            setPhase(Phase.WAIT_SPAWN);
        }
        return 800;
    }

    // ── Phase: WALK_TO_BOSS ──────────────────────────────────────────────
    private int tickWalkToBoss() {
        WorldPoint[] wps = activeBoss.waypoints;
        if (waypointIdx >= wps.length) {
            setPhase(Phase.WAIT_SPAWN);
            return 600;
        }
        WorldPoint target = wps[waypointIdx];
        WorldPoint me = playerTile();
        boolean isFinal = (waypointIdx == wps.length - 1);
        int standoff = activeBoss.preferredBossDistanceMin;

        // For the final waypoint (which is the boss spawn tile), if the boss
        // config requests a standoff distance, stop that many tiles short
        // instead of stepping onto the spawn tile itself.
        if (isFinal && standoff > 0 && me != null) {
            int d = me.distanceTo(target);
            if (d >= standoff && d <= standoff + 2) {
                bot.log("[world-boss] Holding " + d + " tiles from boss spawn (min=" + standoff + ").");
                waypointIdx++;
                return 300;
            }
            if (d < standoff) {
                // We're too close — step directly away from the spawn tile.
                WorldPoint away = stepAwayFrom(me, target, standoff - d + 1);
                if (away != null) bot.ctx.pathing.walkTo(away);
                return 900;
            }
            // Still too far — approach, but aim at a tile `standoff` short of target.
            WorldPoint approach = stepAwayFrom(target, me, standoff);
            bot.ctx.pathing.walkTo(approach != null ? approach : target);
            return 1200;
        }

        // 6-tile early advance keeps movement continuous (same threshold as RETURN_WALK).
        if (me != null && me.distanceTo(target) <= 6) {
            waypointIdx++;
            return 300;
        }
        bot.ctx.pathing.walkTo(jitter(target, 1));
        return 1200;
    }

    /** Returns a point `distance` tiles from `from`, on the line from `from` toward `toward`.
     *  Used to compute a standoff tile short of the boss spawn. */
    private WorldPoint stepAwayFrom(WorldPoint from, WorldPoint toward, int distance) {
        int dx = toward.getX() - from.getX();
        int dy = toward.getY() - from.getY();
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5) return null;
        int ox = (int) Math.round(distance * dx / len);
        int oy = (int) Math.round(distance * dy / len);
        return new WorldPoint(from.getX() + ox, from.getY() + oy, from.getPlane());
    }

    // ── Phase: WAIT_SPAWN ────────────────────────────────────────────────
    private int tickWaitSpawn() {
        long now = System.currentTimeMillis();

        if (now - phaseStartMs > activeBoss.spawnTimeoutMs) {
            bot.log("[world-boss] Boss spawn timed out — aborting.");
            setPhase(Phase.RETURN);
            return 600;
        }

        // Schedule the pre-spawn prep on first tick of this phase.
        if (preSpawnPrepAtMs == 0L) {
            // Random 10-15s after arriving — feels human, covers the "waiting
            // for the boss to show up" window naturally.
            long delay = ThreadLocalRandom.current().nextLong(10_000, 15_001);
            preSpawnPrepAtMs = now + delay;
            bot.log("[world-boss] Waiting for " + activeBoss.displayName
                    + " — will pray+pot in " + (delay / 1000) + "s.");
        }

        // Pre-spawn prep: pray + drink pot once the delay fires, before boss appears.
        if (!preSpawnPrepped && now >= preSpawnPrepAtMs) {
            bot.log("[world-boss] Pre-spawn prep: activating prayers + drinking potion.");
            doActivatePrayers();
            sleep(300);
            doDrinkPotion();
            preSpawnPrepped = true;
            bot.log("[world-boss] Ready. Waiting for " + activeBoss.displayName + " to spawn.");
        }

        // Boss spawned — go straight to FIGHT (already prepped).
        KSNPC boss = findBossNpc();
        if (boss != null && boss.isAlive()) {
            bot.log("[world-boss] " + activeBoss.displayName + " has spawned! Engaging.");
            if (!preSpawnPrepped) {
                // Spawned before prep window — prep immediately.
                doActivatePrayers();
                sleep(200);
                doDrinkPotion();
                preSpawnPrepped = true;
            }
            setPhase(Phase.FIGHT);
            return 200;
        }
        return 1200;
    }

    // ── Phase: DRINK_POTION (legacy fallback — prep now happens in WAIT_SPAWN) ──
    private int tickDrinkPotion() {
        doDrinkPotion();
        setPhase(Phase.FIGHT);
        return 400;
    }

    // ── Phase: PRAY_UP (legacy fallback) ────────────────────────────────
    private int tickPrayUp() {
        doActivatePrayers();
        setPhase(Phase.DRINK_POTION);
        return 400;
    }

    // ── Shared prep helpers ──────────────────────────────────────────────
    private void doActivatePrayers() {
        try {
            Prayer prayer = bot.ctx.prayer;
            if (!prayer.isEnabled(Prayer.Prayers.PROTECT_FROM_MAGIC))
                prayer.enable(Prayer.Prayers.PROTECT_FROM_MAGIC);
            if (!prayer.isEnabled(Prayer.Prayers.EAGLE_EYE))
                prayer.enable(Prayer.Prayers.EAGLE_EYE);
            bot.log("[world-boss] Prayers active: Protect from Magic + Eagle Eye.");
        } catch (Throwable t) {
            bot.log("[world-boss] Prayer activation error: " + t.getMessage());
        }
    }

    private void doDrinkPotion() {
        try {
            KSItem potion = bot.ctx.inventory.getItem(SUPERIOR_POTION_ID);
            if (potion == null) potion = bot.ctx.inventory.getItem(SUPERIOR_POTION_NAME);
            if (potion == null) {
                List<KSItem> matches = bot.ctx.inventory.getItems(i ->
                        i != null && i.getName() != null
                                && i.getName().toLowerCase().contains("superior"));
                if (!matches.isEmpty()) potion = matches.get(0);
            }
            if (potion != null) {
                potion.interact("Drink");
                bot.log("[world-boss] Drank Superior Potion.");
                sleep(600);
            } else {
                bot.log("[world-boss] No Superior Potion found — skipping.");
            }
        } catch (Throwable t) {
            bot.log("[world-boss] Potion drink error: " + t.getMessage());
        }
    }

    // ── Phase: FIGHT ─────────────────────────────────────────────────────
    private int tickFight() {
        // Lazy-init fight tracking on first FIGHT tick.
        if (fightStartedAt == 0L) {
            fightStartedAt = System.currentTimeMillis();
            // 14-28 game ticks (600ms each) before we allow a low-player bail.
            bailGraceMs = ThreadLocalRandom.current().nextInt(14 * 600, 28 * 600 + 1);
            bailedThisFight = false;
            // Count this as an attempt.
            statsFor(activeBoss).attempted++;
            bot.log("[world-boss] FIGHT begin. Bail grace: " + bailGraceMs
                    + "ms (~" + (bailGraceMs / 600) + " ticks).");
        }

        if (System.currentTimeMillis() - phaseStartMs > activeBoss.fightTimeoutMs) {
            bot.log("[world-boss] Fight timed out — moving to loot.");
            disableBossPrayers();
            snapshotInventory();
            setPhase(Phase.LOOT);
            return 600;
        }

        KSNPC boss = findBossNpc();
        if (boss == null || boss.isDead()) {
            bot.log("[world-boss] " + activeBoss.displayName + " killed!");
            statsFor(activeBoss).killed++;
            disableBossPrayers();
            snapshotInventory();
            fightStartedAt = 0L;
            setPhase(Phase.LOOT);
            return 800;
        }

        // Low-player bail: if past grace window and not enough players in
        // range, abandon this fight and go back to perk farming.
        if (activeBoss.minPlayersForFight > 0
                && System.currentTimeMillis() - fightStartedAt > bailGraceMs) {
            int nearby = nearbyPlayerCount;
            if (nearby < activeBoss.minPlayersForFight) {
                bot.log("[world-boss] Not enough players to fight "
                        + activeBoss.displayName
                        + " reliably - going back to farming perk points "
                        + "(nearby=" + nearby + ", min="
                        + activeBoss.minPlayersForFight + ").");
                statsFor(activeBoss).bailed++;
                bailedThisFight = true;
                disableBossPrayers();
                fightStartedAt = 0L;
                setPhase(Phase.RETURN);
                return 600;
            }
        }

        // AOE dodge — react to graphics-object markers within footprint,
        // or boss animation targeting us (earlier warning).
        if (maybeDodgeAoe(boss)) {
            return 400; // give the step time to register
        }

        // Keep prayers up
        try {
            Prayer prayer = bot.ctx.prayer;
            if (!prayer.isEnabled(Prayer.Prayers.PROTECT_FROM_MAGIC))
                prayer.enable(Prayer.Prayers.PROTECT_FROM_MAGIC);
            if (!prayer.isEnabled(Prayer.Prayers.EAGLE_EYE))
                prayer.enable(Prayer.Prayers.EAGLE_EYE);
        } catch (Throwable ignored) {}

        // Sip super restore if prayer is running low
        checkPrayerRestore();

        // Death check: if we died, the server teleports us back to the
        // arrival tile. Detect this by distance from the boss — if we're
        // far away, re-walk the waypoints and re-engage.
        WorldPoint me = playerTile();
        if (me != null && activeBoss.arrivalTile != null) {
            WorldPoint bossTile = activeBoss.waypoints != null && activeBoss.waypoints.length > 0
                    ? activeBoss.waypoints[activeBoss.waypoints.length - 1]
                    : activeBoss.arrivalTile;
            if (me.distanceTo(bossTile) > 20) {
                bot.log("[world-boss] Appears we died — respawned far from boss. Re-walking + re-potting.");
                statsFor(activeBoss).deaths++;
                waypointIdx = 0;
                preSpawnPrepped = false;
                preSpawnPrepAtMs = 0L;
                setPhase(Phase.WALK_TO_BOSS);
                return 600;
            }
        }

        // Passive standoff: if we've drifted inside preferredBossDistanceMin, step back.
        if (activeBoss.preferredBossDistanceMin > 0 && me != null) {
            WorldPoint bossLoc = null;
            try { bossLoc = boss.getWorldLocation(); } catch (Throwable ignored) {}
            if (bossLoc != null && me.distanceTo(bossLoc) < activeBoss.preferredBossDistanceMin) {
                WorldPoint back = stepAwayFrom(me, bossLoc, activeBoss.preferredBossDistanceMin - me.distanceTo(bossLoc) + 1);
                if (back != null) {
                    bot.ctx.pathing.walkTo(back);
                    return 600;
                }
            }
        }

        // Already attacking?
        try {
            if (bot.ctx.players.getLocal().getInteracting() != null) {
                return 1200;
            }
        } catch (Throwable ignored) {}

        boolean ok = safeInteract(() -> boss.interact("Attack"));
        if (!ok) fail("Failed to attack " + activeBoss.displayName);
        return 1500;
    }

    // ── AOE dodge ────────────────────────────────────────────────────────
    /**
     * If the boss is casting a known dangerous animation at us, or a dangerous
     * graphics-object marker is within the AOE footprint of the player, step
     * away (preferred N, fall back to S, E, W). Returns true iff a dodge
     * step was issued this tick.
     */
    private boolean maybeDodgeAoe(KSNPC boss) {
        if (activeBoss == null) return false;
        if (activeBoss.dodgeDistance <= 0) return false;
        int[] badAnims  = activeBoss.dangerousAnimationIds;
        int[] badGfx    = activeBoss.dangerousAoeGraphicIds;
        int   footprint = Math.max(1, activeBoss.aoeFootprintRadius);
        if ((badAnims == null || badAnims.length == 0)
                && (badGfx == null || badGfx.length == 0)) return false;

        long now = System.currentTimeMillis();
        if (now - lastDodgeAtMs < DODGE_COOLDOWN_MS) return false;

        WorldPoint me = playerTile();
        if (me == null) return false;

        boolean threat = false;
        String reason = "";

        // 1. Graphics-object marker inside the AOE footprint = we're standing in it.
        if (badGfx != null && badGfx.length > 0) {
            try {
                Object hit = bot.ctx.graphicsObjects.query()
                        .withId(badGfx)
                        .within(me, footprint)
                        .first();
                if (hit != null) {
                    threat = true;
                    reason = "graphic marker within " + footprint;
                }
            } catch (Throwable ignored) {}
        }

        // 2. Boss animation matches + boss is targeting us = incoming AOE.
        if (!threat && badAnims != null && badAnims.length > 0 && boss != null) {
            try {
                int anim = boss.getAnimation();
                for (int a : badAnims) {
                    if (a == anim) {
                        boolean targetingUs = true;
                        try {
                            Object target = boss.getInteracting();
                            Object self   = bot.ctx.players.getLocal();
                            if (target != null && self != null) {
                                targetingUs = target.equals(self);
                            }
                        } catch (Throwable ignored) {}
                        if (targetingUs) {
                            threat = true;
                            reason = "boss animation " + anim + " aimed at us";
                        }
                        break;
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (!threat) return false;

        // Pick a dodge direction: N, S, E, W (stop at first reachable).
        int d = activeBoss.dodgeDistance;
        WorldPoint[] options = {
                new WorldPoint(me.getX(),     me.getY() + d, me.getPlane()), // N
                new WorldPoint(me.getX(),     me.getY() - d, me.getPlane()), // S
                new WorldPoint(me.getX() + d, me.getY(),     me.getPlane()), // E
                new WorldPoint(me.getX() - d, me.getY(),     me.getPlane()), // W
        };
        String[] dirs = {"N", "S", "E", "W"};
        // pathing.walkTo returns void, so we just fire N first. The next
        // tick's dodge check sees whether the marker is still under us and
        // retries with a different direction if so.
        try {
            bot.ctx.pathing.walkTo(options[0]);
            bot.log("[world-boss] AOE dodge " + dirs[0] + " (" + reason + ")");
        } catch (Throwable ignored) {
            try { bot.ctx.pathing.walkTo(options[1]); } catch (Throwable ignored2) {}
            bot.log("[world-boss] AOE dodge " + dirs[1] + " (N failed, " + reason + ")");
        }
        statsFor(activeBoss).aoeDodges++;
        lastDodgeAtMs = now;
        return true;
    }

    // ── Phase: LOOT ──────────────────────────────────────────────────────
    private int tickLoot() {
        if (bailedThisFight) {
            bot.log("[world-boss] Bailed — skipping loot, heading home.");
            setPhase(Phase.RETURN);
            return 400;
        }
        lootSweeps++;

        // First sweep: wait for ground items to appear, then record auto-loot.
        if (lootSweeps == 1) {
            bot.log("[world-boss] Waiting " + (LOOT_SETTLE_DELAY_MS / 1000)
                    + "s for ground loot to appear...");
            sleep(LOOT_SETTLE_DELAY_MS);
            recordAutoLoot();
        }

        try {
            boolean found = false;
            // Scan ground items near player using TileItemQuery.
            // Boss loot spawns on the player's tile; anything >2 tiles away
            // is someone else's loot or stale drops (arrows/bolts/darts).
            List<KSGroundItem> nearby = new ArrayList<>();
            try {
                nearby.addAll(bot.ctx.groundItems.query()
                        .withinDistance(LOOT_MAX_DISTANCE).list(20));
            } catch (Throwable t) {
                // Fallback: LOOT_LIST — still filtered below by distance.
                if (bot.ctx.groundItems.hasLoot()) {
                    List<KSGroundItem> ll = bot.ctx.groundItems.LOOT_LIST;
                    if (ll != null) nearby.addAll(ll);
                }
            }

            WorldPoint me = playerTile();
            for (KSGroundItem item : nearby) {
                if (item == null) continue;

                // ── Strict distance check (defence-in-depth) ─────────────
                // The query should already filter, but LOOT_LIST fallback
                // doesn't — and we absolutely do not want to loot other
                // players' arrows / bolts / darts lying on the ground.
                if (me != null) {
                    WorldPoint itemLoc = null;
                    try { itemLoc = item.getWorldLocation(); } catch (Throwable ignored) {}
                    if (itemLoc == null || me.distanceTo(itemLoc) > LOOT_MAX_DISTANCE) {
                        continue;   // skip — not on our pile
                    }
                }

                if (bot.ctx.inventory.isFull()) {
                    bot.log("[world-boss] Inventory full during loot.");
                    break;
                }
                String name = item.getName();
                int qty = Math.max(1, item.getQuantity());
                bot.log("[world-boss] Picking up: " + name + " x" + qty
                        + " (on our pile, ≤" + LOOT_MAX_DISTANCE + " tiles)");
                item.interact("Take");
                trackLoot(name, qty);
                found = true;
                sleep(800);
            }

            // Don't bail on first empty sweep — loot may still be loading.
            // Only exit after at least 2 empty sweeps or we hit the max.
            if (lootSweeps >= MAX_LOOT_SWEEPS
                    || (!found && lootSweeps >= 3)) {
                bot.log("[world-boss] Loot sweep done (" + lootSweeps + " passes).");
                setPhase(Phase.RETURN);
                return 600;
            }
        } catch (Throwable t) {
            bot.log("[world-boss] Loot error: " + t.getMessage());
            if (lootSweeps >= MAX_LOOT_SWEEPS) {
                setPhase(Phase.RETURN);
                return 600;
            }
        }
        return 1200;
    }

    // ── Phase: RETURN ────────────────────────────────────────────────────
    private int tickReturn() {
        bot.ctx.chat.sendCommand(RETURN_COMMAND);
        bot.log("[world-boss] Sent ::dz — returning to Donator Zone.");
        sleep(2500);
        returnWpIdx = 0;
        setPhase(Phase.RETURN_WALK);
        return 800;
    }

    // ── Phase: RETURN_WALK ───────────────────────────────────────────────
    private int tickReturnWalk() {
        WorldPoint[] wps = BossConfig.DZ_RETURN_WAYPOINTS;
        WorldPoint me = playerTile();

        if (returnWpIdx < wps.length) {
            WorldPoint target = wps[returnWpIdx];
            // Advance early so the next walkTo fires before we fully arrive —
            // keeps movement continuous rather than stop-start.
            if (me != null && me.distanceTo(target) <= 6) {
                returnWpIdx++;
                return 300;
            }
            bot.ctx.pathing.walkTo(jitter(target, 1));
            return 1200;
        }

        // Final step: walk directly to the known chest tile.
        // We deliberately do NOT call openBank() here — it lets the engine pick
        // the nearest chest by ID which can resolve to the wrong building.
        // REGEAR handles the actual bank interaction once we arrive.
        if (me != null && me.distanceTo(RouteProfile.BANK_CHEST_TILE) <= 6) {
            bot.log("[world-boss] Reached bank area. Loading mining preset.");
            presetStep = 0;
            presetSlotTarget = bot.getMiningPresetSlot();
            presetNextPhase  = Phase.DONE;
            setPhase(Phase.REGEAR);
            return 600;
        }
        bot.ctx.pathing.walkTo(jitter(RouteProfile.BANK_CHEST_TILE, 1));
        return 1200;
    }

    /** Returns a WorldPoint offset by a random ±range on both axes. */
    private WorldPoint jitter(WorldPoint wp, int range) {
        int dx = ThreadLocalRandom.current().nextInt(-range, range + 1);
        int dy = ThreadLocalRandom.current().nextInt(-range, range + 1);
        return new WorldPoint(wp.getX() + dx, wp.getY() + dy, wp.getPlane());
    }

    /**
     * Finds the correct bank chest — the one at {@link RouteProfile#BANK_CHEST_TILE}.
     * Queries by exact tile first; falls back to getClosest only if the result
     * is within 3 tiles of the known coordinate, so we never accidentally
     * interact with a different chest in a nearby building.
     */
    private KSObject findBankChest() {
        try {
            KSObject c = bot.ctx.groundObjects.query()
                    .withId(BANK_CHEST_ID)
                    .atLocation(RouteProfile.BANK_CHEST_TILE)
                    .first();
            if (c != null) return c;
        } catch (Throwable ignored) {}
        try {
            KSObject c = bot.ctx.groundObjects.getClosest(BANK_CHEST_ID);
            if (c != null) {
                WorldPoint loc = c.getWorldLocation();
                if (loc != null && loc.distanceTo(RouteProfile.BANK_CHEST_TILE) <= 3) return c;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    // ── Helpers: potions ─────────────────────────────────────────────────
    private void checkPrayerRestore() {
        try {
            int points = bot.ctx.prayer.getPoints();
            if (points < PRAYER_LOW_THRESHOLD) {
                // Look for any super restore in inventory by name substring
                // getItem doesn't accept a predicate; use getItems(pred).
                List<KSItem> restores = bot.ctx.inventory.getItems(item ->
                        item != null && item.getName() != null
                                && item.getName().toLowerCase().contains("super restore"));
                KSItem restore = restores.isEmpty() ? null : restores.get(0);
                if (restore != null) {
                    restore.interact("Drink");
                    bot.log("[world-boss] Sipped super restore (prayer was " + points + ").");
                    sleep(600);
                }
            }
        } catch (Throwable ignored) {}
    }

    // ── Helpers: loot tracking ───────────────────────────────────────────
    /** Snapshot inventory before loot so we can diff for auto-looted items. */
    private void snapshotInventory() {
        preLootInventory.clear();
        try {
            for (KSItem item : bot.ctx.inventory.getItems(i -> true)) {
                if (item == null) continue;
                String name = item.getName();
                int qty = Math.max(1, item.getQuantity());
                preLootInventory.merge(name, qty, Integer::sum);
            }
        } catch (Throwable ignored) {}
    }

    /** Compare current inventory to pre-loot snapshot; diff = auto-looted items. */
    private void recordAutoLoot() {
        try {
            Map<String, Integer> now = new HashMap<>();
            for (KSItem item : bot.ctx.inventory.getItems(i -> true)) {
                if (item == null) continue;
                now.merge(item.getName(), Math.max(1, item.getQuantity()), Integer::sum);
            }
            for (Map.Entry<String, Integer> e : now.entrySet()) {
                int before = preLootInventory.getOrDefault(e.getKey(), 0);
                int gained = e.getValue() - before;
                if (gained > 0) {
                    bot.log("[world-boss] Auto-looted: " + e.getKey() + " x" + gained);
                    trackLoot(e.getKey(), gained);
                }
            }
        } catch (Throwable ignored) {}
    }

    private void trackLoot(String name, int qty) {
        sessionLoot.merge(name, qty, Integer::sum);
    }

    // ── Helpers: NPC / prayer / misc ─────────────────────────────────────
    private KSNPC findBossNpc() {
        try {
            if (activeBoss.npcId > 0) {
                KSNPC npc = bot.ctx.npcs.query()
                        .withId(activeBoss.npcId)
                        .closest();
                if (npc != null) return npc;
            }
            return bot.ctx.npcs.query()
                    .withName(activeBoss.npcName)
                    .closest();
        } catch (Throwable t) {
            return null;
        }
    }

    private void disableBossPrayers() {
        try {
            bot.ctx.prayer.disable(
                    Prayer.Prayers.PROTECT_FROM_MAGIC,
                    Prayer.Prayers.EAGLE_EYE);
        } catch (Throwable ignored) {}
    }

    private WorldPoint playerTile() {
        try { return bot.ctx.players.getLocal().getWorldLocation(); }
        catch (Throwable t) { return null; }
    }

    private void setPhase(Phase p) {
        bot.log("[world-boss] Phase: " + this.phase + " -> " + p);
        this.phase = p;
        this.phaseStartMs = System.currentTimeMillis();
    }

    private void fail(String why) {
        consecutiveFails++;
        bot.log("[world-boss] fail (" + consecutiveFails + "/" + MAX_FAILS + "): " + why);
        if (consecutiveFails >= MAX_FAILS) {
            bot.log("[world-boss] Too many failures — aborting to return.");
            setPhase(Phase.RETURN);
        }
    }

    private boolean safeInteract(InteractFn fn) {
        try {
            Object r = fn.run();
            if (r instanceof Boolean) return (Boolean) r;
            return true;
        } catch (Throwable t) { return false; }
    }

    @FunctionalInterface
    private interface InteractFn { Object run() throws Exception; }

    private void closeInterfaces() {
        try { if (bot.ctx.bank.isOpen()) bot.ctx.bank.close(); } catch (Throwable ignored) {}
        try { if (bot.ctx.dialog.isOpen()) bot.ctx.dialog.close(); } catch (Throwable ignored) {}
        try { if (bot.ctx.presets.isOpen()) { /* presets close with bank close */ } } catch (Throwable ignored) {}
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
