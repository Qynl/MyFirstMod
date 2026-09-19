package dev.qynl.myfirstmod.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Hand;

public class NullbladeItem extends Item {
    public NullbladeItem(Settings settings) { super(settings); }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player && !player.getWorld().isClient) {
            target.addVelocity(0, 0.18, 0);
            stack.damage(1, player, Hand.MAIN_HAND);
        }
        return true;
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (PlayerEntity player : world.getPlayers()) {
                ItemStack stack = player.getMainHandStack();
                if (stack.getItem() instanceof NullbladeItem && player.age % 5 == 0) {
                    world.spawnParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY()+1, player.getZ(), 2, .25, .45, .25, .01);
                }
            }
        }
    }
}
