package dev.qynl.myfirstmod.client.render;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;

public final class RiftSentinelRenderer extends ZombieEntityRenderer {
    private static final Identifier TEXTURE=Identifier.of("myfirstmod","textures/entity/rift_sentinel.png");
    public RiftSentinelRenderer(EntityRendererFactory.Context context) {super(context);}
    @Override public Identifier getTexture(ZombieEntity entity) {return TEXTURE;}
}
