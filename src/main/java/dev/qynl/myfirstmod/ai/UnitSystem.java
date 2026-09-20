package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.faction.FactionPerk;
import dev.qynl.myfirstmod.faction.FactionRelation;
import dev.qynl.myfirstmod.unit.BattleStats;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitSpawner;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Undead;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Comparator;
import java.util.List;

public final class UnitSystem {
    private UnitSystem() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(UnitSystem::tick);
    }

    public static void tick(MinecraftServer server) {
        UnitWorldData data = UnitWorldData.get(server);
        int interval = Math.max(1, data.aiTickInterval);

        if (server.getTicks() % interval != 0) return;

        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                if (entity instanceof MobEntity mob && entity.isAlive()) {
                    simulateMob(server, world, mob, data);
                }
            }
        }
    }

    private static void simulateMob(MinecraftServer server, ServerWorld world, MobEntity mob, UnitWorldData data) {
        String unitId = getTagValue(mob, "unit:");
        if (unitId == null) return;

        UnitDefinition unit = data.units.get(unitId);
        if (unit == null) return;

        String factionId = unit.factionId;

        // Prevent custom undead units from catching fire in sunlight
        if (mob.isOnFire() && world.isDay() && mob.isUndead()) {
            mob.extinguish();
        }

        // Apply passive faction perks
        if (FactionManager.hasPerk(server, factionId, FactionPerk.REGENERATION) && mob.age % 40 == 0) {
            mob.heal(1.0f);
        }

        if (FactionManager.hasPerk(server, factionId, FactionPerk.NIGHT_FIGHTERS) && mob.age % 80 == 0) {
            if (!world.isDay()) {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 200, 0, false, false));
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 0, false, false));
            }
        }

        // Role-specific routines
        String role = unit.role.toLowerCase();

        if (unit.healAllies && (role.equals("medic") || role.equals("support") || role.equals("healer"))) {
            HealerMedicAI.executeHealer(world, mob, unit);
        }

        if (role.equals("assassin")) {
            AssassinAI.executeAssassin(world, mob, unit);
            return;
        }

        if (role.equals("scout")) {
            ScoutAI.executeScout(world, mob, unit);
        }

        if (role.equals("tank")) {
            TankAI.executeTank(world, mob, unit);
        }

        if (unit.commander) {
            CommanderAI.executeCommander(world, mob, unit);
        }

        if (!unit.squad.isBlank()) {
            SquadAI.executeSquad(world, mob, unit);
        }

        FoodAI.executeFood(world, mob, unit);

        // Target acquisition & combat loop
        LivingEntity target = mob.getTarget();
        if (!isValidTarget(server, unit, mob, target, data)) {
            target = findBestTarget(server, world, mob, unit, data);
            mob.setTarget(target);
        }

        if (target != null && target.isAlive()) {
            // Tactical retreat check
            if (RetreatAI.executeRetreat(world, mob, target, unit)) {
                return;
            }

            // Shield defense
            if (data.shieldDefenseEnabled) {
                ShieldDefenseAI.executeShield(world, mob, target, unit);
            }

            // Engineer building
            if (data.buildingEnabled && (role.equals("engineer") || unit.canBuild)) {
                EngineerBuildingAI.executeBuilding(world, mob, target, unit);
            }

            // Throwable potions
            if (data.potionsEnabled && (role.equals("support") || role.equals("alchemist") || unit.canThrowPotions)) {
                ThrowablePotionAI.executePotionThrowing(world, mob, target, unit);
            }

            // Fireworks artillery
            if (data.fireworksEnabled && (role.equals("pyrotechnic") || role.equals("artillery") || unit.canShootFireworks)) {
                FireworksArtilleryAI.executeArtillery(world, mob, target, unit);
            }

            // Ranged or Melee attack
            if (role.equals("ranged") || mob.getMainHandStack().isOf(Items.BOW) || mob.getMainHandStack().isOf(Items.CROSSBOW)) {
                RangedAI.executeRanged(world, mob, target, unit);
            } else {
                CombatAI.executeMelee(world, mob, target, unit);
            }
        }
    }

    private static LivingEntity findBestTarget(MinecraftServer server, ServerWorld world, MobEntity mob, UnitDefinition unit, UnitWorldData data) {
        List<LivingEntity> candidates = world.getEntitiesByClass(LivingEntity.class, mob.getBoundingBox().expand(unit.followRange),
                e -> e != mob && isValidTarget(server, unit, mob, e, data));

        return candidates.stream()
                .min(Comparator.comparingDouble(target -> scoreTarget(server, unit, mob, target)))
                .orElse(null);
    }

    private static double scoreTarget(MinecraftServer server, UnitDefinition unit, MobEntity self, LivingEntity target) {
        double score = self.squaredDistanceTo(target);
        String targetUnitId = getTagValue(target, "unit:");

        if (unit.targetPriority.equalsIgnoreCase("commander") && target.getCommandTags().contains("commander")) {
            score -= 10000.0;
        }

        if (unit.targetPriority.equalsIgnoreCase("medic") && targetUnitId != null && server != null) {
            UnitDefinition tUnit = UnitWorldData.get(server).units.get(targetUnitId);
            if (tUnit != null && (tUnit.role.equalsIgnoreCase("medic") || tUnit.role.equalsIgnoreCase("healer"))) {
                score -= 6000.0;
            }
        }

        if (unit.targetPriority.equalsIgnoreCase("ranged") && targetUnitId != null && server != null) {
            UnitDefinition tUnit = UnitWorldData.get(server).units.get(targetUnitId);
            if (tUnit != null && tUnit.role.equalsIgnoreCase("ranged")) {
                score -= 3000.0;
            }
        }

        if (unit.targetPriority.equalsIgnoreCase("weakest")) {
            score += target.getHealth() * 15.0;
        }

        return score;
    }

    public static boolean isValidTarget(MinecraftServer server, UnitDefinition unit, MobEntity self, LivingEntity target, UnitWorldData data) {
        if (target == null || !target.isAlive() || target.isSpectator() || target == self) return false;

        // Player check
        if (target instanceof PlayerEntity) {
            return unit.attackPlayers;
        }

        String myFaction = getTagValue(self, "faction:");
        String targetFaction = getTagValue(target, "faction:");

        // Same faction = friendly fire prevented
        if (myFaction != null && myFaction.equalsIgnoreCase(targetFaction) && !data.friendlyFireAllowed) {
            return false;
        }

        // Allied faction = friendly fire prevented
        if (myFaction != null && targetFaction != null && FactionManager.isAllied(server, myFaction, targetFaction) && !data.friendlyFireAllowed) {
            return false;
        }

        // Hostile faction check
        if (myFaction != null && targetFaction != null) {
            return FactionManager.isHostile(server, myFaction, targetFaction);
        }

        // Hostile vanilla mob check
        return unit.attackHostile && target instanceof HostileEntity;
    }

    public static String getTagValue(Entity entity, String prefix) {
        if (entity == null) return null;
        for (String tag : entity.getCommandTags()) {
            if (tag.startsWith(prefix)) {
                return tag.substring(prefix.length());
            }
        }
        return null;
    }

    public static int getNumericTag(Entity entity, String prefix) {
        String val = getTagValue(entity, prefix);
        if (val == null) return 0;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void consumeTag(Entity entity, String prefix) {
        int current = getNumericTag(entity, prefix);
        if (current > 0) {
            entity.getCommandTags().remove(prefix + current);
            entity.getCommandTags().add(prefix + (current - 1));
        }
    }

    public static boolean spawn(ServerPlayerEntity player, UnitDefinition unit) {
        return UnitSpawner.spawnAtPlayer(player, unit);
    }

    public static boolean spawnAt(ServerPlayerEntity player, UnitDefinition unit, double x, double y, double z) {
        return UnitSpawner.spawn(player.getServerWorld(), unit, new net.minecraft.util.math.Vec3d(x, y, z), player.getYaw()) != null;
    }
}
