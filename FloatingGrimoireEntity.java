package com.blackclovermod.entity;

import com.blackclovermod.ceremony.GrimoireTower;
import com.blackclovermod.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Un grimoire qui vole dans la Tour, d'étagère en étagère.
 * Le serveur calcule sa trajectoire (coordonnées polaires autour du centre de la tour),
 * le client le dessine avec le modèle de livre de la table d'enchantement.
 */
public class FloatingGrimoireEntity extends Entity {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(FloatingGrimoireEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(FloatingGrimoireEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GLOW = SynchedEntityData.defineId(FloatingGrimoireEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(FloatingGrimoireEntity.class, EntityDataSerializers.FLOAT);

    public static final int WANDER = 0;   // vole d'étagère en étagère
    public static final int SCRIPTED = 1; // contrôlé par la cérémonie (vient vers le joueur)
    public static final int OPEN = 2;     // grand ouvert devant le joueur, pages qui défilent
    public static final int FLEE = 3;     // se plaque contre les murs, tremblant (grimoire à 5 feuilles)

    // État du vol (serveur uniquement)
    private double fromAng, toAng, fromR, toR, fromY, toY, arc;
    private double progress = 1, speed = 0.02;
    private int life;

    public FloatingGrimoireEntity(EntityType<? extends FloatingGrimoireEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public FloatingGrimoireEntity(Level level, int color) {
        this(ModEntities.FLOATING_GRIMOIRE.get(), level);
        setColor(color);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(COLOR, 0x7B3F2A);
        entityData.define(MODE, WANDER);
        entityData.define(GLOW, false);
        entityData.define(SCALE, 1f);
    }

    public int getColor() { return entityData.get(COLOR); }
    public void setColor(int c) { entityData.set(COLOR, c); }
    public int getMode() { return entityData.get(MODE); }
    public void setMode(int m) { entityData.set(MODE, m); }
    public boolean isGlowingBook() { return entityData.get(GLOW); }
    public void setGlowingBook(boolean g) { entityData.set(GLOW, g); }
    public float getVisualScale() { return entityData.get(SCALE); }
    public void setVisualScale(float s) { entityData.set(SCALE, s); }

    private static Vec3 center() { return GrimoireTower.platformCenter(); }

    private double currentAngle() { return Math.atan2(getZ() - center().z, getX() - center().x); }
    private double currentRadius() { return Math.sqrt(Mth.square(getX() - center().x) + Mth.square(getZ() - center().z)); }

    /** Place le livre au hasard contre les étagères. */
    public void placeRandom() {
        double a = random.nextDouble() * Math.PI * 2;
        double r = 12.5 + random.nextDouble() * 4;
        double y = GrimoireTower.PLATFORM_Y - 26 + random.nextDouble() * 60;
        setPos(center().x + Math.cos(a) * r, y, center().z + Math.sin(a) * r);
        progress = 1;
    }

    private void pickNewTarget() {
        fromAng = currentAngle();
        fromR = currentRadius();
        fromY = getY();
        toAng = fromAng + (random.nextBoolean() ? 1 : -1) * (0.4 + random.nextDouble() * 1.8);
        toR = 12 + random.nextDouble() * 4.8;
        toY = Mth.clamp(fromY + (random.nextDouble() - 0.5) * 26, GrimoireTower.PLATFORM_Y - 30, GrimoireTower.PLATFORM_Y + 38);
        arc = 0.5 + random.nextDouble() * 3;
        speed = 0.012 + random.nextDouble() * 0.02;
        progress = 0;
    }

    /** Le livre fuit et se plaque contre le mur (arrivée du grimoire à 5 feuilles). */
    public void flee() {
        setMode(FLEE);
        fromAng = currentAngle();
        toAng = fromAng + (random.nextDouble() - 0.5) * 0.3;
        fromR = currentRadius();
        toR = 16.8;
        fromY = getY();
        toY = getY() + (random.nextDouble() - 0.5) * 3;
        arc = 0.2;
        speed = 0.05 + random.nextDouble() * 0.03;
        progress = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (isGlowingBook()) {
                level().addParticle(ParticleTypes.ENCHANT, getX() + random.nextGaussian() * 0.4, getY() + 0.5, getZ() + random.nextGaussian() * 0.4, random.nextGaussian() * 0.5, 0.3, random.nextGaussian() * 0.5);
                if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), random.nextGaussian() * 0.02, 0.02, random.nextGaussian() * 0.02);
            } else if (random.nextInt(25) == 0) {
                level().addParticle(ParticleTypes.ENCHANT, getX(), getY() + 0.3, getZ(), 0, 0.1, 0);
            }
            return;
        }

        if (++life > 1200) { // sécurité : jamais de livre oublié dans la tour
            discard();
            return;
        }

        int mode = getMode();
        if (mode == WANDER || mode == FLEE) {
            if (progress >= 1) {
                if (mode == FLEE) { // tremble contre le mur
                    if (tickCount % 2 == 0) setPos(getX() + (random.nextDouble() - 0.5) * 0.08, getY() + (random.nextDouble() - 0.5) * 0.08, getZ() + (random.nextDouble() - 0.5) * 0.08);
                    return;
                }
                pickNewTarget();
            }
            progress = Math.min(1, progress + speed);
            double e = smooth(progress);
            double a = Mth.lerp(e, fromAng, toAng);
            double r = Mth.lerp(e, fromR, toR);
            double y = Mth.lerp(e, fromY, toY) + Math.sin(progress * Math.PI) * arc;
            moveFacing(new Vec3(center().x + Math.cos(a) * r, y, center().z + Math.sin(a) * r));
        }
    }

    private static double smooth(double t) { return t * t * (3 - 2 * t); }

    /** Déplace le livre et l'oriente dans le sens du vol. */
    public void moveFacing(Vec3 np) {
        Vec3 d = np.subtract(position());
        if (d.horizontalDistanceSqr() > 1e-5) {
            setYRot((float) (Mth.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90f);
        }
        setPos(np);
    }

    public void faceToward(Vec3 target) {
        Vec3 d = target.subtract(position());
        setYRot((float) (Mth.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90f);
    }

    @Override
    public boolean shouldBeSaved() { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
