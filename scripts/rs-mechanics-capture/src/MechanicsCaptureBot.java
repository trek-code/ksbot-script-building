package rsmechanicscapture;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.EventBus;
import rs.kreme.ksbot.api.API;
import rs.kreme.ksbot.api.scripts.Category;
import rs.kreme.ksbot.api.scripts.Script;
import rs.kreme.ksbot.api.scripts.ScriptManifest;
import rs.kreme.ksbot.api.wrappers.KSNPC;
import rs.kreme.ksbot.api.wrappers.KSPlayer;

import javax.swing.SwingUtilities;
import java.io.*;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * <b>Mechanics Capture</b> — observer-only recording script.
 *
 * <p>The user fights normally; this script silently captures every animation ID,
 * graphic ID, NPC ID, NPC position trail, and chat event within the configured
 * radius and writes them to five output files in:
 * {@code C:\Users\jonez\.kreme\servers\<server>\mechanics-captures\<label-datetime>\}
 *
 * <ul>
 *   <li>{@code ticks.csv}   — per-tick player snapshot</li>
 *   <li>{@code npcs.csv}    — per-tick NPC data (all NPCs in radius)</li>
 *   <li>{@code events.csv}  — deduplicated new-ID events + bookmarks</li>
 *   <li>{@code chat.txt}    — all chat during the session</li>
 *   <li>{@code summary.txt} — human-readable session report (written on stop)</li>
 * </ul>
 *
 * <p>The live UI ({@link MechanicsCapturePanel}) shows a tile map and a
 * deduplicated mechanics table — one row per unique animation / graphic / NPC
 * ID seen this session.
 */
@ScriptManifest(
        name        = "Mechanics Capture",
        author      = "you",
        version     = 1.0,
        uid         = "kreme.rs.mechanicscapture",
        description = "Observer-only boss mechanics recorder — animations, graphics, NPC IDs, position trails.",
        category    = Category.OTHER,
        servers     = {"Reason"},
        sponsor     = false,
        vip         = false,
        image       = ""
)
public class MechanicsCaptureBot extends Script {

    // ── Config (written by panel before startCapture()) ───────────────────
    volatile String captureLabel  = "boss";
    volatile int    captureRadius = 15;
    volatile String serverName    = "Reason";

    // ── Session state ─────────────────────────────────────────────────────
    volatile boolean capturing      = false;
    volatile int     tickCount      = 0;
    volatile long    sessionStartMs = 0L;
    volatile String  outputDir      = null;

    // ── Deduplicated mechanic sets ────────────────────────────────────────
    // Each set contains IDs seen this session — add() returns true only once.
    final Set<Integer>        seenNpcIds  = new LinkedHashSet<>();
    final Set<Integer>        seenAnimIds = new LinkedHashSet<>();
    final Set<Integer>        seenGfxIds  = new LinkedHashSet<>();
    final List<MechanicEntry> mechanicLog = new CopyOnWriteArrayList<>();

    // ── NPC movement trails: npc_id → ordered list of positions ──────────
    final Map<Integer, List<WorldPoint>> npcTrails = new LinkedHashMap<>();

    // ── Output writers (null when not capturing) ──────────────────────────
    private PrintWriter ticksWriter;
    private PrintWriter npcsWriter;
    private PrintWriter eventsWriter;
    private PrintWriter chatWriter;

    // ── Panel & event bus ─────────────────────────────────────────────────
    private MechanicsCapturePanel panel;
    private Consumer<ChatMessage>  chatConsumer;
    private EventBus.Subscriber    chatSubscriber;

    // ── Formatters ────────────────────────────────────────────────────────
    private static final SimpleDateFormat TS_SHORT = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat TS_DATE  = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    // ═════════════════════════════════════════════════════════════════════
    // Script lifecycle
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public boolean onStart() {
        panel = new MechanicsCapturePanel(this);
        SwingUtilities.invokeLater(panel::buildAndShow);

        chatConsumer = ev -> {
            if (!capturing || chatWriter == null) return;
            String sender  = ev.getName();
            String message = stripTags(ev.getMessage());
            String line    = "[" + nowTs() + "] " + (sender != null ? sender + ": " : "") + message;
            chatWriter.println(line);
            chatWriter.flush();
            panel.appendEvent("[CHAT] " + line);
        };
        try {
            chatSubscriber = API.getEventBus().register(ChatMessage.class, chatConsumer, 0f);
        } catch (Throwable t) {
            System.out.println("[mechanics-capture] Could not register ChatMessage listener: " + t.getMessage());
        }

        setStatus("Ready — press Start in the Mechanics Capture panel.");
        return true;
    }

