package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.faction.FactionPerk;
import dev.qynl.myfirstmod.faction.FactionRelation;
import dev.qynl.myfirstmod.territory.TerritoryManager;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.EntityTypeTags;
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
            dev.qynl.myfirstmod.battle.BattleSandbox.tickBattleCheck(world);
            dev.qynl.myfirstmod.visual.FloatingCombatText.tickFloatingTexts(world);

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

        // Prevent custom undead units from burning in daytime sunlight
        if (mob.isOnFire() && world.isDay() && (mob.getType().isIn(EntityTypeTags.UNDEAD) || mob.getCommandTags().contains("sun_immune"))) {
            mob.extinguish();
        }

        // Armored beast aura glint
        if (mob.age % 20 == 0 && mob.getCommandTags().contains("armored_beast")) {
            world.spawnParticles(net.minecraft.particle.ParticleTypes.ENCHANTED_HIT, mob.getX(), mob.getBodyY(0.5), mob.getZ(), 2, 0.25, 0.3, 0.25, 0.02);
        }

        // Fire mastery perk: clear fire and immune
        if (FactionManager.hasPerk(server, factionId, FactionPerk.FIRE_MASTERY) && mob.isOnFire()) {
            mob.extinguish();
        }

        // Particle Auras: Dynamic Rotating Orbital Arrays
        if (mob.age % 4 == 0 && unit.particleAura != null && !unit.particleAura.equalsIgnoreCase("none")) {
            double angle = (mob.age * 0.2) % (2 * Math.PI);
            double radius = Math.max(0.6, mob.getWidth() * 0.85);
            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;
            double oy = mob.getBodyY(0.4) + Math.sin(angle * 2.0) * 0.2;

            switch (unit.particleAura.toLowerCase()) {
                case "flame" -> {
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.FLAME, mob.getX() + ox, oy, mob.getZ() + oz, 1, 0, 0, 0, 0.01);
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.SMOKE, mob.getX() - ox, oy, mob.getZ() - oz, 1, 0, 0, 0, 0.01);
                }
                case "soul_flame" -> {
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME, mob.getX() + ox, oy, mob.getZ() + oz, 1, 0, 0, 0, 0.01);
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.SOUL, mob.getX() - ox, oy + 0.2, mob.getZ() - oz, 1, 0, 0, 0, 0.01);
                }
                case "enchanted" -> {
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.ENCHANTED_HIT, mob.getX() + ox, oy, mob.getZ() + oz, 2, 0.1, 0.1, 0.1, 0.05);
                }
                case "portal" -> {
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.PORTAL, mob.getX() + ox, oy, mob.getZ() + oz, 3, 0.1, 0.1, 0.1, 0.05);
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.REVERSE_PORTAL, mob.getX() - ox, oy, mob.getZ() - oz, 2, 0.1, 0.1, 0.1, 0.05);
                }
                case "heart" -> {
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.HEART, mob.getX() + ox, mob.getY() + mob.getHeight() + 0.3, mob.getZ() + oz, 1, 0, 0, 0, 0.01);
                }
                case "totem" -> {
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.TOTEM_OF_UNDYING, mob.getX() + ox, oy, mob.getZ() + oz, 2, 0.1, 0.2, 0.1, 0.05);
                }
                case "electric_spark" -> {
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK, mob.getX() + ox, oy, mob.getZ() + oz, 2, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }

        // Territory check for home turf buffs
        TerritoryManager.checkTerritory(world, mob);

        // Morale System check (routing or vengeance)
        MoraleSystem.updateMorale(world, mob, unit);

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

        if (role.equals("druid") || role.equals("nature")) {
            DruidAI.executeDruid(world, mob, unit);
        }

        if (role.equals("necromancer") || FactionManager.hasPerk(server, factionId, FactionPerk.NECROMANCY)) {
            NecromancerAI.executeNecromancer(world, mob, unit);
        }

        if (role.equals("berserker") || FactionManager.hasPerk(server, factionId, FactionPerk.BERSERK_FURY)) {
            BerserkerAI.executeBerserker(world, mob, unit);
        }

        if (role.equals("bard") || FactionManager.hasPerk(server, factionId, FactionPerk.WAR_SONG)) {
            BardAI.executeBard(world, mob, unit);
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
            // Special 1.21 Mob handling (Breeze, Warden, Allay healer, Blaze)
            if (SpecialEntityAI.handleSpecialEntity(world, mob, target, unit)) {
                return;
            }

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
            if (data.fireworksEnabled && (role.equals("pyrotechnic") || unit.canShootFireworks)) {
                FireworksArtilleryAI.executeArtillery(world, mob, target, unit);
            }

            // Siege bombardier
            if (role.equals("bombardier") || role.equals("artillery")) {
                BombardierAI.executeBombardier(world, mob, target, unit);
            }

            // Paladin holy smite
            if (role.equals("paladin") || role.equals("crusader")) {
                PaladinAI.executePaladin(world, mob, target, unit);
                return;
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
