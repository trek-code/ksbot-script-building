package rsperkfarmerclrefinedv2;

import rs.kreme.ksbot.api.wrappers.KSItem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a timestamped plain-text dump of the bot's current state to the
 * script's storage directory. The file is what the user hands over when
 * something goes wrong — it captures enough to diagnose without replaying.
 */
public final class DebugDumper {

    private DebugDumper() {}

    /** Public entry: returns the absolute path of the file written, or null on failure. */
    public static String dump(RsPerkFarmerBot bot, String reason) {
        try {
            File dir = new File(bot.getStorageDirectory(), "debug-dumps");
            if (!dir.exists() && !dir.mkdirs()) {
                bot.log("Could not create debug dump directory: " + dir);
                return null;
            }
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
            File out = new File(dir, "rs-perk-farmer_" + stamp + "_" + safe(reason) + ".txt");
            String body = buildBody(bot, reason);
            Files.writeString(out.toPath(), body);
            bot.log("Debug dump written: " + out.getAbsolutePath());
            return out.getAbsolutePath();
        } catch (IOException io) {
            bot.log("Debug dump FAILED: " + io.getMessage());
            return null;
        }
    }

    private static String safe(String s) {
        if (s == null || s.isBlank()) return "manual";
        return s.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    private static String buildBody(RsPerkFarmerBot bot, String reason) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("=== RS Perk Farmer — Debug Dump ===\n");
        sb.append("Reason: ").append(reason == null ? "manual" : reason).append('\n');
        sb.append("Timestamp: ").append(LocalDateTime.now()).append('\n');
        sb.append('\n');

        sb.append("-- Script --\n");
        sb.append("state: ").append(bot.getBotState()).append('\n');
        sb.append("state entered at: ").append(bot.getStateEnteredAt()).append('\n');
        sb.append("next action at:  ").append(bot.getNextActionAtMs()).append('\n');
        sb.append("runtime (ms):    ").append(bot.getRuntimeMillis()).append('\n');
        sb.append("is paused:       ").append(bot.isPaused()).append('\n');
        sb.append("is running:      ").append(bot.isRunning()).append('\n');
        sb.append("last fault:      ").append(bot.getLastFaultReason()).append('\n');
        sb.append("last task:       ").append(bot.getLastTaskTarget()).append('\n');
        sb.append('\n');

        sb.append("-- Session counters --\n");
        sb.append("tasks completed: ").append(bot.getTasksCompleted()).append('\n');
        sb.append("ores mined:      ").append(bot.getOresMined()).append('\n');
        sb.append("springs caught:  ").append(bot.getSpringsCaught()).append('\n');
        sb.append('\n');

        sb.append("-- Player --\n");
        try {
            sb.append("tile:            ").append(bot.ctx.players.getLocal().getWorldLocation()).append('\n');
            sb.append("animation:       ").append(bot.ctx.players.getLocal().getAnimation()).append('\n');
            sb.append("interacting:     ").append(bot.ctx.players.getLocal().getInteracting() != null).append('\n');
        } catch (Throwable t) { sb.append("(player snapshot failed: ").append(t.getMessage()).append(")\n"); }
        sb.append('\n');

        sb.append("-- Inventory --\n");
        try {
            List<String> items = new ArrayList<>();
            for (KSItem item : bot.ctx.inventory.getItems(i -> true)) {
                if (item == null) continue;
                items.add(item.getName() + " x" + item.getQuantity() + " (id=" + item.getId() + ")");
            }
            if (items.isEmpty()) sb.append("(empty)\n");
            else for (String line : items) sb.append("  ").append(line).append('\n');
        } catch (Throwable t) { sb.append("(inventory snapshot failed: ").append(t.getMessage()).append(")\n"); }
        sb.append('\n');

        sb.append("-- Dialog --\n");
        try {
            sb.append("isOpen: ").append(bot.ctx.dialog.isOpen()).append('\n');
            if (bot.ctx.dialog.isOpen()) {
                sb.append("text:   ").append(bot.ctx.dialog.getText()).append('\n');
                sb.append("viewingOptions: ").append(bot.ctx.dialog.isViewingOptions()).append('\n');
                if (bot.ctx.dialog.isViewingOptions()) {
                    sb.append("options:\n");
                    int i = 1;
                    for (Object w : bot.ctx.dialog.getOptions()) {
                        sb.append("  ").append(i++).append(". ").append(String.valueOf(w)).append('\n');
                    }
                }
            }
        } catch (Throwable t) { sb.append("(dialog snapshot failed: ").append(t.getMessage()).append(")\n"); }
        sb.append('\n');

        sb.append("-- Bank --\n");
        try {
            sb.append("isOpen: ").append(bot.ctx.bank.isOpen()).append('\n');
            sb.append("freeSlots: ").append(bot.ctx.bank.getFreeSlots()).append('\n');
        } catch (Throwable t) { sb.append("(bank snapshot failed: ").append(t.getMessage()).append(")\n"); }
        sb.append('\n');

        return sb.toString();
    }
}
