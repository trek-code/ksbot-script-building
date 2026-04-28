# rs.kreme.ksbot.api.game.Consumables

Package: ``rs.kreme.ksbot.api.game``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public class rs.kreme.ksbot.api.game.Consumables {
  protected rs.kreme.ksbot.api.commons.Timer antiFireTimer;
  protected int brewsDrunk;
  public static final java.lang.String TIMER_GALVEK_EYE;
  protected static final int GALVEK_EYE_DURATION_MS;
  protected static final int UNDEAD_HEART_DURATION_MS;
  protected static final int OVERLOAD_DURATION_MS;
  protected java.util.Set<java.lang.String> unedible;
  protected rs.kreme.ksbot.api.commons.Timer posionTimer;
  protected final java.util.concurrent.ConcurrentHashMap<java.lang.String, rs.kreme.ksbot.api.commons.Timer> timers;
  public static final java.lang.String TIMER_OVERLOAD;
  public static int[] IIIllIIliIIl;
  protected rs.kreme.ksbot.api.commons.Timer antiVenomTimer;
  public static final java.lang.String TIMER_UNDEAD_HEART;
  protected rs.kreme.ksbot.api.KSContext ctx;
  public boolean comboEat();
  public boolean isTimerActive(java.lang.String);
  public boolean eat(int);
  public static java.lang.Object IliIlllilIi(java.lang.Object[]);
  public boolean isInCooldown(rs.kreme.ksbot.api.game.Consumables$PotionType);
  public boolean needsAntipoison();
  static {};
  public void drinkSpecial();
  public rs.kreme.ksbot.api.commons.Timer startTimer(java.lang.String, long);
  public boolean hasPoisonImmunity();
  public int getFoodCount();
  public java.util.Set<java.lang.String> getUnedible();
  public boolean drinkPrayer();
  public boolean hasAntifire();
  public int getBrewsDrunk();
  public boolean isTimerFinished(java.lang.String);
  public rs.kreme.ksbot.api.commons.Timer getTimer(java.lang.String);
  public boolean hasPrayer();
  public boolean eat(java.lang.String);
  public boolean hasKarambwan();
  public int getPotionCount(rs.kreme.ksbot.api.game.Consumables$PotionType);
  public void clearAllTimers();
  public java.util.Set<java.lang.String> getActiveTimerKeys();
  public long getTimerRemaining(java.lang.String);
  public void purgeFinishedTimers();
  public boolean hasPotion(rs.kreme.ksbot.api.game.Consumables$PotionType);
  public boolean hasFood();
  public boolean isOverloaded();
  public boolean eatAtPercent(int);
  public boolean needsAntifire();
  protected void applyEffect(rs.kreme.ksbot.api.game.Consumables$EffectType);
  public static void iliIiiliiiii();
  public boolean restoreStats();
  public boolean needsAntivenom();
  public void onChatMessage(net.runelite.api.events.ChatMessage);
  public boolean hasVenomImmunity();
  public boolean drinkPrayerAtPercent(int);
  protected void expireEffect(rs.kreme.ksbot.api.game.Consumables$EffectType);
  public boolean comboEat(boolean);
  public boolean drink(rs.kreme.ksbot.api.game.Consumables$PotionType);
  public rs.kreme.ksbot.api.game.Consumables(rs.kreme.ksbot.api.KSContext);
  public void resetBrewCount();
  public boolean needsRestore();
  public void clearTimer(java.lang.String);
  public int getPrayerCount();
  public boolean eat();
}
```
