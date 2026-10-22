package com.blackclovermod.magic;

import com.blackclovermod.entity.MagicProjectileEntity;
import com.blackclovermod.entity.ProjectileKind;
import com.blackclovermod.util.Fx;
import com.blackclovermod.util.TaskScheduler;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * TOUS LES SORTS DU MOD. Chaque attribut a 3 pages (débloquées aux niveaux 1, 3 et 6 du grimoire).
 * Pour ajouter un sort : appelle reg(element, "id", coutMana, rechargeEnTicks, (joueur, puissance) -> { ... }).
 * N'oublie pas d'ajouter "spell.blackclover.id" dans les fichiers de langue !
 */
public final class Spells {
    private static final Map<MagicElement, List<Spell>> MAP = new EnumMap<>(MagicElement.class);

    public static List<Spell> of(MagicElement e) {
        return MAP.getOrDefault(e, List.of());
    }

    private static void reg(MagicElement e, String id, int cost, int cooldown, Spell.Action action) {
        MAP.computeIfAbsent(e, k -> new ArrayList<>()).add(new Spell(id, e, cost, cooldown, action));
    }

    static {
        // =====================================================================
        // FEU
        // =====================================================================
        reg(MagicElement.FIRE, "fire_ball", 15, 20, (p, pow) -> {
            shoot(p, ProjectileKind.FIREBALL, 8 * pow, 1.6f, 0.45f);
            sound(p, SoundEvents.BLAZE_SHOOT, 1f, 1f);
            return true;
        });
        reg(MagicElement.FIRE, "flame_lion", 35, 80, (p, pow) -> {
            shoot(p, ProjectileKind.FLAME_LION, 14 * pow, 1.1f, 1.3f);
            sound(p, SoundEvents.RAVAGER_ROAR, 1f, 1.6f);
            sound(p, SoundEvents.FIRECHARGE_USE, 1f, 0.7f);
            return true;
        });
        reg(MagicElement.FIRE, "solar_burst", 70, 300, (p, pow) -> {
            ServerLevel l = p.serverLevel();
            sound(p, SoundEvents.BEACON_POWER_SELECT, 1f, 0.6f);
            TaskScheduler.repeat(30, t -> {
                if (!p.isAlive()) return;
                Vec3 sun = p.position().add(0, 3.2, 0);
                double r = 0.4 + t * 0.06;
                Fx.sphere(l, ParticleTypes.FLAME, sun, r, 14);
                Fx.converge(l, ParticleTypes.ENCHANT, sun, 4, 2.5);
                if (t == 29) {
                    Vec3 c = p.position();
                    for (LivingEntity e : enemies(p, c, 8)) {
                        e.invulnerableTime = 0;
                        hit(p, e, 20 * pow);
                        e.setSecondsOnFire(8);
                    }
                    l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 2, 1, 0.5, 1, 0);
                    Fx.burst(l, ParticleTypes.FLAME, c.add(0, 1, 0), 300, 1.5, 0.5);
                    Fx.burst(l, ParticleTypes.LAVA, c.add(0, 1, 0), 40, 2, 0);
                    sound(p, SoundEvents.GENERIC_EXPLODE, 2f, 0.8f);
                    TaskScheduler.repeat(10, k -> Fx.ringOut(l, ParticleTypes.FLAME, c.add(0, 0.2, 0), 1 + k * 0.8, 24 + k * 6, 0.05));
                }
            });
            return true;
        });

