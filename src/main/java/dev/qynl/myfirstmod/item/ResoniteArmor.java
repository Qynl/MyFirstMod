package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.MyFirstMod;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import java.util.List;
import java.util.Map;

public final class ResoniteArmor {
    public static final RegistryEntry<ArmorMaterial> MATERIAL=Registry.registerReference(Registries.ARMOR_MATERIAL,
            Identifier.of(MyFirstMod.MOD_ID,"resonite"),new ArmorMaterial(
                    Map.of(ArmorItem.Type.HELMET,3,ArmorItem.Type.CHESTPLATE,8,ArmorItem.Type.LEGGINGS,6,ArmorItem.Type.BOOTS,3,ArmorItem.Type.BODY,11),
                    18,SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,()->Ingredient.ofItems(ModItems.RESONITE_INGOT),
                    List.of(new ArmorMaterial.Layer(Identifier.of(MyFirstMod.MOD_ID,"resonite"))),2f,0f));
    public static void tick(MinecraftServer server) {
        var world=server.getWorld(VoidPortalManager.NULL_REALM);
        if(world==null || world.getTime()%20!=0) return;
        for(var player:server.getPlayerManager().getPlayerList()) {
            if(!dev.qynl.myfirstmod.keep.HollowKeep.expedition(player.getWorld()))continue;
            if(!player.isAlive() || player.isSpectator()) continue;
            if(player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.RESONITE_HELMET)
                    && player.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.RESONITE_CHESTPLATE)
                    && player.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.RESONITE_LEGGINGS)
                    && player.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.RESONITE_BOOTS)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,300,0,false,false,true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,60,0,false,false,true));
            }
        }
    }
}
