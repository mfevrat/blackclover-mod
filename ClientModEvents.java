package com.blackclovermod.client;

import com.blackclovermod.BlackCloverMod;
import com.blackclovermod.item.GrimoireItem;
import com.blackclovermod.registry.ModEntities;
import com.blackclovermod.registry.ModItems;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackCloverMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    private static ResourceLocation tex(String name) {
        return BlackCloverMod.id("textures/entity/" + name + ".png");
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.MAGIC_PROJECTILE.get(), NoopRenderer::new);
        e.registerEntityRenderer(ModEntities.FLOATING_GRIMOIRE.get(), FloatingGrimoireRenderer::new);
        e.registerEntityRenderer(ModEntities.DUNGEON_GUARDIAN.get(),
                ctx -> new BossRenderer<>(ctx, tex("dungeon_guardian"), tex("dungeon_guardian_glow"), 1.6f));
        e.registerEntityRenderer(ModEntities.ELF_KING.get(),
                ctx -> new BossRenderer<>(ctx, tex("elf_king"), tex("elf_king_glow"), 1.15f));
        e.registerEntityRenderer(ModEntities.ARCHDEMON.get(),
                ctx -> new BossRenderer<>(ctx, tex("archdemon"), tex("archdemon_glow"), 1.7f));
    }

    /** Ajoute le grimoire flottant à côté des joueurs. */
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers e) {
        for (String skin : e.getSkins()) {
            PlayerRenderer renderer = e.getSkin(skin);
            if (renderer != null) renderer.addLayer(new FloatingGrimoireLayer(renderer, e.getEntityModels()));
        }
    }

    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent e) {
        e.register(KeyBindings.CAST);
        e.register(KeyBindings.CYCLE);
    }

    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("mana_hud", ManaHud.HUD);
    }

    /** Colore la couverture du grimoire (calque 0) selon son attribut. */
    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item e) {
        e.register((stack, tintIndex) -> tintIndex == 0 ? GrimoireItem.color(stack) : -1, ModItems.GRIMOIRE.get());
    }

    private ClientModEvents() {}
}
