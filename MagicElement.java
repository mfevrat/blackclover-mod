package com.blackclovermod.magic;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/**
 * Les attributs magiques. "choosable" = peut être donné par un grimoire normal.
 * ANTI (anti-magie) n'est donné que par un grimoire à 5 feuilles.
 * EARTH n'est utilisé que par le boss Gardien du Donjon.
 */
public enum MagicElement {
    FIRE("fire", 0xFF5A1F, true),
    WATER("water", 0x2F8BFF, true),
    WIND("wind", 0x8CFFC1, true),
    LIGHTNING("lightning", 0xFFE93B, true),
    DARKNESS("darkness", 0x6A2FA0, true),
    LIGHT("light", 0xFFF6C8, true),
    THORN("thorn", 0x3FA34D, true),
    ANTI("anti", 0x1C1C1C, false),
    EARTH("earth", 0x8B7355, false);

    public final String id;
    public final int color;
    public final boolean choosable;

    MagicElement(String id, int color, boolean choosable) {
        this.id = id;
        this.color = color;
        this.choosable = choosable;
    }

    public float red() { return ((color >> 16) & 255) / 255f; }
    public float green() { return ((color >> 8) & 255) / 255f; }
    public float blue() { return (color & 255) / 255f; }

    /** Couleur lisible pour du texte (l'anti-magie est trop sombre sinon). */
    public int textColor() { return this == ANTI ? 0x9A9A9A : color; }

    public MutableComponent displayName() {
        return Component.translatable("element.blackclover." + id).withStyle(s -> s.withColor(textColor()));
    }

    public DustParticleOptions dust(float scale) {
        return new DustParticleOptions(new Vector3f(red(), green(), blue()), Math.min(4f, scale));
    }

    public ParticleOptions particle() {
        return switch (this) {
            case FIRE -> ParticleTypes.FLAME;
            case WATER -> ParticleTypes.SPLASH;
            case WIND -> ParticleTypes.CLOUD;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case DARKNESS -> ParticleTypes.SQUID_INK;
            case LIGHT -> ParticleTypes.END_ROD;
            case THORN -> ParticleTypes.COMPOSTER;
            case ANTI -> ParticleTypes.LARGE_SMOKE;
            case EARTH -> ParticleTypes.ASH;
        };
    }

    public static MagicElement byId(String id) {
        for (MagicElement e : values()) if (e.id.equals(id)) return e;
        return FIRE;
    }

    public static MagicElement byIndex(int i) {
        MagicElement[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }

    public static MagicElement randomChoosable(RandomSource r) {
        MagicElement[] c = {FIRE, WATER, WIND, LIGHTNING, DARKNESS, LIGHT, THORN};
        return c[r.nextInt(c.length)];
    }
}
