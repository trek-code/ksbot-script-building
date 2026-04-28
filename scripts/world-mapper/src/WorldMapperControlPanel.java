package reason.mapper;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

final class WorldMapperControlPanel extends JFrame {
    interface Listener {
        void onCaptureCurrentTile();
        void onCaptureAnchor();
        void onCaptureNearestObject();
        void onCaptureNearestBankTarget();
        void onCaptureNearestTreeTarget();
        void onCaptureNearestNpc();
        void onCaptureNearestGroundItem();
        void onScanNearbyObjects();
        void onScanNearbyNpcs();
        void onScanNearbyGroundItems();
        void onScanBlockedTiles();
        void onQuickSurveyAndExport();
        void onToggleRouteRecorder();
        void onExportSession();
        void onCopyLastExportPath();
        void onClearSession();
    }

    private final Listener listener;

    private final JTextField sessionNameField = new JTextField("reason-route-capture");
    private final JTextField zoneNameField = new JTextField("unassigned-zone");
    private final JTextField anchorLabelField = new JTextField("named-anchor");
    private final JTextField waypointLabelField = new JTextField("manual-waypoint");
    private final JTextField routeRecorderLabelField = new JTextField("named-route");
    private final JTextField scanRadiusField = new JTextField("16");
    private final JTextField waypointDistanceField = new JTextField("4");
    private final JLabel anchorCountValue = valueLabel("0");
    private final JLabel waypointCountValue = valueLabel("0");
    private final JLabel objectCountValue = valueLabel("0");
    private final JLabel npcCountValue = valueLabel("0");
    private final JLabel groundItemCountValue = valueLabel("0");
    private final JLabel blockedTileCountValue = valueLabel("0");
    private final JLabel recorderStatusValue = valueLabel("Off");
    private final JLabel lastExportValue = valueLabel("-");
    private final JTextArea activityLog = new JTextArea();
    private final JButton recorderButton = button("Start Route Recorder", new Color(91, 74, 28));

