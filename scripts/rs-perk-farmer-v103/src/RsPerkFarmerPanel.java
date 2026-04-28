package rsperkfarmerv103;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;

/**
 * Control surface for the RS Perk Farmer script. Two tabs:
 *  1. <b>Main</b> — Start / Pause / Stop / Update buttons + session summary.
 *  2. <b>Debug</b> — dump button, log window, fault details, inspect-state button.
 *
 * A status bar spans the bottom of both tabs: current action | next action |
 * countdown to next action | uptime.
 */
public class RsPerkFarmerPanel extends JPanel {

    private static final Color BG           = new Color(28, 30, 34);
    private static final Color PANEL_BG     = new Color(40, 42, 48);
    private static final Color TEXT         = new Color(230, 230, 230);
    private static final Color GREEN        = new Color(60, 140, 70);
    private static final Color AMBER        = new Color(200, 140, 40);
    private static final Color RED          = new Color(180, 60, 60);
    private static final Color BLUE         = new Color(55, 100, 170);
    private static final Color GREY         = new Color(90, 90, 96);

    private final RsPerkFarmerBot bot;

    // Session stats
    private final JLabel tasksLabel    = value("0");
    private final JLabel oresLabel     = value("0");
    private final JLabel springsLabel  = value("0");
    private final JLabel uptimeLabel   = value("0s");
    private final JLabel perkXpLabel   = value("0");
    private final JLabel taskProgressLabel = value("— / —");

    // Status bar
    private final JLabel statusCurrent = value("Idle");
    private final JLabel statusNext    = value("—");
    private final JLabel statusCountdown = value("—");
    private final JLabel statusUptime  = value("0s");
    private final JLabel statusTask    = value("— / —");

    // Buttons
    private final JButton startBtn  = button("Start",  GREEN);
    private final JButton pauseBtn  = button("Pause",  AMBER);
    private final JButton stopBtn   = button("Stop",   RED);
    private final JButton updateBtn = button("Update Script", BLUE);

    // Debug pane
    private final JTextArea logArea = new JTextArea();
    private final JLabel faultLabel = value("(none)");
    private final JLabel stateLabel = value("IDLE");

    private final Timer refreshTimer;

