package com.blackclovermod.entity.boss;

import com.blackclovermod.entity.MagicProjectileEntity;
import com.blackclovermod.entity.ProjectileKind;
import com.blackclovermod.magic.MagicData;
import com.blackclovermod.magic.MagicElement;
import com.blackclovermod.registry.ModItems;
import com.blackclovermod.util.Fx;
import com.blackclovermod.util.TaskScheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * BOSS 3 : L'ARCHIDÉMON MORVATH — un démon de haut rang.
 * Il RÉSISTE à la magie (-50 % des projectiles magiques) mais l'ANTI-MAGIE lui inflige x2 dégâts.
 * Attaques : Orbes démoniaques, Trou noir, "Paroles maudites" (malédictions).
 * Phase 2 (50 %) : grandit, Griffe démoniaque (téléportation + coup).
 * Phase 3 (25 %) : Pluie de météores des Enfers.
 */
public class ArchdemonEntity extends MagicBossEntity {
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.62f, 0.05f, 0.05f), 1.6f);

    private record Curse(String key, MobEffect effect, int duration, int amplifier) {}

    private static final Curse[] CURSES = {
            new Curse("kneel", MobEffects.MOVEMENT_SLOWDOWN, 80, 3),
            new Curse("blind", MobEffects.BLINDNESS, 80, 0),
            new Curse("rot", MobEffects.WITHER, 100, 1),
            new Curse("rise", MobEffects.LEVITATION, 60, 1),
            new Curse("weaken", MobEffects.WEAKNESS, 120, 1),
    };

    public ArchdemonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        bossEvent.setDarkenScreen(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 700)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.ARMOR_TOUGHNESS, 6)
                .add(Attributes.ATTACK_DAMAGE, 18)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 24f));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public MagicElement bossElement() { return MagicElement.DARKNESS; }
    @Override public String bossId() { return "archdemon"; }
    @Override protected int maxPhase() { return 3; }
    @Override public float renderScaleMultiplier() { return phase() >= 2 ? 1.15f : 1f; }

    @Override
    protected void bossTick(ServerLevel l, @Nullable LivingEntity target) {
        if (tickCount % 2 == 0) {
            l.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.5, getZ(), 2, 0.5, 1, 0.5, 0.01);
            l.sendParticles(BLOOD, getX(), getY() + 2, getZ(), 1, 0.5, 1, 0.5, 0);
        }
        if (phase() >= 3 && tickCount % 3 == 0) l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.2, getZ(), 2, 0.6, 0.1, 0.6, 0.02);
    }

    @Override
    protected int performAttack(ServerLevel l, LivingEntity target) {
        int ph = phase();
        int roll = random.nextInt(ph >= 3 ? 5 : ph >= 2 ? 4 : 3);
        switch (roll) {
            case 0 -> darkOrbs(l, target, 3 + ph * 2);
            case 1 -> blackHole(l, target);
            case 2 -> curse(l, target);
            case 3 -> demonClaw(l, target);
            default -> meteorRain(l, target);
        }
        return 50 - ph * 8;
    }

    private void darkOrbs(ServerLevel l, LivingEntity target, int n) {
        playSound(SoundEvents.WITHER_SHOOT, 1.5f, 0.6f);
        for (int i = 0; i < n; i++) {
            TaskScheduler.later(i * 4, () -> {
                if (!isAlive() || !target.isAlive()) return;
                Vec3 from = position().add(0, getBbHeight() * 0.75, 0);
                Vec3 to = target.getEyePosition().add(random.nextGaussian() * 0.8, random.nextGaussian() * 0.4, random.nextGaussian() * 0.8);
                fireAt(ProjectileKind.DEMON_ORB, 10f, 0.6f, from, to, 1.4f);
            });
        }
    }

    private void blackHole(ServerLevel l, LivingEntity target) {
        Vec3 c = target.position().add(0, 1.5, 0);
        playSound(SoundEvents.WITHER_AMBIENT, 2f, 0.4f);
        TaskScheduler.repeat(70, k -> {
            if (!isAlive()) return;
            for (int i = 0; i < 14; i++) {
                double a = k * 0.3 + i * Math.PI * 2 / 14, r = 5 - (k % 20) * 0.22;
                double px = c.x + Math.cos(a) * r, pz = c.z + Math.sin(a) * r;
                l.sendParticles(ParticleTypes.SQUID_INK, px, c.y, pz, 0, c.x - px, 0, c.z - pz, 0.08);
            }
            Fx.burst(l, ParticleTypes.REVERSE_PORTAL, c, 6, 0.3, 0.05);
            Fx.burst(l, BLOOD, c, 3, 0.3, 0);
            for (LivingEntity e : victimsAt(c, 10)) {
                Vec3 v = c.subtract(e.position()).normalize().scale(0.09);
                e.setDeltaMovement(e.getDeltaMovement().add(v));
                e.hurtMarked = true;
                if (k % 10 == 0 && e.distanceToSqr(c) < 4) {
                    strikeDamage(e, 6f);
                    e.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
                }
            }
            if (k % 20 == 0) Fx.sound(l, c, SoundEvents.WARDEN_HEARTBEAT, 2f, 0.5f);
            if (k == 69) {
                Fx.burst(l, ParticleTypes.EXPLOSION_EMITTER, c, 1, 0, 0);
                for (LivingEntity e : victimsAt(c, 4)) strikeDamage(e, 8f);
                Fx.sound(l, c, SoundEvents.GENERIC_EXPLODE, 2f, 0.5f);
            }
        });
    }

    /** "Paroles maudites" : le démon prononce un mot qui devient réalité. */
    private void curse(ServerLevel l, LivingEntity target) {
        Curse c = CURSES[random.nextInt(CURSES.length)];
        target.addEffect(new MobEffectInstance(c.effect(), c.duration(), c.amplifier()));
        Component word = Component.translatable("curse.blackclover." + c.key()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
        if (target instanceof ServerPlayer sp) Fx.title(sp, Component.literal(""), word, 5, 40, 10);
        for (ServerPlayer p : nearbyPlayers(32)) p.sendSystemMessage(getDisplayName().copy().append(" : ").append(word));
        TaskScheduler.repeat(20, k -> Fx.ring(l, BLOOD, target.position().add(0, 0.1 + k * 0.1, 0), 1.2, 16));
        Fx.burst(l, ParticleTypes.SQUID_INK, target.position().add(0, 1, 0), 30, 0.4, 0.05);
        playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.5f, 0.6f);
    }

    /** Griffe démoniaque (phase 2+) : réapparaît derrière la cible et frappe. */
    private void demonClaw(ServerLevel l, LivingEntity target) {
        Vec3 mark = target.position();
        TaskScheduler.repeat(12, k -> Fx.ring(l, BLOOD, mark.add(0, 0.1, 0), 3.5 * (1 - k / 12.0) + 0.3, 20));
        TaskScheduler.later(12, () -> {
            if (!isAlive() || !target.isAlive()) return;
            Vec3 look = target.getLookAngle();
            Vec3 behind = target.position().subtract(new Vec3(look.x, 0, look.z).normalize().scale(1.8));
            Fx.burst(l, ParticleTypes.SQUID_INK, position().add(0, 1.5, 0), 40, 0.5, 0.05);
            teleportTo(behind.x, target.getY(), behind.z);
            Fx.burst(l, ParticleTypes.SQUID_INK, position().add(0, 1.5, 0), 40, 0.5, 0.05);
            for (LivingEntity e : victims(3.5)) {
                strikeDamage(e, 16f);
                e.knockback(1.2, getX() - e.getX(), getZ() - e.getZ());
            }
            l.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1, target.getZ(), 3, 0.5, 0.3, 0.5, 0);
            playSound(SoundEvents.RAVAGER_ATTACK, 2f, 0.6f);
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2f, 0.4f);
        });
    }

    /** Pluie de météores (phase 3). */
    private void meteorRain(ServerLevel l, LivingEntity target) {
        playSound(SoundEvents.WITHER_SPAWN, 1f, 1.5f);
        TaskScheduler.repeat(80, k -> {
            if (!isAlive() || !target.isAlive() || k % 3 != 0) return;
            Vec3 spot = Fx.groundAt(l, target.position().add(random.nextGaussian() * 6, 1, random.nextGaussian() * 6));
            Vec3 from = spot.add(random.nextGaussian() * 3, 18, random.nextGaussian() * 3);
            Fx.ring(l, BLOOD, spot.add(0, 0.1, 0), 1.3, 14);
            MagicProjectileEntity m = fireAt(ProjectileKind.DEMON_ORB, 10f, 0.9f, from, spot, 1.3f);
            m.setSecondsOnFire(10);
        });
    }

    @Override
    protected void onPhaseChange(ServerLevel l, int phase) {
        if (phase == 2) {
            playSound(SoundEvents.WITHER_SPAWN, 3f, 0.6f);
            for (ServerPlayer p : nearbyPlayers(32)) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
        } else if (phase == 3) {
            playSound(SoundEvents.ENDER_DRAGON_GROWL, 3f, 0.5f);
            for (int i = 0; i < 4; i++) {
                LightningBolt b = EntityType.LIGHTNING_BOLT.create(l);
                if (b == null) continue;
                double a = i * Math.PI / 2;
                b.moveTo(position().add(Math.cos(a) * 5, 0, Math.sin(a) * 5));
                b.setVisualOnly(true);
                l.addFreshEntity(b);
            }
            AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.36);
        }
    }

    @Override
    protected void transformEffects(ServerLevel l, int k) {
        super.transformEffects(l, k);
        Fx.pillar(l, ParticleTypes.SQUID_INK, position(), 6, 0.6, 1);
        Fx.ring(l, BLOOD, position().add(0, 0.1, 0), 4 - k * 0.07, 30);
    }

    /** La faiblesse des démons : l'anti-magie. */
    @Override
    protected float modifyDamage(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Player p && MagicData.hasGrimoire(p) && MagicData.element(p) == MagicElement.ANTI) return amount * 2.0f;
        if (source.getDirectEntity() instanceof MagicProjectileEntity) return amount * 0.5f;
        return amount;
    }

    @Override
    protected void dropBossLoot() {
        spawnAtLocation(new ItemStack(ModItems.DEMON_FRAGMENT.get()));
        spawnAtLocation(new ItemStack(ModItems.ANCIENT_PAGE.get(), 3));
        spawnAtLocation(new ItemStack(ModItems.MANA_CRYSTAL.get(), 4));
        spawnAtLocation(new ItemStack(Items.NETHER_STAR));
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WARDEN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WITHER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }
}
