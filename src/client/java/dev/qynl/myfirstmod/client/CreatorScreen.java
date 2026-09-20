package dev.qynl.myfirstmod.client;

import dev.qynl.myfirstmod.gui.CreatorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/** A deliberately quiet dashboard: navigation, cards, and actions are separated so the editor can grow without a monolithic screen. */
public final class CreatorScreen extends HandledScreen<CreatorScreenHandler> {
    private int tab;
    private final int panel = 0xff172235;
    public CreatorScreen(CreatorScreenHandler handler, PlayerInventory inventory, Text title){super(handler,inventory,title); backgroundWidth=430; backgroundHeight=250;}
    @Override protected void init(){super.init(); rebuild();}
    private void rebuild(){clearChildren(); int left=x+18, top=y+42;
        addDrawableChild(ButtonWidget.builder(Text.literal("UNITS"),b->{tab=0;rebuild();}).dimensions(x+12,y+10,95,22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("FACTIONS"),b->{tab=1;rebuild();}).dimensions(x+112,y+10,95,22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("BATTLE"),b->{tab=2;rebuild();}).dimensions(x+212,y+10,95,22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("SETTINGS"),b->{tab=3;rebuild();}).dimensions(x+312,y+10,95,22).build());
        if(tab==0){button("New Guard",0,left,top);button("New Archer",1,left,top+27);button("Create Faction",2,left,top+54);button("Equip First",3,left,top+81);button("Duplicate First",4,left+205,top);button("Delete First",5,left+205,top+27);button("Save Equipment",7,left+205,top+54);}
        else if(tab==1){button("Create Kingdom + Raiders",2,left,top);button("Equip First Unit",3,left,top+27);}
        else if(tab==2){button("Start Sample Battle",6,left,top);}
    }
    private void button(String label,int id,int x,int y){addDrawableChild(ButtonWidget.builder(Text.literal(label),b->client.interactionManager.clickButton(handler.syncId,id)).dimensions(x,y,190,22).build());}
    @Override protected void drawBackground(DrawContext c,float delta,int mx,int my){c.fill(0,0,width,height,0xaa08101d);c.fill(x,y,x+backgroundWidth,y+backgroundHeight,panel);c.fill(x+12,y+38,x+backgroundWidth-12,y+backgroundHeight-12,0xff0d1626);}
    @Override public void render(DrawContext c,int mx,int my,float delta){renderBackground(c);drawBackground(c,delta,mx,my);c.drawTextWithShadow(textRenderer,Text.literal("UNIT & FACTION SANDBOX"),x+18,y+30,0xffeef5ff);drawContent(c);super.render(c,mx,my,delta);}
    private void drawContent(DrawContext c){int tx=x+225, ty=y+57;c.drawTextWithShadow(textRenderer,Text.literal(tab==0?"UNIT LIBRARY":tab==1?"FACTION LIBRARY":tab==2?"BATTLE SANDBOX":"SERVER SETTINGS"),tx,ty,0xff9ecbff);
        if(tab==0){c.drawText(textRenderer,Text.literal("Saved templates"),tx,ty+22,0xffb8c4d9);c.drawText(textRenderer,Text.literal("Guard  •  Villager  •  Melee"),tx,ty+43,0xffe8edf5);c.drawText(textRenderer,Text.literal("Archer •  Skeleton • Ranged"),tx,ty+61,0xffe8edf5);c.drawText(textRenderer,Text.literal("Use /unit commands for custom IDs"),tx,ty+94,0xff8294ad);c.drawText(textRenderer,Text.literal("Equipment editor"),x+224,y+76,0xff9ecbff);c.drawText(textRenderer,Text.literal("H  C  L     F  M  O"),x+225,y+137,0xff8294ad);}
        else if(tab==1){c.drawText(textRenderer,Text.literal("Relations are server persistent"),tx,ty+25,0xffe8edf5);c.drawText(textRenderer,Text.literal("ALLIED   NEUTRAL   HOSTILE"),tx,ty+48,0xff8eb5e8);c.drawText(textRenderer,Text.literal("No friendly fire within a faction"),tx,ty+76,0xff8294ad);}
        else if(tab==2){c.drawText(textRenderer,Text.literal("Sample battle"),tx,ty+25,0xffe8edf5);c.drawText(textRenderer,Text.literal("Village Alliance  VS  Iron Raiders"),tx,ty+47,0xffffcf72);c.drawText(textRenderer,Text.literal("Spawns saved templates nearby"),tx,ty+72,0xff8294ad);}
        else {c.drawText(textRenderer,Text.literal("AI runs every 10 server ticks"),tx,ty+25,0xffe8edf5);c.drawText(textRenderer,Text.literal("World state is server authoritative"),tx,ty+48,0xffe8edf5);c.drawText(textRenderer,Text.literal("Limits and policies are ready to extend"),tx,ty+76,0xff8294ad);}}
    @Override public boolean keyPressed(int key,int scan,int mods){if(key==256){close();return true;}return super.keyPressed(key,scan,mods);}
}
