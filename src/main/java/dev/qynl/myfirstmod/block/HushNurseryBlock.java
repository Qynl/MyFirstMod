package dev.qynl.myfirstmod.block;

import com.mojang.serialization.MapCodec;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.minecraft.block.*;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/** A renewable, harvest-in-place crop that adapts to the realm's permanent darkness. */
public final class HushNurseryBlock extends PlantBlock implements Fertilizable {
    public static final MapCodec<HushNurseryBlock> CODEC=createCodec(HushNurseryBlock::new);
    public static final IntProperty AGE=IntProperty.of("age",0,3);
    public HushNurseryBlock(Settings settings) {super(settings);setDefaultState(getStateManager().getDefaultState().with(AGE,0));}
    @Override public MapCodec<HushNurseryBlock> getCodec() {return CODEC;}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder) {builder.add(AGE);}
    @Override protected boolean canPlantOnTop(BlockState floor,BlockView world,BlockPos pos) {
        return floor.isOf(ModBlocks.HUSHED_MOSS)||floor.isOf(ModBlocks.LUMEN_MOSS)||floor.isOf(Blocks.MOSS_BLOCK)
                ||floor.isOf(Blocks.DIRT)||floor.isOf(Blocks.GRASS_BLOCK)||floor.isOf(Blocks.FARMLAND);
    }
    @Override protected void randomTick(BlockState state,ServerWorld world,BlockPos pos,Random random) {
        if(state.get(AGE)<3 && (world.getRegistryKey().equals(VoidPortalManager.NULL_REALM)||world.getLightLevel(pos)>=8)
                && random.nextInt(3)==0) grow(world,random,pos,state);
    }
    @Override public boolean isFertilizable(WorldView world,BlockPos pos,BlockState state) {return state.get(AGE)<3;}
    @Override public boolean canGrow(World world,Random random,BlockPos pos,BlockState state) {return true;}
    @Override public void grow(ServerWorld world,Random random,BlockPos pos,BlockState state) {
        world.setBlockState(pos,state.with(AGE,Math.min(3,state.get(AGE)+1)),Block.NOTIFY_LISTENERS);
    }
    public static ActionResult harvest(ServerPlayerEntity player,BlockPos pos) {
        var world=player.getServerWorld();var state=world.getBlockState(pos);
        if(!state.isOf(ModBlocks.HUSH_NURSERY) || state.get(AGE)<3) return ActionResult.PASS;
        if(player.isSpectator() || !world.canPlayerModifyAt(player,pos)) return ActionResult.PASS;
        world.setBlockState(pos,state.with(AGE,1),Block.NOTIFY_LISTENERS);
        player.getInventory().offerOrDrop(new ItemStack(ModItems.HUSHBERRY,2));
        player.getInventory().offerOrDrop(new ItemStack(ModItems.DUSK_FIBER));
        return ActionResult.SUCCESS;
    }
}
