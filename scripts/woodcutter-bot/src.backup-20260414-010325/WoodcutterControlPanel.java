package reason.woodcutter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class WoodcutterControlPanel extends JFrame {
    public interface Listener {
        void onStartRequested(WoodcutterSettings settings);
        void onPauseRequested();
        void onStopRequested();
        void onUpdateRequested(WoodcutterSettings settings);
        String onDebugSnapshotRequested();
    }

    private final Listener listener;

    private final JLabel statusValue = new JLabel("Idle");
    private final JLabel routeValue = new JLabel("-");
    private final JLabel bankValue = new JLabel("-");
    private final JLabel runtimeValue = new JLabel("00:00:00");
    private final JLabel logsCutValue = new JLabel("0");
    private final JLabel lastActionValue = new JLabel("waiting");
    private final JLabel levelValue = new JLabel("0");
    private final JLabel totalXpValue = new JLabel("0");
    private final JLabel gainedXpValue = new JLabel("0");
    private final JLabel xpHourValue = new JLabel("0");
    private final JLabel xpToNextValue = new JLabel("0");
    private final JLabel playerTileValue = new JLabel("-");

    private final JComboBox<WoodcuttingProfile.TreeType> treeTypeBox = new JComboBox<>(WoodcuttingProfile.TreeType.values());
    private final JComboBox<WoodcuttingProfile.RouteProfile> routeBox = new JComboBox<>();
    private final JComboBox<WoodcuttingProfile.Mode> modeBox = new JComboBox<>(WoodcuttingProfile.Mode.values());
    private final JComboBox<WoodcutterSettings.RuntimeLimit> runtimeLimitBox = new JComboBox<>(WoodcutterSettings.RuntimeLimit.values());

    private final JTextField stopAtLevelField = new JTextField();
    private final JCheckBox requireAxeBox = new JCheckBox("Require axe before start", true);
    private final JCheckBox closestTreeBox = new JCheckBox("Use closest valid tree", true);

    private JFrame debugConsoleFrame;
    private JTextArea debugConsoleArea;
    private Timer debugConsoleTimer;

    public WoodcutterControlPanel(WoodcutterSettings initialSettings, Listener listener) {
        super("Cutter of wood");
        this.listener = listener;
        configureWindow();
        buildUi();
        bindEvents();
        applySettings(initialSettings);
    }

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setPreferredSize(new Dimension(720, 640));
        getContentPane().setBackground(new Color(20, 24, 30));
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(new Color(20, 24, 30));
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Cutter of wood");
        title.setForeground(new Color(242, 245, 248));
        title.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 26));

        JLabel subtitle = new JLabel("Minimal route-based woodcutting core for Reason.");
        subtitle.setForeground(new Color(158, 170, 184));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(6));
        titleBlock.add(subtitle);

        JButton debugButton = button("Debug Console", new Color(62, 88, 118));
        debugButton.addActionListener(e -> showDebugConsole());
        JButton exportDebugButton = button("Export Debug", new Color(76, 102, 60));
        exportDebugButton.addActionListener(e -> exportDebugSnapshot());
        JPanel headerActions = new JPanel(new GridLayout(1, 2, 8, 0));
        headerActions.setOpaque(false);
        headerActions.add(debugButton);
        headerActions.add(exportDebugButton);
        header.add(titleBlock, BorderLayout.WEST);
        header.add(headerActions, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Main", buildMainTab());
        tabs.addTab("Stats", buildStatsTab());

        root.add(header, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(buildActionArea(), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
    }

    private JPanel buildMainTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(new Color(20, 24, 30));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));

        JPanel form = new JPanel(new GridLayout(0, 2, 14, 12));
        form.setOpaque(false);
        form.add(label("Status"));
        form.add(statusValue);
        form.add(label("Tree Type"));
        form.add(treeTypeBox);
        form.add(label("Route"));
        form.add(routeBox);
        form.add(label("Mode"));
        form.add(modeBox);
        form.add(label("Stop At Level"));
        form.add(stopAtLevelField);
        form.add(label("Max Runtime"));
        form.add(runtimeLimitBox);
        form.add(label("Selected Route"));
        form.add(routeValue);
        form.add(label("Bank"));
        form.add(bankValue);

        JPanel options = new JPanel();
        options.setOpaque(false);
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(58, 70, 84)),
                "Options",
                0,
                0,
                new Font("Segoe UI Semibold", Font.PLAIN, 12),
                new Color(225, 230, 236)
        ));

        styleCheckbox(requireAxeBox);
        styleCheckbox(closestTreeBox);
        options.add(requireAxeBox);
        options.add(Box.createVerticalStrut(6));
        options.add(closestTreeBox);

        panel.add(form, BorderLayout.CENTER);
        panel.add(options, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildStatsTab() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 14, 12));
        panel.setBackground(new Color(20, 24, 30));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 16, 16, 16));
        panel.add(label("Runtime"));
        panel.add(runtimeValue);
        panel.add(label("Logs Cut"));
        panel.add(logsCutValue);
        panel.add(label("Last Action"));
        panel.add(lastActionValue);
        panel.add(label("Woodcutting Level"));
        panel.add(levelValue);
        panel.add(label("Total XP"));
        panel.add(totalXpValue);
        panel.add(label("XP Gained"));
        panel.add(gainedXpValue);
        panel.add(label("XP / Hour"));
        panel.add(xpHourValue);
        panel.add(label("XP To Next Level"));
        panel.add(xpToNextValue);
        panel.add(label("Player Tile"));
        panel.add(playerTileValue);
        return panel;
    }

    private JPanel buildActionArea() {
        JButton startButton = button("Start Script", new Color(44, 123, 86));
        JButton pauseButton = button("Pause Script", new Color(123, 97, 40));
        JButton updateButton = button("Update Settings", new Color(52, 85, 122));
        JButton stopButton = button("Stop Script", new Color(130, 59, 59));

        startButton.addActionListener(e -> listener.onStartRequested(snapshot()));
        pauseButton.addActionListener(e -> listener.onPauseRequested());
        updateButton.addActionListener(e -> listener.onUpdateRequested(snapshot()));
        stopButton.addActionListener(e -> listener.onStopRequested());

        JPanel actions = new JPanel(new GridLayout(1, 4, 10, 0));
        actions.setOpaque(false);
        actions.add(startButton);
        actions.add(pauseButton);
        actions.add(updateButton);
        actions.add(stopButton);
        return actions;
    }

    private void bindEvents() {
        treeTypeBox.addActionListener(e -> refreshRoutesForSelectedTree());
        routeBox.addActionListener(e -> refreshRouteLabels());
    }

    private void refreshRoutesForSelectedTree() {
        WoodcuttingProfile.TreeType selectedTree = (WoodcuttingProfile.TreeType) treeTypeBox.getSelectedItem();
        routeBox.removeAllItems();
        if (selectedTree == null) {
            refreshRouteLabels();
            return;
        }
        for (WoodcuttingProfile.RouteProfile route : WoodcuttingProfile.routesFor(selectedTree)) {
            routeBox.addItem(route);
        }
        if (routeBox.getItemCount() > 0) {
            routeBox.setSelectedIndex(0);
        }
        refreshRouteLabels();
    }

    private void refreshRouteLabels() {
        WoodcuttingProfile.RouteProfile route = (WoodcuttingProfile.RouteProfile) routeBox.getSelectedItem();
        routeValue.setText(route == null ? "-" : route.label());
        bankValue.setText(route == null ? "-" : route.bankLabel());
    }

    private void showDebugConsole() {
        if (debugConsoleFrame == null) {
            debugConsoleArea = new JTextArea();
            debugConsoleArea.setEditable(false);
            debugConsoleArea.setFont(new Font("Consolas", Font.PLAIN, 12));
            debugConsoleArea.setBackground(new Color(16, 19, 24));
            debugConsoleArea.setForeground(new Color(233, 236, 239));

            JScrollPane scrollPane = new JScrollPane(debugConsoleArea);
            debugConsoleFrame = new JFrame("Woodcutter Debug Console");
            debugConsoleFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            debugConsoleFrame.setSize(new Dimension(920, 680));
            debugConsoleFrame.setLocationRelativeTo(this);
            debugConsoleFrame.getContentPane().setLayout(new BorderLayout());
            debugConsoleFrame.getContentPane().add(scrollPane, BorderLayout.CENTER);
            debugConsoleFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if (debugConsoleTimer != null) {
                        debugConsoleTimer.stop();
                    }
                }

                @Override
                public void windowDeactivated(WindowEvent e) {
                    refreshDebugConsole();
                }
            });

            debugConsoleTimer = new Timer(500, e -> refreshDebugConsole());
        }

        refreshDebugConsole();
        debugConsoleFrame.setVisible(true);
        debugConsoleTimer.start();
    }

    private void refreshDebugConsole() {
        if (debugConsoleArea == null) {
            return;
        }
        int caret = debugConsoleArea.getCaretPosition();
        String snapshot = listener.onDebugSnapshotRequested();
        debugConsoleArea.setText(snapshot);
        debugConsoleArea.setCaretPosition(Math.min(caret, snapshot.length()));
    }

    private void exportDebugSnapshot() {
        String snapshot = listener.onDebugSnapshotRequested();
        Path exportDir = Paths.get("D:\\Codex GPT\\RSPS\\KSBOT Script building\\handoff\\debug\\woodcutter");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path exportPath = exportDir.resolve("woodcutter-debug-" + timestamp + ".txt");

        try {
            Files.createDirectories(exportDir);
            Files.writeString(exportPath, snapshot, StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, exportPath.toString(), "Debug Exported", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void styleCheckbox(JCheckBox box) {
        box.setOpaque(false);
        box.setForeground(new Color(225, 230, 236));
        box.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(225, 230, 236));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return label;
    }

    private JButton button(String text, Color background) {
        JButton button = new JButton(text);
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        return button;
    }

    public WoodcutterSettings snapshot() {
        return new WoodcutterSettings(
                (WoodcuttingProfile.TreeType) treeTypeBox.getSelectedItem(),
                (WoodcuttingProfile.RouteProfile) routeBox.getSelectedItem(),
                (WoodcuttingProfile.Mode) modeBox.getSelectedItem(),
                requireAxeBox.isSelected(),
                closestTreeBox.isSelected(),
                parseStopAtLevel(),
                (WoodcutterSettings.RuntimeLimit) runtimeLimitBox.getSelectedItem()
        );
    }

    public void applySettings(WoodcutterSettings settings) {
        treeTypeBox.setSelectedItem(settings.treeType());
        refreshRoutesForSelectedTree();
        routeBox.setSelectedItem(settings.routeProfile());
        modeBox.setSelectedItem(settings.mode());
        requireAxeBox.setSelected(settings.requireAxe());
        closestTreeBox.setSelected(settings.useClosestTree());
        stopAtLevelField.setText(settings.hasStopAtLevelGoal() ? String.valueOf(settings.stopAtLevel()) : "");
        runtimeLimitBox.setSelectedItem(settings.runtimeLimit());
        routeValue.setText(settings.routeProfile().label());
        bankValue.setText(settings.routeProfile().bankLabel());
    }

    private int parseStopAtLevel() {
        String raw = stopAtLevelField.getText().trim();
        if (raw.isEmpty()) {
            return 0;
        }
        try {
            return Math.max(1, Math.min(99, Integer.parseInt(raw)));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public void updateSession(WoodcutterSessionSnapshot snapshot) {
        runOnEdt(() -> {
            statusValue.setText(snapshot.status());
            routeValue.setText(snapshot.routeLabel());
            bankValue.setText(snapshot.bankLabel());
            runtimeValue.setText(snapshot.runtimeText());
            logsCutValue.setText(String.valueOf(snapshot.logsCut()));
            lastActionValue.setText(snapshot.lastAction());
            levelValue.setText(String.valueOf(snapshot.currentLevel()));
            totalXpValue.setText(String.valueOf(snapshot.totalXp()));
            gainedXpValue.setText(String.valueOf(snapshot.gainedXp()));
            xpHourValue.setText(String.valueOf(snapshot.xpPerHour()));
            xpToNextValue.setText(String.valueOf(snapshot.xpToNextLevel()));
            playerTileValue.setText(snapshot.playerTile());
        });
    }

    public void showPanel() {
        runOnEdt(() -> setVisible(true));
    }

    public void closePanel() {
        runOnEdt(() -> {
            if (debugConsoleTimer != null) {
                debugConsoleTimer.stop();
            }
            setVisible(false);
        });
    }

    private void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
}
