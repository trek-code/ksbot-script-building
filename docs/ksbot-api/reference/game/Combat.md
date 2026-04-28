# rs.kreme.ksbot.api.game.Combat

Package: ``rs.kreme.ksbot.api.game``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public class rs.kreme.ksbot.api.game.Combat {
  public static int[] IIIllIIliIIl;
  public int getMissingHealth();
  public boolean hasDebuff();
  public boolean isRetaliating();
  public boolean isAntifired();
  public boolean inCombat();
  public void enableSpecial();
  public boolean isTargetAlive();
  public int getSpecEnergy();
  public boolean isAttacking(rs.kreme.ksbot.api.wrappers.KSNPC);
  public int getCurrentHealth();
  public boolean isVenomed();
  public void setRetaliate(boolean);
  public boolean isSpecEnabled();
  public double getHealthPercent();
  public boolean isFullHealth();
  public boolean specialEnabled();
  public boolean isLowHealth(int);
  public boolean shouldSpecAttack(int);
  public rs.kreme.ksbot.api.game.Combat$AttackStyle getAttackStyle();
  public rs.kreme.ksbot.api.queries.NPCQuery getAttackableNPC(java.lang.String...);
  public void special(boolean);
  public void setAttackStyle(rs.kreme.ksbot.api.game.Combat$AttackStyle);
  public boolean isPoisoned();
  public rs.kreme.ksbot.api.queries.NPCQuery getAttackableNPC(int...);
  public boolean isUnderAttack();
  public static void iliIiiliiiii();
  public net.runelite.api.Actor getTarget();
  public rs.kreme.ksbot.api.game.Combat(rs.kreme.ksbot.api.KSContext);
  static {};
  public rs.kreme.ksbot.api.wrappers.KSNPC getCurrentTarget();
  public static java.lang.Object IliIlllilIi(java.lang.Object[]);
  public int getEnemyCount();
  public boolean isSuperAntifired();
}
```
