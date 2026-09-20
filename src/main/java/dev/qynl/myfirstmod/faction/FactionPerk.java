package dev.qynl.myfirstmod.faction;

import java.util.Arrays;
import java.util.List;

public enum FactionPerk {
    MILITARY_DISCIPLINE("military_discipline", "Military Discipline", "+15% unit attack damage in combat.", 0x3B82F6),
    HEAVY_ARMOR("heavy_armor", "Heavy Armor", "+20% armor rating and bonus knockback resistance.", 0x64748B),
    SWIFT_ARMY("swift_army", "Swift Army", "+15% army movement and pursuit speed.", 0x10B981),
    RALLY("rally", "Rallying Cry", "Commanders sound war horns granting Strength II & Speed II.", 0xF59E0B),
    REGENERATION("regeneration", "Battle Regeneration", "All units steadily regenerate health over time.", 0xEC4899),
    NIGHT_FIGHTERS("night_fighters", "Night Fighters", "Gains Night Vision and +25% combat power during night.", 0x8B5CF6),
    VETERANS("veterans", "Hardened Veterans", "Surviving warriors gain permanent bonus max health and attack.", 0xD97706),
    PYROTECHNICS("pyrotechnics", "Pyrotechnics", "Artillery fireworks deal +50% explosive splash damage.", 0xEF4444),
    HOLY_MIGHT("holy_might", "Holy Might", "Healers & medics heal +50% more HP with wider reach.", 0x06B6D4),
    FORTIFICATION("fortification", "Iron Fortification", "Reduces all incoming damage taken by 15%.", 0x475569),
    NECROMANCY("necromancy", "Dark Necromancy", "Slain warriors rise as friendly undead thralls.", 0x7C3AED),
    BERSERK_FURY("berserk_fury", "Berserk Fury", "Injured units enter Bloodrage (+50% attack & speed).", 0xDC2626),
    WAR_SONG("war_song", "Harmonic War Song", "Allied units in battle gain passive speed & regen.", 0x14B8A6),
    FIRE_MASTERY("fire_mastery", "Fire Mastery", "Attacks ignite targets and units are immune to fire.", 0xF97316);

    private final String id;
    private final String displayName;
    private final String description;
    private final int colorRgb;

    FactionPerk(String id, String displayName, String description, int colorRgb) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.colorRgb = colorRgb;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getColorRgb() {
        return colorRgb;
    }

    public static FactionPerk fromId(String id) {
        for (FactionPerk perk : values()) {
            if (perk.id.equalsIgnoreCase(id) || perk.name().equalsIgnoreCase(id)) {
                return perk;
            }
        }
        return null;
    }

    public static List<FactionPerk> getAll() {
        return Arrays.asList(values());
    }
}
