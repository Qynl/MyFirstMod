package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.MyFirstMod;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item UNIT_CREATOR = Registry.register(Registries.ITEM, Identifier.of(MyFirstMod.MOD_ID, "unit_creator"), new UnitCreatorItem(new Item.Settings().maxCount(1)));

    public static final Item NULL_RELIC = Registry.register(
            Registries.ITEM,
            Identifier.of(MyFirstMod.MOD_ID, "null_relic"),
            new Item(new Item.Settings().maxCount(1))
    );

    public static final Item NULLBLADE = Registry.register(
            Registries.ITEM,
            Identifier.of(MyFirstMod.MOD_ID, "nullblade"),
            new NullbladeItem(new Item.Settings().maxCount(1).maxDamage(2031))
    );

    public static void register() {}
}
