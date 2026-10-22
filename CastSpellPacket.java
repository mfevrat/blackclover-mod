package com.blackclovermod.network;

import com.blackclovermod.magic.SpellCaster;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> Serveur : "je lance le sort sélectionné". */
public record CastSpellPacket() {
    public void encode(FriendlyByteBuf buf) {}

    public static CastSpellPacket decode(FriendlyByteBuf buf) { return new CastSpellPacket(); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p != null) SpellCaster.tryCast(p);
    }
}
