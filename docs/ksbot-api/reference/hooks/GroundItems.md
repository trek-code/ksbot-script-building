# rs.kreme.ksbot.api.hooks.GroundItems

Package: ``rs.kreme.ksbot.api.hooks``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public class rs.kreme.ksbot.api.hooks.GroundItems implements rs.kreme.ksbot.api.interfaces.Queryable<rs.kreme.ksbot.api.queries.TileItemQuery> {
  public static int[] IIIllIIliIIl;
  protected final java.util.List<rs.kreme.ksbot.api.wrappers.KSGroundItem> tileItems;
  protected int lastUpdateTick;
  public final java.util.List<rs.kreme.ksbot.api.wrappers.KSGroundItem> LOOT_LIST;
  protected rs.kreme.ksbot.api.KSContext ctx;
  protected final java.util.List<net.runelite.api.Actor> attackedActors;
  protected void onPlayerLootReceived(net.runelite.client.events.PlayerLootReceived);
  public rs.kreme.ksbot.api.hooks.GroundItems(rs.kreme.ksbot.api.KSContext);
  public int getCount(java.lang.String);
  public int getCount(int);
  public rs.kreme.ksbot.api.queries.Query query();
  public boolean hasItem(java.lang.String);
  public boolean hasItem(int);
  public rs.kreme.ksbot.api.wrappers.KSGroundItem getClosest(int...);
  public static java.lang.Object IliIlllilIi(java.lang.Object[]);
  public static void iliIiiliiiii();
  public rs.kreme.ksbot.api.queries.TileItemQuery query();
  protected java.util.List<rs.kreme.ksbot.api.wrappers.KSGroundItem> convertToGroundItems(java.util.Collection<net.runelite.client.game.ItemStack>);
  public boolean hasLoot();
  protected void onGameTick(net.runelite.api.events.GameTick);
  static {};
  protected void onNpcLootReceived(net.runelite.client.events.NpcLootReceived);
  protected void onItemDespawned(net.runelite.api.events.ItemDespawned);
  public rs.kreme.ksbot.api.wrappers.KSGroundItem getClosest(java.lang.String...);
}
```
