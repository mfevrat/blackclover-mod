package com.blackclovermod.event;

import com.blackclovermod.BlackCloverMod;
import com.blackclovermod.ceremony.CeremonyManager;
import com.blackclovermod.ceremony.GrimoireTower;
import com.blackclovermod.entity.MagicProjectileEntity;
import com.blackclovermod.entity.boss.MagicBossEntity;
import com.blackclovermod.item.GrimoireItem;
import com.blackclovermod.magic.MagicData;
import com.blackclovermod.magic.MagicElement;
import com.blackclovermod.util.TaskScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = BlackCloverMod.MODID)
public final class CommonEvents {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        TaskScheduler.tick();
        CeremonyManager.tick(server);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp)) return;

        // Un joueur ne doit jamais rester bloqué dans la Tour
        if (sp.level().dimension() == GrimoireTower.DIMENSION && !CeremonyManager.inCeremony(sp)) {
            CeremonyManager.returnFromTower(sp);
            return;
        }

        if (!MagicData.hasGrimoire(sp)) return;
        float max = MagicData.maxMana(sp);
        float m = MagicData.mana(sp);
        if (m < max) {
            // l'anti-magie n'a pas de mana mais de "l'endurance", qui revient plus vite
            float base = MagicData.element(sp) == MagicElement.ANTI ? 0.45f : 0.25f;
            MagicData.setMana(sp, m + base * (1f + 0.05f * MagicData.level(sp)));
        }
        if (sp.tickCount % 10 == 0) MagicData.sync(sp);
    }

    /** XP de grimoire quand on tue un monstre. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        Entity killer = e.getSource().getEntity();
        if (killer instanceof ServerPlayer sp && MagicData.hasGrimoire(sp)) {
            LivingEntity dead = e.getEntity();
            int xp = (int) Math.max(3, dead.getMaxHealth() / 2);
            if (dead instanceof MagicBossEntity) xp *= 2;
            MagicData.addXp(sp, xp);
            MagicData.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent e) {
        // Invincible pendant la cérémonie
        if (e.getEntity() instanceof ServerPlayer sp && CeremonyManager.inCeremony(sp)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        // Passif de l'anti-magie : -40 % de dégâts des projectiles magiques
        if (e.getEntity() instanceof ServerPlayer sp && MagicData.hasGrimoire(sp)
                && MagicData.element(sp) == MagicElement.ANTI
                && e.getSource().getDirectEntity() instanceof MagicProjectileEntity) {
            e.setAmount(e.getAmount() * 0.6f);
        }
    }

    /** Le grimoire ne tombe pas à la mort : il revient au joueur quand il réapparaît. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            boolean removed = e.getDrops().removeIf(ie -> ie.getItem().getItem() instanceof GrimoireItem);
            if (removed) MagicData.setRestoreGrimoire(sp, true);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        MagicData.copy(e.getOriginal(), e.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            if (MagicData.restoreGrimoire(sp)) {
                MagicData.setRestoreGrimoire(sp, false);
                if (MagicData.hasGrimoire(sp)) sp.getInventory().add(MagicData.createGrimoire(sp));
            }
            MagicData.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) MagicData.sync(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) CeremonyManager.abort(sp);
    }

    @SubscribeEvent
    public static void onChangeDim(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) MagicData.sync(sp);
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        ModCommands.register(e.getDispatcher());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        CeremonyManager.clear();
        TaskScheduler.clear();
    }

    private CommonEvents() {}
}
