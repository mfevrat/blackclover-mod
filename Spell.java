package com.blackclovermod.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/** Un sort (une "page" du grimoire). */
public record Spell(String id, MagicElement element, int manaCost, int cooldown, Action action) {

    @FunctionalInterface
    public interface Action {
        /** @return true si le sort a vraiment été lancé (le mana est alors consommé). */
        boolean cast(ServerPlayer player, float power);
    }

    public MutableComponent name() {
        return Component.translatable("spell.blackclover." + id);
    }
}
