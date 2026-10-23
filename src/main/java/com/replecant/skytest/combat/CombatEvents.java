package com.replecant.skytest.combat;

import com.replecant.skytest.SkyTest;

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

/**
 * Серверная часть «физичного» боя:
 * <ul>
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
