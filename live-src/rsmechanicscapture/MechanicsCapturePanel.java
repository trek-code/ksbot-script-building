package rsmechanicscapture;

import net.runelite.api.coords.WorldPoint;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Main UI window for the Mechanics Capture script.
 *
 * <p>Layout:
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │  [Label: ___] [Radius: 15] [Server: Reason] [▶ Start] [■] │  ← Config bar
 * ├───────────────┬─────────────────────────────────────────────┤
 * │               │  Live Mechanics (unique IDs)                │
 * │  Tile Map     │  ┌─────────────────────────────────────┐   │
 * │  (grid)       │  │ Type | ID | Description | Tick      │   │
 * │               │  └─────────────────────────────────────┘   │
 * │               ├─────────────────────────────────────────────┤
 * │               │  Event Log (scrollable)                     │
 * ├───────────────┴─────────────────────────────────────────────┤
 * │  Tick:0  Time:00:00  NPCs:0  Anims:0  GFX:0  [Note:____][🔖]│  ← Stats + bookmark
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class MechanicsCapturePanel {

    // ── Dark theme colours ────────────────────────────────────────────────
    private static final Color BG_DARK   = new Color(22, 22, 22);
    private static final Color BG_MID    = new Color(30, 30, 30);
    private static final Color BG_PANEL  = new Color(38, 38, 38);
    private static final Color FG_TEXT   = new Color(200, 200, 200);
    private static final Color FG_DIM    = new Color(140, 140, 140);
    private static final Color FG_ACCENT = new Color(210, 190, 80);
    private static final Color FG_GOOD   = new Color(100, 220, 100);
    private static final Color FG_MONO   = new Color(170, 210, 170);

    // ── Row colours for the mechanics table (by type) ─────────────────────
    private static final Color ROW_NPC        = new Color(30, 55, 30);
    private static final Color ROW_NPC_ANIM   = new Color(60, 42, 20);
    private static final Color ROW_GFX        = new Color(55, 25, 65);
    private static final Color ROW_PLAYER_GFX = new Color(20, 42, 65);
    private static final Color ROW_DEFAULT    = new Color(35, 35, 35);

    // ── Config controls ───────────────────────────────────────────────────
    private JTextField         labelField;
    private JSpinner           radiusSpinner;
    private JComboBox<String>  serverCombo;

    // ── Action buttons ────────────────────────────────────────────────────
    private JButton    startBtn;
    private JButton    stopBtn;
    private JButton    bookmarkBtn;
    private JTextField bookmarkNoteField;

    // ── Stats labels ──────────────────────────────────────────────────────
    private JLabel tickLabel;
    private JLabel timeLabel;
    private JLabel npcLabel;
    private JLabel animLabel;
    private JLabel gfxLabel;

    // ── Mechanics table ───────────────────────────────────────────────────
    private DefaultTableModel mechModel;
    private JTable            mechTable;

    // ── Event log ─────────────────────────────────────────────────────────
    private JTextArea eventLog;

    // ── Tile map ──────────────────────────────────────────────────────────
    private TileMapCanvas tileMap;

    // ── Frame ─────────────────────────────────────────────────────────────
    private JFrame frame;

    // ── Bot back-reference ────────────────────────────────────────────────
    private final MechanicsCaptureBot bot;

    // ─────────────────────────────────────────────────────────────────────

    public MechanicsCapturePanel(MechanicsCaptureBot bot) {
        this.bot = bot;
    }

    /** Must be called on the Swing EDT. */
    public void buildAndShow() {
        frame = new JFrame("RS Mechanics Capture v1.0.0");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().setBackground(BG_DARK);
        frame.setLayout(new BorderLayout(4, 4));

        frame.add(buildConfigBar(), BorderLayout.NORTH);
        frame.add(buildCenter(),    BorderLayout.CENTER);
        frame.add(buildStatsBar(),  BorderLayout.SOUTH);

        frame.pack();
        frame.setMinimumSize(new Dimension(980, 620));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Panel builders
    // ═════════════════════════════════════════════════════════════════════

    private JPanel buildConfigBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        p.setBackground(BG_MID);
        p.setBorder(new EmptyBorder(2, 6, 2, 6));

        p.add(dimLabel("Label:"));
        labelField = darkTextField("boss", 12);
        p.add(labelField);

        p.add(dimLabel("Radius:"));
        radiusSpinner = new JSpinner(new SpinnerNumberModel(15, 1, 50, 1));
        styleSpinner(radiusSpinner);
        p.add(radiusSpinner);

        p.add(dimLabel("Server:"));
        serverCombo = new JComboBox<>(new String[]{"Reason", "Near-Reality"});
        styleCombo(serverCombo);
        p.add(serverCombo);

        p.add(Box.createHorizontalStrut(10));

        startBtn = colorButton("▶  Start", new Color(35, 100, 35));
        startBtn.addActionListener(e -> onStartClicked());
        p.add(startBtn);

        stopBtn = colorButton("■  Stop", new Color(110, 35, 35));
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> onStopClicked());
        p.add(stopBtn);

        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setBackground(BG_DARK);
        p.setBorder(new EmptyBorder(4, 6, 0, 6));

        // Left: tile map (square)
        tileMap = new TileMapCanvas(15);
        tileMap.setPreferredSize(new Dimension(375, 375));
        tileMap.setBorder(BorderFactory.createLineBorder(new Color(55, 55, 55), 1));
        p.add(tileMap, BorderLayout.WEST);

        // Right: mechanics table on top, event log on bottom
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildMechanicsPanel(), buildEventLogPanel());
        split.setDividerLocation(290);
        split.setResizeWeight(0.55);
        split.setBackground(BG_DARK);
        split.setBorder(null);
        p.add(split, BorderLayout.CENTER);

        return p;
    }

    private JPanel buildMechanicsPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBackground(BG_DARK);
        p.setBorder(new EmptyBorder(0, 4, 0, 0));

        JLabel hdr = new JLabel("  Live Mechanics  — unique IDs only, one row per ID per session");
        hdr.setForeground(FG_ACCENT);
        hdr.setFont(hdr.getFont().deriveFont(Font.BOLD, 12f));
        p.add(hdr, BorderLayout.NORTH);

        String[] cols = {"Type", "ID", "Description", "Tick #"};
        mechModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        mechTable = new JTable(mechModel);
        styleTable(mechTable);

        // Fixed widths for Type / ID / Tick columns
        mechTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        mechTable.getColumnModel().getColumn(0).setMaxWidth(100);
        mechTable.getColumnModel().getColumn(1).setPreferredWidth(65);
        mechTable.getColumnModel().getColumn(1).setMaxWidth(80);
        mechTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        mechTable.getColumnModel().getColumn(3).setMaxWidth(70);

        // Colour rows by type column
        mechTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                String type = mechModel.getValueAt(row, 0) != null
                        ? mechModel.getValueAt(row, 0).toString() : "";
                setBackground(sel ? new Color(60, 70, 110) : rowColor(type));
                setForeground(FG_TEXT);
                setBorder(noFocusBorder);
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(mechTable);
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 50), 1));
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    private JPanel buildEventLogPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBackground(BG_DARK);
        p.setBorder(new EmptyBorder(4, 4, 0, 0));

        JLabel hdr = new JLabel("  Event Log");
        hdr.setForeground(FG_ACCENT);
        hdr.setFont(hdr.getFont().deriveFont(Font.BOLD, 12f));
        p.add(hdr, BorderLayout.NORTH);

        eventLog = new JTextArea();
        eventLog.setEditable(false);
        eventLog.setBackground(new Color(18, 18, 18));
        eventLog.setForeground(FG_MONO);
        eventLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        eventLog.setLineWrap(true);
        eventLog.setWrapStyleWord(false);

        JScrollPane scroll = new JScrollPane(eventLog);
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(new Color(18, 18, 18));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 50), 1));
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    private JPanel buildStatsBar() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_MID);
        outer.setBorder(new EmptyBorder(4, 8, 4, 8));

        // Stats on the left
        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        stats.setBackground(BG_MID);
        tickLabel = monoLabel("Tick: 0");
        timeLabel = monoLabel("Time: 00:00:00");
        npcLabel  = monoLabel("NPCs: 0");
        animLabel = monoLabel("Anims: 0");
        gfxLabel  = monoLabel("GFX: 0");
        stats.add(tickLabel);
        stats.add(timeLabel);
        stats.add(new JSeparator(JSeparator.VERTICAL));
        stats.add(npcLabel);
        stats.add(animLabel);
        stats.add(gfxLabel);
        outer.add(stats, BorderLayout.WEST);

        // Bookmark on the right
        JPanel bk = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        bk.setBackground(BG_MID);
        bk.add(dimLabel("Bookmark note:"));
        bookmarkNoteField = darkTextField("", 20);
        bookmarkNoteField.setToolTipText("Optional note — leave blank for plain tick marker");
        bk.add(bookmarkNoteField);
        bookmarkBtn = colorButton("🔖 Bookmark", new Color(60, 60, 110));
        bookmarkBtn.setEnabled(false);
        bookmarkBtn.addActionListener(e -> {
            bot.addBookmark(bookmarkNoteField.getText().trim());
            bookmarkNoteField.setText("");
        });
        bk.add(bookmarkBtn);
        outer.add(bk, BorderLayout.EAST);

        return outer;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Button actions
    // ═════════════════════════════════════════════════════════════════════

    private void onStartClicked() {
        String lbl = labelField.getText().trim();
        if (lbl.isEmpty()) lbl = "boss";
        int    radius = (int) radiusSpinner.getValue();
        String server = (String) serverCombo.getSelectedItem();

        // Reset UI
        mechModel.setRowCount(0);
        eventLog.setText("");
        tileMap.reset(radius);

        // Toggle buttons
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        bookmarkBtn.setEnabled(true);

        bot.startCapture(lbl, radius, server);
    }

    private void onStopClicked() {
        bot.stopCapture();
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        bookmarkBtn.setEnabled(false);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Called from bot thread (must dispatch to EDT)
    // ═════════════════════════════════════════════════════════════════════

    /** Add a row to the live mechanics table. Thread-safe. */
    void addMechanicRow(MechanicsCaptureBot.MechanicEntry e) {
        SwingUtilities.invokeLater(() -> {
            mechModel.addRow(new Object[]{e.type, e.id, e.description, e.firstTick});
            // Auto-scroll to bottom
            int last = mechTable.getRowCount() - 1;
            if (last >= 0) mechTable.scrollRectToVisible(mechTable.getCellRect(last, 0, true));
        });
    }

    /** Append a line to the event log and auto-scroll. Thread-safe. */
    void appendEvent(String line) {
        SwingUtilities.invokeLater(() -> {
            eventLog.append(line + "\n");
            eventLog.setCaretPosition(eventLog.getDocument().getLength());
        });
    }

    /**
     * Push a new tile-map snapshot from the bot loop.
     * Stores state and schedules a repaint on the EDT.
     */
    void updateSnapshot(WorldPoint playerTile,
                        List<MechanicsCaptureBot.TileEntity> entities,
                        int radius) {
        tileMap.setSnapshot(playerTile, entities, radius);
        SwingUtilities.invokeLater(tileMap::repaint);
    }

    /** Refresh all stats labels. Thread-safe. */
    void updateStats(int tick, int npcs, int anims, int gfxCount, long elapsedMs) {
        SwingUtilities.invokeLater(() -> {
            tickLabel.setText("Tick: " + tick);
            timeLabel.setText("Time: " + MechanicsCaptureBot.formatDuration(elapsedMs));
            npcLabel .setText("NPCs: " + npcs);
            animLabel.setText("Anims: " + anims);
            gfxLabel .setText("GFX: " + gfxCount);
        });
    }

    /** Called when the script is fully stopped (via KSBot stop button). */
    void onScriptStop() {
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        bookmarkBtn.setEnabled(false);
        appendEvent("[SCRIPT STOPPED]");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Swing helpers
    // ═════════════════════════════════════════════════════════════════════

    private static Color rowColor(String type) {
        switch (type) {
            case "NPC":        return ROW_NPC;
            case "NPC_ANIM":   return ROW_NPC_ANIM;
            case "GFX":        return ROW_GFX;
            case "PLAYER_GFX": return ROW_PLAYER_GFX;
            default:           return ROW_DEFAULT;
        }
    }

    private static void styleTable(JTable t) {
        t.setBackground(BG_PANEL);
        t.setForeground(FG_TEXT);
        t.setGridColor(new Color(48, 48, 48));
        t.setSelectionBackground(new Color(55, 65, 110));
        t.setSelectionForeground(Color.WHITE);
        t.setRowHeight(22);
        t.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        t.getTableHeader().setBackground(new Color(40, 40, 40));
        t.getTableHeader().setForeground(FG_DIM);
        t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD));
        t.setShowVerticalLines(false);
    }

    private static JLabel dimLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(FG_DIM);
        return l;
    }

    private static JLabel monoLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(FG_MONO);
        l.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return l;
    }

    private static JTextField darkTextField(String text, int cols) {
        JTextField f = new JTextField(text, cols);
        f.setBackground(new Color(45, 45, 45));
        f.setForeground(FG_TEXT);
        f.setCaretColor(FG_TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70)),
                new EmptyBorder(2, 4, 2, 4)));
        return f;
    }

    private static void styleSpinner(JSpinner s) {
        s.getEditor().getComponent(0).setBackground(new Color(45, 45, 45));
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setForeground(FG_TEXT);
    }

    private static void styleCombo(JComboBox<?> c) {
        c.setBackground(new Color(45, 45, 45));
        c.setForeground(FG_TEXT);
    }

    private static JButton colorButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        return b;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Tile map canvas
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Custom-painted component that draws a top-down tile grid.
     *
     * <p>The player is always at the centre cell.
     * NPCs are orange, other players are blue, AOE graphics objects are purple.
     * Each cell is labelled with the first character of the entity's label when
     * cells are large enough (≥14 px wide).
     */
    static class TileMapCanvas extends JPanel {

        // Colours for each entity type
        private static final Color COL_PLAYER = new Color(80, 220, 80);
        private static final Color COL_NPC    = new Color(220, 110, 30);
        private static final Color COL_OTHER  = new Color(60, 140, 220);
        private static final Color COL_GFX    = new Color(190, 40, 200, 200);
        private static final Color COL_GRID   = new Color(48, 48, 48);
        private static final Color COL_TILE   = new Color(32, 32, 32);

        private volatile WorldPoint                          playerTile = null;
        private volatile List<MechanicsCaptureBot.TileEntity> snapshot   = new ArrayList<>();
        private volatile int radius;

        TileMapCanvas(int initialRadius) {
            this.radius = initialRadius;
            setBackground(new Color(20, 20, 20));
            setToolTipText("Tile map — player @ centre | NPC orange | Player blue | GFX purple");
        }

        /** Reset to cleared state (called when a new capture starts). */
        void reset(int newRadius) {
            this.radius     = newRadius;
            this.playerTile = null;
            this.snapshot   = new ArrayList<>();
            repaint();
        }

        /** Thread-safe snapshot update; caller must schedule repaint on EDT. */
        void setSnapshot(WorldPoint player,
                         List<MechanicsCaptureBot.TileEntity> entities,
                         int r) {
            this.playerTile = player;
            this.snapshot   = new ArrayList<>(entities);
            this.radius     = r;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth(), h = getHeight();
            int diameter = radius * 2 + 1;
            int cellW = Math.max(3, w / diameter);
            int cellH = Math.max(3, h / diameter);

            WorldPoint center   = playerTile;
            List<MechanicsCaptureBot.TileEntity> ents = snapshot;

            // ── Draw empty grid ───────────────────────────────────────────
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int px = (dx + radius) * cellW;
                    int py = (radius - dy) * cellH;   // y-axis flipped (N = top)
                    g2.setColor(COL_TILE);
                    g2.fillRect(px, py, cellW, cellH);
                    g2.setColor(COL_GRID);
                    g2.drawRect(px, py, cellW, cellH);
                }
            }

            // ── Waiting overlay ───────────────────────────────────────────
            if (center == null) {
                g2.setColor(FG_DIM);
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                FontMetrics fm = g2.getFontMetrics();
                String msg = "Waiting for player…";
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                return;
            }

            // ── Draw entities ─────────────────────────────────────────────
            boolean labelCells = (cellW >= 14);
            Font labelFont = new Font(Font.MONOSPACED, Font.BOLD, Math.min(cellW - 3, 10));
            g2.setFont(labelFont);

            for (MechanicsCaptureBot.TileEntity ent : ents) {
                int dx = ent.tile.getX() - center.getX();
                int dy = ent.tile.getY() - center.getY();
                if (Math.abs(dx) > radius || Math.abs(dy) > radius) continue;

                int px = (dx + radius) * cellW + 1;
                int py = (radius - dy) * cellH + 1;
                int cw = cellW - 2, ch = cellH - 2;

                Color fill;
                String tag;
                switch (ent.type) {
                    case NPC:    fill = COL_NPC;   tag = "N"; break;
                    case PLAYER: fill = COL_OTHER;  tag = "P"; break;
                    default:     fill = COL_GFX;    tag = "*"; break;
                }

                g2.setColor(fill);
                g2.fillRect(px, py, cw, ch);

                if (labelCells) {
                    g2.setColor(Color.WHITE);
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = px + (cw - fm.stringWidth(tag)) / 2;
                    int ty = py + (ch + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(tag, tx, ty);
                }
            }

            // ── Draw local player @ centre ────────────────────────────────
            int cx = radius * cellW + 1;
            int cy = radius * cellH + 1;
            g2.setColor(COL_PLAYER);
            g2.fillRect(cx, cy, cellW - 2, cellH - 2);
            if (labelCells) {
                g2.setColor(Color.BLACK);
                FontMetrics fm = g2.getFontMetrics();
                int tx = cx + (cellW - 2 - fm.stringWidth("@")) / 2;
                int ty = cy + (cellH - 2 + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString("@", tx, ty);
            }

            // ── Legend ────────────────────────────────────────────────────
            drawLegend(g2, w, h);
        }

        private void drawLegend(Graphics2D g2, int w, int h) {
            int lx = 4, ly = h - 58;
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));

            legendRow(g2, lx, ly,      COL_PLAYER, "@ You");
            legendRow(g2, lx, ly + 14, COL_NPC,    "N NPC");
            legendRow(g2, lx, ly + 28, COL_OTHER,  "P Other player");
            legendRow(g2, lx, ly + 42, COL_GFX,    "* AOE / Graphic");
        }

        private static void legendRow(Graphics2D g2, int x, int y, Color c, String text) {
            g2.setColor(c);
            g2.fillRect(x, y, 10, 10);
            g2.setColor(new Color(170, 170, 170));
            g2.drawString(text, x + 13, y + 9);
        }
    }
}
