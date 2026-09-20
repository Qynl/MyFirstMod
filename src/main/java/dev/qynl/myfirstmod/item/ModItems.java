package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.MyFirstMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item UNIT_CREATOR = Registry.register(
            Registries.ITEM,
            Identifier.of(MyFirstMod.MOD_ID, "unit_creator"),
            new UnitCreatorItem(new Item.Settings().maxCount(1))
    );

    public static final Item COMMANDER_HORN = Registry.register(
            Registries.ITEM,
            Identifier.of(MyFirstMod.MOD_ID, "commander_horn"),
            new CommanderHornItem(new Item.Settings().maxCount(1))
    );

    public static final Item FACTION_BANNER = Registry.register(
            Registries.ITEM,
            Identifier.of(MyFirstMod.MOD_ID, "faction_banner"),
            new FactionBannerItem(new Item.Settings().maxCount(1))
    );

    public static final Item FACTION_SCEPTER = Registry.register(
            Registries.ITEM,
            Identifier.of(MyFirstMod.MOD_ID, "faction_scepter"),
            new FactionScepterItem(new Item.Settings().maxCount(1))
    );

    public static final Item TACTICAL_WHISTLE = Registry.register(
            Registries.ITEM,
            Identifier.of(MyFirstMod.MOD_ID, "tactical_whistle"),
            new TacticalWhistleItem(new Item.Settings().maxCount(1))
    );

    public static final Item REINFORCEMENT_BEACON = Registry.register(
            Registries.ITEM,
            Identifier.of(MyFirstMod.MOD_ID, "reinforcement_beacon"),
            new ReinforcementBeaconItem(new Item.Settings().maxCount(1))
    );

    private ModItems() {}

    public static void register() {}
}
