package dev.qynl.myfirstmod.block;

import dev.qynl.myfirstmod.MyFirstMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    public static final java.util.List<Block> BUILDING_BLOCKS = new java.util.ArrayList<>();
    private static Block building(String name, Block block) {
        Identifier id=Identifier.of(MyFirstMod.MOD_ID,name);
        Registry.register(Registries.BLOCK,id,block);
        Registry.register(Registries.ITEM,id,new net.minecraft.item.BlockItem(block,new net.minecraft.item.Item.Settings()));
        BUILDING_BLOCKS.add(block);
        return block;
    }
    private static Block stone(String name,float strength,int light) {
        return building(name,new Block(AbstractBlock.Settings.copy(Blocks.DEEPSLATE).strength(strength).luminance(s->light)));
    }
    public static final Block NULLSTONE=stone("nullstone",3,0);
    public static final Block POLISHED_NULLSTONE=stone("polished_nullstone",3,0);
    public static final Block NULLSTONE_BRICKS=stone("nullstone_bricks",3,0);
    public static final Block RESONITE_ORE=stone("resonite_ore",4.5f,3);
    public static final Block PRISM_ORE=stone("prism_ore",4,5);
    public static final Block CINDER_ORE=stone("cinder_ore",5,5);
    public static final Block RESONITE_BLOCK=stone("resonite_block",5,0);
    public static final Block PRISM_LAMP=stone("prism_lamp",1.5f,15);
    public static final Block PRISMSTONE=stone("prismstone",2,0);
    public static final Block CINDERSTONE=stone("cinderstone",3,0);
    public static final Block HUSHED_MOSS=building("hushed_moss",new Block(AbstractBlock.Settings.copy(Blocks.MOSS_BLOCK)));
    public static final Block LUMEN_MOSS=building("lumen_moss",new Block(AbstractBlock.Settings.copy(Blocks.MOSS_BLOCK).luminance(s->4)));
    public static final Block HUSHWOOD=building("hushwood",new Block(AbstractBlock.Settings.copy(Blocks.WARPED_STEM)));
    public static final Block HUSH_PLANKS=building("hush_planks",new Block(AbstractBlock.Settings.copy(Blocks.WARPED_PLANKS)));
    public static final Block HUSH_LEAVES=building("hush_leaves",new Block(AbstractBlock.Settings.copy(Blocks.WARPED_WART_BLOCK)
            .strength(.2f).nonOpaque().luminance(s->3)));
    public static final Block VOID_PORTAL = Registry.register(
            Registries.BLOCK,
            Identifier.of(MyFirstMod.MOD_ID, "void_portal"),
            new VoidPortalBlock(AbstractBlock.Settings.copy(Blocks.NETHER_PORTAL).noCollision().nonOpaque().strength(-1.0F).luminance(state -> 11))
    );

    public static final Block RESONANCE_CORE = Registry.register(Registries.BLOCK,
            Identifier.of(MyFirstMod.MOD_ID, "resonance_core"),
            new Block(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1, 3600000)
                    .luminance(state -> 9)));

    public static final Block ECHO_ALTAR = Registry.register(Registries.BLOCK,
            Identifier.of(MyFirstMod.MOD_ID, "echo_altar"),
            new Block(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1,3600000).luminance(state -> 12)));
    public static final Block WAYSTONE = Registry.register(Registries.BLOCK,
            Identifier.of(MyFirstMod.MOD_ID,"waystone"),
            new Block(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1,3600000).luminance(s->12)));
    public static final Block RIFT_ANCHOR=Registry.register(Registries.BLOCK,Identifier.of(MyFirstMod.MOD_ID,"rift_anchor"),
            new Block(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1,3600000).luminance(s->12)));
    public static final Block ATTUNEMENT_FORGE=building("attunement_forge",new Block(AbstractBlock.Settings.copy(Blocks.SMITHING_TABLE).strength(4).luminance(s->7)));
    public static final Block HUSH_NURSERY=building("hush_nursery",new HushNurseryBlock(AbstractBlock.Settings.copy(Blocks.WHEAT).ticksRandomly().luminance(s->2)));
    public static final Block FUNERARY_SEAL=Registry.register(Registries.BLOCK,Identifier.of(MyFirstMod.MOD_ID,"funerary_seal"),
            new FunerarySealBlock(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1,3600000).luminance(s->s.get(FunerarySealBlock.RITE)==3?12:2)));
    public static final Block MOURNING_RELIQUARY=Registry.register(Registries.BLOCK,Identifier.of(MyFirstMod.MOD_ID,"mourning_reliquary"),
            new FunerarySealBlock(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1,3600000).luminance(s->s.get(FunerarySealBlock.RITE)==3?14:3)));
    public static void register() {}
}
