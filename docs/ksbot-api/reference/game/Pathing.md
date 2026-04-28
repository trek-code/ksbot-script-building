# rs.kreme.ksbot.api.game.Pathing

Package: ``rs.kreme.ksbot.api.game``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public class rs.kreme.ksbot.api.game.Pathing {
  public static int[] IIIllIIliIIl;
  public final rs.kreme.ksbot.api.game.pathing.Reachable reachable;
  public final int[][] directionsMap;
  public boolean canMelee(net.runelite.api.coords.WorldArea, net.runelite.api.coords.WorldArea);
  public int[] toCompressedPoints(rs.kreme.ksbot.api.hooks.WorldArea...);
  public void walkToTile(rs.kreme.ksbot.api.game.Pathing$Direction, int);
  public byte getCompressedPlane(int);
  public int walkPath(net.runelite.api.coords.WorldPoint[], boolean);
  public net.runelite.api.coords.WorldPoint fromCompressed(int);
  public boolean isRunning();
  public int dxy(int, int, int);
  public int distanceTo(rs.kreme.ksbot.api.interfaces.Locatable);
  public double distanceTo(net.runelite.api.coords.LocalPoint, net.runelite.api.coords.LocalPoint);
  public short getCompressedY(int);
  public java.util.List<net.runelite.api.coords.WorldPoint> getPathToSafety(java.util.Collection<rs.kreme.ksbot.api.game.pathing.DangerousTile>);
  public boolean canMelee(rs.kreme.ksbot.api.wrappers.KSNPC);
  public boolean canReach(net.runelite.api.coords.WorldPoint);
  public short getCompressedX(int);
  public boolean walkable(net.runelite.api.coords.LocalPoint);
  public int calculateDanger(net.runelite.api.coords.WorldPoint, java.util.Collection<rs.kreme.ksbot.api.game.pathing.DangerousTile>);
  public rs.kreme.ksbot.api.game.utils.GameArea getGameRegion();
  public boolean withinDanger(net.runelite.api.coords.WorldPoint, java.util.Collection<rs.kreme.ksbot.api.game.pathing.DangerousTile>);
  public net.runelite.api.Point worldToMinimap(net.runelite.api.coords.WorldPoint);
  static {};
  public net.runelite.api.Tile toTile(net.runelite.api.coords.WorldPoint);
  public int distanceTo(net.runelite.api.coords.WorldPoint);
  public boolean withinReaction(net.runelite.api.coords.WorldPoint, java.util.Collection<rs.kreme.ksbot.api.game.pathing.DangerousTile>, int);
  public int dx(int, int);
  public int distanceTo(rs.kreme.ksbot.api.wrappers.KSPlayer);
  public int getRegion();
  public int distanceTo(rs.kreme.ksbot.api.wrappers.KSNPC);
  public double distanceTo(net.runelite.api.coords.LocalPoint, net.runelite.api.coords.WorldPoint);
  public boolean isTileLoaded(net.runelite.api.coords.WorldPoint);
  public int getTileFlag(net.runelite.api.coords.WorldPoint);
  public boolean inGameRegion(rs.kreme.ksbot.api.game.utils.GameArea);
  public java.util.ArrayList<net.runelite.api.coords.WorldPoint> toInstance(net.runelite.api.coords.WorldPoint);
  public java.util.List<net.runelite.api.coords.WorldPoint> safestPath(net.runelite.api.coords.WorldPoint, net.runelite.api.coords.WorldPoint, java.util.Collection<rs.kreme.ksbot.api.game.pathing.DangerousTile>);
  public net.runelite.api.coords.WorldPoint fromInstance(net.runelite.api.coords.WorldPoint);
  public net.runelite.api.coords.WorldPoint getTile(rs.kreme.ksbot.api.game.Pathing$Direction, int);
  public boolean inArea(rs.kreme.ksbot.api.hooks.WorldArea);
  public int dy(int, int);
  public boolean inMotion();
  public java.util.List<net.runelite.api.coords.WorldPoint> shortestPath(net.runelite.api.coords.WorldPoint, net.runelite.api.coords.WorldPoint);
  public int walkPath(net.runelite.api.coords.WorldPoint[]);
  public boolean isPathCompletelySafe(java.util.List<net.runelite.api.coords.WorldPoint>, java.util.Collection<rs.kreme.ksbot.api.game.pathing.DangerousTile>);
  public int countDangerousTiles(java.util.List<net.runelite.api.coords.WorldPoint>, java.util.Collection<rs.kreme.ksbot.api.game.pathing.DangerousTile>);
  public boolean walkable(net.runelite.api.coords.WorldPoint);
  public void face(rs.kreme.ksbot.api.interfaces.Locatable);
  public boolean onTile(net.runelite.api.coords.WorldPoint);
  public boolean walkMiniMap(net.runelite.api.coords.WorldPoint, double);
  public void face(net.runelite.api.coords.WorldPoint);
  public int getRunEnergy();
  public boolean withinReaction(net.runelite.api.coords.WorldPoint, java.util.Collection<rs.kreme.ksbot.api.game.pathing.DangerousTile>);
  public net.runelite.api.coords.LocalPoint toLocal(net.runelite.api.coords.WorldPoint);
  public int compress(net.runelite.api.coords.WorldPoint);
  public rs.kreme.ksbot.api.game.Pathing(rs.kreme.ksbot.api.KSContext);
  public int compress(int, int, int);
  public void walkPoint(net.runelite.api.coords.LocalPoint);
  public boolean canReach(rs.kreme.ksbot.api.interfaces.Locatable);
  public void walkTo(net.runelite.api.coords.WorldPoint);
  public java.util.List<net.runelite.api.coords.WorldPoint> findMeleeRangePath(net.runelite.api.NPC, java.util.Collection<rs.kreme.ksbot.api.game.pathing.DangerousTile>);
  public boolean canMelee(net.runelite.api.coords.WorldPoint);
  public net.runelite.api.coords.WorldPoint derive(net.runelite.api.coords.WorldPoint, int, int);
  public boolean canMelee(net.runelite.api.NPC);
  public void walkPoint(int, int);
  public double distanceTo(net.runelite.api.coords.WorldPoint, net.runelite.api.coords.WorldPoint);
  public boolean onTile(net.runelite.api.coords.LocalPoint);
  public net.runelite.api.coords.WorldArea derive(net.runelite.api.coords.WorldArea, int);
  public java.util.List<net.runelite.api.coords.WorldPoint> constructPath(java.util.Map<net.runelite.api.coords.WorldPoint, net.runelite.api.coords.WorldPoint>, net.runelite.api.coords.WorldPoint);
  public boolean navThroughWorldPath(java.util.List<net.runelite.api.coords.WorldPoint>);
  public java.util.List<net.runelite.api.coords.WorldPoint> shortestSafePath(net.runelite.api.coords.WorldPoint, net.runelite.api.coords.WorldPoint, java.util.Collection<rs.kreme.ksbot.api.game.pathing.DangerousTile>);
  public java.util.List<net.runelite.api.coords.WorldPoint> getWalkable();
  public void toggleRun(boolean);
  public rs.kreme.ksbot.api.hooks.WorldArea derive(rs.kreme.ksbot.api.hooks.WorldArea, int);
  public int walkPath(java.util.List<net.runelite.api.coords.WorldPoint>);
  public void walkToTile(net.runelite.api.coords.WorldPoint);
  public boolean inRegion(int...);
  public boolean navThroughLocalPath(java.util.List<net.runelite.api.coords.LocalPoint>);
  public net.runelite.api.coords.LocalPoint fromWorldInstance(net.runelite.api.coords.WorldPoint);
  public static java.lang.Object IliIlllilIi(java.lang.Object[]);
  public void walkPoint(net.runelite.api.coords.WorldPoint);
  public int[] toCompressedPoints(net.runelite.api.coords.WorldArea[]);
  public boolean inInstance();
  public net.runelite.api.coords.WorldPoint fromLocal(net.runelite.api.coords.LocalPoint);
  public static void iliIiiliiiii();
  public void walkTo(rs.kreme.ksbot.api.interfaces.Locatable);
  public boolean isReachable(net.runelite.api.coords.WorldPoint);
  public int distanceTo(rs.kreme.ksbot.api.wrappers.KSObject);
}
```
