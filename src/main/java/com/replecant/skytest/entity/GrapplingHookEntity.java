package com.replecant.skytest.entity;

import java.util.List;

import com.replecant.skytest.registry.ModEntities;
import com.replecant.skytest.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Летящая скелетная рука. Цепляется за блок и подтягивает к нему владельца.
 * Движение игрока применяется на клиенте (там, где игрок реально управляется),
 * сервер следит за правилами отцепления и сбрасывает урон от падения.
 */
public class GrapplingHookEntity extends Projectile implements ItemSupplier {
    public static final double MAX_RANGE = 40.0;
    private static final double PULL_SPEED = 1.1;
    private static final double HANG_DISTANCE = 1.6;
    private static final int MAX_FLIGHT_TICKS = 30;

    private static final EntityDataAccessor<Boolean> ANCHORED =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.BOOLEAN);
    private static final DustParticleOptions ROPE_PARTICLE = new DustParticleOptions(0xD9D3C2, 0.5F);

    private BlockPos anchorPos;
    private ItemStack displayStack;

    public GrapplingHookEntity(EntityType<? extends GrapplingHookEntity> type, Level level) {
        super(type, level);
    }

    public GrapplingHookEntity(Level level, Player owner) {
        this(ModEntities.GRAPPLING_HOOK.get(), level);
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    public static List<GrapplingHookEntity> findHooksOf(Player player) {
        return player.level().getEntitiesOfClass(GrapplingHookEntity.class,
                player.getBoundingBox().inflate(MAX_RANGE + 16.0),
                hook -> hook.getOwner() == player);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ANCHORED, false);
    }

    public boolean isAnchored() {
        return entityData.get(ANCHORED);
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = getOwner();

        if (level().isClientSide()) {
            if (owner instanceof Player player) {
                spawnRopeParticles(player);
                if (isAnchored() && player.isLocalPlayer()) {
                    pullOwner(player);
                }
            }
            return;
        }

        if (!(owner instanceof Player player) || shouldRelease(player)) {
            discard();
            return;
        }

        if (isAnchored()) {
            if (level().getBlockState(anchorPos).isAir()) {
                discard();
                return;
            }
            player.resetFallDistance();
        } else {
            flyServer();
        }
    }

    private boolean shouldRelease(Player player) {
        if (!player.isAlive() || player.isRemoved() || player.level() != level() || player.isShiftKeyDown()) {
            return true;
        }
        if (!player.getMainHandItem().is(ModItems.GRAPPLING_HOOK.get())
                && !player.getOffhandItem().is(ModItems.GRAPPLING_HOOK.get())) {
            return true;
        }
        if (distanceToSqr(player) > MAX_RANGE * MAX_RANGE) {
            return true;
        }
        return !isAnchored() && tickCount > MAX_FLIGHT_TICKS;
    }

    private void flyServer() {
        Vec3 from = position();
        Vec3 motion = getDeltaMovement();
        Vec3 to = from.add(motion);
        BlockHitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 at = hit.getLocation().subtract(motion.normalize().scale(0.05));
            setPos(at.x, at.y, at.z);
            setDeltaMovement(Vec3.ZERO);
            anchorPos = hit.getBlockPos();
            entityData.set(ANCHORED, true);
            level().playSound(null, at.x, at.y, at.z, SoundEvents.BONE_BLOCK_HIT, SoundSource.PLAYERS, 1.0F, 0.8F);
        } else {
            setPos(to.x, to.y, to.z);
            setDeltaMovement(motion.scale(0.99).add(0.0, -0.02, 0.0));
        }
    }

    private void pullOwner(Player player) {
        Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        Vec3 diff = position().subtract(center);
        double dist = diff.length();

        if (dist < HANG_DISTANCE) {
            // Висим на руке: гасим инерцию и компенсируем гравитацию.
            player.setDeltaMovement(diff.scale(0.25).add(0.0, 0.08, 0.0));
        } else {
            Vec3 desired = diff.scale(Math.min(PULL_SPEED, dist * 0.25 + 0.3) / dist);
            player.setDeltaMovement(player.getDeltaMovement().scale(0.3).add(desired.scale(0.7)));
        }
        player.resetFallDistance();
    }

    private void spawnRopeParticles(Player player) {
        if (tickCount % 2 != 0) {
            return;
        }
        Vec3 start = player.position().add(0.0, player.getBbHeight() * 0.6, 0.0);
        Vec3 diff = position().subtract(start);
        int steps = (int) (diff.length() / 0.6);
        for (int i = 1; i < steps; i++) {
            Vec3 p = start.add(diff.scale((double) i / steps));
            level().addParticle(ROPE_PARTICLE, p.x, p.y, p.z, 0.0, 0.0, 0.0);
        }
    }

    // Рука неуязвима; без @Override, чтобы не зависеть от того, где объявлен метод.
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public ItemStack getItem() {
        if (displayStack == null) {
            displayStack = new ItemStack(ModItems.GRAPPLING_HOOK.get());
        }
        return displayStack;
    }
}
