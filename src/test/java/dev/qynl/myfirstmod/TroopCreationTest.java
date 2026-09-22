package dev.qynl.myfirstmod;

import dev.qynl.myfirstmod.network.ModPackets;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TroopCreationTest {

    @Test
    void testSanitizeId() {
        assertEquals("frost_valkyrie", ModPackets.sanitizeId("Frost_Valkyrie"));
        assertEquals("goblin_sapper_99", ModPackets.sanitizeId("  Goblin_Sapper_99  "));
        assertNull(ModPackets.sanitizeId("invalid-id-with-dash!"));
    }

    @Test
    void testUnitDefinitionInstantiation() {
        UnitDefinition def = new UnitDefinition("frost_valkyrie", "Frost Valkyrie", Identifier.of("minecraft", "skeleton"));
        def.factionId = "kingdom";
        def.role = "ranged";
        def.maxHealth = 45.0f;
        def.attackDamage = 8.0f;
        def.scale = 1.2f;

        assertEquals("frost_valkyrie", def.id);
        assertEquals("Frost Valkyrie", def.name);
        assertEquals(Identifier.of("minecraft", "skeleton"), def.entityId);
        assertEquals("kingdom", def.factionId);
        assertEquals("ranged", def.role);
        assertEquals(45.0f, def.maxHealth, 0.001);
        assertEquals(1.2f, def.scale, 0.001);
    }
}
