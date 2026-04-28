package pfwbh125;

import net.runelite.api.coords.WorldPoint;

/**
 * Hard-coded route for the "Donator Zone (mining rune)" profile.
 * Values mirror route-profile/donator-zone-mining-rune.json. Keeping them in
 * code means the built script needs no external JSON at runtime; the JSON is
 * kept alongside for documentation / future regeneration.
 */
public final class RouteProfile {

    private RouteProfile() {}

    // ── NPC ────────────────────────────────────────────────────────────────
    public static final int PERK_MASTER_ID = 7958;
    public static final String PERK_MASTER_NAME = "Perk Master";
    public static final WorldPoint PERK_MASTER_TILE = new WorldPoint(1273, 2585, 2);
    public static final WorldPoint FRONT_OF_NPC_TILE = new WorldPoint(1274, 2585, 2);

    // ── Bank chest ─────────────────────────────────────────────────────────
    public static final int BANK_CHEST_ID = 26711;
    public static final WorldPoint BANK_CHEST_TILE = new WorldPoint(1273, 2580, 2);
    public static final WorldPoint FRONT_OF_BANK_TILE = new WorldPoint(1274, 2580, 2);

    // ── Resource ───────────────────────────────────────────────────────────
    public static final int RUNITE_ORE_ID = 11376;
    public static final WorldPoint RUNITE_ORE_TILE = new WorldPoint(1276, 2581, 2);
    public static final WorldPoint FRONT_OF_RESOURCE_TILE = new WorldPoint(1275, 2581, 2);

    // ── Item catalogue ─────────────────────────────────────────────────────
    /** Any of these counts as a usable pickaxe when found in inventory or equipment. */
    public static final String[] PICKAXE_NAMES = {
            "3rd age pickaxe", "Crystal pickaxe", "Infernal pickaxe", "Dragon pickaxe",
            "Rune pickaxe", "Adamant pickaxe", "Mithril pickaxe", "Black pickaxe",
            "Steel pickaxe", "Iron pickaxe", "Bronze pickaxe"
    };

    /** Rune ore id + common related drops we always deposit. */
    public static final String[] DEPOSIT_KEEP_PICKAXES_ONLY = {
            "Runite ore", "Uncut sapphire", "Uncut emerald", "Uncut ruby", "Uncut diamond",
            "Amethyst", "Amethyst crystals", "Clue scroll", "Clue scroll (easy)",
            "Clue scroll (medium)", "Clue scroll (hard)", "Clue scroll (elite)",
            "Gold ore", "Coal", "Spring", "Springs"
    };
}
