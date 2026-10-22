package com.blackclovermod.client;

import com.blackclovermod.BlackCloverMod;
import com.blackclovermod.magic.ClientMagicData;
import com.blackclovermod.network.CastSpellPacket;
import com.blackclovermod.network.CycleSpellPacket;
import com.blackclovermod.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackCloverMod.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (KeyBindings.CAST.consumeClick()) ModNetwork.toServer(new CastSpellPacket());
        while (KeyBindings.CYCLE.consumeClick()) ModNetwork.toServer(new CycleSpellPacket());
        ClientMagicData.tick();
    }

    private ClientForgeEvents() {}
}