    WorldMapperControlPanel(Listener listener) {
        super("Reason World Mapper");
        this.listener = listener;
        configureWindow();
        buildUi();
    }

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setPreferredSize(new Dimension(980, 760));
        getContentPane().setBackground(new Color(18, 23, 30));
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(new Color(18, 23, 30));
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel title = new JLabel("Reason World Mapper");
        title.setForeground(new Color(244, 247, 250));
        title.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 28));
        JLabel subtitle = new JLabel("Backend capture for route creation, nearest-target collection, and auto waypoint recording.");
        subtitle.setForeground(new Color(153, 166, 181));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitle);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(290, 620));
        left.add(sectionCard("Session", sessionPanel()));
        left.add(Box.createVerticalStrut(14));
        left.add(sectionCard("Recorder", recorderPanel()));
        left.add(Box.createVerticalStrut(14));
        left.add(sectionCard("Export", exportPanel()));

        JPanel right = new JPanel(new BorderLayout(0, 14));
        right.setOpaque(false);
        right.add(sectionCard("Capture Actions", actionPanel()), BorderLayout.NORTH);
        right.add(sectionCard("Live Summary", summaryPanel()), BorderLayout.CENTER);
        right.add(sectionCard("Activity Log", logPanel()), BorderLayout.SOUTH);

        root.add(header, BorderLayout.NORTH);
        root.add(left, BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);
        setContentPane(root);
        pack();
    }

    private JPanel sessionPanel() {
        JPanel panel = transparentGrid(0, 1, 8, 8);
        panel.add(label("Session Name"));
        panel.add(sessionNameField);
        panel.add(label("Zone Name"));
        panel.add(zoneNameField);
        panel.add(label("Anchor Label"));
        panel.add(anchorLabelField);
        panel.add(label("Manual Waypoint Label"));
        panel.add(waypointLabelField);
        panel.add(label("Route Recorder Label"));
        panel.add(routeRecorderLabelField);
        panel.add(label("Scan Radius"));
        panel.add(scanRadiusField);
        return panel;
    }

    private JPanel recorderPanel() {
        JPanel panel = transparentGrid(0, 1, 8, 8);
        panel.add(label("Waypoint Distance"));
        panel.add(waypointDistanceField);
        panel.add(label("Route Recorder"));
        panel.add(recorderStatusValue);
        recorderButton.addActionListener(e -> listener.onToggleRouteRecorder());
        panel.add(recorderButton);
        return panel;
    }

    private JPanel exportPanel() {
        JPanel panel = transparentGrid(0, 1, 8, 8);
        JButton exportButton = button("Export Session", new Color(41, 122, 85));
        JButton quickSurveyButton = button("Quick Survey + Export", new Color(145, 111, 43));
        JButton copyPathButton = button("Copy Last Export Path", new Color(39, 84, 121));
        JButton clearButton = button("Clear Session", new Color(120, 50, 50));

        exportButton.addActionListener(e -> listener.onExportSession());
        quickSurveyButton.addActionListener(e -> listener.onQuickSurveyAndExport());
        copyPathButton.addActionListener(e -> listener.onCopyLastExportPath());
        clearButton.addActionListener(e -> listener.onClearSession());

        panel.add(exportButton);
        panel.add(quickSurveyButton);
        panel.add(copyPathButton);
        panel.add(clearButton);
        panel.add(label("Last Export"));
        panel.add(lastExportValue);
        return panel;
    }

    private JPanel actionPanel() {
        JPanel panel = transparentGrid(4, 3, 10, 10);

        JButton currentTileButton = button("Capture Current Tile", new Color(39, 84, 121));
        JButton anchorButton = button("Capture Anchor", new Color(70, 97, 52));
        JButton objectButton = button("Capture Nearest Object", new Color(104, 79, 44));
        JButton bankTargetButton = button("Capture Nearest Bank Target", new Color(70, 97, 52));
        JButton treeTargetButton = button("Capture Nearest Tree Target", new Color(70, 97, 52));
        JButton npcButton = button("Capture Nearest NPC", new Color(104, 79, 44));
        JButton groundItemButton = button("Capture Nearest Ground Item", new Color(104, 79, 44));
        JButton nearbyObjectsButton = button("Scan Nearby Objects", new Color(39, 84, 121));
        JButton nearbyNpcButton = button("Scan Nearby NPCs", new Color(39, 84, 121));
        JButton nearbyItemsButton = button("Scan Nearby Ground Items", new Color(39, 84, 121));
        JButton blockedTilesButton = button("Scan Blocked Tiles", new Color(91, 74, 28));

        currentTileButton.addActionListener(e -> listener.onCaptureCurrentTile());
        anchorButton.addActionListener(e -> listener.onCaptureAnchor());
        objectButton.addActionListener(e -> listener.onCaptureNearestObject());
        bankTargetButton.addActionListener(e -> listener.onCaptureNearestBankTarget());
        treeTargetButton.addActionListener(e -> listener.onCaptureNearestTreeTarget());
        npcButton.addActionListener(e -> listener.onCaptureNearestNpc());
        groundItemButton.addActionListener(e -> listener.onCaptureNearestGroundItem());
        nearbyObjectsButton.addActionListener(e -> listener.onScanNearbyObjects());
        nearbyNpcButton.addActionListener(e -> listener.onScanNearbyNpcs());
        nearbyItemsButton.addActionListener(e -> listener.onScanNearbyGroundItems());
        blockedTilesButton.addActionListener(e -> listener.onScanBlockedTiles());

        panel.add(currentTileButton);
        panel.add(anchorButton);
        panel.add(objectButton);
        panel.add(bankTargetButton);
        panel.add(treeTargetButton);
        panel.add(npcButton);
        panel.add(groundItemButton);
        panel.add(nearbyObjectsButton);
        panel.add(nearbyNpcButton);
        panel.add(nearbyItemsButton);
        panel.add(blockedTilesButton);
        return panel;
    }

    private JPanel summaryPanel() {
        JPanel panel = transparentGrid(0, 2, 10, 8);
        panel.add(label("Anchors"));
        panel.add(anchorCountValue);
        panel.add(label("Waypoints"));
        panel.add(waypointCountValue);
        panel.add(label("Objects"));
        panel.add(objectCountValue);
        panel.add(label("NPCs"));
        panel.add(npcCountValue);
        panel.add(label("Ground Items"));
        panel.add(groundItemCountValue);
        panel.add(label("Blocked Tiles"));
        panel.add(blockedTileCountValue);
        return panel;
    }

    private JScrollPane logPanel() {
        activityLog.setEditable(false);
        activityLog.setWrapStyleWord(true);
        activityLog.setLineWrap(true);
        activityLog.setBackground(new Color(24, 31, 41));
        activityLog.setForeground(new Color(221, 229, 236));
        activityLog.setCaretColor(new Color(221, 229, 236));
        activityLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(activityLog);
        scrollPane.setPreferredSize(new Dimension(200, 180));
        return scrollPane;
    }

    private JPanel sectionCard(String title, java.awt.Component content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(new Color(24, 31, 41));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(42, 53, 66)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(244, 247, 250));
        titleLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 16));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(titleLabel, BorderLayout.WEST);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
        body.add(content, BorderLayout.CENTER);

        panel.add(titleRow, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel transparentGrid(int rows, int columns, int hGap, int vGap) {
        JPanel panel = new JPanel(new GridLayout(rows, columns, hGap, vGap));
        panel.setOpaque(false);
        return panel;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(221, 229, 236));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return label;
    }

    private static JLabel valueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(153, 166, 181));
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        return label;
    }

    private JButton button(String text, Color background) {
        JButton button = new JButton(text);
        button.setForeground(Color.WHITE);
        button.setBackground(background);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        return button;
    }

    String sessionName() {
        return sessionNameField.getText().trim();
    }

    String zoneName() {
        return zoneNameField.getText().trim();
    }

    String anchorLabel() {
        return anchorLabelField.getText().trim();
    }

    String waypointLabel() {
        return waypointLabelField.getText().trim();
    }

    String routeRecorderLabel() {
        return routeRecorderLabelField.getText().trim();
    }

    int scanRadius() {
        return parseInt(scanRadiusField.getText(), 16, 1, 32);
    }

    int waypointDistance() {
        return parseInt(waypointDistanceField.getText(), 4, 1, 12);
    }

    void updateSummary(int anchors, int waypoints, int objects, int npcs, int items, int blockedTiles) {
        runOnEdt(() -> {
            anchorCountValue.setText(String.valueOf(anchors));
            waypointCountValue.setText(String.valueOf(waypoints));
            objectCountValue.setText(String.valueOf(objects));
            npcCountValue.setText(String.valueOf(npcs));
            groundItemCountValue.setText(String.valueOf(items));
            blockedTileCountValue.setText(String.valueOf(blockedTiles));
        });
    }

    void updateRecorderState(boolean active) {
        runOnEdt(() -> {
            recorderStatusValue.setText(active ? "On" : "Off");
            recorderButton.setText(active ? "Stop Route Recorder" : "Start Route Recorder");
            recorderButton.setBackground(active ? new Color(120, 50, 50) : new Color(91, 74, 28));
        });
    }

    void updateLastExport(String path) {
        runOnEdt(() -> lastExportValue.setText(path == null || path.isBlank() ? "-" : path));
    }

    void appendLog(String line) {
        runOnEdt(() -> {
            if (!activityLog.getText().isEmpty()) {
                activityLog.append("\n");
            }
            activityLog.append(line);
            activityLog.setCaretPosition(activityLog.getDocument().getLength());
        });
    }

    void showPanel() {
        runOnEdt(() -> setVisible(true));
    }

    void closePanel() {
        runOnEdt(() -> setVisible(false));
    }

    private int parseInt(String raw, int fallback, int min, int max) {
        try {
            int parsed = Integer.parseInt(raw.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
}
