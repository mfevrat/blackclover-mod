package com.blackclovermod.magic;

import com.blackclovermod.ceremony.CeremonyManager;
import com.blackclovermod.item.GrimoireItem;
import com.blackclovermod.network.GrimoireFxPacket;
import com.blackclovermod.network.ModNetwork;
import com.blackclovermod.util.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Gère le lancement et le changement de sort (appelé quand le joueur appuie sur R ou G). */
public final class SpellCaster {

    public static boolean hasGrimoireItem(Player p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof GrimoireItem) return true;
        }
        return false;
    }

    public static void tryCast(ServerPlayer p) {
        if (CeremonyManager.inCeremony(p) || p.isSpectator()) return;
        if (!MagicData.hasGrimoire(p)) {
            p.displayClientMessage(Component.translatable("msg.blackclover.no_grimoire").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!hasGrimoireItem(p)) {
            p.displayClientMessage(Component.translatable("msg.blackclover.grimoire_missing").withStyle(ChatFormatting.RED), true);
            return;
        }
        List<Spell> spells = Spells.of(MagicData.element(p));
        int unlocked = MagicData.unlockedSpells(p);
        if (spells.isEmpty() || unlocked <= 0) return;
        int sel = Mth.clamp(MagicData.selected(p), 0, unlocked - 1);
        Spell s = spells.get(sel);

        long now = p.level().getGameTime();
        long end = MagicData.cooldownEnd(p, sel);
        if (now < end) {
            p.displayClientMessage(Component.translatable("msg.blackclover.cooldown", s.name(),
                    String.format("%.1f", (end - now) / 20f)).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        float mana = MagicData.mana(p);
        if (!p.isCreative() && mana < s.manaCost()) {
            p.displayClientMessage(Component.translatable("msg.blackclover.no_mana").withStyle(ChatFormatting.AQUA), true);
            Fx.sound(p.level(), p.position(), SoundEvents.FIRE_EXTINGUISH, 0.5f, 1.5f);
            return;
        }
        if (!s.action().cast(p, MagicData.power(p))) return;

        if (!p.isCreative()) MagicData.setMana(p, mana - s.manaCost());
        MagicData.setCooldownEnd(p, sel, now + s.cooldown());
        castFx(p, MagicData.element(p));
        MagicData.addXp(p, 2);
        MagicData.sync(p);
    }

    public static void cycle(ServerPlayer p) {
        if (!MagicData.hasGrimoire(p)) return;
        int unlocked = MagicData.unlockedSpells(p);
        if (unlocked <= 1) {
            p.displayClientMessage(Component.translatable("msg.blackclover.only_one_spell").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int next = (MagicData.selected(p) + 1) % unlocked;
        MagicData.setSelected(p, next);
        Spell s = Spells.of(MagicData.element(p)).get(next);
        p.displayClientMessage(Component.translatable("msg.blackclover.selected", s.name()).withStyle(s.element().displayName().getStyle()), true);
        Fx.sound(p.level(), p.position(), SoundEvents.BOOK_PAGE_TURN, 1f, 1.3f);
        MagicData.sync(p);
    }

    /** Le cercle magique au sol + le grimoire qui apparaît à côté du joueur. */
    private static void castFx(ServerPlayer p, MagicElement el) {
        ServerLevel l = p.serverLevel();
        Fx.magicCircle(l, el.dust(1f), p.position().add(0, 0.05, 0), 1.3, (int) l.getGameTime());
        Fx.burst(l, ParticleTypes.ENCHANT, p.position().add(0, 1.2, 0), 25, 0.6, 0.6);
        Fx.sound(l, p.position(), SoundEvents.BOOK_PAGE_TURN, 1f, 1.2f);
        ModNetwork.toTrackingAndSelf(p, new GrimoireFxPacket(p.getId(), GrimoireItem.coverColor(el, MagicData.leaves(p)), 60));
    }

    private SpellCaster() {}
}
