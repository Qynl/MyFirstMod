package dev.qynl.myfirstmod.client;

import dev.qynl.myfirstmod.gui.CreatorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class CreatorScreen extends HandledScreen<CreatorScreenHandler> {
    public CreatorScreen(CreatorScreenHandler handler, PlayerInventory inventory, Text title){super(handler,inventory,title);}
    @Override protected void init(){super.init(); int x=width/2-110; int y=height/2-65;
        addDrawableChild(ButtonWidget.builder(Text.literal("Create Guard"),b->client.interactionManager.clickButton(handler.syncId,0)).dimensions(x,y,220,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Create Archer"),b->client.interactionManager.clickButton(handler.syncId,1)).dimensions(x,y+24,220,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Create hostile factions"),b->client.interactionManager.clickButton(handler.syncId,2)).dimensions(x,y+48,220,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Equip first saved unit"),b->client.interactionManager.clickButton(handler.syncId,3)).dimensions(x,y+72,220,20).build()); }
    @Override protected void drawBackground(DrawContext context,float delta,int mouseX,int mouseY){context.fill(0,0,width,height,0x990b1020);context.drawCenteredTextWithShadow(textRenderer,"UNIT CREATOR",width/2,height/2-92,0xffd9e5ff);}
    @Override public void render(DrawContext c,int mx,int my,float delta){drawBackground(c,delta,mx,my);super.render(c,mx,my,delta);}
}
