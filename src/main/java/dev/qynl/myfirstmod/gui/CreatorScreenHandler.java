package dev.qynl.myfirstmod.gui;

import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CreatorScreenHandler extends ScreenHandler {
    private static final EquipmentSlot[] EDIT_SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND};
    private final SimpleInventory editorInventory=new SimpleInventory(6);
    public CreatorScreenHandler(int syncId, PlayerInventory inventory){super(ModScreenHandlers.CREATOR,syncId);
        for(int i=0;i<6;i++) addSlot(new Slot(editorInventory,i,225+(i%3)*22,88+(i/3)*22));
        if(inventory.player instanceof ServerPlayerEntity server){var d=UnitWorldData.get(server.getServer());var u=d.units.get(d.firstUnit());if(u!=null) for(int i=0;i<6;i++) editorInventory.setStack(i,u.equipment.getOrDefault(EDIT_SLOTS[i],ItemStack.EMPTY).copy());}
    }
    @Override public ItemStack quickMove(PlayerEntity player,int slot){return ItemStack.EMPTY;}
    @Override public boolean canUse(PlayerEntity player){return true;}
    @Override public boolean onButtonClick(PlayerEntity player,int id){ if(!(player instanceof ServerPlayerEntity sp))return true; UnitWorldData d=UnitWorldData.get(sp.getServer());
        if(id==0){UnitDefinition u=new UnitDefinition("guard","Village Guard",Identifier.of("minecraft","villager"));u.description="A configurable faction defender.";u.factionId="village";u.maxHealth=30;u.attackDamage=7;u.armor=12;u.equipment.put(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));u.equipment.put(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));u.equipment.put(EquipmentSlot.LEGS,new ItemStack(Items.IRON_LEGGINGS));u.equipment.put(EquipmentSlot.FEET,new ItemStack(Items.IRON_BOOTS));u.equipment.put(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));u.equipment.put(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));d.saveUnit(u);loadFirst(d);}
        if(id==1){UnitDefinition u=new UnitDefinition("archer","Royal Archer",Identifier.of("minecraft","skeleton"));u.factionId="village";u.role="ranged";u.attackDamage=5;u.equipment.put(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));d.saveUnit(u);loadFirst(d);}
        if(id==2){d.factions.putIfAbsent("village",new Faction("village","Village Alliance"));d.factions.putIfAbsent("raiders",new Faction("raiders","Iron Raiders"));d.factions.get("village").relations.put("raiders",Faction.Relation.HOSTILE);d.factions.get("raiders").relations.put("village",Faction.Relation.HOSTILE);d.markDirty();}
        if(id==3){String first=d.firstUnit();if(!first.isBlank()){d.equipped.put(sp.getUuid(),first);d.markDirty();}}
        if(id==4){String first=d.firstUnit();var source=d.units.get(first);if(source!=null){UnitDefinition copy=new UnitDefinition(source.toNbt());copy.id=first+"_copy";copy.name=source.name+" Copy";d.saveUnit(copy);}}
        if(id==5){String first=d.firstUnit();if(!first.isBlank()){d.units.remove(first);d.markDirty();}}
        if(id==6){var guard=d.units.get("guard");var archer=d.units.get("archer");if(guard!=null)dev.qynl.myfirstmod.unit.UnitSystem.spawnAt(sp,guard,sp.getX()+2,sp.getY(),sp.getZ()+2);if(archer!=null)dev.qynl.myfirstmod.unit.UnitSystem.spawnAt(sp,archer,sp.getX()+10,sp.getY(),sp.getZ()+10);}
        if(id==7){String first=d.firstUnit();var u=d.units.get(first);if(u!=null){for(int i=0;i<6;i++)u.equipment.put(EDIT_SLOTS[i],editorInventory.getStack(i).copy());d.markDirty();}}
        return true; }
    private void loadFirst(UnitWorldData d){var u=d.units.get(d.firstUnit());if(u!=null)for(int i=0;i<6;i++)editorInventory.setStack(i,u.equipment.getOrDefault(EDIT_SLOTS[i],ItemStack.EMPTY).copy());}
}
