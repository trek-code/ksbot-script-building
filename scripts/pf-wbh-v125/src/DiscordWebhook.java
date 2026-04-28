package pfwbh125;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Minimal Discord-webhook client. POSTs JSON to a single webhook URL on a
 * background single-thread executor so we never block the game loop.
 * <p>
 * Which events fire is governed by {@link #enabled} (a set of {@link Event}s).
 * All events default to OFF — user turns them on individually in the UI.
 * <p>
 * Optional PNG attachments: if an event is in {@link #attachScreenshot}, the
 * executor grabs a PNG via {@link ScreenshotCapture} and POSTs a multipart
 * payload. If the capture returns null, the text-only post is sent as
 * fallback. Screenshots can optionally be mirrored to disk.
 */
public final class DiscordWebhook {

    /** Default config path — sits next to the other perk-farmer-*.json files. */
    public static final String DEFAULT_CONFIG_PATH = "perk-farmer-webhook.json";

    public enum Event {
        BOT_STARTED    ("Bot started"),
        BOT_STOPPED    ("Bot stopped"),
        FAULT          ("Fault / script error"),
        TASK_COMPLETED ("Perk task completed"),
        BOSS_DETECTED  ("Boss spawn detected"),
        BOSS_KILLED    ("Boss killed"),
        RARE_DROP      ("Rare drop received"),
        LEVEL_UP       ("Level up"),
        BREAK_STARTED  ("Break started"),
        BREAK_ENDED    ("Break ended"),
        MILESTONE      ("Session milestone (every N ore)"),
        STAFF_NEARBY   ("Staff member nearby"),
        STAFF_CHAT     ("Staff member spoke in chat");

        public final String label;
        Event(String label) { this.label = label; }
    }

    /** Comma-separated keywords. A loot drop whose lowercase name contains
     *  any of these triggers RARE_DROP. */
    private volatile String rareDropKeywords = "pet,bond,rare,mystery,blood key,spring key";

    public String  getRareDropKeywords()        { return rareDropKeywords; }
    public void    setRareDropKeywords(String s){ this.rareDropKeywords = (s == null) ? "" : s; }

    /** True iff {@code itemName} matches any configured rare-drop keyword. */
    public boolean isRareDrop(String itemName) {
        if (itemName == null || rareDropKeywords == null || rareDropKeywords.isBlank()) return false;
        String n = itemName.toLowerCase();
        for (String raw : rareDropKeywords.split(",")) {
            String kw = raw.trim().toLowerCase();
            if (!kw.isEmpty() && n.contains(kw)) return true;
        }
        return false;
    }

    private volatile String webhookUrl = "";
    /** EnumSet add/remove are not thread-safe; UI writes + send() reads from
     *  several threads, so guard mutations with a single synchronized block. */
    private final Set<Event> enabled = EnumSet.noneOf(Event.class);
    private final Set<Event> attachScreenshot = EnumSet.noneOf(Event.class);
    /** Concurrent so the rate-limit check/put can race safely from
     *  game-tick, EventBus, BreakManager, and EDT threads at once. */
    private final Map<Event, Long> lastFiredAtMs = new ConcurrentHashMap<>();

    /** Minimum ms between two fires of the same event — rate limit. */
    private volatile long perEventCooldownMs = 30_000L;

    /** Every-N-ore value for MILESTONE. */
    private volatile int milestoneEveryN = 100;

    /** Max PNG width (0 = original). Smaller = faster upload, less quality. */
    private volatile int screenshotMaxWidth = 1280;

    /** If true, also write the PNG to disk next to the jar / session dir. */
    private volatile boolean screenshotSaveToDisk = false;
    private volatile String  screenshotSaveDir    = "screenshots";

    private final ExecutorService exec = Executors.newSingleThreadExecutor(
            new ThreadFactory() {
                @Override public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "perk-farmer-webhook");
                    t.setDaemon(true);
                    return t;
                }
            });

    /** Separate thread for screenshot capture so a hanging Robot call
     *  (Windows DWM stall, locked desktop, GPU hiccup) doesn't poison
     *  the webhook executor and back up every subsequent event. */
    private final ExecutorService captureExec = Executors.newSingleThreadExecutor(
            new ThreadFactory() {
                @Override public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "perk-farmer-capture");
                    t.setDaemon(true);
                    return t;
                }
            });

    private static final long CAPTURE_TIMEOUT_MS = 3_000L;

    /** Capture with a hard deadline. Returns null if the capture takes too
     *  long, throws, or returns null itself. Cancels the future on timeout
     *  so the worker thread doesn't keep blocking the queue forever. */
    private byte[] captureWithTimeout() {
        Future<byte[]> f = captureExec.submit(
                (Callable<byte[]>) () -> ScreenshotCapture.capturePng(screenshotMaxWidth));
        try {
            return f.get(CAPTURE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            f.cancel(true);
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    public String getWebhookUrl()       { return webhookUrl; }
    public void setWebhookUrl(String s) { this.webhookUrl = s == null ? "" : s.trim(); }

    public boolean isEnabled(Event e)             { synchronized (enabled) { return enabled.contains(e); } }
    public void setEnabled(Event e, boolean on)   {
        synchronized (enabled) { if (on) enabled.add(e); else enabled.remove(e); }
    }

    public boolean isAttachScreenshot(Event e)           {
        synchronized (attachScreenshot) { return attachScreenshot.contains(e); }
    }
    public void    setAttachScreenshot(Event e, boolean on) {
        synchronized (attachScreenshot) { if (on) attachScreenshot.add(e); else attachScreenshot.remove(e); }
    }

    public int  getMilestoneEveryN()           { return milestoneEveryN; }
    public void setMilestoneEveryN(int n)      { this.milestoneEveryN = Math.max(1, n); }

    public int  getScreenshotMaxWidth()        { return screenshotMaxWidth; }
    public void setScreenshotMaxWidth(int w)   { this.screenshotMaxWidth = Math.max(0, w); }

    public boolean isScreenshotSaveToDisk()    { return screenshotSaveToDisk; }
    public void    setScreenshotSaveToDisk(boolean b) { this.screenshotSaveToDisk = b; }

    public String  getScreenshotSaveDir()          { return screenshotSaveDir; }
    public void    setScreenshotSaveDir(String s)  {
        this.screenshotSaveDir = (s == null || s.isBlank()) ? "screenshots" : s.trim();
    }

    /** Fire a message if the event is enabled and not rate-limited.
     *  Thread-safe: rate-limit gate uses an atomic CHM merge so concurrent
     *  callers from game-tick, EventBus, BreakManager, and EDT threads
     *  can't double-fire or corrupt the map. */
    public void send(Event e, String message) {
        if (!isEnabled(e)) return;
        if (webhookUrl.isEmpty()) return;
        final long now = System.currentTimeMillis();
        // merge() atomically reads + decides. We mark "fire allowed" by
        // writing `now` only when the previous value was null OR older than
        // the cooldown. If we DON'T fire, we keep the old value.
        final boolean[] fire = { false };
        lastFiredAtMs.merge(e, now, (oldVal, newVal) -> {
            if (newVal - oldVal >= perEventCooldownMs) { fire[0] = true; return newVal; }
            return oldVal;   // keep old, don't fire
        });
        // First-ever fire (no prior entry) — merge inserted `now` but the
        // remap function never ran, so the flag is still false. Fix that.
        if (!fire[0] && lastFiredAtMs.get(e) == now) fire[0] = true;
        if (!fire[0]) return;

        final String url    = webhookUrl;
        final String full   = "[" + e.label + "] " + message;
        final boolean wantPng = isAttachScreenshot(e);
        final Event   evRef = e;
        exec.submit(() -> {
            byte[] png = null;
            if (wantPng) {
                png = captureWithTimeout();   // hard 3s deadline
                if (png != null && screenshotSaveToDisk) savePngToDisk(evRef, png);
            }
            try {
                if (png != null) {
                    String fname = "rspf-" + evRef.name().toLowerCase() + "-"
                            + System.currentTimeMillis() + ".png";
                    postMultipartTo(url, full, png, fname);
                } else {
                    postJsonTo(url, full);
                }
            } catch (Throwable ignored) {}
        });
    }

    /** Test button — bypasses enabled-event filter. Always JSON (no screenshot). */
    public String testPing() {
        if (webhookUrl.isEmpty()) return "No webhook URL set.";
        try {
            postJsonTo(webhookUrl,
                    "Webhook test from RS Perk Farmer  (" + new java.util.Date() + ")");
            return "OK — test message posted.";
        } catch (Throwable t) {
            return "Error: " + t.getMessage();
        }
    }

    /** Test button for screenshot attachments. Bypasses enabled-event filter. */
    public String testPingWithScreenshot() {
        if (webhookUrl.isEmpty()) return "No webhook URL set.";
        try {
            byte[] png = captureWithTimeout();
            if (png == null) {
                return "Capture failed — posting text only. Check that the client window is visible.";
            }
            if (screenshotSaveToDisk) savePngToDisk(null, png);
            String fname = "rspf-test-" + System.currentTimeMillis() + ".png";
            postMultipartTo(webhookUrl,
                    "Screenshot test from RS Perk Farmer (" + new java.util.Date() + ")",
                    png, fname);
            return "OK — screenshot posted (" + (png.length / 1024) + " KB).";
        } catch (Throwable t) {
            return "Error: " + t.getMessage();
        }
    }

    // ── HTTP ────────────────────────────────────────────────────────────

    private static void postJsonTo(String url, String message) throws Exception {
        String body = "{\"content\":\"" + jsonEscape(message) + "\"}";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        conn.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        conn.setDoOutput(true);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) { os.write(bytes); }
        int code = conn.getResponseCode();
        conn.disconnect();
        if (code / 100 != 2) throw new RuntimeException("HTTP " + code);
    }

    /** multipart/form-data POST with one JSON payload + one PNG attachment. */
    private static void postMultipartTo(String url, String message,
                                        byte[] pngBytes, String filename) throws Exception {
        String boundary = "----rspf" + Long.toHexString(System.nanoTime());
        String payloadJson = "{\"content\":\"" + jsonEscape(message) + "\"}";

        ByteArrayOutputStream body = new ByteArrayOutputStream(pngBytes.length + 1024);

        // Part 1 — payload_json
        writeAscii(body, "--" + boundary + "\r\n");
        writeAscii(body, "Content-Disposition: form-data; name=\"payload_json\"\r\n");
        writeAscii(body, "Content-Type: application/json\r\n\r\n");
        body.write(payloadJson.getBytes(StandardCharsets.UTF_8));
        writeAscii(body, "\r\n");

        // Part 2 — files[0]
        writeAscii(body, "--" + boundary + "\r\n");
        writeAscii(body, "Content-Disposition: form-data; name=\"files[0]\"; filename=\""
                + filename + "\"\r\n");
        writeAscii(body, "Content-Type: image/png\r\n\r\n");
        body.write(pngBytes);
        writeAscii(body, "\r\n");

        writeAscii(body, "--" + boundary + "--\r\n");

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        conn.setReadTimeout((int) Duration.ofSeconds(20).toMillis());   // uploads can be slow
        conn.setDoOutput(true);
        conn.setFixedLengthStreamingMode(body.size());
        try (OutputStream os = conn.getOutputStream()) { body.writeTo(os); }
        int code = conn.getResponseCode();
        conn.disconnect();
        if (code / 100 != 2) throw new RuntimeException("HTTP " + code);
    }

    private static void writeAscii(ByteArrayOutputStream os, String s) {
        byte[] b = s.getBytes(StandardCharsets.US_ASCII);
        os.write(b, 0, b.length);
    }

    // ── Disk save ────────────────────────────────────────────────────────

    private void savePngToDisk(Event e, byte[] png) {
        try {
            File dir = new File(screenshotSaveDir);
            if (!dir.exists() && !dir.mkdirs()) return;
            String tag = (e == null) ? "test" : e.name().toLowerCase();
            File out = new File(dir, "rspf-" + tag + "-" + System.currentTimeMillis() + ".png");
            try (FileOutputStream fos = new FileOutputStream(out)) { fos.write(png); }
        } catch (Throwable ignored) {}
    }

    // ── JSON escape ──────────────────────────────────────────────────────

    // ── Persistence (hand-rolled JSON, same style as DelayProfile) ───────

    public void saveTo(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"WEBHOOK_URL\": \"").append(jsonEscape(webhookUrl)).append("\",\n");
        sb.append("  \"MILESTONE_EVERY_N\": ").append(milestoneEveryN).append(",\n");
        sb.append("  \"SCREENSHOT_MAX_WIDTH\": ").append(screenshotMaxWidth).append(",\n");
        sb.append("  \"SCREENSHOT_SAVE_TO_DISK\": ").append(screenshotSaveToDisk).append(",\n");
        sb.append("  \"SCREENSHOT_SAVE_DIR\": \"").append(jsonEscape(screenshotSaveDir)).append("\",\n");
        sb.append("  \"RARE_DROP_KEYWORDS\": \"").append(jsonEscape(rareDropKeywords)).append("\",\n");
        sb.append("  \"ENABLED_EVENTS\": [");
        boolean first = true;
        for (Event e : Event.values()) {
            if (!enabled.contains(e)) continue;
            if (!first) sb.append(", ");
            sb.append('"').append(e.name()).append('"');
            first = false;
        }
        sb.append("],\n");
        sb.append("  \"ATTACH_SCREENSHOT_EVENTS\": [");
        first = true;
        for (Event e : Event.values()) {
            if (!attachScreenshot.contains(e)) continue;
            if (!first) sb.append(", ");
            sb.append('"').append(e.name()).append('"');
            first = false;
        }
        sb.append("]\n");
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
        String url = parseString(text, "WEBHOOK_URL");
        if (url != null) webhookUrl = url;
        Integer mi = parseInt(text, "MILESTONE_EVERY_N");
        if (mi != null) milestoneEveryN = Math.max(1, mi);
        Integer sw = parseInt(text, "SCREENSHOT_MAX_WIDTH");
        if (sw != null) screenshotMaxWidth = Math.max(0, sw);
        Boolean sd = parseBool(text, "SCREENSHOT_SAVE_TO_DISK");
        if (sd != null) screenshotSaveToDisk = sd;
        String dir = parseString(text, "SCREENSHOT_SAVE_DIR");
        if (dir != null && !dir.isBlank()) screenshotSaveDir = dir;
        String kw = parseString(text, "RARE_DROP_KEYWORDS");
        if (kw != null) rareDropKeywords = kw;
        enabled.clear();
        for (String name : parseStringArray(text, "ENABLED_EVENTS")) {
            try { enabled.add(Event.valueOf(name)); } catch (IllegalArgumentException ignored) {}
        }
        attachScreenshot.clear();
        for (String name : parseStringArray(text, "ATTACH_SCREENSHOT_EVENTS")) {
            try { attachScreenshot.add(Event.valueOf(name)); } catch (IllegalArgumentException ignored) {}
        }
    }

    /** Attempts to load from {@link #DEFAULT_CONFIG_PATH}. Silent no-op if missing. */
    public boolean tryLoadDefault() {
        File f = new File(DEFAULT_CONFIG_PATH);
        if (!f.isFile()) return false;
        try { loadFrom(f.getAbsolutePath()); return true; }
        catch (Throwable t) { return false; }
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

    private static String parseString(String text, String key) {
        int k = text.indexOf('"' + key + '"');
        if (k < 0) return null;
        int colon = text.indexOf(':', k);
        if (colon < 0) return null;
        int openQ = text.indexOf('"', colon + 1);
        if (openQ < 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = openQ + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char n = text.charAt(i + 1);
                switch (n) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    default:   sb.append(n);
                }
                i++;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return null;
    }

    private static java.util.List<String> parseStringArray(String text, String key) {
        java.util.List<String> out = new java.util.ArrayList<>();
        int k = text.indexOf('"' + key + '"');
        if (k < 0) return out;
        int lb = text.indexOf('[', k);
        int rb = text.indexOf(']', lb);
        if (lb < 0 || rb < 0) return out;
        String inner = text.substring(lb + 1, rb);
        int i = 0;
        while (i < inner.length()) {
            int q1 = inner.indexOf('"', i);
            if (q1 < 0) break;
            int q2 = inner.indexOf('"', q1 + 1);
            if (q2 < 0) break;
            out.add(inner.substring(q1 + 1, q2));
            i = q2 + 1;
        }
        return out;
    }

    private static int indexOfAny(String text, int from, String chars) {
        int best = -1;
        for (int i = 0; i < chars.length(); i++) {
            int idx = text.indexOf(chars.charAt(i), from);
            if (idx >= 0 && (best < 0 || idx < best)) best = idx;
        }
        return best;
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else          sb.append(c);
            }
        }
        return sb.toString();
    }
}
