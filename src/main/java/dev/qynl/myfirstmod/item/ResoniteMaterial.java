package dev.qynl.myfirstmod.item;

import net.minecraft.block.Block;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

public enum ResoniteMaterial implements ToolMaterial {
    INSTANCE;
    @Override public int getDurability() {return 1800;}
    @Override public float getMiningSpeedMultiplier() {return 8.5f;}
    @Override public float getAttackDamage() {return 3;}
    @Override public TagKey<Block> getInverseTag() {return BlockTags.INCORRECT_FOR_DIAMOND_TOOL;}
    @Override public int getEnchantability() {return 18;}
    @Override public Ingredient getRepairIngredient() {return Ingredient.ofItems(ModItems.RESONITE_INGOT);}
}
