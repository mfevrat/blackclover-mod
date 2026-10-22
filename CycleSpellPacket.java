package com.blackclovermod.network;

import com.blackclovermod.magic.SpellCaster;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> Serveur : "je passe à la page suivante". */
public record CycleSpellPacket() {
    public void encode(FriendlyByteBuf buf) {}

    public static CycleSpellPacket decode(FriendlyByteBuf buf) { return new CycleSpellPacket(); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p != null) SpellCaster.cycle(p);
    }
}
