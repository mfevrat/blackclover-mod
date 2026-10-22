package com.blackclovermod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Boîte à outils d'effets visuels (formes de particules, sons, titres). */
public final class Fx {
    public static final Vec3 UP = new Vec3(0, 1, 0);

    public static void ring(ServerLevel l, ParticleOptions p, Vec3 c, double r, int points) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            l.sendParticles(p, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    /** Anneau dont les particules partent vers l'extérieur. */
    public static void ringOut(ServerLevel l, ParticleOptions p, Vec3 c, double r, int points, double speed) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            double cx = Math.cos(a), cz = Math.sin(a);
            l.sendParticles(p, c.x + cx * r, c.y, c.z + cz * r, 0, cx, 0.05, cz, speed);
        }
    }

    public static void sphere(ServerLevel l, ParticleOptions p, Vec3 c, double r, int count) {
        RandomSource rnd = l.getRandom();
        for (int i = 0; i < count; i++) {
            double u = rnd.nextDouble() * 2 - 1;
            double a = rnd.nextDouble() * Math.PI * 2;
            double s = Math.sqrt(1 - u * u);
            l.sendParticles(p, c.x + s * Math.cos(a) * r, c.y + u * r, c.z + s * Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    public static void line(ServerLevel l, ParticleOptions p, Vec3 a, Vec3 b, double step) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1e-3) return;
        Vec3 dir = d.scale(1 / len);
        for (double t = 0; t <= len; t += step) {
            Vec3 q = a.add(dir.scale(t));
            l.sendParticles(p, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }

    public static void burst(ServerLevel l, ParticleOptions p, Vec3 c, int count, double spread, double speed) {
        l.sendParticles(p, c.x, c.y, c.z, count, spread, spread, spread, speed);
    }

    public static void pillar(ServerLevel l, ParticleOptions p, Vec3 base, double height, double radius, int perBlock) {
        for (double y = 0; y < height; y += 0.5) {
            l.sendParticles(p, base.x, base.y + y, base.z, perBlock, radius, 0.2, radius, 0.01);
        }
    }

    /** Particules qui "aspirent" vers un point (les particules ENCHANT/PORTAL volent vers leur origine). */
    public static void converge(ServerLevel l, ParticleOptions p, Vec3 c, int count, double dist) {
        RandomSource rnd = l.getRandom();
        for (int i = 0; i < count; i++) {
            Vec3 d = new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).normalize().scale(dist);
            l.sendParticles(p, c.x, c.y, c.z, 0, d.x, d.y, d.z, 1.0);
        }
    }

    /** Cercle magique rotatif au sol : deux anneaux + une étoile à 5 branches. */
    public static void magicCircle(ServerLevel l, ParticleOptions dust, Vec3 c, double r, int tick) {
        double rot = tick * 0.08;
        ring(l, dust, c, r, (int) (r * 14));
        ring(l, dust, c, r * 0.72, (int) (r * 10));
        Vec3[] pts = new Vec3[5];
        for (int i = 0; i < 5; i++) {
            double a = rot + i * Math.PI * 2 / 5;
            pts[i] = c.add(Math.cos(a) * r * 0.72, 0, Math.sin(a) * r * 0.72);
        }
        for (int i = 0; i < 5; i++) line(l, dust, pts[i], pts[(i + 2) % 5], 0.25);
    }

    /** Dessine un trèfle (3, 4 ou 5 feuilles) dans le plan (right, up). */
    public static void clover(ServerLevel l, Vec3 c, Vec3 right, Vec3 up, int leaves, ParticleOptions dust, double size) {
        for (int i = 0; i < leaves; i++) {
            double a = Math.PI / 2 + i * Math.PI * 2 / leaves;
            Vec3 lc = c.add(right.scale(Math.cos(a) * size * 0.5)).add(up.scale(Math.sin(a) * size * 0.5));
            for (int k = 0; k < 14; k++) {
                double b = k * Math.PI * 2 / 14;
                Vec3 q = lc.add(right.scale(Math.cos(b) * size * 0.32)).add(up.scale(Math.sin(b) * size * 0.32));
                l.sendParticles(dust, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            }
        }
        line(l, dust, c, c.subtract(up.scale(size * 1.1)).add(right.scale(size * 0.15)), 0.08);
    }

    public static Vec3 groundAt(Level level, Vec3 pos) {
        BlockPos.MutableBlockPos m = BlockPos.containing(pos).mutable();
        for (int i = 0; i < 30 && m.getY() > level.getMinBuildHeight(); i++) {
            if (!level.getBlockState(m).isAir()) return new Vec3(pos.x, m.getY() + 1.0, pos.z);
            m.move(0, -1, 0);
        }
        return pos;
    }

    public static void sound(Level l, Vec3 p, SoundEvent s, float volume, float pitch) {
        l.playSound(null, p.x, p.y, p.z, s, SoundSource.PLAYERS, volume, pitch);
    }

    public static void title(ServerPlayer p, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        if (subtitle != null) p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private Fx() {}
}
