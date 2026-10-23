package com.replecant.skytest.client.feel;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.client.DialogueScreen;
import com.replecant.skytest.entity.GrapplingHookEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * «Физичность» движения и боя на стороне клиента (движение игрока считает клиент):
 * <ul>
 *   <li>инерция — скорость набирается и гасится постепенно, повороты идут дугой;</li>
 *   <li>вес шагов — камера проседает на каждом шаге, на бегу сильнее;</li>
 *   <li>приземление — «присед» камеры и короткая потеря скорости по силе падения;</li>
 *   <li>удар — выпад вперёд, толчок камеры и короткое «завершение» замаха;</li>
 *   <li>полученный удар — встряска камеры.</li>
 * </ul>
 * Покачивания камеры отключаются вместе с ванильной настройкой «Покачивание камеры».
 */
@EventBusSubscriber(modid = SkyTest.MODID, value = Dist.CLIENT)
public final class MovementFeel {
    /** Доля ванильного изменения скорости за тик при разгоне (меньше — тяжелее разгон). */
    private static final double ACCELERATION = 0.4;
    /** То же при торможении (меньше — дольше скольжение). */
    private static final double DECELERATION = 0.55;
    /** Длина шага в блоках. */
    private static final double STRIDE = 1.35;
    /** Длительность «завершения» замаха после удара, в тиках. */
    private static final int SWING_RECOVERY = 6;
    private static final double LUNGE = 0.2;

    private static final CameraSpring CAMERA = new CameraSpring();

    private static Vec3 lastHorizontal = Vec3.ZERO;
    private static boolean wasOnGround = true;
    private static double fallSpeed;
    private static double strideProgress;
    private static boolean leftFoot;
    private static int recoveryTicks;
    private static int lastHurtTime;
    private static float lean;
    private static long lastFrameNanos;

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Vec3 velocity = player.getDeltaMovement();
        Vec3 horizontal = new Vec3(velocity.x, 0.0, velocity.z);

        // Полученный удар: встряска камеры; ванильный отброс не сглаживаем.
        boolean justHurt = player.hurtTime > lastHurtTime;
        lastHurtTime = player.hurtTime;
        if (justHurt) {
            CAMERA.kick(-90.0F, (player.getRandom().nextBoolean() ? 1 : -1) * 110.0F);
        }

        // Резкий рывок скорости (отброс щитом, толчок) — не сглаживаем.
        boolean externalMotion = horizontal.length() - lastHorizontal.length() > 0.25
                || player.hurtTime > 4
                || player.tickCount - GrapplingHookEntity.lastClientPullTick < 3
                || mc.screen instanceof DialogueScreen;
        boolean freeMovement = !player.getAbilities().flying && !player.isSpectator() && !player.isPassenger()
                && !player.isInWater() && !player.isInLava() && !player.isFallFlying() && !player.onClimbable();

        if (freeMovement && !externalMotion && player.onGround() && !player.horizontalCollision) {
            double k = horizontal.lengthSqr() > lastHorizontal.lengthSqr() ? ACCELERATION : DECELERATION;
            if (recoveryTicks > 0) {
                k *= 0.5; // во время замаха ноги «вкопаны»
            }
            Vec3 smoothed = lastHorizontal.add(horizontal.subtract(lastHorizontal).scale(k));
            player.setDeltaMovement(smoothed.x, velocity.y, smoothed.z);
            horizontal = smoothed;
        }
        lastHorizontal = horizontal;
        if (recoveryTicks > 0) {
            recoveryTicks--;
        }

        boolean bob = mc.options.bobView().get();
        // Приземление: чем сильнее падение, тем глубже «присед» и потеря скорости.
        if (player.onGround() && !wasOnGround && freeMovement) {
            double impact = Mth.clamp((-fallSpeed - 0.3) / 0.9, 0.0, 1.0);
            if (impact > 0.0) {
                if (bob) {
                    CAMERA.kick((float) (60.0 + 160.0 * impact), 0.0F);
                }
                Vec3 v = player.getDeltaMovement();
                double keep = 1.0 - 0.6 * impact;
                player.setDeltaMovement(v.x * keep, v.y, v.z * keep);
                lastHorizontal = lastHorizontal.scale(keep);
                recoveryTicks = Math.max(recoveryTicks, (int) (2 + 6 * impact));
            }
            strideProgress = 0.0;
        }
        if (!player.onGround()) {
            fallSpeed = velocity.y;
        }
        wasOnGround = player.onGround();

        // Шаги: камера проседает на каждом шаге, на бегу сильнее.
        if (player.onGround() && freeMovement) {
            double speed = horizontal.length();
            strideProgress += speed;
            if (strideProgress >= STRIDE) {
                strideProgress -= STRIDE;
                leftFoot = !leftFoot;
                if (bob) {
                    float weight = (float) Mth.clamp(speed / 0.28, 0.2, 1.2);
                    CAMERA.kick(18.0F + 30.0F * weight, (leftFoot ? 1 : -1) * (6.0F + 14.0F * weight));
                }
            }
        }
    }

    /** Удар игрока: выпад вперёд, толчок камеры и короткое «завершение» замаха. */
    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        float strength = player.getAttackStrengthScale(0.5F);
        if (player.onGround()) {
            Vec3 look = player.getLookAngle();
            Vec3 forward = new Vec3(look.x, 0.0, look.z).normalize().scale(LUNGE * strength);
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x + forward.x, v.y, v.z + forward.z);
            lastHorizontal = lastHorizontal.add(forward);
        }
        recoveryTicks = SWING_RECOVERY;
        if (Minecraft.getInstance().options.bobView().get()) {
            CAMERA.kick(70.0F * strength, (player.getRandom().nextBoolean() ? 1 : -1) * 45.0F * strength);
        }
    }

    @SubscribeEvent
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0 ? 0.0F : Math.min(0.1F, (now - lastFrameNanos) / 1.0E9F);
        lastFrameNanos = now;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !mc.options.getCameraType().isFirstPerson() || !mc.options.bobView().get()) {
            CAMERA.reset();
            lean = 0.0F;
            return;
        }
        CAMERA.update(dt);

        // Крен при стрейфе: камеру слегка «заносит» в сторону движения.
        Vec3 right = Vec3.directionFromRotation(0.0F, player.getYRot() + 90.0F);
        double lateral = lastHorizontal.x * right.x + lastHorizontal.z * right.z;
        float leanTarget = player.onGround() ? (float) Mth.clamp(lateral * 9.0, -2.5, 2.5) : 0.0F;
        lean += (leanTarget - lean) * Math.min(1.0F, dt * 8.0F);

        event.setPitch(event.getPitch() + CAMERA.pitch());
        event.setRoll(event.getRoll() + CAMERA.roll() + lean);
    }

    private MovementFeel() {}
}
