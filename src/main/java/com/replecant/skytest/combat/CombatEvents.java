package com.replecant.skytest.combat;

import com.replecant.skytest.SkyTest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.replecant.skytest.network.HitstopPayload;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Серверная часть «физичного» боя:
 * <ul>
 *   <li>комбо из трёх ударов — третий (добивающий) сильнее, дальше отбрасывает и пробивает стойку;</li>
 *   <li>парирование — ПКМ мечом за мгновение до удара врага: урон отменяется, враг оглушён,
 *       а следующие удары по нему сильнее (рипост);</li>
 *   <li>хитстоп — цель удара на мгновение замирает, затем её отбрасывает;</li>
 *   <li>рывок — несколько тиков неуязвимости к ударам существ;</li>
 *   <li>импульс — удар на бегу (в сторону цели) бьёт до 50% сильнее и отбрасывает дальше;</li>
 *   <li>оглушение — враг после удара на мгновение «проседает» и замедляется;</li>
 *   <li>удары по игроку отбрасывают его заметнее.</li>
 * </ul>
 */
@EventBusSubscriber(modid = SkyTest.MODID)
public final class CombatEvents {
    /** Скорость к цели (блоков/тик), при которой бонус импульса максимален. */
    private static final double FULL_MOMENTUM = 0.25;
    private static final float MAX_DAMAGE_BONUS = 0.5F;
    private static final int STAGGER_TICKS = 10;
    private static final float PLAYER_KNOCKBACK_SCALE = 1.35F;
    /** Кадры неуязвимости рывка (тики). */
    private static final int DASH_IFRAMES = 6;
    private static final int HITSTOP_TICKS = 3;
    private static final int HEAVY_HITSTOP_TICKS = 5;

    /** Окно между ударами, в которое комбо продолжается (тики). */
    private static final int COMBO_WINDOW = 28;
    /** Минимальный заряд удара, чтобы он продолжил комбо. */
    private static final float COMBO_MIN_CHARGE = 0.7F;
    private static final float[] COMBO_DAMAGE = {1.0F, 1.0F, 1.15F, 1.6F};
    private static final int FINISHER_HITSTOP_TICKS = 6;
    /** Окно парирования после ПКМ (тики) и перезарядка между попытками. */
    public static final int PARRY_WINDOW = 8;
    public static final int PARRY_COOLDOWN = 18;
    private static final int RIPOSTE_TICKS = 40;
    private static final float RIPOSTE_DAMAGE = 1.5F;

    /** Боевое состояние игрока. */
    private static final class Fighter {
        int combo;
        long lastSwing = -100;
        int step;
        long stepTick = -1;
        long parryUntil = -1;
        int riposteTarget = -1;
        long riposteUntil = -1;
    }

    private static final Map<UUID, Fighter> FIGHTERS = new HashMap<>();
    private static final Map<UUID, Long> DODGE_UNTIL = new HashMap<>();

    private static Fighter fighter(Player player) {
        return FIGHTERS.computeIfAbsent(player.getUUID(), id -> new Fighter());
    }

    /** Ступень комбо текущего удара игрока (1–3), 0 — если он сейчас не бьёт. */
    public static int comboStep(Player player) {
        Fighter f = FIGHTERS.get(player.getUUID());
        return f != null && f.stepTick == player.level().getGameTime() ? f.step : 0;
    }

    /** Каждый взмах: продолжить комбо или начать заново. */
    @SubscribeEvent
    static void onSwing(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Fighter f = fighter(player);
        long now = player.level().getGameTime();
        if (player.getAttackStrengthScale(0.5F) < COMBO_MIN_CHARGE) {
            f.combo = 0; // беспорядочное закликивание сбивает ритм
            f.step = 1;
        } else {
            f.combo = now - f.lastSwing <= COMBO_WINDOW && f.combo < 3 ? f.combo + 1 : 1;
            f.step = f.combo;
            if (f.combo == 3) {
                f.combo = 0;
            }
        }
        f.lastSwing = now;
        f.stepTick = now;
    }

