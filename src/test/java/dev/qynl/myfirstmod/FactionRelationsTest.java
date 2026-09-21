package dev.qynl.myfirstmod;

import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.faction.FactionPerk;
import dev.qynl.myfirstmod.faction.FactionRelation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FactionRelationsTest {
    private Faction kingdom;
    private Faction raiders;
    private Faction villagers;

    @BeforeEach
    void setUp() {
        kingdom = new Faction("kingdom", "Kingdom of Eldoria", "#3B82F6", "Defenders");
        raiders = new Faction("raiders", "Iron Raiders", "#EF4444", "Raiders");
        villagers = new Faction("villagers", "Village Alliance", "#10B981", "Villagers");

        kingdom.relations.put("raiders", FactionRelation.HOSTILE);
        kingdom.relations.put("villagers", FactionRelation.ALLIED);

        raiders.relations.put("kingdom", FactionRelation.HOSTILE);
        raiders.relations.put("villagers", FactionRelation.HOSTILE);

        villagers.relations.put("kingdom", FactionRelation.ALLIED);
        villagers.relations.put("raiders", FactionRelation.HOSTILE);
    }

    @Test
    void testFactionInitializationAndColorParsing() {
        assertEquals("kingdom", kingdom.id);
        assertEquals("Kingdom of Eldoria", kingdom.name);
        assertEquals(0x3B82F6, kingdom.getParsedColor());
        assertEquals(0xEF4444, raiders.getParsedColor());
        assertEquals(0x10B981, villagers.getParsedColor());
    }

    @Test
    void testFactionRelations() {
        assertEquals(FactionRelation.HOSTILE, kingdom.relations.get("raiders"));
        assertEquals(FactionRelation.ALLIED, kingdom.relations.get("villagers"));
        assertEquals(FactionRelation.HOSTILE, raiders.relations.get("kingdom"));
        assertNull(kingdom.relations.get("unknown_faction"));
    }

    @Test
    void testFactionPerks() {
        kingdom.perks.add("military_discipline");
        kingdom.perks.add("heavy_armor");

        assertTrue(kingdom.hasPerk(FactionPerk.MILITARY_DISCIPLINE));
        assertTrue(kingdom.hasPerk(FactionPerk.HEAVY_ARMOR));
        assertFalse(kingdom.hasPerk(FactionPerk.NECROMANCY));
    }
}
