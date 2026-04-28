# rs.kreme.ksbot.api.hooks.Mouse

Package: ``rs.kreme.ksbot.api.hooks``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public class rs.kreme.ksbot.api.hooks.Mouse {
  protected final java.util.Random random;
  protected final rs.kreme.ksbot.api.KSContext ctx;
  public static int[] IIIllIIliIIl;
  static {};
  public void move(net.runelite.api.Point);
  public boolean clickAction(java.awt.geom.Rectangle2D, java.lang.String);
  public boolean isEntityVisible(java.awt.geom.Rectangle2D);
  public rs.kreme.ksbot.api.hooks.Mouse(rs.kreme.ksbot.api.KSContext);
  public void installCanvasListeners();
  public void click(int, int, int);
  public boolean clickBounds(java.awt.geom.Rectangle2D, int);
  public void drag(int, int);
  public void rightClick(int, int);
  public java.awt.geom.Rectangle2D ensureEntityVisible(java.util.function.Supplier<java.awt.geom.Rectangle2D>, net.runelite.api.coords.WorldPoint);
  public void press(int, int, int);
  public void scroll(int, int, int);
  public void move(int, int);
  public static java.lang.Object IliIlllilIi(java.lang.Object[]);
  public void click(int, int);
  public java.awt.geom.Rectangle2D getViewportClippedBounds(java.awt.geom.Rectangle2D);
  public void rightClick(net.runelite.api.Point);
  public void release(int, int, int);
  public void click(net.runelite.api.Point);
  public static void iliIiiliiiii();
  public java.util.List<rs.kreme.ksbot.api.hooks.Mouse$ClickEvent> getRecentClicks();
}
```
