package com.replecant.skytest.entity;

import com.replecant.skytest.soul.SoulCharge;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Сгусток души — замена опыту. Вылетает из поверженного врага, недолго
 * всплывает вверх, а затем летит к ближайшему игроку и растворяется в нём,
 * пополняя {@link SoulCharge заряд душ}. Сам по себе невидим: его рисуют
 * мелкие фиолетовые частицы.
 */
public class SoulOrbEntity extends Entity {
    public static final int CORE_COLOR = 0xB86BFF;
    public static final int TRAIL_COLOR = 0x6A2FC8;
    private static final int RISE_TICKS = 12;
    private static final double SEEK_RANGE = 16.0;
    private static final int LIFETIME = 6000;

    private int value = 1;
    private int age;
    private int seekTicks;

    public SoulOrbEntity(EntityType<? extends SoulOrbEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static void spawn(ServerLevel level, Vec3 at, int value) {
        SoulOrbEntity orb = new SoulOrbEntity(com.replecant.skytest.registry.ModEntities.SOUL_ORB.get(), level);
        orb.value = value;
        orb.setPos(at);
        double angle = level.random.nextDouble() * Math.PI * 2.0;
        double spread = 0.05 + level.random.nextDouble() * 0.08;
        orb.setDeltaMovement(Math.cos(angle) * spread, 0.12 + level.random.nextDouble() * 0.1, Math.sin(angle) * spread);
        level.addFreshEntity(orb);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            trail();
            return;
        }
        age++;
        if (age > LIFETIME) {
            discard();
            return;
        }
        Vec3 motion = getDeltaMovement();
        ServerPlayer player = age < RISE_TICKS ? null : nearestPlayer();
        if (player == null) {
            // Всплывает и зависает, слегка покачиваясь.
            motion = motion.scale(0.88).add(0.0, Math.sin((age + getId()) * 0.15) * 0.004, 0.0);
            seekTicks = 0;
        } else {
            seekTicks++;
            Vec3 to = player.position().add(0.0, player.getBbHeight() * 0.55, 0.0).subtract(position());
            double distance = to.length();
            if (distance < 0.7) {
                SoulCharge.absorb(player, value, position());
                discard();
                return;
            }
            // Чем дольше летит, тем сильнее тянет: сперва плавная дуга, потом рывок.
            double pull = 0.035 + Math.min(seekTicks, 30) * 0.004;
            motion = motion.scale(0.86).add(to.scale(pull / distance));
            double speed = motion.length();
            if (speed > 0.9) {
                motion = motion.scale(0.9 / speed);
            }
        }
        setDeltaMovement(motion);
        setPos(position().add(motion));
    }

    private ServerPlayer nearestPlayer() {
        ServerPlayer best = null;
        double bestDistance = SEEK_RANGE * SEEK_RANGE;
        for (ServerPlayer player : ((ServerLevel) level()).players()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            double distance = player.distanceToSqr(this);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }

    /** Клиент: мерцающее ядро и тающий след из мелких фиолетовых частиц. */
    private void trail() {
        var random = level().getRandom();
        level().addParticle(new DustParticleOptions(CORE_COLOR, 0.45F + random.nextFloat() * 0.2F),
                getX() + (random.nextDouble() - 0.5) * 0.12, getY() + (random.nextDouble() - 0.5) * 0.12,
                getZ() + (random.nextDouble() - 0.5) * 0.12, 0.0, 0.0, 0.0);
        if (random.nextInt(2) == 0) {
            level().addParticle(new DustParticleOptions(TRAIL_COLOR, 0.3F + random.nextFloat() * 0.25F),
                    getX() + (random.nextDouble() - 0.5) * 0.3, getY() + (random.nextDouble() - 0.5) * 0.3,
                    getZ() + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.01, 0.0);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64.0 * 64.0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        value = input.getIntOr("Value", 1);
        age = input.getIntOr("Age", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Value", value);
        output.putInt("Age", age);
    }
}
