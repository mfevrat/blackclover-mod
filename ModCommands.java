package com.blackclovermod.event;

import com.blackclovermod.ceremony.CeremonyManager;
import com.blackclovermod.item.GrimoireItem;
import com.blackclovermod.magic.MagicData;
import com.blackclovermod.magic.MagicElement;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/**
 * Commandes de TEST (réservées aux opérateurs / mode triche) :
 *   /grimoire ceremony         → relance la cérémonie (efface ton grimoire actuel)
 *   /grimoire reset            → supprime ta magie
 *   /grimoire give fire 4      → te donne directement un grimoire (élément, feuilles)
 *   /grimoire level 6          → change le niveau du grimoire
 *   /grimoire mana             → remplit ton mana
 */
public final class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("grimoire").requires(s -> s.hasPermission(2))
                .then(Commands.literal("ceremony").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    MagicData.reset(p);
                    MagicData.sync(p);
                    CeremonyManager.start(p);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    MagicData.reset(p);
                    MagicData.sync(p);
                    c.getSource().sendSuccess(() -> Component.literal("Magie réinitialisée."), false);
                    return 1;
                }))
                .then(Commands.literal("give")
                        .then(Commands.argument("element", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(MagicElement.values()).filter(e -> e != MagicElement.EARTH).map(e -> e.id), b))
                                .then(Commands.argument("feuilles", IntegerArgumentType.integer(3, 5)).executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    MagicElement el = MagicElement.byId(StringArgumentType.getString(c, "element"));
                                    int leaves = IntegerArgumentType.getInteger(c, "feuilles");
                                    MagicData.grant(p, el, leaves);
                                    p.getInventory().add(GrimoireItem.create(el, leaves, p.getGameProfile().getName()));
                                    MagicData.sync(p);
                                    c.getSource().sendSuccess(() -> Component.literal("Grimoire donné : ").append(el.displayName()), false);
                                    return 1;
                                }))))
                .then(Commands.literal("level")
                        .then(Commands.argument("niveau", IntegerArgumentType.integer(1, MagicData.MAX_LEVEL)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            MagicData.setLevel(p, IntegerArgumentType.getInteger(c, "niveau"));
                            MagicData.sync(p);
                            c.getSource().sendSuccess(() -> Component.literal("Niveau du grimoire modifié."), false);
                            return 1;
                        })))
                .then(Commands.literal("mana").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    MagicData.setMana(p, MagicData.maxMana(p));
                    MagicData.sync(p);
                    return 1;
                })));
    }

    private ModCommands() {}
}
