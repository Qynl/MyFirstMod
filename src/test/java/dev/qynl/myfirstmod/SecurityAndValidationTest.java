package dev.qynl.myfirstmod;

import dev.qynl.myfirstmod.network.ModPackets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAndValidationTest {

    @Test
    void testSanitizeId() {
        assertEquals("kingdom", ModPackets.sanitizeId("kingdom"));
        assertEquals("royal_knight", ModPackets.sanitizeId("ROYAL_KNIGHT"));
        assertEquals("undead_legion", ModPackets.sanitizeId("  undead_legion  "));
        assertNull(ModPackets.sanitizeId(null));
        assertNull(ModPackets.sanitizeId("invalid-id!"));
        assertNull(ModPackets.sanitizeId("DROP TABLE;--"));
    }

    @Test
    void testAttributeClampingBounds() {
        float rawHealth = 99999.0f;
        float clampedHealth = Math.max(1.0f, Math.min(1000.0f, rawHealth));
        assertEquals(1000.0f, clampedHealth);

        float rawDamage = -50.0f;
        float clampedDamage = Math.max(0.0f, Math.min(200.0f, rawDamage));
        assertEquals(0.0f, clampedDamage);

        float rawScale = 50.0f;
        float clampedScale = Math.max(0.2f, Math.min(5.0f, rawScale));
        assertEquals(5.0f, clampedScale);
    }
}
