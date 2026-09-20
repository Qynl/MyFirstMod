package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.equipment.ItemCapabilities;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class EngineerBuildingAI {
    private EngineerBuildingAI() {}

    public static void executeBuilding(ServerWorld world, MobEntity engineer, LivingEntity target, UnitDefinition unit) {
        if (engineer == null || !unit.canBuild || engineer.age % 60 != 0) return;

        // Place torch if too dark (light level < 4)
        BlockPos currentPos = engineer.getBlockPos();
        if (world.getLightLevel(currentPos) < 4) {
            tryPlaceTorch(world, engineer, currentPos, unit);
        }

        // If in combat and target is far or shooting, build cover
        if (target != null && target.isAlive()) {
            double distSq = engineer.squaredDistanceTo(target);
            if (distSq > 36.0 && distSq < 400.0) { // 6 to 20 blocks
                buildBarricade(world, engineer, target, unit);
            }
        }
    }

    private static void tryPlaceTorch(ServerWorld world, MobEntity mob, BlockPos pos, UnitDefinition unit) {
        if (!hasItem(mob, Items.TORCH)) return;

        if (world.isAir(pos) && world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
            world.setBlockState(pos, Blocks.TORCH.getDefaultState(), Block.NOTIFY_ALL);
            world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 0.8f, 1.0f);
            consumeItem(mob, Items.TORCH, unit);
        }
    }

    private static void buildBarricade(ServerWorld world, MobEntity engineer, LivingEntity target, UnitDefinition unit) {
        ItemStack blockStack = getBuildingBlockStack(engineer);
        if (blockStack.isEmpty() && !unit.infiniteAmmo) return;

        Block blockToPlace = Blocks.COBBLESTONE;
        if (!blockStack.isEmpty() && blockStack.getItem() instanceof BlockItem blockItem) {
            blockToPlace = blockItem.getBlock();
        }

        Vec3d dir = target.getPos().subtract(engineer.getPos()).normalize();
        BlockPos basePos = engineer.getBlockPos().add((int) Math.round(dir.x * 2), 0, (int) Math.round(dir.z * 2));

        for (int dy = 0; dy <= 1; dy++) {
            BlockPos targetPos = basePos.up(dy);
            if (world.isAir(targetPos) || world.getBlockState(targetPos).isReplaceable()) {
                if (world.getBlockState(targetPos.down()).isSolidBlock(world, targetPos.down()) || dy > 0) {
                    world.setBlockState(targetPos, blockToPlace.getDefaultState(), Block.NOTIFY_ALL);
                    world.playSound(null, targetPos.getX(), targetPos.getY(), targetPos.getZ(), SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 0.8f, 1.0f);
                    world.spawnParticles(ParticleTypes.POOF, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.05);

                    if (!unit.infiniteAmmo) {
                        consumeBuildingBlock(engineer);
                    }
                    break;
                }
            }
        }
    }

    private static boolean hasItem(MobEntity mob, net.minecraft.item.Item item) {
        if (mob.getMainHandStack().isOf(item) || mob.getOffHandStack().isOf(item)) return true;
        if (mob instanceof InventoryOwner owner) {
            for (int i = 0; i < owner.getInventory().size(); i++) {
                if (owner.getInventory().getStack(i).isOf(item)) return true;
            }
        }
        return false;
    }

    private static ItemStack getBuildingBlockStack(MobEntity mob) {
        if (mob.getMainHandStack().getItem() instanceof BlockItem) return mob.getMainHandStack();
        if (mob.getOffHandStack().getItem() instanceof BlockItem) return mob.getOffHandStack();
        if (mob instanceof InventoryOwner owner) {
            for (int i = 0; i < owner.getInventory().size(); i++) {
                ItemStack stack = owner.getInventory().getStack(i);
                if (stack.getItem() instanceof BlockItem) return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void consumeItem(MobEntity mob, net.minecraft.item.Item item, UnitDefinition unit) {
        if (unit.infiniteAmmo) return;
        if (mob.getMainHandStack().isOf(item)) {
            mob.getMainHandStack().decrement(1);
            return;
        }
        if (mob.getOffHandStack().isOf(item)) {
            mob.getOffHandStack().decrement(1);
            return;
        }
        if (mob instanceof InventoryOwner owner) {
            for (int i = 0; i < owner.getInventory().size(); i++) {
                ItemStack stack = owner.getInventory().getStack(i);
                if (stack.isOf(item)) {
                    stack.decrement(1);
                    owner.getInventory().setStack(i, stack);
                    return;
                }
            }
        }
    }

    private static void consumeBuildingBlock(MobEntity mob) {
        if (mob.getMainHandStack().getItem() instanceof BlockItem) {
            mob.getMainHandStack().decrement(1);
            return;
        }
        if (mob.getOffHandStack().getItem() instanceof BlockItem) {
            mob.getOffHandStack().decrement(1);
            return;
        }
        if (mob instanceof InventoryOwner owner) {
            for (int i = 0; i < owner.getInventory().size(); i++) {
                ItemStack stack = owner.getInventory().getStack(i);
                if (stack.getItem() instanceof BlockItem) {
                    stack.decrement(1);
                    owner.getInventory().setStack(i, stack);
                    return;
                }
            }
        }
    }
}
