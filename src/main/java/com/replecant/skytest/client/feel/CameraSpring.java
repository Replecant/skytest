package com.replecant.skytest.client.feel;

import net.minecraft.util.Mth;

/**
 * Камера на «пружине»: толчки (шаг, удар, приземление) сбивают наклон
 * и крен, а пружина с демпфером плавно возвращает их к нулю.
 */
final class CameraSpring {
    private static final float STIFFNESS = 170.0F;
    private static final float DAMPING = 17.0F;
    private static final float MAX_ANGLE = 12.0F;

    private float pitch;
    private float pitchVelocity;
    private float roll;
    private float rollVelocity;

    /** Толчок в градусах в секунду: положительный pitch — камера кивает вниз. */
    void kick(float pitchImpulse, float rollImpulse) {
        pitchVelocity += pitchImpulse;
        rollVelocity += rollImpulse;
    }

    void update(float dt) {
        // Мелкие подшаги: стабильно даже при низком FPS.
        int steps = Math.max(1, (int) Math.ceil(dt / (1.0F / 120.0F)));
        float h = dt / steps;
        for (int i = 0; i < steps; i++) {
            pitchVelocity += (-STIFFNESS * pitch - DAMPING * pitchVelocity) * h;
            rollVelocity += (-STIFFNESS * roll - DAMPING * rollVelocity) * h;
            pitch = Mth.clamp(pitch + pitchVelocity * h, -MAX_ANGLE, MAX_ANGLE);
            roll = Mth.clamp(roll + rollVelocity * h, -MAX_ANGLE, MAX_ANGLE);
        }
    }

    float pitch() {
        return pitch;
    }

    float roll() {
        return roll;
    }

    void reset() {
        pitch = pitchVelocity = roll = rollVelocity = 0.0F;
    }
}
