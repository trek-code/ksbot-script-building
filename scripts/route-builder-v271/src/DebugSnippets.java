package route.buildercl271;

import net.runelite.api.coords.WorldPoint;
import rs.kreme.ksbot.api.KSContext;
import rs.kreme.ksbot.api.wrappers.KSGroundItem;
import rs.kreme.ksbot.api.wrappers.KSItem;
import rs.kreme.ksbot.api.wrappers.KSNPC;
import rs.kreme.ksbot.api.wrappers.KSObject;
import rs.kreme.ksbot.api.wrappers.KSPlayer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class DebugSnippets {

    private static final int DEFAULT_RADIUS = 12;

    private DebugSnippets() {}

    public static String forObject(KSObject o) {
        if (o == null) return "// no object in range";
        WorldPoint wp = o.getWorldLocation();
        String coords = (wp == null) ? "?, ?, ?" : wp.getX() + ", " + wp.getY() + ", " + wp.getPlane();
        return ""
                + "// " + safe(o.getName()) + " (id=" + o.getId() + ") @ " + coords + "\n"
                + "// actions: " + joinActions(o.getActions()) + "\n"
                + "KSObject target = ctx.groundObjects.query()\n"
                + "        .withId(" + o.getId() + ")\n"
                + "        .withinDistance(" + DEFAULT_RADIUS + ")\n"
                + "        .closest();";
    }

    public static String forNPC(KSNPC n) {
        if (n == null) return "// no NPC in range";
        WorldPoint wp = n.getWorldLocation();
        String coords = (wp == null) ? "?, ?, ?" : wp.getX() + ", " + wp.getY() + ", " + wp.getPlane();
        return ""
                + "// " + safe(n.getName()) + " (id=" + n.getId() + ", lvl=" + n.getCombatLevel() + ") @ " + coords + "\n"
                + "// actions: " + joinActions(n.getActions()) + "\n"
                + "KSNPC target = ctx.npcs.query()\n"
                + "        .withId(" + n.getId() + ")\n"
                + "        .withinDistance(" + DEFAULT_RADIUS + ")\n"
                + "        .closest();";
    }

    public static String forGroundItem(KSGroundItem g) {
        if (g == null) return "// no ground item in range";
        WorldPoint wp = g.getWorldLocation();
        String coords = (wp == null) ? "?, ?, ?" : wp.getX() + ", " + wp.getY() + ", " + wp.getPlane();
        return ""
                + "// " + safe(g.getName()) + " (id=" + g.getId() + ") @ " + coords + "\n"
                + "// actions: " + joinActions(g.getActions()) + "\n"
                + "KSGroundItem loot = ctx.groundItems.query()\n"
                + "        .withId(" + g.getId() + ")\n"
                + "        .withinDistance(" + DEFAULT_RADIUS + ")\n"
                + "        .closest();";
    }

    public static String forPlayer(KSPlayer p) {
        if (p == null) return "// no player snapshot";
        WorldPoint wp = p.getWorldLocation();
        String coords = (wp == null) ? "?, ?, ?" : wp.getX() + ", " + wp.getY() + ", " + wp.getPlane();
        return ""
                + "// " + safe(p.getName()) + " @ " + coords + "\n"
                + "KSPlayer me = ctx.players.getLocal();";
    }

    public static String forItem(KSItem i, String container) {
        if (i == null) return "// no item captured";
        String c = (container == null || container.isBlank()) ? "inventory" : container;
        return ""
                + "// " + safe(i.getName()) + " (id=" + i.getId() + ") slot=" + i.getSlot()
                + " qty=" + i.getQuantity() + (i.isNoted() ? " [noted]" : "")
                + (i.isStackable() ? " [stackable]" : "") + "\n"
                + "// actions: " + joinActions(i.getActions()) + "\n"
                + "KSItem item = ctx." + c + ".query()\n"
                + "        .withId(" + i.getId() + ")\n"
                + "        .first();";
    }

    public static String forWorldPoint(WorldPoint wp, String label) {
        if (wp == null) return "// no tile";
        String var = camel(label == null ? "tile" : label);
        return ""
                + "// " + (label == null ? "tile" : label) + "\n"
                + "WorldPoint " + var + " = new WorldPoint("
                + wp.getX() + ", " + wp.getY() + ", " + wp.getPlane() + ");";
    }

    public static String captureNearestObject(KSContext ctx) {
        return forObject(ctx.groundObjects.query().withinDistance(DEFAULT_RADIUS).closest());
    }

    public static String captureNearestNPC(KSContext ctx) {
        return forNPC(ctx.npcs.query().withinDistance(DEFAULT_RADIUS).closest());
    }

    public static String captureNearestGroundItem(KSContext ctx) {
        return forGroundItem(ctx.groundItems.query().withinDistance(DEFAULT_RADIUS).closest());
    }

    public static String captureLocalPlayer(KSContext ctx) {
        return forPlayer(ctx.players.getLocal());
    }

    public static String captureCombatTarget(KSContext ctx) {
        return forNPC(ctx.combat.getCurrentTarget());
    }

    public static String captureMyTile(KSContext ctx) {
        KSPlayer me = ctx.players.getLocal();
        return forWorldPoint(me == null ? null : me.getWorldLocation(), "my tile");
    }

    public static String captureFirstInventoryItem(KSContext ctx) {
        return forItem(ctx.inventory.query().first(), "inventory");
    }

    public static void toClipboard(String s) {
        if (s == null) return;
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(s), null);
        } catch (Exception ignored) {
        }
    }

    public static String captureAndCopy(Supplier<String> capture, Consumer<String> log) {
        String s = capture.get();
        toClipboard(s);
        if (log != null) log.accept("[snippet copied]\n" + s);
        return s;
    }

    public static final class Panel extends JPanel {

        public Panel(KSContext ctx, Consumer<String> logger) {
            setLayout(new GridLayout(0, 2, 6, 6));
            setBorder(BorderFactory.createTitledBorder("Debug -> Code Snippet"));

            addBtn("Nearest Object",      () -> captureNearestObject(ctx), logger);
            addBtn("Nearest NPC",         () -> captureNearestNPC(ctx), logger);
            addBtn("Nearest Ground Item", () -> captureNearestGroundItem(ctx), logger);
            addBtn("Combat Target",       () -> captureCombatTarget(ctx), logger);
            addBtn("Local Player",        () -> captureLocalPlayer(ctx), logger);
            addBtn("My Tile",             () -> captureMyTile(ctx), logger);
            addBtn("First Inv Item",      () -> captureFirstInventoryItem(ctx), logger);
        }

        public Panel addCustom(String label, Supplier<String> capture, Consumer<String> logger) {
            addBtn(label, capture, logger);
            return this;
        }

        private void addBtn(String label, Supplier<String> capture, Consumer<String> logger) {
            JButton b = new JButton(label);
            b.setToolTipText("Capture + copy snippet to clipboard");
            b.addActionListener(e -> captureAndCopy(capture, logger));
            add(b);
        }
    }

    private static String joinActions(String[] actions) {
        if (actions == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String a : actions) {
            if (a != null && !a.isBlank() && !"null".equalsIgnoreCase(a)) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(a.trim());
            }
        }
        return sb.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\n", " ").replace("\r", " ");
    }

    private static String camel(String label) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                upper = false;
            } else {
                upper = sb.length() > 0;
            }
        }
        String out = sb.toString();
        return out.isEmpty() ? "tile" : out;
    }
}
