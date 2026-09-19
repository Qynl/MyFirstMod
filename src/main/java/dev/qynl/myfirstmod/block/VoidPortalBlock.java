package dev.qynl.myfirstmod.block;

import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class VoidPortalBlock extends Block {
    public VoidPortalBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient) VoidPortalManager.tryEnter(entity);
        super.onEntityCollision(state, world, pos, entity);
    }
}
