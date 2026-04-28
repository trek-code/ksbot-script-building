# rs.kreme.ksbot.api.queries.ChatQuery

Package: ``rs.kreme.ksbot.api.queries``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public class rs.kreme.ksbot.api.queries.ChatQuery extends rs.kreme.ksbot.api.queries.Query<rs.kreme.ksbot.api.wrappers.KSChatMessage, rs.kreme.ksbot.api.queries.ChatQuery> {
  public static int[] iIlIlllllill;
  public rs.kreme.ksbot.api.queries.ChatQuery withType(rs.kreme.ksbot.api.wrappers.KSChatMessage$MessageTypes);
  public rs.kreme.ksbot.api.queries.ChatQuery withoutType(rs.kreme.ksbot.api.wrappers.KSChatMessage$MessageTypes);
  public rs.kreme.ksbot.api.queries.ChatQuery fromPlayer(java.lang.String);
  protected java.lang.String getName(rs.kreme.ksbot.api.wrappers.KSChatMessage);
  public rs.kreme.ksbot.api.queries.ChatQuery withType(int);
  protected java.lang.String getName(java.lang.Object);
  public rs.kreme.ksbot.api.queries.ChatQuery contains(java.lang.String);
  public rs.kreme.ksbot.api.queries.ChatQuery betweenTimestamps(long, long);
  public rs.kreme.ksbot.api.queries.ChatQuery messagesAgo(long, java.util.concurrent.TimeUnit);
  protected java.lang.String[] getOptions(java.lang.Object);
  static {};
  public rs.kreme.ksbot.api.queries.ChatQuery inLastMinutes(long);
  public rs.kreme.ksbot.api.queries.ChatQuery withoutType(int);
  public rs.kreme.ksbot.api.queries.ChatQuery inLastSeconds(long);
  public rs.kreme.ksbot.api.queries.ChatQuery startsWith(java.lang.String);
  public static void IilliliilIIi();
  protected int getId(rs.kreme.ksbot.api.wrappers.KSChatMessage);
  protected int getId(java.lang.Object);
  protected java.lang.String[] getOptions(rs.kreme.ksbot.api.wrappers.KSChatMessage);
  public rs.kreme.ksbot.api.queries.ChatQuery(java.util.Collection<rs.kreme.ksbot.api.wrappers.KSChatMessage>);
}
```
