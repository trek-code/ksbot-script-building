package rsperkfarmerclworldbosshandlertest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
    private final JLabel perkXpLabel     = value("0");
    private final JLabel perkLevelLabel  = value("—");
    private final JLabel perkPointsLabel = value("—");
    private final JLabel taskProgressLabel = value("— / —");
    private final JLabel nearbyPlayersLabel = value("0");
    private final JLabel lastMineLabel      = value("—");
    private final JLabel lastActionLabel    = value("—");

    // Boss stats
    private final JLabel bossStatsLabel = value("—");

    // Anti-ban controls
    private final JCheckBox antibanCheck = new JCheckBox("Random delay between actions");
    private final JSpinner antibanMin = new JSpinner(new SpinnerNumberModel(150, 0, 10000, 25));
    private final JSpinner antibanMax = new JSpinner(new SpinnerNumberModel(450, 0, 10000, 25));

    // Gear preset dropdowns
    private final String[] PRESET_OPTIONS = {"Preset 1", "Preset 2", "Preset 3", "Preset 4", "Preset 5"};
    private final JComboBox<String> miningPresetDropdown = new JComboBox<>(PRESET_OPTIONS);
    private final JComboBox<String> bossPresetDropdown   = new JComboBox<>(PRESET_OPTIONS);

    // Boss tab
    private final JLabel bossStatusLabel = value("—");

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

    // Anti-Ban tab
    private final DefaultTableModel staffTableModel = new DefaultTableModel(
            new String[]{"Name", "Distance", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable              staffTable          = new JTable(staffTableModel);
    private final JTextArea           staffChatArea       = new JTextArea();
    private final JComboBox<String>   chatFilterDropdown  =
            new JComboBox<>(new String[]{"All Channels", "Help Only", "Yell Only", "Staff Only", "Nearby (≤16t)"});
    private final JLabel              staffKnownLabel     = value("0");
    private final JLabel              staffNearbyStatLabel = value("0");

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
        tabs.addTab("Main",     buildMainTab());
        tabs.addTab("Boss",     buildBossTab());
        tabs.addTab("Loot",     buildLootTab());
        tabs.addTab("Anti-Ban", buildAntiBanTab());
        tabs.addTab("Debug",    buildDebugTab());
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
        stats.add(label("Perk tree level:"));      stats.add(perkLevelLabel);
        stats.add(label("Perk points:"));          stats.add(perkPointsLabel);
        stats.add(label("Runite ore mined (session):")); stats.add(oresLabel);
        stats.add(label("Spring creatures caught:")); stats.add(springsLabel);
        stats.add(label("Players nearby:"));       stats.add(nearbyPlayersLabel);
        stats.add(label("Time since last mine:")); stats.add(lastMineLabel);
        stats.add(label("Time since last action:")); stats.add(lastActionLabel);
        stats.add(label("Session uptime:"));       stats.add(uptimeLabel);

        JPanel center = panel(new BorderLayout(0, 8));
        center.add(stats, BorderLayout.CENTER);

        JPanel bottom = panel(new GridLayout(0, 1, 0, 6));
        bottom.add(buildGearSelectionPanel());
        bottom.add(buildAntibanPanel());
        center.add(bottom, BorderLayout.SOUTH);
        p.add(center, BorderLayout.CENTER);

        JLabel hint = new JLabel(
                "<html><body style='color:#bbb;font-size:11px;'>" +
                "Route: Reason &rarr; Donator Zone &rarr; Perk Master &rarr; Elite Skilling (Runite).<br>" +
                "Minimap is zoomed to 2.0 on start. A pickaxe must be in inventory or equipped.</body></html>");
        hint.setForeground(TEXT);
        p.add(hint, BorderLayout.SOUTH);

        return p;
    }

    // Loot tab
    private final JTextArea lootArea = new JTextArea();

    // ── Tab: Boss ──────────────────────────────────────────────────────────
    private JPanel buildBossTab() {
        JPanel p = panel(new BorderLayout(0, 8));

        ReasonWorldBossHandler handler = bot.getWorldBossHandler();

        // Master toggle
        JCheckBox masterToggle = new JCheckBox("Enable world boss handler");
        masterToggle.setSelected(handler.isGlobalEnabled());
        masterToggle.setBackground(PANEL_BG);
        masterToggle.setForeground(TEXT);
        masterToggle.setFocusPainted(false);

        // Per-boss checkboxes
        JPanel bossChecks = panel(new GridLayout(0, 1, 4, 4));
        bossChecks.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GREY, 1),
                        "Bosses to handle", 0, 0, null, TEXT),
                new EmptyBorder(6, 8, 6, 8)));

        java.util.List<JCheckBox> perBoss = new java.util.ArrayList<>();
        for (BossConfig cfg : handler.getRegisteredBosses()) {
            JCheckBox cb = new JCheckBox(cfg.displayName
                    + "  (::" + cfg.teleportCommand + ")"
                    + (cfg.npcId > 0 ? "  [ID " + cfg.npcId + "]" : "  [ID unknown]"));
            cb.setSelected(handler.isBossEnabled(cfg.displayName));
            cb.setBackground(PANEL_BG);
            cb.setForeground(TEXT);
            cb.setFocusPainted(false);
            cb.setEnabled(handler.isGlobalEnabled());
            cb.addActionListener(e -> {
                handler.setBossEnabled(cfg.displayName, cb.isSelected());
                appendLog("World boss: " + cfg.displayName + " "
                        + (cb.isSelected() ? "enabled" : "disabled") + ".");
            });
            perBoss.add(cb);
            bossChecks.add(cb);
        }

        masterToggle.addActionListener(e -> {
            boolean on = masterToggle.isSelected();
            handler.setGlobalEnabled(on);
            for (JCheckBox cb : perBoss) cb.setEnabled(on);
            appendLog("World boss handler " + (on ? "enabled" : "disabled") + ".");
        });

        // Boss gear preset dropdown
        JPanel bossGearBox = panel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bossGearBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GREY, 1),
                        "Gear Selection (world boss)", 0, 0, null, TEXT),
                new EmptyBorder(4, 6, 4, 6)));
        int bossIdx = Math.max(1, Math.min(5, handler.getBossPresetSlot())) - 1;
        bossPresetDropdown.setSelectedIndex(bossIdx);
        bossPresetDropdown.addActionListener(e -> {
            int slot = bossPresetDropdown.getSelectedIndex() + 1;
            handler.setBossPresetSlot(slot);
            appendLog("World boss preset set to slot " + slot + ".");
        });
        bossGearBox.add(label("Combat preset:"));
        bossGearBox.add(bossPresetDropdown);

        // Status display
        JPanel statusPanel = panel(new GridLayout(0, 2, 8, 4));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREY, 1),
                new EmptyBorder(6, 8, 6, 8)));
        statusPanel.add(label("Boss status:"));
        statusPanel.add(bossStatusLabel);
        statusPanel.add(label("Players nearby (live):"));
        statusPanel.add(nearbyPlayersLabel);

        // Per-boss stats panel
        bossStatsLabel.setVerticalAlignment(SwingConstants.TOP);
        bossStatsLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane statsScroll = new JScrollPane(bossStatsLabel);
        statsScroll.setPreferredSize(new Dimension(600, 160));
        statsScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GREY, 1),
                        "Per-boss stats (attempts / kills / bails / deaths / AOE dodges)",
                        0, 0, null, TEXT),
                new EmptyBorder(2, 4, 2, 4)));

        JPanel top = panel(new BorderLayout(0, 6));
        top.add(masterToggle, BorderLayout.NORTH);
        top.add(bossChecks,   BorderLayout.CENTER);
        top.add(bossGearBox,  BorderLayout.SOUTH);
        p.add(top, BorderLayout.NORTH);
        JPanel mid = panel(new BorderLayout(0, 6));
        mid.add(statusPanel, BorderLayout.NORTH);
        mid.add(statsScroll, BorderLayout.CENTER);
        p.add(mid, BorderLayout.CENTER);

        JLabel hint = new JLabel(
                "<html><body style='color:#bbb;font-size:11px;'>" +
                "When enabled, chat notifications for world bosses will interrupt mining.<br>" +
                "The bot loads your 'world boss' bank preset, teleports, fights, loots,<br>" +
                "then returns to Donator Zone, re-loads 'mining' preset, and resumes.</body></html>");
        hint.setForeground(TEXT);
        p.add(hint, BorderLayout.SOUTH);

        return p;
    }

    // ── Tab: Loot ──────────────────────────────────────────────────────────
    private JPanel buildLootTab() {
        JPanel p = panel(new BorderLayout(0, 8));

        JPanel top = panel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton refreshLootBtn = button("Refresh",        BLUE);
        JButton clearLootBtn   = button("Clear Loot Log", GREY);
        JButton copyLootBtn    = button("Copy Loot",      GREY);

        refreshLootBtn.addActionListener(e -> refreshLootArea());
        clearLootBtn.addActionListener(e -> {
            bot.getWorldBossHandler().clearSessionLoot();
            lootArea.setText("(No loot yet)");
            appendLog("Boss loot log cleared.");
        });
        copyLootBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(lootArea.getText()), null);
            appendLog("Loot log copied to clipboard.");
        });

        top.add(refreshLootBtn);
        top.add(clearLootBtn);
        top.add(copyLootBtn);
        p.add(top, BorderLayout.NORTH);

        lootArea.setEditable(false);
        lootArea.setLineWrap(false);
        lootArea.setBackground(new Color(20, 22, 26));
        lootArea.setForeground(TEXT);
        lootArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(lootArea);
        scroll.setPreferredSize(new Dimension(600, 260));
        p.add(scroll, BorderLayout.CENTER);

        JLabel hint = new JLabel(
                "<html><body style='color:#bbb;font-size:11px;'>" +
                "Tracks all loot collected from world boss kills this session.<br>" +
                "Both auto-looted (inventory) and ground pickup items are recorded.</body></html>");
        hint.setForeground(TEXT);
        p.add(hint, BorderLayout.SOUTH);

        return p;
    }

    // ── Gear Selection (mining preset) ────────────────────────────────────
    private JPanel buildGearSelectionPanel() {
        JPanel box = panel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GREY, 1),
                        "Gear Selection (mining)", 0, 0, null, TEXT),
                new EmptyBorder(4, 6, 4, 6)));

        int miningIdx = Math.max(1, Math.min(5, bot.getMiningPresetSlot())) - 1;
        miningPresetDropdown.setSelectedIndex(miningIdx);
        miningPresetDropdown.addActionListener(e -> {
            int slot = miningPresetDropdown.getSelectedIndex() + 1;
            bot.setMiningPresetSlot(slot);
            appendLog("Mining preset (for re-gear after boss) set to slot " + slot + ".");
        });

        box.add(label("Pickaxe preset (re-gear after boss):"));
        box.add(miningPresetDropdown);
        return box;
    }

    // ── Anti-ban controls ─────────────────────────────────────────────────
    private JPanel buildAntibanPanel() {
        JPanel box = panel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GREY, 1),
                        "Anti-ban delay", 0, 0, null, TEXT),
                new EmptyBorder(4, 6, 4, 6)));

        antibanCheck.setSelected(bot.isAntibanEnabled());
        antibanCheck.setBackground(PANEL_BG);
        antibanCheck.setForeground(TEXT);
        antibanCheck.setFocusPainted(false);

        ((JSpinner.DefaultEditor) antibanMin.getEditor()).getTextField().setColumns(5);
        ((JSpinner.DefaultEditor) antibanMax.getEditor()).getTextField().setColumns(5);
        antibanMin.setValue(bot.getAntibanMinMs());
        antibanMax.setValue(bot.getAntibanMaxMs());
        boolean initOn = bot.isAntibanEnabled();
        antibanMin.setEnabled(initOn);
        antibanMax.setEnabled(initOn);

        antibanCheck.addActionListener(e -> {
            boolean on = antibanCheck.isSelected();
            bot.setAntibanEnabled(on);
            antibanMin.setEnabled(on);
            antibanMax.setEnabled(on);
            appendLog("Anti-ban delay " + (on ? "enabled" : "disabled") + ".");
        });
        javax.swing.event.ChangeListener rangeSync = e -> {
            int lo = ((Number) antibanMin.getValue()).intValue();
            int hi = ((Number) antibanMax.getValue()).intValue();
            if (hi < lo) { hi = lo; antibanMax.setValue(hi); }
            bot.setAntibanRange(lo, hi);
        };
        antibanMin.addChangeListener(rangeSync);
        antibanMax.addChangeListener(rangeSync);

        box.add(antibanCheck);
        box.add(label("min"));
        box.add(antibanMin);
        box.add(label("max"));
        box.add(antibanMax);
        box.add(label("ms"));
        return box;
    }

    // ── Tab: Anti-Ban ─────────────────────────────────────────────────────
    private JPanel buildAntiBanTab() {
        JPanel p = panel(new BorderLayout(0, 8));

        // Top button bar
        JPanel top = panel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton refreshBtn = button("Refresh", BLUE);
        refreshBtn.addActionListener(e -> SwingUtilities.invokeLater(this::refreshAntiBanTab));
        top.add(refreshBtn);
        p.add(top, BorderLayout.NORTH);

        // Summary stats row
        JPanel stats = panel(new GridLayout(1, 4, 8, 0));
        stats.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREY, 1),
                new EmptyBorder(6, 8, 6, 8)));
        stats.add(label("Known staff:"));    stats.add(staffKnownLabel);
        stats.add(label("Nearby (≤16t):")); stats.add(staffNearbyStatLabel);

        // Staff table
        staffTable.setBackground(new Color(20, 22, 26));
        staffTable.setForeground(TEXT);
        staffTable.setGridColor(GREY);
        staffTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        staffTable.getTableHeader().setBackground(PANEL_BG);
        staffTable.getTableHeader().setForeground(TEXT);
        staffTable.setSelectionBackground(BLUE);
        staffTable.setRowHeight(20);
        staffTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        staffTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        staffTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        JScrollPane tableScroll = new JScrollPane(staffTable);
        tableScroll.setPreferredSize(new Dimension(600, 160));
        tableScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GREY, 1),
                        "Known Staff Members", 0, 0, null, TEXT),
                new EmptyBorder(2, 4, 2, 4)));

        // Chat filter header
        JPanel chatHeader = panel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        chatHeader.add(label("Filter:"));
        chatFilterDropdown.setBackground(PANEL_BG);
        chatFilterDropdown.setForeground(TEXT);
        chatFilterDropdown.addActionListener(e -> SwingUtilities.invokeLater(this::refreshAntiBanTab));
        chatHeader.add(chatFilterDropdown);

        // Chat area
        staffChatArea.setEditable(false);
        staffChatArea.setLineWrap(true);
        staffChatArea.setWrapStyleWord(true);
        staffChatArea.setBackground(new Color(20, 22, 26));
        staffChatArea.setForeground(TEXT);
        staffChatArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane chatScroll = new JScrollPane(staffChatArea);
        chatScroll.setPreferredSize(new Dimension(600, 110));

        JPanel chatPanel = panel(new BorderLayout(0, 4));
        chatPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GREY, 1),
                        "Staff Chat Log", 0, 0, null, TEXT),
                new EmptyBorder(2, 4, 2, 4)));
        chatPanel.add(chatHeader, BorderLayout.NORTH);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel center = panel(new BorderLayout(0, 8));
        center.add(stats,       BorderLayout.NORTH);
        center.add(tableScroll, BorderLayout.CENTER);
        center.add(chatPanel,   BorderLayout.SOUTH);
        p.add(center, BorderLayout.CENTER);

        JLabel hint = new JLabel(
                "<html><body style='color:#bbb;font-size:11px;'>" +
                "Uses KSBot's built-in staff detection API. 'Known' = full staff list loaded from server.<br>" +
                "'Nearby' = staff detected within 16 tiles. Chat log captures messages from known staff.</body></html>");
        hint.setForeground(TEXT);
        p.add(hint, BorderLayout.SOUTH);

        return p;
    }

    private void refreshAntiBanTab() {
        staffKnownLabel.setText(String.valueOf(bot.getStaffOnlineCount()));
        staffNearbyStatLabel.setText(String.valueOf(bot.getStaffNearbyCount()));

        // Rebuild the staff table: all known staff, mark nearby ones with distance.
        String[] all = bot.getStaffAllList();
        List<RsPerkFarmerBot.StaffEntry> nearbyList = bot.getStaffNearbyList();

        java.util.Map<String, Integer> nearbyMap = new java.util.HashMap<>();
        for (RsPerkFarmerBot.StaffEntry e : nearbyList) {
            nearbyMap.put(e.name.toLowerCase(), e.distance);
        }

        staffTableModel.setRowCount(0);
        for (String name : all) {
            Integer dist   = nearbyMap.get(name.toLowerCase());
            String  distStr = dist != null ? dist + "t" : "—";
            String  status  = dist != null ? "NEARBY" : "—";
            staffTableModel.addRow(new Object[]{name, distStr, status});
        }

        // Rebuild chat area according to selected filter.
        int filterIdx = chatFilterDropdown.getSelectedIndex();
        List<RsPerkFarmerBot.StaffChatEntry> chatLog = bot.getStaffChatLog();
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        for (RsPerkFarmerBot.StaffChatEntry entry : chatLog) {
            String ch = entry.channel != null ? entry.channel : "?";
            switch (filterIdx) {
                case 1: if (!"Help".equals(ch))              continue; break; // Help Only
                case 2: if (!"Yell".equals(ch))              continue; break; // Yell Only
                case 3: if (!entry.isStaff)                  continue; break; // Staff Only
                case 4: if (!entry.nearbyAtSend)             continue; break; // Nearby
                default: break;                                                // All Channels
            }
            sb.append('[').append(sdf.format(new Date(entry.timestampMs))).append("] ");
            sb.append('[').append(ch).append("] ");
            if (entry.isStaff) sb.append("[STAFF] ");
            if (entry.nearbyAtSend) sb.append("(nearby) ");
            sb.append(entry.sender).append(": ").append(entry.message).append('\n');
        }
        staffChatArea.setText(sb.length() == 0 ? "(No messages recorded yet)" : sb.toString());
        staffChatArea.setCaretPosition(staffChatArea.getDocument().getLength());
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
            if (bot.isPaused()) { bot.resume(); appendLog("Script resumed."); return; }
            if (bot.isUserStarted()) { appendLog("Script is already running."); return; }
            bot.userStart();
        });
        pauseBtn.addActionListener(e -> {
            if (!bot.isUserStarted()) { appendLog("Press Start first."); return; }
            if (bot.isPaused()) { bot.resume(); appendLog("Script resumed."); }
            else { bot.pause(); appendLog("Script paused."); }
        });
        stopBtn.addActionListener(e -> {
            appendLog("Stop requested.");
            bot.userStop();
        });
        updateBtn.addActionListener(e -> bot.onUpdateScriptRequested());
    }

    // ── Live refresh ──────────────────────────────────────────────────────
    public void refreshStatus() {
        SwingUtilities.invokeLater(() -> {
            BotState s = bot.getBotState();
            long now = System.currentTimeMillis();
            long delta = bot.getNextActionAtMs() - now;

            // While antiban is holding us, surface that in the "Now:" slot
            // instead of the idle state name so it's visibly working.
            if (s == BotState.WORLD_BOSS) {
                String bossText = bot.getWorldBossHandler().statusText();
                statusCurrent.setText(bossText);
                bossStatusLabel.setText(bossText);
            } else if (bot.isAntibanEnabled() && delta > 0 && bot.getLastAntibanExtraMs() > 0
                    && !bot.isPaused() && s != BotState.FAULT) {
                statusCurrent.setText("Delayed " + bot.getLastAntibanExtraMs()
                        + " ms (antiban)");
                bossStatusLabel.setText("—");
            } else {
                statusCurrent.setText(s.currentAction());
                bossStatusLabel.setText(bot.getWorldBossHandler().isGlobalEnabled()
                        ? "Watching for events" : "Disabled");
            }
            statusNext.setText(s.nextAction());
            stateLabel.setText(s.name());

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

            int lvl  = bot.getCurrentPerkLevel();
            int pts  = bot.getCurrentPerkPoints();
            int gain = bot.getTotalPerkPointsEarned();
            perkLevelLabel.setText(lvl  > 0 ? String.valueOf(lvl) : "—");
            perkPointsLabel.setText(pts > 0
                    ? pts + "  (session +" + gain + ")"
                    : "—");

            int target = bot.getTaskTargetCount();
            int mined  = bot.getTaskMinedThisTask();
            String prog = (target > 0 ? mined + " / " + target + " runite ore"
                                      : (mined > 0 ? mined + " / ? runite ore" : "— / —"));
            taskProgressLabel.setText(prog);
            statusTask.setText(prog);

            String fault = bot.getLastFaultReason();
            faultLabel.setText(fault == null || fault.isBlank() ? "(none)" : fault);

            // Players nearby (live from handler)
            nearbyPlayersLabel.setText(String.valueOf(
                    bot.getWorldBossHandler().getNearbyPlayerCount()));

            // Time since last mine / last action
            long lm = bot.getLastMineAtMs();
            lastMineLabel.setText(lm == 0 ? "—" : formatMs(now - lm) + " ago");
            long la = bot.getLastStateChangeMs();
            lastActionLabel.setText(la == 0 ? "—" : formatMs(now - la) + " ago");

            // Per-boss stats
            java.util.Map<String, ReasonWorldBossHandler.BossStats> bs =
                    bot.getWorldBossHandler().getBossStats();
            StringBuilder sb = new StringBuilder("<html><pre style='color:#eee;margin:0;'>");
            sb.append(String.format("%-20s %4s %4s %4s %4s %4s%n",
                    "Boss", "Try", "Kil", "Bai", "Dth", "AOE"));
            int tT=0,tK=0,tB=0,tD=0,tA=0;
            for (java.util.Map.Entry<String, ReasonWorldBossHandler.BossStats> e : bs.entrySet()) {
                ReasonWorldBossHandler.BossStats st = e.getValue();
                sb.append(String.format("%-20s %4d %4d %4d %4d %4d%n",
                        trunc(e.getKey(), 20),
                        st.attempted, st.killed, st.bailed, st.deaths, st.aoeDodges));
                tT+=st.attempted; tK+=st.killed; tB+=st.bailed;
                tD+=st.deaths;    tA+=st.aoeDodges;
            }
            sb.append(String.format("%-20s %4d %4d %4d %4d %4d",
                    "TOTAL", tT, tK, tB, tD, tA));
            sb.append("</pre></html>");
            bossStatsLabel.setText(sb.toString());

            // Refresh loot + anti-ban tabs every tick
            refreshLootArea();
            refreshAntiBanTab();
        });
    }

    private static String trunc(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }

    /** Pull latest loot data from the world boss handler and update the Loot tab. */
    private void refreshLootArea() {
        java.util.Map<String, Integer> loot = bot.getWorldBossHandler().getSessionLoot();
        if (loot.isEmpty()) {
            lootArea.setText("(No loot yet)\n\nLoot from world boss kills will appear here.\n"
                    + "Both auto-looted and ground-pickup items are tracked.");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Boss Loot (session) ===\n\n");
            int totalItems = 0;
            for (java.util.Map.Entry<String, Integer> e : loot.entrySet()) {
                sb.append("  ").append(e.getKey())
                  .append(" x").append(e.getValue()).append('\n');
                totalItems += e.getValue();
            }
            sb.append("\n--- ").append(loot.size()).append(" unique items, ")
              .append(totalItems).append(" total ---\n");
            lootArea.setText(sb.toString());
        }
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
