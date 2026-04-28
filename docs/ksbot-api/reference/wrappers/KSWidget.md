# rs.kreme.ksbot.api.wrappers.KSWidget

Package: ``rs.kreme.ksbot.api.wrappers``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public class rs.kreme.ksbot.api.wrappers.KSWidget implements rs.kreme.ksbot.api.interfaces.Interactable,rs.kreme.ksbot.api.interfaces.Identifiable {
  public static int[] IIIllIIliIIl;
  public void interact(rs.kreme.ksbot.api.wrappers.KSWidget$WidgetOption);
  public void interact(net.runelite.api.MenuAction, int, int, int);
  public java.lang.String getText();
  public boolean interact(java.lang.String);
  public int getBorderType();
  public rs.kreme.ksbot.api.wrappers.KSWidget getDynamicChild(int);
  public int getParentId();
  public void interact(net.runelite.api.MenuAction);
  public boolean hasListener();
  public boolean hasText(java.lang.String);
  public net.runelite.api.Point getCanvasLocation();
  public java.lang.String toString();
  public void interact(int, net.runelite.api.MenuAction);
  public rs.kreme.ksbot.api.wrappers.KSWidget findChild(java.util.function.Predicate<rs.kreme.ksbot.api.wrappers.KSWidget>);
  public rs.kreme.ksbot.api.wrappers.KSWidget[] getStaticChildren();
  public rs.kreme.ksbot.api.wrappers.KSWidget getParent();
  public rs.kreme.ksbot.api.wrappers.KSWidget[] getChildren();
  public int getOptionIndex(java.lang.String);
  public void interact(int, int);
  public rs.kreme.ksbot.api.wrappers.KSWidget(net.runelite.api.widgets.Widget);
  public rs.kreme.ksbot.api.wrappers.KSWidget findChildByText(java.lang.String);
  public java.lang.String[] getActions();
  public boolean isUsable();
  public int getItemQuantity();
  public int getId();
  public rs.kreme.ksbot.api.wrappers.KSWidget getChild(int);
  public static java.lang.Object IliIlllilIi(java.lang.Object[]);
  public boolean interact(java.lang.String...);
  static {};
  public static void iliIiiliiiii();
  public int getIndex();
  public rs.kreme.ksbot.api.wrappers.KSItem getItem(int);
  public rs.kreme.ksbot.api.wrappers.KSWidget[] getNestedChildren();
  public rs.kreme.ksbot.api.wrappers.KSWidget[] getDynamicChildren();
  public void interact(int);
  public net.runelite.api.SpritePixels getSprite();
  public int getSpriteId();
  public java.util.List<rs.kreme.ksbot.api.wrappers.KSItem> getItems();
  public boolean isHidden();
  public void interact(int, net.runelite.api.MenuAction, int, int);
  public rs.kreme.ksbot.api.wrappers.KSWidget getStaticChild(int);
  public void interact(net.runelite.api.MenuAction, int, int);
  public int getItemId();
  public java.lang.String getName();
  public int getModel();
  public java.awt.geom.Rectangle2D getClickBounds();
  public net.runelite.api.widgets.Widget getWidget();
}
```
