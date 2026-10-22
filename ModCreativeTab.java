package com.blackclovermod.registry;

import com.blackclovermod.BlackCloverMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BlackCloverMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.blackclover"))
            .icon(() -> new ItemStack(ModItems.TOWER_SUMMONS.get()))
            .displayItems((params, out) -> {
                // Le grimoire n'est PAS dans l'onglet : il doit être gagné pendant la cérémonie !
                out.accept(ModItems.TOWER_SUMMONS.get());
                out.accept(ModItems.MANA_CRYSTAL.get());
                out.accept(ModItems.ANCIENT_PAGE.get());
                out.accept(ModItems.DEMON_FRAGMENT.get());
                out.accept(ModItems.DUNGEON_KEY.get());
                out.accept(ModItems.ELF_TEAR.get());
                out.accept(ModItems.DEMON_HEART.get());
            })
            .build());

    private ModCreativeTab() {}
}
