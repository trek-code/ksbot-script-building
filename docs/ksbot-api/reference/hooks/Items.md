# rs.kreme.ksbot.api.hooks.Items

Package: ``rs.kreme.ksbot.api.hooks``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public abstract class rs.kreme.ksbot.api.hooks.Items implements rs.kreme.ksbot.api.interfaces.Queryable<rs.kreme.ksbot.api.queries.ItemQuery> {
  protected net.runelite.api.InventoryID inventoryID;
  protected java.util.List<rs.kreme.ksbot.api.wrappers.KSItem> items;
  protected final java.util.function.Function<rs.kreme.ksbot.api.wrappers.KSItem, java.lang.Boolean> modification;
  protected int lastUpdateTick;
  protected final rs.kreme.ksbot.api.KSContext ctx;
  public int getCount(java.lang.String);
  public boolean onlyContains(int...);
  public int getCount(int);
  public boolean containsAll(int...);
  public int getCount(java.util.function.Predicate<? super rs.kreme.ksbot.api.wrappers.KSItem>);
  public boolean contains(java.lang.String...);
  public int getCount(java.util.List<?>);
  public boolean interact(java.lang.String, java.lang.String);
  public rs.kreme.ksbot.api.queries.ItemQuery query();
  public int getEmptySlots();
  protected rs.kreme.ksbot.api.hooks.Items(net.runelite.api.InventoryID, java.util.function.Function<rs.kreme.ksbot.api.wrappers.KSItem, java.lang.Boolean>);
  public java.util.List<rs.kreme.ksbot.api.wrappers.KSItem> getItems(java.util.function.Predicate<? super rs.kreme.ksbot.api.wrappers.KSItem>);
  public boolean isFull();
  public boolean interact(int, java.lang.String);
  public int getTotalQuantity(int);
  public boolean isEmpty();
  public boolean containsAll(java.lang.String...);
  public boolean contains(int...);
  public boolean onlyContains(java.lang.String...);
  public rs.kreme.ksbot.api.wrappers.KSItem getItem(java.util.List<?>);
  public boolean contains(java.util.function.Predicate<? super rs.kreme.ksbot.api.wrappers.KSItem>);
  public rs.kreme.ksbot.api.wrappers.KSItem getItem(java.lang.String...);
  public rs.kreme.ksbot.api.wrappers.KSItem getItem(int...);
  public int getTotalQuantity(java.lang.String);
  public boolean contains(java.util.List<?>);
  public rs.kreme.ksbot.api.queries.Query query();
  public int size();
  public boolean containsAll(java.util.List<?>);
}
```
