package rsperkfarmerv103;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.EventBus;
import rs.kreme.ksbot.api.API;
import rs.kreme.ksbot.api.scripts.Category;
import rs.kreme.ksbot.api.scripts.Script;
import rs.kreme.ksbot.api.scripts.ScriptManifest;
import rs.kreme.ksbot.api.wrappers.KSNPC;
import rs.kreme.ksbot.api.wrappers.KSObject;
import rs.kreme.ksbot.api.wrappers.KSWidget;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <b>RS Perk Farmer</b> — fully automated perk-task grinder for Reason's
 * Donator Zone. Walks to the Perk Master, requests an Elite Skilling task,
 * picks the Runite ore option, mines, banks, repeats. Handles Spring Creature
 * random events inline.
 *
 * <p>The UI lives in {@link RsPerkFarmerPanel}: Start / Pause / Stop / Update
 * buttons, a status bar (current action, next action, countdown), plus a
 * Debug tab that can dump the full bot state to disk.
 */
@ScriptManifest(
        name        = "RS Perk Farmer v1.03",
        author      = "you",
        version     = 1.03,
        uid         = "kreme.rs.perkfarmer.v103",
        description = "Auto Elite Skilling perk tasks (Runite ore) at the Donator Zone.",
        category    = Category.MINING,
        servers     = {"Reason"},
        sponsor     = false,
        vip         = false,
        image       = ""
)
public class RsPerkFarmerBot extends Script {

    // ── Config ─────────────────────────────────────────────────────────────
    /** Target minimap zoom the panel pushes once at startup. 2.0 is max-out. */
    public static final double MINIMAP_ZOOM = 2.0;

    /** Polling cadence while the loop is active. */
    private static final int TICK_MS = 600;

    /** Safety: after this many consecutive failed interact attempts in a state, fault out. */
    private static final int MAX_CONSECUTIVE_FAILS = 12;

    // ── Runtime state ──────────────────────────────────────────────────────
    private volatile BotState state = BotState.IDLE;
    private volatile long    stateEnteredAt = System.currentTimeMillis();
    private volatile long    nextActionAtMs = 0L;
    private volatile BotState stateBeforeSpring = BotState.IDLE;
    private volatile int     consecutiveFails = 0;

    // Session counters (panel reads these for its stats pane)
    private volatile int tasksCompleted = 0;
    private volatile int oresMined      = 0;
    private volatile int springsCaught  = 0;
    private volatile long sessionStartMs = 0L;
    private volatile String lastFaultReason = "";
    private volatile String lastTaskTarget  = "Runite ore";

    // Per-task progress — parsed from the dialog option "Mine NN runite ore"
    // and cross-checked against the completion chat line
    //   "You have completed your perk task and received 7,706 perk experience."
    private volatile int  taskTargetCount     = 0;   // 0 = unknown
    private volatile int  taskMinedThisTask   = 0;   // counts every new ore added to inventory since task accept
    private volatile int  lastSeenRuneOreInv  = 0;   // prev inventory rune count (for delta tracking across banks)
    private volatile long taskStartedAtMs     = 0L;
    private volatile long totalPerkXpEarned   = 0L;
    private volatile long lastPerkXpEarned    = 0L;
    private static final Pattern TASK_COUNT_PATTERN =
            Pattern.compile("(\\d+)\\s+runite", Pattern.CASE_INSENSITIVE);
    private static final Pattern TASK_COMPLETE_PATTERN =
            Pattern.compile("completed your perk task.*?([0-9,]+)\\s+perk experience",
                            Pattern.CASE_INSENSITIVE);

    /** How many extra ore to grant past the quota before we give up waiting
     *  on the chat-completion event and force-end the task ourselves. */
    private static final int QUOTA_GRACE_ORES = 3;

    /** Set the instant the ChatMessage event fires the completion line. The
     *  next onProcess tick reads + consumes this. Using an event-bus listener
     *  removes the ChatQuery-timing fragility we had before. */
    private volatile boolean pendingCompletionEvent = false;
    private volatile long    pendingCompletionXp    = 0L;

