# rs.kreme.ksbot.api.interfaces.Interactable

Package: ``rs.kreme.ksbot.api.interfaces``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public interface rs.kreme.ksbot.api.interfaces.Interactable {
  public static final rs.kreme.ksbot.api.KSContext ctx;
  public abstract boolean interact(java.lang.String...);
  public abstract void interact(int, net.runelite.api.MenuAction, int, int);
  public abstract void interact(net.runelite.api.MenuAction);
  public default boolean hasAction(java.lang.String);
  static {};
  public default java.awt.geom.Rectangle2D getClickBounds();
  public abstract boolean interact(java.lang.String);
  public abstract void interact(int);
  public default int getOptionIndex(java.lang.String);
  public abstract java.lang.String[] getActions();
  public abstract void interact(int, net.runelite.api.MenuAction);
}
```
