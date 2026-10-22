package com.blackclovermod.network;

import com.blackclovermod.magic.ClientMagicData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Serveur -> Client : envoie le mana, le niveau, etc. pour l'affichage (HUD). */
public record SyncMagicPacket(boolean has, int element, int leaves, int level, int xp, int xpNext,
                              float mana, float maxMana, int selected, int unlocked, int cooldown) {

    public void encode(FriendlyByteBuf b) {
        b.writeBoolean(has);
        b.writeVarInt(element);
        b.writeVarInt(leaves);
        b.writeVarInt(level);
        b.writeVarInt(xp);
        b.writeVarInt(xpNext);
        b.writeFloat(mana);
        b.writeFloat(maxMana);
        b.writeVarInt(selected);
        b.writeVarInt(unlocked);
        b.writeVarInt(cooldown);
    }

    public static SyncMagicPacket decode(FriendlyByteBuf b) {
        return new SyncMagicPacket(b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(),
                b.readVarInt(), b.readFloat(), b.readFloat(), b.readVarInt(), b.readVarInt(), b.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ClientMagicData.has = has;
        ClientMagicData.element = element;
        ClientMagicData.leaves = leaves;
        ClientMagicData.level = level;
        ClientMagicData.xp = xp;
        ClientMagicData.xpNext = xpNext;
        ClientMagicData.mana = mana;
        ClientMagicData.maxMana = maxMana;
        ClientMagicData.selected = selected;
        ClientMagicData.unlocked = unlocked;
        ClientMagicData.cooldown = cooldown;
    }
}
