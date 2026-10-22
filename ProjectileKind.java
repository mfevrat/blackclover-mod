package com.blackclovermod.entity;

import com.blackclovermod.magic.MagicElement;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;

/** Les différents types de projectiles magiques et leur comportement. */
public enum ProjectileKind {
    //            élément               perçant annuleMagie resetInvu gravité durée couleur(-1 = élément)
    FIREBALL(MagicElement.FIRE,          false, false, false, 0f,     60, -1),
    FLAME_LION(MagicElement.FIRE,        true,  false, false, 0f,     50, -1),
    WATER_BULLET(MagicElement.WATER,     false, false, false, 0f,     40, -1),
    WATER_DRAGON(MagicElement.WATER,     true,  false, false, 0f,     60, -1),
    WIND_BLADE(MagicElement.WIND,        true,  false, false, 0f,     25, -1),
    THUNDER(MagicElement.LIGHTNING,      false, false, false, 0f,     30, -1),
    DARK_SLASH(MagicElement.DARKNESS,    true,  true,  false, 0f,     30, -1),
    LIGHT_SWORD(MagicElement.LIGHT,      false, false, true,  0f,     40, -1),
    THORN(MagicElement.THORN,            false, false, false, 0f,     40, -1),
    ANTI_SLASH(MagicElement.ANTI,        true,  true,  false, 0f,     30, -1),
    ROCK(MagicElement.EARTH,             false, false, false, 0.035f, 80, -1),
    ELF_LIGHT(MagicElement.LIGHT,        false, false, true,  0f,     60, 0xFFE27A),
    DEMON_ORB(MagicElement.DARKNESS,     false, false, false, 0f,     80, 0x8B0A0A);

    public final MagicElement element;
    public final boolean piercing;
    public final boolean cancelsMagic;
    public final boolean resetInvulnerability;
    public final float gravity;
    public final int lifetime;
    private final int colorOverride;

    ProjectileKind(MagicElement element, boolean piercing, boolean cancelsMagic, boolean resetInvulnerability,
                   float gravity, int lifetime, int colorOverride) {
        this.element = element;
        this.piercing = piercing;
        this.cancelsMagic = cancelsMagic;
        this.resetInvulnerability = resetInvulnerability;
        this.gravity = gravity;
        this.lifetime = lifetime;
        this.colorOverride = colorOverride;
    }

    public int color() { return colorOverride >= 0 ? colorOverride : element.color; }

    public DustParticleOptions dust(float scale) {
        int c = color();
        return new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f),
                Math.max(0.2f, Math.min(4f, scale)));
    }

    public static ProjectileKind byIndex(int i) {
        ProjectileKind[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }
}
