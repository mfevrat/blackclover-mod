package com.blackclovermod.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Calque qui brille dans le noir (comme les yeux des araignées). */
public class GlowLayer<T extends Entity, M extends EntityModel<T>> extends EyesLayer<T, M> {
    private final RenderType type;

    public GlowLayer(RenderLayerParent<T, M> parent, ResourceLocation texture) {
        super(parent);
        this.type = RenderType.eyes(texture);
    }

    @Override
    public RenderType renderType() {
        return type;
    }
}
