package dev.qynl.myfirstmod.unit;

import dev.qynl.myfirstmod.faction.Faction;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

/** Throttled, spatially bounded simulation pass. No global entity cross-product scans. */
public final class UnitSystem {
    private UnitSystem() {}
    public static void register() { ServerTickEvents.END_SERVER_TICK.register(UnitSystem::tick); }
    public static void tick(MinecraftServer server) { if (server.getTicks()%10 != 0) return; for (ServerWorld world: server.getWorlds()) for (var e: world.iterateEntities()) if (e instanceof MobEntity mob) simulate(server, world, mob); }
    private static void simulate(MinecraftServer server, ServerWorld world, MobEntity mob) {
        String unitId = tagValue(mob,"unit:"); if(unitId==null)return; UnitDefinition unit=UnitWorldData.get(server).units.get(unitId); if(unit==null)return;
        if (!mob.isAlive()) return; LivingEntity target = mob.getTarget(); if (!valid(server, unit, mob, target)) {
            target = world.getEntitiesByClass(LivingEntity.class, mob.getBoundingBox().expand(unit.followRange), e -> e != mob && valid(server, unit, mob, e)).stream().min((a,b)->Double.compare(mob.squaredDistanceTo(a),mob.squaredDistanceTo(b))).orElse(null); mob.setTarget(target);
        }
        if(target!=null){ if(unit.retreatHealth > 0 && mob.getHealth() <= mob.getMaxHealth() * unit.retreatHealth){ double dx=mob.getX()-target.getX(), dz=mob.getZ()-target.getZ(); mob.getNavigation().startMovingTo(mob.getX()+dx*4, mob.getY(), mob.getZ()+dz*4, 1.15); return; } double distance=mob.squaredDistanceTo(target); if(unit.role.equalsIgnoreCase("ranged")){ if(distance<64) mob.getNavigation().startMovingTo(target.getX(),target.getY(),target.getZ(),0.8); else if(distance>256) mob.getNavigation().startMovingTo(target,1.0); } else if(distance>4.0) mob.getNavigation().startMovingTo(target,1.0); else if(mob.age%10==0) mob.tryAttack(target); }
    }
    private static boolean valid(MinecraftServer server, UnitDefinition unit, MobEntity self, LivingEntity target) {
        if(target==null||!target.isAlive()||target.isSpectator())return false; if(target instanceof PlayerEntity && !unit.attackPlayers)return false;
        String mine=tagValue(self,"faction:"); String theirs=tagValue(target,"faction:"); if(mine!=null&&mine.equals(theirs))return false;
        if(theirs!=null){ Faction f=UnitWorldData.get(server).factions.get(mine); if(f!=null&&f.relations.getOrDefault(theirs,Faction.Relation.NEUTRAL)!=Faction.Relation.HOSTILE)return false; return true; }
        return unit.attackHostile && target instanceof HostileEntity;
    }
    private static String tagValue(net.minecraft.entity.Entity e,String prefix){ return e.getCommandTags().stream().filter(s->s.startsWith(prefix)).map(s->s.substring(prefix.length())).findFirst().orElse(null); }
    public static boolean spawn(ServerPlayerEntity player, UnitDefinition unit) { return spawnAt(player, unit, player.getX()+2, player.getY(), player.getZ()+2); }
    public static boolean spawnAt(ServerPlayerEntity player, UnitDefinition unit, double x, double y, double z) {
        var type=net.minecraft.registry.Registries.ENTITY_TYPE.getOrEmpty(unit.entityId).orElse(null); if(type==null||!type.isSummonable())return false; var entity=type.create(player.getServerWorld()); if(!(entity instanceof LivingEntity living))return false;
        living.refreshPositionAndAngles(x,y,z,player.getYaw(),0); unit.apply(living); living.getCommandTags().add("unit:"+unit.id); living.getCommandTags().add("faction:"+unit.factionId); player.getServerWorld().spawnEntity(living); return true;
    }
}
