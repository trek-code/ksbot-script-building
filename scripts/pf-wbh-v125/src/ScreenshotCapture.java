package pfwbh125;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * Captures the game client viewport as PNG bytes for Discord webhook attachments.
 * <p>
 * Strategy (in order):
 * <ol>
 *   <li>Find the largest visible {@link Canvas} nested inside any AWT
 *       {@link Frame} — this is the RuneLite game viewport.</li>
 *   <li>{@link Robot#createScreenCapture(Rectangle)} against its on-screen
 *       bounds — reliable with hardware-accelerated rendering.</li>
 *   <li>If the Canvas isn't available (or Robot is blocked), fall back to
 *       painting the largest visible Frame into a BufferedImage on the EDT.</li>
 * </ol>
 * All captures are downscaled to {@code maxWidth} to keep the upload fast
 * and well under Discord's 8 MB free-tier cap. Returns {@code null} on any
 * failure — callers should treat that as "skip attachment, send text only".
 */
public final class ScreenshotCapture {

    private ScreenshotCapture() {}

    /**
     * @param maxWidth max horizontal resolution of the encoded PNG; 0 = no cap.
     * @return PNG bytes, or null on any failure.
     */
    public static byte[] capturePng(int maxWidth) {
        try {
            BufferedImage img = null;

            Canvas gameCanvas = findGameCanvas();
            if (gameCanvas != null && gameCanvas.isShowing()) {
                img = robotCapture(gameCanvas);
                if (img == null || isBlank(img)) {
                    BufferedImage painted = paintComponent(gameCanvas);
                    if (painted != null && !isBlank(painted)) img = painted;
                }
            }

            if (img == null || isBlank(img)) {
                Frame frame = findLargestVisibleFrame();
                if (frame != null) {
                    img = robotCapture(frame);
                    if (img == null || isBlank(img)) img = paintComponent(frame);
                }
            }

            if (img == null) return null;
            if (maxWidth > 0 && img.getWidth() > maxWidth) img = scale(img, maxWidth);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }

    // ── Component discovery ──────────────────────────────────────────────

    private static Canvas findGameCanvas() {
        Canvas best = null;
        int    bestArea = 0;
        for (Frame f : Frame.getFrames()) {
            if (!f.isShowing()) continue;
            Canvas c = findCanvasIn(f);
            if (c == null || !c.isShowing()) continue;
            int area = c.getWidth() * c.getHeight();
            if (area > bestArea) { bestArea = area; best = c; }
        }
        return best;
    }

    private static Canvas findCanvasIn(Container parent) {
        for (Component ch : parent.getComponents()) {
            if (ch instanceof Canvas) return (Canvas) ch;
            if (ch instanceof Container) {
                Canvas x = findCanvasIn((Container) ch);
                if (x != null) return x;
            }
        }
        return null;
    }

    private static Frame findLargestVisibleFrame() {
        Frame best = null;
        int   bestArea = 0;
        for (Frame f : Frame.getFrames()) {
            if (!f.isShowing()) continue;
            int area = f.getWidth() * f.getHeight();
            if (area > bestArea) { bestArea = area; best = f; }
        }
        return best;
    }

    // ── Capture strategies ───────────────────────────────────────────────

    private static BufferedImage robotCapture(Component c) {
        try {
            if (c.getWidth() < 32 || c.getHeight() < 32) return null;
            Point p = c.getLocationOnScreen();
            Rectangle r = new Rectangle(p.x, p.y, c.getWidth(), c.getHeight());
            return new Robot().createScreenCapture(r);
        } catch (Throwable t) {
            return null;
        }
    }

    private static BufferedImage paintComponent(final Component c) {
        final BufferedImage[] out = new BufferedImage[1];
        Runnable r = () -> {
            try {
                int w = Math.max(1, c.getWidth());
                int h = Math.max(1, c.getHeight());
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                try { c.paint(g); } finally { g.dispose(); }
                out[0] = img;
            } catch (Throwable ignored) {}
        };
        try {
            if (SwingUtilities.isEventDispatchThread()) r.run();
            else SwingUtilities.invokeAndWait(r);
        } catch (Throwable ignored) {}
        return out[0];
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** All four corners identical → probably unrendered / occluded. */
    private static boolean isBlank(BufferedImage img) {
        try {
            int w = img.getWidth(), h = img.getHeight();
            if (w < 2 || h < 2) return true;
            int a = img.getRGB(0, 0);
            int b = img.getRGB(w - 1, 0);
            int c = img.getRGB(0, h - 1);
            int d = img.getRGB(w - 1, h - 1);
            int e = img.getRGB(w / 2, h / 2);
            return a == b && b == c && c == d && d == e;
        } catch (Throwable t) { return false; }
    }

    private static BufferedImage scale(BufferedImage src, int maxWidth) {
        double s = (double) maxWidth / src.getWidth();
        int w = maxWidth;
        int h = Math.max(1, (int) Math.round(src.getHeight() * s));
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, w, h, null);
        } finally { g.dispose(); }
        return dst;
    }
}
