package com.blackclovermod.item;

import com.blackclovermod.entity.boss.MagicBossEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/** Objet d'invocation de boss : clic droit sur un bloc. */
public class BossSummonItem extends LoreItem {
    private final Supplier<? extends EntityType<? extends MagicBossEntity>> type;

    public BossSummonItem(Properties props, Supplier<? extends EntityType<? extends MagicBossEntity>> type) {
        super(props);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        Player player = ctx.getPlayer();
        if (sl.getDifficulty() == Difficulty.PEACEFUL) {
            if (player != null) player.displayClientMessage(Component.translatable("msg.blackclover.peaceful").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        MagicBossEntity boss = type.get().create(sl);
        if (boss == null) return InteractionResult.FAIL;
        float yaw = player != null ? player.getYRot() + 180f : 0f;
        boss.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0f);
        boss.setYHeadRot(yaw);
        boss.yBodyRot = yaw;
        boss.startSpawnAnimation();
        sl.addFreshEntity(boss);
        if (player != null && !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