    /** ПКМ мечом: открыть окно парирования. */
    public static void startParry(ServerPlayer player) {
        fighter(player).parryUntil = player.level().getGameTime() + PARRY_WINDOW;
        if (com.replecant.skytest.story.SmokeTour.ENABLED) {
            SkyTest.LOGGER.info("SKYTEST PARRY window t={}..{}", player.level().getGameTime(), fighter(player).parryUntil);
        }
    }

    private static boolean tryParry(ServerPlayer player, DamageSource source) {
        Fighter f = FIGHTERS.get(player.getUUID());
        long now = player.level().getGameTime();
        if (com.replecant.skytest.story.SmokeTour.ENABLED && source.getEntity() instanceof LivingEntity) {
            SkyTest.LOGGER.info("SKYTEST PARRY check t={} until={} direct={}", now, f == null ? -1 : f.parryUntil,
                    source.getDirectEntity() == source.getEntity());
        }
        if (f == null || f.parryUntil < now || !(source.getEntity() instanceof LivingEntity attacker)
                || source.getDirectEntity() != attacker) {
            return false;
        }
        Vec3 look = player.getLookAngle();
        Vec3 to = attacker.position().subtract(player.position());
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        if (flat.lengthSqr() > 1.0E-4 && new Vec3(look.x, 0.0, look.z).normalize().dot(flat.normalize()) < 0.2) {
            if (com.replecant.skytest.story.SmokeTour.ENABLED) {
                SkyTest.LOGGER.info("SKYTEST PARRY rejected: attacker not in front");
            }
            return false; // удар сбоку или со спины не парируется
        }
        f.parryUntil = -1;
        f.riposteTarget = attacker.getId();
        f.riposteUntil = now + RIPOSTE_TICKS;

        ServerLevel level = player.level();
        Vec3 mid = player.getEyePosition().add(attacker.getEyePosition()).scale(0.5);
        level.sendParticles(ParticleTypes.CRIT, mid.x, mid.y, mid.z, 24, 0.15, 0.15, 0.15, 0.6);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK.value(),
                SoundSource.PLAYERS, 1.2F, 1.5F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS, 0.25F, 2.0F);

