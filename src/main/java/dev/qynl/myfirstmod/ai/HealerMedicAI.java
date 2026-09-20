package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.faction.FactionPerk;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

import java.util.Comparator;
import java.util.List;

public final class HealerMedicAI {
    private HealerMedicAI() {}

    public static void executeHealer(ServerWorld world, MobEntity medic, UnitDefinition unit) {
        if (medic == null || !medic.isAlive()) return;

        MinecraftServer server = world.getServer();
        String myFaction = UnitSystem.getTagValue(medic, "faction:");
        if (myFaction == null) return;

        // Self-heal check if critically low
        if (medic.getHealth() < medic.getMaxHealth() * 0.4f && medic.age % 40 == 0) {
            selfHeal(world, medic);
        }

        // Find most injured ally within heal range
        double range = unit.healRange;
        if (server != null && FactionManager.hasPerk(server, myFaction, FactionPerk.HOLY_MIGHT)) {
            range *= 1.5;
        }

        List<LivingEntity> allies = world.getEntitiesByClass(LivingEntity.class, medic.getBoundingBox().expand(range),
                e -> e != medic && e.isAlive() && FactionManager.isAllied(server, myFaction, UnitSystem.getTagValue(e, "faction:")) && e.getHealth() < e.getMaxHealth());

        LivingEntity mostInjured = allies.stream()
                .min(Comparator.comparingDouble(e -> (double) e.getHealth() / e.getMaxHealth()))
                .orElse(null);

        if (mostInjured == null) return;

        double distSq = medic.squaredDistanceTo(mostInjured);

        // Move closer to ally if beyond 5 blocks
        if (distSq > 25.0) {
            medic.getNavigation().startMovingTo(mostInjured, 1.1);
        }

        medic.getLookControl().lookAt(mostInjured, 30.0f, 30.0f);

        // Perform heal action every 20 ticks (1 second)
        if (medic.age % 20 == 0) {
            medic.swingHand(Hand.MAIN_HAND);

            // Check if medic has splash potion to throw
            if (unit.canThrowPotions && (hasSplashPotion(medic) || hasInventorySplashPotion(medic))) {
                ThrowablePotionAI.throwSplashHealing(world, medic, mostInjured);
                return;
            }

            // Innate divine heal
            float healAmount = 4.0f;
            if (server != null && FactionManager.hasPerk(server, myFaction, FactionPerk.HOLY_MIGHT)) {
                healAmount = 6.5f;
            }

            mostInjured.heal(healAmount);
            mostInjured.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 0));

            // Cleanse negative status effects
            mostInjured.removeStatusEffect(StatusEffects.POISON);
            mostInjured.removeStatusEffect(StatusEffects.WITHER);
            mostInjured.removeStatusEffect(StatusEffects.SLOWNESS);

            // Visual healing particles
            world.spawnParticles(ParticleTypes.HEART, mostInjured.getX(), mostInjured.getBodyY(0.6), mostInjured.getZ(), 5, 0.3, 0.3, 0.3, 0.05);
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, mostInjured.getX(), mostInjured.getY() + 0.5, mostInjured.getZ(), 8, 0.4, 0.5, 0.4, 0.05);

            // Sound effect
            world.playSound(null, mostInjured.getX(), mostInjured.getY(), mostInjured.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME.value(), SoundCategory.NEUTRAL, 0.9f, 1.4f);
        }
    }

    private static void selfHeal(ServerWorld world, MobEntity medic) {
        medic.heal(6.0f);
        medic.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
        world.spawnParticles(ParticleTypes.HEART, medic.getX(), medic.getBodyY(0.5), medic.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
        world.playSound(null, medic.getX(), medic.getY(), medic.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP.value(), SoundCategory.NEUTRAL, 0.8f, 1.2f);
    }

    private static boolean hasSplashPotion(MobEntity mob) {
        return mob.getMainHandStack().isOf(Items.SPLASH_POTION) || mob.getOffHandStack().isOf(Items.SPLASH_POTION);
    }

    private static boolean hasInventorySplashPotion(MobEntity mob) {
        if (mob instanceof net.minecraft.entity.InventoryOwner owner) {
            for (int i = 0; i < owner.getInventory().size(); i++) {
                if (owner.getInventory().getStack(i).isOf(Items.SPLASH_POTION)) {
                    return true;
                }
            }
        }
        return false;
    }
}
