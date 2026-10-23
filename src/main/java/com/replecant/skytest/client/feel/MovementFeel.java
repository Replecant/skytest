package com.replecant.skytest.client.feel;

import com.replecant.skytest.SkyTest;

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
 * Отклик камеры на движение и бой. Управление остаётся мгновенным, как в ванили, —
 * «вес» передаётся только камерой:
 * <ul>
 *   <li>шаги — камера чуть проседает на каждом шаге, на бегу сильнее;</li>
 *   <li>приземление — «присед» камеры по силе падения;</li>
 *   <li>удар — короткий выпад вперёд и толчок камеры;</li>
 *   <li>полученный удар — встряска;</li>
 *   <li>стрейф — лёгкий крен.</li>
 * </ul>
 * Покачивания отключаются вместе с ванильной настройкой «Покачивание камеры».
 */
@EventBusSubscriber(modid = SkyTest.MODID, value = Dist.CLIENT)
public final class MovementFeel {
    private static final double STRIDE = 1.35;
    private static final double LUNGE = 0.12;

    static final CameraSpring CAMERA = new CameraSpring();

    private static Vec3 horizontal = Vec3.ZERO;
    private static boolean wasOnGround = true;
    private static double fallSpeed;
    private static double strideProgress;
    private static boolean leftFoot;
    private static int lastHurtTime;
    private static float lean;
    private static long lastFrameNanos;

    /** Толчок камеры из других систем (рывок, хитстоп). */
    public static void kick(float pitch, float roll) {
        if (Minecraft.getInstance().options.bobView().get()) {
            CAMERA.kick(pitch, roll);
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        Vec3 velocity = player.getDeltaMovement();
        horizontal = new Vec3(velocity.x, 0.0, velocity.z);

        if (player.hurtTime > lastHurtTime) {
            kick(-90.0F, (player.getRandom().nextBoolean() ? 1 : -1) * 110.0F);
        }
        lastHurtTime = player.hurtTime;

        boolean grounded = player.onGround() && !player.getAbilities().flying && !player.isInWater();
        if (grounded && !wasOnGround) {
            double impact = Mth.clamp((-fallSpeed - 0.3) / 0.9, 0.0, 1.0);
            if (impact > 0.0) {
                kick((float) (60.0 + 160.0 * impact), 0.0F);
            }
            strideProgress = 0.0;
        }
        if (!player.onGround()) {
            fallSpeed = velocity.y;
        }
        wasOnGround = grounded;

        if (grounded) {
            double speed = horizontal.length();
            strideProgress += speed;
            if (strideProgress >= STRIDE) {
                strideProgress -= STRIDE;
                leftFoot = !leftFoot;
                float weight = (float) Mth.clamp(speed / 0.28, 0.2, 1.2);
                kick(14.0F + 24.0F * weight, (leftFoot ? 1 : -1) * (5.0F + 10.0F * weight));
            }
        }
    }

    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        float strength = player.getAttackStrengthScale(0.5F);
        if (player.onGround()) {
            Vec3 look = player.getLookAngle();
            Vec3 forward = new Vec3(look.x, 0.0, look.z).normalize().scale(LUNGE * strength);
            player.setDeltaMovement(player.getDeltaMovement().add(forward));
        }
        kick(55.0F * strength, (player.getRandom().nextBoolean() ? 1 : -1) * 35.0F * strength);
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

        Vec3 right = Vec3.directionFromRotation(0.0F, player.getYRot() + 90.0F);
        double lateral = horizontal.x * right.x + horizontal.z * right.z;
        float leanTarget = player.onGround() || Dash.isDashing() ? (float) Mth.clamp(lateral * 9.0, -3.0, 3.0) : 0.0F;
        lean += (leanTarget - lean) * Math.min(1.0F, dt * 8.0F);

        event.setPitch(event.getPitch() + CAMERA.pitch());
        event.setRoll(event.getRoll() + CAMERA.roll() + lean);
    }

    private MovementFeel() {}
}
