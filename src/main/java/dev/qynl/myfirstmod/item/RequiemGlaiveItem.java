package dev.qynl.myfirstmod.item;
import net.minecraft.item.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
public final class RequiemGlaiveItem extends SwordItem {
    public RequiemGlaiveItem(Settings settings){super(ResoniteMaterial.INSTANCE,settings.attributeModifiers(SwordItem.createAttributeModifiers(ResoniteMaterial.INSTANCE,5,-2.6f)));}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand){var stack=player.getStackInHand(hand);if(player.getItemCooldownManager().isCoolingDown(this))return TypedActionResult.fail(stack);player.setCurrentHand(hand);return TypedActionResult.consume(stack);}
    @Override public int getMaxUseTime(ItemStack stack,LivingEntity user){return 72000;}
    @Override public UseAction getUseAction(ItemStack stack){return UseAction.BOW;}
    @Override public void onStoppedUsing(ItemStack stack,World world,LivingEntity user,int remaining){
        if(!(world instanceof ServerWorld server)||!(user instanceof PlayerEntity player)||getMaxUseTime(stack,user)-remaining<20||player.getItemCooldownManager().isCoolingDown(this))return;
        Vec3d aim=player.getRotationVec(1).multiply(1,0,1).normalize();boolean hit=false;
        for(var enemy:server.getEntitiesByClass(HostileEntity.class,player.getBoundingBox().expand(6),e->e.isAlive()&&!e.isTeammate(player))){
            var d=enemy.getPos().subtract(player.getPos());if(d.lengthSquared()>36||d.multiply(1,0,1).normalize().dotProduct(aim)<.25||!player.canSee(enemy))continue;
            if(enemy.damage(server.getDamageSources().playerAttack(player),14)){hit=true;enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,60,1));}
        }
        if(hit)player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,100,0));
        RelicAttunements.afterCast(player,stack,hit);player.getItemCooldownManager().set(this,RelicAttunements.cooldown(stack,140));
        stack.damage(3,player,player.getActiveHand()==Hand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
        double base=Math.atan2(aim.z,aim.x);for(int i=-12;i<=12;i++){double a=base+i*.1;server.spawnParticles(net.minecraft.particle.ParticleTypes.SOUL,player.getX()+Math.cos(a)*6,player.getY()+.8,player.getZ()+Math.sin(a)*6,2,0,.2,0,.02);}
        player.playSound(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,1,.5f);
    }
}
