package dev.qynl.myfirstmod.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
/** Quenched state persists in the block state: a cooled forge stays cooled across restarts. */
public final class CrucibleBlock extends Block {
    public static final BooleanProperty QUENCHED=BooleanProperty.of("quenched");
    public static final MapCodec<CrucibleBlock> CODEC=createCodec(CrucibleBlock::new);
    public CrucibleBlock(Settings settings){super(settings);setDefaultState(getStateManager().getDefaultState().with(QUENCHED,false));}
    @Override public MapCodec<CrucibleBlock> getCodec(){return CODEC;}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(QUENCHED);}
}
