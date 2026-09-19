package dev.qynl.myfirstmod.item;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class CinderMaulItem extends SwordItem {
    public CinderMaulItem(Settings settings) {
        super(ResoniteMaterial.INSTANCE,settings.attributeModifiers(SwordItem.createAttributeModifiers(ResoniteMaterial.INSTANCE,7,-3.1f)));
    }
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand) {
        var stack=user.getStackInHand(hand);
        if(!user.isOnGround() || user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        if(world instanceof ServerWorld server) {
            boolean landed=false;
            for(var enemy:server.getEntitiesByClass(HostileEntity.class,user.getBoundingBox().expand(4),
                    e->e.isAlive() && !e.isTeammate(user) && e.squaredDistanceTo(user)<=16 && user.canSee(e))) {
                if(enemy.damage(server.getDamageSources().playerAttack(user),7)) {
                    landed=true;
                    enemy.takeKnockback(.8,user.getX()-enemy.getX(),user.getZ()-enemy.getZ());
                }
            }
            for(int i=0;i<40;i++) {
                double angle=i*Math.PI/20;
                server.spawnParticles(ParticleTypes.FLAME,user.getX()+Math.cos(angle)*4,user.getY()+.2,
                        user.getZ()+Math.sin(angle)*4,1,0,.1,0,.01);
            }
            RelicAttunements.afterCast(user,stack,landed);
            user.getItemCooldownManager().set(this,RelicAttunements.cooldown(stack,120));
            stack.damage(3,user,hand==Hand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
        }
        return TypedActionResult.success(stack,world.isClient);
    }
}
