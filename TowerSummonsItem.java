package com.blackclovermod.item;

import com.blackclovermod.ceremony.CeremonyManager;
import com.blackclovermod.magic.MagicData;
import com.blackclovermod.magic.SpellCaster;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * La Convocation de la Tour des Grimoires.
 * CONDITIONS POUR OBTENIR UN GRIMOIRE :
 *   1. Ne pas déjà avoir de grimoire
 *   2. Avoir au moins 15 niveaux d'expérience ("avoir 15 ans", comme dans l'animé)
 *   3. Être dans l'Overworld
 *   4. Qu'il fasse nuit
 */
public class TowerSummonsItem extends Item {
    public static final int REQUIRED_LEVEL = 15;

    public TowerSummonsItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);

        // Déjà un grimoire ? Si on l'a perdu, la Tour en renvoie un nouveau (sans cérémonie).
        if (MagicData.hasGrimoire(sp)) {
            if (!SpellCaster.hasGrimoireItem(sp)) {
                sp.getInventory().add(MagicData.createGrimoire(sp));
                sp.sendSystemMessage(Component.translatable("msg.blackclover.replaced").withStyle(ChatFormatting.GOLD));
                if (!sp.getAbilities().instabuild) stack.shrink(1);
                return InteractionResultHolder.consume(stack);
            }
            sp.displayClientMessage(Component.translatable("msg.blackclover.already_has").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }
        if (sp.experienceLevel < REQUIRED_LEVEL && !sp.getAbilities().instabuild) {
            sp.displayClientMessage(Component.translatable("msg.blackclover.need_level", REQUIRED_LEVEL).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (level.dimension() != Level.OVERWORLD) {
            sp.displayClientMessage(Component.translatable("msg.blackclover.need_overworld").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        long time = level.getDayTime() % 24000L;
        if ((time < 13000L || time > 23000L) && !sp.getAbilities().instabuild) {
            sp.displayClientMessage(Component.translatable("msg.blackclover.need_night").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (CeremonyManager.start(sp)) {
            if (!sp.getAbilities().instabuild) stack.shrink(1);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.blackclover.tower_summons.desc1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.blackclover.tower_summons.desc2", REQUIRED_LEVEL).withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.translatable("item.blackclover.tower_summons.desc3").withStyle(ChatFormatting.DARK_AQUA));
    }
}
