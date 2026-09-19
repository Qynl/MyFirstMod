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
    }
}
