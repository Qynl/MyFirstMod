package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

public final class SquadAI {
    private SquadAI() {}

    public static void executeSquad(ServerWorld world, MobEntity member, UnitDefinition unit) {
        if (member == null || unit.commander) return;

        String squadName = UnitSystem.getTagValue(member, "squad:");
        if (squadName == null || squadName.isBlank()) return;

        String myFaction = UnitSystem.getTagValue(member, "faction:");
        if (myFaction == null) return;

        // Find squad commander
        List<MobEntity> leaders = world.getEntitiesByClass(MobEntity.class, member.getBoundingBox().expand(24.0),
                e -> e != member && e.isAlive() &&
                     e.getCommandTags().contains("commander") &&
                     squadName.equals(UnitSystem.getTagValue(e, "squad:")) &&
                     myFaction.equals(UnitSystem.getTagValue(e, "faction:")));

        MobEntity leader = leaders.stream().findFirst().orElse(null);
        if (leader == null) return;

        double distSq = member.squaredDistanceTo(leader);

        // If far from leader, follow commander formation
        if (distSq > 100.0) { // > 10 blocks
            member.getNavigation().startMovingTo(leader, 1.15);
        }

        // Defend commander if commander is targeted or target commander's target
        LivingEntity leaderTarget = leader.getTarget();
        if (leaderTarget != null && leaderTarget.isAlive() && member.getTarget() == null) {
            member.setTarget(leaderTarget);
        }
    }
}
