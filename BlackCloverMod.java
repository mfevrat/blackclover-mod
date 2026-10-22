package com.blackclovermod;

import com.blackclovermod.network.ModNetwork;
import com.blackclovermod.registry.ModCreativeTab;
import com.blackclovermod.registry.ModEntities;
import com.blackclovermod.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Point d'entrée du mod "Black Clover : Grimoires".
 */
@Mod(BlackCloverMod.MODID)
public class BlackCloverMod {
    public static final String MODID = "blackclover";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BlackCloverMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        modBus.addListener(ModEntities::onAttributes);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
