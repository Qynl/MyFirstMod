package dev.qynl.myfirstmod.client;

import dev.qynl.myfirstmod.item.*;
import dev.qynl.myfirstmod.mob.*;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.text.Text;

/** Restrained overlays: no forced camera, flashes, or opaque combat cutscenes. */
public final class PilgrimageHud {
    private static String biome="",title="";
    private static int titleTicks,quietTicks;
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(client.world==null || client.player==null || !client.world.getRegistryKey().equals(VoidPortalManager.NULL_REALM)) {
                biome="";titleTicks=0;quietTicks=0;return;
            }
            if(client.isPaused()) return;
            if(titleTicks>0) titleTicks--;if(quietTicks>0) quietTicks--;
            if(client.player.age%20!=0) return;
            String current=client.world.getBiome(client.player.getBlockPos()).getKey().map(k->k.getValue().toString()).orElse("");
            if(!current.equals(biome) && quietTicks==0 && current.startsWith("myfirstmod:")) {
                biome=current;title=current.substring(current.indexOf(':')+1);titleTicks=100;quietTicks=240;
            }
        });
        HudRenderCallback.EVENT.register((draw,counter)->{
            var c=MinecraftClient.getInstance();if(c.player==null || c.world==null || c.options.hudHidden) return;
            int w=draw.getScaledWindowWidth(),h=draw.getScaledWindowHeight();
            boolean realm=c.world.getRegistryKey().equals(VoidPortalManager.NULL_REALM);
            if(realm && titleTicks>0 && c.currentScreen==null) {
                int alpha=Math.min(255,Math.min(titleTicks,100-titleTicks)*18);
                if(alpha>8) {
                    int y=h/3;
                    draw.fill(w/2-70,y+14,w/2+70,y+15,(alpha<<24)|0x90785B);
                    draw.drawCenteredTextWithShadow(c.textRenderer,Text.translatable("biome.myfirstmod."+title),w/2,y,(alpha<<24)|0xE1D1AD);
                    draw.drawCenteredTextWithShadow(c.textRenderer,Text.translatable("region.myfirstmod."+title),w/2,y+23,(alpha<<24)|0xB7ABA0);
                }
            }
            var stack=c.player.getMainHandStack().isOf(ModItems.ASHEN_FLASK)?c.player.getMainHandStack():c.player.getOffHandStack();
            if(stack.isOf(ModItems.ASHEN_FLASK)) {
                var data=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();
                int capacity=Math.max(3,Math.min(5,data.getInt("AshenCapacity"))),charges=Math.max(0,Math.min(capacity,data.getInt("AshenCharges")));
                int x=Math.max(8,w-139),y=h-61;
                draw.fill(x,y,w-8,y+34,0xD015141B);
                draw.drawTextWithShadow(c.textRenderer,Text.translatable("hud.myfirstmod.flask",charges,capacity),x+7,y+6,0xE5C88A);
                for(int i=0;i<capacity;i++) draw.fill(x+8+i*20,y+22,x+22+i*20,y+26,i<charges?0xFFE3BA70:0xFF403D49);
                if(c.player.isUsingItem() && c.player.getActiveItem().isOf(ModItems.ASHEN_FLASK)) {
                    var active=c.player.getActiveItem();int duration=AshenFlaskItem.resting(active)?100:32;
                    int progress=Math.min(120,c.player.getItemUseTime()*120/duration);
                    draw.fill(w/2-60,h-68,w/2+60,h-64,0xB0302832);draw.fill(w/2-60,h-68,w/2-60+progress,h-64,0xFFE5C88A);
                }
            }
            if(c.targetedEntity instanceof HostileEntity enemy && enemy.getType().getTranslationKey().startsWith("entity.myfirstmod.") && enemy.isAlive()) {
                int x=w/2-70,y=54;
                draw.drawCenteredTextWithShadow(c.textRenderer,enemy.getDisplayName(),w/2,y,0xD5C3B0);
                draw.fill(x,y+12,x+140,y+15,0xC0352932);draw.fill(x,y+12,x+(int)(140*Math.max(0,Math.min(1,enemy.getHealth()/enemy.getMaxHealth()))),y+15,0xFFA5675E);
                int tell=enemy instanceof RiftSentinelEntity knight?knight.tell():enemy instanceof ShardstalkerEntity hunter?hunter.tell():0;
                if(tell!=0) draw.drawCenteredTextWithShadow(c.textRenderer,Text.translatable(tell<0?"hud.myfirstmod.recovery":enemy instanceof RiftSentinelEntity?"hud.myfirstmod.cleave":"hud.myfirstmod.pounce"),w/2,y+22,tell<0?0xACD5BD:0xEDC794);
            }
        });
    }
}
