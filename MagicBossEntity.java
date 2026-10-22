package com.blackclovermod.entity.boss;

import com.blackclovermod.entity.MagicProjectileEntity;
import com.blackclovermod.entity.ProjectileKind;
import com.blackclovermod.magic.MagicElement;
import com.blackclovermod.util.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base commune à tous les boss :
 *  - barre de vie de boss
 *  - animation d'apparition (le boss sort du sol dans une spirale de magie, 4 secondes)
 *  - phases (transformation à 50 % et 25 % de vie)
 *  - animation de mort prolongée
 */
public abstract class MagicBossEntity extends Monster {
    private static final EntityDataAccessor<Integer> SPAWN_TICKS = SynchedEntityData.defineId(MagicBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(MagicBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRANSFORM = SynchedEntityData.defineId(MagicBossEntity.class, EntityDataSerializers.INT);

    public static final int SPAWN_DURATION = 80;
    public static final int TRANSFORM_DURATION = 50;
    public static final int DEATH_DURATION = 60;

    protected final ServerBossEvent bossEvent;
    protected int attackTimer = 50;

    protected MagicBossEntity(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(this.getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10);
        this.xpReward = 250;
        this.setPersistenceRequired();
    }

    // ---------------- À définir dans chaque boss ----------------
    public abstract MagicElement bossElement();
    public abstract String bossId();
    protected abstract int maxPhase();
    /** Lance une attaque et renvoie le nombre de ticks avant la suivante. */
    protected abstract int performAttack(ServerLevel level, LivingEntity target);
    protected abstract void dropBossLoot();
    protected void onPhaseChange(ServerLevel level, int phase) {}
    protected void bossTick(ServerLevel level, @Nullable LivingEntity target) {}
    public float renderScaleMultiplier() { return 1f; }

    // ---------------- Données synchronisées ----------------
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SPAWN_TICKS, 0);
        entityData.define(PHASE, 1);
        entityData.define(TRANSFORM, 0);
    }

    public void startSpawnAnimation() { entityData.set(SPAWN_TICKS, SPAWN_DURATION); }
    public int spawnTicks() { return entityData.get(SPAWN_TICKS); }
    public int phase() { return entityData.get(PHASE); }
    public int transformTicks() { return entityData.get(TRANSFORM); }