    @Override
    public int onProcess() {
        if (!capturing) return 500;

        tickCount++;
        long now = System.currentTimeMillis();

        // ── Local player ─────────────────────────────────────────────────
        KSPlayer me      = null;
        WorldPoint myLoc = null;
        try { me    = ctx.players.getLocal(); }                  catch (Throwable t) {}
        try { if (me != null) myLoc = me.getWorldLocation(); }  catch (Throwable t) {}
        if (myLoc == null) return 600;

        // ── Gather nearby entities ────────────────────────────────────────
        List<?> rawNpcs    = gatherNpcs();
        List<?> rawPlayers = gatherPlayers();
        List<?> rawGfx     = gatherGfx(myLoc);

        int hp = -1;
        try { hp = ctx.combat.getCurrentHealth(); } catch (Throwable t) {}

        // ── Write tick row ────────────────────────────────────────────────
        writeTick(now, myLoc, hp, rawNpcs.size(), rawGfx.size(), rawPlayers.size());

        // ── Process NPCs ──────────────────────────────────────────────────
        List<TileEntity> entities = new ArrayList<>();

        for (Object raw : rawNpcs) {
            if (!(raw instanceof KSNPC)) continue;
            KSNPC npc = (KSNPC) raw;

            int        npcId   = -1;
            String     npcName = "?";
            WorldPoint loc     = null;
            int        anim    = -1;
            boolean    alive   = false;

            try { npcId   = npc.getId(); }            catch (Throwable t) {}
            try { npcName = npc.getName(); }           catch (Throwable t) {}
            try { loc     = npc.getWorldLocation(); }  catch (Throwable t) {}
            try { anim    = npc.getAnimation(); }      catch (Throwable t) {}
            try { alive   = npc.isAlive(); }           catch (Throwable t) {}

            if (npcId < 0) continue;

            writeNpcRow(now, npcId, npcName, loc, anim, alive);

            // ── Deduplicate NPC ID ──────────────────────────────────────
            if (seenNpcIds.add(npcId)) {
                MechanicEntry e = new MechanicEntry("NPC", npcId, npcName, tickCount);
                mechanicLog.add(e);
                panel.addMechanicRow(e);
                writeEvent(now, "NEW_NPC", "id=" + npcId + " name=" + npcName);
                panel.appendEvent("[NEW NPC] id=" + npcId + " \"" + npcName + "\" — tick " + tickCount);
            }

            // ── Deduplicate NPC animation ID ────────────────────────────
            if (anim > 0 && seenAnimIds.add(anim)) {
                String desc = "from NPC \"" + npcName + "\" (id=" + npcId + ")";
                MechanicEntry e = new MechanicEntry("NPC_ANIM", anim, desc, tickCount);
                mechanicLog.add(e);
                panel.addMechanicRow(e);
                writeEvent(now, "NEW_ANIM", "anim=" + anim + " " + desc);
                panel.appendEvent("[NEW ANIM] id=" + anim + " from " + npcName + " — tick " + tickCount);
            }

            // ── NPC movement trail ──────────────────────────────────────
            if (loc != null) {
                List<WorldPoint> trail = npcTrails.computeIfAbsent(npcId, k -> new ArrayList<>());
                if (trail.isEmpty() || !loc.equals(trail.get(trail.size() - 1))) {
                    trail.add(loc);
                }
                entities.add(new TileEntity(TileEntity.Type.NPC, loc, npcName, npcId));
            }
        }

        // ── Process players (other players + self-graphics detection) ─────
        for (Object raw : rawPlayers) {
            if (!(raw instanceof KSPlayer)) continue;
            KSPlayer player = (KSPlayer) raw;

            WorldPoint plLoc  = null;
            int        plGfx  = -1;
            String     plName = "?";

            try { plLoc  = player.getWorldLocation(); } catch (Throwable t) {}
            try { plGfx  = player.getGraphics(); }       catch (Throwable t) {}
            try { plName = player.getName(); }            catch (Throwable t) {}

            // Add non-self players to tile map
            if (plLoc != null && !plLoc.equals(myLoc)) {
                entities.add(new TileEntity(TileEntity.Type.PLAYER, plLoc, plName, -1));
            }

            // Deduplicate graphics on players (AOE spells hitting them)
            if (plGfx > 0 && seenGfxIds.add(plGfx)) {
                String desc = "on player \"" + plName + "\"";
                MechanicEntry e = new MechanicEntry("PLAYER_GFX", plGfx, desc, tickCount);
                mechanicLog.add(e);
                panel.addMechanicRow(e);
                writeEvent(now, "NEW_GFX", "gfx=" + plGfx + " " + desc);
                panel.appendEvent("[NEW GFX] id=" + plGfx + " on player " + plName + " — tick " + tickCount);
            }
        }

        // ── Process graphics objects (ground AOE markers) ─────────────────
        for (Object gfxObj : rawGfx) {
            int        gfxId  = reflectGetId(gfxObj);
            WorldPoint gfxLoc = reflectGetWorldLocation(gfxObj);

            if (gfxId < 0) continue;

            if (gfxLoc != null) {
                entities.add(new TileEntity(TileEntity.Type.GFX, gfxLoc, "gfx:" + gfxId, gfxId));
            }

            if (seenGfxIds.add(gfxId)) {
                String locStr = gfxLoc != null ? gfxLoc.getX() + "," + gfxLoc.getY() : "?";
                String desc   = "ground graphic at " + locStr;
                MechanicEntry e = new MechanicEntry("GFX", gfxId, desc, tickCount);
                mechanicLog.add(e);
                panel.addMechanicRow(e);
                writeEvent(now, "NEW_GFX", "gfx=" + gfxId + " at " + locStr);
                panel.appendEvent("[NEW GFX] id=" + gfxId + " at " + locStr + " — tick " + tickCount);
            }
        }

        if (npcsWriter != null) npcsWriter.flush();

        // ── Update panel ──────────────────────────────────────────────────
        panel.updateSnapshot(myLoc, entities, captureRadius);
        panel.updateStats(tickCount, seenNpcIds.size(), seenAnimIds.size(),
                seenGfxIds.size(), now - sessionStartMs);

        setStatus("Capturing — tick " + tickCount
                + " | NPCs:" + seenNpcIds.size()
                + " Anims:" + seenAnimIds.size()
                + " GFX:" + seenGfxIds.size());

        return 600; // ~one RS game tick
    }

