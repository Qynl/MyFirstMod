package dev.qynl.myfirstmod.block;

import dev.qynl.myfirstmod.MyFirstMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBlock;
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
    private static Block stairs(String name,Block base){return building(name,new StairsBlock(base.getDefaultState(),AbstractBlock.Settings.copy(base)));}
    private static Block slab(String name,Block base){return building(name,new SlabBlock(AbstractBlock.Settings.copy(base)));}
    private static Block wall(String name,Block base){return building(name,new WallBlock(AbstractBlock.Settings.copy(base)));}
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
    public static final Block MEMORY_STELE=Registry.register(Registries.BLOCK,Identifier.of(MyFirstMod.MOD_ID,"memory_stele"),new Block(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1,3600000).luminance(s->7)));
    public static final Block PILGRIM_LEDGER=building("pilgrim_ledger",new Block(AbstractBlock.Settings.copy(Blocks.LECTERN).strength(3).luminance(s->4)));
    public static final Block KEEP_GATE=Registry.register(Registries.BLOCK,Identifier.of(MyFirstMod.MOD_ID,"keep_gate"),new Block(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1,3600000).luminance(s->12)));
    public static final Block KEEP_HEART=Registry.register(Registries.BLOCK,Identifier.of(MyFirstMod.MOD_ID,"keep_heart"),new Block(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1,3600000).luminance(s->8)));
    public static final Block KEEP_WARD=Registry.register(Registries.BLOCK,Identifier.of(MyFirstMod.MOD_ID,"keep_ward"),new Block(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1,3600000).luminance(s->6)));
    public static final Block BRINESILT=building("brinesilt",new Block(AbstractBlock.Settings.copy(Blocks.MOSS_BLOCK)));
    public static final Block VEILSTONE=building("veilstone",new Block(AbstractBlock.Settings.copy(Blocks.MOSS_BLOCK).luminance(s->2)));
    public static final Block VENT_BASALT=stone("vent_basalt",3.5f,0);
    public static final Block SPIRE_CRYSTAL=stone("spire_crystal",2.5f,7);
    public static final Block OXIDIZED_TRIM=building("oxidized_trim",new Block(AbstractBlock.Settings.copy(Blocks.OXIDIZED_COPPER)));
    public static final Block CROWN_GATE=Registry.register(Registries.BLOCK,Identifier.of(MyFirstMod.MOD_ID,"crown_gate"),new CrownGateBlock(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1,3600000).luminance(s->4*s.get(CrownGateBlock.PHASE))));
    public static final Block NULLSTONE_STAIRS=stairs("nullstone_stairs",NULLSTONE);
    public static final Block NULLSTONE_SLAB=slab("nullstone_slab",NULLSTONE);
    public static final Block NULLSTONE_WALL=wall("nullstone_wall",NULLSTONE);
    public static final Block POLISHED_NULLSTONE_STAIRS=stairs("polished_nullstone_stairs",POLISHED_NULLSTONE);
    public static final Block POLISHED_NULLSTONE_SLAB=slab("polished_nullstone_slab",POLISHED_NULLSTONE);
    public static final Block POLISHED_NULLSTONE_WALL=wall("polished_nullstone_wall",POLISHED_NULLSTONE);
    public static final Block NULLSTONE_BRICK_STAIRS=stairs("nullstone_brick_stairs",NULLSTONE_BRICKS);
    public static final Block NULLSTONE_BRICK_SLAB=slab("nullstone_brick_slab",NULLSTONE_BRICKS);
    public static final Block NULLSTONE_BRICK_WALL=wall("nullstone_brick_wall",NULLSTONE_BRICKS);
    public static final Block VEILSTONE_STAIRS=stairs("veilstone_stairs",VEILSTONE);
    public static final Block VEILSTONE_SLAB=slab("veilstone_slab",VEILSTONE);
    public static final Block VEILSTONE_WALL=wall("veilstone_wall",VEILSTONE);
    public static final Block PRISMSTONE_STAIRS=stairs("prismstone_stairs",PRISMSTONE);
    public static final Block PRISMSTONE_SLAB=slab("prismstone_slab",PRISMSTONE);
    public static final Block PRISMSTONE_WALL=wall("prismstone_wall",PRISMSTONE);
    public static final Block CINDERSTONE_STAIRS=stairs("cinderstone_stairs",CINDERSTONE);
    public static final Block CINDERSTONE_SLAB=slab("cinderstone_slab",CINDERSTONE);
    public static final Block CINDERSTONE_WALL=wall("cinderstone_wall",CINDERSTONE);
    public static final Block HUSH_PLANK_STAIRS=stairs("hush_plank_stairs",HUSH_PLANKS);
    public static final Block HUSH_PLANK_SLAB=slab("hush_plank_slab",HUSH_PLANKS);
    public static final Block SLAGGLASS=building("slagglass",new Block(AbstractBlock.Settings.copy(Blocks.OBSIDIAN)));
    public static final Block EMBER_CRUCIBLE=Registry.register(Registries.BLOCK,Identifier.of(MyFirstMod.MOD_ID,"ember_crucible"),new CrucibleBlock(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1,3600000).luminance(s->s.get(CrucibleBlock.QUENCHED)?4:15)));
    public static final Block SEAL_PLINTH=Registry.register(Registries.BLOCK,Identifier.of(MyFirstMod.MOD_ID,"seal_plinth"),new SealPlinthBlock(AbstractBlock.Settings.copy(Blocks.POLISHED_BLACKSTONE).luminance(s->2*s.get(SealPlinthBlock.SEALS))));
    public static final Block TIDE_BELL=Registry.register(Registries.BLOCK,Identifier.of(MyFirstMod.MOD_ID,"tide_bell"),new TideBellBlock(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1,3600000).luminance(s->s.get(TideBellBlock.DRAINED)?6:12)));
    private static Block monastery(String id){return Registry.register(Registries.BLOCK,Identifier.of(MyFirstMod.MOD_ID,id),new dev.qynl.myfirstmod.kingdom.MonasteryBlock(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1,3600000).luminance(s->s.get(dev.qynl.myfirstmod.kingdom.MonasteryBlock.BELLS)==3?12:5)));}
    public static final Block ROOT_HEART=monastery("root_heart"), CLOISTER_BELL=monastery("cloister_bell"),ROOT_RELIQUARY=monastery("root_reliquary");
    public static void register() {}
}
