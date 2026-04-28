# rs.kreme.ksbot.api.interfaces.Locatable

Package: ``rs.kreme.ksbot.api.interfaces``

Generated from:
- ``D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar``

## Public Signature Dump

```text
public interface rs.kreme.ksbot.api.interfaces.Locatable extends rs.kreme.ksbot.api.interfaces.Positionable {
  public default int distanceTo(rs.kreme.ksbot.api.interfaces.Locatable);
  public default int getWorldY();
  public default int getWorldX();
  public default net.runelite.api.coords.LocalPoint getLocalLocation();
  public default int distanceTo(net.runelite.api.coords.WorldPoint);
  public default int getPlane();
  public abstract net.runelite.api.coords.WorldPoint getWorldLocation();
}
```
