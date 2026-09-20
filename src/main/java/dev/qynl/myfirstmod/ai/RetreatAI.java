package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

public final class RetreatAI {
    private RetreatAI() {}

    public static boolean executeRetreat(ServerWorld world, MobEntity mob, LivingEntity target, UnitDefinition unit) {
        if (mob == null || target == null || unit.retreatHealth <= 0.0f) return false;

        float threshold = mob.getMaxHealth() * unit.retreatHealth;
        if (mob.getHealth() > threshold) return false;

        // Flee away from the target
        double dx = mob.getX() - target.getX();
        double dz = mob.getZ() - target.getZ();
        mob.getNavigation().startMovingTo(mob.getX() + dx * 3.5, mob.getY(), mob.getZ() + dz * 3.5, 1.25);
        return true;
    }
}
