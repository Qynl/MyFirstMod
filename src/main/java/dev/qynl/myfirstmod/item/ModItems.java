package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.MyFirstMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item UNIT_CREATOR = Registry.register(Registries.ITEM, Identifier.of(MyFirstMod.MOD_ID, "unit_creator"), new UnitCreatorItem(new Item.Settings().maxCount(1)));
    private ModItems() {}
    public static void register() {}
}
