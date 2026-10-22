package com.blackclovermod.ceremony;

import com.blackclovermod.BlackCloverMod;
import com.blackclovermod.entity.FloatingGrimoireEntity;
import com.blackclovermod.item.GrimoireItem;
import com.blackclovermod.magic.MagicData;
import com.blackclovermod.magic.MagicElement;
import com.blackclovermod.magic.Spells;
import com.blackclovermod.registry.ModItems;
import com.blackclovermod.util.Fx;
import com.blackclovermod.util.TaskScheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

/**
 * LA CÉRÉMONIE D'ATTRIBUTION DES GRIMOIRES (comme dans l'animé).
 *
 * Déroulement (en ticks, 20 ticks = 1 seconde) :
 *  -50 → 0   : dans l'Overworld, le joueur lévite dans une spirale de runes, puis éclair blanc
 *    0       : arrivée dans la Tour, sur la plateforme centrale. ~46 grimoires volent d'étagère en étagère
 *  160       : UN grimoire s'arrête... et descend en spirale vers le joueur
 *              (5 feuilles : un grimoire noir et abîmé remonte des profondeurs, les autres fuient contre les murs)
 *  240       : le grimoire s'ouvre devant le joueur : explosion de lumière, titre, trèfle dessiné en particules
 *  300→312   : le grimoire rentre dans le joueur
 *  312       : le joueur reçoit le grimoire
 *  360       : retour dans l'Overworld
 */
