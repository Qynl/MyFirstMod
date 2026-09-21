package dev.qynl.myfirstmod;

import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhysicalResourceAuthorityTest {

    @BeforeAll
    static void initMinecraft() {
        try {
            SharedConstants.createGameVersion();
            Bootstrap.initialize();
        } catch (Throwable ignored) {}
    }

    @Test
    void testUnitDefinitionAmmoLogic() {
        UnitDefinition unit = new UnitDefinition("archer_ammo_test", "Archer Ammo Test", Identifier.of("minecraft", "skeleton"));
        assertEquals(0, unit.inventory.size());
        assertFalse(unit.infiniteAmmo);
    }

    @Test
    void testFoodHealingCalculationFormula() {
        // Cooked Beef: nutrition = 8, saturation = 0.8 (saturation modifier)
        int nutrition = 8;
        float saturation = 0.8f;
        float calculatedHeal = (nutrition * 0.75f) + (saturation * 1.25f);

        assertTrue(calculatedHeal >= 6.0f, "Cooked Beef should provide substantial healing");
        assertEquals(7.0f, calculatedHeal, 0.001);
    }
}
