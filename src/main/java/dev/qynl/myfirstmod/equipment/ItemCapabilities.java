package dev.qynl.myfirstmod.equipment;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Central capability classifier used by future item-use goals; it relies on vanilla tags/types, not item-name lists. */
public final class ItemCapabilities {
    public enum Capability { WEAPON, SHIELD, RANGED_WEAPON, AMMUNITION, FOOD, POTION, TOOL, BLOCK, NONE }
    private ItemCapabilities() {}
    public static Capability classify(ItemStack stack) {
        if(stack.isEmpty()) return Capability.NONE;
        if(stack.isOf(Items.SHIELD)) return Capability.SHIELD;
        if(stack.isOf(Items.BOW)||stack.isOf(Items.CROSSBOW)||stack.isOf(Items.TRIDENT)) return Capability.RANGED_WEAPON;
        if(stack.isOf(Items.ARROW)||stack.isOf(Items.SPECTRAL_ARROW)||stack.isOf(Items.TIPPED_ARROW)) return Capability.AMMUNITION;
        if(stack.isFood()) return Capability.FOOD;
        if(stack.isOf(Items.POTION)||stack.isOf(Items.SPLASH_POTION)||stack.isOf(Items.LINGERING_POTION)) return Capability.POTION;
        if(stack.getItem() instanceof net.minecraft.item.ToolItem) return Capability.TOOL;
        if(stack.getItem() instanceof net.minecraft.item.BlockItem) return Capability.BLOCK;
        if(stack.getItem() instanceof net.minecraft.item.SwordItem||stack.getItem() instanceof net.minecraft.item.AxeItem||stack.getItem() instanceof net.minecraft.item.MaceItem) return Capability.WEAPON;
        return Capability.NONE;
    }
}
