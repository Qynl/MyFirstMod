package dev.qynl.myfirstmod.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
/** Sanctuary monument counting how many royal seals the world has attuned. */
public final class SealPlinthBlock extends Block {
    public static final IntProperty SEALS=IntProperty.of("seals",0,4);
    public static final MapCodec<SealPlinthBlock> CODEC=createCodec(SealPlinthBlock::new);
    public SealPlinthBlock(Settings settings){super(settings);setDefaultState(getStateManager().getDefaultState().with(SEALS,0));}
    @Override public MapCodec<SealPlinthBlock> getCodec(){return CODEC;}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(SEALS);}
}
