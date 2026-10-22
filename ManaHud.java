package com.blackclovermod.client;

import com.blackclovermod.magic.ClientMagicData;
import com.blackclovermod.magic.MagicElement;
import com.blackclovermod.magic.Spell;
import com.blackclovermod.magic.Spells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/** Le petit panneau en bas à gauche : attribut, barre de mana, sort sélectionné. */
public final class ManaHud {
    private static final int W = 120;

    public static final IGuiOverlay HUD = (gui, g, partialTick, screenW, screenH) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || !ClientMagicData.has) return;
        Font font = mc.font;
        MagicElement el = MagicElement.byIndex(ClientMagicData.element);
        int x = 6, y = screenH - 52;

        g.fill(x - 3, y - 3, x + W + 3, y + 40, 0x90000000);
        g.drawString(font, el.displayName().copy().append(Component.literal(" \u2663" + ClientMagicData.leaves).withStyle(s -> s.withColor(0xFFD54A))), x, y, 0xFFFFFF, true);
        g.drawString(font, Component.translatable("hud.blackclover.level", ClientMagicData.level), x + W - 34, y, 0xE0C070, true);

        // Barre de mana
        int barY = y + 12;
        float ratio = ClientMagicData.maxMana <= 0 ? 0 : ClientMagicData.mana / ClientMagicData.maxMana;
        int barColor = el == MagicElement.ANTI ? 0xFF8E44AD : (0xFF000000 | el.color);
        g.fill(x, barY, x + W, barY + 6, 0xFF202020);
        g.fill(x, barY, x + (int) (W * Math.min(1f, ratio)), barY + 6, barColor);
        Component label = Component.translatable(el == MagicElement.ANTI ? "hud.blackclover.endurance" : "hud.blackclover.mana",
                (int) ClientMagicData.mana, (int) ClientMagicData.maxMana);
        g.drawString(font, label, x, barY + 8, 0xBBBBBB, false);

        // Barre d'XP du grimoire
        float xpRatio = ClientMagicData.xpNext <= 0 ? 0 : ClientMagicData.xp / (float) ClientMagicData.xpNext;
        g.fill(x, barY + 18, x + W, barY + 19, 0xFF303030);
        g.fill(x, barY + 18, x + (int) (W * Math.min(1f, xpRatio)), barY + 19, 0xFFE0C070);

        // Sort sélectionné
        List<Spell> spells = Spells.of(el);
        if (!spells.isEmpty()) {
            int sel = Math.max(0, Math.min(spells.size() - 1, ClientMagicData.selected));
            Component name = Component.literal("[" + (sel + 1) + "/" + ClientMagicData.unlocked + "] ").append(spells.get(sel).name());
            int color = ClientMagicData.cooldown > 0 ? 0x888888 : 0xFFFFFF;
            g.drawString(font, name, x, barY + 22, color, true);
            if (ClientMagicData.cooldown > 0) {
                String cd = String.format("%.1fs", ClientMagicData.cooldown / 20f);
                g.drawString(font, cd, x + W - font.width(cd), barY + 22, 0xFF7070, true);
            }
        }
    };

    private ManaHud() {}
}
