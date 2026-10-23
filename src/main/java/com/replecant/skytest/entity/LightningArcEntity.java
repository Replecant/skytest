package com.replecant.skytest.entity;

import org.joml.Vector3f;

import com.replecant.skytest.registry.ModEntities;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Разряд молнии: живёт несколько тиков, клиент рисует ломаную светящуюся дугу
 * от своей позиции до {@link #offset() цели}. Урон наносит тот, кто его создал.
 */
public class LightningArcEntity extends Entity {
    public static final int LIFETIME = 7;
    private static final EntityDataAccessor<Vector3f> OFFSET =
            SynchedEntityData.defineId(LightningArcEntity.class, EntityDataSerializers.VECTOR3);

    public LightningArcEntity(EntityType<? extends LightningArcEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static void spawn(ServerLevel level, Vec3 from, Vec3 to) {
        LightningArcEntity arc = new LightningArcEntity(ModEntities.LIGHTNING_ARC.get(), level);
        arc.setPos(from);
        Vec3 d = to.subtract(from);
        arc.entityData.set(OFFSET, new Vector3f((float) d.x, (float) d.y, (float) d.z));
        level.addFreshEntity(arc);
    }

    public Vec3 offset() {
        Vector3f v = entityData.get(OFFSET);
        return new Vec3(v.x(), v.y(), v.z());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > LIFETIME) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OFFSET, new Vector3f());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0 * 96.0;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
