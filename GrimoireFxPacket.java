package com.blackclovermod.network;

import com.blackclovermod.magic.ClientFxState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Serveur -> Clients proches : fait apparaître le grimoire flottant à côté d'un joueur qui lance un sort. */
public record GrimoireFxPacket(int entityId, int color, int ticks) {
    public void encode(FriendlyByteBuf b) {
        b.writeVarInt(entityId);
        b.writeInt(color);
        b.writeVarInt(ticks);
    }

    public static GrimoireFxPacket decode(FriendlyByteBuf b) {
        return new GrimoireFxPacket(b.readVarInt(), b.readInt(), b.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ClientFxState.activate(entityId, color, ticks);
    }
}
