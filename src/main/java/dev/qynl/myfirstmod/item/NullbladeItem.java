package dev.qynl.myfirstmod.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class NullbladeItem extends SwordItem {
    public NullbladeItem(Settings settings) {
        super(ToolMaterials.NETHERITE, settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return ActionResult.PASS;
        }

        user.getItemCooldownManager().set(this, 70);

        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            double radius = 5.0;
            Box area = user.getBoundingBox().expand(radius);
            for (LivingEntity target : serverWorld.getEntitiesByClass(
                    LivingEntity.class,
                    area,
                    entity -> entity != user && entity.isAlive()
            )) {
                target.damage(serverWorld.getDamageSources().playerAttack(user), 8.0F);
                target.addVelocity(
                        target.getX() - user.getX(),
                        0.15,
                        target.getZ() - user.getZ()
                );
            }

            for (int i = 0; i < 80; i++) {
                double angle = i * Math.PI * 2.0 / 80.0;
                serverWorld.spawnParticles(
                        ParticleTypes.REVERSE_PORTAL,
                        user.getX() + Math.cos(angle) * radius,
                        user.getY() + 0.6,
                        user.getZ() + Math.sin(angle) * radius,
                        1, 0, 0, 0, 0
                );
            }

            serverWorld.playSound(
                    null,
                    user.getBlockPos(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.PLAYERS,
                    1.4F,
                    0.55F
            );
        }

        return ActionResult.SUCCESS;
    }

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
                if (stack.getItem() instanceof NullbladeItem && player.age % 4 == 0) {
                    world.spawnParticles(
                            ParticleTypes.REVERSE_PORTAL,
                            player.getX(),
                            player.getY() + 1,
                            player.getZ(),
                            2, .25, .45, .25, .01
                    );
                }
            }
        }
    }
}