    /** Subscription handle from API.getEventBus().register(...). */
    private EventBus.Subscriber chatSubscription;

    private volatile JFrame window;
    private volatile RsPerkFarmerPanel panel;

    // ── Lifecycle ──────────────────────────────────────────────────────────
    @Override
    public boolean onStart() {
        sessionStartMs = System.currentTimeMillis();
        setState(BotState.STARTING);
        setStatus("Starting");
        ensureWindow();
        // Zoom the minimap all the way out (2.0) — once, on the client thread.
        zoomMinimap(MINIMAP_ZOOM);
        // Subscribe to chat events so task-completion is caught instantly
        // instead of polling ChatQuery. The subscriber handle is used in
        // onStop() to unregister cleanly.
        try {
            chatSubscription = API.getEventBus()
                    .register(ChatMessage.class, this::onChatMessageEvent, 0f);
            log("ChatMessage event subscription registered.");
        } catch (Throwable t) {
            log("Could not subscribe to ChatMessage events: " + t.getMessage()
                    + " (falling back to ChatQuery polling).");
        }
        return true;
    }

    @Override
    public int onProcess() {
        if (isPaused()) {
            setStatus("Paused");
            return TICK_MS;
        }

        if (window == null) ensureWindow();

        // Perk-task completion watcher — authoritative signal. The moment
        // chat says "You have completed your perk task and received N perk
        // experience." we abandon the current action, bank any ore we have,
        // and race back to the Perk Master for the next task. Quota + grace
        // is a fallback in case the chat event ever misfires.
        checkTaskCompletion();

        // Spring creature check runs on every tick except when we're already in
        // the middle of handling one, or when we're faulted out.
        if (state != BotState.SPRING_EVENT && state != BotState.FAULT) {
            KSNPC spring = findSpringCreature();
            if (spring != null) {
                log("Spring creature spotted — handling event.");
                stateBeforeSpring = state;
                setState(BotState.SPRING_EVENT);
            }
        }

        try {
            switch (state) {
                case STARTING:            tickStarting(); break;
                case CHECK_EQUIPMENT:     tickCheckEquipment(); break;
                case WALK_TO_NPC:         tickWalkToNpc(); break;
                case REQUEST_TASK:        tickRequestTask(); break;
                case CHOOSE_TASK_TYPE:    tickChooseTaskType(); break;
                case CHOOSE_DIFFICULTY:   tickChooseDifficulty(); break;
                case CHOOSE_RESOURCE:     tickChooseResource(); break;
                case TASK_ALREADY_ACTIVE: tickTaskAlreadyActive(); break;
                case WALK_TO_RESOURCE:    tickWalkToResource(); break;
                case MINING:              tickMining(); break;
                case WALK_TO_BANK:        tickWalkToBank(); break;
                case BANKING:             tickBanking(); break;
                case SPRING_EVENT:        tickSpringEvent(); break;
                case FAULT:               /* halt */ return TICK_MS * 5;
                default:                  setState(BotState.CHECK_EQUIPMENT); break;
            }
        } catch (Exception e) {
            fault("Exception in state " + state + ": " + e.getMessage());
            e.printStackTrace();
        }

        if (panel != null) panel.refreshStatus();
        setStatus(state.currentAction());
        return TICK_MS;
    }

    @Override
    public void onStop() {
        // Unhook chat event listener first so it can't fire against a dying bot.
        try {
            if (chatSubscription != null) {
                API.getEventBus().unregister(chatSubscription);
                chatSubscription = null;
            }
        } catch (Throwable ignored) {}

        SwingUtilities.invokeLater(() -> {
            if (window != null) {
                window.setVisible(false);
                window.dispose();
                window = null;
                panel  = null;
            }
        });
    }

