# rs.kreme.ksbot.api.queries.TileObjectQuery

Package: ``rs.kreme.ksbot.api.queries``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public class rs.kreme.ksbot.api.queries.TileObjectQuery extends rs.kreme.ksbot.api.queries.Query<rs.kreme.ksbot.api.wrappers.KSObject, rs.kreme.ksbot.api.queries.TileObjectQuery> {
  public static int[] iIlIlllllill;
  final rs.kreme.ksbot.api.KSContext ctx;
  protected java.lang.String[] getOptions(java.lang.Object);
  public rs.kreme.ksbot.api.queries.TileObjectQuery animating();
  public rs.kreme.ksbot.api.queries.TileObjectQuery withinDistance(int);
  public rs.kreme.ksbot.api.queries.TileObjectQuery notAtLocation(java.util.List<net.runelite.api.coords.WorldPoint>);
  protected int getId(rs.kreme.ksbot.api.wrappers.KSObject);
  public rs.kreme.ksbot.api.wrappers.KSObject nearestToPoint(net.runelite.api.coords.WorldPoint);
  protected java.lang.String getName(java.lang.Object);
  public rs.kreme.ksbot.api.queries.TileObjectQuery(java.util.Collection<rs.kreme.ksbot.api.wrappers.KSObject>);
  public rs.kreme.ksbot.api.queries.TileObjectQuery withImposterId(int);
  public rs.kreme.ksbot.api.queries.TileObjectQuery atLocation(net.runelite.api.coords.WorldPoint...);
  protected int getId(java.lang.Object);
  public rs.kreme.ksbot.api.queries.TileObjectQuery idle();
  public rs.kreme.ksbot.api.queries.TileObjectQuery aboveDistance(int);
  public rs.kreme.ksbot.api.queries.TileObjectQuery notInArea(rs.kreme.ksbot.api.hooks.WorldArea);
  static {};
  public static void IilliliilIIi();
  protected java.lang.String getName(rs.kreme.ksbot.api.wrappers.KSObject);
  public rs.kreme.ksbot.api.queries.TileObjectQuery atLocation(java.util.List<net.runelite.api.coords.WorldPoint>);
  protected java.lang.String[] getOptions(rs.kreme.ksbot.api.wrappers.KSObject);
  public rs.kreme.ksbot.api.queries.TileObjectQuery notInArea(net.runelite.api.coords.WorldArea);
  public rs.kreme.ksbot.api.wrappers.KSObject nearestToPlayer();
  public rs.kreme.ksbot.api.queries.TileObjectQuery notAtLocation(net.runelite.api.coords.WorldPoint...);
  public rs.kreme.ksbot.api.wrappers.KSObject closest();
  public rs.kreme.ksbot.api.queries.TileObjectQuery inArea(net.runelite.api.coords.WorldArea);
  public rs.kreme.ksbot.api.queries.TileObjectQuery inArea(rs.kreme.ksbot.api.hooks.WorldArea);
}
```
