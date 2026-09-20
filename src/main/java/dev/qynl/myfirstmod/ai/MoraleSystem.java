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
        if (mob == null || !mob.isAlive() || mob.age % 80 != 0) return;

        String squad = UnitSystem.getTagValue(mob, "squad:");
        String faction = UnitSystem.getTagValue(mob, "faction:");
        if (squad == null || squad.isBlank() || faction == null) return;

        // If unit is commander or veteran, immune to panic
        if (unit.commander || "commander".equalsIgnoreCase(unit.rank) || "veteran".equalsIgnoreCase(unit.rank) || "captain".equalsIgnoreCase(unit.rank)) {
            return;
        }

        // Check if commander is alive in 32 blocks
        List<MobEntity> leaders = world.getEntitiesByClass(MobEntity.class, mob.getBoundingBox().expand(32.0),
                e -> e.isAlive() && e.getCommandTags().contains("commander") && squad.equals(UnitSystem.getTagValue(e, "squad:")) && faction.equals(UnitSystem.getTagValue(e, "faction:")));

        // If squad had a commander and leader has fallen:
        if (leaders.isEmpty() && mob.getHealth() < mob.getMaxHealth() * 0.4f) {
            // Panic state: recruit retreats and cries
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, false, false));
            world.spawnParticles(ParticleTypes.SPLASH, mob.getX(), mob.getEyeY() + 0.3, mob.getZ(), 6, 0.2, 0.2, 0.2, 0.05);

            if (mob.age % 160 == 0) {
                world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 0.8f, 1.2f);
            }
        }
    }
}
