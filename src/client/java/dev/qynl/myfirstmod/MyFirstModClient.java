package dev.qynl.myfirstmod;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.boss.ModEntities;
import dev.qynl.myfirstmod.client.NullWardenTexture;
import dev.qynl.myfirstmod.client.model.NullWardenModel;
import dev.qynl.myfirstmod.client.render.NullWardenRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;

public class MyFirstModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.VOID_PORTAL, RenderLayer.getTranslucent());

        EntityModelLayerRegistry.registerModelLayer(
                NullWardenRenderer.MODEL_LAYER,
                NullWardenModel::getTexturedModelData);

        NullWardenTexture.TEXTURE = NullWardenTexture.register();
        NullWardenTexture.GLOW_TEXTURE = NullWardenTexture.registerGlow();

        EntityRendererRegistry.register(ModEntities.NULL_WARDEN, NullWardenRenderer::new);
        EntityRendererRegistry.register(ModEntities.RIFT_SENTINEL, dev.qynl.myfirstmod.client.render.RiftSentinelRenderer::new);
        EntityRendererRegistry.register(ModEntities.SHARDSTALKER, dev.qynl.myfirstmod.client.render.ShardstalkerRenderer::new);
        dev.qynl.myfirstmod.client.RealmHud.register();
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getNamespace().equals("myfirstmod")) {
                String key = stack.getTranslationKey() + ".tooltip";
                if (net.minecraft.client.resource.language.I18n.hasTranslation(key)) {
                    var client=net.minecraft.client.MinecraftClient.getInstance();
                    int width=Math.max(100,Math.min(240,client.getWindow().getScaledWidth()-24));
                    for(var line:client.textRenderer.getTextHandler().wrapLines(
                            net.minecraft.text.Text.translatable(key),width,net.minecraft.text.Style.EMPTY))
                        lines.add(net.minecraft.text.Text.literal(line.getString()).formatted(net.minecraft.util.Formatting.GRAY));
                }
            }
        });
        net.minecraft.client.item.ModelPredicateProviderRegistry.register(
                dev.qynl.myfirstmod.item.ModItems.ARENA_COMPASS, net.minecraft.util.Identifier.ofVanilla("angle"),
                (stack, world, entity, seed) -> {
                    if (entity == null) return 0;
                    if (!entity.getWorld().getRegistryKey().equals(dev.qynl.myfirstmod.portal.VoidPortalManager.NULL_REALM))
                        return (entity.age % 64) / 64f;
                    var target = dev.qynl.myfirstmod.item.ArenaCompassItem.target(stack);
                    double bearing = Math.atan2(entity.getX()-target.getX(), target.getZ()-entity.getZ()) - Math.toRadians(entity.getYaw());
                    return (float) ((bearing / (Math.PI * 2) % 1 + 1) % 1);
                });
    }
}
