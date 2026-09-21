package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.List;

public final class DruidAI {
    private DruidAI() {}

    public static void executeDruid(ServerWorld world, MobEntity druid, UnitDefinition unit) {
        if (druid == null || !druid.isAlive()) return;

        MinecraftServer server = world.getServer();
        String myFaction = UnitSystem.getTagValue(druid, "faction:");
        if (myFaction == null) return;

        // Entanglement roots every 60 ticks
        if (druid.age % 60 == 0) {
            List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class, druid.getBoundingBox().expand(18.0),
                    e -> e != druid && e.isAlive() && FactionManager.isHostile(server, myFaction, UnitSystem.getTagValue(e, "faction:")));

            if (!enemies.isEmpty()) {
                druid.swingHand(Hand.MAIN_HAND);
                for (LivingEntity enemy : enemies) {
                    enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 3, false, false));
                    enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 1, false, false));
                    world.spawnParticles(ParticleTypes.COMPOSTER, enemy.getX(), enemy.getY() + 0.3, enemy.getZ(), 15, 0.4, 0.4, 0.4, 0.05);
                    world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, enemy.getX(), enemy.getY() + 0.6, enemy.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
                }
                world.playSound(null, druid.getX(), druid.getY(), druid.getZ(), SoundEvents.BLOCK_GRASS_BREAK, SoundCategory.NEUTRAL, 1.2f, 0.8f);
            }
        }

        // Summon Nature Spirit Wolf if none active
        if (druid.age % 140 == 0) {
            List<WolfEntity> spirits = world.getEntitiesByClass(WolfEntity.class, druid.getBoundingBox().expand(20.0),
                    w -> w.isAlive() && w.getCommandTags().contains("nature_spirit") && myFaction.equals(UnitSystem.getTagValue(w, "faction:")));

            if (spirits.isEmpty()) {
                druid.swingHand(Hand.MAIN_HAND);
                WolfEntity wolf = EntityType.WOLF.create(world);
                if (wolf != null) {
                    wolf.refreshPositionAndAngles(druid.getX() + 1.0, druid.getY(), druid.getZ() + 1.0, druid.getYaw(), 0.0f);
                    wolf.setCustomName(Text.literal("🌿 Nature Spirit"));
                    wolf.setCustomNameVisible(true);
                    wolf.getCommandTags().add("unit:nature_spirit");
                    wolf.getCommandTags().add("faction:" + myFaction);
                    wolf.getCommandTags().add("role:melee");
                    wolf.getCommandTags().add("nature_spirit");
                    wolf.getCommandTags().add("battle_mob");

                    world.spawnEntity(wolf);
                    world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, wolf.getX(), wolf.getY() + 0.5, wolf.getZ(), 15, 0.3, 0.4, 0.3, 0.05);
                    world.playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(), SoundEvents.ENTITY_WOLF_HOWL, SoundCategory.NEUTRAL, 1.0f, 1.2f);
                }
            }
        }
    }
}
