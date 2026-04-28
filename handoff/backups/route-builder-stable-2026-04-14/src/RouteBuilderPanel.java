package route.builder;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * Route Builder UI panel — unified single-table layout.
 *
 *   ┌─ Top bar: Server | Bot Type | Route Name | Min Level | [New] [Load] [Save] [Export]
 *   │ ┌─ Tabs: Route Details | Path Chains | Log ─────────────────────────────┐
 *   │ │  Toolbar: [+Add] [Record My Tile] [Record Nearest] [Remove]           │
 *   │ │           [+ Name] [+ ID]                                              │
 *   │ │  Table (editable):  Name ▾ | ID ▾ | X | Y | P | Actions | Notes       │
 *   │ └───────────────────────────────────────────────────────────────────────┘
 *   │ ┌─ Live Capture bar (auto-updates every 300 ms) ────────────────────────┐
 *   │ │  My Tile:  X  Y  P   [Copy X][Copy Y][Copy XYP]  [+ Row: My Tile]     │
 *   │ │  Nearest:  Name ID X Y P   [Copy ID][Copy X][Copy Y][Copy XYP]        │
 *   │ │                             [Copy ID,X,Y,P]  [+ Row: Nearest]         │
 *   │ └───────────────────────────────────────────────────────────────────────┘
 */