public final class CeremonyManager {
    public static final int PRE = -50, T_CHOOSE = 160, T_ARRIVE = 240, T_ABSORB = 300, T_GRANT = 312, T_END = 360;
    private static final int BOOK_COUNT = 46;
    private static final int[] BOOK_COLORS = {0x7B3F2A, 0x3B5BA5, 0x2E6B3A, 0x8E2B2B, 0x5A4632, 0x6A4C93, 0x3A3A3A, 0xB08D3C, 0x4F7C7C, 0x9C6B30};
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.84f, 0.3f), 1.2f);
    private static final DustParticleOptions DEVIL_RED = new DustParticleOptions(new Vector3f(0.62f, 0.08f, 0.08f), 1.3f);

    private static final Map<UUID, Ceremony> ACTIVE = new HashMap<>();

    private static final class Ceremony {
        final UUID playerId;
        int tick = PRE;
        ResourceKey<Level> returnDim;
        Vec3 returnPos;
        float yRot, xRot;
        MagicElement element;
        int leaves;
        final List<FloatingGrimoireEntity> books = new ArrayList<>();
        FloatingGrimoireEntity chosen;
        Vec3 chosenStart;
        double startR, spinDelta;
        Vec3 absorbStart;
        boolean granted;

        Ceremony(UUID id) { this.playerId = id; }

        boolean isAnti() { return element == MagicElement.ANTI; }

        void discardBooks() {
            for (FloatingGrimoireEntity b : books) if (b.isAlive()) b.discard();
            books.clear();
        }
    }

    public static boolean inCeremony(Player p) { return ACTIVE.containsKey(p.getUUID()); }

    // ------------------------------------------------------------------
    // Démarrage
    // ------------------------------------------------------------------
    public static boolean start(ServerPlayer p) {
        if (inCeremony(p)) return false;
        if (!ACTIVE.isEmpty()) {
            p.displayClientMessage(Component.translatable("msg.blackclover.busy").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        if (p.server.getLevel(GrimoireTower.DIMENSION) == null) {
            p.sendSystemMessage(Component.translatable("msg.blackclover.no_dimension").withStyle(ChatFormatting.RED));
            return false;
        }
        Ceremony c = new Ceremony(p.getUUID());
        c.returnDim = p.level().dimension();
        c.returnPos = p.position();
        c.yRot = p.getYRot();
        c.xRot = p.getXRot();
        decideMagic(p, c);
        ACTIVE.put(p.getUUID(), c);

        MagicData.setPendingReturn(p, c.returnDim, c.returnPos);
        p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, -PRE, 0, false, false));
        Fx.sound(p.level(), p.position(), SoundEvents.PORTAL_TRIGGER, 0.5f, 1.4f);
        Fx.title(p, Component.translatable("title.blackclover.summons").withStyle(ChatFormatting.GOLD),
                Component.translatable("title.blackclover.summons_sub").withStyle(ChatFormatting.YELLOW), 10, 40, 10);
        return true;
    }

    /** C'est ici que l'on décide QUEL grimoire va choisir le joueur. */
    private static void decideMagic(ServerPlayer p, Ceremony c) {
        RandomSource r = p.getRandom();
        int fragmentSlot = -1;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(ModItems.DEMON_FRAGMENT.get())) { fragmentSlot = i; break; }
        }
        // Grimoire à 5 feuilles : 1 % de chance, ou garanti si le joueur porte un Fragment de démon
        if (fragmentSlot >= 0 || r.nextFloat() < 0.01f) {
            c.element = MagicElement.ANTI;
            c.leaves = 5;
            if (fragmentSlot >= 0) p.getInventory().getItem(fragmentSlot).shrink(1);
        } else {
            c.element = MagicElement.randomChoosable(r);
            // Trèfle à 4 feuilles : 7 % de chance (+3 % par point de chance, ex. potion de chance)
            float chance4 = 0.07f + Math.max(0f, p.getLuck()) * 0.03f;
            c.leaves = r.nextFloat() < chance4 ? 4 : 3;
        }
    }

    // ------------------------------------------------------------------
    // Boucle principale (appelée chaque tick serveur)
    // ------------------------------------------------------------------
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        Iterator<Map.Entry<UUID, Ceremony>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Ceremony c = it.next().getValue();
            ServerPlayer p = server.getPlayerList().getPlayer(c.playerId);
            if (p == null) {
                c.discardBooks();
                it.remove();
                continue;
            }
            try {
                if (tickCeremony(server, p, c)) it.remove();
            } catch (Exception ex) {
                BlackCloverMod.LOGGER.error("Erreur pendant la ceremonie", ex);
                if (!c.granted && c.tick >= 0) grantSilently(p, c);
                finish(p, c);
                it.remove();
            }
        }
    }

    private static boolean tickCeremony(MinecraftServer server, ServerPlayer p, Ceremony c) {
        int t = c.tick++;
        if (t < 0) {
            preEffects(p, t);
            return false;
        }
        ServerLevel tower = server.getLevel(GrimoireTower.DIMENSION);
        if (tower == null) {
            finish(p, c);
            return true;
        }
        if (t == 0) arrive(p, c, tower);
        lock(p);

        Vec3 ctr = GrimoireTower.platformCenter();
        if (t % 2 == 0) {
            ParticleOptions circle = t > T_CHOOSE ? (c.isAnti() ? DEVIL_RED : c.element.dust(1.2f)) : GOLD;
            Fx.magicCircle(tower, circle, ctr.add(0, 0.05, 0), 3.4, t);
        }
        if (t % 7 == 0 && !c.books.isEmpty()) {
            FloatingGrimoireEntity b = c.books.get(tower.getRandom().nextInt(c.books.size()));
            tower.playSound(null, b.getX(), b.getY(), b.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.AMBIENT, 2f, 0.8f + tower.getRandom().nextFloat() * 0.4f);
        }
        if (t == 25) Fx.title(p, Component.translatable("title.blackclover.tower").withStyle(ChatFormatting.GOLD),
                Component.translatable("title.blackclover.tower_sub").withStyle(ChatFormatting.GRAY), 20, 70, 20);
        if (t == 110) p.displayClientMessage(Component.translatable("msg.blackclover.searching").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);

        if (t == T_CHOOSE) choose(p, c, tower);
        if (t > T_CHOOSE && t <= T_ARRIVE) approach(p, c, tower, t);
        if (t == T_ARRIVE) reveal(p, c, tower);
        if (t > T_ARRIVE && t < T_ABSORB) showcase(p, c, tower, t);
        if (t >= T_ABSORB && t < T_GRANT) absorb(p, c, tower, t);
        if (t == T_GRANT) grant(p, c, tower);
        if (t > T_GRANT && t < T_END) despawnSome(c, tower);
        if (t >= T_END) {
            finish(p, c);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Les étapes
    // ------------------------------------------------------------------
    private static void preEffects(ServerPlayer p, int t) {
        ServerLevel l = p.serverLevel();
        Vec3 pos = p.position();
        int k = t - PRE;
        for (int i = 0; i < 3; i++) {
            double a = k * 0.35 + i * 2.094, r = 1.5 - k * 0.02;
            l.sendParticles(ParticleTypes.ENCHANT, pos.x + Math.cos(a) * r, pos.y + k * 0.05, pos.z + Math.sin(a) * r, 2, 0, 0, 0, 0);
            l.sendParticles(GOLD, pos.x + Math.cos(a + Math.PI) * r, pos.y + 0.2 + k * 0.04, pos.z + Math.sin(a + Math.PI) * r, 1, 0, 0, 0, 0);
        }
        if (k % 10 == 0) Fx.sound(l, pos, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.6f + k * 0.02f);
        if (t == -1) {
            Fx.burst(l, ParticleTypes.FLASH, pos.add(0, 1, 0), 1, 0, 0);
            Fx.burst(l, ParticleTypes.END_ROD, pos.add(0, 1, 0), 60, 0.3, 0.3);
            Fx.sound(l, pos, SoundEvents.ENDERMAN_TELEPORT, 1f, 0.5f);
        }
    }

    private static void arrive(ServerPlayer p, Ceremony c, ServerLevel tower) {
        GrimoireTower.ensureBuilt(tower);
        p.removeEffect(MobEffects.LEVITATION);
        Vec3 ctr = GrimoireTower.platformCenter();
        p.teleportTo(tower, ctr.x, ctr.y, ctr.z, 0f, -10f);
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, T_END + 40, 0, false, false));

        RandomSource r = tower.getRandom();
        for (int i = 0; i < BOOK_COUNT; i++) {
            FloatingGrimoireEntity b = new FloatingGrimoireEntity(tower, BOOK_COLORS[r.nextInt(BOOK_COLORS.length)]);
            b.placeRandom();
            tower.addFreshEntity(b);
            c.books.add(b);
        }
        Fx.sound(tower, ctr, SoundEvents.AMETHYST_BLOCK_CHIME, 2f, 0.5f);
    }

    private static void choose(ServerPlayer p, Ceremony c, ServerLevel tower) {
        Vec3 ctr = GrimoireTower.platformCenter();
        FloatingGrimoireEntity b;
        if (c.isAnti()) {
            // Un vieux grimoire noir remonte des profondeurs de la tour...
            b = new FloatingGrimoireEntity(tower, 0x141414);
            b.setPos(ctr.x + 6, ctr.y - 24, ctr.z);
            tower.addFreshEntity(b);
            c.books.add(b);
            for (FloatingGrimoireEntity o : c.books) if (o != b) o.flee();
            Fx.sound(tower, ctr, SoundEvents.WITHER_AMBIENT, 1.5f, 0.4f);
            Fx.sound(tower, ctr, SoundEvents.WARDEN_HEARTBEAT, 2f, 0.7f);
            p.displayClientMessage(Component.translatable("msg.blackclover.anti_coming").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
        } else {
            // Un des grimoires s'arrête net... et se met à briller
            b = c.books.get(tower.getRandom().nextInt(c.books.size()));
            b.setColor(GrimoireItem.coverColor(c.element, c.leaves));
            Fx.burst(tower, ParticleTypes.ENCHANT, b.position(), 120, 0.5, 1);
            Fx.burst(tower, c.element.dust(1.5f), b.position(), 40, 0.4, 0);
            Fx.sound(tower, b.position(), SoundEvents.BEACON_POWER_SELECT, 2f, 1.2f);
            p.displayClientMessage(Component.translatable("msg.blackclover.chosen_coming").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
        }
        b.setMode(FloatingGrimoireEntity.SCRIPTED);
        b.setGlowingBook(true);
        c.chosen = b;
        c.chosenStart = b.position();
        c.startR = Math.sqrt(Mth.square(b.getX() - ctr.x) + Mth.square(b.getZ() - ctr.z));
        double startAng = Math.atan2(b.getZ() - ctr.z, b.getX() - ctr.x);
        double finalAng = frontAngle(p);
        double wrap = ((startAng - finalAng) % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
        c.spinDelta = (c.isAnti() ? Math.PI * 2 : Math.PI * 4) + wrap;
    }

    /** Le grimoire choisi descend en spirale vers le joueur. */
    private static void approach(ServerPlayer p, Ceremony c, ServerLevel tower, int t) {
        if (c.chosen == null) return;
        Vec3 ctr = GrimoireTower.platformCenter();
        double prog = (t - T_CHOOSE) / (double) (T_ARRIVE - T_CHOOSE);
        double e = prog < 0.5 ? 4 * prog * prog * prog : 1 - Math.pow(-2 * prog + 2, 3) / 2;
        double ang = frontAngle(p) + (1 - e) * c.spinDelta;
        double r = Mth.lerp(e, c.startR, 1.5);
        double finalY = p.getEyeY() - 0.35;
        double y = Mth.lerp(e, c.chosenStart.y, finalY) + Math.sin(e * Math.PI) * (c.isAnti() ? 1.0 : 2.5);
        Vec3 pos = new Vec3(ctr.x + Math.cos(ang) * r, y, ctr.z + Math.sin(ang) * r);
        c.chosen.moveFacing(pos);

        tower.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y + 0.3, pos.z, 6, 0.2, 0.2, 0.2, 0.5);
        if (c.isAnti()) {
            tower.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 0.3, pos.z, 3, 0.15, 0.15, 0.15, 0.01);
            tower.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 0.3, pos.z, 2, 0.2, 0.2, 0.2, 0.01);
            tower.sendParticles(DEVIL_RED, pos.x, pos.y + 0.3, pos.z, 3, 0.2, 0.2, 0.2, 0);
            if (t % 20 == 0) Fx.sound(tower, pos, SoundEvents.WARDEN_HEARTBEAT, 2f, 0.6f);
        } else {
            tower.sendParticles(c.element.dust(1.4f), pos.x, pos.y + 0.3, pos.z, 4, 0.15, 0.15, 0.15, 0);
            if (t % 3 == 0) tower.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.3, pos.z, 1, 0.1, 0.1, 0.1, 0.01);
        }
        if (t % 10 == 0) Fx.sound(tower, pos, SoundEvents.BOOK_PAGE_TURN, 1.5f, 1.5f);
    }

    /** Le grimoire s'ouvre : explosion de lumière + titre. */
    private static void reveal(ServerPlayer p, Ceremony c, ServerLevel tower) {
        if (c.chosen == null) return;
        FloatingGrimoireEntity b = c.chosen;
        b.setMode(FloatingGrimoireEntity.OPEN);
        b.faceToward(p.getEyePosition());
        Vec3 bp = b.position().add(0, 0.3, 0);
        Vec3 ctr = GrimoireTower.platformCenter();

        Fx.burst(tower, ParticleTypes.ENCHANT, bp, 400, 1.2, 1.0);
        Fx.burst(tower, ParticleTypes.END_ROD, bp, 80, 0.3, 0.25);
        Fx.burst(tower, c.element.dust(2f), bp, 100, 1.5, 0);
        if (c.isAnti()) {
            Fx.burst(tower, ParticleTypes.LARGE_SMOKE, bp, 150, 0.8, 0.15);
            Fx.burst(tower, ParticleTypes.SQUID_INK, bp, 100, 0.8, 0.1);
            Fx.burst(tower, ParticleTypes.SOUL, bp, 60, 1, 0.05);
            Fx.sound(tower, bp, SoundEvents.WITHER_SPAWN, 0.6f, 1.2f);
            Fx.sound(tower, bp, SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 0.6f);
        } else {
            Fx.burst(tower, ParticleTypes.TOTEM_OF_UNDYING, bp, 120, 0.6, 0.5);
            Fx.burst(tower, c.element.particle(), bp, 120, 0.6, 0.2);
            Fx.sound(tower, bp, SoundEvents.ENCHANTMENT_TABLE_USE, 2f, 1f);
            Fx.sound(tower, bp, SoundEvents.BEACON_ACTIVATE, 2f, 1.2f);
            if (c.leaves == 4) Fx.sound(tower, bp, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        ParticleOptions waveDust = c.isAnti() ? DEVIL_RED : c.element.dust(1.5f);
        TaskScheduler.repeat(14, k -> Fx.ring(tower, waveDust, ctr.add(0, 0.15, 0), 1 + k * 1.2, 30 + k * 8));

        Component title = Component.translatable(c.isAnti() ? "title.blackclover.chosen_anti" : "title.blackclover.chosen")
                .withStyle(c.isAnti() ? ChatFormatting.DARK_RED : ChatFormatting.GOLD);
        Component sub = Component.translatable("title.blackclover.chosen_sub", c.element.displayName(),
                Component.translatable("leaves.blackclover." + c.leaves));
        Fx.title(p, title, sub, 5, 70, 20);
    }

    /** Le trèfle du grimoire se dessine au-dessus du livre ouvert. */
    private static void showcase(ServerPlayer p, Ceremony c, ServerLevel tower, int t) {
        if (c.chosen == null || t % 2 != 0) return;
        Vec3 toPlayer = p.getEyePosition().subtract(c.chosen.position());
        Vec3 h = new Vec3(toPlayer.x, 0, toPlayer.z);
        h = h.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : h.normalize();
        Vec3 right = h.cross(Fx.UP).normalize();
        Vec3 cc = c.chosen.position().add(0, 1.25, 0);
        ParticleOptions dust = c.isAnti() ? DEVIL_RED : GOLD;
        Fx.clover(tower, cc, right, Fx.UP, c.leaves, dust, 0.9);
        tower.sendParticles(ParticleTypes.ENCHANT, cc.x, cc.y, cc.z, 4, 0.3, 0.3, 0.3, 0.6);
        if (t % 10 == 0) Fx.sound(tower, c.chosen.position(), SoundEvents.BOOK_PAGE_TURN, 1.5f, 1.8f);
    }

    /** Le grimoire vient se loger dans les mains du joueur. */
    private static void absorb(ServerPlayer p, Ceremony c, ServerLevel tower, int t) {
        if (c.chosen == null) return;
        if (t == T_ABSORB) c.absorbStart = c.chosen.position();
        double k = (t - T_ABSORB + 1) / (double) (T_GRANT - T_ABSORB);
        Vec3 target = p.position().add(0, 1.1, 0);
        Vec3 pos = c.absorbStart.add(target.subtract(c.absorbStart).scale(k));
        c.chosen.setPos(pos);
        c.chosen.setVisualScale((float) (1 - k * 0.8));
        Fx.converge(tower, ParticleTypes.ENCHANT, target, 20, 1.6);
    }

    private static void grant(ServerPlayer p, Ceremony c, ServerLevel tower) {
        grantSilently(p, c);
        if (c.chosen != null) c.chosen.discard();
        Vec3 chest = p.position().add(0, 1, 0);
        Fx.burst(tower, c.isAnti() ? ParticleTypes.LARGE_SMOKE : ParticleTypes.TOTEM_OF_UNDYING, chest, 80, 0.5, 0.5);
        Fx.sound(tower, chest, SoundEvents.PLAYER_LEVELUP, 1f, 1f);
        Fx.sound(tower, chest, SoundEvents.ITEM_PICKUP, 1f, 0.8f);

        p.sendSystemMessage(Component.translatable("msg.blackclover.received", c.element.displayName(),
                Component.translatable("leaves.blackclover." + c.leaves)).withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.translatable("msg.blackclover.first_spell", Spells.of(c.element).get(0).name()).withStyle(ChatFormatting.LIGHT_PURPLE));
        p.sendSystemMessage(Component.translatable("msg.blackclover.controls").withStyle(ChatFormatting.GRAY));
    }

    private static void grantSilently(ServerPlayer p, Ceremony c) {
        if (c.granted) return;
        c.granted = true;
        MagicData.grant(p, c.element, c.leaves);
        ItemStack g = GrimoireItem.create(c.element, c.leaves, p.getGameProfile().getName());
        if (!p.getInventory().add(g)) p.drop(g, false);
        MagicData.sync(p);
    }

    private static void despawnSome(Ceremony c, ServerLevel tower) {
        for (int i = 0; i < 2 && !c.books.isEmpty(); i++) {
            FloatingGrimoireEntity b = c.books.remove(c.books.size() - 1);
            if (b.isAlive()) {
                tower.sendParticles(ParticleTypes.POOF, b.getX(), b.getY() + 0.3, b.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                b.discard();
            }
        }
    }

    private static void finish(ServerPlayer p, Ceremony c) {
        c.discardBooks();
        p.removeEffect(MobEffects.NIGHT_VISION);
        p.removeEffect(MobEffects.LEVITATION);
        sendBack(p, c.returnDim, c.returnPos, c.yRot, c.xRot);
        MagicData.clearPendingReturn(p);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
        Fx.burst(p.serverLevel(), ParticleTypes.END_ROD, p.position().add(0, 1, 0), 50, 0.4, 0.15);
        Fx.sound(p.level(), p.position(), SoundEvents.ENDERMAN_TELEPORT, 1f, 1.2f);
    }

    // ------------------------------------------------------------------
    // Sécurités : déconnexion, crash...
    // ------------------------------------------------------------------
    public static void abort(ServerPlayer p) {
        Ceremony c = ACTIVE.remove(p.getUUID());
        if (c == null) return;
        c.discardBooks();
        if (c.tick < 0) {
            // pas encore parti : on rend la convocation
            p.removeEffect(MobEffects.LEVITATION);
            MagicData.clearPendingReturn(p);
            p.getInventory().add(new ItemStack(ModItems.TOWER_SUMMONS.get()));
        } else if (!c.granted) {
            grantSilently(p, c); // le retour sera fait à la reconnexion
        }
    }

    /** Appelé si un joueur se retrouve dans la Tour sans cérémonie en cours. */
    public static void returnFromTower(ServerPlayer p) {
        CompoundTag pending = MagicData.pendingReturn(p);
        if (pending != null) {
            if (!MagicData.hasGrimoire(p)) p.getInventory().add(new ItemStack(ModItems.TOWER_SUMMONS.get()));
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, MagicData.pendingDim(pending));
            sendBack(p, dim, new Vec3(pending.getDouble("X"), pending.getDouble("Y"), pending.getDouble("Z")), p.getYRot(), p.getXRot());
            MagicData.clearPendingReturn(p);
        } else {
            ServerLevel ow = p.server.overworld();
            var spawn = ow.getSharedSpawnPos();
            p.teleportTo(ow, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, p.getYRot(), p.getXRot());
        }
        p.removeEffect(MobEffects.NIGHT_VISION);
    }

    private static void sendBack(ServerPlayer p, ResourceKey<Level> dim, Vec3 pos, float yRot, float xRot) {
        ServerLevel dest = p.server.getLevel(dim);
        if (dest == null || dim == GrimoireTower.DIMENSION) dest = p.server.overworld();
        p.teleportTo(dest, pos.x, pos.y, pos.z, yRot, xRot);
    }

    public static void clear() {
        for (Ceremony c : ACTIVE.values()) c.discardBooks();
        ACTIVE.clear();
    }

    // ------------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------------
    /** Empêche le joueur de bouger (il peut toujours tourner la tête). */
    private static void lock(ServerPlayer p) {
        Vec3 c = GrimoireTower.platformCenter();
        if (p.level().dimension() == GrimoireTower.DIMENSION && p.position().distanceToSqr(c) > 0.04) {
            p.connection.teleport(c.x, c.y, c.z, 0f, 0f, EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT));
        }
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
    }

    /** Angle (en radians, autour du centre) de la direction où regarde le joueur. */
    private static double frontAngle(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 h = new Vec3(look.x, 0, look.z);
        h = h.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : h.normalize();
        return Math.atan2(h.z, h.x);
    }

    private CeremonyManager() {}
}
