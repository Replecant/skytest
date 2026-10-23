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
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Серверная часть «физичного» боя:
 * <ul>
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

    private static final Map<UUID, Long> DODGE_UNTIL = new HashMap<>();
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
    private static void hitstop(LivingEntity target, Player attacker, int ticks) {
        FROZEN_UNTIL.put(target.getUUID(), target.level().getGameTime() + ticks);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new HitstopPayload(target.getId(), attacker.getId(), ticks));
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
        if (isPlayerMelee(event.getEntity(), event.getSource())) {
            Player attacker = (Player) event.getSource().getEntity();
            float bonus = momentum(attacker, event.getEntity());
            event.setAmount(event.getAmount() * (1.0F + MAX_DAMAGE_BONUS * bonus));
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
        target.knockback(0.2 + 0.6 * bonus, dx, dz);
        if (target instanceof Mob mob) {
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, STAGGER_TICKS, 3, false, false));
            mob.getNavigation().stop();
        }
        if (!(target instanceof Player)) {
            int ticks = bonus > 0.5F || target.isDeadOrDying() ? HEAVY_HITSTOP_TICKS : HITSTOP_TICKS;
            hitstop(target, attacker, ticks);
            if (com.replecant.skytest.story.SmokeTour.ENABLED) {
                SkyTest.LOGGER.info("SKYTEST HITSTOP {} for {} ticks", target.getName().getString(), ticks);
            }
        }
        if (bonus > 0.5F) {
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
