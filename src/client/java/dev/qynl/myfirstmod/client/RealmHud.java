package dev.qynl.myfirstmod.client;

import dev.qynl.myfirstmod.boss.NullWardenEntity;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.item.NullbladeItem;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Local presentation only. Cinematics never seize the player's camera or movement. */
public final class RealmHud {
    public static void register() {
        HudRenderCallback.EVENT.register((draw, tickCounter) -> render(draw));
    }
    private static void render(DrawContext draw) {
        MinecraftClient client=MinecraftClient.getInstance();
        if(client.player==null || client.world==null || client.options.hudHidden) return;
        int width=draw.getScaledWindowWidth(),height=draw.getScaledWindowHeight();
        var player=client.player;
        if(player.getMainHandStack().isOf(ModItems.ARENA_COMPASS) || player.getOffHandStack().isOf(ModItems.ARENA_COMPASS)) {
            draw.fill(8,8,204,47,0xD0101725);
            draw.fill(8,8,10,47,0xFF77E8E4);
            draw.drawTextWithShadow(client.textRenderer,Text.translatable("hud.myfirstmod.compass"),18,16,0xA3F4EC);
            var stack=player.getMainHandStack().isOf(ModItems.ARENA_COMPASS)?player.getMainHandStack():player.getOffHandStack();
            var target=dev.qynl.myfirstmod.item.ArenaCompassItem.target(stack);
            Text destination=client.world.getRegistryKey().equals(VoidPortalManager.NULL_REALM)
                    ? Text.translatable("message.myfirstmod.route",Text.translatable("route.myfirstmod."+dev.qynl.myfirstmod.item.ArenaCompassItem.mode(stack)),
                        (int)Math.hypot(player.getX()-target.getX(),player.getZ()-target.getZ()))
                    : Text.translatable("message.myfirstmod.compass_dormant");
            draw.drawTextWithShadow(client.textRenderer,destination,18,32,0xD0CFDF);
        }
        if(player.isUsingItem() && player.getActiveItem().getItem() instanceof NullbladeItem) {
            int charge=Math.min(100,player.getItemUseTime()*100/30);
            int x=width/2-60,y=height-68;
            draw.fill(x,y,x+120,y+4,0xC020213A);
            draw.fill(x,y,x+120*charge/100,y+4,0xFF8BF2E6);
            draw.drawCenteredTextWithShadow(client.textRenderer,Text.translatable("hud.myfirstmod.charge",charge),width/2,y-12,0xD0FFF6);
        }
        if(player.isUsingItem() && player.getActiveItem().isOf(ModItems.WAYFARER_THREAD)) {
            int charge=Math.min(100,player.getItemUseTime()*100/60);
            int x=width/2-60,y=height-68;
            draw.fill(x,y,x+120,y+4,0xC020213A);
            draw.fill(x,y,x+120*charge/100,y+4,0xFFD1ADF8);
            draw.drawCenteredTextWithShadow(client.textRenderer,Text.translatable("hud.myfirstmod.recall",charge),width/2,y-12,0xE0D2FF);
        }
        if(player.getMainHandStack().isOf(ModItems.PRISM_STAFF) || player.getOffHandStack().isOf(ModItems.PRISM_STAFF)) {
            int dust=0;
            for(int i=0;i<player.getInventory().size();i++) {
                var stack=player.getInventory().getStack(i);
                if(stack.isOf(ModItems.PRISM_DUST)) dust+=stack.getCount();
            }
            draw.fill(8,height-52,156,height-24,0xD0101725);
            draw.drawTextWithShadow(client.textRenderer,Text.translatable("hud.myfirstmod.staff_ammo",dust),16,height-42,0xC8B5F3);
        }
        if(!client.world.getRegistryKey().equals(VoidPortalManager.NULL_REALM)) return;
        NullWardenEntity boss=null;
        double nearest=48*48;
        for(var entity:client.world.getEntities()) if(entity instanceof NullWardenEntity w) {
            double distance=player.squaredDistanceTo(w);
            if(distance<nearest) {boss=w;nearest=distance;}
        }
        if(boss==null) return;
        int cinematic=boss.getCinematic();
        if(cinematic!=0) {
            int bar=Math.max(30,height/9);
            draw.fill(0,0,width,bar,0xE8000000);
            draw.fill(0,height-bar,width,height,0xE8000000);
            String key=switch(cinematic) {case 1->"awakening";case 2->"victory";default->"transition";};
            draw.drawCenteredTextWithShadow(client.textRenderer,Text.translatable("cinematic.myfirstmod."+key),width/2,bar/2-4,0xB1F7EF);
            draw.drawCenteredTextWithShadow(client.textRenderer,Text.translatable("cinematic.myfirstmod."+key+".subtitle"),width/2,height-bar/2-4,0xE1D9F2);
        } else if(boss.isCounterWindow()) {
            draw.drawCenteredTextWithShadow(client.textRenderer,Text.translatable("hud.myfirstmod.warden_open"),width/2,Math.max(90,height/5),0xABD6BC);
        } else if(boss.getVisualAttack()!=0) {
            // Text counterplay also communicates the tells without relying on particle color.
            draw.drawCenteredTextWithShadow(client.textRenderer,Text.translatable("attack.myfirstmod."+boss.getVisualAttack()),
                    width/2,Math.max(65,height/5),0xF6DF99);
        }
    }
}
