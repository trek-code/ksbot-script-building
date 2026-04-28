package reason.woodcutter;

import rs.kreme.ksbot.api.scripts.Category;
import rs.kreme.ksbot.api.scripts.Script;
import rs.kreme.ksbot.api.scripts.ScriptManifest;

@ScriptManifest(
        name = "Reason Woodcutter Probe",
        author = "MindMyLogic",
        servers = {"Reason"},
        description = "Minimal probe script used to verify local script discovery for the woodcutter package.",
        version = 1.0,
        uid = "reason-woodcutter-probe",
        category = Category.OTHER
)
public class ReasonWoodcutterProbeBot extends Script {
    @Override
    public boolean onStart() {
        setStatus("Probe started");
        return true;
    }

    @Override
    public int onProcess() {
        setStatus("Probe idle");
        return 250;
    }
}
