package com.blackclovermod.item;

import com.blackclovermod.magic.MagicData;
import com.blackclovermod.magic.MagicElement;
import com.blackclovermod.magic.Spell;
import com.blackclovermod.magic.Spells;
import com.blackclovermod.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Le grimoire du joueur. Il doit être dans l'inventaire pour lancer des sorts. */
public class GrimoireItem extends Item {

    public GrimoireItem(Properties props) {
        super(props);
    }

    public static ItemStack create(MagicElement element, int leaves, String owner) {
        ItemStack s = new ItemStack(ModItems.GRIMOIRE.get());
        CompoundTag t = s.getOrCreateTag();
        t.putString("Element", element.id);
        t.putInt("Leaves", leaves);
        t.putString("Owner", owner);
        return s;
    }

    public static MagicElement element(ItemStack s) {
        return s.hasTag() ? MagicElement.byId(s.getTag().getString("Element")) : MagicElement.FIRE;
    }

    public static int leaves(ItemStack s) {
        int l = s.hasTag() ? s.getTag().getInt("Leaves") : 3;
        return l == 0 ? 3 : l;
    }

    public static String owner(ItemStack s) {
        return s.hasTag() ? s.getTag().getString("Owner") : "";
    }

    /** Couleur de la couverture selon l'attribut. */
    public static int coverColor(MagicElement e, int leaves) {
        if (leaves >= 5) return 0x161616;
        return switch (e) {
            case FIRE -> 0x9E2A1E;
            case WATER -> 0x1F4E8C;
            case WIND -> 0x2F8F5B;
            case LIGHTNING -> 0xC9A227;
            case DARKNESS -> 0x3A1F5C;
            case LIGHT -> 0xE8E0C0;
            case THORN -> 0x2E5E2A;
            case ANTI -> 0x161616;
            case EARTH -> 0x6B5537;
        };
    }

    public static int color(ItemStack s) {
        return coverColor(element(s), leaves(s));
    }

    @Override
    public Component getName(ItemStack stack) {
        String owner = owner(stack);
        return owner.isEmpty() ? super.getName(stack) : Component.translatable("item.blackclover.grimoire.named", owner);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return leaves(stack) >= 4;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.blackclover.grimoire.element", element(stack).displayName()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("leaves.blackclover." + leaves(stack)).withStyle(leaves(stack) >= 5 ? ChatFormatting.DARK_RED : leaves(stack) == 4 ? ChatFormatting.GREEN : ChatFormatting.DARK_GREEN));
        tip.add(Component.translatable("tooltip.blackclover.grimoire.hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    /** Clic droit : affiche les pages du grimoire dans le chat. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (!MagicData.hasGrimoire(sp)) {
                sp.sendSystemMessage(Component.translatable("msg.blackclover.not_yours").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            MagicElement el = MagicData.element(sp);
            int leaves = MagicData.leaves(sp);
            int unlocked = MagicData.unlockedSpells(sp);
            sp.sendSystemMessage(Component.translatable("msg.blackclover.pages_header", el.displayName(), MagicData.level(sp),
                    MagicData.xp(sp), MagicData.xpForNext(MagicData.level(sp))).withStyle(ChatFormatting.GOLD));
            List<Spell> spells = Spells.of(el);
            for (int i = 0; i < spells.size(); i++) {
                Spell s = spells.get(i);
                if (i < unlocked) {
                    sp.sendSystemMessage(Component.translatable("msg.blackclover.page_unlocked", s.name(), s.manaCost(), s.cooldown() / 20f).withStyle(ChatFormatting.WHITE));
                } else {
                    sp.sendSystemMessage(Component.translatable("msg.blackclover.page_locked", MagicData.requiredLevel(i, leaves)).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, player.getSoundSource(), 1f, 1f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Un grimoire posé au sol ne disparaît jamais. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        entity.setUnlimitedLifetime();
        return false;
    }
}
