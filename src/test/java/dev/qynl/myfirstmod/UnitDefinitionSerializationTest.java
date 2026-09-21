package dev.qynl.myfirstmod;

import com.google.gson.JsonObject;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnitDefinitionSerializationTest {

    @Test
    void testUnitDefinitionInstantiation() {
        UnitDefinition unit = new UnitDefinition("paladin_test", "Paladin Test", Identifier.of("minecraft", "villager"));
        unit.factionId = "kingdom";
        unit.maxHealth = 60.0f;
        unit.attackDamage = 8.5f;
        unit.armor = 14.0f;
        unit.scale = 1.2f;
        unit.role = "paladin";
        unit.rank = "captain";
        unit.mount = "minecraft:horse";
        unit.particleAura = "totem";
        unit.deathAction = "healing_mist";

        assertEquals("paladin_test", unit.id);
        assertEquals("Paladin Test", unit.name);
        assertEquals("kingdom", unit.factionId);
        assertEquals(60.0f, unit.maxHealth);
        assertEquals(8.5f, unit.attackDamage);
        assertEquals(14.0f, unit.armor);
        assertEquals(1.2f, unit.scale);
        assertEquals("paladin", unit.role);
        assertEquals("captain", unit.rank);
        assertEquals("minecraft:horse", unit.mount);
        assertEquals("totem", unit.particleAura);
        assertEquals("healing_mist", unit.deathAction);
    }

    @Test
    void testJsonSerializationRoundtrip() {
        UnitDefinition unit = new UnitDefinition("berserker_test", "Berserker Test", Identifier.of("minecraft", "piglin_brute"));
        unit.factionId = "raiders";
        unit.maxHealth = 45.0f;
        unit.attackDamage = 10.0f;
        unit.armor = 12.0f;
        unit.role = "berserker";
        unit.rank = "warlord";
        unit.commander = true;

        JsonObject json = unit.toJson();
        assertNotNull(json);
        assertEquals("berserker_test", json.get("id").getAsString());

        UnitDefinition deserialized = UnitDefinition.fromJson(json);
        assertNotNull(deserialized);
        assertEquals("berserker_test", deserialized.id);
        assertEquals("Berserker Test", deserialized.name);
        assertEquals("raiders", deserialized.factionId);
        assertEquals(45.0f, deserialized.maxHealth);
        assertEquals(10.0f, deserialized.attackDamage);
        assertEquals(12.0f, deserialized.armor);
        assertEquals("berserker", deserialized.role);
        assertEquals("warlord", deserialized.rank);
        assertTrue(deserialized.commander);
    }
}
