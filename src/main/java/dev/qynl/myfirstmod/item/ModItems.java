package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.MyFirstMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public final class ModItems {
    public static final java.util.List<Item> ALL = new java.util.ArrayList<>();
    private static Item register(String name,Item item) {
        ALL.add(item);
        return Registry.register(Registries.ITEM,Identifier.of(MyFirstMod.MOD_ID,name),item);
    }
    public static final Item NULL_RELIC=register("null_relic",new NullHeartItem(new Item.Settings().maxCount(1).fireproof().rarity(Rarity.EPIC)));
    public static final Item NULLBLADE=register("nullblade",new NullbladeItem(new Item.Settings().maxDamage(2531).fireproof().rarity(Rarity.EPIC)));
    public static final Item ASCENDED_NULLBLADE=register("ascended_nullblade",new NullbladeItem(new Item.Settings().maxDamage(4096).fireproof().rarity(Rarity.EPIC),true));
    public static final Item ARENA_COMPASS=register("arena_compass",new ArenaCompassItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));
    public static final Item RESONANT_SHARD=register("resonant_shard",new Item(new Item.Settings().fireproof().rarity(Rarity.UNCOMMON)));
    public static final Item RESONANCE_MATRIX=register("resonance_matrix",new Item(new Item.Settings().fireproof().rarity(Rarity.RARE)));
    public static final Item ECHO_SIGIL=register("echo_sigil",new Item(new Item.Settings().maxCount(16).fireproof().rarity(Rarity.RARE)));
    public static final Item WARDEN_CREST=register("warden_crest",new Item(new Item.Settings().fireproof().rarity(Rarity.EPIC)));
    public static final Item RIFT_AEGIS=register("rift_aegis",new RiftAegisItem(new Item.Settings().maxDamage(768).fireproof().rarity(Rarity.EPIC)));
    public static final Item RAW_RESONITE=register("raw_resonite",new Item(new Item.Settings()));
    public static final Item RESONITE_INGOT=register("resonite_ingot",new Item(new Item.Settings().fireproof()));
    public static final Item PRISM_DUST=register("prism_dust",new Item(new Item.Settings()));
    public static final Item CINDER_PEARL=register("cinder_pearl",new Item(new Item.Settings().fireproof()));
    public static final Item DUSK_FIBER=register("dusk_fiber",new Item(new Item.Settings()));
    public static final Item HUSHBERRY=register("hushberry",new Item(new Item.Settings().food(
            new net.minecraft.component.type.FoodComponent.Builder().nutrition(3).saturationModifier(.3f).build())));
    public static final Item EXPEDITION_STEW=register("expedition_stew",new Item(new Item.Settings().maxCount(1).food(
            new net.minecraft.component.type.FoodComponent.Builder().nutrition(10).saturationModifier(.8f).usingConvertsTo(net.minecraft.item.Items.BOWL).build())));
    public static final Item EMBER_TONIC=register("ember_tonic",new EmberTonicItem(new Item.Settings().maxCount(16).fireproof()));
    public static final Item RESONITE_PICKAXE=register("resonite_pickaxe",new net.minecraft.item.PickaxeItem(ResoniteMaterial.INSTANCE,
            new Item.Settings().fireproof().attributeModifiers(net.minecraft.item.PickaxeItem.createAttributeModifiers(ResoniteMaterial.INSTANCE,1,-2.8f))));
    public static final Item RESONITE_AXE=register("resonite_axe",new net.minecraft.item.AxeItem(ResoniteMaterial.INSTANCE,
            new Item.Settings().fireproof().attributeModifiers(net.minecraft.item.AxeItem.createAttributeModifiers(ResoniteMaterial.INSTANCE,5.5f,-3f))));
    public static final Item RESONITE_SHOVEL=register("resonite_shovel",new net.minecraft.item.ShovelItem(ResoniteMaterial.INSTANCE,
            new Item.Settings().fireproof().attributeModifiers(net.minecraft.item.ShovelItem.createAttributeModifiers(ResoniteMaterial.INSTANCE,1.5f,-3f))));
    public static final Item PRISM_STAFF=register("prism_staff",new PrismStaffItem(new Item.Settings().maxDamage(768).rarity(Rarity.RARE)));
    public static final Item CINDER_MAUL=register("cinder_maul",new CinderMaulItem(new Item.Settings().fireproof().rarity(Rarity.RARE)));
    public static final Item SURVEY_LENS=register("survey_lens",new ExpeditionUtilityItem(new Item.Settings().maxCount(1),ExpeditionUtilityItem.Kind.SURVEY));
    public static final Item VEIL_CHARM=register("veil_charm",new ExpeditionUtilityItem(new Item.Settings().maxDamage(128).rarity(Rarity.RARE),ExpeditionUtilityItem.Kind.VEIL));
    public static final Item REPAIR_KIT=register("repair_kit",new ExpeditionUtilityItem(new Item.Settings().maxCount(16),ExpeditionUtilityItem.Kind.REPAIR));
    public static final Item WAYFARER_THREAD=register("wayfarer_thread",new WayfarerThreadItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));
    public static final Item RESONITE_HELMET=register("resonite_helmet",new net.minecraft.item.ArmorItem(ResoniteArmor.MATERIAL,net.minecraft.item.ArmorItem.Type.HELMET,new Item.Settings().fireproof().maxDamage(net.minecraft.item.ArmorItem.Type.HELMET.getMaxDamage(38))));
    public static final Item RESONITE_CHESTPLATE=register("resonite_chestplate",new net.minecraft.item.ArmorItem(ResoniteArmor.MATERIAL,net.minecraft.item.ArmorItem.Type.CHESTPLATE,new Item.Settings().fireproof().maxDamage(net.minecraft.item.ArmorItem.Type.CHESTPLATE.getMaxDamage(38))));
    public static final Item RESONITE_LEGGINGS=register("resonite_leggings",new net.minecraft.item.ArmorItem(ResoniteArmor.MATERIAL,net.minecraft.item.ArmorItem.Type.LEGGINGS,new Item.Settings().fireproof().maxDamage(net.minecraft.item.ArmorItem.Type.LEGGINGS.getMaxDamage(38))));
    public static final Item RESONITE_BOOTS=register("resonite_boots",new net.minecraft.item.ArmorItem(ResoniteArmor.MATERIAL,net.minecraft.item.ArmorItem.Type.BOOTS,new Item.Settings().fireproof().maxDamage(net.minecraft.item.ArmorItem.Type.BOOTS.getMaxDamage(38))));
    public static final Item ASTRAL_CORE=register("astral_core",new Item(new Item.Settings().fireproof().rarity(Rarity.EPIC)));
    public static final Item VIGOR_RUNE=register("vigor_rune",new Item(new Item.Settings().maxCount(16).fireproof().rarity(Rarity.RARE)));
    public static final Item GALE_RUNE=register("gale_rune",new Item(new Item.Settings().maxCount(16).fireproof().rarity(Rarity.RARE)));
    public static final Item FOCUS_RUNE=register("focus_rune",new Item(new Item.Settings().maxCount(16).fireproof().rarity(Rarity.RARE)));
    public static void register() {
        net.fabricmc.fabric.api.registry.FuelRegistry.INSTANCE.add(dev.qynl.myfirstmod.block.ModBlocks.HUSHWOOD,300);
        Registry.register(Registries.ITEM_GROUP,Identifier.of(MyFirstMod.MOD_ID,"null_realm"),
                net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()
                        .displayName(net.minecraft.text.Text.translatable("itemGroup.myfirstmod.null_realm"))
                        .icon(()->new net.minecraft.item.ItemStack(ARENA_COMPASS))
                        .entries((context,entries)->{
                            ALL.forEach(entries::add);
                            dev.qynl.myfirstmod.block.ModBlocks.BUILDING_BLOCKS.forEach(entries::add);
                        }).build());
    }
}
