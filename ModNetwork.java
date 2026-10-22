package com.blackclovermod.network;

import com.blackclovermod.BlackCloverMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            BlackCloverMod.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(CastSpellPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CastSpellPacket::encode).decoder(CastSpellPacket::decode)
                .consumerMainThread(CastSpellPacket::handle).add();
        CHANNEL.messageBuilder(CycleSpellPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CycleSpellPacket::encode).decoder(CycleSpellPacket::decode)
                .consumerMainThread(CycleSpellPacket::handle).add();
        CHANNEL.messageBuilder(SyncMagicPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncMagicPacket::encode).decoder(SyncMagicPacket::decode)
                .consumerMainThread(SyncMagicPacket::handle).add();
        CHANNEL.messageBuilder(GrimoireFxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GrimoireFxPacket::encode).decoder(GrimoireFxPacket::decode)
                .consumerMainThread(GrimoireFxPacket::handle).add();
    }

    public static void toServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void toPlayer(ServerPlayer p, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), msg);
    }

    public static void toTrackingAndSelf(ServerPlayer p, Object msg) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), msg);
    }

    private ModNetwork() {}
}
