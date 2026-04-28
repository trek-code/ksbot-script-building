import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import java.util.stream.*;

// ─────────────────────────────────────────────────────────────────────────────
//  Route Profile Manager — standalone RSPS bot route editor
//  No KSBot API dependency — compiles with plain JDK 11
//  Reads/writes route-profile.json in the woodcutting-bot schema
// ─────────────────────────────────────────────────────────────────────────────

public class RouteProfileManager extends JFrame {

    // ── Palette ──────────────────────────────────────────────────────────────
    static final Color C_BG     = new Color(18, 23, 30);
    static final Color C_CARD   = new Color(24, 31, 41);
    static final Color C_BORDER = new Color(42, 53, 66);
    static final Color C_FG     = new Color(244, 247, 250);
    static final Color C_DIM    = new Color(153, 166, 181);
    static final Color C_GREEN  = new Color(53, 130, 87);
    static final Color C_BLUE   = new Color(39, 100, 145);
    static final Color C_RED    = new Color(160, 50, 50);
    static final Color C_YELLOW = new Color(175, 135, 43);
    static final Color C_PURPLE = new Color(110, 60, 155);
    static final Color C_ORANGE = new Color(180, 100, 30);
    static final Color C_TEAL   = new Color(30, 120, 130);

    // ── State ─────────────────────────────────────────────────────────────────
    private Path profilesRoot;
    private final List<RouteModel> routes = new ArrayList<>();
    private RouteModel selectedRoute;

    // ── Panels ───────────────────────────────────────────────────────────────
    private final RouteListPanel routeListPanel;
    private final RouteEditorPanel editorPanel;
    private final MiniMapPanel miniMapPanel;
    private final ValidationPanel validationPanel;
    private final QuickAddPanel quickAddPanel;
    private final JLabel rootPathLabel = dim("-");

