package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.List;

public final class NecromancerAI {
    private NecromancerAI() {}

    public static void executeNecromancer(ServerWorld world, MobEntity necromancer, UnitDefinition unit) {
        if (necromancer == null || !necromancer.isAlive()) return;

        MinecraftServer server = world.getServer();
        String myFaction = UnitSystem.getTagValue(necromancer, "faction:");
        if (myFaction == null) return;

        // Summoning ritual every 120 ticks (6 seconds) when in combat
        if (necromancer.age % 120 == 0) {
            List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class, necromancer.getBoundingBox().expand(unit.followRange),
                    e -> e != necromancer && e.isAlive() && FactionManager.isHostile(server, myFaction, UnitSystem.getTagValue(e, "faction:")));

            if (!enemies.isEmpty()) {
                // Check if minion cap reached (max 3 active minions per necromancer)
                List<MobEntity> existingMinions = world.getEntitiesByClass(MobEntity.class, necromancer.getBoundingBox().expand(24.0),
                        e -> e.isAlive() && e.getCommandTags().contains("necromancer_minion") && myFaction.equals(UnitSystem.getTagValue(e, "faction:")));

                if (existingMinions.size() < 3) {
                    summonUndeadMinion(world, necromancer, myFaction, enemies.get(0));
                }
            }
        }
    }

    private static void summonUndeadMinion(ServerWorld world, MobEntity necromancer, String factionId, LivingEntity target) {
        double offsetX = (world.random.nextDouble() - 0.5) * 4.0;
        double offsetZ = (world.random.nextDouble() - 0.5) * 4.0;
        double sx = necromancer.getX() + offsetX;
        double sy = necromancer.getY();
        double sz = necromancer.getZ() + offsetZ;

        boolean isSkeleton = world.random.nextBoolean();
        MobEntity minion;

        if (isSkeleton) {
            SkeletonEntity skel = EntityType.SKELETON.create(world);
            if (skel == null) return;
            skel.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
            skel.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            minion = skel;
        } else {
            ZombieEntity zomb = EntityType.ZOMBIE.create(world);
            if (zomb == null) return;
            zomb.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            zomb.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            minion = zomb;
        }

        minion.refreshPositionAndAngles(sx, sy, sz, necromancer.getYaw(), 0.0f);
        minion.setCustomName(Text.literal("☠ " + factionId.toUpperCase() + " Thrall"));
        minion.setCustomNameVisible(true);

        minion.getCommandTags().add("unit:necromancer_thrall");
        minion.getCommandTags().add("faction:" + factionId);
        minion.getCommandTags().add("role:melee");
        minion.getCommandTags().add("necromancer_minion");
        minion.getCommandTags().add("battle_mob");
        minion.getCommandTags().add("ammo:64");

        minion.setTarget(target);
        world.spawnEntity(minion);

        // Visual summoning pentagram & portal burst
        world.spawnParticles(ParticleTypes.PORTAL, sx, sy + 0.8, sz, 30, 0.4, 0.6, 0.4, 0.15);
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, sx, sy + 0.2, sz, 15, 0.3, 0.3, 0.3, 0.05);
        world.playSound(null, sx, sy, sz, SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON.value(), SoundCategory.HOSTILE, 1.2f, 0.9f);
    }
}
