package local.reason.woodcutter;

import rs.kreme.ksbot.api.scripts.Category;
import rs.kreme.ksbot.api.scripts.ScriptManifest;

@ScriptManifest(
        name = "Cutter of wood",
        author = "MindMyLogic",
        servers = {"Reason"},
        description = "Single-window route-based woodcutter for Reason with powercut and banking modes.",
        version = 1.3,
        uid = "reason-woodcutter-bot",
        category = Category.OTHER
)
public class ReasonWoodcutterBot extends WoodcutterBot {
}
