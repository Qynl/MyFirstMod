package dev.qynl.myfirstmod;

import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhysicalResourceAuthorityTest {

    @Test
    void testUnitInventoryStackingAndAmmunition() {
        UnitDefinition unit = new UnitDefinition("archer_ammo_test", "Archer Ammo Test", Identifier.of("minecraft", "skeleton"));
        unit.inventory.add(new ItemStack(Items.ARROW, 32));
        unit.inventory.add(new ItemStack(Items.SPECTRAL_ARROW, 16));

        int totalArrows = unit.inventory.stream()
                .filter(s -> s.isOf(Items.ARROW) || s.isOf(Items.SPECTRAL_ARROW))
                .mapToInt(ItemStack::getCount)
                .sum();

        assertEquals(48, totalArrows);
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
