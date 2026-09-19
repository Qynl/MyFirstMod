package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.MyFirstMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public final class ModItems {
    private static Item register(String name,Item item) {
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
    public static void register() {}
}
