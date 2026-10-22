package com.blackclovermod.entity;

import com.blackclovermod.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Projectile magique générique. Il n'a pas de modèle 3D : il est entièrement dessiné
 * avec des particules (côté client, dans tick()).
 */
public class MagicProjectileEntity extends Projectile {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(MagicProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(MagicProjectileEntity.class, EntityDataSerializers.FLOAT);

    private float damage = 5f;
    private final Set<Integer> hitIds = new HashSet<>();

    public MagicProjectileEntity(EntityType<? extends MagicProjectileEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public MagicProjectileEntity(Level level, LivingEntity owner, ProjectileKind kind, float damage, float size) {
        this(ModEntities.MAGIC_PROJECTILE.get(), level);
        this.setOwner(owner);
        this.damage = damage;
        this.entityData.set(KIND, kind.ordinal());
        this.entityData.set(SIZE, size);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(KIND, 0);
        this.entityData.define(SIZE, 0.4f);
    }

    public ProjectileKind kind() { return ProjectileKind.byIndex(this.entityData.get(KIND)); }

    public float size() { return this.entityData.get(SIZE); }

    @Override
    public void tick() {
        super.tick();
        ProjectileKind k = kind();
        Vec3 motion = getDeltaMovement();

        if (level().isClientSide) {
            clientTrail(k, motion);
            if (k.gravity > 0) setDeltaMovement(motion.add(0, -k.gravity, 0));
            setPos(position().add(motion));
            return;
        }

        Vec3 start = position();
        Vec3 end = start.add(motion);

        if (k.cancelsMagic) cancelOtherMagic();

        // Collision avec les entités
        AABB box = getBoundingBox().expandTowards(motion).inflate(size() * 0.5 + 0.3);
        for (Entity e : level().getEntities(this, box, this::canHitEntity)) {
            if (e instanceof LivingEntity le && hitIds.add(le.getId())) {
                hitLiving(le, k);
                if (!k.piercing) {
                    impact(position(), k);
                    discard();
                    return;
                }
            }
        }

        // Collision avec les blocs
        BlockHitResult bhr = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (bhr.getType() != HitResult.Type.MISS) {
            setPos(bhr.getLocation());
            impact(bhr.getLocation(), k);
            discard();
            return;
        }

        if (tickCount > k.lifetime) {
            impact(position(), k);
            discard();
            return;
        }

        if (k.gravity > 0) setDeltaMovement(motion.add(0, -k.gravity, 0));
        setPos(end);
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!(e instanceof LivingEntity) || !e.isAlive() || e.isSpectator()) return false;
        Entity owner = getOwner();
        if (owner == null) return true;
        if (e == owner) return false;
        if (owner instanceof Monster && e instanceof Monster) return false; // les boss ne blessent pas leurs sbires
        return !e.isAlliedTo(owner);
    }

    private DamageSource source() {
        Entity owner = getOwner();
        return owner instanceof LivingEntity lo ? damageSources().mobProjectile(this, lo) : damageSources().magic();
    }

    private void hitLiving(LivingEntity le, ProjectileKind k) {
        if (k.resetInvulnerability) le.invulnerableTime = 0;
        le.hurt(source(), damage);
        Vec3 dir = getDeltaMovement().normalize();
        switch (k) {
            case FIREBALL, FLAME_LION -> le.setSecondsOnFire(6);
            case WATER_BULLET -> { le.clearFire(); le.knockback(0.8, -dir.x, -dir.z); }
            case WATER_DRAGON -> { le.clearFire(); le.knockback(1.6, -dir.x, -dir.z); }
            case WIND_BLADE -> le.knockback(0.5, -dir.x, -dir.z);
            case THUNDER -> {
                visualLightning(le.position());
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
            }
            case THORN -> {
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                le.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            }
            case DARK_SLASH -> le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            case LIGHT_SWORD, ELF_LIGHT -> le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0));
            case ANTI_SLASH -> { if (!(le instanceof Player)) le.removeAllEffects(); }
            case ROCK -> le.knockback(1.2, -dir.x, -dir.z);
            case DEMON_ORB -> le.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
        }
    }

    /** Effet quand le projectile explose / disparaît. */
    private void impact(Vec3 pos, ProjectileKind k) {
        if (!(level() instanceof ServerLevel l)) return;
        switch (k) {
            case FIREBALL -> {
                l.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 40, 0.3, 0.3, 0.3, 0.12);
                l.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
                playAt(pos, SoundEvents.GENERIC_EXPLODE, 0.7f, 1.4f);
                splash(pos, 2.5, damage * 0.4f, true);
            }
            case FLAME_LION -> {
                l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
                l.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 150, 0.8, 0.8, 0.8, 0.25);
                l.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 20, 0.5, 0.5, 0.5, 0);
                playAt(pos, SoundEvents.GENERIC_EXPLODE, 1.2f, 0.9f);
                splash(pos, 4, damage * 0.6f, true);
            }
            case WATER_BULLET, WATER_DRAGON -> {
                l.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y, pos.z, k == ProjectileKind.WATER_DRAGON ? 200 : 50, 0.6, 0.6, 0.6, 0.2);
                l.sendParticles(k.dust(1.5f), pos.x, pos.y, pos.z, 40, 0.6, 0.6, 0.6, 0);
                playAt(pos, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 0.8f, 1.2f);
            }
            case THUNDER -> {
                visualLightning(pos);
                splash(pos, 2, damage * 0.5f, false);
            }
            case DARK_SLASH -> l.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y, pos.z, 40, 0.5, 0.5, 0.5, 0.05);
            case ANTI_SLASH -> l.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 40, 0.5, 0.5, 0.5, 0.05);
            case ROCK -> {
                l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), pos.x, pos.y, pos.z, 50, 0.6, 0.4, 0.6, 0.15);
                playAt(pos, SoundEvents.STONE_BREAK, 1.5f, 0.6f);
                splash(pos, 2.5, damage * 0.5f, false);
            }
            case DEMON_ORB -> {
                l.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y, pos.z, 30, 0.5, 0.5, 0.5, 0.05);
                l.sendParticles(k.dust(2f), pos.x, pos.y, pos.z, 30, 0.6, 0.6, 0.6, 0);
                playAt(pos, SoundEvents.WITHER_SHOOT, 0.6f, 0.6f);
                splash(pos, 2, damage * 0.5f, false);
            }
            default -> l.sendParticles(k.element.particle(), pos.x, pos.y, pos.z, 15, 0.3, 0.3, 0.3, 0.05);
        }
    }

    /** Dégâts de zone autour d'un point. */
    private void splash(Vec3 pos, double radius, float dmg, boolean fire) {
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(radius), this::canHitEntity)) {
            if (hitIds.contains(e.getId())) continue;
            e.hurt(source(), dmg);
            if (fire) e.setSecondsOnFire(4);
        }
    }

    private void visualLightning(Vec3 pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level());
        if (bolt != null) {
            bolt.moveTo(pos);
            bolt.setVisualOnly(true);
            level().addFreshEntity(bolt);
        }
    }

    /** Anti-magie / Ténèbres : détruit les projectiles magiques adverses touchés. */
    private void cancelOtherMagic() {
        for (MagicProjectileEntity other : level().getEntitiesOfClass(MagicProjectileEntity.class, getBoundingBox().inflate(1.5),
                o -> o != this && o.getOwner() != getOwner())) {
            if (level() instanceof ServerLevel l) {
                l.sendParticles(ParticleTypes.LARGE_SMOKE, other.getX(), other.getY(), other.getZ(), 15, 0.3, 0.3, 0.3, 0.02);
            }
            other.discard();
        }
    }

    private void playAt(Vec3 p, net.minecraft.sounds.SoundEvent s, float v, float pitch) {
        level().playSound(null, p.x, p.y, p.z, s, SoundSource.PLAYERS, v, pitch);
    }

    /** Traînée de particules côté client : c'est ce qui "dessine" le sort. */
    private void clientTrail(ProjectileKind k, Vec3 motion) {
        float s = size();
        Vec3 dir = motion.lengthSqr() > 1e-6 ? motion.normalize() : new Vec3(0, 0, 1);
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : side.normalize();
        double x = getX(), y = getY() + 0.25, z = getZ();

        int n = Math.max(2, (int) (s * 8));
        for (int i = 0; i < n; i++) {
            level().addParticle(k.dust(0.7f + s), x + (random.nextDouble() - 0.5) * s, y + (random.nextDouble() - 0.5) * s,
                    z + (random.nextDouble() - 0.5) * s, 0, 0, 0);
        }

        switch (k) {
            case FIREBALL -> {
                for (int i = 0; i < 4; i++) level().addParticle(ParticleTypes.FLAME, x, y, z, random.nextGaussian() * 0.03, random.nextGaussian() * 0.03, random.nextGaussian() * 0.03);
                level().addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
            }
            case FLAME_LION -> {
                for (int i = 0; i < 14; i++) level().addParticle(ParticleTypes.FLAME, x + random.nextGaussian() * s * 0.5, y + random.nextGaussian() * s * 0.5, z + random.nextGaussian() * s * 0.5, -dir.x * 0.1, -dir.y * 0.1 + 0.02, -dir.z * 0.1);
                if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
                // crinière
                for (int i = 0; i < 10; i++) {
                    double a = i * Math.PI * 2 / 10 + tickCount * 0.4;
                    Vec3 up = side.cross(dir);
                    Vec3 q = new Vec3(x, y, z).add(side.scale(Math.cos(a) * s)).add(up.scale(Math.sin(a) * s));
                    level().addParticle(ParticleTypes.FLAME, q.x, q.y, q.z, -dir.x * 0.15, -dir.y * 0.15, -dir.z * 0.15);
                }
            }
            case WATER_DRAGON -> {
                Vec3 up = side.cross(dir);
                for (int i = 0; i < 3; i++) {
                    double a = tickCount * 0.8 + i * Math.PI * 2 / 3;
                    Vec3 q = new Vec3(x, y, z).add(side.scale(Math.cos(a) * s * 0.7)).add(up.scale(Math.sin(a) * s * 0.7));
                    level().addParticle(ParticleTypes.SPLASH, q.x, q.y, q.z, 0, 0, 0);
                    level().addParticle(k.dust(1.8f), q.x, q.y, q.z, 0, 0, 0);
                }
                level().addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0, 0, 0);
            }
            case WIND_BLADE -> {
                for (double t = -s; t <= s; t += 0.15) {
                    level().addParticle(ParticleTypes.CLOUD, x + side.x * t, y + side.y * t - Math.abs(t) * 0.3, z + side.z * t, 0, 0, 0);
                }
            }
            case THUNDER -> {
                for (int i = 0; i < 4; i++) level().addParticle(ParticleTypes.ELECTRIC_SPARK, x + random.nextGaussian() * 0.2, y + random.nextGaussian() * 0.2, z + random.nextGaussian() * 0.2, random.nextGaussian() * 0.1, random.nextGaussian() * 0.1, random.nextGaussian() * 0.1);
            }
            case DARK_SLASH, ANTI_SLASH -> {
                // croissant de lune perpendiculaire à la trajectoire
                for (double t = -s; t <= s; t += 0.1) {
                    double curve = (1 - (t * t) / (s * s)) * 0.4;
                    double px = x + side.x * t - dir.x * curve, py = y + side.y * t, pz = z + side.z * t - dir.z * curve;
                    level().addParticle(k.dust(1.6f), px, py, pz, 0, 0, 0);
                    if (random.nextInt(3) == 0) level().addParticle(k == ProjectileKind.ANTI_SLASH ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SQUID_INK, px, py, pz, 0, 0, 0);
                }
            }
            case LIGHT_SWORD, ELF_LIGHT -> {
                // une lame de lumière : une ligne derrière la pointe + une garde
                for (double t = 0; t < 1.2; t += 0.12) level().addParticle(ParticleTypes.END_ROD, x - dir.x * t, y - dir.y * t, z - dir.z * t, 0, 0, 0);
                for (double t = -0.25; t <= 0.25; t += 0.1) level().addParticle(k.dust(1f), x - dir.x * 0.9 + side.x * t, y - dir.y * 0.9, z - dir.z * 0.9 + side.z * t, 0, 0, 0);
            }
            case THORN -> {
                level().addParticle(ParticleTypes.COMPOSTER, x, y, z, 0, 0, 0);
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_LEAVES.defaultBlockState()), x, y, z, 0, 0, 0);
            }
            case ROCK -> level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()), x, y, z, 0, 0, 0);
            case DEMON_ORB -> {
                level().addParticle(ParticleTypes.SQUID_INK, x, y, z, 0, 0, 0);
                level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + random.nextGaussian() * 0.2, y, z + random.nextGaussian() * 0.2, 0, 0, 0);
            }
            default -> level().addParticle(k.element.particle(), x, y, z, 0, 0, 0);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("Kind", entityData.get(KIND));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        entityData.set(KIND, tag.getInt("Kind"));
    }

    @Override
    public boolean shouldBeSaved() {
        return false; // les sorts ne sont pas sauvegardés dans le monde
    }
}
