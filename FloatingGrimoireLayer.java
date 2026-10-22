package com.blackclovermod.client;

import com.blackclovermod.magic.ClientFxState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Comme dans l'animé : quand un joueur lance un sort, son grimoire apparaît
 * et flotte ouvert à côté de lui, les pages tournant toutes seules.
 * (Visible en vue 3e personne et par les autres joueurs.)
 * Astuce : si la position ne te plaît pas, modifie les valeurs de ps.translate(...).
 */
public class FloatingGrimoireLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation BOOK = new ResourceLocation("textures/entity/enchanting_table_book.png");
    private final BookModel book;

    public FloatingGrimoireLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        this.book = new BookModel(models.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
        ClientFxState.Active a = ClientFxState.get(player.getId());
        if (a == null) return;
        float p = a.progress();
        float s = Mth.clamp(Math.min(p * 8f, (1f - p) * 5f), 0f, 1f) * 0.85f; // apparition / disparition

        ps.pushPose();
        // espace du modèle : x négatif = côté droit du joueur, y positif = vers le bas
        ps.translate(-0.75, 0.05 + Mth.sin(age * 0.12f) * 0.06f, -0.35);
        ps.mulPose(Axis.ZP.rotationDegrees(180f));
        ps.mulPose(Axis.YP.rotationDegrees(110f));
        ps.mulPose(Axis.ZP.rotationDegrees(70f));
        ps.scale(s, s, s);
        float f1 = Mth.frac(age * 0.2f + 0.25f) * 1.6f - 0.3f;
        float f2 = Mth.frac(age * 0.2f + 0.75f) * 1.6f - 0.3f;
        book.setupAnim(age, Mth.clamp(f1, 0f, 1f), Mth.clamp(f2, 0f, 1f), 1f);
        int c = a.color();
        book.renderToBuffer(ps, buf.getBuffer(RenderType.entityCutout(BOOK)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                ((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, 1f);
        ps.popPose();
    }
}