    public RsPerkFarmerPanel(RsPerkFarmerBot bot) {
        this.bot = bot;
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(8, 8, 8, 8));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG);
        tabs.setForeground(TEXT);
        tabs.addTab("Main",  buildMainTab());
        tabs.addTab("Debug", buildDebugTab());
        add(tabs, BorderLayout.CENTER);

        add(buildStatusBar(), BorderLayout.SOUTH);

        wireButtons();

        // Lightweight refresh every 500ms so the status + countdown feel alive.
        refreshTimer = new Timer(500, e -> refreshStatus());
        refreshTimer.start();
    }

    // ── Tab: Main ─────────────────────────────────────────────────────────
    private JPanel buildMainTab() {
        JPanel p = panel(new BorderLayout(0, 8));

        JPanel buttonRow = panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonRow.add(startBtn);
        buttonRow.add(pauseBtn);
        buttonRow.add(stopBtn);
        buttonRow.add(updateBtn);
        p.add(buttonRow, BorderLayout.NORTH);

        JPanel stats = panel(new GridLayout(0, 2, 8, 4));
        stats.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREY, 1),
                new EmptyBorder(8, 8, 8, 8)));
        stats.add(label("Current task progress:")); stats.add(taskProgressLabel);
        stats.add(label("Perk tasks completed:")); stats.add(tasksLabel);
        stats.add(label("Total perk XP earned:")); stats.add(perkXpLabel);
        stats.add(label("Runite ore mined (all-time):")); stats.add(oresLabel);
        stats.add(label("Spring creatures caught:")); stats.add(springsLabel);
        stats.add(label("Session uptime:"));       stats.add(uptimeLabel);
        p.add(stats, BorderLayout.CENTER);

        JLabel hint = new JLabel(
                "<html><body style='color:#bbb;font-size:11px;'>" +
                "Route: Reason &rarr; Donator Zone &rarr; Perk Master &rarr; Elite Skilling (Runite).<br>" +
                "Minimap is zoomed to 2.0 on start. A pickaxe must be in inventory or equipped.</body></html>");
        hint.setForeground(TEXT);
        p.add(hint, BorderLayout.SOUTH);

        return p;
    }

    // ── Tab: Debug ────────────────────────────────────────────────────────
    private JPanel buildDebugTab() {
        JPanel p = panel(new BorderLayout(0, 6));

        JPanel top = panel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton dumpBtn     = button("Dump State to File", BLUE);
        JButton copyLogBtn  = button("Copy Log",           GREY);
        JButton openDirBtn  = button("Open Dump Folder",   GREY);
        JButton clearLogBtn = button("Clear Log",          GREY);

        dumpBtn.addActionListener(e -> {
            String path = DebugDumper.dump(bot, "manual");
            appendLog(path != null ? "Dump saved: " + path : "Dump failed.");
        });
        copyLogBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(logArea.getText()), null);
            appendLog("Log copied to clipboard (" + logArea.getText().length() + " chars).");
        });
        openDirBtn.addActionListener(e -> {
            try {
                java.io.File dir = new java.io.File(bot.getStorageDirectory(), "debug-dumps");
                if (!dir.exists()) dir.mkdirs();
                Desktop.getDesktop().open(dir);
            } catch (Exception ex) {
                appendLog("Could not open dump folder: " + ex.getMessage());
            }
        });
        clearLogBtn.addActionListener(e -> logArea.setText(""));

        top.add(dumpBtn);
        top.add(copyLogBtn);
        top.add(openDirBtn);
        top.add(clearLogBtn);
        p.add(top, BorderLayout.NORTH);

        JPanel diag = panel(new GridLayout(0, 2, 8, 4));
        diag.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREY, 1),
                new EmptyBorder(6, 8, 6, 8)));
        diag.add(label("Current state:"));     diag.add(stateLabel);
        diag.add(label("Last fault reason:")); diag.add(faultLabel);

        logArea.setEditable(false);
        logArea.setLineWrap(false);
        logArea.setBackground(new Color(20, 22, 26));
        logArea.setForeground(TEXT);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setPreferredSize(new Dimension(600, 240));

        JPanel center = panel(new BorderLayout(0, 6));
        center.add(diag,   BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);

        JLabel hint = new JLabel(
                "<html><body style='color:#bbb;font-size:11px;'>" +
                "Dumps land in <b>" + bot.getStorageDirectory().getAbsolutePath() + "\\debug-dumps</b>.<br>" +
                "Auto-dump also fires whenever the script faults out.</body></html>");
        hint.setForeground(TEXT);
        p.add(hint, BorderLayout.SOUTH);

        return p;
    }

    // ── Status bar (shared) ───────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = panel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GREY));
        bar.add(sectionLabel("Now:"));      bar.add(statusCurrent);
        bar.add(sep());
        bar.add(sectionLabel("Next:"));     bar.add(statusNext);
        bar.add(sep());
        bar.add(sectionLabel("In:"));       bar.add(statusCountdown);
        bar.add(sep());
        bar.add(sectionLabel("Task:"));     bar.add(statusTask);
        bar.add(sep());
        bar.add(sectionLabel("Uptime:"));   bar.add(statusUptime);
        return bar;
    }

    // ── Wiring ────────────────────────────────────────────────────────────
    private void wireButtons() {
        startBtn.addActionListener(e -> {
            if (bot.isPaused()) { bot.resume(); appendLog("Script resumed."); }
            else if (!bot.isRunning()) { appendLog("Start is handled by KSBot — press the Run button on the KSBot toolbar."); }
            else { appendLog("Script is already running."); }
        });
        pauseBtn.addActionListener(e -> {
            if (bot.isPaused()) { bot.resume(); appendLog("Script resumed."); }
            else { bot.pause(); appendLog("Script paused."); }
        });
        stopBtn.addActionListener(e -> {
            appendLog("Stop requested.");
            bot.stop();
        });
        updateBtn.addActionListener(e -> bot.onUpdateScriptRequested());
    }

    // ── Live refresh ──────────────────────────────────────────────────────
    public void refreshStatus() {
        SwingUtilities.invokeLater(() -> {
            BotState s = bot.getBotState();
            statusCurrent.setText(s.currentAction());
            statusNext.setText(s.nextAction());
            stateLabel.setText(s.name());

            long now = System.currentTimeMillis();
            long delta = bot.getNextActionAtMs() - now;
            if (delta <= 0 || bot.isPaused() || s == BotState.FAULT) {
                statusCountdown.setText(bot.isPaused() ? "paused" : (s == BotState.FAULT ? "halted" : "now"));
            } else {
                statusCountdown.setText(formatMs(delta));
            }

            long start = bot.getSessionStartMs();
            String up = start == 0 ? "0s" : formatMs(now - start);
            uptimeLabel.setText(up);
            statusUptime.setText(up);

            tasksLabel.setText(String.valueOf(bot.getTasksCompleted()));
            oresLabel.setText(String.valueOf(bot.getOresMined()));
            springsLabel.setText(String.valueOf(bot.getSpringsCaught()));
            perkXpLabel.setText(String.format("%,d", bot.getTotalPerkXpEarned())
                    + (bot.getLastPerkXpEarned() > 0
                            ? "  (last +" + String.format("%,d", bot.getLastPerkXpEarned()) + ")"
                            : ""));

            int target = bot.getTaskTargetCount();
            int mined  = bot.getTaskMinedThisTask();
            String prog = (target > 0 ? mined + " / " + target + " runite ore"
                                      : (mined > 0 ? mined + " / ? runite ore" : "— / —"));
            taskProgressLabel.setText(prog);
            statusTask.setText(prog);

            String fault = bot.getLastFaultReason();
            faultLabel.setText(fault == null || fault.isBlank() ? "(none)" : fault);
        });
    }

    public void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line);
            logArea.append("\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private static String formatMs(long ms) {
        if (ms < 0) ms = 0;
        long s = ms / 1000;
        if (s < 60) return s + "s";
        long m = s / 60; s %= 60;
        if (m < 60) return m + "m " + s + "s";
        long h = m / 60; m %= 60;
        return h + "h " + m + "m " + s + "s";
    }

    // ── Tiny UI helpers ───────────────────────────────────────────────────
    private JPanel panel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(PANEL_BG);
        return p;
    }

    private JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(TEXT);
        return l;
    }

    private JLabel value(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(Color.WHITE);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private JLabel sectionLabel(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(new Color(170, 170, 180));
        return l;
    }

    private JSeparator sep() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(2, 16));
        return sep;
    }

    private JButton button(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(6, 12, 6, 12));
        return b;
    }
}
