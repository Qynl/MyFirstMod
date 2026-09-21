package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

public final class RetreatAI {
    private RetreatAI() {}

    public static boolean executeRetreat(ServerWorld world, MobEntity mob, LivingEntity target, UnitDefinition unit) {
        if (mob == null || target == null || unit.retreatHealth <= 0.0f) return false;

        float threshold = mob.getMaxHealth() * unit.retreatHealth;
        if (mob.getHealth() > threshold) return false;

        String myFaction = UnitSystem.getTagValue(mob, "faction:");

        // Tactical cover-seeking: Look for friendly frontline tanks or medics nearby to retreat behind
        List<MobEntity> protectors = world.getEntitiesByClass(MobEntity.class, mob.getBoundingBox().expand(24.0),
                e -> e != mob && e.isAlive() && myFaction != null && myFaction.equals(UnitSystem.getTagValue(e, "faction:")) &&
                     (e.getCommandTags().contains("role:tank") || e.getCommandTags().contains("role:medic") || e.getHealth() > e.getMaxHealth() * 0.7f));

        if (!protectors.isEmpty()) {
            MobEntity ally = protectors.get(0);
            mob.getNavigation().startMovingTo(ally, 1.35);
        } else {
            // Direct tactical retreat away from enemy
            double dx = mob.getX() - target.getX();
            double dz = mob.getZ() - target.getZ();
            mob.getNavigation().startMovingTo(mob.getX() + dx * 3.5, mob.getY(), mob.getZ() + dz * 3.5, 1.30);
        }

        // Visual retreat / distress cues
        if (mob.age % 20 == 0) {
            world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, mob.getX(), mob.getEyeY() + 0.3, mob.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 1, false, false));
        }

        return true;
    }
}
