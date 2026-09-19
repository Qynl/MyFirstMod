package dev.qynl.myfirstmod.block;

import dev.qynl.myfirstmod.MyFirstMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    public static final Block VOID_PORTAL = Registry.register(
            Registries.BLOCK,
            Identifier.of(MyFirstMod.MOD_ID, "void_portal"),
            new VoidPortalBlock(AbstractBlock.Settings.copy(Blocks.NETHER_PORTAL).noCollision().nonOpaque().strength(-1.0F).luminance(state -> 11))
    );

    public static final Block RESONANCE_CORE = Registry.register(Registries.BLOCK,
            Identifier.of(MyFirstMod.MOD_ID, "resonance_core"),
            new Block(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1, 3600000)
                    .luminance(state -> 9)));

    public static void register() {}
}
