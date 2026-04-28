# rs.kreme.ksbot.api.game.pathing.MovementFlags

Package: ``rs.kreme.ksbot.api.game.pathing``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public final class rs.kreme.ksbot.api.game.pathing.MovementFlags extends java.lang.Enum<rs.kreme.ksbot.api.game.pathing.MovementFlags> {
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_LINE_OF_SIGHT_NORTH;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_LINE_OF_SIGHT_FULL;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_MOVEMENT_FLOOR_DECORATION;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_MOVEMENT_NORTH;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_MOVEMENT_FLOOR;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_MOVEMENT_SOUTH;
  public static final java.util.Set<rs.kreme.ksbot.api.game.pathing.MovementFlags> NOT_WALKABLE;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_MOVEMENT_NORTH_WEST;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_MOVEMENT_FULL;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_MOVEMENT_SOUTH_EAST;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_MOVEMENT_OBJECT;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_LINE_OF_SIGHT_SOUTH;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_MOVEMENT_NORTH_EAST;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_LINE_OF_SIGHT_EAST;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_MOVEMENT_EAST;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_MOVEMENT_WEST;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_LINE_OF_SIGHT_WEST;
  public static int[] IIIllIIliIIl;
  public static final rs.kreme.ksbot.api.game.pathing.MovementFlags BLOCK_MOVEMENT_SOUTH_WEST;
  public static int getRawFlags(net.runelite.api.coords.WorldPoint);
  public static boolean isWalkable(net.runelite.api.coords.LocalPoint);
  public static boolean isBlocked(net.runelite.api.coords.WorldPoint, rs.kreme.ksbot.api.game.pathing.MovementFlags);
  public static boolean isWalkable(net.runelite.api.coords.WorldPoint);
  public static java.lang.Object IliIlllilIi(java.lang.Object[]);
  public static void iliIiiliiiii();
  public static rs.kreme.ksbot.api.game.pathing.MovementFlags[] values();
  public static rs.kreme.ksbot.api.game.pathing.MovementFlags valueOf(java.lang.String);
  public int getFlag();
  public static java.util.Set<rs.kreme.ksbot.api.game.pathing.MovementFlags> getSetFlags(net.runelite.api.coords.WorldPoint);
  static {};
  public static java.util.Set<rs.kreme.ksbot.api.game.pathing.MovementFlags> getSetFlags(int);
  public static java.util.List<rs.kreme.ksbot.api.game.pathing.MovementFlags> getListFlags(int);
}
```
