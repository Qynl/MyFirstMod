package dev.qynl.myfirstmod.block;

import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class VoidPortalBlock extends Block {
    public static final net.minecraft.state.property.EnumProperty<net.minecraft.util.math.Direction.Axis> AXIS=net.minecraft.state.property.Properties.HORIZONTAL_AXIS;
    public VoidPortalBlock(Settings settings) {
        super(settings);setDefaultState(getStateManager().getDefaultState().with(AXIS,net.minecraft.util.math.Direction.Axis.X));
    }
    @Override protected void appendProperties(net.minecraft.state.StateManager.Builder<Block,BlockState> builder){builder.add(AXIS);}
    @Override protected net.minecraft.util.shape.VoxelShape getOutlineShape(BlockState state,net.minecraft.world.BlockView world,BlockPos pos,net.minecraft.block.ShapeContext context){
        return state.get(AXIS)==net.minecraft.util.math.Direction.Axis.X?Block.createCuboidShape(0,0,6,16,16,10):Block.createCuboidShape(6,0,0,10,16,16);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient) VoidPortalManager.tryEnter(entity);
        super.onEntityCollision(state, world, pos, entity);
    }
}
