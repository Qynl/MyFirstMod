package dev.qynl.myfirstmod.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

/** A short, grounded evasive step. No invulnerability and no wall/ledge traversal. */
public final class PilgrimStepItem extends Item {
    public PilgrimStepItem(Settings settings) {super(settings);}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand) {
        var stack=player.getStackInHand(hand);
        if(!player.isOnGround() || player.hasVehicle() || player.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        if(world instanceof ServerWorld server) {
            Vec3d start=player.getPos(),destination=start;
            Vec3d direction=player.getRotationVec(1).multiply(1,0,1).normalize().multiply(player.isSneaking()?-1:1);
            for(double d=.25;d<=3;d+=.25) {
                Vec3d next=start.add(direction.multiply(d));BlockPos feet=BlockPos.ofFloored(next),below=feet.down();
                var box=player.getBoundingBox().offset(next.subtract(start));
                if(!world.isChunkLoaded(feet) || !world.getWorldBorder().contains(box) || !world.isSpaceEmpty(player,box)
                        || world.containsFluid(box)) break;
                var ground=world.getBlockState(below);
                if(!ground.isSolidBlock(world,below) || !world.getFluidState(below).isEmpty()
                        || ground.isOf(net.minecraft.block.Blocks.MAGMA_BLOCK) || ground.isOf(net.minecraft.block.Blocks.CAMPFIRE)
                        || ground.isOf(net.minecraft.block.Blocks.SOUL_CAMPFIRE) || world.getBlockState(feet).isIn(net.minecraft.registry.tag.BlockTags.FIRE)) break;
                destination=next;
            }
            if(destination.squaredDistanceTo(start)<.25) return TypedActionResult.fail(stack);
            player.requestTeleport(destination.x,destination.y,destination.z);player.fallDistance=0;
            player.getItemCooldownManager().set(this,50);player.addExhaustion(.6f);
            for(int i=0;i<8;i++) {
                var p=start.lerp(destination,i/7.0);server.spawnParticles(ParticleTypes.ASH,p.x,p.y+.6,p.z,3,.15,.3,.15,.01);
            }
            player.playSound(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,.6f,.65f);
        }
        return TypedActionResult.success(stack,world.isClient);
    }
}