    @Override
    public void onStop() {
        capturing = false;
        if (chatSubscriber != null) {
            try { API.getEventBus().unregister(chatSubscriber); } catch (Throwable ignored) {}
        }
        writeSummary();
        closeWriters();
        if (panel != null) SwingUtilities.invokeLater(panel::onScriptStop);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Panel → Bot API
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Called by the panel Start button. Resets all session state and opens
     * the output files.
     */
    void startCapture(String label, int radius, String server) {
        this.captureLabel   = label;
        this.captureRadius  = radius;
        this.serverName     = server;
        this.tickCount      = 0;
        this.sessionStartMs = System.currentTimeMillis();

        seenNpcIds.clear();
        seenAnimIds.clear();
        seenGfxIds.clear();
        mechanicLog.clear();
        npcTrails.clear();

        String dateStr = TS_DATE.format(new Date(sessionStartMs));
        this.outputDir = "C:\\Users\\jonez\\.kreme\\servers\\" + server
                + "\\mechanics-captures\\" + label + "-" + dateStr + "\\";
        try { new File(outputDir).mkdirs(); } catch (Throwable t) {}

        try {
            ticksWriter  = openWriter(outputDir + "ticks.csv");
            npcsWriter   = openWriter(outputDir + "npcs.csv");
            eventsWriter = openWriter(outputDir + "events.csv");
            chatWriter   = openWriter(outputDir + "chat.txt");

            ticksWriter .println("tick,timestamp_ms,player_x,player_y,plane,hp,npc_count,gfx_count,player_count");
            npcsWriter  .println("tick,npc_id,npc_name,x,y,animation,alive");
            eventsWriter.println("tick,timestamp_ms,type,description");
            ticksWriter.flush(); npcsWriter.flush(); eventsWriter.flush();
        } catch (IOException ex) {
            panel.appendEvent("[ERROR] Cannot open output files: " + ex.getMessage());
            return;
        }

        capturing = true;
        writeEvent(sessionStartMs, "SESSION_START",
                "label=" + label + " radius=" + radius + " server=" + server);
        panel.appendEvent("[SESSION] Started — output: " + outputDir);
    }

    /**
     * Called by the panel Stop button (manual stop). Does not stop the KSBot
     * script process itself — only pauses recording.
     */
    void stopCapture() {
        if (!capturing) return;
        capturing = false;
        writeSummary();
        closeWriters();
        panel.appendEvent("[SESSION] Stopped — " + tickCount + " ticks recorded.");
        setStatus("Idle — press Start to begin a new capture.");
    }

    /** Insert a named bookmark into events.csv and the event log. */
    void addBookmark(String note) {
        if (!capturing) return;
        String msg = note.isEmpty() ? "tick " + tickCount : note;
        writeEvent(System.currentTimeMillis(), "BOOKMARK", msg);
        panel.appendEvent("[BOOKMARK] " + msg + " — tick " + tickCount);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Gather helpers
    // ═════════════════════════════════════════════════════════════════════

    private List<?> gatherNpcs() {
        try { return ctx.npcs.query().withinDistance(captureRadius).list(); }
        catch (Throwable t) { return Collections.emptyList(); }
    }

    private List<?> gatherPlayers() {
        try { return ctx.players.query().withinDistance(captureRadius).list(); }
        catch (Throwable t) { return Collections.emptyList(); }
    }

    private List<?> gatherGfx(WorldPoint center) {
        try { return ctx.graphicsObjects.query().within(center, captureRadius).list(); }
        catch (Throwable t) { return Collections.emptyList(); }
    }

    // ═════════════════════════════════════════════════════════════════════
    // CSV / file helpers
    // ═════════════════════════════════════════════════════════════════════

    private void writeTick(long now, WorldPoint loc, int hp,
                           int npcCount, int gfxCount, int playerCount) {
        if (ticksWriter == null) return;
        ticksWriter.println(tickCount + "," + now
                + "," + loc.getX() + "," + loc.getY() + "," + loc.getPlane()
                + "," + hp + "," + npcCount + "," + gfxCount + "," + playerCount);
        ticksWriter.flush();
    }

    private void writeNpcRow(long now, int npcId, String name,
                             WorldPoint loc, int anim, boolean alive) {
        if (npcsWriter == null) return;
        npcsWriter.println(tickCount + "," + npcId + "," + csv(name)
                + "," + (loc != null ? loc.getX() : "?")
                + "," + (loc != null ? loc.getY() : "?")
                + "," + anim + "," + alive);
    }

    private void writeEvent(long now, String type, String desc) {
        if (eventsWriter == null) return;
        eventsWriter.println(tickCount + "," + now + "," + type + "," + csv(desc));
        eventsWriter.flush();
    }

    private void writeSummary() {
        if (outputDir == null || sessionStartMs == 0) return;
        try (PrintWriter w = openWriter(outputDir + "summary.txt")) {
            long elapsed = System.currentTimeMillis() - sessionStartMs;
            w.println("==========================================");
            w.println("  Mechanics Capture -- Session Summary");
            w.println("==========================================");
            w.println("Label    : " + captureLabel);
            w.println("Server   : " + serverName);
            w.println("Started  : " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(sessionStartMs)));
            w.println("Duration : " + formatDuration(elapsed));
            w.println("Ticks    : " + tickCount);
            w.println("Radius   : " + captureRadius + " tiles");
            w.println("");

            w.println("-- Unique NPCs (" + seenNpcIds.size() + ") ----------------------");
            for (MechanicEntry e : mechanicLog) {
                if ("NPC".equals(e.type))
                    w.println("  npc_id=" + e.id + "  name=" + e.description + "  first_tick=" + e.firstTick);
            }

            w.println("");
            w.println("-- Unique NPC Animations (" + seenAnimIds.size() + ") ----------");
            for (MechanicEntry e : mechanicLog) {
                if ("NPC_ANIM".equals(e.type))
                    w.println("  anim_id=" + e.id + "  " + e.description + "  first_tick=" + e.firstTick);
            }

            w.println("");
            w.println("-- Unique Graphics / AOE Markers (" + seenGfxIds.size() + ") --");
            for (MechanicEntry e : mechanicLog) {
                if ("GFX".equals(e.type) || "PLAYER_GFX".equals(e.type))
                    w.println("  gfx_id=" + e.id + "  " + e.description + "  first_tick=" + e.firstTick);
            }

            w.println("");
            w.println("-- NPC Movement Trails ---------------------");
            for (Map.Entry<Integer, List<WorldPoint>> trail : npcTrails.entrySet()) {
                int npcId = trail.getKey();
                List<WorldPoint> pts = trail.getValue();
                w.println("  npc_id=" + npcId + "  positions=" + pts.size());
                for (WorldPoint wp : pts) {
                    w.println("    " + wp.getX() + "," + wp.getY() + "," + wp.getPlane());
                }
            }

            w.println("");
            w.println("-- Notes -----------------------------------");
            w.println("  Animation-to-damage correlation: see events.csv NEW_ANIM entries,");
            w.println("  then cross-reference tick numbers with npcs.csv animation column.");
            w.println("  Projectile data: not available in current KSBot API.");
            w.println("==========================================");
        } catch (IOException ex) {
            System.out.println("[mechanics-capture] Could not write summary: " + ex.getMessage());
        }
    }

    private void closeWriters() {
        for (PrintWriter w : new PrintWriter[]{ticksWriter, npcsWriter, eventsWriter, chatWriter}) {
            try { if (w != null) w.close(); } catch (Throwable ignored) {}
        }
        ticksWriter = npcsWriter = eventsWriter = chatWriter = null;
    }

    private static PrintWriter openWriter(String path) throws IOException {
        return new PrintWriter(new BufferedWriter(new FileWriter(path, false)));
    }

    // ═════════════════════════════════════════════════════════════════════
    // Reflection helpers (for graphics object wrappers of unknown type)
    // ═════════════════════════════════════════════════════════════════════

    /** Call {@code getId()} on an object of unknown wrapper type via reflection. */
    static int reflectGetId(Object obj) {
        if (obj == null) return -1;
        try {
            Method m = obj.getClass().getMethod("getId");
            Object r = m.invoke(obj);
            return r instanceof Number ? ((Number) r).intValue() : -1;
        } catch (Throwable t) { return -1; }
    }

    /** Try {@code getWorldLocation()} on an object of unknown wrapper type. */
    static WorldPoint reflectGetWorldLocation(Object obj) {
        if (obj == null) return null;
        try {
            Method m = obj.getClass().getMethod("getWorldLocation");
            Object r = m.invoke(obj);
            return r instanceof WorldPoint ? (WorldPoint) r : null;
        } catch (Throwable ignored) {}
        return null;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Static utilities
    // ═════════════════════════════════════════════════════════════════════

    static String nowTs()             { return TS_SHORT.format(new Date()); }
    static String csv(String s)       { return s == null ? "" : "\"" + s.replace("\"", "\"\"") + "\""; }
    static String stripTags(String s) { return s == null ? "" : s.replaceAll("<[^>]+>", ""); }
    static String formatDuration(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60;
        return String.format("%02d:%02d:%02d", h, m % 60, s % 60);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Inner data classes
    // ═════════════════════════════════════════════════════════════════════

    /**
     * One row in the live deduplicated mechanics table.
     * <ul>
     *   <li>type — NPC | NPC_ANIM | GFX | PLAYER_GFX</li>
     *   <li>id   — the raw game ID</li>
     *   <li>description — human-readable context (NPC name, location, etc.)</li>
     *   <li>firstTick — tick number when this ID was first observed</li>
     * </ul>
     */
    static final class MechanicEntry {
        final String type;
        final int    id;
        final String description;
        final int    firstTick;

        MechanicEntry(String type, int id, String description, int firstTick) {
            this.type        = type;
            this.id          = id;
            this.description = description;
            this.firstTick   = firstTick;
        }
    }

    /** Tile-map entity passed to the panel each tick. */
    static final class TileEntity {
        enum Type { NPC, PLAYER, GFX }

        final Type       type;
        final WorldPoint tile;
        final String     label;
        final int        id;

        TileEntity(Type type, WorldPoint tile, String label, int id) {
            this.type  = type;
            this.tile  = tile;
            this.label = label;
            this.id    = id;
        }
    }
}
