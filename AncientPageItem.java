package com.blackclovermod.item;

import com.blackclovermod.magic.MagicData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Page ancienne : +250 XP de grimoire. */
public class AncientPageItem extends LoreItem {
    public AncientPageItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (!MagicData.hasGrimoire(sp)) {
            sp.displayClientMessage(Component.translatable("msg.blackclover.need_grimoire_first").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        MagicData.addXp(sp, 250);
        MagicData.sync(sp);
        sp.serverLevel().sendParticles(ParticleTypes.ENCHANT, sp.getX(), sp.getY() + 1.2, sp.getZ(), 80, 0.6, 0.8, 0.6, 1);
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BOOK_PAGE_TURN, sp.getSoundSource(), 1.5f, 0.7f);
        sp.displayClientMessage(Component.translatable("msg.blackclover.page_used").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }
}
