package dev.qynl.myfirstmod.kingdom;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.*;
import net.minecraft.util.math.Direction;
/** Both puzzle progress and template orientation survive chunk unloads. */
public final class MonasteryBlock extends Block {
    public static final IntProperty BELLS=IntProperty.of("bells",0,3);
    public static final EnumProperty<Direction> FACING=Properties.HORIZONTAL_FACING;
    public static final MapCodec<MonasteryBlock> CODEC=createCodec(MonasteryBlock::new);
    public MonasteryBlock(Settings settings){super(settings);setDefaultState(getStateManager().getDefaultState().with(BELLS,0).with(FACING,Direction.NORTH));}
    @Override public MapCodec<MonasteryBlock> getCodec(){return CODEC;}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(BELLS,FACING);}
    @Override protected BlockState rotate(BlockState state,BlockRotation rotation){return state.with(FACING,rotation.rotate(state.get(FACING)));}
    @Override protected BlockState mirror(BlockState state,BlockMirror mirror){return state.rotate(mirror.getRotation(state.get(FACING)));}
}
