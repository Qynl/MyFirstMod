package dev.qynl.myfirstmod.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
/** The drained state lives in the block state, so a flooded archive stays drained across restarts. */
public final class TideBellBlock extends Block {
    public static final BooleanProperty DRAINED=BooleanProperty.of("drained");
    public static final MapCodec<TideBellBlock> CODEC=createCodec(TideBellBlock::new);
    public TideBellBlock(Settings settings){super(settings);setDefaultState(getStateManager().getDefaultState().with(DRAINED,false));}
    @Override public MapCodec<TideBellBlock> getCodec(){return CODEC;}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(DRAINED);}
}
