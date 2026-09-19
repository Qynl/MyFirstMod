package dev.qynl.myfirstmod.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
/** Progress lives in the world block state, and therefore survives unloads and restarts. */
public final class FunerarySealBlock extends Block {
    public static final IntProperty RITE=IntProperty.of("rite",0,3);
    public static final MapCodec<FunerarySealBlock> CODEC=createCodec(FunerarySealBlock::new);
    public FunerarySealBlock(Settings settings) {super(settings);setDefaultState(getStateManager().getDefaultState().with(RITE,0));}
    @Override public MapCodec<FunerarySealBlock> getCodec() {return CODEC;}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder) {builder.add(RITE);}
}
