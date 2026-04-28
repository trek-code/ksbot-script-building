package pfwbh125;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Smart break scheduler layered on top of KSBot's built-in pause/break hooks.
 * <p>
 * Runs two break flavours concurrently:
 * <ul>
 *   <li>QUICK — 5–30 min frequency-based (smoke / bathroom)</li>
 *   <li>LONG  — 6–9 hr time-window-based (sleep / shift)</li>
 * </ul>
 * <p>
 * Each break type is gated by a {@link BotContextGates} check — breaks never
 * fire mid-combat, with a full inventory, or next to a staff member. If gates
 * keep blocking past a configurable max-wait, the break fires anyway.
 * <p>
 * State transitions per tick are idempotent: {@link #tick(long)} is safe to
 * call from the bot's onProcess loop every iteration.
 */
public final class BreakManager {

    public enum State {
        /** Not breaking, not scheduled to break soon. */
        IDLE,
        /** Break is scheduled; gates are being checked / we're waiting for a safe point. */
        PENDING,
        /** Currently breaking. */
        ACTIVE,
        /** Break just ended; bot is in "ramp" mode (slower delays for a while). */
        RAMP
    }

    // ── Config ──────────────────────────────────────────────────────────
    public final BreakConfig quick = new BreakConfig(BreakConfig.Kind.QUICK);
    public final BreakConfig longg = new BreakConfig(BreakConfig.Kind.LONG);

    // ── Runtime state ───────────────────────────────────────────────────
    private final long scriptStartMs = System.currentTimeMillis();
    private volatile State state = State.IDLE;
    private volatile BreakConfig activeConfig = null;

    /** When the next scheduled break should fire (ms epoch). 0 = none scheduled. */
    private volatile long nextQuickAtMs = 0L;
    private volatile long nextLongAtMs  = 0L;

    /** When the current break ends (ms epoch). Only valid when ACTIVE. */
    private volatile long breakEndAtMs = 0L;

    /** When post-break ramp ends (ms epoch). Only valid when RAMP. */
    private volatile long rampEndAtMs = 0L;

    /** When the last break ended — used for "min uptime between breaks". */
    private volatile long lastBreakEndedAtMs = 0L;

    /** If gates keep blocking, force the break once we pass this deadline. */
    private volatile long gateForceDeadlineMs = 0L;

    /** Timestamps of recent break starts — for the per-hour cap. */
    private final Deque<Long> recentBreakStarts = new ArrayDeque<>();

    // ── Pluggable surfaces ──────────────────────────────────────────────
    private BotContextGates gates;
    /** Callback to "log out" — caller supplies the impl (widget click, stop, etc). */
    private Runnable logoutAction;
    /** Callback to "log in" / resume after break — typically just a state reset. */
    private Runnable resumeAction;
    /** Event hook: "break started" + "break ended" + "scheduled" + "fault". */
    private Consumer<String> eventLog;

    // ── History ─────────────────────────────────────────────────────────
    public static final class BreakRecord {
        public final BreakConfig.Kind kind;
        public final BreakConfig.LogoutMode mode;
        public final long startMs;
        public final long endMs;
        public BreakRecord(BreakConfig.Kind k, BreakConfig.LogoutMode m, long s, long e) {
            this.kind = k; this.mode = m; this.startMs = s; this.endMs = e;
        }
        public long durationMs() { return Math.max(0, endMs - startMs); }
    }
    private final List<BreakRecord> history = new ArrayList<>();

    // ── Setup ───────────────────────────────────────────────────────────
    public void setGates(BotContextGates g)              { this.gates = g; }
    public void setLogoutAction(Runnable r)              { this.logoutAction = r; }
    public void setResumeAction(Runnable r)              { this.resumeAction = r; }
    public void setEventLog(Consumer<String> c)          { this.eventLog = c; }

    public State getState()                  { return state; }
    public BreakConfig getActiveConfig()     { return activeConfig; }
    public long getBreakEndAtMs()            { return breakEndAtMs; }
    public long getRampEndAtMs()             { return rampEndAtMs; }
    public long getNextQuickAtMs()           { return nextQuickAtMs; }
    public long getNextLongAtMs()            { return nextLongAtMs; }
    public long getLastBreakEndedAtMs()      { return lastBreakEndedAtMs; }
    public List<BreakRecord> getHistory()    { return new ArrayList<>(history); }

    /** True if script should short-circuit onProcess. */
    public boolean shouldSuspend() { return state == State.ACTIVE; }
    public boolean isRamp()        { return state == State.RAMP; }

    private void log(String s) { if (eventLog != null) eventLog.accept(s); }

    // ── Manual controls ─────────────────────────────────────────────────
    public void takeBreakNow(BreakConfig.Kind k) {
        BreakConfig cfg = (k == BreakConfig.Kind.QUICK) ? quick : longg;
        scheduleBreak(cfg, System.currentTimeMillis());
        gateForceDeadlineMs = System.currentTimeMillis(); // bypass gates
        state = State.PENDING;
        log("Manual break requested: " + k);
    }

    public void skipNextBreak() {
        long now = System.currentTimeMillis();
        nextQuickAtMs = now + quick.frequencyMinMs;
        nextLongAtMs  = now + longg.frequencyMinMs;
        state = State.IDLE;
        activeConfig = null;
        log("Next break skipped / rescheduled.");
    }

    public void delayNextBreak(long ms) {
        if (nextQuickAtMs > 0) nextQuickAtMs += ms;
        if (nextLongAtMs  > 0) nextLongAtMs  += ms;
        log("Next break delayed by " + (ms / 1000) + "s.");
    }

    // ── Main tick ───────────────────────────────────────────────────────
    /**
     * Call once per onProcess iteration. Returns true if the bot should
     * skip its normal tick work this iteration (i.e., we're on break).
     */
    public boolean tick(long nowMs) {
        switch (state) {
            case IDLE:   return tickIdle(nowMs);
            case PENDING: return tickPending(nowMs);
            case ACTIVE:  return tickActive(nowMs);
            case RAMP:    return tickRamp(nowMs);
        }
        return false;
    }

    private boolean tickIdle(long now) {
        // Lazy schedule on first tick / after a skip.
        if (nextQuickAtMs == 0 && quick.enabled) {
            long base = Math.max(scriptStartMs + quick.minUptimeBeforeFirstBreakMs, now);
            nextQuickAtMs = base + randRange(quick.frequencyMinMs, quick.frequencyMaxMs);
            log("Quick break scheduled in " + ((nextQuickAtMs - now) / 60000) + " min.");
        }
        if (nextLongAtMs == 0 && longg.enabled) {
            scheduleNextLongBreak(now);
        }

        // Pick whichever is due first.
        BreakConfig due = null;
        long dueAt = Long.MAX_VALUE;
        if (quick.enabled && nextQuickAtMs > 0 && nextQuickAtMs <= now && nextQuickAtMs < dueAt) {
            due = quick; dueAt = nextQuickAtMs;
        }
        if (longg.enabled && nextLongAtMs > 0 && nextLongAtMs <= now && nextLongAtMs < dueAt) {
            due = longg; dueAt = nextLongAtMs;
        }
        if (due == null) return false;

        // Enforce min uptime between breaks.
        if (lastBreakEndedAtMs > 0
                && now - lastBreakEndedAtMs < due.minUptimeBetweenBreaksMs) {
            long reschedule = lastBreakEndedAtMs + due.minUptimeBetweenBreaksMs;
            if (due.kind == BreakConfig.Kind.QUICK) nextQuickAtMs = reschedule;
            else                                    nextLongAtMs  = reschedule;
            return false;
        }

        // Hourly cap for QUICK.
        if (due.kind == BreakConfig.Kind.QUICK && due.maxBreaksPerHour > 0) {
            pruneHourly(now);
            if (recentBreakStarts.size() >= due.maxBreaksPerHour) {
                // Push next attempt to top of next hour.
                long oldest = recentBreakStarts.peekFirst();
                long retry = oldest + 3_600_000 + 1000;
                nextQuickAtMs = retry;
                return false;
            }
        }

        // LONG — check time-of-day window.
        if (due.kind == BreakConfig.Kind.LONG && due.windowEnabled
                && !inTimeWindow(now, due)) {
            // Push to the next window opening.
            nextLongAtMs = nextWindowOpenMs(now, due);
            return false;
        }

        // Day-of-week check.
        if (!due.allowedDays.contains(LocalDateTime.now().getDayOfWeek())) {
            if (due.kind == BreakConfig.Kind.QUICK) {
                nextQuickAtMs = now + randRange(quick.frequencyMinMs, quick.frequencyMaxMs);
            } else {
                scheduleNextLongBreak(now + 12L * 3_600_000);
            }
            return false;
        }

        // Move to PENDING — start waiting for a safe point.
        scheduleBreak(due, now);
        // Force-fire window: 10 min of gate-blocking for QUICK, 30 min for LONG.
        long forceWindow = (due.kind == BreakConfig.Kind.QUICK) ? 10L * 60_000 : 30L * 60_000;
        gateForceDeadlineMs = now + forceWindow;
        return false;
    }

    private boolean tickPending(long now) {
        boolean gatesOK = gatesPermit();
        boolean forced  = now >= gateForceDeadlineMs;
        if (!gatesOK && !forced) return false;

        // START the break.
        long duration = randRange(activeConfig.minDurationMs, activeConfig.maxDurationMs);
        breakEndAtMs = now + duration;
        state = State.ACTIVE;
        recentBreakStarts.addLast(now);
        pruneHourly(now);

        log("Break START: " + activeConfig.kind
                + " (" + activeConfig.logoutMode + ") for " + (duration / 60000) + " min.");

        if (activeConfig.logoutMode == BreakConfig.LogoutMode.LOGOUT
                && logoutAction != null) {
            try { logoutAction.run(); } catch (Throwable t) { log("Logout error: " + t.getMessage()); }
        }
        return true;   // suspend onProcess
    }

    private boolean tickActive(long now) {
        if (now < breakEndAtMs) return true;  // still breaking
        // END the break.
        long startMs = recentBreakStarts.isEmpty() ? now : recentBreakStarts.peekLast();
        history.add(new BreakRecord(
                activeConfig.kind, activeConfig.logoutMode, startMs, now));
        lastBreakEndedAtMs = now;

        boolean ramp = activeConfig.postBreakRampMs > 0;
        log("Break END. " + (ramp ? "Entering ramp mode." : ""));

        // Re-schedule the same flavour for the future.
        if (activeConfig.kind == BreakConfig.Kind.QUICK) {
            nextQuickAtMs = now + randRange(quick.frequencyMinMs, quick.frequencyMaxMs);
        } else {
            scheduleNextLongBreak(now);
        }

        if (ramp) {
            rampEndAtMs = now + activeConfig.postBreakRampMs;
            state = State.RAMP;
        } else {
            state = State.IDLE;
        }
        activeConfig = null;
        if (resumeAction != null) {
            try { resumeAction.run(); } catch (Throwable t) { log("Resume error: " + t.getMessage()); }
        }
        return false;
    }

    private boolean tickRamp(long now) {
        if (now < rampEndAtMs) return false;  // stay in ramp, but don't suspend
        state = State.IDLE;
        log("Ramp complete; back to full speed.");
        return false;
    }

    // ── Helpers ─────────────────────────────────────────────────────────
    private void scheduleBreak(BreakConfig cfg, long now) {
        activeConfig = cfg;
        state = State.PENDING;
    }

    private void scheduleNextLongBreak(long afterMs) {
        long base = Math.max(scriptStartMs + longg.minUptimeBeforeFirstBreakMs, afterMs);
        if (longg.windowEnabled) {
            nextLongAtMs = nextWindowOpenMs(base, longg);
        } else {
            nextLongAtMs = base + randRange(longg.frequencyMinMs, longg.frequencyMaxMs);
        }
    }

    private boolean inTimeWindow(long nowMs, BreakConfig cfg) {
        LocalTime now = LocalDateTime.now().toLocalTime();
        LocalTime s = cfg.windowStart, e = cfg.windowEnd;
        if (s.isBefore(e)) return !now.isBefore(s) && now.isBefore(e);
        // Crosses midnight
        return !now.isBefore(s) || now.isBefore(e);
    }

    private long nextWindowOpenMs(long fromMs, BreakConfig cfg) {
        // Advance 1 min at a time up to 48h out — coarse but cheap.
        LocalDateTime dt = LocalDateTime.now();
        for (int i = 0; i < 48 * 60; i++) {
            LocalDateTime probe = dt.plusMinutes(i);
            LocalTime t = probe.toLocalTime();
            DayOfWeek dow = probe.getDayOfWeek();
            if (!cfg.allowedDays.contains(dow)) continue;
            LocalTime s = cfg.windowStart, e = cfg.windowEnd;
            boolean inside = s.isBefore(e)
                    ? (!t.isBefore(s) && t.isBefore(e))
                    : (!t.isBefore(s) || t.isBefore(e));
            if (inside) return fromMs + (long) i * 60_000;
        }
        return fromMs + 24L * 3_600_000;
    }

    private boolean gatesPermit() {
        if (gates == null) return true;
        if (gates.isInCombat())            return false;
        if (gates.isInventoryFullOfLoot()) return false;
        if (gates.isMidTask() && activeConfig != null
                && activeConfig.kind == BreakConfig.Kind.LONG) return false;
        if (!gates.isInSafeState())        return false;
        if (gates.isStaffNearby())         return false;
        if (gates.isLowHp())               return false;
        return true;
    }

    private void pruneHourly(long now) {
        while (!recentBreakStarts.isEmpty()
                && now - recentBreakStarts.peekFirst() > 3_600_000) {
            recentBreakStarts.pollFirst();
        }
    }

    private static long randRange(long lo, long hi) {
        if (hi <= lo) return lo;
        return ThreadLocalRandom.current().nextLong(lo, hi + 1);
    }

    // ── Persistence (hand-rolled JSON) ──────────────────────────────────
    public void saveTo(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"quick\": ");  writeCfg(sb, quick); sb.append(",\n");
        sb.append("  \"long\":  ");  writeCfg(sb, longg); sb.append('\n');
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
        int qs = text.indexOf("\"quick\"");
        int ls = text.indexOf("\"long\"");
        if (qs >= 0) readCfg(text.substring(qs, ls < 0 ? text.length() : ls), quick);
        if (ls >= 0) readCfg(text.substring(ls), longg);
        // Reset scheduling so new config takes effect immediately.
        nextQuickAtMs = 0;
        nextLongAtMs = 0;
    }

    private static void writeCfg(StringBuilder sb, BreakConfig c) {
        sb.append("{\n");
        sb.append("    \"enabled\": ").append(c.enabled).append(",\n");
        sb.append("    \"minDurationMs\": ").append(c.minDurationMs).append(",\n");
        sb.append("    \"maxDurationMs\": ").append(c.maxDurationMs).append(",\n");
        sb.append("    \"frequencyMinMs\": ").append(c.frequencyMinMs).append(",\n");
        sb.append("    \"frequencyMaxMs\": ").append(c.frequencyMaxMs).append(",\n");
        sb.append("    \"maxBreaksPerHour\": ").append(c.maxBreaksPerHour).append(",\n");
        sb.append("    \"logoutMode\": \"").append(c.logoutMode.name()).append("\",\n");
        sb.append("    \"postBreakRampMs\": ").append(c.postBreakRampMs).append(",\n");
        sb.append("    \"windowEnabled\": ").append(c.windowEnabled).append(",\n");
        sb.append("    \"windowStart\": \"").append(c.windowStart).append("\",\n");
        sb.append("    \"windowEnd\": \"").append(c.windowEnd).append("\",\n");
        sb.append("    \"minUptimeBeforeFirstBreakMs\": ").append(c.minUptimeBeforeFirstBreakMs).append(",\n");
        sb.append("    \"minUptimeBetweenBreaksMs\": ").append(c.minUptimeBetweenBreaksMs).append(",\n");
        sb.append("    \"allowedDays\": [");
        boolean first = true;
        for (DayOfWeek d : c.allowedDays) {
            if (!first) sb.append(',');
            sb.append('"').append(d.name()).append('"');
            first = false;
        }
        sb.append("]\n  }");
    }

    private static void readCfg(String scope, BreakConfig c) {
        Boolean b;
        Long    l;
        Integer i;
        String  s;
        if ((b = parseBool(scope, "enabled"))      != null) c.enabled = b;
        if ((l = parseLong(scope, "minDurationMs"))!= null) c.minDurationMs = l;
        if ((l = parseLong(scope, "maxDurationMs"))!= null) c.maxDurationMs = l;
        if ((l = parseLong(scope, "frequencyMinMs"))!= null) c.frequencyMinMs = l;
        if ((l = parseLong(scope, "frequencyMaxMs"))!= null) c.frequencyMaxMs = l;
        if ((i = parseInt (scope, "maxBreaksPerHour"))!= null) c.maxBreaksPerHour = i;
        if ((s = parseStr (scope, "logoutMode"))   != null) {
            try { c.logoutMode = BreakConfig.LogoutMode.valueOf(s); } catch (Throwable ignored) {}
        }
        if ((l = parseLong(scope, "postBreakRampMs"))!= null) c.postBreakRampMs = l;
        if ((b = parseBool(scope, "windowEnabled"))!= null) c.windowEnabled = b;
        if ((s = parseStr (scope, "windowStart"))  != null) {
            try { c.windowStart = LocalTime.parse(s); } catch (Throwable ignored) {}
        }
        if ((s = parseStr (scope, "windowEnd"))    != null) {
            try { c.windowEnd = LocalTime.parse(s); } catch (Throwable ignored) {}
        }
        if ((l = parseLong(scope, "minUptimeBeforeFirstBreakMs"))!= null) c.minUptimeBeforeFirstBreakMs = l;
        if ((l = parseLong(scope, "minUptimeBetweenBreaksMs"))   != null) c.minUptimeBetweenBreaksMs = l;
        // allowedDays: if present, replace.
        int ad = scope.indexOf("\"allowedDays\"");
        if (ad >= 0) {
            int lb = scope.indexOf('[', ad);
            int rb = scope.indexOf(']', lb);
            if (lb >= 0 && rb >= 0) {
                EnumSet<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
                for (String tok : scope.substring(lb + 1, rb).split(",")) {
                    String t = tok.replace('"', ' ').trim();
                    if (t.isEmpty()) continue;
                    try { set.add(DayOfWeek.valueOf(t)); } catch (Throwable ignored) {}
                }
                if (!set.isEmpty()) {
                    c.allowedDays.clear();
                    c.allowedDays.addAll(set);
                }
            }
        }
    }

    private static Long parseLong(String text, String key) {
        String v = rawVal(text, key); if (v == null) return null;
        try { return Long.parseLong(v.trim()); } catch (Throwable t) { return null; }
    }
    private static Integer parseInt(String text, String key) {
        String v = rawVal(text, key); if (v == null) return null;
        try { return Integer.parseInt(v.trim()); } catch (Throwable t) { return null; }
    }
    private static Boolean parseBool(String text, String key) {
        String v = rawVal(text, key); if (v == null) return null;
        String t = v.trim().toLowerCase();
        if (t.startsWith("true"))  return Boolean.TRUE;
        if (t.startsWith("false")) return Boolean.FALSE;
        return null;
    }
    private static String parseStr(String text, String key) {
        int k = text.indexOf('"' + key + '"'); if (k < 0) return null;
        int c = text.indexOf(':', k); if (c < 0) return null;
        int q1 = text.indexOf('"', c + 1); if (q1 < 0) return null;
        int q2 = text.indexOf('"', q1 + 1); if (q2 < 0) return null;
        return text.substring(q1 + 1, q2);
    }
    private static String rawVal(String text, String key) {
        int k = text.indexOf('"' + key + '"'); if (k < 0) return null;
        int c = text.indexOf(':', k); if (c < 0) return null;
        int end = c + 1;
        while (end < text.length() && ",\n}]".indexOf(text.charAt(end)) < 0) end++;
        return text.substring(c + 1, end);
    }
}
