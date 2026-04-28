package pfwbh125;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Per-category min/max action delays for anti-ban variation.
 * <p>
 * Each category carries a [min, max] window (milliseconds). Callers ask for
 * {@link #roll(Category)} and receive a uniform-random value inside that
 * window. Persisted to / loaded from JSON via {@link #saveTo(String)} /
 * {@link #loadFrom(String)}.
 * <p>
 * Defaults are sourced from the previously-hardcoded sleep / scheduleIn values
 * in the mining pipeline and world-boss handler, ±a sensible jitter band.
 */
public final class DelayProfile {

    /** Action categories exposed in the UI. Order = display order. */
    public enum Category {
        ATTACK_BOSS      ("Attack Boss"),
        FIGHT_ENGAGE     ("Fight Engage"),
        PRAYER           ("Prayer Switch"),
        POTION           ("Potion Drink"),
        EAT_FOOD         ("Eat Food"),
        TELEPORT         ("Teleport"),
        MINE_ROCK        ("Mine Rock"),
        BANKING          ("Banking Action"),
        LOOT_PICKUP      ("Loot Pickup"),
        NPC_DIALOGUE     ("NPC Dialogue"),
        WALK_MOVEMENT    ("Walk / Movement"),
        POST_KILL_SETTLE ("Post-Kill Settle");

        public final String label;
        Category(String label) { this.label = label; }
    }

    private static final Map<Category, int[]> DEFAULTS = new EnumMap<>(Category.class);
    static {
        // [min, max] ms — tuned around the current hardcoded values.
        DEFAULTS.put(Category.ATTACK_BOSS,      new int[]{ 2000, 2800 });
        DEFAULTS.put(Category.FIGHT_ENGAGE,     new int[]{  300,  700 });
        DEFAULTS.put(Category.PRAYER,           new int[]{  200,  400 });
        DEFAULTS.put(Category.POTION,           new int[]{  500,  800 });
        DEFAULTS.put(Category.EAT_FOOD,         new int[]{  500,  800 });
        DEFAULTS.put(Category.TELEPORT,         new int[]{ 2200, 3000 });
        DEFAULTS.put(Category.MINE_ROCK,        new int[]{ 1500, 2100 });
        DEFAULTS.put(Category.BANKING,          new int[]{  450,  900 });
        DEFAULTS.put(Category.LOOT_PICKUP,      new int[]{  600, 1000 });
        DEFAULTS.put(Category.NPC_DIALOGUE,     new int[]{  500,  900 });
        DEFAULTS.put(Category.WALK_MOVEMENT,    new int[]{  600, 1000 });
        DEFAULTS.put(Category.POST_KILL_SETTLE, new int[]{ 2500, 3500 });
    }

    private final Map<Category, int[]> values = new EnumMap<>(Category.class);

    // ── Idle / AFK jitter ────────────────────────────────────────────────
    /** Master toggle — when false, {@link #rollIdleJitterMs()} always returns 0. */
    private volatile boolean idleEnabled = false;
    /** Min pause length in ms when idle jitter fires. */
    private volatile int idleMinMs = 5000;
    /** Max pause length in ms when idle jitter fires. */
    private volatile int idleMaxMs = 30000;
    /** Average N scheduleIn() calls between idle-jitter triggers (1-in-N roll). */
    private volatile int idleFrequency = 50;

    public DelayProfile() {
        for (Category c : Category.values()) {
            int[] d = DEFAULTS.get(c);
            values.put(c, new int[]{ d[0], d[1] });
        }
    }

    public boolean isIdleEnabled()    { return idleEnabled; }
    public int     getIdleMinMs()     { return idleMinMs; }
    public int     getIdleMaxMs()     { return idleMaxMs; }
    public int     getIdleFrequency() { return idleFrequency; }

    public void setIdleEnabled(boolean on)     { this.idleEnabled = on; }
    public void setIdleMinMs(int ms)           { this.idleMinMs = Math.max(0, ms); }
    public void setIdleMaxMs(int ms)           { this.idleMaxMs = Math.max(0, ms); }
    public void setIdleFrequency(int freq)     { this.idleFrequency = Math.max(1, freq); }

    /** Roll the 1-in-N chance of an idle pause; return its ms length, or 0 if
     *  disabled / didn't trigger. Call once per scheduleIn to decide. */
    public int rollIdleJitterMs() {
        if (!idleEnabled) return 0;
        int freq = Math.max(1, idleFrequency);
        if (ThreadLocalRandom.current().nextInt(freq) != 0) return 0;
        int lo = Math.max(0, idleMinMs);
        int hi = Math.max(lo, idleMaxMs);
        return (hi <= lo) ? lo : ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }

    public int min(Category c) { return values.get(c)[0]; }
    public int max(Category c) { return values.get(c)[1]; }

    public void set(Category c, int min, int max) {
        int lo = Math.max(0, min);
        int hi = Math.max(lo, max);
        values.get(c)[0] = lo;
        values.get(c)[1] = hi;
    }

    /** Sample a uniform-random value in [min, max] (inclusive). */
    public int roll(Category c) {
        int lo = min(c);
        int hi = max(c);
        if (hi <= lo) return lo;
        // Post-break "ramp" mode — bias toward the upper half of the window
        // so the bot acts groggy / reaction-time-slow for a few minutes after
        // waking up. Flag is toggled by BreakManager.
        if (rampActive) {
            int mid = lo + (hi - lo) / 2;
            return ThreadLocalRandom.current().nextInt(mid, hi + 1);
        }
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }

    /** Post-break ramp: when true, every {@link #roll(Category)} returns a
     *  value in the upper half of the window. Set by BreakManager. */
    private volatile boolean rampActive = false;
    public void setRampActive(boolean on) { this.rampActive = on; }
    public boolean isRampActive() { return rampActive; }

    // ── Persistence (hand-rolled JSON — no external deps) ────────────────

    public void saveTo(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            sb.append("  \"").append(c.name()).append("\": [")
              .append(min(c)).append(", ").append(max(c)).append("],\n");
        }
        // Idle-jitter block
        sb.append("  \"IDLE_ENABLED\": ").append(idleEnabled).append(",\n");
        sb.append("  \"IDLE_MIN_MS\": ").append(idleMinMs).append(",\n");
        sb.append("  \"IDLE_MAX_MS\": ").append(idleMaxMs).append(",\n");
        sb.append("  \"IDLE_FREQUENCY\": ").append(idleFrequency).append('\n');
        sb.append("}\n");
        try (BufferedWriter w = new BufferedWriter(new FileWriter(path))) {
            w.write(sb.toString());
        }
    }

    public void loadFrom(String path) throws IOException {
        StringBuilder raw = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = r.readLine()) != null) raw.append(line).append('\n');
        }
        String text = raw.toString();
        for (Category c : Category.values()) {
            int[] pair = parsePair(text, c.name());
            if (pair != null) set(c, pair[0], pair[1]);
        }
        // Idle-jitter block (all optional).
        Boolean ie = parseBool(text, "IDLE_ENABLED");    if (ie != null) idleEnabled   = ie;
        Integer im = parseInt (text, "IDLE_MIN_MS");     if (im != null) idleMinMs     = Math.max(0, im);
        Integer ix = parseInt (text, "IDLE_MAX_MS");     if (ix != null) idleMaxMs     = Math.max(0, ix);
        Integer fq = parseInt (text, "IDLE_FREQUENCY");  if (fq != null) idleFrequency = Math.max(1, fq);
    }

    private static Integer parseInt(String text, String key) {
        int k = text.indexOf('"' + key + '"');
        if (k < 0) return null;
        int colon = text.indexOf(':', k);
        int end = indexOfAny(text, colon + 1, ",}\n");
        if (colon < 0 || end < 0) return null;
        try { return Integer.parseInt(text.substring(colon + 1, end).trim()); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static Boolean parseBool(String text, String key) {
        int k = text.indexOf('"' + key + '"');
        if (k < 0) return null;
        int colon = text.indexOf(':', k);
        int end = indexOfAny(text, colon + 1, ",}\n");
        if (colon < 0 || end < 0) return null;
        String v = text.substring(colon + 1, end).trim().toLowerCase();
        if (v.equals("true"))  return Boolean.TRUE;
        if (v.equals("false")) return Boolean.FALSE;
        return null;
    }

    private static int indexOfAny(String text, int from, String chars) {
        int best = -1;
        for (int i = 0; i < chars.length(); i++) {
            int idx = text.indexOf(chars.charAt(i), from);
            if (idx >= 0 && (best < 0 || idx < best)) best = idx;
        }
        return best;
    }

    /** Pulls [min, max] from `"KEY": [min, max]`. Lenient whitespace. */
    private static int[] parsePair(String text, String key) {
        int k = text.indexOf('"' + key + '"');
        if (k < 0) return null;
        int lb = text.indexOf('[', k);
        int rb = text.indexOf(']', lb);
        if (lb < 0 || rb < 0) return null;
        String inner = text.substring(lb + 1, rb).trim();
        String[] parts = inner.split("\\s*,\\s*");
        if (parts.length != 2) return null;
        try {
            return new int[]{ Integer.parseInt(parts[0].trim()),
                              Integer.parseInt(parts[1].trim()) };
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