    // ---------------- Logique principale ----------------
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || spawnTicks() > 0 || transformTicks() > 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel l) || isDeadOrDying()) return;

        int st = spawnTicks();
        if (st > 0) {
            spawnEffects(l, SPAWN_DURATION - st);
            entityData.set(SPAWN_TICKS, st - 1);
            if (st == 1) onSpawnFinished(l);
            return;
        }
        int tr = transformTicks();
        if (tr > 0) {
            transformEffects(l, TRANSFORM_DURATION - tr);
            entityData.set(TRANSFORM, tr - 1);
            if (tr == 1) onTransformFinished(l);
            return;
        }

        bossEvent.setProgress(getHealth() / getMaxHealth());
        checkPhases(l);

        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || (target instanceof Player pl && (pl.isCreative() || pl.isSpectator())))) target = null;
        bossTick(l, target);
        if (target != null && --attackTimer <= 0) attackTimer = performAttack(l, target);
    }

    private void checkPhases(ServerLevel l) {
        float ratio = getHealth() / getMaxHealth();
        int wanted = 1;
        if (maxPhase() >= 2 && ratio <= 0.5f) wanted = 2;
        if (maxPhase() >= 3 && ratio <= 0.25f) wanted = 3;
        if (wanted > phase()) {
            entityData.set(PHASE, wanted);
            entityData.set(TRANSFORM, TRANSFORM_DURATION);
            announce(getDisplayName().copy().withStyle(ChatFormatting.DARK_RED),
                    Component.translatable("boss.blackclover." + bossId() + ".phase" + wanted).withStyle(ChatFormatting.RED), 50);
            onPhaseChange(l, wanted);
        }
    }

    // ---------------- Animations ----------------
    /** Le boss "sort" du sol dans une spirale (la descente visuelle est faite dans BossRenderer). */
    protected void spawnEffects(ServerLevel l, int k) {
        Vec3 base = position();
        MagicElement el = bossElement();
        double r = 2.6 - k * 0.02;
        for (int i = 0; i < 4; i++) {
            double a = k * 0.25 + i * Math.PI / 2;
            l.sendParticles(el.dust(2f), base.x + Math.cos(a) * r, base.y + k * 0.045, base.z + Math.sin(a) * r, 2, 0.05, 0.05, 0.05, 0);
        }
        l.sendParticles(el.particle(), base.x, base.y + 0.2, base.z, 4, 1.2, 0.1, 1.2, 0.02);
        l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(blockPosition().below())), base.x, base.y + 0.1, base.z, 10, 1, 0.1, 1, 0.1);
        if (k % 4 == 0) Fx.magicCircle(l, el.dust(1.5f), base.add(0, 0.05, 0), 3.5, k);
        if (k % 20 == 0) l.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2f, 0.8f);
    }

    protected void onSpawnFinished(ServerLevel l) {
        Vec3 c = position().add(0, getBbHeight() / 2, 0);
        Fx.burst(l, ParticleTypes.EXPLOSION_EMITTER, c, 1, 0, 0);
        Fx.burst(l, bossElement().dust(2.5f), c, 200, 2, 0);
        Fx.burst(l, bossElement().particle(), c, 120, 1.5, 0.3);
        Fx.ringOut(l, bossElement().dust(2f), position().add(0, 0.2, 0), 1, 60, 0.8);
        l.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2f, 0.8f);
        announce(getDisplayName().copy().withStyle(ChatFormatting.GOLD),
                Component.translatable("boss.blackclover." + bossId() + ".subtitle").withStyle(ChatFormatting.GRAY), 60);
    }

    protected void transformEffects(ServerLevel l, int k) {
        Vec3 c = position().add(0, getBbHeight() / 2, 0);
        Fx.converge(l, ParticleTypes.ENCHANT, c, 10, 4);
        Fx.converge(l, bossElement().dust(2f), c, 6, 3 - k * 0.04);
        l.sendParticles(bossElement().particle(), c.x, c.y, c.z, 4, 0.5, 1, 0.5, 0.05);
        if (k % 10 == 0) l.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2f, 0.6f + k * 0.01f);
    }

    protected void onTransformFinished(ServerLevel l) {
        Vec3 c = position().add(0, getBbHeight() / 2, 0);
        Fx.burst(l, ParticleTypes.EXPLOSION_EMITTER, c, 2, 1, 0);
        Fx.burst(l, bossElement().dust(3f), c, 250, 2.5, 0);
        Fx.ringOut(l, bossElement().dust(2.5f), position().add(0, 0.3, 0), 1, 80, 1.0);
        l.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2f, 0.6f);
        for (LivingEntity e : victims(8)) e.knockback(1.2, getX() - e.getX(), getZ() - e.getZ());
    }

    /** Mort spectaculaire : le boss se désagrège pendant 3 secondes. */
    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (level() instanceof ServerLevel l) {
            Vec3 c = position().add(0, getBbHeight() / 2, 0);
            l.sendParticles(bossElement().dust(2f), c.x, c.y, c.z, 8, 0.6, 1, 0.6, 0);
            l.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 3, 0.4, 0.8, 0.4, 0.15);
            if (deathTime % 10 == 0) l.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1f, 1.5f);
            if (deathTime >= DEATH_DURATION) {
                Fx.burst(l, ParticleTypes.EXPLOSION_EMITTER, c, 3, 1, 0);
                Fx.burst(l, ParticleTypes.TOTEM_OF_UNDYING, c, 200, 1, 0.6);
                l.playSound(null, getX(), getY(), getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 1.5f, 1f);
                level().broadcastEntityEvent(this, (byte) 60);
                remove(RemovalReason.KILLED);
            }
        }
    }

    // ---------------- Dégâts ----------------
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (spawnTicks() > 0 || transformTicks() > 0) return false;
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_DROWNING)) return false;
        return super.hurt(source, modifyDamage(source, amount));
    }

    protected float modifyDamage(DamageSource source, float amount) { return amount; }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        dropBossLoot();
    }

    // ---------------- Outils pour les attaques ----------------
    protected List<LivingEntity> victims(double r) {
        return victimsAt(position(), r);
    }

    protected List<LivingEntity> victimsAt(Vec3 c, double r) {
        return level().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(r),
                e -> e != this && e.isAlive() && !(e instanceof Monster)
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                        && e.distanceToSqr(c) <= r * r);
    }

    protected void strikeDamage(LivingEntity e, float amount) {
        e.invulnerableTime = 0;
        e.hurt(damageSources().mobAttack(this), amount);
    }

    protected MagicProjectileEntity fireAt(ProjectileKind kind, float dmg, float size, Vec3 from, Vec3 to, float speed) {
        MagicProjectileEntity m = new MagicProjectileEntity(level(), this, kind, dmg, size);
        m.setPos(from.x, from.y, from.z);
        Vec3 d = to.subtract(from);
        m.shoot(d.x, d.y, d.z, speed, 1f);
        level().addFreshEntity(m);
        return m;
    }

    protected List<ServerPlayer> nearbyPlayers(double r) {
        return ((ServerLevel) level()).getPlayers(p -> p.distanceToSqr(this) < r * r && !p.isSpectator());
    }

    protected void announce(Component title, Component sub, int stay) {
        for (ServerPlayer p : nearbyPlayers(64)) Fx.title(p, title, sub, 10, stay, 20);
    }

    // ---------------- Barre de boss & sauvegarde ----------------
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BossPhase", phase());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(PHASE, Math.max(1, tag.getInt("BossPhase")));
        if (hasCustomName()) bossEvent.setName(getDisplayName());
    }
}
