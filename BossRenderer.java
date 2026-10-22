package com.blackclovermod.client;

import com.blackclovermod.entity.boss.MagicBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Rendu des boss : modèle humanoïde agrandi + calque lumineux (yeux / runes qui brillent dans le noir).
 * Pendant l'apparition, le boss est dessiné plus bas : il "sort du sol".
 * Pour des modèles plus fous (cornes, ailes 3D...), utilise Blockbench + GeckoLib plus tard !
 */
public class BossRenderer<T extends MagicBossEntity> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation texture;
    private final float baseScale;

    public BossRenderer(EntityRendererProvider.Context ctx, ResourceLocation texture, ResourceLocation glow, float scale) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f * scale);
        this.texture = texture;
        this.baseScale = scale;
        if (glow != null) addLayer(new GlowLayer<>(this, glow));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    protected void scale(T entity, PoseStack ps, float partialTick) {
        float s = baseScale * entity.renderScaleMultiplier();
        ps.scale(s, s, s);
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        int st = entity.spawnTicks();
        if (st > 0) {
            float prog = Math.max(0f, (st - partialTick) / MagicBossEntity.SPAWN_DURATION);
            ps.translate(0, -prog * 2.0 * baseScale, 0);
        }
        super.render(entity, yaw, partialTick, ps, buf, light);
        ps.popPose();
    }
}