public final class RouteBuilderPanel extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color BG     = new Color(18, 23, 30);
    private static final Color CARD   = new Color(24, 31, 41);
    private static final Color BORDER = new Color(42, 53, 66);
    private static final Color FG     = new Color(244, 247, 250);
    private static final Color DIM    = new Color(153, 166, 181);
    private static final Color GREEN  = new Color(53, 130, 87);
    private static final Color BLUE   = new Color(39, 100, 145);
    private static final Color RED    = new Color(160, 50, 50);
    private static final Color YELLOW = new Color(175, 135, 43);
    private static final Color PURPLE = new Color(100, 55, 150);
    private static final Color TEAL   = new Color(30, 110, 120);

    private final RouteBuilderBot bot;

    // ── Top bar fields ────────────────────────────────────────────────────────
    private final JTextField routeNameField = styledField(22);
    private final JComboBox<String> serverBox  = new JComboBox<>(new String[]{"Reason", "Near-Reality"});
    private final JComboBox<String> botTypeBox = new JComboBox<>(new String[]{"Woodcutting", "Fishing", "Mining", "Agility", "Thieving"});
    private final JTextField levelField = styledField(4);

    // ── Unified tile table (replaces resource + bank sections) ───────────────
    private static final int COL_NAME = 0, COL_ID = 1, COL_X = 2, COL_Y = 3,
                             COL_PLANE = 4, COL_ACTIONS = 5, COL_NOTES = 6;
    private final DefaultTableModel tileModel = tileTableModel();
    private final JTable tileTable = styledTable(tileModel);

    // Dropdown models for Name + ID columns (editable combos, growable)
    private final DefaultComboBoxModel<String> namesModel = new DefaultComboBoxModel<>();
    private final DefaultComboBoxModel<String> idsModel   = new DefaultComboBoxModel<>();
    private final DefaultTableModel recentRoutesModel = recentRoutesTableModel();
    private final JTable recentRoutesTable = styledTable(recentRoutesModel);
    private final java.util.List<Path> recentRoutePaths = new ArrayList<>();

    // ── Path tab ──────────────────────────────────────────────────────────────
    private final JLabel bankPathLabel     = dim("0 waypoints");
    private final JLabel resourcePathLabel = dim("0 waypoints");

    // ── Live capture bar ──────────────────────────────────────────────────────
    private final JLabel liveXLabel      = liveVal("---");
    private final JLabel liveYLabel      = liveVal("---");
    private final JLabel livePLabel      = liveVal("-");
    private final JLabel nearNameLabel   = liveVal("---");
    private final JLabel nearIdLabel     = liveVal("---");
    private final JLabel nearXLabel      = liveVal("---");
    private final JLabel nearYLabel      = liveVal("---");
    private final JLabel nearPLabel      = liveVal("-");
    private final JLabel pathStatusLabel = dim("Idle");
    private final JLabel currentRouteLabel = dim("(no route loaded)");

    // ── Log ───────────────────────────────────────────────────────────────────
    private final JTextArea logArea = new JTextArea(3, 40);

    // ── State ─────────────────────────────────────────────────────────────────
    private JTabbedPane tabs;
    private volatile Path profilesRoot;

    public RouteBuilderPanel(RouteBuilderBot bot) {
        super("Route Builder");
        this.bot = bot;
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setPreferredSize(new Dimension(1200, 780));
        getContentPane().setBackground(BG);

        seedDefaultNames();

        buildLayout();
        pack();
        setLocationRelativeTo(null);
    }

    /** Called by the bot once it knows the profiles root. */
    public void setProfilesRoot(Path root) {
        this.profilesRoot = root;
        loadDropdownPrefs();
        refreshRecentRoutesTable();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        root.add(buildTopBar(),          BorderLayout.NORTH);
        root.add(buildEditorPanel(),     BorderLayout.CENTER);
        root.add(buildLiveCaptureBar(),  BorderLayout.SOUTH);
        setContentPane(root);
    }

    // ── Top bar ───────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JLabel title = new JLabel("Route Builder");
        title.setForeground(FG);
        title.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 20));

        styleBox(serverBox);
        styleBox(botTypeBox);

        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        fields.setOpaque(false);
        fields.add(labeled("Server:",     serverBox));
        fields.add(labeled("Bot Type:",   botTypeBox));
        fields.add(labeled("Route Name:", routeNameField));
        fields.add(labeled("Min Level:",  levelField));

        JButton newBtn    = bigBtn("New",    BLUE);
        JButton loadBtn   = bigBtn("Load",   TEAL);
        JButton saveBtn   = bigBtn("Save",   GREEN);
        JButton exportBtn = bigBtn("Export", PURPLE);

        newBtn.addActionListener(e -> bot.onNewRoute());
        loadBtn.addActionListener(e -> showLoadDialog());
        saveBtn.addActionListener(e -> bot.onSave());
        exportBtn.addActionListener(e -> bot.onExport());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(newBtn);
        actions.add(loadBtn);
        actions.add(saveBtn);
        actions.add(exportBtn);

        bar.add(title,   BorderLayout.WEST);
        bar.add(fields,  BorderLayout.CENTER);
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    // ── Editor panel (tabs) ───────────────────────────────────────────────────

    private JTabbedPane buildEditorPanel() {
        tabs = new JTabbedPane();
        tabs.setBackground(BG);
        tabs.setForeground(FG);
        tabs.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));

        tabs.addTab("Route Details", buildUnifiedTab());
        tabs.addTab("Path Chains",   buildPathTab());
        tabs.addTab("Log",           buildLogTab());
        return tabs;
    }

    // ── Unified tile table tab ────────────────────────────────────────────────

    private JPanel buildUnifiedTab() {
        JPanel panel = darkCard();
        panel.setLayout(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel hint = dim("All route tiles on one table. Use the Name column to tag a row "
                + "(e.g. \"oak\", \"bank booth\", \"stand tile\", \"inside tile\", \"return anchor\", \"waypoint\").");

        // ── Toolbar ──────────────────────────────────────────────────────────
        JButton addBlank     = btn("+ Add Row",        BLUE);
        JButton addMyTile    = btn("Record My Tile",   GREEN);
        JButton addNearest   = btn("Record Nearest",   TEAL);
        JButton removeSel    = btn("Remove Selected",  RED);
        JButton addName      = btn("+ Name",           YELLOW);
        JButton addId        = btn("+ ID",             YELLOW);

        addBlank.addActionListener(e   -> addBlankRow());
        addMyTile.addActionListener(e  -> bot.onAddMyTileRow());
        addNearest.addActionListener(e -> bot.onAddNearestRow());
        removeSel.addActionListener(e  -> deleteSelectedRows(tileTable, tileModel));
        addName.addActionListener(e    -> promptAddName());
        addId.addActionListener(e      -> promptAddId());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        toolbar.setOpaque(false);
        toolbar.add(addBlank);
        toolbar.add(addMyTile);
        toolbar.add(addNearest);
        toolbar.add(removeSel);
        toolbar.add(Box.createHorizontalStrut(18));
        toolbar.add(dim("Dropdowns:"));
        toolbar.add(addName);
        toolbar.add(addId);

        // ── Dropdown cell editors for Name + ID ──────────────────────────────
        installDropdownEditors();

        JScrollPane scroll = styledScroll(tileTable);
        JPanel recentCard = buildRecentRoutesCard();

        panel.add(hint,    BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(toolbar, BorderLayout.NORTH);
        center.add(scroll,  BorderLayout.CENTER);
        center.add(recentCard, BorderLayout.EAST);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRecentRoutesCard() {
        JPanel card = darkCard();
        card.setLayout(new BorderLayout(0, 8));
        card.setPreferredSize(new Dimension(360, 0));
        card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = sectionLabel("Recent Routes");
        JButton refresh = btn("Refresh", BLUE);
        JButton loadSelected = btn("Load Selected", TEAL);
        refresh.addActionListener(e -> refreshRecentRoutesTable());
        loadSelected.addActionListener(e -> loadSelectedRecentRoute());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(refresh);
        actions.add(loadSelected);
        top.add(actions, BorderLayout.EAST);

        recentRoutesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recentRoutesTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        recentRoutesTable.getColumnModel().getColumn(1).setPreferredWidth(75);
        recentRoutesTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        recentRoutesTable.getColumnModel().getColumn(3).setPreferredWidth(55);
        recentRoutesTable.getColumnModel().getColumn(4).setPreferredWidth(130);
        recentRoutesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    loadSelectedRecentRoute();
                }
            }
        });

        card.add(top, BorderLayout.NORTH);
        card.add(styledScroll(recentRoutesTable), BorderLayout.CENTER);
        return card;
    }

    private void installDropdownEditors() {
        JComboBox<String> nameCombo = new JComboBox<>(namesModel);
        nameCombo.setEditable(true);
        tileTable.getColumnModel().getColumn(COL_NAME)
                .setCellEditor(new DefaultCellEditor(nameCombo));

        JComboBox<String> idCombo = new JComboBox<>(idsModel);
        idCombo.setEditable(true);
        tileTable.getColumnModel().getColumn(COL_ID)
                .setCellEditor(new DefaultCellEditor(idCombo));
    }

    // ── Path tab (unchanged behaviour) ───────────────────────────────────────

    private JPanel buildPathTab() {
        JPanel panel = darkCard();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        panel.add(wrap(dim("Walk the path in-game while recording — waypoints are auto-captured every "
                + RouteBuilderBot.PATH_STEP_DISTANCE + " tiles.")));
        panel.add(Box.createVerticalStrut(14));

        panel.add(sectionLabel("Path: Resource \u2192 Bank"));
        panel.add(Box.createVerticalStrut(6));
        JPanel toBankRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toBankRow.setOpaque(false);
        JButton startToBank = btn("Start Recording", GREEN);
        JButton stopToBank  = btn("Stop",            RED);
        JButton clearToBank = btn("Clear",           YELLOW);
        toBankRow.add(startToBank);
        toBankRow.add(stopToBank);
        toBankRow.add(clearToBank);
        toBankRow.add(Box.createHorizontalStrut(12));
        toBankRow.add(dim("Waypoints:"));
        toBankRow.add(bankPathLabel);
        startToBank.addActionListener(e -> { bot.onStartPathRecording(true);  pathStatusLabel.setText("Recording: Resource \u2192 Bank"); });
        stopToBank.addActionListener(e  -> { bot.onStopPathRecording();       pathStatusLabel.setText("Idle"); });
        clearToBank.addActionListener(e -> bot.onClearPath(true));
        panel.add(toBankRow);
        panel.add(Box.createVerticalStrut(14));

        panel.add(sectionLabel("Path: Bank \u2192 Resource"));
        panel.add(Box.createVerticalStrut(6));
        JPanel toResRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toResRow.setOpaque(false);
        JButton startToRes = btn("Start Recording", GREEN);
        JButton stopToRes  = btn("Stop",            RED);
        JButton clearToRes = btn("Clear",           YELLOW);
        toResRow.add(startToRes);
        toResRow.add(stopToRes);
        toResRow.add(clearToRes);
        toResRow.add(Box.createHorizontalStrut(12));
        toResRow.add(dim("Waypoints:"));
        toResRow.add(resourcePathLabel);
        startToRes.addActionListener(e -> { bot.onStartPathRecording(false); pathStatusLabel.setText("Recording: Bank \u2192 Resource"); });
        stopToRes.addActionListener(e  -> { bot.onStopPathRecording();       pathStatusLabel.setText("Idle"); });
        clearToRes.addActionListener(e -> bot.onClearPath(false));
        panel.add(toResRow);
        panel.add(Box.createVerticalStrut(14));

        panel.add(sectionLabel("Recording Status:"));
        panel.add(wrap(pathStatusLabel));
        return panel;
    }

    // ── Log tab ───────────────────────────────────────────────────────────────

    private JPanel buildLogTab() {
        JPanel panel = darkCard();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        logArea.setBackground(new Color(12, 16, 22));
        logArea.setForeground(DIM);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return panel;
    }

    // ── Live capture bar ──────────────────────────────────────────────────────

    private JPanel buildLiveCaptureBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setBackground(new Color(12, 16, 22));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(55, 140, 90)),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));

        // My Tile row
        JPanel playerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        playerRow.setOpaque(false);
        playerRow.add(sectionLabel("My Tile:"));
        playerRow.add(labeled("X:", liveXLabel));
        playerRow.add(labeled("Y:", liveYLabel));
        playerRow.add(labeled("P:", livePLabel));
        playerRow.add(copyBtn("Copy X",    () -> copyToClipboard(liveXLabel.getText())));
        playerRow.add(copyBtn("Copy Y",    () -> copyToClipboard(liveYLabel.getText())));
        playerRow.add(copyBtn("Copy XYP",  () -> copyToClipboard(liveXLabel.getText() + ", " + liveYLabel.getText() + ", " + livePLabel.getText())));
        JButton addMyRow = btn("+ Row: My Tile", GREEN);
        addMyRow.addActionListener(e -> bot.onAddMyTileRow());
        playerRow.add(addMyRow);

        // Nearest row
        JPanel nearRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        nearRow.setOpaque(false);
        nearRow.add(sectionLabel("Nearest:"));
        nearRow.add(labeled("Name:", nearNameLabel));
        nearRow.add(labeled("ID:",   nearIdLabel));
        nearRow.add(labeled("X:",    nearXLabel));
        nearRow.add(labeled("Y:",    nearYLabel));
        nearRow.add(labeled("P:",    nearPLabel));
        JButton refreshNearest = btn("Refresh Nearest", BLUE);
        refreshNearest.addActionListener(e -> bot.onRefreshNearest());
        nearRow.add(refreshNearest);
        nearRow.add(copyBtn("Copy ID",       () -> copyToClipboard(nearIdLabel.getText())));
        nearRow.add(copyBtn("Copy X",        () -> copyToClipboard(nearXLabel.getText())));
        nearRow.add(copyBtn("Copy Y",        () -> copyToClipboard(nearYLabel.getText())));
        nearRow.add(copyBtn("Copy XYP",      () -> copyToClipboard(nearXLabel.getText() + ", " + nearYLabel.getText() + ", " + nearPLabel.getText())));
        nearRow.add(copyBtn("Copy ID,X,Y,P", () -> copyToClipboard(nearIdLabel.getText() + " @ " + nearXLabel.getText() + ", " + nearYLabel.getText() + ", " + nearPLabel.getText())));
        JButton addNearRow = btn("+ Row: Nearest", TEAL);
        addNearRow.addActionListener(e -> bot.onAddNearestRow());
        nearRow.add(addNearRow);

        // Status row (current route + path status)
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statusRow.setOpaque(false);
        statusRow.add(sectionLabel("Current route:"));
        statusRow.add(currentRouteLabel);
        statusRow.add(Box.createHorizontalStrut(18));
        statusRow.add(sectionLabel("Path recorder:"));
        statusRow.add(pathStatusLabel);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        rows.add(playerRow);
        rows.add(Box.createVerticalStrut(4));
        rows.add(nearRow);
        rows.add(Box.createVerticalStrut(4));
        rows.add(statusRow);

        bar.add(rows, BorderLayout.CENTER);
        return bar;
    }

    // ── Dropdown seeding + add/remove ────────────────────────────────────────

    private void seedDefaultNames() {
        String[] defaults = {
            // Woodcutting
            "tree", "oak", "willow", "maple", "yew", "magic", "teak", "mahogany",
            // Fishing
            "fishing spot", "rod fishing spot", "harpoon spot", "cage spot", "net spot",
            // Mining
            "copper rock", "tin rock", "iron rock", "coal rock", "silver rock",
            "gold rock", "mithril rock", "adamant rock", "rune rock", "clay rock",
            // Banks + bankers
            "bank booth", "bank chest", "banker",
            // Route navigation tags
            "stand tile", "inside tile", "front of booth", "return anchor", "waypoint",
            // Generic
            "door", "stairs", "ladder", "gate"
        };
        for (String n : defaults) namesModel.addElement(n);
    }

    private void promptAddName() {
        String s = JOptionPane.showInputDialog(this, "Add a new Name option to the dropdown:",
                "Add Name", JOptionPane.PLAIN_MESSAGE);
        if (s == null) return;
        s = s.trim();
        if (s.isEmpty()) return;
        if (containsIgnoreCase(namesModel, s)) {
            appendLog("Name already in dropdown: " + s);
            return;
        }
        namesModel.addElement(s);
        saveDropdownPrefs();
        appendLog("Added name: " + s);
    }

    private void promptAddId() {
        String s = JOptionPane.showInputDialog(this, "Add a new ID option to the dropdown:",
                "Add ID", JOptionPane.PLAIN_MESSAGE);
        if (s == null) return;
        s = s.trim();
        if (s.isEmpty()) return;
        if (containsIgnoreCase(idsModel, s)) {
            appendLog("ID already in dropdown: " + s);
            return;
        }
        idsModel.addElement(s);
        saveDropdownPrefs();
        appendLog("Added ID: " + s);
    }

    private static boolean containsIgnoreCase(DefaultComboBoxModel<String> m, String v) {
        for (int i = 0; i < m.getSize(); i++) {
            if (m.getElementAt(i).equalsIgnoreCase(v)) return true;
        }
        return false;
    }

    /** Remember any new name/ID the user typed directly into a cell. */
    private void absorbCellValueIntoDropdown(DefaultComboBoxModel<String> model, String val) {
        if (val == null) return;
        val = val.trim();
        if (val.isEmpty()) return;
        if (!containsIgnoreCase(model, val)) {
            model.addElement(val);
            saveDropdownPrefs();
        }
    }

    // ── Dropdown persistence ─────────────────────────────────────────────────

    private Path prefsFile() {
        if (profilesRoot == null) return null;
        return profilesRoot.resolve("_dropdowns.txt");
    }

    private void loadDropdownPrefs() {
        Path file = prefsFile();
        if (file == null || !Files.exists(file)) return;
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            String mode = "";
            for (String raw : content.split("\\R")) {
                String line = raw.trim();
                if (line.isEmpty()) continue;
                if (line.equalsIgnoreCase("[names]")) { mode = "names"; continue; }
                if (line.equalsIgnoreCase("[ids]"))   { mode = "ids";   continue; }
                if (mode.equals("names") && !containsIgnoreCase(namesModel, line)) namesModel.addElement(line);
                if (mode.equals("ids")   && !containsIgnoreCase(idsModel,   line)) idsModel.addElement(line);
            }
        } catch (Exception e) {
            appendLog("Failed to load dropdown prefs: " + e.getMessage());
        }
    }

    private void saveDropdownPrefs() {
        Path file = prefsFile();
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            StringBuilder sb = new StringBuilder();
            sb.append("[names]\n");
            for (int i = 0; i < namesModel.getSize(); i++) sb.append(namesModel.getElementAt(i)).append('\n');
            sb.append("[ids]\n");
            for (int i = 0; i < idsModel.getSize();   i++) sb.append(idsModel.getElementAt(i)).append('\n');
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            appendLog("Failed to save dropdown prefs: " + e.getMessage());
        }
    }

    // ── Load dialog (file picker) ────────────────────────────────────────────

    private void showLoadDialog() {
        JFileChooser chooser = new JFileChooser();
        if (profilesRoot != null) chooser.setCurrentDirectory(profilesRoot.toFile());
        chooser.setDialogTitle("Load Route Profile");
        chooser.setFileFilter(new FileNameExtensionFilter("Route Profile (*.json)", "json"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            bot.onLoadRoute(chooser.getSelectedFile().toPath());
        }
    }

    // ── Bot callbacks ─────────────────────────────────────────────────────────

    public void onLiveUpdate(RouteBuilderBot.LiveSnapshot snap) {
        SwingUtilities.invokeLater(() -> {
            if (snap.playerValid) {
                liveXLabel.setText(String.valueOf(snap.playerX));
                liveYLabel.setText(String.valueOf(snap.playerY));
                livePLabel.setText(String.valueOf(snap.playerPlane));
            } else {
                liveXLabel.setText("---"); liveYLabel.setText("---"); livePLabel.setText("-");
            }

            if (snap.nearestValid) {
                nearNameLabel.setText(truncate(snap.nearestName, 20));
                nearIdLabel.setText(String.valueOf(snap.nearestId));
                nearXLabel.setText(String.valueOf(snap.nearestX));
                nearYLabel.setText(String.valueOf(snap.nearestY));
                nearPLabel.setText(String.valueOf(snap.nearestPlane));
            } else {
                nearNameLabel.setText("---"); nearIdLabel.setText("---");
                nearXLabel.setText("---");    nearYLabel.setText("---"); nearPLabel.setText("-");
            }
        });
    }

    public void onPathWaypointAdded(int bankCount, int resCount) {
        SwingUtilities.invokeLater(() -> {
            bankPathLabel.setText(bankCount + " waypoints");
            resourcePathLabel.setText(resCount + " waypoints");
        });
    }

    /** Add a new row pre-filled from current player tile. Name is left blank for user to pick. */
    public void addRowFromMyTile(int x, int y, int plane) {
        SwingUtilities.invokeLater(() -> {
            tileModel.addRow(new Object[]{"waypoint", "", x, y, plane, "", ""});
            scrollToLastRow();
        });
    }

    /** Add a new row pre-filled from nearest object. */
    public void addRowFromNearest(String name, int id, int x, int y, int plane, String actions) {
        SwingUtilities.invokeLater(() -> {
            tileModel.addRow(new Object[]{name == null ? "" : name, id, x, y, plane, actions == null ? "" : actions, ""});
            absorbCellValueIntoDropdown(namesModel, name);
            if (id > 0) absorbCellValueIntoDropdown(idsModel, String.valueOf(id));
            scrollToLastRow();
        });
    }

    private void addBlankRow() {
        tileModel.addRow(new Object[]{"", "", 0, 0, 0, "", ""});
        scrollToLastRow();
    }

    private void scrollToLastRow() {
        int last = tileModel.getRowCount() - 1;
        if (last >= 0) {
            tileTable.setRowSelectionInterval(last, last);
            tileTable.scrollRectToVisible(tileTable.getCellRect(last, 0, true));
        }
    }

    /** Populate UI from a loaded session. */
    public void applySession(RouteBuilderSession session) {
        SwingUtilities.invokeLater(() -> {
            routeNameField.setText(session.routeName == null ? "" : session.routeName);
            if (session.serverName != null) serverBox.setSelectedItem(session.serverName);
            if (session.botType    != null) botTypeBox.setSelectedItem(session.botType);
            levelField.setText(session.levelRequired > 0 ? String.valueOf(session.levelRequired) : "");

            tileModel.setRowCount(0);

            // Resource tiles first
            for (RouteBuilderSession.CaptureTile t : session.resourceTiles) {
                addTileRowSilently(t, nonBlank(t.name, "tree"));
            }
            // Bank booth
            if (session.bankTile != null) {
                addTileRowSilently(session.bankTile, nonBlank(session.bankTile.name, "bank booth"));
            }
            // Stand tile
            if (session.bankStandTile != null) {
                addTileRowSilently(session.bankStandTile, "stand tile");
            }
            // Inside tiles
            for (RouteBuilderSession.CaptureTile t : session.bankInsideTiles) {
                addTileRowSilently(t, nonBlank(t.name, "inside tile"));
            }
            // Return anchor
            if (session.returnAnchor != null) {
                addTileRowSilently(session.returnAnchor, "return anchor");
            }

            bankPathLabel.setText(session.pathToBank.size() + " waypoints");
            resourcePathLabel.setText(session.pathToResource.size() + " waypoints");

            currentRouteLabel.setText(
                    (session.routeName == null || session.routeName.isBlank() ? "(unnamed)" : session.routeName)
                    + "  \u2014  " + (session.filePath == null ? "(not yet saved)" : session.filePath));
            refreshRecentRoutesTable();
        });
    }

    private void addTileRowSilently(RouteBuilderSession.CaptureTile t, String nameTag) {
        String name = (t.name == null || t.name.isBlank()) ? nameTag : t.name;
        tileModel.addRow(new Object[]{name, t.id > 0 ? t.id : "", t.x, t.y, t.plane, t.actions == null ? "" : t.actions, t.notes == null ? "" : t.notes});
    }

    /** Pull the form + table state back into the session before save. */
    public void applyToSession(RouteBuilderSession session) {
        session.routeName  = routeNameField.getText().trim();
        session.serverName = (String) serverBox.getSelectedItem();
        session.botType    = (String) botTypeBox.getSelectedItem();
        try { session.levelRequired = Integer.parseInt(levelField.getText().trim()); } catch (Exception ignored) {}

        // Rebuild buckets from unified table
        session.resourceTiles.clear();
        session.bankInsideTiles.clear();
        session.bankTile      = null;
        session.bankStandTile = null;
        session.returnAnchor  = null;

        for (int r = 0; r < tileModel.getRowCount(); r++) {
            String name    = str(tileModel, r, COL_NAME);
            int    id      = num(tileModel, r, COL_ID);
            int    x       = num(tileModel, r, COL_X);
            int    y       = num(tileModel, r, COL_Y);
            int    plane   = num(tileModel, r, COL_PLANE);
            String actions = str(tileModel, r, COL_ACTIONS);
            String notes   = str(tileModel, r, COL_NOTES);

            if (x == 0 && y == 0) continue;  // skip empty rows

            RouteBuilderSession.CaptureTile t =
                    new RouteBuilderSession.CaptureTile(name, id, x, y, plane, actions);
            t.notes = notes;

            // absorb typed-in values into dropdowns for next time
            absorbCellValueIntoDropdown(namesModel, name);
            if (id > 0) absorbCellValueIntoDropdown(idsModel, String.valueOf(id));

            String lower = name.toLowerCase();
            if (lower.contains("bank booth") || lower.contains("bank chest")) {
                if (session.bankTile == null) session.bankTile = t;
                else session.resourceTiles.add(t);
            } else if (lower.contains("stand tile")) {
                if (session.bankStandTile == null) session.bankStandTile = t;
                else session.bankInsideTiles.add(t);
            } else if (lower.contains("inside") || lower.contains("front of booth")) {
                session.bankInsideTiles.add(t);
            } else if (lower.contains("return anchor")) {
                if (session.returnAnchor == null) session.returnAnchor = t;
            } else {
                session.resourceTiles.add(t);
            }
        }
    }

    public void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            if (!logArea.getText().isEmpty()) logArea.append("\n");
            logArea.append(line);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void showPanel()  { SwingUtilities.invokeLater(() -> setVisible(true)); }
    public void closePanel() { SwingUtilities.invokeLater(() -> setVisible(false)); }

    // ── Table models ──────────────────────────────────────────────────────────

    private static DefaultTableModel tileTableModel() {
        return new DefaultTableModel(new String[]{"Name", "ID", "X", "Y", "Plane", "Actions", "Notes"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return true; }
        };
    }

    private static DefaultTableModel recentRoutesTableModel() {
        return new DefaultTableModel(new String[]{"Route", "Server", "Bot", "Level", "Updated"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private void refreshRecentRoutesTable() {
        SwingUtilities.invokeLater(() -> {
            recentRoutesModel.setRowCount(0);
            recentRoutePaths.clear();
        });

        if (profilesRoot == null || !Files.exists(profilesRoot)) {
            return;
        }

        java.util.List<Path> files = new ArrayList<>();
        try (java.util.stream.Stream<Path> walk = Files.walk(profilesRoot, 5)) {
            walk.filter(p -> p.getFileName().toString().equalsIgnoreCase("route-profile.json"))
                    .forEach(files::add);
        } catch (Exception e) {
            appendLog("Failed to scan recent routes: " + e.getMessage());
            return;
        }

        files.sort((a, b) -> {
            try {
                return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
            } catch (Exception ignored) {
                return a.compareTo(b);
            }
        });

        java.util.List<Object[]> rows = new ArrayList<>();
        java.util.List<Path> paths = new ArrayList<>();
        int limit = Math.min(files.size(), 12);
        for (int i = 0; i < limit; i++) {
            Path file = files.get(i);
            try {
                RouteBuilderSession tmp = new RouteBuilderSession();
                RouteBuilderSession.loadInto(tmp, Files.readString(file));
                String route = nonBlank(tmp.routeName, file.getParent().getFileName().toString());
                String server = nonBlank(tmp.serverName, "-");
                String bot = nonBlank(tmp.botType, "-");
                String level = tmp.levelRequired > 0 ? String.valueOf(tmp.levelRequired) : "-";
                String updated = Files.getLastModifiedTime(file).toString().replace('T', ' ');
                if (updated.length() > 16) updated = updated.substring(0, 16);
                rows.add(new Object[]{route, server, bot, level, updated});
                paths.add(file);
            } catch (Exception ignored) {
            }
        }

        SwingUtilities.invokeLater(() -> {
            recentRoutesModel.setRowCount(0);
            recentRoutePaths.clear();
            for (int i = 0; i < rows.size(); i++) {
                recentRoutesModel.addRow(rows.get(i));
                recentRoutePaths.add(paths.get(i));
            }
        });
    }

    private void loadSelectedRecentRoute() {
        int row = recentRoutesTable.getSelectedRow();
        if (row < 0 || row >= recentRoutePaths.size()) {
            appendLog("Select a recent route first.");
            return;
        }
        bot.onLoadRoute(recentRoutePaths.get(row));
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static void copyToClipboard(String text) {
        if (text == null || text.isBlank()) return;
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text.trim()), null);
    }

    private static void deleteSelectedRows(JTable table, DefaultTableModel model) {
        int[] rows = table.getSelectedRows();
        for (int i = rows.length - 1; i >= 0; i--) model.removeRow(rows[i]);
    }

    private static String str(DefaultTableModel m, int r, int c) {
        Object v = m.getValueAt(r, c); return v == null ? "" : v.toString().trim();
    }
    private static int num(DefaultTableModel m, int r, int c) {
        try { return Integer.parseInt(str(m, r, c)); } catch (Exception e) { return 0; }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
    }

    private static String nonBlank(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    // ── Widget factories ──────────────────────────────────────────────────────

    private static JTextField styledField(int cols) {
        JTextField f = new JTextField(cols);
        f.setBackground(new Color(14, 18, 25));
        f.setForeground(FG);
        f.setCaretColor(FG);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        f.setFont(new Font("Consolas", Font.PLAIN, 12));
        return f;
    }

    private static JTable styledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setBackground(new Color(14, 18, 25));
        t.setForeground(FG);
        t.setSelectionBackground(BLUE);
        t.setSelectionForeground(Color.WHITE);
        t.setGridColor(BORDER);
        t.setFont(new Font("Consolas", Font.PLAIN, 12));
        t.setRowHeight(24);
        t.getTableHeader().setBackground(CARD);
        t.getTableHeader().setForeground(DIM);
        t.getTableHeader().setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        return t;
    }

    private static JScrollPane styledScroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(new Color(14, 18, 25));
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sp;
    }

    private static JPanel darkCard() {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createLineBorder(BORDER));
        return p;
    }

    private static void styleBox(JComboBox<String> box) {
        box.setBackground(new Color(14, 18, 25));
        box.setForeground(FG);
        box.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    }

    private static JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
        b.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        return b;
    }

    private static JButton bigBtn(String text, Color bg) {
        JButton b = btn(text, bg);
        b.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return b;
    }

    private static JButton copyBtn(String text, Runnable action) {
        JButton b = btn(text, TEAL);
        b.addActionListener(e -> action.run());
        return b;
    }

    private static JLabel dim(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(DIM);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return l;
    }

    private static JLabel liveVal(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(120, 210, 140));
        l.setFont(new Font("Consolas", Font.BOLD, 12));
        return l;
    }

    private static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(FG);
        l.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        p.add(dim(label));
        p.add(field);
        return p;
    }

    private static JPanel wrap(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(c);
        return p;
    }
}
