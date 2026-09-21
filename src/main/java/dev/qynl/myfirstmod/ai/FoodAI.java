package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.equipment.ItemCapabilities;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public final class FoodAI {
    private FoodAI() {}

    public static void executeFood(ServerWorld world, MobEntity mob, UnitDefinition unit) {
        if (mob == null || mob.getHealth() >= mob.getMaxHealth() * 0.70f || mob.age % 40 != 0) return;

        // 1. Check main hand
        ItemStack mainHand = mob.getMainHandStack();
        if (ItemCapabilities.isFood(mainHand)) {
            consumeFood(world, mob, mainHand, EquipmentSlot.MAINHAND, -1);
            return;
        }

        // 2. Check off hand
        ItemStack offHand = mob.getOffHandStack();
        if (ItemCapabilities.isFood(offHand)) {
            consumeFood(world, mob, offHand, EquipmentSlot.OFFHAND, -1);
            return;
        }

        // 3. Check physical inventory
        if (mob instanceof InventoryOwner owner) {
            for (int i = 0; i < owner.getInventory().size(); i++) {
                ItemStack stack = owner.getInventory().getStack(i);
                if (ItemCapabilities.isFood(stack)) {
                    consumeFood(world, mob, stack, null, i);
                    return;
                }
            }
        }
    }

    private static void consumeFood(ServerWorld world, MobEntity mob, ItemStack stack, EquipmentSlot slot, int invIndex) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        float healAmount = 5.0f;
        if (food != null) {
            // Authentic Minecraft nutrition and saturation calculation
            healAmount = (food.nutrition() * 0.75f) + (food.saturation() * 1.25f);
        }

        mob.heal(healAmount);
        dev.qynl.myfirstmod.visual.FloatingCombatText.spawnHeal(world, mob.getX(), mob.getBodyY(0.7), mob.getZ(), healAmount);

        // Native eating sounds and food crumb particles
        world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.NEUTRAL, 0.9f, 0.9f + (world.random.nextFloat() * 0.2f));
        world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.NEUTRAL, 0.8f, 1.0f);

        try {
            world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, stack), mob.getX(), mob.getEyeY() - 0.1, mob.getZ(), 8, 0.2, 0.15, 0.2, 0.05);
        } catch (Exception ignored) {
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getEyeY() - 0.1, mob.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
        }

        stack.decrement(1);
        if (slot != null) {
            mob.equipStack(slot, stack);
        } else if (invIndex >= 0 && mob instanceof InventoryOwner owner) {
            owner.getInventory().setStack(invIndex, stack);
        }
    }
}
