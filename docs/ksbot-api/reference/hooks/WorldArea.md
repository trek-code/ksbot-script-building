# rs.kreme.ksbot.api.hooks.WorldArea

Package: ``rs.kreme.ksbot.api.hooks``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public class rs.kreme.ksbot.api.hooks.WorldArea {
  protected final int y;
  public static int[] IIIllIIliIIl;
  protected final int height;
  protected final int x;
  protected final int width;
  protected final int plane;
  public static void iliIiiliiiii();
  public boolean contains(rs.kreme.ksbot.api.wrappers.KSGroundItem);
  public int getHeight();
  public rs.kreme.ksbot.api.hooks.WorldArea(int, int, int, int, int);
  public rs.kreme.ksbot.api.hooks.WorldArea(net.runelite.api.coords.WorldPoint, net.runelite.api.coords.WorldPoint);
  public boolean contains(int, int, int);
  static {};
  public net.runelite.api.coords.WorldPoint getCentrePoint();
  public int getWidth();
  public boolean overlaps(rs.kreme.ksbot.api.hooks.WorldArea);
  public static rs.kreme.ksbot.api.hooks.WorldArea fromRuneLite(net.runelite.api.coords.WorldArea);
  public int getMaxY();
  public boolean equals(java.lang.Object);
  public net.runelite.api.coords.WorldArea toRuneLite();
  public java.lang.String toString();
  public java.util.List<net.runelite.api.coords.WorldPoint> toWorldPointList();
  public boolean contains(net.runelite.api.coords.WorldPoint);
  public net.runelite.api.coords.WorldPoint getRandomPoint();
  public static java.lang.Object IliIlllilIi(java.lang.Object[]);
  public boolean contains(rs.kreme.ksbot.api.hooks.WorldArea);
  public boolean contains(rs.kreme.ksbot.api.wrappers.KSObject);
  public int getMaxX();
  public net.runelite.api.coords.WorldPoint[] getCorners();
  public rs.kreme.ksbot.api.hooks.WorldArea shift(int, int);
  public int getY();
  public rs.kreme.ksbot.api.hooks.WorldArea(net.runelite.api.coords.WorldPoint...);
  public rs.kreme.ksbot.api.hooks.WorldArea expand(int);
  public int getTileCount();
  public boolean contains(net.runelite.api.Actor);
  public int getPlane();
  public int getX();
  public int hashCode();
}
```
