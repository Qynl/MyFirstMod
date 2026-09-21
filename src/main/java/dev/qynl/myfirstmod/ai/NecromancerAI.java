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
import net.minecraft.util.Hand;

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
                    necromancer.swingHand(Hand.MAIN_HAND);
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

        // Visual summoning pentagram: Concentric Soul Array & Portal Vortex
        for (int i = 0; i < 24; i++) {
            double angle = (2 * Math.PI * i) / 24.0;
            double px1 = sx + Math.cos(angle) * 1.8;
            double pz1 = sz + Math.sin(angle) * 1.8;
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, px1, sy + 0.1, pz1, 2, 0.05, 0.05, 0.05, 0.01);

            double px2 = sx + Math.cos(angle) * 0.9;
            double pz2 = sz + Math.sin(angle) * 0.9;
            world.spawnParticles(ParticleTypes.SMOKE, px2, sy + 0.1, pz2, 1, 0, 0, 0, 0);
        }
        for (double dy = 0; dy <= 2.5; dy += 0.3) {
            world.spawnParticles(ParticleTypes.PORTAL, sx, sy + dy, sz, 10, 0.3, 0.2, 0.3, 0.1);
        }
        world.playSound(null, sx, sy, sz, SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1.3f, 0.85f);
    }
}
