# rs.kreme.ksbot.api.queries.PlayerQuery

Package: ``rs.kreme.ksbot.api.queries``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public class rs.kreme.ksbot.api.queries.PlayerQuery extends rs.kreme.ksbot.api.queries.Query<rs.kreme.ksbot.api.wrappers.KSPlayer, rs.kreme.ksbot.api.queries.PlayerQuery> {
  public static int[] iIlIlllllill;
  public rs.kreme.ksbot.api.queries.PlayerQuery idle();
  public rs.kreme.ksbot.api.queries.PlayerQuery wearingAll(java.util.List<?>);
  public rs.kreme.ksbot.api.wrappers.KSPlayer furthest();
  public rs.kreme.ksbot.api.queries.PlayerQuery interactingWithLocal();
  public static void IilliliilIIi();
  public rs.kreme.ksbot.api.queries.PlayerQuery alive();
  public rs.kreme.ksbot.api.queries.PlayerQuery wearingItem(int);
  protected java.lang.String[] getOptions(rs.kreme.ksbot.api.wrappers.KSPlayer);
  public rs.kreme.ksbot.api.queries.PlayerQuery wearingItem(java.lang.String);
  public rs.kreme.ksbot.api.queries.PlayerQuery notLocalPlayer();
  public rs.kreme.ksbot.api.queries.PlayerQuery(java.util.Collection<rs.kreme.ksbot.api.wrappers.KSPlayer>);
  protected java.lang.String getName(java.lang.Object);
  protected int getId(java.lang.Object);
  public rs.kreme.ksbot.api.wrappers.KSPlayer closest();
  protected java.lang.String getName(rs.kreme.ksbot.api.wrappers.KSPlayer);
  protected int getId(rs.kreme.ksbot.api.wrappers.KSPlayer);
  protected java.lang.String[] getOptions(java.lang.Object);
  static {};
  public rs.kreme.ksbot.api.queries.PlayerQuery withinLevel(int, int);
  public rs.kreme.ksbot.api.queries.PlayerQuery withinDistance(int);
  public rs.kreme.ksbot.api.queries.PlayerQuery isSkulled();
  public rs.kreme.ksbot.api.queries.PlayerQuery withinDistance(int, net.runelite.api.coords.WorldPoint);
  public rs.kreme.ksbot.api.queries.PlayerQuery animating();
  public rs.kreme.ksbot.api.queries.PlayerQuery hasOverheadPrayer();
  public rs.kreme.ksbot.api.queries.PlayerQuery wearingAny(int...);
  public rs.kreme.ksbot.api.queries.PlayerQuery moving();
  public rs.kreme.ksbot.api.queries.PlayerQuery wearingAny(java.util.List<?>);
  public rs.kreme.ksbot.api.queries.PlayerQuery onTeam(int);
  public rs.kreme.ksbot.api.queries.PlayerQuery wearingAny(java.lang.String...);
  public rs.kreme.ksbot.api.queries.PlayerQuery wearingAll(java.lang.String...);
  public rs.kreme.ksbot.api.queries.PlayerQuery interactingWith(net.runelite.api.Actor);
  public rs.kreme.ksbot.api.queries.PlayerQuery sortByDistance(boolean);
  public rs.kreme.ksbot.api.queries.PlayerQuery wearingAll(int...);
}
```