        if (attacker instanceof ParryReactive reactive) {
            reactive.onParried(player);
        } else {
            attacker.knockback(0.9, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
            attacker.hurtMarked = true;
            if (attacker instanceof Mob mob) {
                mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 4, false, false));
                mob.getNavigation().stop();
            }
        }
        FROZEN_UNTIL.put(attacker.getUUID(), now + FINISHER_HITSTOP_TICKS);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(attacker,
                new HitstopPayload(attacker.getId(), player.getId(), FINISHER_HITSTOP_TICKS, 0, true));
        if (com.replecant.skytest.story.SmokeTour.ENABLED) {
            SkyTest.LOGGER.info("SKYTEST PARRY {} parried", attacker.getName().getString());
        }
        return true;
    }
    private static final Map<UUID, Long> FROZEN_UNTIL = new HashMap<>();

    /** Игрок сделал рывок: неуязвимость к ударам существ на несколько тиков. */
    public static void onDash(ServerPlayer player) {
        DODGE_UNTIL.put(player.getUUID(), player.level().getGameTime() + DASH_IFRAMES);
        player.resetFallDistance();
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 6, 0.2, 0.05, 0.2, 0.01);
        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_JUMP,
                SoundSource.PLAYERS, 0.5F, 1.5F);
    }

    private static boolean isDodging(LivingEntity entity) {
        Long until = DODGE_UNTIL.get(entity.getUUID());
        return until != null && entity.level().getGameTime() <= until;
    }

    /** Хитстоп: сервер пропускает тики цели, клиенты замораживают её анимацию. */
    private static void hitstop(LivingEntity target, Player attacker, int ticks, int combo) {
        FROZEN_UNTIL.put(target.getUUID(), target.level().getGameTime() + ticks);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,
                new HitstopPayload(target.getId(), attacker.getId(), ticks, combo, false));
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 10, 0.2, 0.25, 0.2, 0.35);
        }
    }

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Pre event) {
        if (FROZEN_UNTIL.isEmpty() || event.getEntity().level().isClientSide()) {
            return;
        }
        Long until = FROZEN_UNTIL.get(event.getEntity().getUUID());
        if (until == null) {
            return;
        }
        if (event.getEntity().level().getGameTime() < until && event.getEntity().isAlive()) {
            event.setCanceled(true);
        } else {
            FROZEN_UNTIL.remove(event.getEntity().getUUID());
        }
    }

    /** Насколько игрок «вложился» в удар: 0 — стоял, 1 — нёсся на цель. */
    public static float momentum(Player attacker, LivingEntity target) {
        Vec3 movement = attacker instanceof ServerPlayer serverPlayer
                ? serverPlayer.getKnownMovement() : attacker.getDeltaMovement();
        Vec3 toTarget = target.position().subtract(attacker.position());
        Vec3 direction = new Vec3(toTarget.x, 0.0, toTarget.z);
        if (direction.lengthSqr() < 1.0E-4) {
            return 0.0F;
        }
        double along = movement.x * direction.x + movement.z * direction.z;
        return (float) Mth.clamp(along / direction.length() / FULL_MOMENTUM, 0.0, 1.0);
    }

    private static boolean isPlayerMelee(LivingEntity target, net.minecraft.world.damagesource.DamageSource source) {
        return source.getEntity() instanceof Player attacker && source.getDirectEntity() == attacker && attacker != target;
    }

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // Рывок: удары существ проходят сквозь игрока.
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().getEntity() != null
                && event.getSource().getEntity() != player && isDodging(player)) {
            event.setCanceled(true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.8F, 1.6F);
            if (com.replecant.skytest.story.SmokeTour.ENABLED) {
                SkyTest.LOGGER.info("SKYTEST DODGE {} avoided by dash", event.getSource().getEntity().getName().getString());
            }
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && tryParry(player, event.getSource())) {
            event.setCanceled(true);
            return;
        }
        if (isPlayerMelee(event.getEntity(), event.getSource())) {
            Player attacker = (Player) event.getSource().getEntity();
            float bonus = momentum(attacker, event.getEntity());
            float multiplier = (1.0F + MAX_DAMAGE_BONUS * bonus) * COMBO_DAMAGE[comboStep(attacker)];
            Fighter f = fighter(attacker);
            boolean riposte = f.riposteTarget == event.getEntity().getId()
                    && attacker.level().getGameTime() <= f.riposteUntil;
            if (riposte) {
                multiplier *= RIPOSTE_DAMAGE;
            }
            event.setAmount(event.getAmount() * multiplier);
            if (com.replecant.skytest.story.SmokeTour.ENABLED) {
                SkyTest.LOGGER.info("SKYTEST COMBO step {} on {} x{}{} = {}", comboStep(attacker),
                        event.getEntity().getName().getString(), String.format("%.2f", multiplier),
                        riposte ? " (riposte)" : "", String.format("%.1f", event.getAmount()));
            }
        }
    }

    @SubscribeEvent
    static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (!isPlayerMelee(target, event.getSource()) || target.level().isClientSide()) {
            return;
        }
        Player attacker = (Player) event.getSource().getEntity();
        float bonus = momentum(attacker, target);
        // Дополнительный отброс от импульса, поверх ванильного.
        double dx = attacker.getX() - target.getX();
        double dz = attacker.getZ() - target.getZ();
        int step = comboStep(attacker);
        target.knockback(0.2 + 0.6 * bonus + (step == 3 ? 0.6 : 0.0), dx, dz);
        if (target instanceof Mob mob) {
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, STAGGER_TICKS, 3, false, false));
            mob.getNavigation().stop();
        }
        if (!(target instanceof Player)) {
            int ticks = step == 3 ? FINISHER_HITSTOP_TICKS
                    : bonus > 0.5F || target.isDeadOrDying() ? HEAVY_HITSTOP_TICKS : HITSTOP_TICKS;
            hitstop(target, attacker, ticks, step);
            if (com.replecant.skytest.story.SmokeTour.ENABLED) {
                SkyTest.LOGGER.info("SKYTEST HITSTOP {} for {} ticks", target.getName().getString(), ticks);
            }
        }
        if (bonus > 0.5F || step == 3) {
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        SkyTest.LOGGER.debug("Hit {} momentum={} damage={}", target.getName().getString(), bonus, event.getNewDamage());
    }

    @SubscribeEvent
    static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setStrength(event.getStrength() * PLAYER_KNOCKBACK_SCALE);
        }
    }

    private CombatEvents() {}
}