    /**
     * Called by RuneLite's EventBus on every ChatMessage. We only care about
     * the perk-task completion line; stash it on the bot and let the next
     * onProcess() tick consume it (so state transitions happen on the
     * bot's own thread rather than here on the client thread).
     */
    private void onChatMessageEvent(ChatMessage evt) {
        try {
            String msg = evt.getMessage();
            if (msg == null) return;
            String clean = stripColorTags(msg);
            if (clean == null) return;
            if (clean.toLowerCase().contains("completed your perk task")) {
                long xp = 0;
                Matcher m = TASK_COMPLETE_PATTERN.matcher(clean);
                if (m.find()) {
                    try { xp = Long.parseLong(m.group(1).replace(",", "")); } catch (NumberFormatException ignored) {}
                }
                pendingCompletionXp    = xp;
                pendingCompletionEvent = true;
                log("[chat-event] Task complete detected: " + clean);
            }
        } catch (Throwable t) {
            log("[chat-event] handler error: " + t.getMessage());
        }
    }

    @Override public void onPause()  { log("Paused by user."); }
    @Override public void onResume() { log("Resumed by user."); }

    // ── State handlers ─────────────────────────────────────────────────────
    private void tickStarting() {
        // Small settle so the minimap zoom definitely registered.
        scheduleIn(600);
        setState(BotState.CHECK_EQUIPMENT);
    }

    private void tickCheckEquipment() {
        if (!hasPickaxe()) {
            fault("No pickaxe found in inventory or equipment. Equip or bank a pickaxe and restart.");
            return;
        }
        setState(BotState.WALK_TO_NPC);
    }

    private void tickWalkToNpc() {
        WorldPoint me = playerTile();
        if (me == null) { scheduleIn(400); return; }

        if (me.distanceTo(RouteProfile.FRONT_OF_NPC_TILE) > 2) {
            ctx.pathing.walkTo(RouteProfile.FRONT_OF_NPC_TILE);
            scheduleIn(900);
            return;
        }
        setState(BotState.REQUEST_TASK);
    }

    private void tickRequestTask() {
        // If dialog is already up, skip the NPC interact.
        if (ctx.dialog.isOpen()) {
            setState(BotState.CHOOSE_TASK_TYPE);
            return;
        }

        KSNPC master = findPerkMaster();
        if (master == null) {
            failStep("Perk Master not visible");
            scheduleIn(800);
            return;
        }

        boolean ok = safeInteract(() -> master.interact("Get-task"));
        if (!ok) {
            // Some servers report the action as "Get task" (no hyphen).
            ok = safeInteract(() -> master.interact("Get task"));
        }
        if (!ok) {
            failStep("Failed to right-click Get-task on Perk Master");
            scheduleIn(800);
            return;
        }
        consecutiveFails = 0;
        scheduleIn(1200);
        setState(BotState.CHOOSE_TASK_TYPE);
    }

    private void tickChooseTaskType() {
        // Check for the "already have a task" message first — it can surface
        // either as dialog text or as a recent chat line.
        if (hasAlreadyActiveMessage()) {
            log("Perk Master says a task is already active; skipping straight to mining.");
            if (ctx.dialog.isOpen()) ctx.dialog.close();
            setState(BotState.TASK_ALREADY_ACTIVE);
            return;
        }

        if (!ctx.dialog.isViewingOptions()) {
            // dialog may still be printing an intro line — continue it.
            if (ctx.dialog.canContinue()) ctx.dialog.continueSpace();
            scheduleIn(500);
            return;
        }
        boolean picked = ctx.dialog.chooseOption("Skilling") || ctx.dialog.chooseOption(1);
        if (!picked) { failStep("Could not choose Skilling"); return; }
        consecutiveFails = 0;
        scheduleIn(900);
        setState(BotState.CHOOSE_DIFFICULTY);
    }

