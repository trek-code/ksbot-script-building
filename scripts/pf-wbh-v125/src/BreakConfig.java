package pfwbh125;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Configuration for one break type (Quick or Long). Both kinds share the
 * same shape; defaults differ.
 *
 * <ul>
 *   <li>QUICK  — 5–30 min; frequency-based; smoke / bathroom / phone call</li>
 *   <li>LONG   — sleep / work shift; time-window-based; 6–9 hrs</li>
 * </ul>
 */
public final class BreakConfig {

    /** Which break flavour this config represents. */
    public enum Kind { QUICK, LONG }

    /** How the bot "leaves" during a break. */
    public enum LogoutMode {
        /** Click the logout widget and leave the game. */
        LOGOUT,
        /** Stay logged in; short-circuit onProcess. Reason tolerates 24h AFK. */
        AFK_IN_PLACE
    }

    public final Kind kind;

    // Enable toggle
    public volatile boolean enabled;

    // Break-duration window (min/max ms)
    public volatile long minDurationMs;
    public volatile long maxDurationMs;

    // Frequency window — "next break in [min, max] ms of uptime" (QUICK).
    // For LONG breaks, only used as fallback when time-window is disabled.
    public volatile long frequencyMinMs;
    public volatile long frequencyMaxMs;

    // Hourly cap — QUICK only; 0 disables.
    public volatile int maxBreaksPerHour;

    // How to leave
    public volatile LogoutMode logoutMode;

    // Post-break ramp length (ms). 0 disables ramp.
    public volatile long postBreakRampMs;

    // Day-of-week allow list — LONG breaks only (mostly). Default: all days.
    public final Set<DayOfWeek> allowedDays = EnumSet.allOf(DayOfWeek.class);

    // Time-of-day window (LONG only). Break will only start between
    // [windowStart, windowEnd] local time.
    public volatile boolean windowEnabled;
    public volatile LocalTime windowStart;
    public volatile LocalTime windowEnd;

    // Min uptime from script-start until first break can fire (prevents
    // breaking 10 min after launching the bot).
    public volatile long minUptimeBeforeFirstBreakMs;

    // Min uptime between the END of one break and the START of the next
    // (prevents stacked breaks).
    public volatile long minUptimeBetweenBreaksMs;

    public BreakConfig(Kind kind) {
        this.kind = kind;
        if (kind == Kind.QUICK) {
            this.enabled         = false;
            this.minDurationMs   = 5L  * 60_000;   // 5 min
            this.maxDurationMs   = 30L * 60_000;   // 30 min
            this.frequencyMinMs  = 45L * 60_000;   // every 45-90 min of uptime
            this.frequencyMaxMs  = 90L * 60_000;
            this.maxBreaksPerHour = 2;
            this.logoutMode      = LogoutMode.AFK_IN_PLACE;
            this.postBreakRampMs = 2L  * 60_000;   // 2 min groggy
            this.windowEnabled   = false;
            this.windowStart     = LocalTime.of(0, 0);
            this.windowEnd       = LocalTime.of(23, 59);
            this.minUptimeBeforeFirstBreakMs  = 30L * 60_000;
            this.minUptimeBetweenBreaksMs     = 30L * 60_000;
        } else {
            this.enabled         = false;
            this.minDurationMs   = 6L  * 3_600_000;   // 6 h
            this.maxDurationMs   = 9L  * 3_600_000;   // 9 h
            this.frequencyMinMs  = 20L * 3_600_000;   // ~daily
            this.frequencyMaxMs  = 26L * 3_600_000;
            this.maxBreaksPerHour = 0;
            this.logoutMode      = LogoutMode.LOGOUT;
            this.postBreakRampMs = 5L  * 60_000;   // 5 min groggy after sleep
            this.windowEnabled   = true;
            this.windowStart     = LocalTime.of(23, 0);
            this.windowEnd       = LocalTime.of(1, 30);   // crosses midnight
            this.minUptimeBeforeFirstBreakMs  = 4L * 3_600_000;    // 4h
            this.minUptimeBetweenBreaksMs     = 12L * 3_600_000;   // 12h
        }
    }

    public BreakConfig copy() {
        BreakConfig c = new BreakConfig(kind);
        c.enabled = enabled;
        c.minDurationMs = minDurationMs;
        c.maxDurationMs = maxDurationMs;
        c.frequencyMinMs = frequencyMinMs;
        c.frequencyMaxMs = frequencyMaxMs;
        c.maxBreaksPerHour = maxBreaksPerHour;
        c.logoutMode = logoutMode;
        c.postBreakRampMs = postBreakRampMs;
        c.allowedDays.clear();
        c.allowedDays.addAll(allowedDays);
        c.windowEnabled = windowEnabled;
        c.windowStart = windowStart;
        c.windowEnd = windowEnd;
        c.minUptimeBeforeFirstBreakMs = minUptimeBeforeFirstBreakMs;
        c.minUptimeBetweenBreaksMs = minUptimeBetweenBreaksMs;
        return c;
    }
}