        // =====================================================================
        // EAU
        // =====================================================================
        reg(MagicElement.WATER, "water_bullet", 10, 10, (p, pow) -> {
            shoot(p, ProjectileKind.WATER_BULLET, 6 * pow, 2.0f, 0.35f);
            sound(p, SoundEvents.PLAYER_SPLASH, 0.6f, 1.6f);
            return true;
        });
        reg(MagicElement.WATER, "healing_bubble", 30, 200, (p, pow) -> {
            ServerLevel l = p.serverLevel();
            p.clearFire();
            p.heal(6 * pow);
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
            for (Player ally : l.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(6), a -> a != p)) {
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0));
                ally.clearFire();
            }
            TaskScheduler.repeat(40, t -> {
                if (!p.isAlive()) return;
                Fx.sphere(l, MagicElement.WATER.dust(1.2f), p.position().add(0, 1, 0), 1.6, 12);
                if (t % 4 == 0) Fx.sphere(l, ParticleTypes.SPLASH, p.position().add(0, 1, 0), 1.6, 10);
            });
            sound(p, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.5f, 1f);
            return true;
        });
        reg(MagicElement.WATER, "water_dragon", 60, 200, (p, pow) -> {
            shoot(p, ProjectileKind.WATER_DRAGON, 16 * pow, 1.1f, 1.5f);
            sound(p, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.2f, 0.6f);
            sound(p, SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 1.7f);
            return true;
        });

        // =====================================================================
        // VENT
        // =====================================================================
        reg(MagicElement.WIND, "wind_blade", 10, 8, (p, pow) -> {
            shoot(p, ProjectileKind.WIND_BLADE, 7 * pow, 2.6f, 0.7f);
            sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.6f);
            return true;
        });
        reg(MagicElement.WIND, "tornado", 35, 160, (p, pow) -> {
            ServerLevel l = p.serverLevel();
            Vec3 at = Fx.groundAt(l, targetPoint(p, 20));
            sound(p, SoundEvents.ELYTRA_FLYING, 1.5f, 0.6f);
            TaskScheduler.repeat(80, t -> {
                for (int h = 0; h < 14; h++) {
                    double y = h * 0.5, r = 0.4 + h * 0.22, a = t * 0.7 + h * 0.9;
                    l.sendParticles(ParticleTypes.CLOUD, at.x + Math.cos(a) * r, at.y + y, at.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                    l.sendParticles(MagicElement.WIND.dust(1.2f), at.x + Math.cos(a + Math.PI) * r, at.y + y, at.z + Math.sin(a + Math.PI) * r, 1, 0, 0, 0, 0);
                }
                AABB box = new AABB(at.x - 3.5, at.y - 1, at.z - 3.5, at.x + 3.5, at.y + 7, at.z + 3.5);
                for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, box, e -> isEnemy(p, e))) {
                    Vec3 toC = new Vec3(at.x - e.getX(), 0, at.z - e.getZ());
                    Vec3 spin = new Vec3(-toC.z, 0, toC.x).normalize().scale(0.25);
                    e.setDeltaMovement(toC.scale(0.12).add(spin).add(0, 0.32, 0));
                    e.hurtMarked = true;
                    if (t % 10 == 0) hit(p, e, 3 * pow);
                }
            });
            return true;
        });
        reg(MagicElement.WIND, "storm_dash", 25, 60, (p, pow) -> {
            ServerLevel l = p.serverLevel();
            Vec3 look = p.getLookAngle();
            p.setDeltaMovement(look.scale(2.2).add(0, 0.25, 0));
            p.hurtMarked = true;
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
            Set<Integer> already = new HashSet<>();
            TaskScheduler.repeat(12, t -> {
                p.fallDistance = 0;
                Fx.burst(l, ParticleTypes.CLOUD, p.position().add(0, 1, 0), 6, 0.3, 0.02);
                Fx.burst(l, MagicElement.WIND.dust(1.5f), p.position().add(0, 1, 0), 6, 0.4, 0);
                for (LivingEntity e : enemies(p, p.position(), 2.5)) {
                    if (already.add(e.getId())) {
                        hit(p, e, 8 * pow);
                        e.knockback(1.0, -look.x, -look.z);
                    }
                }
            });
            sound(p, SoundEvents.ELYTRA_FLYING, 0.8f, 1.8f);
            return true;
        });

        // =====================================================================
        // FOUDRE
        // =====================================================================
        reg(MagicElement.LIGHTNING, "thunder_spear", 15, 15, (p, pow) -> {
            shoot(p, ProjectileKind.THUNDER, 9 * pow, 3.2f, 0.35f);
            sound(p, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6f, 1.8f);
            return true;
        });
        reg(MagicElement.LIGHTNING, "thunder_gauntlets", 30, 400, (p, pow) -> {
            ServerLevel l = p.serverLevel();
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 2));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 300, 1));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0));
            Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, p.position().add(0, 1, 0), 80, 0.6, 0.4);
            TaskScheduler.repeat(300, t -> {
                if (!p.isAlive() || t % 2 != 0) return;
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, p.position().add(0, 0.9, 0), 3, 0.4, 0.05);
                if (t % 10 == 0) Fx.burst(l, MagicElement.LIGHTNING.dust(1f), p.position().add(0, 0.1, 0), 4, 0.3, 0);
            });
            sound(p, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5f, 1.8f);
            return true;
        });
        reg(MagicElement.LIGHTNING, "thunder_judgment", 70, 300, (p, pow) -> {
            List<Vec3> spots = new ArrayList<>();
            for (LivingEntity e : enemies(p, p.position(), 14)) {
                spots.add(e.position());
                if (spots.size() >= 6) break;
            }
            while (spots.size() < 4) {
                double a = p.getRandom().nextDouble() * Math.PI * 2, r = 4 + p.getRandom().nextDouble() * 6;
                spots.add(Fx.groundAt(p.level(), p.position().add(Math.cos(a) * r, 3, Math.sin(a) * r)));
            }
            for (int i = 0; i < spots.size(); i++) {
                Vec3 s = spots.get(i);
                TaskScheduler.later(i * 6, () -> strike(p, s, 14 * pow));
            }
            return true;
        });

        // =====================================================================
        // TÉNÈBRES
        // =====================================================================
        reg(MagicElement.DARKNESS, "dark_blade", 20, 25, (p, pow) -> {
            shoot(p, ProjectileKind.DARK_SLASH, 11 * pow, 1.8f, 1.0f);
            sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.5f);
            return true;
        });
        reg(MagicElement.DARKNESS, "black_hole", 45, 240, (p, pow) -> {
            ServerLevel l = p.serverLevel();
            Vec3 at = targetPoint(p, 18).add(0, 1.5, 0);
            sound(p, SoundEvents.WITHER_AMBIENT, 0.6f, 0.5f);
            TaskScheduler.repeat(80, t -> {
                for (int k = 0; k < 12; k++) {
                    double a = t * 0.3 + k * Math.PI * 2 / 12, r = 4 - (t % 20) * 0.18;
                    double px = at.x + Math.cos(a) * r, pz = at.z + Math.sin(a) * r;
                    l.sendParticles(ParticleTypes.SQUID_INK, px, at.y, pz, 0, at.x - px, 0, at.z - pz, 0.08);
                }
                Fx.burst(l, ParticleTypes.REVERSE_PORTAL, at, 6, 0.3, 0.05);
                Fx.burst(l, MagicElement.DARKNESS.dust(2.5f), at, 3, 0.2, 0);
                for (LivingEntity e : enemies(p, at, 9)) {
                    Vec3 v = at.subtract(e.position()).normalize().scale(0.25);
                    e.setDeltaMovement(e.getDeltaMovement().add(v));
                    e.hurtMarked = true;
                    if (t % 10 == 0 && e.distanceToSqr(at) < 4) hit(p, e, 4 * pow);
                }
                // avale les projectiles ennemis
                for (Projectile pr : l.getEntitiesOfClass(Projectile.class, new AABB(at, at).inflate(4), pr -> pr.getOwner() != p)) pr.discard();
                if (t % 20 == 0) Fx.sound(l, at, SoundEvents.WARDEN_HEARTBEAT, 1f, 0.6f);
            });
            return true;
        });
        reg(MagicElement.DARKNESS, "dimension_slash", 80, 400, (p, pow) -> {
            ServerLevel l = p.serverLevel();
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 35, 3, false, false));
            sound(p, SoundEvents.WARDEN_HEARTBEAT, 1f, 1.5f);
            TaskScheduler.repeat(35, t -> {
                if (!p.isAlive()) return;
                Fx.converge(l, ParticleTypes.SQUID_INK, p.position().add(0, 1, 0), 6, 2.5 - t * 0.05);
                Fx.converge(l, MagicElement.DARKNESS.dust(1.5f), p.position().add(0, 1, 0), 4, 2);
                if (t == 34) {
                    Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
                    for (double d = 1; d < 30; d += 0.5) {
                        Vec3 q = eye.add(look.scale(d));
                        l.sendParticles(MagicElement.DARKNESS.dust(2.5f), q.x, q.y, q.z, 3, 0.15, 0.6, 0.15, 0);
                        l.sendParticles(ParticleTypes.SQUID_INK, q.x, q.y, q.z, 1, 0.1, 0.4, 0.1, 0);
                        if (((int) d) % 3 == 0) l.sendParticles(ParticleTypes.SWEEP_ATTACK, q.x, q.y, q.z, 1, 0, 0, 0, 0);
                    }
                    for (LivingEntity e : enemiesOnLine(p, eye, look, 30, 2)) {
                        e.invulnerableTime = 0;
                        e.hurt(p.damageSources().indirectMagic(p, p), 25 * pow);
                    }
                    sound(p, SoundEvents.WARDEN_SONIC_BOOM, 1.2f, 0.7f);
                    sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.4f);
                }
            });
            return true;
        });

        // =====================================================================
        // LUMIÈRE
        // =====================================================================
        reg(MagicElement.LIGHT, "light_swords", 20, 30, (p, pow) -> {
            Vec3 target = targetPoint(p, 32);
            Vec3 look = p.getLookAngle();
            Vec3 right = look.cross(Fx.UP);
            right = right.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : right.normalize();
            for (int i = -2; i <= 2; i++) {
                final Vec3 origin = p.getEyePosition().add(right.scale(i * 0.7)).add(0, 0.8 - Math.abs(i) * 0.15, 0).subtract(look.scale(0.3));
                final int idx = i;
                TaskScheduler.repeat(4, k -> p.serverLevel().sendParticles(ParticleTypes.END_ROD, origin.x, origin.y, origin.z, 2, 0.05, 0.05, 0.05, 0));
                TaskScheduler.later((i + 2) * 2 + 4, () -> {
                    if (!p.isAlive()) return;
                    Vec3 dir = target.subtract(origin);
                    MagicProjectileEntity m = new MagicProjectileEntity(p.level(), p, ProjectileKind.LIGHT_SWORD, 5 * pow, 0.35f);
                    m.setPos(origin.x, origin.y, origin.z);
                    m.shoot(dir.x, dir.y, dir.z, 3.0f, 0f);
                    p.level().addFreshEntity(m);
                    sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.4f + idx * 0.1f);
                });
            }
            return true;
        });
        reg(MagicElement.LIGHT, "light_speed", 20, 40, (p, pow) -> {
            Vec3 start = p.position();
            Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
            BlockHitResult r = p.level().clip(new ClipContext(eye, eye.add(look.scale(16)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            Vec3 dest = r.getLocation().subtract(look.scale(0.7));
            Vec3 feet = new Vec3(dest.x, dest.y - p.getEyeHeight() + 0.2, dest.z);
            for (double dy = 0; dy <= 1.5; dy += 0.5) {
                Vec3 cand = feet.add(0, dy, 0);
                if (p.level().noCollision(p, p.getBoundingBox().move(cand.subtract(start)))) {
                    ServerLevel l = p.serverLevel();
                    Fx.line(l, ParticleTypes.END_ROD, start.add(0, 1, 0), cand.add(0, 1, 0), 0.3);
                    Fx.burst(l, ParticleTypes.FLASH, start.add(0, 1, 0), 1, 0, 0);
                    p.teleportTo(cand.x, cand.y, cand.z);
                    p.fallDistance = 0;
                    Fx.burst(l, ParticleTypes.END_ROD, cand.add(0, 1, 0), 30, 0.4, 0.1);
                    sound(p, SoundEvents.ENDERMAN_TELEPORT, 1f, 1.8f);
                    return true;
                }
            }
            return false;
        });
        reg(MagicElement.LIGHT, "light_judgment", 75, 300, (p, pow) -> {
            ServerLevel l = p.serverLevel();
            List<Vec3> spots = new ArrayList<>();
            for (LivingEntity e : enemies(p, p.position(), 16)) {
                spots.add(Fx.groundAt(l, e.position().add(0, 1, 0)));
                if (spots.size() >= 8) break;
            }
            while (spots.size() < 6) {
                double a = p.getRandom().nextDouble() * Math.PI * 2, r = 3 + p.getRandom().nextDouble() * 7;
                spots.add(Fx.groundAt(l, p.position().add(Math.cos(a) * r, 3, Math.sin(a) * r)));
            }
            sound(p, SoundEvents.BEACON_ACTIVATE, 1.5f, 1.6f);
            for (int i = 0; i < spots.size(); i++) {
                Vec3 s = spots.get(i);
                TaskScheduler.later(i * 4, () -> TaskScheduler.repeat(20, t -> {
                    if (t < 15) {
                        Fx.ring(l, MagicElement.LIGHT.dust(1.4f), s.add(0, 0.1, 0), 1.6, 18);
                        Fx.ring(l, ParticleTypes.END_ROD, s.add(0, 0.1, 0), 1.6 * (1 - t / 15.0), 8);
                    } else if (t == 15) {
                        Fx.pillar(l, ParticleTypes.END_ROD, s, 18, 0.35, 4);
                        Fx.burst(l, ParticleTypes.FLASH, s.add(0, 1, 0), 1, 0, 0);
                        for (LivingEntity e : enemies(p, s, 1.8)) {
                            e.invulnerableTime = 0;
                            hit(p, e, 10 * pow);
                        }
                        Fx.sound(l, s, SoundEvents.AMETHYST_BLOCK_CHIME, 2f, 0.6f);
                    }
                }));
            }
            return true;
        });

        // =====================================================================
        // ÉPINES / PLANTES
        // =====================================================================
        reg(MagicElement.THORN, "thorn_bind", 15, 20, (p, pow) -> {
            shoot(p, ProjectileKind.THORN, 6 * pow, 1.8f, 0.35f);
            sound(p, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 1f, 0.8f);
            return true;
        });
        reg(MagicElement.THORN, "healing_bloom", 30, 240, (p, pow) -> {
            ServerLevel l = p.serverLevel();
            p.heal(8 * pow);
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            for (Player ally : l.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(6), a -> a != p)) {
                ally.heal(4 * pow);
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            }
            TaskScheduler.repeat(30, t -> {
                Fx.ring(l, ParticleTypes.CHERRY_LEAVES, p.position().add(0, 0.2 + t * 0.06, 0), 1.2 + t * 0.05, 10);
                Fx.ring(l, ParticleTypes.HAPPY_VILLAGER, p.position().add(0, 0.1, 0), 2.5, 8);
            });
            sound(p, SoundEvents.BONE_MEAL_USE, 1.5f, 0.8f);
            return true;
        });
        reg(MagicElement.THORN, "thorn_forest", 60, 300, (p, pow) -> {
            ServerLevel l = p.serverLevel();
            Vec3 c = p.position();
            BlockParticleOption leaves = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_LEAVES.defaultBlockState());
            for (int wave = 0; wave < 3; wave++) {
                final double radius = 3 + wave * 2;
                TaskScheduler.later(wave * 10, () -> {
                    int pts = (int) (radius * 7);
                    for (int i = 0; i < pts; i++) {
                        double a = Math.PI * 2 * i / pts;
                        Vec3 s = Fx.groundAt(l, c.add(Math.cos(a) * radius, 2, Math.sin(a) * radius));
                        for (double y = 0; y < 2.2; y += 0.3) {
                            l.sendParticles(MagicElement.THORN.dust(1.3f), s.x, s.y + y, s.z, 1, 0.05, 0, 0.05, 0);
                        }
                        l.sendParticles(leaves, s.x, s.y + 0.5, s.z, 3, 0.2, 0.4, 0.2, 0.1);
                    }
                    for (LivingEntity e : enemies(p, c, radius + 1)) {
                        double d = Math.sqrt(e.distanceToSqr(c.x, e.getY(), c.z));
                        if (Math.abs(d - radius) < 1.5) {
                            e.invulnerableTime = 0;
                            hit(p, e, 9 * pow);
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 5));
                        }
                    }
                    Fx.sound(l, c, SoundEvents.GRASS_BREAK, 2f, 0.5f);
                });
            }
            return true;
        });

        // =====================================================================
        // ANTI-MAGIE (grimoire à 5 feuilles uniquement)
        // =====================================================================
        reg(MagicElement.ANTI, "demon_slayer", 15, 15, (p, pow) -> {
            ServerLevel l = p.serverLevel();
            Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
            for (LivingEntity e : enemies(p, p.position(), 4.5)) {
                Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye).normalize();
                if (to.dot(look) > 0.35) {
                    e.invulnerableTime = 0;
                    hit(p, e, 12 * pow);
                    if (!(e instanceof Player)) e.removeAllEffects();
                    e.knockback(0.8, -look.x, -look.z);
                }
            }
            cancelMagicAround(p, p.position(), 5);
            for (int a = -60; a <= 60; a += 6) {
                Vec3 d = look.yRot((float) Math.toRadians(a));
                Vec3 q = eye.add(d.scale(2.8)).add(0, -0.3, 0);
                l.sendParticles(MagicElement.ANTI.dust(2f), q.x, q.y, q.z, 2, 0.05, 0.05, 0.05, 0);
                l.sendParticles(ParticleTypes.LARGE_SMOKE, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0.01);
            }
            l.sendParticles(ParticleTypes.SWEEP_ATTACK, eye.x + look.x * 2, eye.y - 0.3 + look.y * 2, eye.z + look.z * 2, 1, 0, 0, 0, 0);
            p.swing(InteractionHand.MAIN_HAND, true);
            sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.5f);
            sound(p, SoundEvents.ANVIL_LAND, 0.3f, 1.6f);
            return true;
        });
        reg(MagicElement.ANTI, "anti_slash", 25, 40, (p, pow) -> {
            shoot(p, ProjectileKind.ANTI_SLASH, 14 * pow, 2.0f, 1.1f);
            p.swing(InteractionHand.MAIN_HAND, true);
            sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.4f);
            return true;
        });
        reg(MagicElement.ANTI, "black_form", 60, 600, (p, pow) -> {
            ServerLevel l = p.serverLevel();
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 0));
            p.addEffect(new MobEffectInstance(MobEffects.JUMP, 400, 1));
            for (LivingEntity e : enemies(p, p.position(), 6)) e.knockback(1.5, p.getX() - e.getX(), p.getZ() - e.getZ());
            Fx.burst(l, ParticleTypes.LARGE_SMOKE, p.position().add(0, 1, 0), 150, 0.5, 0.3);
            Fx.ringOut(l, MagicElement.ANTI.dust(2.5f), p.position().add(0, 0.3, 0), 1, 40, 0.6);
            TaskScheduler.repeat(400, t -> {
                if (!p.isAlive() || t % 2 != 0) return;
                Fx.burst(l, ParticleTypes.LARGE_SMOKE, p.position().add(0, 1, 0), 2, 0.3, 0.01);
                Fx.burst(l, MagicElement.ANTI.dust(1.5f), p.position().add(0, 1.2, 0), 3, 0.4, 0);
            });
            sound(p, SoundEvents.WITHER_SPAWN, 0.5f, 1.4f);
            return true;
        });
    }

    // =====================================================================
    // Fonctions utilitaires partagées par les sorts
    // =====================================================================
    static boolean isEnemy(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand) return false;
        if (e.isAlliedTo(p)) return false;
        return !(e instanceof TamableAnimal t && t.isOwnedBy(p));
    }

    static List<LivingEntity> enemies(ServerPlayer p, Vec3 c, double r) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, AABB.ofSize(c, r * 2, r * 2, r * 2),
                e -> isEnemy(p, e) && e.distanceToSqr(c) <= r * r);
    }

    static List<LivingEntity> enemiesOnLine(ServerPlayer p, Vec3 from, Vec3 dir, double len, double width) {
        Vec3 to = from.add(dir.scale(len));
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(width),
                e -> isEnemy(p, e) && distToSegment(e.getBoundingBox().getCenter(), from, to) <= width + e.getBbWidth() / 2);
    }

    static double distToSegment(Vec3 pt, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double t = Mth.clamp(pt.subtract(a).dot(ab) / ab.lengthSqr(), 0, 1);
        return pt.distanceTo(a.add(ab.scale(t)));
    }

    static void hit(ServerPlayer p, LivingEntity e, float dmg) {
        e.hurt(p.damageSources().playerAttack(p), dmg);
    }

    static MagicProjectileEntity shoot(ServerPlayer p, ProjectileKind kind, float dmg, float speed, float size) {
        MagicProjectileEntity m = new MagicProjectileEntity(p.level(), p, kind, dmg, size);
        Vec3 look = p.getLookAngle();
        m.setPos(p.getX() + look.x * 0.8, p.getEyeY() - 0.2 + look.y * 0.8, p.getZ() + look.z * 0.8);
        m.shoot(look.x, look.y, look.z, speed, 0f);
        p.level().addFreshEntity(m);
        p.swing(InteractionHand.MAIN_HAND, true);
        return m;
    }

    static Vec3 targetPoint(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult r = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return r.getType() == HitResult.Type.MISS ? end : r.getLocation();
    }

    static void strike(ServerPlayer p, Vec3 pos, float dmg) {
        ServerLevel l = p.serverLevel();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(l);
        if (bolt != null) {
            bolt.moveTo(pos);
            bolt.setVisualOnly(true);
            l.addFreshEntity(bolt);
        }
        for (LivingEntity e : enemies(p, pos, 2.5)) {
            e.invulnerableTime = 0;
            hit(p, e, dmg);
        }
        Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos.add(0, 0.5, 0), 40, 0.8, 0.3);
    }

    static void cancelMagicAround(ServerPlayer p, Vec3 c, double r) {
        for (MagicProjectileEntity m : p.serverLevel().getEntitiesOfClass(MagicProjectileEntity.class, AABB.ofSize(c, r * 2, r * 2, r * 2), m -> m.getOwner() != p)) {
            p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY(), m.getZ(), 15, 0.3, 0.3, 0.3, 0.02);
            m.discard();
        }
    }

    static void sound(ServerPlayer p, SoundEvent s, float v, float pitch) {
        Fx.sound(p.level(), p.position(), s, v, pitch);
    }

    private Spells() {}
}
