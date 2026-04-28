package rsperkfarmerv103;

/**
 * High-level state machine for the RS Perk Farmer loop. The names are
 * deliberately human-readable because they are rendered directly in the
 * "Current action" / "Next action" status labels.
 */
public enum BotState {
    STARTING             ("Starting up",                "Check equipment"),
    CHECK_EQUIPMENT      ("Checking pickaxe",           "Walk to Perk Master"),
    WALK_TO_NPC          ("Walking to Perk Master",     "Request task"),
    REQUEST_TASK         ("Requesting perk task",       "Choose Skilling"),
    CHOOSE_TASK_TYPE     ("Choosing Skilling",          "Choose Elite difficulty"),
    CHOOSE_DIFFICULTY    ("Choosing Elite difficulty",  "Choose Runite task"),
    CHOOSE_RESOURCE      ("Choosing Runite ore task",   "Walk to runite"),
    WALK_TO_RESOURCE     ("Walking to Runite ore",      "Mining runite"),
    MINING               ("Mining runite ore",          "Bank ore"),
    WALK_TO_BANK         ("Walking to Bank chest",      "Deposit ore"),
    BANKING              ("Depositing ore",             "Return to Perk Master"),
    SPRING_EVENT         ("Catching Spring creature",   "Resume previous action"),
    TASK_ALREADY_ACTIVE  ("Existing task detected",     "Walk to runite"),
    FAULT                ("Fault — script stopped",     "—"),
    IDLE                 ("Idle",                       "—");

    private final String current;
    private final String next;

    BotState(String current, String next) {
        this.current = current;
        this.next = next;
    }

    public String currentAction() { return current; }
    public String nextAction()    { return next; }
}
