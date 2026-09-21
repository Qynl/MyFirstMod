package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

public final class MoraleSystem {
    private MoraleSystem() {}

    public static void updateMorale(ServerWorld world, MobEntity mob, UnitDefinition unit) {
        checkMorale(world, mob, unit);
    }

    public static void checkMorale(ServerWorld world, MobEntity mob, UnitDefinition unit) {
        if (mob == null || !mob.isAlive() || mob.age % 60 != 0) return;

        String squad = UnitSystem.getTagValue(mob, "squad:");
        String faction = UnitSystem.getTagValue(mob, "faction:");
        if (squad == null || squad.isBlank() || faction == null) return;

        // Check if commander is alive in 36 blocks
        List<MobEntity> leaders = world.getEntitiesByClass(MobEntity.class, mob.getBoundingBox().expand(36.0),
                e -> e.isAlive() && e.getCommandTags().contains("commander") && squad.equals(UnitSystem.getTagValue(e, "squad:")) && faction.equals(UnitSystem.getTagValue(e, "faction:")));

        // If squad leader has fallen:
        if (leaders.isEmpty()) {
            boolean isVeteran = "veteran".equalsIgnoreCase(unit.rank) || "captain".equalsIgnoreCase(unit.rank) || "warlord".equalsIgnoreCase(unit.rank);

            if (isVeteran) {
                // Vengeance Fury for veteran troops
                if (!mob.hasStatusEffect(StatusEffects.STRENGTH)) {
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 1, false, true));
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1, false, true));
                    dev.qynl.myfirstmod.visual.FloatingCombatText.spawnStatus(world, mob.getX(), mob.getEyeY() + 0.8, mob.getZ(), "🔥 VENGEANCE!", net.minecraft.util.Formatting.RED);
                    world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getEyeY() + 0.3, mob.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
                    world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_PIGLIN_BRUTE_ANGRY, SoundCategory.NEUTRAL, 0.8f, 1.2f);
                }
            } else if (!unit.commander && mob.getHealth() < mob.getMaxHealth() * 0.5f) {
                // Panic State for recruits
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0, false, false));
                if (mob.age % 120 == 0) {
                    dev.qynl.myfirstmod.visual.FloatingCombatText.spawnStatus(world, mob.getX(), mob.getEyeY() + 0.8, mob.getZ(), "💦 PANIC!", net.minecraft.util.Formatting.BLUE);
                }
                world.spawnParticles(ParticleTypes.SPLASH, mob.getX(), mob.getEyeY() + 0.3, mob.getZ(), 6, 0.2, 0.2, 0.2, 0.05);

                if (mob.age % 120 == 0) {
                    world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 0.8f, 1.2f);
                }
            }
        }
    }
}
