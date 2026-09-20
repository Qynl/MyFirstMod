package dev.qynl.myfirstmod.gui;

import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CreatorScreenHandler extends ScreenHandler {
    public CreatorScreenHandler(int syncId, PlayerInventory inventory){super(ModScreenHandlers.CREATOR,syncId);}
    @Override public ItemStack quickMove(PlayerEntity player,int slot){return ItemStack.EMPTY;}
    @Override public boolean canUse(PlayerEntity player){return true;}
    @Override public boolean onButtonClick(PlayerEntity player,int id){ if(!(player instanceof ServerPlayerEntity sp))return true; UnitWorldData d=UnitWorldData.get(sp.getServer());
        if(id==0){UnitDefinition u=new UnitDefinition("guard","Village Guard",Identifier.of("minecraft","villager"));u.description="A configurable faction defender.";u.factionId="village";u.maxHealth=30;u.attackDamage=7;u.armor=12;u.armorToughness=0;u.equipment.put(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));u.equipment.put(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));u.equipment.put(EquipmentSlot.LEGS,new ItemStack(Items.IRON_LEGGINGS));u.equipment.put(EquipmentSlot.FEET,new ItemStack(Items.IRON_BOOTS));u.equipment.put(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));u.equipment.put(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));d.saveUnit(u);}
        if(id==1){UnitDefinition u=new UnitDefinition("archer","Royal Archer",Identifier.of("minecraft","skeleton"));u.factionId="village";u.role="ranged";u.attackDamage=5;u.equipment.put(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));d.saveUnit(u);}
        if(id==2){d.factions.putIfAbsent("village",new Faction("village","Village Alliance"));d.factions.putIfAbsent("raiders",new Faction("raiders","Iron Raiders"));d.factions.get("village").relations.put("raiders",Faction.Relation.HOSTILE);d.factions.get("raiders").relations.put("village",Faction.Relation.HOSTILE);d.markDirty();}
        if(id==3){String first=d.firstUnit();if(!first.isBlank()){d.equipped.put(sp.getUuid(),first);d.markDirty();}}
        return true; }
}
