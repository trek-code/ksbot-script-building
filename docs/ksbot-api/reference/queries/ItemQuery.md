# rs.kreme.ksbot.api.queries.ItemQuery

Package: ``rs.kreme.ksbot.api.queries``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public class rs.kreme.ksbot.api.queries.ItemQuery extends rs.kreme.ksbot.api.queries.Query<rs.kreme.ksbot.api.wrappers.KSItem, rs.kreme.ksbot.api.queries.ItemQuery> {
  public static int[] IIIllIIliIIl;
  protected java.lang.String getName(java.lang.Object);
  public rs.kreme.ksbot.api.queries.ItemQuery withinPriceRange(int, int);
  public rs.kreme.ksbot.api.queries.ItemQuery omitContains(java.lang.String);
  public rs.kreme.ksbot.api.queries.ItemQuery untradeable();
  public static void iliIiiliiiii();
  protected java.lang.String[] getOptions(java.lang.Object);
  public rs.kreme.ksbot.api.queries.ItemQuery priceLessThan(int);
  static {};
  public rs.kreme.ksbot.api.queries.ItemQuery tradeable();
  protected java.lang.String getName(rs.kreme.ksbot.api.wrappers.KSItem);
  public rs.kreme.ksbot.api.queries.ItemQuery filterContains(java.lang.String...);
  public rs.kreme.ksbot.api.queries.ItemQuery quantityEquals(int);
  public rs.kreme.ksbot.api.queries.ItemQuery quantityLessThan(int);
  public rs.kreme.ksbot.api.queries.ItemQuery priceOver(int);
  protected java.lang.String[] getOptions(rs.kreme.ksbot.api.wrappers.KSItem);
  public rs.kreme.ksbot.api.queries.ItemQuery differenceInValueLessThan(int);
  public rs.kreme.ksbot.api.queries.ItemQuery quantityGreaterThan(int);
  public rs.kreme.ksbot.api.queries.ItemQuery(java.util.Collection<rs.kreme.ksbot.api.wrappers.KSItem>);
  public rs.kreme.ksbot.api.queries.ItemQuery onlyUnnoted();
  public static java.lang.Object IliIlllilIi(java.lang.Object[]);
  protected int getId(rs.kreme.ksbot.api.wrappers.KSItem);
  public rs.kreme.ksbot.api.queries.ItemQuery nonPlaceHolder();
  public rs.kreme.ksbot.api.queries.ItemQuery onlyNoted();
  protected int getId(java.lang.Object);
  public rs.kreme.ksbot.api.queries.ItemQuery withSlot(int);
  public rs.kreme.ksbot.api.queries.ItemQuery onlyStackable();
}
```
