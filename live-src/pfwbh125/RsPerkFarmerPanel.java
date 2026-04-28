package pfwbh125;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
        tabs.addTab("Delays",   buildDelaysTab());
        tabs.addTab("Breaks",   buildBreaksTab());
        tabs.addTab("Webhook",  buildWebhookTab());
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

    // ── Tab: Delays ───────────────────────────────────────────────────────
    /** Per-category min/max spinner pairs. Kept so Save/Load can mutate them. */
    private final Map<DelayProfile.Category, JSpinner> delayMinSpinners =
            new EnumMap<>(DelayProfile.Category.class);
    private final Map<DelayProfile.Category, JSpinner> delayMaxSpinners =
            new EnumMap<>(DelayProfile.Category.class);

    private JPanel buildDelaysTab() {
        JPanel p = panel(new BorderLayout(0, 8));

        // Header explainer
        JLabel header = new JLabel(
                "<html><body style='color:#bbb;font-size:11px;'>" +
                "Per-action delay windows (ms). The bot picks a uniform-random value " +
                "in each window every time that action fires.<br>" +
                "Save your tuned profile as JSON, or load one you saved earlier.</body></html>");
        header.setForeground(TEXT);
        header.setBorder(new EmptyBorder(0, 2, 6, 2));
        p.add(header, BorderLayout.NORTH);

        // Delay rows
        JPanel rows = panel(new GridBagLayout());
        rows.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GREY, 1),
                        "Action Delay Windows", 0, 0, null, TEXT),
                new EmptyBorder(6, 8, 6, 8)));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;
        for (DelayProfile.Category cat : DelayProfile.Category.values()) {
            int curMin = bot.delays.min(cat);
            int curMax = bot.delays.max(cat);

            JSpinner minSp = new JSpinner(new SpinnerNumberModel(curMin, 0, 20000, 25));
            JSpinner maxSp = new JSpinner(new SpinnerNumberModel(curMax, 0, 20000, 25));
            styleSpinner(minSp);
            styleSpinner(maxSp);
            delayMinSpinners.put(cat, minSp);
            delayMaxSpinners.put(cat, maxSp);

            // Live-push spinner values into the bot whenever the user changes them.
            javax.swing.event.ChangeListener cl = e -> pushDelayToBot(cat);
            minSp.addChangeListener(cl);
            maxSp.addChangeListener(cl);

            gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
            JLabel nameLbl = label(cat.label);
            nameLbl.setPreferredSize(new Dimension(120, 22));
            rows.add(nameLbl, gc);
            gc.gridx = 1; rows.add(label("Min:"), gc);
            gc.gridx = 2; rows.add(minSp, gc);
            gc.gridx = 3; rows.add(label("Max:"), gc);
            gc.gridx = 4; rows.add(maxSp, gc);
            gc.gridx = 5; rows.add(label("ms"), gc);
            row++;
        }

        // Idle / AFK jitter section — separate from the per-action windows.
        JPanel idle = panel(new GridBagLayout());
        idle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GREY, 1),
                        "Idle / AFK Jitter", 0, 0, null, TEXT),
                new EmptyBorder(6, 8, 6, 8)));
        GridBagConstraints ig = new GridBagConstraints();
        ig.insets = new Insets(4, 6, 4, 6);
        ig.anchor = GridBagConstraints.WEST;

        idleEnabledCheck.setBackground(PANEL_BG);
        idleEnabledCheck.setForeground(TEXT);
        idleEnabledCheck.setFocusPainted(false);
        idleEnabledCheck.setSelected(bot.delays.isIdleEnabled());
        idleEnabledCheck.addActionListener(e -> bot.delays.setIdleEnabled(idleEnabledCheck.isSelected()));

        idleMinSpinner.setValue(bot.delays.getIdleMinMs());
        idleMaxSpinner.setValue(bot.delays.getIdleMaxMs());
        idleFreqSpinner.setValue(bot.delays.getIdleFrequency());
        styleSpinner(idleMinSpinner);
        styleSpinner(idleMaxSpinner);
        styleSpinner(idleFreqSpinner);
        idleMinSpinner.addChangeListener(e -> pushIdleToBot());
        idleMaxSpinner.addChangeListener(e -> pushIdleToBot());
        idleFreqSpinner.addChangeListener(e -> pushIdleToBot());

        ig.gridx = 0; ig.gridy = 0; ig.gridwidth = 6;
        idle.add(idleEnabledCheck, ig);
        ig.gridwidth = 1;

        ig.gridy = 1;
        ig.gridx = 0; idle.add(label("Pause length:"), ig);
        ig.gridx = 1; idle.add(label("Min:"), ig);
        ig.gridx = 2; idle.add(idleMinSpinner, ig);
        ig.gridx = 3; idle.add(label("Max:"), ig);
        ig.gridx = 4; idle.add(idleMaxSpinner, ig);
        ig.gridx = 5; idle.add(idleUnitLabel, ig);

        // Unit dropdown row — ms or seconds
        ig.gridy = 2;
        ig.gridx = 0; idle.add(label("Unit:"), ig);
        ig.gridx = 1; ig.gridwidth = 2;
        idleUnitDropdown.setBackground(PANEL_BG);
        idleUnitDropdown.setForeground(TEXT);
        idleUnitDropdown.addActionListener(e -> applyIdleUnit());
        idle.add(idleUnitDropdown, ig);
        ig.gridwidth = 1;
        // initialize spinners to match current unit selection + bot values
        applyIdleUnit();

        ig.gridy = 3;
        ig.gridx = 0; idle.add(label("Frequency:"), ig);
        ig.gridx = 1; idle.add(label("1 in"), ig);
        ig.gridx = 2; idle.add(idleFreqSpinner, ig);
        ig.gridx = 3; ig.gridwidth = 3;
        idle.add(label("scheduleIn calls trigger a pause"), ig);
        ig.gridwidth = 1;

        // Stack rows (per-action) + idle section in CENTER.
        JPanel center = panel(new BorderLayout(0, 8));
        center.add(rows, BorderLayout.CENTER);
        center.add(idle, BorderLayout.SOUTH);
        p.add(center, BorderLayout.CENTER);

        // Save / Load button bar
        JPanel btns = panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton saveBtn    = button("Save Config",    GREEN);
        JButton loadBtn    = button("Load Config",    BLUE);
        JButton defaultBtn = button("Reset Defaults", GREY);
        saveBtn.addActionListener(e -> saveDelayConfig());
        loadBtn.addActionListener(e -> loadDelayConfig());
        defaultBtn.addActionListener(e -> resetDelayDefaults());
        btns.add(saveBtn);
        btns.add(loadBtn);
        btns.add(defaultBtn);
        p.add(btns, BorderLayout.SOUTH);

        return p;
    }

    // Idle-jitter widgets (declared here so buildDelaysTab can wire them).
    private final JCheckBox idleEnabledCheck =
            new JCheckBox("Enable idle / AFK jitter (random long pauses)");
    private final JSpinner idleMinSpinner  =
            new JSpinner(new SpinnerNumberModel(5000,  0, 600000, 500));
    private final JSpinner idleMaxSpinner  =
            new JSpinner(new SpinnerNumberModel(30000, 0, 600000, 500));
    private final JSpinner idleFreqSpinner =
            new JSpinner(new SpinnerNumberModel(50, 1, 10000, 5));
    /** Unit dropdown for idle jitter fields — Milliseconds or Seconds.
     *  Internally the bot always stores ms; this just changes the display
     *  and step size in the spinners. */
    private final JComboBox<String> idleUnitDropdown =
            new JComboBox<>(new String[]{"Milliseconds", "Seconds"});
    private final JLabel idleUnitLabel = label("ms");

    private boolean idleUnitIsSeconds() {
        return idleUnitDropdown.getSelectedIndex() == 1;
    }

    /** Apply the current unit to the three idle spinners — rescale the
     *  underlying values so switching units is lossless for the user. */
    private void applyIdleUnit() {
        boolean sec = idleUnitIsSeconds();
        int mnMs = bot.delays.getIdleMinMs();
        int mxMs = bot.delays.getIdleMaxMs();
        SpinnerNumberModel mnMdl = (SpinnerNumberModel) idleMinSpinner.getModel();
        SpinnerNumberModel mxMdl = (SpinnerNumberModel) idleMaxSpinner.getModel();
        idleUnitLabel.setText(sec ? "sec" : "ms");
        idleSuspendPush = true;
        try {
            if (sec) {
                mnMdl.setMinimum(0); mnMdl.setMaximum(3600); mnMdl.setStepSize(1);
                mxMdl.setMinimum(0); mxMdl.setMaximum(3600); mxMdl.setStepSize(1);
                idleMinSpinner.setValue(Math.round(mnMs / 1000f));
                idleMaxSpinner.setValue(Math.round(mxMs / 1000f));
            } else {
                mnMdl.setMinimum(0); mnMdl.setMaximum(600000); mnMdl.setStepSize(500);
                mxMdl.setMinimum(0); mxMdl.setMaximum(600000); mxMdl.setStepSize(500);
                idleMinSpinner.setValue(mnMs);
                idleMaxSpinner.setValue(mxMs);
            }
        } finally {
            idleSuspendPush = false;
        }
    }

    /** Guard so applyIdleUnit() rescaling doesn't re-push stale values to the bot. */
    private boolean idleSuspendPush = false;

    private void pushIdleToBot() {
        if (idleSuspendPush) return;
        int mn = (Integer) idleMinSpinner.getValue();
        int mx = (Integer) idleMaxSpinner.getValue();
        if (mx < mn) { mx = mn; idleMaxSpinner.setValue(mx); }
        int fq = (Integer) idleFreqSpinner.getValue();
        int mnMs = idleUnitIsSeconds() ? mn * 1000 : mn;
        int mxMs = idleUnitIsSeconds() ? mx * 1000 : mx;
        bot.delays.setIdleMinMs(mnMs);
        bot.delays.setIdleMaxMs(mxMs);
        bot.delays.setIdleFrequency(fq);
    }

    private void refreshIdleFromBot() {
        idleEnabledCheck.setSelected(bot.delays.isIdleEnabled());
        applyIdleUnit();  // also pulls latest min/max in the right unit
        idleFreqSpinner.setValue(bot.delays.getIdleFrequency());
    }

    private void styleSpinner(JSpinner sp) {
        sp.setPreferredSize(new Dimension(80, 22));
        JComponent editor = sp.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setBackground(PANEL_BG);
            ((JSpinner.DefaultEditor) editor).getTextField().setForeground(TEXT);
        }
    }

    /** Clamp max ≥ min, then push this category's pair into the bot. */
    private void pushDelayToBot(DelayProfile.Category cat) {
        int mn = (Integer) delayMinSpinners.get(cat).getValue();
        int mx = (Integer) delayMaxSpinners.get(cat).getValue();
        if (mx < mn) {
            mx = mn;
            delayMaxSpinners.get(cat).setValue(mx);
        }
        bot.delays.set(cat, mn, mx);
    }

    private void refreshDelaySpinnersFromBot() {
        for (DelayProfile.Category cat : DelayProfile.Category.values()) {
            delayMinSpinners.get(cat).setValue(bot.delays.min(cat));
            delayMaxSpinners.get(cat).setValue(bot.delays.max(cat));
        }
    }

    private void saveDelayConfig() {
        // Push latest spinner values first in case a change is mid-edit.
        for (DelayProfile.Category cat : DelayProfile.Category.values()) pushDelayToBot(cat);

        JFileChooser ch = new JFileChooser();
        ch.setDialogTitle("Save delay profile");
        ch.setSelectedFile(new File("perk-farmer-delays.json"));
        if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = ch.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".json")) {
            f = new File(f.getParentFile(), f.getName() + ".json");
        }
        try {
            bot.delays.saveTo(f.getAbsolutePath());
            JOptionPane.showMessageDialog(this,
                    "Saved delay profile to:\n" + f.getAbsolutePath(),
                    "Save OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Save failed: " + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDelayConfig() {
        JFileChooser ch = new JFileChooser();
        ch.setDialogTitle("Load delay profile");
        if (ch.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = ch.getSelectedFile();
        try {
            bot.delays.loadFrom(f.getAbsolutePath());
            refreshDelaySpinnersFromBot();
            refreshIdleFromBot();
            JOptionPane.showMessageDialog(this,
                    "Loaded delay profile from:\n" + f.getAbsolutePath(),
                    "Load OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Load failed: " + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetDelayDefaults() {
        DelayProfile fresh = new DelayProfile();
        for (DelayProfile.Category cat : DelayProfile.Category.values()) {
            bot.delays.set(cat, fresh.min(cat), fresh.max(cat));
        }
        bot.delays.setIdleEnabled(fresh.isIdleEnabled());
        bot.delays.setIdleMinMs(fresh.getIdleMinMs());
        bot.delays.setIdleMaxMs(fresh.getIdleMaxMs());
        bot.delays.setIdleFrequency(fresh.getIdleFrequency());
        refreshDelaySpinnersFromBot();
        refreshIdleFromBot();
    }

    // ── Tab: Breaks ───────────────────────────────────────────────────────
    // Widgets kept as fields so refresh can update them.
    private final JLabel breakStateLabel       = value("IDLE");
    private final JLabel nextBreakLabel        = value("—");
    private final JLabel lastBreakLabel        = value("—");
    private final JLabel breakHistoryLabel     = value("—");

    // Per-kind controls (Quick + Long).
    private final java.util.Map<BreakConfig.Kind, JCheckBox>  brkEnabled    = new java.util.EnumMap<>(BreakConfig.Kind.class);
    private final java.util.Map<BreakConfig.Kind, JSpinner>   brkDurMin     = new java.util.EnumMap<>(BreakConfig.Kind.class);
    private final java.util.Map<BreakConfig.Kind, JSpinner>   brkDurMax     = new java.util.EnumMap<>(BreakConfig.Kind.class);
    private final java.util.Map<BreakConfig.Kind, JSpinner>   brkFreqMin    = new java.util.EnumMap<>(BreakConfig.Kind.class);
    private final java.util.Map<BreakConfig.Kind, JSpinner>   brkFreqMax    = new java.util.EnumMap<>(BreakConfig.Kind.class);
    private final java.util.Map<BreakConfig.Kind, JComboBox<String>> brkLogoutMode = new java.util.EnumMap<>(BreakConfig.Kind.class);
    private final java.util.Map<BreakConfig.Kind, JSpinner>   brkRamp       = new java.util.EnumMap<>(BreakConfig.Kind.class);
    private final java.util.Map<BreakConfig.Kind, JSpinner>   brkMaxPerHour = new java.util.EnumMap<>(BreakConfig.Kind.class);
    private final java.util.Map<BreakConfig.Kind, JCheckBox>  brkWindowEnabled = new java.util.EnumMap<>(BreakConfig.Kind.class);
    private final java.util.Map<BreakConfig.Kind, JSpinner>   brkWindowStart = new java.util.EnumMap<>(BreakConfig.Kind.class);
    private final java.util.Map<BreakConfig.Kind, JSpinner>   brkWindowEnd   = new java.util.EnumMap<>(BreakConfig.Kind.class);
    private final java.util.Map<BreakConfig.Kind, java.util.Map<java.time.DayOfWeek, JCheckBox>> brkDays =
            new java.util.EnumMap<>(BreakConfig.Kind.class);

    private JPanel buildBreaksTab() {
        JPanel p = panel(new BorderLayout(0, 8));

        // Header
        JLabel header = new JLabel(
                "<html><body style='color:#bbb;font-size:11px;'>" +
                "Smart break scheduler. <b>Quick</b> = short 5–30 min breaks on a frequency cadence " +
                "(smoke / bathroom).<br><b>Long</b> = 6–9 hr breaks inside a time-of-day window (sleep / shift).<br>" +
                "Breaks never fire mid-combat, inventory-full, or near staff. Values in minutes unless labeled.</body></html>");
        header.setForeground(TEXT);
        header.setBorder(new EmptyBorder(0, 2, 6, 2));
        p.add(header, BorderLayout.NORTH);

        // Live status row
        JPanel status = panel(new GridLayout(1, 8, 8, 0));
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREY, 1),
                new EmptyBorder(6, 8, 6, 8)));
        status.add(label("State:"));       status.add(breakStateLabel);
        status.add(label("Next break:"));  status.add(nextBreakLabel);
        status.add(label("Last ended:"));  status.add(lastBreakLabel);
        status.add(label("History:"));     status.add(breakHistoryLabel);

        // Two configuration panels side-by-side
        JPanel cfgRow = panel(new GridLayout(1, 2, 8, 0));
        cfgRow.add(buildBreakKindPanel(BreakConfig.Kind.QUICK, bot.breaks.quick));
        cfgRow.add(buildBreakKindPanel(BreakConfig.Kind.LONG,  bot.breaks.longg));

        // Manual controls + Save/Load
        JPanel btns = panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton takeQuickBtn = button("Take Quick Now",  AMBER);
        JButton takeLongBtn  = button("Take Long Now",   AMBER);
        JButton skipBtn      = button("Skip Next",       GREY);
        JButton delayBtn     = button("Delay +30 min",   GREY);
        JButton saveBtn      = button("Save Config",     GREEN);
        JButton loadBtn      = button("Load Config",     BLUE);
        JButton resetBtn     = button("Reset Defaults",  GREY);

        takeQuickBtn.addActionListener(e -> { bot.breaks.takeBreakNow(BreakConfig.Kind.QUICK); appendLog("Manual Quick break requested."); });
        takeLongBtn .addActionListener(e -> { bot.breaks.takeBreakNow(BreakConfig.Kind.LONG);  appendLog("Manual Long break requested."); });
        skipBtn     .addActionListener(e -> { bot.breaks.skipNextBreak(); appendLog("Next break skipped / rescheduled."); });
        delayBtn    .addActionListener(e -> { bot.breaks.delayNextBreak(30L * 60_000); appendLog("Next break pushed +30 min."); });
        saveBtn     .addActionListener(e -> saveBreaksConfig());
        loadBtn     .addActionListener(e -> loadBreaksConfig());
        resetBtn    .addActionListener(e -> resetBreaksDefaults());

        btns.add(takeQuickBtn); btns.add(takeLongBtn); btns.add(skipBtn); btns.add(delayBtn);
        btns.add(saveBtn);      btns.add(loadBtn);     btns.add(resetBtn);

        JPanel center = panel(new BorderLayout(0, 8));
        center.add(status, BorderLayout.NORTH);
        center.add(cfgRow, BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);
        p.add(btns,   BorderLayout.SOUTH);

        return p;
    }

    private JPanel buildBreakKindPanel(BreakConfig.Kind kind, BreakConfig cfg) {
        JPanel box = panel(new GridBagLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GREY, 1),
                        kind == BreakConfig.Kind.QUICK ? "Quick Break (5–30 min)" : "Long Break (Sleep / Shift)",
                        0, 0, null, TEXT),
                new EmptyBorder(6, 8, 6, 8)));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 4, 3, 4);
        gc.anchor = GridBagConstraints.WEST;

        JCheckBox enabled = new JCheckBox("Enabled");
        enabled.setBackground(PANEL_BG); enabled.setForeground(TEXT); enabled.setFocusPainted(false);
        enabled.setSelected(cfg.enabled);
        enabled.addActionListener(e -> { cfg.enabled = enabled.isSelected(); appendLog(kind + " break " + (cfg.enabled ? "enabled" : "disabled") + "."); });
        brkEnabled.put(kind, enabled);
        gc.gridy = 0; gc.gridx = 0; gc.gridwidth = 4; box.add(enabled, gc); gc.gridwidth = 1;

        // Duration min/max (minutes for Quick; minutes for Long too — we'll store in ms)
        int durScale = 60_000;   // spinners are in MINUTES
        JSpinner dMin = minuteSpinner((int)(cfg.minDurationMs / durScale), 1, kind == BreakConfig.Kind.LONG ? 720 : 120);
        JSpinner dMax = minuteSpinner((int)(cfg.maxDurationMs / durScale), 1, kind == BreakConfig.Kind.LONG ? 720 : 120);
        dMin.addChangeListener(e -> cfg.minDurationMs = ((Integer)dMin.getValue()) * (long)durScale);
        dMax.addChangeListener(e -> cfg.maxDurationMs = ((Integer)dMax.getValue()) * (long)durScale);
        brkDurMin.put(kind, dMin); brkDurMax.put(kind, dMax);
        gc.gridy = 1; gc.gridx = 0; box.add(label("Duration:"), gc);
        gc.gridx = 1; box.add(label("Min"), gc);
        gc.gridx = 2; box.add(dMin, gc);
        gc.gridx = 3; box.add(label("Max"), gc);
        gc.gridx = 4; box.add(dMax, gc);
        gc.gridx = 5; box.add(label("min"), gc);

        JSpinner fMin = minuteSpinner((int)(cfg.frequencyMinMs / durScale), 1, kind == BreakConfig.Kind.LONG ? 2880 : 720);
        JSpinner fMax = minuteSpinner((int)(cfg.frequencyMaxMs / durScale), 1, kind == BreakConfig.Kind.LONG ? 2880 : 720);
        fMin.addChangeListener(e -> cfg.frequencyMinMs = ((Integer)fMin.getValue()) * (long)durScale);
        fMax.addChangeListener(e -> cfg.frequencyMaxMs = ((Integer)fMax.getValue()) * (long)durScale);
        brkFreqMin.put(kind, fMin); brkFreqMax.put(kind, fMax);
        gc.gridy = 2; gc.gridx = 0; box.add(label("Frequency:"), gc);
        gc.gridx = 1; box.add(label("Min"), gc);
        gc.gridx = 2; box.add(fMin, gc);
        gc.gridx = 3; box.add(label("Max"), gc);
        gc.gridx = 4; box.add(fMax, gc);
        gc.gridx = 5; box.add(label("min"), gc);

        // Max per hour (QUICK) / not really used for LONG but show anyway
        JSpinner mph = new JSpinner(new SpinnerNumberModel(cfg.maxBreaksPerHour, 0, 20, 1));
        styleSpinner(mph);
        mph.addChangeListener(e -> cfg.maxBreaksPerHour = (Integer)mph.getValue());
        brkMaxPerHour.put(kind, mph);
        gc.gridy = 3; gc.gridx = 0; box.add(label("Max / hour:"), gc);
        gc.gridx = 1; gc.gridwidth = 2; box.add(mph, gc);
        gc.gridx = 3; gc.gridwidth = 3; box.add(label("(0 = unlimited)"), gc);
        gc.gridwidth = 1;

        // Logout mode
        JComboBox<String> logout = new JComboBox<>(new String[]{"Logout (click logout widget)", "AFK in place"});
        logout.setBackground(PANEL_BG); logout.setForeground(TEXT);
        logout.setSelectedIndex(cfg.logoutMode == BreakConfig.LogoutMode.LOGOUT ? 0 : 1);
        logout.addActionListener(e -> cfg.logoutMode =
                logout.getSelectedIndex() == 0 ? BreakConfig.LogoutMode.LOGOUT : BreakConfig.LogoutMode.AFK_IN_PLACE);
        brkLogoutMode.put(kind, logout);
        gc.gridy = 4; gc.gridx = 0; box.add(label("On break:"), gc);
        gc.gridx = 1; gc.gridwidth = 5; box.add(logout, gc); gc.gridwidth = 1;

        // Post-break ramp (minutes)
        JSpinner ramp = minuteSpinner((int)(cfg.postBreakRampMs / durScale), 0, 60);
        ramp.addChangeListener(e -> cfg.postBreakRampMs = ((Integer)ramp.getValue()) * (long)durScale);
        brkRamp.put(kind, ramp);
        gc.gridy = 5; gc.gridx = 0; box.add(label("Ramp after:"), gc);
        gc.gridx = 1; gc.gridwidth = 2; box.add(ramp, gc);
        gc.gridx = 3; gc.gridwidth = 3; box.add(label("min (slower actions)"), gc);
        gc.gridwidth = 1;

        // Time window (LONG)
        JCheckBox winOn = new JCheckBox("Time-of-day window");
        winOn.setBackground(PANEL_BG); winOn.setForeground(TEXT); winOn.setFocusPainted(false);
        winOn.setSelected(cfg.windowEnabled);
        winOn.addActionListener(e -> cfg.windowEnabled = winOn.isSelected());
        brkWindowEnabled.put(kind, winOn);

        JSpinner winS = timeSpinner(cfg.windowStart);
        JSpinner winE = timeSpinner(cfg.windowEnd);
        winS.addChangeListener(e -> cfg.windowStart = toLocalTime(winS.getValue()));
        winE.addChangeListener(e -> cfg.windowEnd   = toLocalTime(winE.getValue()));
        brkWindowStart.put(kind, winS); brkWindowEnd.put(kind, winE);

        gc.gridy = 6; gc.gridx = 0; gc.gridwidth = 6; box.add(winOn, gc); gc.gridwidth = 1;
        gc.gridy = 7; gc.gridx = 0; box.add(label("From:"), gc);
        gc.gridx = 1; box.add(winS, gc);
        gc.gridx = 2; box.add(label("To:"), gc);
        gc.gridx = 3; box.add(winE, gc);
        gc.gridx = 4; gc.gridwidth = 2; box.add(label("(crosses midnight OK)"), gc);
        gc.gridwidth = 1;

        // Allowed days
        JPanel days = panel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        java.util.Map<java.time.DayOfWeek, JCheckBox> dayMap = new java.util.EnumMap<>(java.time.DayOfWeek.class);
        for (java.time.DayOfWeek dow : java.time.DayOfWeek.values()) {
            JCheckBox cb = new JCheckBox(dow.name().substring(0, 2));
            cb.setBackground(PANEL_BG); cb.setForeground(TEXT); cb.setFocusPainted(false);
            cb.setSelected(cfg.allowedDays.contains(dow));
            cb.addActionListener(e -> {
                if (cb.isSelected()) cfg.allowedDays.add(dow);
                else                 cfg.allowedDays.remove(dow);
            });
            dayMap.put(dow, cb);
            days.add(cb);
        }
        brkDays.put(kind, dayMap);
        gc.gridy = 8; gc.gridx = 0; box.add(label("Days:"), gc);
        gc.gridx = 1; gc.gridwidth = 5; box.add(days, gc); gc.gridwidth = 1;

        return box;
    }

    private JSpinner minuteSpinner(int cur, int min, int max) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(
                Math.max(min, Math.min(max, cur)), min, max, 1));
        styleSpinner(s);
        return s;
    }

    private JSpinner timeSpinner(java.time.LocalTime t) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, t.getHour());
        cal.set(java.util.Calendar.MINUTE,      t.getMinute());
        cal.set(java.util.Calendar.SECOND, 0);
        SpinnerDateModel mdl = new SpinnerDateModel(cal.getTime(), null, null, java.util.Calendar.MINUTE);
        JSpinner s = new JSpinner(mdl);
        s.setEditor(new JSpinner.DateEditor(s, "HH:mm"));
        s.setPreferredSize(new Dimension(70, 22));
        JComponent ed = s.getEditor();
        if (ed instanceof JSpinner.DateEditor) {
            ((JSpinner.DateEditor) ed).getTextField().setBackground(PANEL_BG);
            ((JSpinner.DateEditor) ed).getTextField().setForeground(TEXT);
        }
        return s;
    }

    private java.time.LocalTime toLocalTime(Object val) {
        if (val instanceof java.util.Date) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.setTime((java.util.Date) val);
            return java.time.LocalTime.of(c.get(java.util.Calendar.HOUR_OF_DAY),
                                          c.get(java.util.Calendar.MINUTE));
        }
        return java.time.LocalTime.MIDNIGHT;
    }

    private void saveBreaksConfig() {
        JFileChooser ch = new JFileChooser();
        ch.setDialogTitle("Save break config");
        ch.setSelectedFile(new File("perk-farmer-breaks.json"));
        if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = ch.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".json"))
            f = new File(f.getParentFile(), f.getName() + ".json");
        try {
            bot.breaks.saveTo(f.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Saved break config:\n" + f.getAbsolutePath(),
                    "Save OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadBreaksConfig() {
        JFileChooser ch = new JFileChooser();
        ch.setDialogTitle("Load break config");
        if (ch.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = ch.getSelectedFile();
        try {
            bot.breaks.loadFrom(f.getAbsolutePath());
            refreshBreaksFromBot();
            JOptionPane.showMessageDialog(this, "Loaded break config:\n" + f.getAbsolutePath(),
                    "Load OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetBreaksDefaults() {
        BreakConfig dq = new BreakConfig(BreakConfig.Kind.QUICK);
        BreakConfig dl = new BreakConfig(BreakConfig.Kind.LONG);
        copyCfg(dq, bot.breaks.quick);
        copyCfg(dl, bot.breaks.longg);
        refreshBreaksFromBot();
        appendLog("Break defaults restored.");
    }

    private static void copyCfg(BreakConfig src, BreakConfig dst) {
        dst.enabled = src.enabled;
        dst.minDurationMs = src.minDurationMs;
        dst.maxDurationMs = src.maxDurationMs;
        dst.frequencyMinMs = src.frequencyMinMs;
        dst.frequencyMaxMs = src.frequencyMaxMs;
        dst.maxBreaksPerHour = src.maxBreaksPerHour;
        dst.logoutMode = src.logoutMode;
        dst.postBreakRampMs = src.postBreakRampMs;
        dst.windowEnabled = src.windowEnabled;
        dst.windowStart = src.windowStart;
        dst.windowEnd = src.windowEnd;
        dst.minUptimeBeforeFirstBreakMs = src.minUptimeBeforeFirstBreakMs;
        dst.minUptimeBetweenBreaksMs = src.minUptimeBetweenBreaksMs;
        dst.allowedDays.clear();
        dst.allowedDays.addAll(src.allowedDays);
    }

    /** Push every control's value back from the bot (e.g. after Load / Reset). */
    private void refreshBreaksFromBot() {
        refreshBreakKind(BreakConfig.Kind.QUICK, bot.breaks.quick);
        refreshBreakKind(BreakConfig.Kind.LONG,  bot.breaks.longg);
    }

    private void refreshBreakKind(BreakConfig.Kind k, BreakConfig cfg) {
        if (brkEnabled.get(k) != null) brkEnabled.get(k).setSelected(cfg.enabled);
        if (brkDurMin.get(k)  != null) brkDurMin.get(k).setValue((int)(cfg.minDurationMs / 60_000));
        if (brkDurMax.get(k)  != null) brkDurMax.get(k).setValue((int)(cfg.maxDurationMs / 60_000));
        if (brkFreqMin.get(k) != null) brkFreqMin.get(k).setValue((int)(cfg.frequencyMinMs / 60_000));
        if (brkFreqMax.get(k) != null) brkFreqMax.get(k).setValue((int)(cfg.frequencyMaxMs / 60_000));
        if (brkMaxPerHour.get(k) != null) brkMaxPerHour.get(k).setValue(cfg.maxBreaksPerHour);
        if (brkLogoutMode.get(k) != null) brkLogoutMode.get(k).setSelectedIndex(
                cfg.logoutMode == BreakConfig.LogoutMode.LOGOUT ? 0 : 1);
        if (brkRamp.get(k)    != null) brkRamp.get(k).setValue((int)(cfg.postBreakRampMs / 60_000));
        if (brkWindowEnabled.get(k) != null) brkWindowEnabled.get(k).setSelected(cfg.windowEnabled);
        // Window spinners — recreate the Date from LocalTime
        if (brkWindowStart.get(k) != null) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.set(java.util.Calendar.HOUR_OF_DAY, cfg.windowStart.getHour());
            c.set(java.util.Calendar.MINUTE, cfg.windowStart.getMinute());
            c.set(java.util.Calendar.SECOND, 0);
            brkWindowStart.get(k).setValue(c.getTime());
        }
        if (brkWindowEnd.get(k) != null) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.set(java.util.Calendar.HOUR_OF_DAY, cfg.windowEnd.getHour());
            c.set(java.util.Calendar.MINUTE, cfg.windowEnd.getMinute());
            c.set(java.util.Calendar.SECOND, 0);
            brkWindowEnd.get(k).setValue(c.getTime());
        }
        java.util.Map<java.time.DayOfWeek, JCheckBox> map = brkDays.get(k);
        if (map != null) {
            for (java.util.Map.Entry<java.time.DayOfWeek, JCheckBox> e : map.entrySet()) {
                e.getValue().setSelected(cfg.allowedDays.contains(e.getKey()));
            }
        }
    }

    /** Refresh the live break status labels. Called from refreshStatus(). */
    private void refreshBreakStatus() {
        BreakManager.State s = bot.breaks.getState();
        breakStateLabel.setText(s.name() + (bot.delays.isRampActive() ? " (ramp)" : ""));
        long now = System.currentTimeMillis();

        String next;
        switch (s) {
            case ACTIVE: next = "ends in " + formatMs(Math.max(0, bot.breaks.getBreakEndAtMs() - now)); break;
            case RAMP:   next = "ramp " + formatMs(Math.max(0, bot.breaks.getRampEndAtMs()  - now));   break;
            default: {
                long nq = bot.breaks.getNextQuickAtMs();
                long nl = bot.breaks.getNextLongAtMs();
                long pick = 0; String kind = "";
                if (nq > 0 && (pick == 0 || nq < pick)) { pick = nq; kind = "Quick"; }
                if (nl > 0 && (pick == 0 || nl < pick)) { pick = nl; kind = "Long";  }
                next = pick == 0 ? "—" : kind + " in " + formatMs(Math.max(0, pick - now));
            }
        }
        nextBreakLabel.setText(next);

        long lbe = bot.breaks.getLastBreakEndedAtMs();
        lastBreakLabel.setText(lbe == 0 ? "—" : formatMs(now - lbe) + " ago");

        java.util.List<BreakManager.BreakRecord> hist = bot.breaks.getHistory();
        if (hist.isEmpty()) {
            breakHistoryLabel.setText("0 breaks this session");
        } else {
            int q = 0, l = 0;
            long totalMs = 0;
            for (BreakManager.BreakRecord r : hist) {
                if (r.kind == BreakConfig.Kind.QUICK) q++; else l++;
                totalMs += r.durationMs();
            }
            breakHistoryLabel.setText(q + " quick, " + l + " long (total " + formatMs(totalMs) + ")");
        }
    }

    // ── Tab: Webhook ──────────────────────────────────────────────────────
    private final JTextField webhookUrlField  = new JTextField("", 40);
    private final JSpinner   webhookMilestone = new JSpinner(new SpinnerNumberModel(100, 1, 100000, 10));
    private final JSpinner   webhookShotMaxW  = new JSpinner(new SpinnerNumberModel(1280, 320, 3840, 64));
    private final JCheckBox  webhookShotSaveDisk = new JCheckBox("Save PNG to disk");
    private final JTextField webhookShotSaveDir  = new JTextField("screenshots", 20);
    private final JTextField webhookRareKeywords = new JTextField("", 30);
    private final java.util.Map<DiscordWebhook.Event, JCheckBox> webhookChecks =
            new java.util.EnumMap<>(DiscordWebhook.Event.class);
    private final java.util.Map<DiscordWebhook.Event, JCheckBox> webhookShotChecks =
            new java.util.EnumMap<>(DiscordWebhook.Event.class);
    private final JLabel     webhookStatusLabel = value("—");

    private JPanel buildWebhookTab() {
        JPanel p = panel(new BorderLayout(0, 8));

        JLabel header = new JLabel(
                "<html><body style='color:#bbb;font-size:11px;'>" +
                "Paste your Discord webhook URL and toggle which events to broadcast.<br>" +
                "Events are rate-limited to once every 30 seconds per type to avoid spam.</body></html>");
        header.setForeground(TEXT);
        header.setBorder(new EmptyBorder(0, 2, 6, 2));
        p.add(header, BorderLayout.NORTH);

        // URL row
        JPanel top = panel(new BorderLayout(6, 6));
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GREY, 1),
                        "Webhook URL", 0, 0, null, TEXT),
                new EmptyBorder(4, 6, 4, 6)));
        webhookUrlField.setText(bot.webhook.getWebhookUrl());
        webhookUrlField.setBackground(new Color(20, 22, 26));
        webhookUrlField.setForeground(TEXT);
        webhookUrlField.setCaretColor(TEXT);
        webhookUrlField.addActionListener(e -> bot.webhook.setWebhookUrl(webhookUrlField.getText()));
        webhookUrlField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                bot.webhook.setWebhookUrl(webhookUrlField.getText());
            }
        });
        top.add(webhookUrlField, BorderLayout.CENTER);

        JButton testBtn = button("Test Ping", BLUE);
        testBtn.addActionListener(e -> {
            // Sync HTTP would freeze the UI for up to 15s on a slow webhook —
            // run it on a daemon thread, hop back to EDT to update the label.
            bot.webhook.setWebhookUrl(webhookUrlField.getText());
            webhookStatusLabel.setText("Posting...");
            testBtn.setEnabled(false);
            Thread t = new Thread(() -> {
                String result;
                try { result = bot.webhook.testPing(); }
                catch (Throwable ex) { result = "Error: " + ex.getMessage(); }
                final String r = result;
                SwingUtilities.invokeLater(() -> {
                    webhookStatusLabel.setText(r);
                    testBtn.setEnabled(true);
                    appendLog("[webhook] test: " + r);
                });
            }, "webhook-test-ping");
            t.setDaemon(true);
            t.start();
        });
        JButton testShotBtn = button("Test + Screenshot", BLUE);
        testShotBtn.addActionListener(e -> {
            bot.webhook.setWebhookUrl(webhookUrlField.getText());
            webhookStatusLabel.setText("Capturing + posting...");
            testShotBtn.setEnabled(false);
            Thread t = new Thread(() -> {
                String result;
                try { result = bot.webhook.testPingWithScreenshot(); }
                catch (Throwable ex) { result = "Error: " + ex.getMessage(); }
                final String r = result;
                SwingUtilities.invokeLater(() -> {
                    webhookStatusLabel.setText(r);
                    testShotBtn.setEnabled(true);
                    appendLog("[webhook] test+png: " + r);
                });
            }, "webhook-test-shot");
            t.setDaemon(true);
            t.start();
        });
        JButton saveBtn = button("Save", GREEN);
        saveBtn.setToolTipText("Write the URL + enabled events + screenshot settings to "
                + DiscordWebhook.DEFAULT_CONFIG_PATH + " — auto-loaded on script restart.");
        saveBtn.addActionListener(e -> {
            // Make sure the current textbox value is pushed before saving.
            bot.webhook.setWebhookUrl(webhookUrlField.getText());
            try {
                bot.webhook.saveTo(DiscordWebhook.DEFAULT_CONFIG_PATH);
                webhookStatusLabel.setText("Saved to " + DiscordWebhook.DEFAULT_CONFIG_PATH);
                appendLog("[webhook] config saved to " + DiscordWebhook.DEFAULT_CONFIG_PATH);
            } catch (Exception ex) {
                webhookStatusLabel.setText("Save failed: " + ex.getMessage());
                appendLog("[webhook] save failed: " + ex.getMessage());
            }
        });
        JButton loadBtn = button("Load", BLUE);
        loadBtn.setToolTipText("Reload the config from " + DiscordWebhook.DEFAULT_CONFIG_PATH + ".");
        loadBtn.addActionListener(e -> {
            try {
                bot.webhook.loadFrom(DiscordWebhook.DEFAULT_CONFIG_PATH);
                refreshWebhookFromBot();
                webhookStatusLabel.setText("Loaded from " + DiscordWebhook.DEFAULT_CONFIG_PATH);
                appendLog("[webhook] config loaded from " + DiscordWebhook.DEFAULT_CONFIG_PATH);
            } catch (Exception ex) {
                webhookStatusLabel.setText("Load failed: " + ex.getMessage());
                appendLog("[webhook] load failed: " + ex.getMessage());
            }
        });

        JPanel rightBar = panel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        rightBar.add(testBtn);
        rightBar.add(testShotBtn);
        rightBar.add(saveBtn);
        rightBar.add(loadBtn);
        rightBar.add(label("Status:"));
        rightBar.add(webhookStatusLabel);
        top.add(rightBar, BorderLayout.SOUTH);

        // Events grid — two columns per row: [enable] [attach 📎]
        JPanel eventsBox = panel(new GridBagLayout());
        eventsBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GREY, 1),
                        "Events to broadcast   (📎 = attach screenshot)", 0, 0, null, TEXT),
                new EmptyBorder(4, 6, 4, 6)));
        GridBagConstraints ec = new GridBagConstraints();
        ec.insets = new Insets(1, 4, 1, 4);
        ec.anchor = GridBagConstraints.WEST;
        ec.fill   = GridBagConstraints.HORIZONTAL;

        DiscordWebhook.Event[] evs = DiscordWebhook.Event.values();
        int cols = 2;
        int rows = (evs.length + cols - 1) / cols;
        for (int i = 0; i < evs.length; i++) {
            DiscordWebhook.Event ev = evs[i];
            int col = i / rows;
            int row = i % rows;

            JCheckBox cb = new JCheckBox(ev.label);
            cb.setBackground(PANEL_BG); cb.setForeground(TEXT); cb.setFocusPainted(false);
            cb.setSelected(bot.webhook.isEnabled(ev));
            cb.addActionListener(e -> bot.webhook.setEnabled(ev, cb.isSelected()));
            webhookChecks.put(ev, cb);

            JCheckBox shot = new JCheckBox("📎");
            shot.setToolTipText("Attach a game-client screenshot when this event fires.");
            shot.setBackground(PANEL_BG); shot.setForeground(TEXT); shot.setFocusPainted(false);
            shot.setSelected(bot.webhook.isAttachScreenshot(ev));
            shot.addActionListener(e -> bot.webhook.setAttachScreenshot(ev, shot.isSelected()));
            webhookShotChecks.put(ev, shot);

            ec.gridx = col * 2;     ec.gridy = row; ec.weightx = 1.0;
            eventsBox.add(cb, ec);
            ec.gridx = col * 2 + 1; ec.gridy = row; ec.weightx = 0.0;
            eventsBox.add(shot, ec);
        }

        // Global screenshot settings + milestone
        JPanel globals = panel(new GridBagLayout());
        globals.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GREY, 1),
                        "Screenshot + misc settings", 0, 0, null, TEXT),
                new EmptyBorder(4, 6, 4, 6)));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 4, 2, 4);
        gc.anchor = GridBagConstraints.WEST;

        styleSpinner(webhookShotMaxW);
        webhookShotMaxW.setValue(bot.webhook.getScreenshotMaxWidth() == 0 ? 1280 : bot.webhook.getScreenshotMaxWidth());
        webhookShotMaxW.addChangeListener(e ->
                bot.webhook.setScreenshotMaxWidth((Integer) webhookShotMaxW.getValue()));
        gc.gridx = 0; gc.gridy = 0; globals.add(label("Max PNG width:"), gc);
        gc.gridx = 1;              globals.add(webhookShotMaxW,         gc);
        gc.gridx = 2;              globals.add(label("px  (downscaled bilinearly)"), gc);

        webhookShotSaveDisk.setBackground(PANEL_BG); webhookShotSaveDisk.setForeground(TEXT);
        webhookShotSaveDisk.setFocusPainted(false);
        webhookShotSaveDisk.setSelected(bot.webhook.isScreenshotSaveToDisk());
        webhookShotSaveDisk.addActionListener(e ->
                bot.webhook.setScreenshotSaveToDisk(webhookShotSaveDisk.isSelected()));
        gc.gridx = 0; gc.gridy = 1; globals.add(webhookShotSaveDisk, gc);
        webhookShotSaveDir.setBackground(new Color(20, 22, 26));
        webhookShotSaveDir.setForeground(TEXT);
        webhookShotSaveDir.setCaretColor(TEXT);
        webhookShotSaveDir.setText(bot.webhook.getScreenshotSaveDir());
        webhookShotSaveDir.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                bot.webhook.setScreenshotSaveDir(webhookShotSaveDir.getText());
            }
        });
        gc.gridx = 1; gc.gridwidth = 2; globals.add(webhookShotSaveDir, gc); gc.gridwidth = 1;

        styleSpinner(webhookMilestone);
        webhookMilestone.setValue(bot.webhook.getMilestoneEveryN());
        webhookMilestone.addChangeListener(e -> bot.webhook.setMilestoneEveryN((Integer)webhookMilestone.getValue()));
        gc.gridx = 0; gc.gridy = 2; globals.add(label("Milestone every:"), gc);
        gc.gridx = 1;              globals.add(webhookMilestone,         gc);
        gc.gridx = 2;              globals.add(label("ore"),             gc);

        webhookRareKeywords.setBackground(new Color(20, 22, 26));
        webhookRareKeywords.setForeground(TEXT);
        webhookRareKeywords.setCaretColor(TEXT);
        webhookRareKeywords.setText(bot.webhook.getRareDropKeywords());
        webhookRareKeywords.setToolTipText("Comma-separated keywords (case-insensitive). "
                + "A loot whose name contains any of these fires the RARE_DROP event.");
        webhookRareKeywords.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                bot.webhook.setRareDropKeywords(webhookRareKeywords.getText());
            }
        });
        gc.gridx = 0; gc.gridy = 3; globals.add(label("Rare keywords:"), gc);
        gc.gridx = 1; gc.gridwidth = 2; globals.add(webhookRareKeywords, gc); gc.gridwidth = 1;

        JPanel center = panel(new BorderLayout(0, 8));
        center.add(top,       BorderLayout.NORTH);
        center.add(eventsBox, BorderLayout.CENTER);
        center.add(globals,   BorderLayout.SOUTH);
        p.add(center, BorderLayout.CENTER);

        JLabel hint = new JLabel(
                "<html><body style='color:#bbb;font-size:11px;'>" +
                "Click <b>Test Ping</b> to verify the URL, or <b>Test + Screenshot</b> to verify the PNG pipeline.<br>" +
                "Screenshots capture the game viewport via Robot (on-screen bounds) — make sure the client is visible.<br>" +
                "Webhook posts happen on a daemon background thread — they never block the game loop.</body></html>");
        hint.setForeground(TEXT);
        p.add(hint, BorderLayout.SOUTH);

        return p;
    }

    /** Repopulate every Webhook-tab control from the bot's DiscordWebhook state.
     *  Called after Load, and once at panel construction so a config auto-loaded
     *  in onStart() is reflected in the UI the first time the panel opens. */
    private void refreshWebhookFromBot() {
        SwingUtilities.invokeLater(() -> {
            webhookUrlField.setText(bot.webhook.getWebhookUrl());
            webhookMilestone.setValue(bot.webhook.getMilestoneEveryN());
            int w = bot.webhook.getScreenshotMaxWidth();
            webhookShotMaxW.setValue(w == 0 ? 1280 : w);
            webhookShotSaveDisk.setSelected(bot.webhook.isScreenshotSaveToDisk());
            webhookShotSaveDir.setText(bot.webhook.getScreenshotSaveDir());
            webhookRareKeywords.setText(bot.webhook.getRareDropKeywords());
            for (DiscordWebhook.Event ev : DiscordWebhook.Event.values()) {
                JCheckBox cb = webhookChecks.get(ev);
                if (cb != null) cb.setSelected(bot.webhook.isEnabled(ev));
                JCheckBox shot = webhookShotChecks.get(ev);
                if (shot != null) shot.setSelected(bot.webhook.isAttachScreenshot(ev));
            }
        });
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
            refreshBreakStatus();
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
