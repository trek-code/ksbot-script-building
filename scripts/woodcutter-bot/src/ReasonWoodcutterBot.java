package reason.woodcutter;

import rs.kreme.ksbot.api.scripts.Category;
import rs.kreme.ksbot.api.scripts.ScriptManifest;

@ScriptManifest(
        name = "Cutter of wood",
        author = "MindMyLogic",
        servers = {"Reason"},
        description = "Minimal route-based woodcutter core for Reason.",
        version = 3.3,
        uid = "reason-woodcutter-bot",
        category = Category.OTHER
)
public class ReasonWoodcutterBot extends WoodcutterBot {
}
