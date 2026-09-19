package dev.qynl.myfirstmod.item;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class NullbladeItem extends SwordItem {
    private final boolean ascended;
    public NullbladeItem(Settings settings) { this(settings,false); }
    public NullbladeItem(Settings settings,boolean ascended) {
        super(ToolMaterials.NETHERITE,settings.attributeModifiers(
                SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE,ascended?7:5,-2.2f)));
        this.ascended=ascended;
    }
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand) {
        ItemStack stack=user.getStackInHand(hand);
        if(user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        if(user.isSneaking()) {
            if(world instanceof ServerWorld server) riftstep(server,user,stack,hand);
            return TypedActionResult.success(stack,world.isClient);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }
    @Override public int getMaxUseTime(ItemStack stack,LivingEntity user) {return 72000;}
    @Override public UseAction getUseAction(ItemStack stack) {return UseAction.BOW;}
    @Override public void onStoppedUsing(ItemStack stack,World world,LivingEntity user,int remaining) {
        if(!(world instanceof ServerWorld server) || !(user instanceof PlayerEntity player)) return;
        int held=getMaxUseTime(stack,user)-remaining;
        if(held<12 || player.getItemCooldownManager().isCoolingDown(this)) return;
        float charge=Math.min(1,(held-12)/18f);
        double radius=4+charge*(ascended?4:2.5);
        int hits=0;
        Vec3d look=player.getRotationVec(1).multiply(1,0,1).normalize();
        for(HostileEntity target:server.getEntitiesByClass(HostileEntity.class,player.getBoundingBox().expand(radius),
                e->e.isAlive() && !e.isTeammate(player))) {
            Vec3d delta=target.getPos().subtract(player.getPos());
            if(delta.lengthSquared()>radius*radius || !player.canSee(target)
                    || delta.multiply(1,0,1).normalize().dotProduct(look)<-.15) continue;
            if(target.damage(server.getDamageSources().playerAttack(player),(ascended?12:8)+charge*(ascended?12:8))) {
                hits++;
                target.takeKnockback(.65,-delta.x,-delta.z);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,40,1));
            }
        }
        if(hits>0) player.heal(Math.min(ascended?6:4,hits*1.5f));
        RelicAttunements.afterCast(player,stack,hits>0);
        cooldown(player,RelicAttunements.cooldown(stack,ascended?90:120));
        stack.damage(3,player,player.getActiveHand()==Hand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
        for(int i=0;i<64;i++) {
            double angle=i*Math.PI*2/64;
            server.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,player.getX()+Math.cos(angle)*radius,
                    player.getY()+.6,player.getZ()+Math.sin(angle)*radius,1,0,.15,0,.01);
        }
        server.playSound(null,player.getBlockPos(),SoundEvents.ENTITY_WARDEN_SONIC_BOOM,SoundCategory.PLAYERS,.8f,1.5f);
    }
    private void riftstep(ServerWorld world,PlayerEntity player,ItemStack stack,Hand hand) {
        Vec3d start=player.getPos(),destination=start;
        Vec3d direction=player.getRotationVec(1).multiply(1,0,1).normalize();
        for(double d=.5;d<=(ascended?8:6);d+=.5) {
            Vec3d next=start.add(direction.multiply(d));
            var box=player.getBoundingBox().offset(next.subtract(start));
            var feet=net.minecraft.util.math.BlockPos.ofFloored(next);
            if(!world.isChunkLoaded(feet) || !world.getWorldBorder().contains(box)
                    || !world.isSpaceEmpty(player,box) || world.containsFluid(box)) break;
            // Do not dash off a ledge, into lava, or through a wall.
            var below=feet.down();
            var ground=world.getBlockState(below);
            if(world.getBlockState(feet).isIn(net.minecraft.registry.tag.BlockTags.FIRE)
                    || ground.isOf(net.minecraft.block.Blocks.MAGMA_BLOCK)
                    || ground.isOf(net.minecraft.block.Blocks.CAMPFIRE)
                    || ground.isOf(net.minecraft.block.Blocks.SOUL_CAMPFIRE)) break;
            if(ground.isSolidBlock(world,below) && world.getFluidState(below).isEmpty()) destination=next;
        }
        if(destination.squaredDistanceTo(start)<1) return;
        player.requestTeleport(destination.x,destination.y,destination.z);
        player.fallDistance=0;
        RelicAttunements.afterCast(player,stack,false);
        cooldown(player,RelicAttunements.cooldown(stack,80));
        stack.damage(2,player,hand==Hand.OFF_HAND?EquipmentSlot.OFFHAND:EquipmentSlot.MAINHAND);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,start.x,start.y+1,start.z,32,.4,.7,.4,.04);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,destination.x,destination.y+1,destination.z,32,.4,.7,.4,.04);
    }
    private static void cooldown(PlayerEntity player,int ticks) {
        // Shared cooldown prevents alternating the base and ascended weapon to bypass recovery.
        player.getItemCooldownManager().set(ModItems.NULLBLADE,ticks);
        player.getItemCooldownManager().set(ModItems.ASCENDED_NULLBLADE,ticks);
    }
    @Override public boolean postHit(ItemStack stack,LivingEntity target,LivingEntity attacker) {
        if(!attacker.getWorld().isClient) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,20,0));
        return super.postHit(stack,target,attacker);
    }
    public static void tick(net.minecraft.server.MinecraftServer server) {
        for(var player:server.getPlayerManager().getPlayerList()) if(player.getMainHandStack().getItem() instanceof NullbladeItem
                && player.age%10==0) player.getServerWorld().spawnParticles(ParticleTypes.REVERSE_PORTAL,
                player.getX(),player.getY()+1,player.getZ(),2,.25,.45,.25,.01);
    }
}
