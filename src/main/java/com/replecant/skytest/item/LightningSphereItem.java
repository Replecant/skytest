package com.replecant.skytest.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.replecant.skytest.entity.LightningArcEntity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Сфера молний: над ладонью вращается электрический шар. ПКМ — за заряд душ
 * бьёт молнией туда, куда смотрит игрок; разряд перескакивает ещё на двух
 * ближайших врагов. Металл щита электричество не держит.
 */
public class LightningSphereItem extends SoulCasterItem {
    public static final int COST = 3;
    private static final double RANGE = 24.0;
    private static final float DAMAGE = 7.0F;
    private static final float CHAIN_DAMAGE = 4.0F;
    private static final double CHAIN_RANGE = 6.0;
    private static final int CHAIN_JUMPS = 2;

    public LightningSphereItem(Properties properties) {
        super(properties, COST, 14);
    }

    @Override
    protected void cast(ServerLevel level, Player player, InteractionHand hand) {
        Vec3 from = palm(player, hand);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RANGE));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end,
                player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e.isPickable(), 0.3F);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS, 0.35F, 1.9F);
        if (hit == null) {
            LightningArcEntity.spawn(level, from, end);
            impact(level, end);
            return;
        }
        // Цепь: от ладони к первой цели, потом к ближайшим ещё не задетым.
        List<LivingEntity> struck = new ArrayList<>();
        LivingEntity target = (LivingEntity) hit.getEntity();
        Vec3 point = from;
        for (int jump = 0; target != null && jump <= CHAIN_JUMPS; jump++) {
            Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
            LightningArcEntity.spawn(level, point, center);
            impact(level, center);
            target.hurtServer(level, player.damageSources().indirectMagic(player, player), jump == 0 ? DAMAGE : CHAIN_DAMAGE);
            struck.add(target);
            point = center;
            LivingEntity from0 = target;
            target = level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(CHAIN_RANGE),
                            e -> e.isAlive() && e != player && !(e instanceof Player) && !struck.contains(e) && isFoe(e))
                    .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(from0))).orElse(null);
        }
    }

    /** Цепная молния перескакивает только на тех, кто может драться. */
    private static boolean isFoe(Entity entity) {
        return entity instanceof net.minecraft.world.entity.monster.Enemy
                || entity instanceof com.replecant.skytest.entity.RatEntity;
    }

    private static void impact(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 18, 0.25, 0.25, 0.25, 0.25);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 3, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7F, 1.5F);
    }
}
