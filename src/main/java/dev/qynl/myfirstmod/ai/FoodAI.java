package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.equipment.ItemCapabilities;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public final class FoodAI {
    private FoodAI() {}

    public static void executeFood(ServerWorld world, MobEntity mob, UnitDefinition unit) {
        if (mob == null || mob.getHealth() >= mob.getMaxHealth() * 0.65f || mob.age % 60 != 0) return;

        // Check main hand
        ItemStack mainHand = mob.getMainHandStack();
        if (ItemCapabilities.isFood(mainHand)) {
            consumeFood(world, mob, mainHand, EquipmentSlot.MAINHAND, -1);
            return;
        }

        // Check off hand
        ItemStack offHand = mob.getOffHandStack();
        if (ItemCapabilities.isFood(offHand)) {
            consumeFood(world, mob, offHand, EquipmentSlot.OFFHAND, -1);
            return;
        }

        // Check inventory
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
        mob.heal(5.0f);
        world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.NEUTRAL, 0.8f, 1.0f);
        world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, mob.getX(), mob.getEyeY(), mob.getZ(), 6, 0.2, 0.2, 0.2, 0.05);

        stack.decrement(1);
        if (slot != null) {
            mob.equipStack(slot, stack);
        } else if (invIndex >= 0 && mob instanceof InventoryOwner owner) {
            owner.getInventory().setStack(invIndex, stack);
        }
    }
}