    public RouteProfileManager() {
        super("Route Profile Manager — RSPS Bot Tools");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1380, 860));
        getContentPane().setBackground(C_BG);

        profilesRoot = findDefaultProfilesRoot();

        miniMapPanel    = new MiniMapPanel();
        validationPanel = new ValidationPanel();
        editorPanel     = new RouteEditorPanel(this::saveRoute, this::importMapper, this::editorChanged);
        routeListPanel  = new RouteListPanel(this::selectRoute, this::newRoute, this::deleteRoute);
        quickAddPanel   = new QuickAddPanel(this::onQuickAdd);

        buildLayout();
        buildMenuBar();
        pack();
        setLocationRelativeTo(null);
        loadAllRoutes();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new RouteProfileManager().setVisible(true));
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(C_BG);

        // Top bar
        root.add(buildTopBar(), BorderLayout.NORTH);

        // Main split: list | editor+map
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                card(routeListPanel, 240), buildCenterPanel());
        mainSplit.setDividerLocation(260);
        mainSplit.setDividerSize(4);
        mainSplit.setBackground(C_BG);
        mainSplit.setBorder(null);

        root.add(mainSplit, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(C_CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JLabel title = new JLabel("Route Profile Manager");
        title.setForeground(C_FG);
        title.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 18));

        JPanel pathRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pathRow.setOpaque(false);
        pathRow.add(dim("Profiles root:"));
        rootPathLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        pathRow.add(rootPathLabel);
        JButton changeBtn = smallButton("Change…", C_BLUE);
        changeBtn.addActionListener(e -> changeProfilesRoot());
        pathRow.add(changeBtn);

        bar.add(title, BorderLayout.WEST);
        bar.add(pathRow, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildCenterPanel() {
        // Top split: editor | mini-map
        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                editorPanel, card(miniMapPanel, 320));
        topSplit.setResizeWeight(0.70);
        topSplit.setDividerSize(4);
        topSplit.setBorder(null);
        topSplit.setBackground(C_BG);

        // Validation row under editor
        JSplitPane editorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                topSplit, validationPanel);
        editorSplit.setResizeWeight(0.72);
        editorSplit.setDividerSize(4);
        editorSplit.setBorder(null);
        editorSplit.setBackground(C_BG);

        // Quick Add panel at very bottom
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(C_BG);
        panel.add(editorSplit, BorderLayout.CENTER);
        panel.add(quickAddPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(C_CARD);

        JMenu file = new JMenu("File");
        file.setForeground(C_FG);
        addMenuItem(file, "New Route", "ctrl N", e -> newRoute());
        addMenuItem(file, "Save Route", "ctrl S", e -> saveSelectedRoute());
        file.addSeparator();
        addMenuItem(file, "Change Profiles Root…", null, e -> changeProfilesRoot());
        addMenuItem(file, "Reload All Routes", "F5", e -> loadAllRoutes());

        JMenu tools = new JMenu("Tools");
        tools.setForeground(C_FG);
        addMenuItem(tools, "Import from Mapper Export…", null, e -> importMapper(selectedRoute));
        addMenuItem(tools, "Validate All Routes", null, e -> validateAll());

        bar.add(file);
        bar.add(tools);
        setJMenuBar(bar);
    }

    private void addMenuItem(JMenu menu, String label, String accel, ActionListener action) {
        JMenuItem item = new JMenuItem(label);
        item.setBackground(C_CARD);
        item.setForeground(C_FG);
        if (accel != null) item.setAccelerator(KeyStroke.getKeyStroke(accel));
        item.addActionListener(action);
        menu.add(item);
    }

    // ── Route management ──────────────────────────────────────────────────────

    private void loadAllRoutes() {
        routes.clear();
        if (profilesRoot != null && Files.isDirectory(profilesRoot)) {
            try {
                Files.list(profilesRoot).filter(Files::isDirectory).sorted().forEach(dir -> {
                    Path file = dir.resolve("route-profile.json");
                    if (Files.isRegularFile(file)) {
                        RouteModel model = ProfileIO.read(file);
                        if (model != null) {
                            model.filePath = file;
                            routes.add(model);
                        }
                    }
                });
            } catch (IOException ignored) {}
        }
        rootPathLabel.setText(profilesRoot != null ? profilesRoot.toString() : "not set");
        routeListPanel.setRoutes(routes);
        if (!routes.isEmpty()) selectRoute(routes.get(0));
        else clearEditor();
    }

    private void selectRoute(RouteModel route) {
        selectedRoute = route;
        editorPanel.loadRoute(route);
        miniMapPanel.loadRoute(route);
        validationPanel.validate(route);
    }

    private void newRoute() {
        RouteModel blank = new RouteModel();
        blank.routeName = "New Route";
        blank.serverName = "Reason";
        blank.resourceType = "Oak Tree";
        blank.status = "draft";
        routes.add(0, blank);
        routeListPanel.setRoutes(routes);
        selectRoute(blank);
    }

    private void deleteRoute(RouteModel route) {
        if (route == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete route \"" + route.routeName + "\"?\n" +
                (route.filePath != null ? "This will delete the file: " + route.filePath : "Route has not been saved yet."),
                "Delete Route", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        if (route.filePath != null) {
            try { Files.deleteIfExists(route.filePath); } catch (IOException ignored) {}
        }
        routes.remove(route);
        routeListPanel.setRoutes(routes);
        if (!routes.isEmpty()) selectRoute(routes.get(0));
        else clearEditor();
    }

    private void saveRoute(RouteModel route) {
        if (route == null) return;
        // Pull latest data from editor into route model
        editorPanel.applyToModel(route);

        // Determine file path
        if (route.filePath == null) {
            String slug = slugify(route.routeName);
            Path dir = profilesRoot.resolve(slug);
            try { Files.createDirectories(dir); } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Could not create directory: " + e.getMessage());
                return;
            }
            route.filePath = dir.resolve("route-profile.json");
        }

        try {
            String json = ProfileIO.write(route, profilesRoot);
            Files.writeString(route.filePath, json, StandardCharsets.UTF_8);
            routeListPanel.setRoutes(routes); // refresh badges
            validationPanel.validate(route);
            miniMapPanel.loadRoute(route);
            JOptionPane.showMessageDialog(this, "Saved to:\n" + route.filePath,
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Save failed: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveSelectedRoute() {
        saveRoute(selectedRoute);
    }

    private void editorChanged() {
        if (selectedRoute == null) return;
        editorPanel.applyToModel(selectedRoute);
        miniMapPanel.loadRoute(selectedRoute);
        validationPanel.validate(selectedRoute);
    }

    private void importMapper(RouteModel target) {
        if (target == null) target = selectedRoute;
        if (target == null) {
            JOptionPane.showMessageDialog(this, "Select or create a route first.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Mapper Session Export JSON");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
        Path startDir = findMapperExportsDir();
        if (startDir != null) chooser.setCurrentDirectory(startDir.toFile());
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            String json = Files.readString(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8);
            MapperImporter.importInto(target, json);
            editorPanel.loadRoute(target);
            miniMapPanel.loadRoute(target);
            validationPanel.validate(target);
            JOptionPane.showMessageDialog(this,
                    "Imported " + target.treeTiles.size() + " tree tile(s) and " +
                    target.bankTiles.size() + " bank tile(s).",
                    "Import Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Import failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void validateAll() {
        long incomplete = routes.stream().filter(r -> !r.isComplete()).count();
        JOptionPane.showMessageDialog(this,
                routes.size() + " route(s) loaded. " + incomplete + " incomplete.",
                "Validation Summary", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onQuickAdd(TileModel tile, String targetList) {
        if (selectedRoute == null) {
            JOptionPane.showMessageDialog(this, "Select or create a route first.");
            return;
        }
        // Apply any pending editor changes first
        editorPanel.applyToModel(selectedRoute);

        if ("Bank Tile".equals(targetList)) {
            selectedRoute.bankTiles.add(tile);
        } else if ("Bank Inside Tile".equals(targetList)) {
            selectedRoute.bankInsideTiles.add(tile);
        } else if ("Bank Stand Tile".equals(targetList)) {
            selectedRoute.bankStandTile = tile;
        } else if ("Return Anchor".equals(targetList)) {
            selectedRoute.returnAnchor = tile;
        } else {
            selectedRoute.treeTiles.add(tile);
        }

        editorPanel.loadRoute(selectedRoute);
        miniMapPanel.loadRoute(selectedRoute);
        validationPanel.validate(selectedRoute);
    }

    private void clearEditor() {
        selectedRoute = null;
        editorPanel.loadRoute(null);
        miniMapPanel.loadRoute(null);
        validationPanel.validate(null);
    }

    private void changeProfilesRoot() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Route Profiles Root Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (profilesRoot != null) chooser.setCurrentDirectory(profilesRoot.toFile());
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        profilesRoot = chooser.getSelectedFile().toPath();
        loadAllRoutes();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    static Path findDefaultProfilesRoot() {
        Path primary  = Path.of("D:\\Claude Code\\OLDER GPT Agent code framework\\RSPS\\KSBOT Script building\\research\\route-profiles\\reason\\woodcutting-bot");
        Path fallback = Path.of("D:\\Codex GPT\\RSPS\\KSBOT Script building\\research\\route-profiles\\reason\\woodcutting-bot");
        if (Files.isDirectory(primary))  return primary;
        if (Files.isDirectory(fallback)) return fallback;
        return primary; // return primary even if it doesn't exist yet
    }

    static Path findMapperExportsDir() {
        Path primary  = Path.of("D:\\Claude Code\\OLDER GPT Agent code framework\\RSPS\\KSBOT Script building\\research\\mapping-sessions\\reason");
        Path fallback = Path.of("D:\\Codex GPT\\RSPS\\KSBOT Script building\\research\\mapping-sessions\\reason");
        if (Files.isDirectory(primary))  return primary;
        if (Files.isDirectory(fallback)) return fallback;
        return null;
    }

    static String slugify(String raw) {
        if (raw == null || raw.isBlank()) return "unnamed-route";
        return raw.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    static JLabel dim(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(C_DIM);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }

    static JButton smallButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
        b.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        return b;
    }

    static JPanel card(JComponent content, int preferredWidth) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(C_CARD);
        panel.setBorder(BorderFactory.createLineBorder(C_BORDER));
        if (preferredWidth > 0) panel.setPreferredSize(new Dimension(preferredWidth, 0));
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Data model
// ─────────────────────────────────────────────────────────────────────────────

class RouteModel {
    // Meta
    String routeName    = "";
    String serverName   = "Reason";
    String resourceType = "";
    String status       = "draft";
    int    levelRequirement = 0;

    // Tile lists
    List<TileModel> treeTiles        = new ArrayList<>();
    List<TileModel> bankTiles        = new ArrayList<>();
    List<TileModel> bankInsideTiles  = new ArrayList<>();
    TileModel       bankStandTile    = null;
    TileModel       returnAnchor     = null;

    // File reference
    Path filePath = null;

    boolean isComplete() {
        return !routeName.isBlank()
                && !resourceType.isBlank()
                && !treeTiles.isEmpty()
                && !bankTiles.isEmpty()
                && treeTiles.stream().allMatch(t -> t.id > 0)
                && bankTiles.stream().allMatch(t -> t.id > 0);
    }

    List<String> missingFields() {
        List<String> missing = new ArrayList<>();
        if (routeName.isBlank())      missing.add("Route Name");
        if (resourceType.isBlank())   missing.add("Resource Type");
        if (treeTiles.isEmpty())      missing.add("Tree tiles (none captured)");
        else if (treeTiles.stream().anyMatch(t -> t.id <= 0))
            missing.add("Tree tile ID(s) — some are 0 or missing");
        if (bankTiles.isEmpty())      missing.add("Bank tiles (none captured)");
        else if (bankTiles.stream().anyMatch(t -> t.id <= 0))
            missing.add("Bank tile ID(s) — some are 0 or missing");
        if (bankStandTile == null)    missing.add("Bank stand tile (where player stands at booth)");
        return missing;
    }
}

class TileModel {
    String label    = "";
    String category = "";
    int    x, y, plane;
    int    id;
    String actions  = "";
    String notes    = "";

    TileModel() {}

    TileModel(String label, String category, int x, int y, int plane, int id, String actions, String notes) {
        this.label    = label;
        this.category = category;
        this.x        = x;
        this.y        = y;
        this.plane    = plane;
        this.id       = id;
        this.actions  = actions;
        this.notes    = notes;
    }

    List<String> actionList() {
        if (actions == null || actions.isBlank()) return new ArrayList<>();
        return Arrays.stream(actions.split("[,|]")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return label + " [" + x + "," + y + "," + plane + "] id=" + id;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  JSON I/O
// ─────────────────────────────────────────────────────────────────────────────

class ProfileIO {

    static RouteModel read(Path file) {
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            return parse(json);
        } catch (Exception e) {
            return null;
        }
    }

    private static RouteModel parse(String json) {
        RouteModel m = new RouteModel();
        m.routeName    = str(json, "routeName");
        m.serverName   = str(json, "serverName");
        m.resourceType = str(json, "resourceType");
        m.status       = str(json, "status");
        String lvl     = str(json, "levelRequirement");
        try { m.levelRequirement = lvl.isBlank() ? 0 : Integer.parseInt(lvl.trim()); } catch (Exception ignored) {}
        if (m.routeName.isBlank()) return null;

        String resSection  = section(json, "resourceTargets");
        String bankSection = section(json, "bankTargets");
        String anchSection = section(json, "routeAnchors");

        m.treeTiles       = tiles(array(resSection, "tiles"), "resourceTile");
        m.bankTiles       = tiles(array(bankSection, "tiles"), "bankTarget");
        m.bankInsideTiles = tiles(array(anchSection, "bankInsideTiles"), "bankInside");

        List<TileModel> standList  = tiles(array(anchSection, "bankStandTile"), "bankStand");
        List<TileModel> returnList = tiles(array(anchSection, "returnTargetAnchor"), "returnTarget");
        m.bankStandTile = standList.isEmpty() ? null : standList.get(0);
        m.returnAnchor  = returnList.isEmpty() ? null : returnList.get(0);
        return m;
    }

    // ── Write ────────────────────────────────────────────────────────────────

    static String write(RouteModel m, Path profilesRoot) {
        String slug = RouteProfileManager.slugify(m.routeName);
        String routeRoot = profilesRoot != null
                ? profilesRoot.toString() + "\\" + slug
                : slug;
        String today = LocalDate.now().toString();

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        obj(sb, "meta", () -> {
            sf(sb, "serverName",   m.serverName,   true);
            sf(sb, "serverSlug",   RouteProfileManager.slugify(m.serverName), true);
            sf(sb, "botFamily",    "woodcutting-bot", true);
            sf(sb, "botFamilySlug","woodcutting-bot", true);
            sf(sb, "routeName",    m.routeName,    true);
            sf(sb, "routeSlug",    slug,           true);
            sf(sb, "activityType", "woodcutting",  true);
            sf(sb, "resourceType", m.resourceType, true);
            sf(sb, "createdDate",  today,          true);
            sf(sb, "status",       m.status,       false);
        });
        obj(sb, "requirements", () -> {
            sf(sb, "levelRequirement", m.levelRequirement > 0 ? String.valueOf(m.levelRequirement) : "", true);
            sb.append("    \"itemRequirements\": [],\n");
            sb.append("    \"accountRequirements\": [],\n");
            sb.append("    \"teleportRequirements\": [],\n");
            sb.append("    \"notes\": []\n");
        });
        obj(sb, "routeAnchors", () -> {
            sb.append("    \"teleportDestination\": [],\n");
            sb.append("    \"treeOrTargetAnchor\": [],\n");
            tileArray(sb, "returnTargetAnchor",
                    m.returnAnchor != null ? List.of(m.returnAnchor)
                            : (m.treeTiles.isEmpty() ? List.of() : List.of(m.treeTiles.get(0))), true);
            sb.append("    \"bankDoorTiles\": [],\n");
            tileArray(sb, "bankInsideTiles", m.bankInsideTiles, true);
            tileArray(sb, "bankStandTile",
                    m.bankStandTile != null ? List.of(m.bankStandTile) : List.of(), true);
            tileArray(sb, "bankObjectTile",
                    m.bankTiles.isEmpty() ? List.of() : List.of(m.bankTiles.get(0)), true);
            sb.append("    \"safeStandTiles\": []\n");
        });
        obj(sb, "resourceTargets", () -> {
            List<String> names = m.treeTiles.stream().map(t -> t.label).distinct().collect(Collectors.toList());
            List<Integer> ids  = m.treeTiles.stream().map(t -> t.id).distinct().filter(i -> i > 0).collect(Collectors.toList());
            strArr(sb, "names", names, true);
            intArr(sb, "objectIds", ids, true);
            sb.append("    \"npcIds\": [],\n");
            sb.append("    \"groundItemIds\": [],\n");
            tileArray(sb, "tiles", m.treeTiles, false);
        });
        obj(sb, "bankTargets", () -> {
            List<Integer> ids     = m.bankTiles.stream().map(t -> t.id).distinct().filter(i -> i > 0).collect(Collectors.toList());
            List<String>  actions = m.bankTiles.stream().flatMap(t -> t.actionList().stream()).distinct().collect(Collectors.toList());
            sf(sb, "type", "booth", true);
            intArr(sb, "objectIds", ids, true);
            sb.append("    \"npcIds\": [],\n");
            strArr(sb, "actions", actions, true);
            tileArray(sb, "tiles", m.bankTiles, false);
        });
        rawObj(sb, "pathChains",
                "    \"toResource\": [],\n    \"toBank\": [],\n    \"returnToResource\": [],\n    \"alternatePaths\": []\n");
        rawObj(sb, "interactionTargets",
                "    \"doors\": [],\n    \"gates\": [],\n    \"ladders\": [],\n    \"stairs\": [],\n    \"portals\": [],\n    \"obstacles\": []\n");
        rawObj(sb, "walkerHints",
                "    \"preferredScanRadius\": 16,\n    \"avoidTiles\": [],\n    \"blockedTileFlags\": [],\n    \"regionNotes\": [],\n    \"customServerWarnings\": []\n");
        rawObj(sb, "loot",
                "    \"defaultEnabled\": false,\n    \"suggestedItems\": [],\n    \"pickupStyle\": \"immediate\"\n");
        rawObj(sb, "sourceData",
                "    \"importedMapperExports\": [],\n    \"manualCaptureRows\": []\n");
        rawObj(sb, "testStatus",
                "    \"choppingOrInteraction\": \"\",\n    \"banking\": \"\",\n    \"powercutting\": \"\",\n    \"looting\": \"\",\n    \"knownIssues\": [],\n    \"nextFixes\": []\n");
        obj(sb, "paths", () -> {
            sf(sb, "routeRoot",     routeRoot,                   true);
            sf(sb, "capturesDir",   routeRoot + "\\captures",    true);
            sf(sb, "exportsDir",    routeRoot + "\\exports",     true);
            sf(sb, "screenshotsDir",routeRoot + "\\screenshots", false);
        });
        sb.append("}\n");
        return sb.toString();
    }

    // ── Write helpers ─────────────────────────────────────────────────────────

    private static void obj(StringBuilder sb, String key, Runnable body) {
        sb.append("  \"").append(key).append("\": {\n");
        body.run();
        sb.append("  },\n");
    }

    private static void rawObj(StringBuilder sb, String key, String body) {
        sb.append("  \"").append(key).append("\": {\n").append(body).append("  },\n");
    }

    private static void sf(StringBuilder sb, String key, String value, boolean comma) {
        sb.append("    \"").append(key).append("\": \"").append(esc(value)).append("\"");
        if (comma) sb.append(",");
        sb.append("\n");
    }

    private static void tileArray(StringBuilder sb, String key, List<TileModel> tiles, boolean comma) {
        sb.append("    \"").append(key).append("\": [");
        if (!tiles.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < tiles.size(); i++) {
                TileModel t = tiles.get(i);
                sb.append("      {\n");
                sf(sb, "category", t.category.isBlank() ? key : t.category, true);
                sf(sb, "label", t.label, true);
                sb.append("        \"x\": ").append(t.x).append(",\n");
                sb.append("        \"y\": ").append(t.y).append(",\n");
                sb.append("        \"plane\": ").append(t.plane).append(",\n");
                sb.append("        \"id\": ").append(t.id).append(",\n");
                strArr(sb, "actions", t.actionList(), true);
                sf(sb, "notes", t.notes, false);
                sb.append("      }");
                if (i < tiles.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("    ");
        }
        sb.append("]");
        if (comma) sb.append(",");
        sb.append("\n");
    }

    private static void strArr(StringBuilder sb, String key, List<String> values, boolean comma) {
        sb.append("    \"").append(key).append("\": [");
        if (!values.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < values.size(); i++) {
                sb.append("      \"").append(esc(values.get(i))).append("\"");
                if (i < values.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("    ");
        }
        sb.append("]");
        if (comma) sb.append(",");
        sb.append("\n");
    }

    private static void intArr(StringBuilder sb, String key, List<Integer> values, boolean comma) {
        sb.append("    \"").append(key).append("\": [");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(values.get(i));
        }
        sb.append("]");
        if (comma) sb.append(",");
        sb.append("\n");
    }

    private static String esc(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }

    // ── Parse helpers ─────────────────────────────────────────────────────────

    static String str(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    static int intVal(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        if (!m.find()) return 0;
        try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { return 0; }
    }

    static String section(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return "";
        int start = json.indexOf('{', idx);
        if (start < 0) return "";
        return balanced(json, start, '{', '}');
    }

    static String array(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return "";
        int start = json.indexOf('[', idx);
        if (start < 0) return "";
        return balanced(json, start, '[', ']');
    }

    static String balanced(String json, int start, char open, char close) {
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == open)       depth++;
            else if (json.charAt(i) == close) { depth--; if (depth == 0) return json.substring(start, i + 1); }
        }
        return "";
    }

    static List<TileModel> tiles(String arraySection, String defaultCategory) {
        List<TileModel> result = new ArrayList<>();
        if (arraySection == null || arraySection.isBlank()) return result;
        int i = 0;
        while (i < arraySection.length()) {
            int objStart = arraySection.indexOf('{', i);
            if (objStart < 0) break;
            String obj = balanced(arraySection, objStart, '{', '}');
            if (obj.isBlank()) break;
            int x = intVal(obj, "x"), y = intVal(obj, "y"), plane = intVal(obj, "plane"), id = intVal(obj, "id");
            String label = str(obj, "label"), cat = str(obj, "category"), notes = str(obj, "notes");
            List<String> actions = new ArrayList<>();
            Matcher am = Pattern.compile("\"([^\"]*)\"").matcher(array(obj, "actions"));
            while (am.find()) actions.add(am.group(1));
            if (x != 0 || y != 0) {
                TileModel t = new TileModel(label, cat.isBlank() ? defaultCategory : cat, x, y, plane, id,
                        String.join(", ", actions), notes);
                result.add(t);
            }
            i = objStart + obj.length();
        }
        return result;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Mapper session importer
// ─────────────────────────────────────────────────────────────────────────────

class MapperImporter {
    static void importInto(RouteModel target, String mapperJson) {
        String objArray = ProfileIO.array(mapperJson, "objects");
        if (objArray.isBlank()) return;
        List<TileModel> allObjects = ProfileIO.tiles(objArray, "");
        for (TileModel t : allObjects) {
            String nameLower = t.label.toLowerCase();
            String actLower  = t.actions.toLowerCase();
            if (isTree(nameLower, actLower)) {
                t.category = "resourceTile";
                if (target.treeTiles.stream().noneMatch(existing -> existing.x == t.x && existing.y == t.y)) {
                    target.treeTiles.add(t);
                }
            } else if (isBank(nameLower, actLower)) {
                t.category = "bankTarget";
                if (target.bankTiles.stream().noneMatch(existing -> existing.x == t.x && existing.y == t.y)) {
                    target.bankTiles.add(t);
                }
            }
        }
        // Also pull anchors labeled "inside"
        String anchArray = ProfileIO.array(mapperJson, "anchors");
        List<TileModel> anchors = ProfileIO.tiles(anchArray, "bankInside");
        for (TileModel a : anchors) {
            if (a.label.toLowerCase().contains("inside")) {
                a.category = "bankInside";
                target.bankInsideTiles.add(a);
            }
        }
    }

    private static boolean isTree(String name, String act) {
        return name.contains("tree") || name.contains("oak") || name.contains("willow")
                || name.contains("maple") || name.contains("yew") || name.contains("magic")
                || name.contains("redwood") || act.contains("chop");
    }

    private static boolean isBank(String name, String act) {
        return name.contains("bank") || name.contains("banker") || name.contains("booth")
                || name.contains("chest") || act.contains("bank") || act.contains("collect");
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Route list panel (left sidebar)
// ─────────────────────────────────────────────────────────────────────────────

class RouteListPanel extends JPanel {
    interface SelectListener { void onSelect(RouteModel route); }
    interface ActionListener  { void onAction(RouteModel route); }

    private final SelectListener onSelect;
    private final Runnable       onNew;
    private final ActionListener onDelete;
    private final DefaultListModel<RouteModel> model = new DefaultListModel<>();
    private final JList<RouteModel> list = new JList<>(model);

    RouteListPanel(SelectListener onSelect, Runnable onNew, ActionListener onDelete) {
        this.onSelect = onSelect;
        this.onNew    = onNew;
        this.onDelete = onDelete;
        setLayout(new BorderLayout(0, 8));
        setBackground(RouteProfileManager.C_CARD);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildUi();
    }

    private void buildUi() {
        JLabel title = new JLabel("Routes");
        title.setForeground(RouteProfileManager.C_FG);
        title.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttonRow.setOpaque(false);
        JButton newBtn = RouteProfileManager.smallButton("+ New", RouteProfileManager.C_GREEN);
        JButton delBtn = RouteProfileManager.smallButton("Delete", RouteProfileManager.C_RED);
        newBtn.addActionListener(e -> onNew.run());
        delBtn.addActionListener(e -> onDelete.onAction(list.getSelectedValue()));
        buttonRow.add(newBtn);
        buttonRow.add(delBtn);

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(buttonRow, BorderLayout.SOUTH);

        list.setBackground(RouteProfileManager.C_BG);
        list.setForeground(RouteProfileManager.C_FG);
        list.setSelectionBackground(new Color(39, 84, 121));
        list.setSelectionForeground(Color.WHITE);
        list.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        list.setCellRenderer(new RouteCellRenderer());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                onSelect.onSelect(list.getSelectedValue());
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(RouteProfileManager.C_BORDER));
        scroll.setBackground(RouteProfileManager.C_BG);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    void setRoutes(List<RouteModel> routes) {
        RouteModel selected = list.getSelectedValue();
        model.clear();
        for (RouteModel r : routes) model.addElement(r);
        if (selected != null && routes.contains(selected)) list.setSelectedValue(selected, true);
        else if (!routes.isEmpty()) list.setSelectedIndex(0);
    }

    private static class RouteCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof RouteModel) {
                RouteModel route = (RouteModel) value;
                String badge = route.isComplete() ? "\u25cf" : (route.treeTiles.isEmpty() || route.bankTiles.isEmpty() ? "\u25cb" : "\u25d1");
                Color  badgeColor = route.isComplete() ? RouteProfileManager.C_GREEN
                        : (route.treeTiles.isEmpty() || route.bankTiles.isEmpty() ? RouteProfileManager.C_RED : RouteProfileManager.C_YELLOW);
                setText("<html><font color='#" + hex(badgeColor) + "'>" + badge + "</font> "
                        + (route.routeName.isBlank() ? "<i>Unnamed</i>" : route.routeName)
                        + " <font color='#" + hex(RouteProfileManager.C_DIM) + "'><small>" + route.resourceType + "</small></font></html>");
            }
            setBackground(isSelected ? new Color(39, 84, 121) : RouteProfileManager.C_BG);
            setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            return this;
        }

        private String hex(Color c) {
            return String.format("%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Route editor panel (center — tabbed)
// ─────────────────────────────────────────────────────────────────────────────

class RouteEditorPanel extends JPanel {
    interface SaveListener   { void onSave(RouteModel route); }
    interface ImportListener { void onImport(RouteModel route); }
    interface ChangeListener { void onChange(); }

    private final SaveListener   onSave;
    private final ImportListener onImport;
    private final ChangeListener onChange;

    private RouteModel currentRoute;

    // Meta fields
    private final JTextField nameField      = field("Route name");
    private final JTextField serverField    = field("Reason");
    private final JTextField typeField      = field("Oak Tree");
    private final JTextField levelField     = field("0");
    private final JComboBox<String> statusBox = new JComboBox<>(new String[]{"draft", "active", "deprecated"});

    // Tile tables
    private final TileTablePanel treeTilePanel  = new TileTablePanel("resourceTile");
    private final TileTablePanel bankTilePanel  = new TileTablePanel("bankTarget");
    private final TileTablePanel insideTilePanel = new TileTablePanel("bankInside");

    // Stand / return tile
    private final JTextField standX = numField(), standY = numField(), standPlane = numField();
    private final JTextField retX = numField(), retY = numField(), retPlane = numField();
    private final JTextField retId = numField(), retLabel = field("closest tree from bank side");

    RouteEditorPanel(SaveListener onSave, ImportListener onImport, ChangeListener onChange) {
        this.onSave   = onSave;
        this.onImport = onImport;
        this.onChange = onChange;
        setLayout(new BorderLayout(0, 0));
        setBackground(RouteProfileManager.C_CARD);
        buildUi();
    }

    private void buildUi() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(RouteProfileManager.C_BG);
        tabs.setForeground(RouteProfileManager.C_FG);
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        tabs.addTab("Metadata", metaTab());
        tabs.addTab("Tree Tiles", treeTilePanel);
        tabs.addTab("Bank Tiles", bankTilePanel);
        tabs.addTab("Anchors", anchorsTab());

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        buttonBar.setBackground(RouteProfileManager.C_CARD);
        buttonBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, RouteProfileManager.C_BORDER));

        JButton importBtn = RouteProfileManager.smallButton("Import Mapper Export…", RouteProfileManager.C_YELLOW);
        JButton saveBtn   = RouteProfileManager.smallButton("Save Route", RouteProfileManager.C_GREEN);
        importBtn.addActionListener(e -> onImport.onImport(currentRoute));
        saveBtn.addActionListener(e -> onSave.onSave(currentRoute));
        buttonBar.add(importBtn);
        buttonBar.add(saveBtn);

        add(tabs, BorderLayout.CENTER);
        add(buttonBar, BorderLayout.SOUTH);
    }

    private JPanel metaTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(RouteProfileManager.C_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0; lc.gridy = 0; lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(4, 0, 4, 10); lc.fill = GridBagConstraints.NONE;
        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = 1; fc.gridy = 0; fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1; fc.insets = new Insets(4, 0, 4, 0);

        String[][] rows = {{"Route Name", null}, {"Server Name", null}, {"Resource Type", null},
                           {"Min Level", null}, {"Status", null}};
        JComponent[] fields = {nameField, serverField, typeField, levelField, statusBox};

        for (int i = 0; i < fields.length; i++) {
            lc.gridy = fc.gridy = i;
            panel.add(dimLabel(rows[i][0]), lc);
            panel.add(fields[i], fc);
            if (fields[i] instanceof JTextField) {
                ((JTextField) fields[i]).getDocument().addDocumentListener(
                        (SimpleDocumentListener) e -> onChange.onChange());
            }
        }
        statusBox.setBackground(RouteProfileManager.C_BG);
        statusBox.setForeground(RouteProfileManager.C_FG);
        statusBox.addActionListener(e -> onChange.onChange());

        // Pad bottom
        GridBagConstraints pad = new GridBagConstraints();
        pad.gridy = rows.length; pad.weighty = 1; pad.fill = GridBagConstraints.VERTICAL;
        panel.add(Box.createVerticalGlue(), pad);
        return panel;
    }

    private JPanel anchorsTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(RouteProfileManager.C_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        panel.add(sectionLabel("Bank Stand Tile (where player stands at the booth)"));
        panel.add(tileRow("Stand tile:", standX, standY, standPlane, null));
        panel.add(Box.createVerticalStrut(14));
        panel.add(sectionLabel("Return Anchor (closest tree from bank side)"));
        panel.add(tileRow("Return tile:", retX, retY, retPlane, retId));
        panel.add(Box.createVerticalStrut(14));
        panel.add(sectionLabel("Bank Inside Tiles (optional — for banks inside buildings)"));
        insideTilePanel.setPreferredSize(new Dimension(0, 200));
        panel.add(insideTilePanel);
        return panel;
    }

    private JPanel tileRow(String labelText, JTextField xf, JTextField yf, JTextField pf, JTextField idf) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row.setOpaque(false);
        row.add(dimLabel(labelText));
        row.add(labeled("X:", xf));
        row.add(labeled("Y:", yf));
        row.add(labeled("Plane:", pf));
        if (idf != null) row.add(labeled("ID:", idf));
        return row;
    }

    private JPanel labeled(String text, JTextField field) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        p.setOpaque(false);
        p.add(dimLabel(text));
        p.add(field);
        return p;
    }

    void loadRoute(RouteModel route) {
        currentRoute = route;
        if (route == null) {
            nameField.setText("");
            serverField.setText("Reason");
            typeField.setText("");
            levelField.setText("0");
            statusBox.setSelectedItem("draft");
            treeTilePanel.setTiles(List.of());
            bankTilePanel.setTiles(List.of());
            insideTilePanel.setTiles(List.of());
            clearTileFields(standX, standY, standPlane);
            clearTileFields(retX, retY, retPlane);
            return;
        }
        nameField.setText(route.routeName);
        serverField.setText(route.serverName);
        typeField.setText(route.resourceType);
        levelField.setText(String.valueOf(route.levelRequirement));
        statusBox.setSelectedItem(route.status);
        treeTilePanel.setTiles(route.treeTiles);
        bankTilePanel.setTiles(route.bankTiles);
        insideTilePanel.setTiles(route.bankInsideTiles);
        fillTileFields(route.bankStandTile, standX, standY, standPlane, null);
        fillTileFields(route.returnAnchor,  retX,   retY,   retPlane,   retId);
    }

    void applyToModel(RouteModel route) {
        if (route == null) return;
        route.routeName      = nameField.getText().trim();
        route.serverName     = serverField.getText().trim();
        route.resourceType   = typeField.getText().trim();
        route.status         = (String) statusBox.getSelectedItem();
        try { route.levelRequirement = Integer.parseInt(levelField.getText().trim()); } catch (Exception ignored) {}
        route.treeTiles       = treeTilePanel.getTiles();
        route.bankTiles       = bankTilePanel.getTiles();
        route.bankInsideTiles = insideTilePanel.getTiles();
        route.bankStandTile   = readTileFields(null, "bankStand", standX, standY, standPlane, null);
        route.returnAnchor    = readTileFields(retLabel.getText(), "returnTarget", retX, retY, retPlane, retId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void fillTileFields(TileModel tile, JTextField xf, JTextField yf, JTextField pf, JTextField idf) {
        if (tile == null) { clearTileFields(xf, yf, pf); if (idf != null) idf.setText(""); return; }
        xf.setText(String.valueOf(tile.x));
        yf.setText(String.valueOf(tile.y));
        pf.setText(String.valueOf(tile.plane));
        if (idf != null) idf.setText(String.valueOf(tile.id));
    }

    private void clearTileFields(JTextField xf, JTextField yf, JTextField pf) {
        xf.setText(""); yf.setText(""); pf.setText("");
    }

    private TileModel readTileFields(String label, String cat, JTextField xf, JTextField yf, JTextField pf, JTextField idf) {
        try {
            int x = Integer.parseInt(xf.getText().trim());
            int y = Integer.parseInt(yf.getText().trim());
            if (x == 0 && y == 0) return null;
            int plane = pf.getText().isBlank() ? 0 : Integer.parseInt(pf.getText().trim());
            int id    = (idf != null && !idf.getText().isBlank()) ? Integer.parseInt(idf.getText().trim()) : 0;
            return new TileModel(label != null ? label : cat, cat, x, y, plane, id, "", "");
        } catch (Exception e) { return null; }
    }

    private static JTextField field(String placeholder) {
        JTextField tf = new JTextField(20);
        tf.setBackground(RouteProfileManager.C_BG);
        tf.setForeground(RouteProfileManager.C_FG);
        tf.setCaretColor(RouteProfileManager.C_FG);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RouteProfileManager.C_BORDER),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return tf;
    }

    private static JTextField numField() {
        JTextField tf = field("");
        tf.setColumns(7);
        return tf;
    }

    private static JLabel dimLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(RouteProfileManager.C_DIM);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }

    private static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(RouteProfileManager.C_FG);
        l.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Reusable tile table (used for trees, bank tiles, inside tiles)
// ─────────────────────────────────────────────────────────────────────────────

class TileTablePanel extends JPanel {
    private static final String[] COLUMNS = {"Label", "X", "Y", "Plane", "ID", "Actions"};

    private final String defaultCategory;
    private final DefaultTableModel tableModel;
    private final JTable table;

    TileTablePanel(String defaultCategory) {
        this.defaultCategory = defaultCategory;
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return true; }
        };
        table = new JTable(tableModel);
        styleTable();
        buildUi();
    }

    private void styleTable() {
        table.setBackground(RouteProfileManager.C_BG);
        table.setForeground(RouteProfileManager.C_FG);
        table.setSelectionBackground(new Color(39, 84, 121));
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(RouteProfileManager.C_BORDER);
        table.setFont(new Font("Consolas", Font.PLAIN, 11));
        table.setRowHeight(22);
        table.getTableHeader().setBackground(RouteProfileManager.C_CARD);
        table.getTableHeader().setForeground(RouteProfileManager.C_DIM);
        table.getTableHeader().setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
        // Column widths
        int[] widths = {130, 60, 60, 50, 70, 160};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private void buildUi() {
        setLayout(new BorderLayout(0, 4));
        setBackground(RouteProfileManager.C_CARD);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttonRow.setOpaque(false);
        JButton addBtn = RouteProfileManager.smallButton("+ Add Row", RouteProfileManager.C_GREEN);
        JButton delBtn = RouteProfileManager.smallButton("Remove Selected", RouteProfileManager.C_RED);
        JButton dupBtn = RouteProfileManager.smallButton("Duplicate", RouteProfileManager.C_BLUE);
        addBtn.addActionListener(e -> addRow());
        delBtn.addActionListener(e -> removeSelected());
        dupBtn.addActionListener(e -> duplicateSelected());
        buttonRow.add(addBtn);
        buttonRow.add(delBtn);
        buttonRow.add(dupBtn);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(RouteProfileManager.C_BG);
        scroll.setBorder(BorderFactory.createLineBorder(RouteProfileManager.C_BORDER));

        add(buttonRow, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    void setTiles(List<TileModel> tiles) {
        tableModel.setRowCount(0);
        for (TileModel t : tiles) {
            tableModel.addRow(new Object[]{t.label, t.x, t.y, t.plane, t.id, t.actions});
        }
    }

    List<TileModel> getTiles() {
        List<TileModel> result = new ArrayList<>();
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            String label   = str(r, 0);
            int x          = num(r, 1), y = num(r, 2), plane = num(r, 3), id = num(r, 4);
            String actions = str(r, 5);
            if (x != 0 || y != 0) {
                result.add(new TileModel(label, defaultCategory, x, y, plane, id, actions, ""));
            }
        }
        return result;
    }

    private void addRow() {
        tableModel.addRow(new Object[]{"tile " + (tableModel.getRowCount() + 1), 0, 0, 0, 0, ""});
        table.scrollRectToVisible(table.getCellRect(tableModel.getRowCount() - 1, 0, true));
    }

    private void removeSelected() {
        int[] selected = table.getSelectedRows();
        for (int i = selected.length - 1; i >= 0; i--) tableModel.removeRow(selected[i]);
    }

    private void duplicateSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        Object[] rowData = new Object[tableModel.getColumnCount()];
        for (int c = 0; c < rowData.length; c++) rowData[c] = tableModel.getValueAt(row, c);
        rowData[0] = rowData[0] + " (copy)";
        tableModel.insertRow(row + 1, rowData);
    }

    private String str(int row, int col) {
        Object v = tableModel.getValueAt(row, col);
        return v == null ? "" : v.toString().trim();
    }

    private int num(int row, int col) {
        try { return Integer.parseInt(str(row, col)); } catch (Exception e) { return 0; }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Mini-map — 2D dot visualization of tile positions
// ─────────────────────────────────────────────────────────────────────────────

class MiniMapPanel extends JPanel implements MouseMotionListener {
    private RouteModel route;
    private String hoverCoords = "";

    MiniMapPanel() {
        setBackground(new Color(10, 14, 20));
        setPreferredSize(new Dimension(320, 0));
        addMouseMotionListener(this);
    }

    void loadRoute(RouteModel route) {
        this.route = route;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        // Title
        g2.setColor(RouteProfileManager.C_DIM);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.drawString("Tile Map", 10, 18);

        if (route == null || (route.treeTiles.isEmpty() && route.bankTiles.isEmpty())) {
            g2.setColor(RouteProfileManager.C_DIM);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.drawString("No tiles captured", w / 2 - 55, h / 2);
            return;
        }

        // Collect all points
        List<TileModel> all = new ArrayList<>();
        all.addAll(route.treeTiles);
        all.addAll(route.bankTiles);
        all.addAll(route.bankInsideTiles);
        if (route.bankStandTile != null) all.add(route.bankStandTile);
        if (route.returnAnchor  != null) all.add(route.returnAnchor);
        if (all.isEmpty()) return;

        int minX = all.stream().mapToInt(t -> t.x).min().getAsInt();
        int minY = all.stream().mapToInt(t -> t.y).min().getAsInt();
        int maxX = all.stream().mapToInt(t -> t.x).max().getAsInt();
        int maxY = all.stream().mapToInt(t -> t.y).max().getAsInt();

        int rangeX = Math.max(maxX - minX, 1);
        int rangeY = Math.max(maxY - minY, 1);
        int pad = 36;
        double scaleX = (w - pad * 2.0) / rangeX;
        double scaleY = (h - pad * 2.0) / rangeY;
        double scale  = Math.min(scaleX, scaleY);
        scale = Math.min(scale, 20.0); // cap tile size

        // Grid
        g2.setColor(new Color(30, 38, 50));
        for (int x = minX; x <= maxX + 1; x++) {
            int sx = toScreen(x, minX, scale, pad, w);
            g2.drawLine(sx, pad, sx, h - pad);
        }
        for (int y = minY; y <= maxY + 1; y++) {
            int sy = toScreenY(y, minY, maxY, scale, pad, h);
            g2.drawLine(pad, sy, w - pad, sy);
        }

        // Draw tiles
        int dot = Math.max(4, (int) Math.min(scale - 1, 14));
        drawTiles(g2, route.bankTiles,       RouteProfileManager.C_BLUE,   dot, minX, minY, maxY, scale, pad, w, h, "Bank");
        drawTiles(g2, route.bankInsideTiles, RouteProfileManager.C_TEAL,   dot, minX, minY, maxY, scale, pad, w, h, "Inside");
        drawTiles(g2, route.treeTiles,       RouteProfileManager.C_GREEN,  dot, minX, minY, maxY, scale, pad, w, h, "Tree");
        if (route.bankStandTile != null)
            drawOneTile(g2, route.bankStandTile, RouteProfileManager.C_ORANGE, dot, minX, minY, maxY, scale, pad, w, h, "Stand");
        if (route.returnAnchor != null)
            drawOneTile(g2, route.returnAnchor, RouteProfileManager.C_PURPLE, dot, minX, minY, maxY, scale, pad, w, h, "Return");

        // Legend
        int lx = 10, ly = h - 80;
        drawLegend(g2, lx, ly,      RouteProfileManager.C_GREEN,  "Tree");
        drawLegend(g2, lx, ly + 14, RouteProfileManager.C_BLUE,   "Bank");
        drawLegend(g2, lx, ly + 28, RouteProfileManager.C_TEAL,   "Inside");
        drawLegend(g2, lx, ly + 42, RouteProfileManager.C_ORANGE, "Stand");
        drawLegend(g2, lx, ly + 56, RouteProfileManager.C_PURPLE, "Return");

        // Hover coords
        if (!hoverCoords.isEmpty()) {
            g2.setColor(RouteProfileManager.C_DIM);
            g2.setFont(new Font("Consolas", Font.PLAIN, 10));
            g2.drawString(hoverCoords, 10, h - 8);
        }

        // Coordinate labels
        g2.setColor(RouteProfileManager.C_DIM);
        g2.setFont(new Font("Consolas", Font.PLAIN, 9));
        g2.drawString(minX + "," + minY, pad - 4, h - pad + 12);
        g2.drawString(maxX + "," + maxY, w - pad - 40, pad - 4);
    }

    private void drawTiles(Graphics2D g2, List<TileModel> tiles, Color color, int dot,
                           int minX, int minY, int maxY, double scale, int pad, int w, int h, String label) {
        for (TileModel t : tiles) drawOneTile(g2, t, color, dot, minX, minY, maxY, scale, pad, w, h, label);
    }

    private void drawOneTile(Graphics2D g2, TileModel t, Color color, int dot,
                              int minX, int minY, int maxY, double scale, int pad, int w, int h, String label) {
        int sx = toScreen(t.x, minX, scale, pad, w);
        int sy = toScreenY(t.y, minY, maxY, scale, pad, h);
        g2.setColor(color);
        g2.fillRoundRect(sx - dot / 2, sy - dot / 2, dot, dot, 3, 3);
        g2.setColor(color.brighter());
        g2.drawRoundRect(sx - dot / 2, sy - dot / 2, dot, dot, 3, 3);
        if (dot >= 8) {
            g2.setColor(RouteProfileManager.C_DIM);
            g2.setFont(new Font("Consolas", Font.PLAIN, 8));
            g2.drawString(t.id > 0 ? String.valueOf(t.id) : "?", sx + dot / 2 + 2, sy + 4);
        }
    }

    private void drawLegend(Graphics2D g2, int x, int y, Color color, String label) {
        g2.setColor(color);
        g2.fillRect(x, y - 8, 8, 8);
        g2.setColor(RouteProfileManager.C_DIM);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.drawString(label, x + 12, y);
    }

    private int toScreen(int coord, int min, double scale, int pad, int totalSize) {
        return pad + (int) ((coord - min) * scale);
    }

    private int toScreenY(int coord, int minY, int maxY, double scale, int pad, int h) {
        // Invert Y (game Y increases upward, screen Y increases downward)
        return h - pad - (int) ((coord - minY) * scale);
    }

    @Override public void mouseMoved(MouseEvent e)   { updateHover(e); }
    @Override public void mouseDragged(MouseEvent e) { updateHover(e); }

    private void updateHover(MouseEvent e) {
        if (route == null) return;
        List<TileModel> all = new ArrayList<>(route.treeTiles);
        all.addAll(route.bankTiles);
        if (all.isEmpty()) return;
        int minX = all.stream().mapToInt(t -> t.x).min().getAsInt();
        int minY = all.stream().mapToInt(t -> t.y).min().getAsInt();
        int maxX = all.stream().mapToInt(t -> t.x).max().getAsInt();
        int maxY = all.stream().mapToInt(t -> t.y).max().getAsInt();
        int rangeX = Math.max(maxX - minX, 1);
        int rangeY = Math.max(maxY - minY, 1);
        int pad = 36, w = getWidth(), h = getHeight();
        double scaleX = (w - pad * 2.0) / rangeX;
        double scaleY = (h - pad * 2.0) / rangeY;
        double scale = Math.min(Math.min(scaleX, scaleY), 20.0);
        int worldX = minX + (int) ((e.getX() - pad) / scale);
        int worldY = maxY - (int) ((e.getY() - pad) / scale);
        hoverCoords = worldX + ", " + worldY;
        repaint();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Validation panel
// ─────────────────────────────────────────────────────────────────────────────

class ValidationPanel extends JPanel {
    private final JTextArea output = new JTextArea();

    ValidationPanel() {
        setLayout(new BorderLayout());
        setBackground(RouteProfileManager.C_CARD);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, RouteProfileManager.C_BORDER));

        JLabel title = new JLabel("  Validation");
        title.setForeground(RouteProfileManager.C_DIM);
        title.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
        title.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        output.setEditable(false);
        output.setBackground(RouteProfileManager.C_CARD);
        output.setForeground(RouteProfileManager.C_FG);
        output.setFont(new Font("Consolas", Font.PLAIN, 11));
        output.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        add(title, BorderLayout.NORTH);
        add(new JScrollPane(output), BorderLayout.CENTER);
        setPreferredSize(new Dimension(0, 130));
    }

    void validate(RouteModel route) {
        if (route == null) { output.setText("No route selected."); return; }
        List<String> missing = route.missingFields();
        if (missing.isEmpty()) {
            output.setText("✓ Route is complete. " + route.treeTiles.size() + " tree tile(s), " +
                    route.bankTiles.size() + " bank tile(s).\n" +
                    "  Tree IDs: " + route.treeTiles.stream().map(t -> String.valueOf(t.id)).distinct().collect(Collectors.joining(", ")) + "\n" +
                    "  Bank IDs: " + route.bankTiles.stream().map(t -> String.valueOf(t.id)).distinct().collect(Collectors.joining(", ")));
        } else {
            output.setText("⚠ Missing or incomplete fields:\n" +
                    missing.stream().map(s -> "  • " + s).collect(Collectors.joining("\n")));
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Quick Add Panel — paste or type KSBot examine output to add tiles instantly
//
//  KSBot Examine + Tile Location output formats this handles:
//    "Bank booth"       → name only
//    "id=10583"         → id only
//    "3186, 3436, 0"    → x, y, plane
//    "3186 3436 0"      → x y plane (space-separated)
//    "Name (10583) @ 3186, 3436, 0"   → combined one-liner
//    Any mix of the above on multiple lines
//
//  Usage: right-click an object in KSBot with Examine + Tile Location enabled,
//  copy whatever text appears in the infobox/examine area, then click
//  "Paste & Parse" — the panel fills x/y/id/name automatically.
// ─────────────────────────────────────────────────────────────────────────────

class QuickAddPanel extends JPanel {
    interface AddListener { void onAdd(TileModel tile, String targetList); }

    private final AddListener onAdd;

    // Manual entry fields
    private final JTextField nameField   = qField(14);
    private final JTextField idField     = qField(7);
    private final JTextField xField      = qField(7);
    private final JTextField yField      = qField(7);
    private final JTextField planeField  = qField(4);
    private final JTextField actField    = qField(14);

    // Target selector
    private final JComboBox<String> targetBox = new JComboBox<>(
            new String[]{"Tree Tile", "Bank Tile", "Bank Inside Tile", "Bank Stand Tile", "Return Anchor"});

    // Paste / parse area
    private final JTextArea pasteArea = new JTextArea(4, 30);

    QuickAddPanel(AddListener onAdd) {
        this.onAdd = onAdd;
        setLayout(new BorderLayout(0, 8));
        setBackground(RouteProfileManager.C_CARD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, RouteProfileManager.C_BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        buildUi();
    }

    private void buildUi() {
        // ── Header ──────────────────────────────────────────────────────────
        JLabel title = new JLabel("Quick Add  —  paste KSBot examine text or enter manually");
        title.setForeground(RouteProfileManager.C_FG);
        title.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));

        // ── Paste area ───────────────────────────────────────────────────────
        pasteArea.setBackground(new Color(14, 18, 25));
        pasteArea.setForeground(RouteProfileManager.C_DIM);
        pasteArea.setCaretColor(RouteProfileManager.C_FG);
        pasteArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        pasteArea.setLineWrap(true);
        pasteArea.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        pasteArea.setToolTipText("Paste any text from KSBot examine/tile-location infobox here, then click Parse");
        JScrollPane pasteScroll = new JScrollPane(pasteArea);
        pasteScroll.setBorder(BorderFactory.createLineBorder(RouteProfileManager.C_BORDER));
        pasteScroll.setPreferredSize(new Dimension(0, 72));

        JButton pasteBtn = RouteProfileManager.smallButton("Paste & Parse", RouteProfileManager.C_YELLOW);
        JButton clearBtn = RouteProfileManager.smallButton("Clear", RouteProfileManager.C_RED);
        pasteBtn.addActionListener(e -> pasteFromClipboardAndParse());
        clearBtn.addActionListener(e -> { pasteArea.setText(""); clearFields(); });

        JPanel pasteRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pasteRow.setOpaque(false);
        pasteRow.add(pasteBtn);
        pasteRow.add(clearBtn);
        pasteRow.add(dim("\u2190 copies clipboard into the box automatically"));

        JPanel pastePanel = new JPanel(new BorderLayout(0, 4));
        pastePanel.setOpaque(false);
        pastePanel.add(pasteScroll, BorderLayout.CENTER);
        pastePanel.add(pasteRow, BorderLayout.SOUTH);

        // ── Manual fields ─────────────────────────────────────────────────────
        JPanel fieldsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        fieldsPanel.setOpaque(false);
        fieldsPanel.add(labeled2("Name / Label:", nameField));
        fieldsPanel.add(labeled2("ID:", idField));
        fieldsPanel.add(labeled2("X:", xField));
        fieldsPanel.add(labeled2("Y:", yField));
        fieldsPanel.add(labeled2("Plane:", planeField));
        fieldsPanel.add(labeled2("Actions:", actField));

        // ── Target + Add button ──────────────────────────────────────────────
        targetBox.setBackground(RouteProfileManager.C_BG);
        targetBox.setForeground(RouteProfileManager.C_FG);
        targetBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton addBtn = RouteProfileManager.smallButton("Add to Route →", RouteProfileManager.C_GREEN);
        addBtn.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        addBtn.addActionListener(e -> commitTile());

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow.setOpaque(false);
        actionRow.add(dim("Add as:"));
        actionRow.add(targetBox);
        actionRow.add(addBtn);

        // ── Layout ────────────────────────────────────────────────────────────
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(pastePanel);
        center.add(Box.createVerticalStrut(6));
        center.add(fieldsPanel);
        center.add(Box.createVerticalStrut(4));
        center.add(actionRow);

        add(title, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }

    // ── Parse logic ───────────────────────────────────────────────────────────

    private void pasteFromClipboardAndParse() {
        try {
            String clip = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (clip != null && !clip.isBlank()) {
                pasteArea.setText(clip.trim());
            }
        } catch (Exception ignored) {}
        parseText(pasteArea.getText());
    }

    /**
     * Parses whatever text the user pasted. Handles several KSBot output formats:
     *
     *   Examine line:     "Bank booth"  or  "Yew tree"
     *   ID line:          "id: 10583"   or  "10583"   or  "(10583)"
     *   Tile Location:    "3186, 3436, 0"  or  "3186 3436"
     *   Combined:         "Bank booth (10583) @ 3186, 3436, 0"
     *   Any subset of above spread across multiple lines
     */
    void parseText(String raw) {
        if (raw == null || raw.isBlank()) return;

        String name = "";
        int    id   = 0, x = 0, y = 0, plane = 0;

        // ── Combined one-liner: "Name (id) @ x, y, plane" ───────────────────
        java.util.regex.Matcher combo = java.util.regex.Pattern.compile(
                "([A-Za-z][\\w\\s]+?)\\s*\\(?\\s*(\\d+)\\s*\\)?\\s*[@,]?\\s*(\\d{3,5})[,\\s]+(\\d{3,5})[,\\s]*(\\d?)")
                .matcher(raw);
        if (combo.find()) {
            name  = combo.group(1).trim();
            id    = safe(combo.group(2));
            x     = safe(combo.group(3));
            y     = safe(combo.group(4));
            plane = safe(combo.group(5));
            fill(name, id, x, y, plane);
            return;
        }

        // ── Line-by-line parsing ──────────────────────────────────────────────
        String[] lines = raw.split("[\\r\\n]+");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Coordinate line: contains two or three numbers that look like game coords
            java.util.regex.Matcher coords = java.util.regex.Pattern.compile(
                    "(\\d{3,5})[,\\s]+(\\d{3,5})(?:[,\\s]+(\\d))?").matcher(line);
            if (coords.find()) {
                int cx = safe(coords.group(1)), cy = safe(coords.group(2));
                // Game world coords are typically 1500–4000
                if (cx >= 1000 && cx <= 5000 && cy >= 1000 && cy <= 5000) {
                    x = cx; y = cy;
                    if (coords.group(3) != null) plane = safe(coords.group(3));
                    continue;
                }
            }

            // ID line: "id: 1234", "(1234)", or standalone 4-5 digit number
            java.util.regex.Matcher idMatch = java.util.regex.Pattern.compile(
                    "(?:id[:\\s]+|\\()?(\\d{4,6})\\)?").matcher(line.toLowerCase());
            if (idMatch.find() && !line.matches(".*[,\\s]{2}.*")) {
                int candidate = safe(idMatch.group(1));
                if (candidate >= 100 && candidate < 70000) { id = candidate; continue; }
            }

            // Name line: mostly letters, not a coordinate or ID
            if (line.matches("[A-Za-z][\\w\\s'\\-]+") && id == 0 && x == 0) {
                name = line;
            }
        }
        fill(name, id, x, y, plane);
    }

    private void fill(String name, int id, int x, int y, int plane) {
        nameField.setText(name);
        idField.setText(id > 0 ? String.valueOf(id) : "");
        xField.setText(x > 0 ? String.valueOf(x) : "");
        yField.setText(y > 0 ? String.valueOf(y) : "");
        planeField.setText(String.valueOf(plane));
        // Auto-suggest target based on name
        if (!name.isBlank()) {
            String lower = name.toLowerCase();
            if (lower.contains("bank") || lower.contains("booth") || lower.contains("chest")) {
                targetBox.setSelectedItem("Bank Tile");
            } else if (lower.contains("tree") || lower.contains("oak") || lower.contains("willow")
                    || lower.contains("maple") || lower.contains("yew") || lower.contains("magic")) {
                targetBox.setSelectedItem("Tree Tile");
            }
        }
    }

    private void clearFields() {
        nameField.setText(""); idField.setText(""); xField.setText(""); yField.setText("");
        planeField.setText("0"); actField.setText("");
    }

    private void commitTile() {
        try {
            int x = Integer.parseInt(xField.getText().trim());
            int y = Integer.parseInt(yField.getText().trim());
            if (x == 0 && y == 0) {
                JOptionPane.showMessageDialog(this, "X and Y coordinates are required.");
                return;
            }
            int plane = planeField.getText().isBlank() ? 0 : Integer.parseInt(planeField.getText().trim());
            int id    = idField.getText().isBlank() ? 0 : Integer.parseInt(idField.getText().trim());
            String name    = nameField.getText().trim();
            String actions = actField.getText().trim();
            String target  = (String) targetBox.getSelectedItem();

            String cat;
            if ("Bank Tile".equals(target))        cat = "bankTarget";
            else if ("Bank Inside Tile".equals(target)) cat = "bankInside";
            else if ("Bank Stand Tile".equals(target))  cat = "bankStand";
            else if ("Return Anchor".equals(target))    cat = "returnTarget";
            else                                        cat = "resourceTile";

            TileModel tile = new TileModel(name.isBlank() ? cat + " tile" : name, cat, x, y, plane, id, actions, "");
            onAdd.onAdd(tile, target);
            clearFields();
            pasteArea.setText("");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid number in X, Y, Plane, or ID field.");
        }
    }

    private static int safe(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private static JTextField qField(int cols) {
        JTextField tf = new JTextField(cols);
        tf.setBackground(new Color(14, 18, 25));
        tf.setForeground(RouteProfileManager.C_FG);
        tf.setCaretColor(RouteProfileManager.C_FG);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RouteProfileManager.C_BORDER),
                BorderFactory.createEmptyBorder(3, 5, 3, 5)));
        tf.setFont(new Font("Consolas", Font.PLAIN, 12));
        return tf;
    }

    private static JLabel dim(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(RouteProfileManager.C_DIM);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return l;
    }

    private static JPanel labeled2(String label, JTextField field) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        p.add(dim(label));
        p.add(field);
        return p;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Utility — simple DocumentListener functional interface
// ─────────────────────────────────────────────────────────────────────────────

interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
    void update(javax.swing.event.DocumentEvent e);
    default void insertUpdate(javax.swing.event.DocumentEvent e) { update(e); }
    default void removeUpdate(javax.swing.event.DocumentEvent e) { update(e); }
    default void changedUpdate(javax.swing.event.DocumentEvent e) { update(e); }
}
