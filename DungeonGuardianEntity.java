package com.blackclovermod.entity.boss;

import com.blackclovermod.entity.MagicProjectileEntity;
import com.blackclovermod.entity.ProjectileKind;
import com.blackclovermod.magic.MagicElement;
import com.blackclovermod.registry.ModItems;
import com.blackclovermod.util.Fx;
import com.blackclovermod.util.TaskScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/**
 * BOSS 1 : LE GARDIEN DU DONJON — un golem de pierre gravé de runes.
 * Attaques : Frappe sismique (onde de choc), Lancer de rochers.
 * Phase 2 (50 %) : Gravité écrasante + invoque des Sentinelles.
 */
public class DungeonGuardianEntity extends MagicBossEntity {
    private static final DustParticleOptions RUNE = new DustParticleOptions(new Vector3f(0.3f, 0.9f, 1f), 1f);
    private int summonCooldown = 0;

    public DungeonGuardianEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.ATTACK_DAMAGE, 14)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public MagicElement bossElement() { return MagicElement.EARTH; }
    @Override public String bossId() { return "dungeon_guardian"; }
    @Override protected int maxPhase() { return 2; }

    @Override
    protected void bossTick(ServerLevel l, @Nullable LivingEntity target) {
        if (summonCooldown > 0) summonCooldown--;
        if (tickCount % 5 == 0) l.sendParticles(RUNE, getX(), getY() + getBbHeight() * 0.6, getZ(), 2, 0.4, 0.8, 0.4, 0);
    }

    @Override
    protected int performAttack(ServerLevel l, LivingEntity target) {
        boolean enraged = phase() >= 2;
        if (enraged && summonCooldown <= 0 && random.nextFloat() < 0.35f) {
            summonSentinels(l, target);
            summonCooldown = 400;
            return 40;
        }
        if (distanceTo(target) < 7) {
            groundSlam(l);
            return enraged ? 45 : 70;
        }
        throwRocks(l, target, enraged ? 3 : 1);
        return enraged ? 40 : 60;
    }

    /** Frappe sismique : le golem saute puis écrase le sol, une onde de choc se propage. */
    private void groundSlam(ServerLevel l) {
        setDeltaMovement(getDeltaMovement().add(0, 0.7, 0));
        hurtMarked = true;
        playSound(SoundEvents.IRON_GOLEM_ATTACK, 2f, 0.5f);
        Fx.ring(l, RUNE, position().add(0, 0.1, 0), 6, 40); // avertissement
        TaskScheduler.later(14, () -> {
            if (!isAlive()) return;
            Vec3 c = position();
            BlockState under = level().getBlockState(BlockPos.containing(c).below());
            BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, under.isAir() ? Blocks.STONE.defaultBlockState() : under);
            playSound(SoundEvents.GENERIC_EXPLODE, 2f, 0.6f);
            playSound(SoundEvents.ANVIL_LAND, 1.5f, 0.5f);
            l.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 3, 1, 0.2, 1, 0);
            Set<Integer> hit = new HashSet<>();
            float dmg = phase() >= 2 ? 14f : 10f;
            TaskScheduler.repeat(10, k -> {
                double r = 1 + k;
                Fx.ring(l, debris, c.add(0, 0.2, 0), r, (int) (r * 10));
                Fx.ring(l, ParticleTypes.CLOUD, c.add(0, 0.1, 0), r, (int) (r * 4));
                for (LivingEntity e : victimsAt(c, r + 1.5)) {
                    double d = Math.sqrt(e.distanceToSqr(c.x, e.getY(), c.z));
                    if (Math.abs(d - r) < 1.2 && Math.abs(e.getY() - c.y) < 2.0 && hit.add(e.getId())) {
                        strikeDamage(e, dmg);
                        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.8, 0));
                        e.hurtMarked = true;
                    }
                }
            });
        });
    }

    /** Lance des rochers en cloche (trajectoire calculée pour tomber sur la cible). */
    private void throwRocks(ServerLevel l, LivingEntity target, int count) {
        for (int i = 0; i < count; i++) {
            TaskScheduler.later(i * 8, () -> {
                if (!isAlive() || !target.isAlive()) return;
                Vec3 from = position().add(0, getBbHeight() + 0.5, 0);
                Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
                double dx = to.x - from.x, dz = to.z - from.z, dy = to.y - from.y;
                double hd = Math.sqrt(dx * dx + dz * dz);
                double time = Math.max(6, hd / 1.2);
                double g = ProjectileKind.ROCK.gravity;
                double vh = hd / time;
                double vy = (dy + 0.5 * g * time * time) / time;
                double vx = hd > 1e-3 ? dx / hd * vh : 0, vz = hd > 1e-3 ? dz / hd * vh : 0;
                float speed = (float) Math.sqrt(vh * vh + vy * vy);
                MagicProjectileEntity m = new MagicProjectileEntity(l, this, ProjectileKind.ROCK, 9f, 0.9f);
                m.setPos(from.x, from.y, from.z);
                m.shoot(vx, vy, vz, speed, 1f);
                l.addFreshEntity(m);
                l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()), from.x, from.y, from.z, 20, 0.4, 0.4, 0.4, 0.1);
                playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.5f, 0.7f);
            });
        }
    }

    private void summonSentinels(ServerLevel l, LivingEntity target) {
        playSound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 2f, 0.5f);
        for (int i = 0; i < 3; i++) {
            Zombie z = EntityType.ZOMBIE.create(l);
            if (z == null) continue;
            double a = i * 2.094;
            z.moveTo(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3, random.nextFloat() * 360f, 0f);
            z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
            z.setDropChance(EquipmentSlot.HEAD, 0f);
            z.setDropChance(EquipmentSlot.MAINHAND, 0f);
            z.setCustomName(Component.translatable("entity.blackclover.sentinel"));
            z.setTarget(target);
            l.addFreshEntity(z);
            Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE_BRICKS.defaultBlockState()), z.position().add(0, 0.5, 0), 40, 0.4, 0.1);
            Fx.burst(l, RUNE, z.position().add(0, 1, 0), 20, 0.4, 0);
        }
    }

    @Override
    protected void onPhaseChange(ServerLevel l, int phase) {
        playSound(SoundEvents.RAVAGER_ROAR, 3f, 0.5f);
        for (ServerPlayer p : nearbyPlayers(16)) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            l.sendParticles(RUNE, p.getX(), p.getY() + 2.5, p.getZ(), 30, 0.5, 0.2, 0.5, 0);
        }
    }

    @Override
    protected void dropBossLoot() {
        spawnAtLocation(new ItemStack(ModItems.MANA_CRYSTAL.get(), 2 + random.nextInt(2)));
        spawnAtLocation(new ItemStack(ModItems.ANCIENT_PAGE.get()));
    }

    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }
    @Override protected void playStepSound(BlockPos pos, BlockState state) { playSound(SoundEvents.IRON_GOLEM_STEP, 1f, 0.6f); }
}
