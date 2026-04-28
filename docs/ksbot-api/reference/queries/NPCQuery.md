# rs.kreme.ksbot.api.queries.NPCQuery

Package: ``rs.kreme.ksbot.api.queries``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public class rs.kreme.ksbot.api.queries.NPCQuery extends rs.kreme.ksbot.api.queries.Query<rs.kreme.ksbot.api.wrappers.KSNPC, rs.kreme.ksbot.api.queries.NPCQuery> {
  public static int[] iIlIlllllill;
  protected rs.kreme.ksbot.api.KSContext ctx;
  public rs.kreme.ksbot.api.queries.NPCQuery inCombat();
  public static void IilliliilIIi();
  protected java.lang.String[] getOptions(java.lang.Object);
  static {};
  public rs.kreme.ksbot.api.wrappers.KSNPC furthest();
  public rs.kreme.ksbot.api.queries.NPCQuery interactingWithLocal();
  public rs.kreme.ksbot.api.queries.NPCQuery withIndex(int);
  public rs.kreme.ksbot.api.queries.NPCQuery notInArea(rs.kreme.ksbot.api.hooks.WorldArea);
  public rs.kreme.ksbot.api.queries.NPCQuery notInteracting();
  public rs.kreme.ksbot.api.queries.NPCQuery notInArea(net.runelite.api.coords.WorldArea);
  protected java.lang.String[] getOptions(rs.kreme.ksbot.api.wrappers.KSNPC);
  public rs.kreme.ksbot.api.queries.NPCQuery idle();
  protected int getId(rs.kreme.ksbot.api.wrappers.KSNPC);
  public rs.kreme.ksbot.api.queries.NPCQuery withAnimation(int);
  public rs.kreme.ksbot.api.queries.NPCQuery alive();
  protected java.lang.String getName(java.lang.Object);
  public rs.kreme.ksbot.api.queries.NPCQuery inArea(rs.kreme.ksbot.api.hooks.WorldArea);
  public rs.kreme.ksbot.api.queries.NPCQuery withinLevel(int, int);
  public rs.kreme.ksbot.api.queries.NPCQuery withGraphic(int);
  protected java.lang.String getName(rs.kreme.ksbot.api.wrappers.KSNPC);
  public rs.kreme.ksbot.api.queries.NPCQuery(java.util.Collection<rs.kreme.ksbot.api.wrappers.KSNPC>);
  public rs.kreme.ksbot.api.queries.NPCQuery noOneInteractingWith();
  public rs.kreme.ksbot.api.queries.NPCQuery dead();
  public rs.kreme.ksbot.api.queries.NPCQuery withoutGraphic(int);
  public rs.kreme.ksbot.api.queries.NPCQuery withinDistance(int);
  public rs.kreme.ksbot.api.queries.NPCQuery animating();
  public rs.kreme.ksbot.api.queries.NPCQuery atLocation(net.runelite.api.coords.WorldPoint);
  public rs.kreme.ksbot.api.queries.NPCQuery hasHintArrow();
  public rs.kreme.ksbot.api.queries.NPCQuery interactingWith(net.runelite.api.Actor);
  public rs.kreme.ksbot.api.queries.NPCQuery meleeable();
  protected int getId(java.lang.Object);
  public rs.kreme.ksbot.api.wrappers.KSNPC closest();
  public rs.kreme.ksbot.api.queries.NPCQuery interacting();
  public rs.kreme.ksbot.api.queries.NPCQuery withoutAnimation(int);
  public rs.kreme.ksbot.api.queries.NPCQuery withLoS();
  public rs.kreme.ksbot.api.queries.NPCQuery aboveDistance(int);
  public rs.kreme.ksbot.api.queries.NPCQuery inArea(net.runelite.api.coords.WorldArea);
}
```