    private void tickChooseDifficulty() {
        if (hasAlreadyActiveMessage()) {
            if (ctx.dialog.isOpen()) ctx.dialog.close();
            setState(BotState.TASK_ALREADY_ACTIVE);
            return;
        }
        if (!ctx.dialog.isViewingOptions()) {
            if (ctx.dialog.canContinue()) ctx.dialog.continueSpace();
            scheduleIn(500);
            return;
        }
        boolean picked = ctx.dialog.chooseOption("Elite") || ctx.dialog.chooseOption(4);
        if (!picked) { failStep("Could not choose Elite"); return; }
        consecutiveFails = 0;
        scheduleIn(900);
        setState(BotState.CHOOSE_RESOURCE);
    }

    private void tickChooseResource() {
        if (hasAlreadyActiveMessage()) {
            if (ctx.dialog.isOpen()) ctx.dialog.close();
            setState(BotState.TASK_ALREADY_ACTIVE);
            return;
        }
        if (!ctx.dialog.isViewingOptions()) {
            if (ctx.dialog.canContinue()) ctx.dialog.continueSpace();
            scheduleIn(500);
            return;
        }

        // Options look like "Mine 21 runite ore" / "Mine 48 amethyst crystals".
        // We ALWAYS want the runite one, whichever slot it is — amethyst is
        // explicitly forbidden. Parse the target count off the runite line so
        // we can stop mining the instant it completes.
        //
        // IMPORTANT: chooseOption(int) types that digit as a keypress and is
        // 1-based (1 = first option, 2 = second), NOT 0-based like the list
        // index returned by getOptions(). That's what was picking amethyst
        // when it happened to be slot 1.
        String runiteText = null;
        int runiteSlot = -1;   // 1-based dialog slot number
        String amethystText = null;
        int amethystSlot = -1;
        List<String> allOptionTexts = new java.util.ArrayList<>();
        try {
            List<KSWidget> opts = ctx.dialog.getOptions();
            for (int i = 0; i < opts.size(); i++) {
                KSWidget w = opts.get(i);
                String raw = w != null ? w.getText() : null;
                String txt = stripColorTags(raw);
                allOptionTexts.add((i + 1) + ". " + (txt == null ? "(null)" : txt));
                if (txt == null) continue;
                String lower = txt.toLowerCase();
                if (lower.contains("runite")) {
                    if (runiteSlot < 0) { runiteText = txt; runiteSlot = i + 1; }
                } else if (lower.contains("amethyst")) {
                    if (amethystSlot < 0) { amethystText = txt; amethystSlot = i + 1; }
                }
            }
        } catch (Throwable ignored) {}

        log("Perk task options seen: " + allOptionTexts);

        if (runiteSlot < 1) {
            // Menu is up but the runite option isn't listed yet — wait a tick
            // for the widgets to populate rather than pressing anything. Do
            // NOT fall back to a blind keypress, we refuse to accept amethyst.
            log("Runite option not yet present in dialog — waiting.");
            scheduleIn(400);
            return;
        }
        if (amethystSlot == runiteSlot) {
            // Extremely defensive: if our parser collapsed both texts to the
            // same slot somehow, bail and re-check next tick.
            failStep("Ambiguous task menu (runite/amethyst slot collision)");
            return;
        }

        // Type the runite slot number — 1-based. This is what the API really does.
        boolean picked = ctx.dialog.chooseOption(runiteSlot);
        if (!picked) {
            // As a backup, KSBot's predicate form iterates widget text itself.
            picked = ctx.dialog.chooseOption(text -> text != null
                    && text.toLowerCase().contains("runite"));
        }
        if (!picked) { failStep("Could not select Runite option (slot " + runiteSlot + ")"); return; }

        // Parse target count, e.g. "Mine 21 runite ore" -> 21
        int target = 0;
        if (runiteText != null) {
            Matcher m = TASK_COUNT_PATTERN.matcher(runiteText);
            if (m.find()) {
                try { target = Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
            }
        }
        taskTargetCount    = target;
        taskMinedThisTask  = 0;
        lastSeenRuneOreInv = runeOreCount();
        taskStartedAtMs    = System.currentTimeMillis();
        consecutiveFails = 0;
        lastTaskTarget = target > 0 ? "Mine " + target + " runite ore" : "Runite ore";
        log("Accepted perk task: " + lastTaskTarget);
        scheduleIn(1200);
        setState(BotState.WALK_TO_RESOURCE);
    }

    private void tickTaskAlreadyActive() {
        // Assume the active task is runite (only option we ever accept). Just go mine.
        log("Resuming existing task — walking to runite ore.");
        setState(BotState.WALK_TO_RESOURCE);
    }

    private void tickWalkToResource() {
        if (ctx.inventory.isFull()) {
            setState(BotState.WALK_TO_BANK);
            return;
        }
        WorldPoint me = playerTile();
        if (me == null) { scheduleIn(400); return; }

        if (me.distanceTo(RouteProfile.FRONT_OF_RESOURCE_TILE) > 2) {
            ctx.pathing.walkTo(RouteProfile.FRONT_OF_RESOURCE_TILE);
            scheduleIn(900);
            return;
        }
        setState(BotState.MINING);
    }

    private void tickMining() {
        // Quota reached via parsed count → go bank immediately. The chat
        // completion line is still the authoritative finisher (handled in
        // checkTaskCompletionChat), but this catches the moment in case the
        // chat line arrives a tick late.
        // Update per-task counter by watching inventory deltas (survives bank trips).
        int curInv = runeOreCount();
        if (curInv > lastSeenRuneOreInv) {
            taskMinedThisTask += (curInv - lastSeenRuneOreInv);
        }
        lastSeenRuneOreInv = curInv;

        // NOTE: we intentionally do NOT short-circuit to banking on
        // "quota reached" here. The perk task only ends when the server
        // fires the completion chat line — until then, we keep mining (the
        // counter is a UI progress indicator, not a trigger). Banking is
        // driven solely by inventory-full or by the completion chat.
        if (ctx.inventory.isFull()) {
            setState(BotState.WALK_TO_BANK);
            return;
        }

        KSObject ore = findRuniteOre();
        if (ore == null) {
            // Rock depleted / not in view. Nudge toward the canonical tile and wait.
            WorldPoint me = playerTile();
            if (me != null && me.distanceTo(RouteProfile.RUNITE_ORE_TILE) > 2) {
                ctx.pathing.walkTo(RouteProfile.FRONT_OF_RESOURCE_TILE);
            }
            scheduleIn(1200);
            return;
        }

        // Are we already mining? Check local player animation before re-clicking.
        if (isPlayerAnimating()) {
            // Track ore count delta via inventory once per second.
            int newCount = runeOreCount();
            if (newCount > oresMined) oresMined = newCount;
            scheduleIn(1500);
            return;
        }

        boolean ok = safeInteract(() -> ore.interact("Mine"));
        if (!ok) {
            failStep("Failed to Mine runite ore");
            scheduleIn(900);
            return;
        }
        consecutiveFails = 0;
        scheduleIn(1800);
    }

    private void tickWalkToBank() {
        WorldPoint me = playerTile();
        if (me == null) { scheduleIn(400); return; }

        if (me.distanceTo(RouteProfile.FRONT_OF_BANK_TILE) > 2) {
            ctx.pathing.walkTo(RouteProfile.FRONT_OF_BANK_TILE);
            scheduleIn(900);
            return;
        }
        setState(BotState.BANKING);
    }

    private void tickBanking() {
        if (!ctx.bank.isOpen()) {
            boolean opened = safeInteract(() -> ctx.bank.openBank());
            if (!opened) {
                failStep("Failed to open bank chest");
                scheduleIn(900);
                return;
            }
            ctx.bank.waitForOpen(4000);
            scheduleIn(600);
            return;
        }

        // Deposit all except any pickaxe we have.
        ctx.bank.depositAllExcept(RouteProfile.PICKAXE_NAMES);
        scheduleIn(500);
        ctx.bank.close();

        // Task-complete counter is driven by the chat watcher, not banking.
        consecutiveFails = 0;
        setState(BotState.WALK_TO_NPC);
    }

    private void tickSpringEvent() {
        KSNPC spring = findSpringCreature();
        if (spring == null) {
            // Creature gone — resume where we left off.
            log("Spring creature handled. Resuming previous task.");
            springsCaught++;
            setState(stateBeforeSpring == BotState.SPRING_EVENT
                    ? BotState.CHECK_EQUIPMENT
                    : stateBeforeSpring);
            return;
        }
        boolean ok = safeInteract(() -> spring.interact("Catch"))
                || safeInteract(() -> spring.interact("Catch-springs"))
                || safeInteract(() -> spring.interact("Capture"))
                || safeInteract(() -> spring.interact("Click"));
        if (!ok) {
            // If we can't interact for ~10 ticks we give up and resume — the
            // creature despawns on its own and we don't want to deadlock.
            failStep("Could not click Spring creature");
        }
        scheduleIn(900);
    }

    // ── Helpers: lookups ──────────────────────────────────────────────────
    private KSNPC findPerkMaster() {
        try {
            KSNPC n = ctx.npcs.query()
                    .withId(RouteProfile.PERK_MASTER_ID)
                    .closest();
            if (n != null) return n;
        } catch (Throwable ignored) {}
        try {
            return ctx.npcs.query()
                    .withName(RouteProfile.PERK_MASTER_NAME)
                    .closest();
        } catch (Throwable ignored) {}
        return null;
    }

    private KSObject findRuniteOre() {
        try {
            KSObject o = ctx.groundObjects.query()
                    .withId(RouteProfile.RUNITE_ORE_ID)
                    .withinDistance(8)
                    .nearestToPlayer();
            if (o != null) return o;
        } catch (Throwable ignored) {}
        try {
            return ctx.groundObjects.query()
                    .withName("Runite ore", "Runite rocks")
                    .withinDistance(8)
                    .nearestToPlayer();
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Finds <i>our</i> Spring impling, not someone else's. The server spawns
     * a personal random-event NPC with a hint arrow pointing at it for the
     * owner — we use that as the ownership signal. If no hint-arrow impling
     * is present we return null rather than clicking on another player's
     * creature.
     */
    private KSNPC findSpringCreature() {
        // Primary: impling with a hint arrow pointing at it (= ours).
        try {
            KSNPC mine = ctx.npcs.query()
                    .withName("Spring impling", "Spring Impling")
                    .hasHintArrow()
                    .withinDistance(15)
                    .closest();
            if (mine != null) return mine;
        } catch (Throwable ignored) {}

        // Secondary: any hint-arrowed NPC in range whose name mentions spring —
        // catches minor name variants ("Spring creature", etc.) but still only
        // ours because of the hint arrow filter.
        try {
            KSNPC mine = ctx.npcs.query()
                    .hasHintArrow()
                    .withinDistance(15)
                    .closest();
            if (mine != null) {
                String name = mine.getName();
                if (name != null && name.toLowerCase().contains("spring")) return mine;
            }
        } catch (Throwable ignored) {}

        // No hint-arrowed spring impling = not ours. Don't touch.
        return null;
    }

    // ── Helpers: checks ───────────────────────────────────────────────────
    private boolean hasPickaxe() {
        try {
            if (ctx.inventory.contains(RouteProfile.PICKAXE_NAMES)) return true;
        } catch (Throwable ignored) {}
        try {
            if (ctx.equipment.contains(RouteProfile.PICKAXE_NAMES)) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private boolean hasAlreadyActiveMessage() {
        try {
            String t = ctx.dialog.isOpen() ? ctx.dialog.getText() : null;
            if (t != null && t.toLowerCase().contains("already have a perk")) return true;
        } catch (Throwable ignored) {}
        try {
            // Chat fallback — last ~10 seconds, containing the signature phrase.
            Object recent = ctx.chat.query()
                    .contains("already have a Perk")
                    .inLastSeconds(10)
                    .first();
            if (recent != null) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * Consumes the task-completion signal on the bot's own thread.
     *
     * <p>Two paths trigger completion:
     * <ol>
     *   <li>Primary — {@link #onChatMessageEvent} set {@code pendingCompletionEvent}
     *       after the RuneLite ChatMessage bus fired the "completed your perk
     *       task" line. This is instant and authoritative.</li>
     *   <li>Fallback — we've mined more runite ore than the task quota plus
     *       {@link #QUOTA_GRACE_ORES}. In the rare event the chat listener
     *       misses the line (e.g. the server hides it behind a filter), the
     *       grace buffer guarantees we don't sit there forever.</li>
     * </ol>
     */
    private void checkTaskCompletion() {
        BotState s = state;
        // Only relevant while a task is active.
        if (s == BotState.FAULT || s == BotState.STARTING || s == BotState.CHECK_EQUIPMENT
                || s == BotState.WALK_TO_NPC || s == BotState.REQUEST_TASK
                || s == BotState.CHOOSE_TASK_TYPE || s == BotState.CHOOSE_DIFFICULTY
                || s == BotState.CHOOSE_RESOURCE) {
            return;
        }

        boolean chatFired = pendingCompletionEvent;
        boolean quotaBlown = taskTargetCount > 0
                && taskMinedThisTask >= (taskTargetCount + QUOTA_GRACE_ORES);

        if (!chatFired && !quotaBlown) return;

        long xp;
        if (chatFired) {
            xp = pendingCompletionXp;
            pendingCompletionEvent = false;
            pendingCompletionXp    = 0L;
            log("Perk task COMPLETE (chat event) — +" + xp + " perk xp.");
        } else {
            xp = 0;
            log("Perk task COMPLETE (quota fallback: " + taskMinedThisTask
                    + " >= " + taskTargetCount + "+" + QUOTA_GRACE_ORES
                    + " grace) — no chat event caught, assuming done.");
        }

        lastPerkXpEarned   = xp;
        totalPerkXpEarned += xp;
        tasksCompleted++;

        // Reset per-task tracking so the next task starts clean.
        taskTargetCount    = 0;
        taskMinedThisTask  = 0;
        lastSeenRuneOreInv = runeOreCount();
        taskStartedAtMs    = 0L;

        // Ore in bag → bank on the way. Otherwise straight to the Perk Master.
        if (hasAnyRuneOre()) {
            setState(BotState.WALK_TO_BANK);
        } else {
            setState(BotState.WALK_TO_NPC);
        }
        scheduleIn(200);
    }

    /** Strips RuneScape color/format tags like &lt;col=ff9040&gt; so text is matchable. */
    private static String stripColorTags(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("<[^>]+>", "").trim();
    }

    /** True iff the inventory currently has any runite ore. */
    private boolean hasAnyRuneOre() {
        try {
            return ctx.inventory.contains("Runite ore");
        } catch (Throwable ignored) { return false; }
    }

    private WorldPoint playerTile() {
        try {
            return ctx.players.getLocal().getWorldLocation();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean isPlayerAnimating() {
        try {
            return ctx.players.getLocal().getAnimation() != -1;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private int runeOreCount() {
        try {
            return ctx.inventory.getTotalQuantity("Runite ore");
        } catch (Throwable ignored) {
            return oresMined;
        }
    }

    // ── Helpers: flow ─────────────────────────────────────────────────────
    /** Set state + record timestamp so the panel can compute elapsed/next time. */
    private void setState(BotState next) {
        if (this.state != next) {
            log("[state] " + this.state + " -> " + next);
            this.state = next;
            this.stateEnteredAt = System.currentTimeMillis();
        }
    }

    private void scheduleIn(long ms) {
        this.nextActionAtMs = System.currentTimeMillis() + ms;
    }

    private void failStep(String why) {
        consecutiveFails++;
        log("step fail (" + consecutiveFails + "/" + MAX_CONSECUTIVE_FAILS + "): " + why);
        if (consecutiveFails >= MAX_CONSECUTIVE_FAILS) {
            fault("Repeated failure: " + why);
        }
    }

    private void fault(String reason) {
        lastFaultReason = reason;
        log("[FAULT] " + reason);
        setState(BotState.FAULT);
        setStatus("FAULT: " + reason);
        // Auto-dump so the user has something to review on their next glance.
        try {
            DebugDumper.dump(this, "auto-fault");
        } catch (Throwable ignored) {}
    }

    private boolean safeInteract(InteractFn fn) {
        try {
            Object r = fn.run();
            if (r instanceof Boolean) return (Boolean) r;
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @FunctionalInterface
    private interface InteractFn { Object run() throws Exception; }

    // ── Minimap zoom ──────────────────────────────────────────────────────
    private void zoomMinimap(double zoom) {
        try {
            API.runOnClientThread(() -> {
                try {
                    API.getClient().setMinimapZoom(zoom);
                } catch (Throwable inner) {
                    log("setMinimapZoom unavailable: " + inner.getMessage());
                }
            });
            log("Minimap zoom pushed to " + zoom);
        } catch (Throwable t) {
            log("Minimap zoom failed: " + t.getMessage());
        }
    }

    // ── UI hookup ─────────────────────────────────────────────────────────
    private void ensureWindow() {
        if (window != null) return;
        Runnable build = () -> {
            JFrame f = new JFrame("RS Perk Farmer v1.03");
            f.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            RsPerkFarmerPanel p = new RsPerkFarmerPanel(this);
            f.getContentPane().add(p);
            f.setPreferredSize(new Dimension(720, 520));
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            this.panel  = p;
            this.window = f;
        };
        if (SwingUtilities.isEventDispatchThread()) build.run();
        else {
            try { SwingUtilities.invokeAndWait(build); }
            catch (Exception e) { throw new IllegalStateException("Cannot build window", e); }
        }
    }

    public void log(String line) {
        System.out.println("[RS-PerkFarmer-v103] " + line);
        if (panel != null) panel.appendLog(line);
    }

    // ── Accessors the panel reads ────────────────────────────────────────
    public BotState getBotState()       { return state; }
    public long getStateEnteredAt()     { return stateEnteredAt; }
    public long getNextActionAtMs()     { return nextActionAtMs; }
    public long getSessionStartMs()     { return sessionStartMs; }
    public int  getTasksCompleted()     { return tasksCompleted; }
    public int  getOresMined()          { return oresMined; }
    public int  getSpringsCaught()      { return springsCaught; }
    public String getLastFaultReason()  { return lastFaultReason; }
    public String getLastTaskTarget()   { return lastTaskTarget; }
    public int  getTaskTargetCount()    { return taskTargetCount; }
    public int  getTaskMinedThisTask()  { return taskMinedThisTask; }
    public long getTotalPerkXpEarned()  { return totalPerkXpEarned; }
    public long getLastPerkXpEarned()   { return lastPerkXpEarned; }

    // Panel → bot controls
    public void onUpdateScriptRequested() {
        log("Update requested — resetting counters and re-checking equipment.");
        consecutiveFails = 0;
        sessionStartMs = System.currentTimeMillis();
        tasksCompleted = 0;
        oresMined = 0;
        springsCaught = 0;
        lastFaultReason = "";
        taskTargetCount    = 0;
        taskMinedThisTask  = 0;
        totalPerkXpEarned  = 0L;
        lastPerkXpEarned   = 0L;
        setState(BotState.CHECK_EQUIPMENT);
        zoomMinimap(MINIMAP_ZOOM);
    }
}
