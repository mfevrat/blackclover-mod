package com.blackclovermod.client;

import com.blackclovermod.entity.FloatingGrimoireEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dessine les grimoires volants avec le modèle de livre de la table d'enchantement, teinté en couleur. */
public class FloatingGrimoireRenderer extends EntityRenderer<FloatingGrimoireEntity> {
    private static final ResourceLocation BOOK = new ResourceLocation("textures/entity/enchanting_table_book.png");
    private final BookModel book;

    public FloatingGrimoireRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.book = new BookModel(ctx.bakeLayer(ModelLayers.BOOK));
        this.shadowRadius = 0f;
    }

    @Override
    public void render(FloatingGrimoireEntity e, float entityYaw, float partialTicks, PoseStack ps, MultiBufferSource buf, int light) {
        float t = e.tickCount + partialTicks;
        int mode = e.getMode();
        float scale = e.getVisualScale();

        ps.pushPose();
        ps.translate(0, 0.35 + Mth.sin(t * 0.15f) * 0.05f, 0);
        ps.scale(scale, scale, scale);
        float yaw = Mth.rotLerp(partialTicks, e.yRotO, e.getYRot());
        ps.mulPose(Axis.YP.rotationDegrees(-yaw + 90f));
        ps.mulPose(Axis.ZP.rotationDegrees(80f));

        float open;
        float flipSpeed = 0.06f;
        switch (mode) {
            case FloatingGrimoireEntity.OPEN -> { open = 1f; flipSpeed = 0.35f; }       // pages qui défilent à toute vitesse
            case FloatingGrimoireEntity.SCRIPTED -> open = 0.65f + Mth.sin(t * 0.3f) * 0.2f;
            case FloatingGrimoireEntity.FLEE -> open = 0.08f;                           // fermé, tremblant
            default -> open = 0.45f + Mth.sin(t * 0.5f) * 0.35f;                        // battements d'ailes
        }
        float f1 = Mth.frac(t * flipSpeed + 0.25f) * 1.6f - 0.3f;
        float f2 = Mth.frac(t * flipSpeed + 0.75f) * 1.6f - 0.3f;
        book.setupAnim(t, Mth.clamp(f1, 0f, 1f), Mth.clamp(f2, 0f, 1f), open);

        int c = e.getColor();
        float r = ((c >> 16) & 255) / 255f, g = ((c >> 8) & 255) / 255f, b = (c & 255) / 255f;
        int l = e.isGlowingBook() ? LightTexture.FULL_BRIGHT : light;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(BOOK));
        book.renderToBuffer(ps, vc, l, OverlayTexture.NO_OVERLAY, r, g, b, 1f);
        ps.popPose();
        super.render(e, entityYaw, partialTicks, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(FloatingGrimoireEntity e) {
        return BOOK;
    }
}
