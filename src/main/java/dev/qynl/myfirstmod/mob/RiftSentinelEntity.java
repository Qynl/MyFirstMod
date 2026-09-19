package dev.qynl.myfirstmod.mob;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** Armored guardian: a visible, stationary charge gives the player time to disengage. */
public class RiftSentinelEntity extends ZombieEntity {
    private int pulseCooldown = 100;
    private int charge;
    public RiftSentinelEntity(EntityType<? extends ZombieEntity> type, World world) { super(type,world); }
    public static DefaultAttributeContainer.Builder attributes() {
        return ZombieEntity.createZombieAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,36)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,6).add(EntityAttributes.GENERIC_ARMOR,6)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED,.23);
    }
    @Override protected boolean burnsInDaylight() { return false; }
    @Override public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerWorld world) || !isAlive()) return;
        if (charge > 0) {
            getNavigation().stop();
            setVelocity(0,getVelocity().y,0);
            if (charge % 4 == 0) for (int i=0;i<24;i++) {
                double angle=i*Math.PI/12;
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,getX()+Math.cos(angle)*3,
                        getY()+.15,getZ()+Math.sin(angle)*3,1,0,0,0,0);
            }
            if (--charge == 0) {
                for (var player : world.getPlayers()) if (player.isAlive() && !player.isSpectator()
                        && squaredDistanceTo(player) <= 9 && canSee(player)) {
                    player.damage(world.getDamageSources().mobAttack(this),7);
                    player.takeKnockback(.6,getX()-player.getX(),getZ()-player.getZ());
                }
                playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK,1,.6f);
                pulseCooldown=120;
            }
        } else if (--pulseCooldown <= 0 && getTarget()!=null && squaredDistanceTo(getTarget()) < 25) {
            charge=36;
            playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,1,1.4f);
        }
    }
}
