package dev.qynl.myfirstmod.arsenal;

import dev.qynl.myfirstmod.boss.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The anchor thrown by the {@link dev.qynl.myfirstmod.item.RiftlineItem}.
 *
 * <p>Flies in a straight line (no gravity). On a block hit it latches and the
 * Riftline reels the owner in; on an entity hit it yanks the victim toward the
 * owner instead. Fizzles out after a short lifetime so missed casts stay cheap.
 */
public class RiftAnchorEntity extends ThrownItemEntity {
    private static final int MAX_LIFE_TICKS = 26;
    private static final float YANK_DAMAGE = 4.0F;

    private int life;

    public RiftAnchorEntity(EntityType<? extends RiftAnchorEntity> type, World world) {
        super(type, world);
    }

    public RiftAnchorEntity(World world, LivingEntity owner) {
        super(ModEntities.RIFT_ANCHOR, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.ECHO_SHARD;
    }

    @Override
    protected double getGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved()) return;

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    this.getX(), this.getY(), this.getZ(),
                    2, 0.08, 0.08, 0.08, 0.0
            );
            if (++this.life > MAX_LIFE_TICKS) {
                // The line went slack: the anchor flew too far and dissolves.
                serverWorld.playSound(
                        null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BLOCK_CHAIN_FALL, SoundCategory.PLAYERS,
                        0.6F, 0.7F
                );
                this.discard();
            }
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;
        if (!(hit.getEntity() instanceof LivingEntity target)) return;
        if (!(this.getOwner() instanceof ServerPlayerEntity owner) || target == owner) return;

        // "Get over here!" - the line reverses and drags the victim to its owner.
        Vec3d pull = owner.getEyePos().subtract(target.getPos());
        double distance = pull.length();
        if (distance > 0.001) {
            Vec3d velocity = pull.normalize().multiply(Math.min(2.2, 0.6 + distance * 0.16));
            target.setVelocity(
                    velocity.x,
                    Math.min(1.0, 0.25 + distance * 0.045),
                    velocity.z
            );
            target.velocityModified = true;
        }
        target.damage(serverWorld.getDamageSources().playerAttack(owner), YANK_DAMAGE);

        serverWorld.spawnParticles(
                ParticleTypes.SCULK_SOUL,
                target.getX(), target.getBodyY(0.5), target.getZ(),
                12, 0.3, 0.4, 0.3, 0.02
        );
        serverWorld.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_CHAIN_BREAK,
                SoundCategory.PLAYERS, 1.2F, 0.6F);
        serverWorld.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT,
                SoundCategory.PLAYERS, 0.7F, 1.4F);
    }

    @Override
    protected void onBlockHit(BlockHitResult hit) {
        super.onBlockHit(hit);
        if (!(this.getWorld() instanceof ServerWorld)) return;
        if (this.getOwner() instanceof ServerPlayerEntity owner) {
            RiftlineManager.latch(owner, hit);
        }
    }

    @Override
    protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (!this.getWorld().isClient) {
            this.discard();
        }
    }
}
