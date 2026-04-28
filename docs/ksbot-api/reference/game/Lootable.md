# rs.kreme.ksbot.api.game.Lootable

Package: ``rs.kreme.ksbot.api.game``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public class rs.kreme.ksbot.api.game.Lootable {
  public static boolean LOOT_PRAYER_POTIONS;
  public static int[] IIIllIIliIIl;
  public static boolean LOOT_FOOD;
  public void invalidate();
  public rs.kreme.ksbot.api.game.Lootable$Priority getPriority(rs.kreme.ksbot.api.wrappers.KSGroundItem);
  public rs.kreme.ksbot.api.KSContext getCtx();
  public boolean contains(int);
  public java.lang.String[] getItems();
  public boolean pickup(java.lang.String...);
  public void addItem(rs.kreme.ksbot.api.game.Lootable$Priority, java.lang.String...);
  public java.util.HashMap<java.lang.String, rs.kreme.ksbot.api.game.Lootable$Priority> getLootations();
  public java.util.HashMap<java.lang.String, java.lang.Integer> getDrops();
  public void setPickupOwn(boolean);
  static {};
  public void reset();
  public java.util.HashMap<java.lang.String, java.lang.Integer> getAllDrops();
  public void removeItem(int...);
  public rs.kreme.ksbot.api.game.Lootable(rs.kreme.ksbot.api.KSContext);
  public void removeItem(java.lang.String...);
  public void setDistance(int);
  public void addDrop(rs.kreme.ksbot.api.wrappers.KSItem);
  public boolean pickup();
  public void addItem(rs.kreme.ksbot.api.game.Lootable$Priority, int...);
  public boolean isPickupOwn();
  public int getDistance();
  public static void iliIiiliiiii();
  public boolean contains(java.lang.String);
  public java.lang.String[] getDROPPABLE_ITEMS();
  public boolean pickup(int...);
  public int getDropCount(java.lang.String);
  public java.util.List<java.util.function.Consumer<rs.kreme.ksbot.api.wrappers.KSItem>> getDropListeners();
  public void addDrop(rs.kreme.ksbot.api.wrappers.KSGroundItem);
  public void updatePriority(java.lang.String, rs.kreme.ksbot.api.game.Lootable$Priority);
  public void onDrop(java.util.function.Consumer<rs.kreme.ksbot.api.wrappers.KSItem>);
  public static java.lang.Object IliIlllilIi(java.lang.Object[]);
  public int getTotalDropCount();
}
```
