package dev.qynl.myfirstmod.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
/** Sealed, in session, or ruled: the capital's gate remembers the court's verdict. */
public final class CrownGateBlock extends Block {
    public static final IntProperty PHASE=IntProperty.of("phase",0,2);
    public static final MapCodec<CrownGateBlock> CODEC=createCodec(CrownGateBlock::new);
    public CrownGateBlock(Settings settings){super(settings);setDefaultState(getStateManager().getDefaultState().with(PHASE,0));}
    @Override public MapCodec<CrownGateBlock> getCodec(){return CODEC;}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(PHASE);}
}
