package dev.qynl.myfirstmod.mob;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/** Fast crystal hunter; its close-range chill is telegraphed before it resolves. */
public class ShardstalkerEntity extends SpiderEntity {
    private int pulse = 100;
    public ShardstalkerEntity(EntityType<? extends SpiderEntity> type, World world) { super(type,world); }
    public static DefaultAttributeContainer.Builder attributes() {
        return SpiderEntity.createSpiderAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,24)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,4).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,.34);
    }
    @Override public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerWorld world) || getTarget()==null || !isAlive()) return;
        if (--pulse < 30 && pulse % 3 == 0)
            world.spawnParticles(ParticleTypes.END_ROD,getX(),getY()+.6,getZ(),4,1,.2,1,.01);
        if (pulse <= 0) {
            var target=getTarget();
            if (squaredDistanceTo(target)<16 && canSee(target))
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,50,0));
            pulse=140;
        }
    }
}
