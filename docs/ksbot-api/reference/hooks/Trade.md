# rs.kreme.ksbot.api.hooks.Trade

Package: ``rs.kreme.ksbot.api.hooks``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public class rs.kreme.ksbot.api.hooks.Trade {
  final java.util.function.Supplier<rs.kreme.ksbot.api.wrappers.KSWidget> THEIR_VALUE_CONFIRM;
  final java.util.function.Supplier<rs.kreme.ksbot.api.wrappers.KSWidget> THEIR_VALUE_TRADE;
  final java.util.function.Supplier<rs.kreme.ksbot.api.wrappers.KSWidget> YOUR_VALUE_TRADE;
  final java.util.function.Supplier<rs.kreme.ksbot.api.wrappers.KSWidget> ACCEPT_BTN_CONFIRM;
  public static int[] IIIllIIliIIl;
  final java.util.function.Supplier<rs.kreme.ksbot.api.wrappers.KSWidget> DECLINE_BTN_TRADE;
  final java.util.function.Supplier<rs.kreme.ksbot.api.wrappers.KSWidget> ACCEPT_STATUS_CONFIRM;
  final java.util.function.Supplier<rs.kreme.ksbot.api.wrappers.KSWidget> ACCEPT_BTN_TRADE;
  final java.util.function.Supplier<rs.kreme.ksbot.api.wrappers.KSWidget> THEIR_ITEMS_TRADE;
  final java.util.function.Supplier<rs.kreme.ksbot.api.wrappers.KSWidget> ACCEPT_STATUS_TRADE;
  final java.util.function.Supplier<rs.kreme.ksbot.api.wrappers.KSWidget> YOUR_ITEMS_TRADE;
  final java.util.function.Supplier<rs.kreme.ksbot.api.wrappers.KSWidget> DECLINE_BTN_CONFIRM;
  final java.util.function.Supplier<rs.kreme.ksbot.api.wrappers.KSWidget> YOUR_VALUE_CONFIRM;
  public boolean waitingForThem();
  public boolean isConfirmScreen();
  public void offer(rs.kreme.ksbot.api.wrappers.KSItem, int);
  public boolean waitingForYou();
  public long getLastTradeReceivedTime();
  public int getYourTradeValue();
  public rs.kreme.ksbot.api.wrappers.KSPlayer getLastTradeReceivedPlayer();
  public static void iliIiiliiiii();
  public boolean sendTradeOffer(rs.kreme.ksbot.api.wrappers.KSPlayer);
  public void onChatMessage(net.runelite.api.events.ChatMessage);
  public void acceptTrade();
  public int getTradeState();
  public void remove(int, int);
  static {};
  public void offer(java.lang.String, int);
  public int getTheirTradeValue();
  public rs.kreme.ksbot.api.queries.ItemQuery query(boolean);
  public void remove(rs.kreme.ksbot.api.wrappers.KSItem, int);
  public boolean hasActiveTradeRequest();
  public void remove(java.lang.String, int);
  public void declineTrade();
  public static java.lang.Object IliIlllilIi(java.lang.Object[]);
  public boolean isOpen();
  public void offer(int, int);
  public rs.kreme.ksbot.api.hooks.Trade(rs.kreme.ksbot.api.KSContext);
  public boolean sendTradeOffer(java.lang.String);
}
```
