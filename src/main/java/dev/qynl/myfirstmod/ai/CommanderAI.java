package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.faction.FactionPerk;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

import java.util.List;

public final class CommanderAI {
    private CommanderAI() {}

    public static void executeCommander(ServerWorld world, MobEntity commander, UnitDefinition unit) {
        if (commander == null || !unit.commander || commander.age % 100 != 0) return;

        MinecraftServer server = world.getServer();
        String myFaction = UnitSystem.getTagValue(commander, "faction:");
        if (myFaction == null) return;

        // Sound rally horn
        commander.swingHand(Hand.MAIN_HAND);
        world.playSound(null, commander.getX(), commander.getY(), commander.getZ(), SoundEvents.EVENT_RAID_HORN, SoundCategory.NEUTRAL, 1.4f, 1.0f);
        world.spawnParticles(ParticleTypes.RAID_OMEN, commander.getX(), commander.getY() + 1.2, commander.getZ(), 10, 0.4, 0.6, 0.4, 0.05);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, commander.getX(), commander.getY() + 0.8, commander.getZ(), 15, 0.6, 0.4, 0.6, 0.1);

        int amp = (server != null && FactionManager.hasPerk(server, myFaction, FactionPerk.RALLY)) ? 1 : 0;

        List<LivingEntity> allies = world.getEntitiesByClass(LivingEntity.class, commander.getBoundingBox().expand(18.0),
                e -> e.isAlive() && FactionManager.isAllied(server, myFaction, UnitSystem.getTagValue(e, "faction:")));

        for (LivingEntity ally : allies) {
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 160, amp));
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 160, amp));
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 160, 0));
        }
    }
}
