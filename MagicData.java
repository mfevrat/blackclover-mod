package com.blackclovermod.magic;

import com.blackclovermod.BlackCloverMod;
import com.blackclovermod.item.GrimoireItem;
import com.blackclovermod.network.ModNetwork;
import com.blackclovermod.network.SyncMagicPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Données magiques du joueur, stockées dans son NBT "PlayerPersisted" (conservé après la mort).
 * Toutes les méthodes sont à appeler côté SERVEUR.
 */
public final class MagicData {
    public static final int MAX_LEVEL = 10;
    public static final int MAX_BONUS_MANA = 200;

    private static CompoundTag tag(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains("PlayerPersisted", 10)) pd.put("PlayerPersisted", new CompoundTag());
        CompoundTag persisted = pd.getCompound("PlayerPersisted");
        if (!persisted.contains(BlackCloverMod.MODID, 10)) persisted.put(BlackCloverMod.MODID, new CompoundTag());
        return persisted.getCompound(BlackCloverMod.MODID);
    }

    /** Copie les données après la mort (appelé depuis PlayerEvent.Clone). */
    public static void copy(Player from, Player to) {
        tag(to);
        to.getPersistentData().getCompound("PlayerPersisted").put(BlackCloverMod.MODID, tag(from).copy());
    }

    public static void reset(Player p) {
        p.getPersistentData().getCompound("PlayerPersisted").remove(BlackCloverMod.MODID);
    }

    // ---------- Informations de base ----------
    public static boolean hasGrimoire(Player p) { return tag(p).getBoolean("Has"); }
    public static MagicElement element(Player p) { return MagicElement.byId(tag(p).getString("Element")); }
    public static int leaves(Player p) { int l = tag(p).getInt("Leaves"); return l == 0 ? 3 : l; }
    public static int level(Player p) { return Math.max(1, tag(p).getInt("Level")); }
    public static int xp(Player p) { return tag(p).getInt("Xp"); }
    public static int selected(Player p) { return tag(p).getInt("Selected"); }
    public static void setSelected(Player p, int s) { tag(p).putInt("Selected", s); }
    public static int bonusMana(Player p) { return tag(p).getInt("BonusMana"); }
    public static void addBonusMana(Player p, int v) { tag(p).putInt("BonusMana", Math.min(MAX_BONUS_MANA, bonusMana(p) + v)); }

    public static void grant(Player p, MagicElement element, int leaves) {
        CompoundTag t = tag(p);
        t.putBoolean("Has", true);
        t.putString("Element", element.id);
        t.putInt("Leaves", leaves);
        t.putInt("Level", 1);
        t.putInt("Xp", 0);
        t.putInt("Selected", 0);
        t.putFloat("Mana", maxMana(p));
    }

    public static void setLevel(Player p, int level) {
        tag(p).putInt("Level", Mth.clamp(level, 1, MAX_LEVEL));
        tag(p).putInt("Xp", 0);
    }

    // ---------- Mana ----------
    public static float maxMana(Player p) {
        return 100 + 15 * (level(p) - 1) + bonusMana(p) + (leaves(p) >= 4 ? 50 : 0);
    }

    public static float mana(Player p) { return tag(p).getFloat("Mana"); }
    public static void setMana(Player p, float v) { tag(p).putFloat("Mana", Mth.clamp(v, 0, maxMana(p))); }

    /** Multiplicateur de puissance des sorts. */
    public static float power(Player p) {
        int l = leaves(p);
        return 1f + 0.08f * (level(p) - 1) + (l == 4 ? 0.3f : 0f) + (l == 5 ? 0.25f : 0f);
    }

    /** Nombre de pages (sorts) débloquées : niveau 1 / 3 / 6. Un trèfle à 4 ou 5 feuilles a 2 niveaux d'avance. */
    public static int unlockedSpells(Player p) {
        int lvl = level(p) + (leaves(p) >= 4 ? 2 : 0);
        int n = 1;
        if (lvl >= 3) n = 2;
        if (lvl >= 6) n = 3;
        return Math.min(n, Spells.of(element(p)).size());
    }

    public static int requiredLevel(int spellIndex, int leaves) {
        int base = spellIndex == 0 ? 1 : spellIndex == 1 ? 3 : 6;
        return Math.max(1, base - (leaves >= 4 ? 2 : 0));
    }

    // ---------- Recharges ----------
    public static long cooldownEnd(Player p, int i) { return tag(p).getLong("Cd" + i); }
    public static void setCooldownEnd(Player p, int i, long end) { tag(p).putLong("Cd" + i, end); }

    // ---------- Expérience de grimoire ----------
    public static int xpForNext(int level) { return 80 + level * 60; }

    public static void addXp(ServerPlayer p, int amount) {
        if (!hasGrimoire(p)) return;
        CompoundTag t = tag(p);
        int lvl = level(p);
        if (lvl >= MAX_LEVEL) return;
        int before = unlockedSpells(p);
        int xp = t.getInt("Xp") + amount;
        boolean leveled = false;
        while (lvl < MAX_LEVEL && xp >= xpForNext(lvl)) {
            xp -= xpForNext(lvl);
            lvl++;
            leveled = true;
        }
        t.putInt("Xp", lvl >= MAX_LEVEL ? 0 : xp);
        t.putInt("Level", lvl);
        if (leveled) {
            p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 0.8f);
            p.serverLevel().sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 120, 0.8, 1, 0.8, 1);
            p.displayClientMessage(Component.translatable("msg.blackclover.levelup", lvl).withStyle(ChatFormatting.GOLD), true);
            int after = unlockedSpells(p);
            if (after > before) {
                List<Spell> spells = Spells.of(element(p));
                for (int i = before; i < after; i++) {
                    p.sendSystemMessage(Component.translatable("msg.blackclover.new_page", spells.get(i).name()).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1f);
            }
        }
    }

    // ---------- Retour de la Tour (sécurité en cas de déconnexion) ----------
    public static void setPendingReturn(Player p, ResourceKey<Level> dim, Vec3 pos) {
        CompoundTag r = new CompoundTag();
        r.putString("Dim", dim.location().toString());
        r.putDouble("X", pos.x);
        r.putDouble("Y", pos.y);
        r.putDouble("Z", pos.z);
        tag(p).put("PendingReturn", r);
    }

    public static CompoundTag pendingReturn(Player p) {
        CompoundTag t = tag(p);
        return t.contains("PendingReturn", 10) ? t.getCompound("PendingReturn") : null;
    }

    public static void clearPendingReturn(Player p) { tag(p).remove("PendingReturn"); }

    public static ResourceLocation pendingDim(CompoundTag r) { return new ResourceLocation(r.getString("Dim")); }

    // ---------- Grimoire perdu à la mort ----------
    public static void setRestoreGrimoire(Player p, boolean b) { tag(p).putBoolean("Restore", b); }
    public static boolean restoreGrimoire(Player p) { return tag(p).getBoolean("Restore"); }

    public static ItemStack createGrimoire(Player p) {
        return GrimoireItem.create(element(p), leaves(p), p.getGameProfile().getName());
    }

    // ---------- Synchronisation vers le client ----------
    public static void sync(ServerPlayer p) {
        boolean has = hasGrimoire(p);
        int sel = selected(p);
        long now = p.level().getGameTime();
        int cd = (int) Math.max(0, cooldownEnd(p, sel) - now);
        ModNetwork.toPlayer(p, new SyncMagicPacket(has, element(p).ordinal(), leaves(p), level(p), xp(p),
                xpForNext(level(p)), mana(p), maxMana(p), sel, has ? unlockedSpells(p) : 0, cd));
    }

    private MagicData() {}
}
