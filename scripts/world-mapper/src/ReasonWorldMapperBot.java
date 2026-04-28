package reason.mapper;

import rs.kreme.ksbot.api.scripts.Category;
import rs.kreme.ksbot.api.scripts.ScriptManifest;

@ScriptManifest(
        name = "Reason World Mapper",
        author = "MindMyLogic",
        servers = {"Reason"},
        description = "Manual-guided backend mapper for route capture, nearest-target capture, and auto waypoint recording.",
        version = 0.43,
        uid = "reason-world-mapper",
        category = Category.OTHER
)
public class ReasonWorldMapperBot extends WorldMapperBot {
}
