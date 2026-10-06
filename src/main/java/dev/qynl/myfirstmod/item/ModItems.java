package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.MyFirstMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItems {
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

    public static final Item RIFTLINE = Registry.register(
            Registries.ITEM,
            Identifier.of(MyFirstMod.MOD_ID, "riftline"),
            new RiftlineItem(new Item.Settings().maxCount(1).maxDamage(250))
    );

    public static final Item NULL_GRASP = Registry.register(
            Registries.ITEM,
            Identifier.of(MyFirstMod.MOD_ID, "null_grasp"),
            new NullGraspItem(new Item.Settings().maxCount(1).maxDamage(455))
    );

    public static final RegistryKey<ItemGroup> ARSENAL_GROUP = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            Identifier.of(MyFirstMod.MOD_ID, "arsenal")
    );

    static {
        Registry.register(Registries.ITEM_GROUP, ARSENAL_GROUP, FabricItemGroup.builder()
                .icon(() -> new ItemStack(NULLBLADE))
                .displayName(Text.translatable("itemGroup.myfirstmod.arsenal"))
                .entries((context, entries) -> {
                    entries.add(RIFTLINE);
                    entries.add(NULL_GRASP);
                    entries.add(NULLBLADE);
                    entries.add(NULL_RELIC);
                })
                .build());
    }

    public static void register() {}
}
