package com.replecant.skytest.entity;

import com.replecant.skytest.registry.ModEntities;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Порождение Первородного змея: летит, извиваясь, кусает первого встречного
 * и впрыскивает яд. О камень рассыпается зелёной пылью.
 */
public class SerpentEntity extends ThrowableProjectile {
    public static final int VENOM_COLOR = 0x6FD13A;
    private static final float BITE_DAMAGE = 3.0F;
    private static final int POISON_TICKS = 140;
    private static final int LIFETIME = 50;

    public SerpentEntity(EntityType<? extends SerpentEntity> type, Level level) {
        super(type, level);
    }

    public SerpentEntity(Level level, Player owner, Vec3 at) {
        this(ModEntities.SERPENT.get(), level);
        setOwner(owner);
        setPos(at);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (random.nextInt(2) == 0) {
                level().addParticle(new DustParticleOptions(VENOM_COLOR, 0.5F),
                        getX() + (random.nextDouble() - 0.5) * 0.3, getY() + 0.1, getZ() + (random.nextDouble() - 0.5) * 0.3,
                        0.0, 0.0, 0.0);
            }
        } else if (tickCount > LIFETIME) {
            dissolve();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof SerpentEntity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Entity target = result.getEntity();
        Entity owner = getOwner();
        DamageSource source = damageSources().thrown(this, owner);
        playSound(SoundEvents.FOX_BITE, 1.0F, 0.6F);
        // Щит Стража укус останавливает (hurtServer вернёт false) — тогда и яда нет.
        if (target.hurtServer(level, source, BITE_DAMAGE) && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 1), owner);
            level.sendParticles(new DustParticleOptions(VENOM_COLOR, 0.9F), target.getX(), target.getY(0.6), target.getZ(),
                    14, 0.3, 0.4, 0.3, 0.0);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) {
            dissolve();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        playSound(SoundEvents.SILVERFISH_DEATH, 0.8F, 0.6F);
    }

    private void dissolve() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(new DustParticleOptions(VENOM_COLOR, 0.8F), getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.0);
        }
        discard();
    }

    // Без @Override: неуязвим, сбить нельзя.
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
