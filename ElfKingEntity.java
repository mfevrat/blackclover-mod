package com.blackclovermod.entity.boss;

import com.blackclovermod.entity.ProjectileKind;
import com.blackclovermod.magic.MagicElement;
import com.blackclovermod.registry.ModItems;
import com.blackclovermod.util.Fx;
import com.blackclovermod.util.TaskScheduler;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * BOSS 2 : LE ROI ELFE SPECTRAL — flotte dans les airs avec des ailes de lumière.
 * Attaques : Pluie d'épées de lumière, Téléportation éclair, Jugement céleste (colonnes de lumière).
 * Phase 2 (50 %) : plus d'épées, jugement en triangle, invoque des esprits.
 */
public class ElfKingEntity extends MagicBossEntity {
    private static final DustParticleOptions WING = new DustParticleOptions(new Vector3f(1f, 0.9f, 0.5f), 1.1f);
    private int spiritCooldown = 0;

    public ElfKingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.ATTACK_DAMAGE, 10)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 32f));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public MagicElement bossElement() { return MagicElement.LIGHT; }
    @Override public String bossId() { return "elf_king"; }
    @Override protected int maxPhase() { return 2; }

    @Override
    protected void bossTick(ServerLevel l, @Nullable LivingEntity target) {
        if (spiritCooldown > 0) spiritCooldown--;
        if (tickCount % 3 == 0) l.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.1, getZ(), 1, 0.3, 0.05, 0.3, 0.01);
        if (tickCount % 4 == 0) drawWings(l);

        if (target != null) {
            // Vol en orbite autour de la cible
            double a = tickCount * 0.015;
            Vec3 desired = target.position().add(Math.cos(a) * 9, 4.5, Math.sin(a) * 9);
            Vec3 diff = desired.subtract(position());
            Vec3 acc = diff.lengthSqr() > 1 ? diff.normalize().scale(0.06) : diff.scale(0.03);
            setDeltaMovement(getDeltaMovement().scale(0.9).add(acc));
            double dx = target.getX() - getX(), dz = target.getZ() - getZ();
            float yaw = (float) (Mth.atan2(dz, dx) * (180 / Math.PI)) - 90f;
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.8).add(0, Math.sin(tickCount * 0.05) * 0.005, 0));
        }
    }

    /** Ailes de lumière dans le dos. */
    private void drawWings(ServerLevel l) {
        Vec3 fwd = Vec3.directionFromRotation(0, yBodyRot);
        Vec3 side = fwd.cross(Fx.UP).normalize();
        Vec3 back = position().add(0, 1.6, 0).subtract(fwd.scale(0.35));
        float flap = (float) Math.sin(tickCount * 0.2) * 0.3f;
        for (int s = -1; s <= 1; s += 2) {
            for (int i = 1; i <= 7; i++) {
                double t = i / 7.0;
                Vec3 q = back.add(side.scale(s * t * 1.8)).add(0, Math.sin(t * Math.PI) * 0.8 + t * flap, 0).subtract(fwd.scale(t * 0.4));
                l.sendParticles(WING, q.x, q.y, q.z, 1, 0, 0, 0, 0);
                l.sendParticles(WING, q.x, q.y - 0.4 * (1 - t), q.z, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    protected int performAttack(ServerLevel l, LivingEntity target) {
        boolean p2 = phase() >= 2;
        int roll = random.nextInt(p2 ? 4 : 3);
        if (roll == 3 && spiritCooldown > 0) roll = 0;
        switch (roll) {
            case 0 -> { lightSwords(l, target, p2 ? 9 : 5); return p2 ? 45 : 60; }
            case 1 -> { blink(l, target); return 25; }
            case 2 -> { judgment(l, p2); return p2 ? 70 : 90; }
            default -> { summonSpirits(l, target); spiritCooldown = 500; return 50; }
        }
    }

    private void lightSwords(ServerLevel l, LivingEntity target, int n) {
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        Vec3 side = fwd.cross(Fx.UP).normalize();
        playSound(SoundEvents.EVOKER_CAST_SPELL, 2f, 1.4f);
        for (int i = 0; i < n; i++) {
            double off = i - (n - 1) / 2.0;
            final Vec3 spot = position().add(0, 3.0 - Math.abs(off) * 0.15, 0).add(side.scale(off * 0.7)).subtract(fwd.scale(0.5));
            final int idx = i;
            TaskScheduler.repeat(10 + i * 3, k -> l.sendParticles(ParticleTypes.END_ROD, spot.x, spot.y, spot.z, 1, 0.05, 0.05, 0.05, 0));
            TaskScheduler.later(10 + i * 3, () -> {
                if (!isAlive() || !target.isAlive()) return;
                fireAt(ProjectileKind.ELF_LIGHT, 7f, 0.4f, spot, target.getEyePosition(), 1.7f);
                l.sendParticles(ParticleTypes.END_ROD, spot.x, spot.y, spot.z, 5, 0.1, 0.1, 0.1, 0.05);
                playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 1.2f + idx * 0.05f);
            });
        }
    }

    private void blink(ServerLevel l, LivingEntity target) {
        Vec3 old = position();
        double a = random.nextDouble() * Math.PI * 2;
        Vec3 np = target.position().add(Math.cos(a) * 7, 4 + random.nextDouble() * 2, Math.sin(a) * 7);
        Fx.burst(l, ParticleTypes.END_ROD, old.add(0, 1, 0), 40, 0.4, 0.15);
        Fx.line(l, ParticleTypes.END_ROD, old.add(0, 1, 0), np.add(0, 1, 0), 0.5);
        teleportTo(np.x, np.y, np.z);
        Fx.burst(l, ParticleTypes.FLASH, np.add(0, 1, 0), 1, 0, 0);
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5f, 1.6f);
    }

    /** Jugement céleste : un cercle d'avertissement sous chaque joueur, puis une colonne de lumière. */
    private void judgment(ServerLevel l, boolean p2) {
        playSound(SoundEvents.BEACON_ACTIVATE, 2f, 1.5f);
        for (ServerPlayer pl : nearbyPlayers(40)) {
            List<Vec3> spots = new ArrayList<>();
            spots.add(Fx.groundAt(l, pl.position().add(0, 1, 0)));
            if (p2) {
                for (int i = 0; i < 3; i++) {
                    double a = i * 2.094 + random.nextDouble();
                    spots.add(Fx.groundAt(l, pl.position().add(Math.cos(a) * 3, 1, Math.sin(a) * 3)));
                }
            }
            for (Vec3 s : spots) {
                TaskScheduler.repeat(30, k -> {
                    if (!isAlive()) return;
                    if (k < 22) {
                        Fx.ring(l, MagicElement.LIGHT.dust(1.5f), s.add(0, 0.1, 0), 2.0, 20);
                        Fx.ring(l, ParticleTypes.END_ROD, s.add(0, 0.1, 0), 2.0 * (1 - k / 22.0), 10);
                    } else if (k == 22) {
                        Fx.pillar(l, ParticleTypes.END_ROD, s, 20, 0.5, 3);
                        Fx.burst(l, ParticleTypes.FLASH, s.add(0, 1, 0), 1, 0, 0);
                        for (LivingEntity e : victimsAt(s, 2.2)) strikeDamage(e, 12f);
                        Fx.sound(l, s, SoundEvents.AMETHYST_BLOCK_CHIME, 2f, 0.5f);
                    }
                });
            }
        }
    }

    private void summonSpirits(ServerLevel l, LivingEntity target) {
        playSound(SoundEvents.EVOKER_CAST_SPELL, 2f, 0.8f);
        for (int i = 0; i < 2; i++) {
            Vex v = EntityType.VEX.create(l);
            if (v == null) continue;
            v.moveTo(getX() + (i == 0 ? 1.5 : -1.5), getY() + 1, getZ(), 0f, 0f);
            v.setLimitedLife(600);
            v.setCustomName(Component.translatable("entity.blackclover.elf_spirit"));
            v.setTarget(target);
            l.addFreshEntity(v);
            Fx.burst(l, ParticleTypes.END_ROD, v.position(), 25, 0.3, 0.1);
        }
    }

    @Override
    protected void onPhaseChange(ServerLevel l, int phase) {
        heal(getMaxHealth() * 0.05f);
        playSound(SoundEvents.BEACON_POWER_SELECT, 3f, 0.6f);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override
    protected void dropBossLoot() {
        spawnAtLocation(new ItemStack(ModItems.ANCIENT_PAGE.get(), 2));
        spawnAtLocation(new ItemStack(ModItems.MANA_CRYSTAL.get(), 3));
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.EVOKER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.EVOKER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.EVOKER_DEATH; }
}
