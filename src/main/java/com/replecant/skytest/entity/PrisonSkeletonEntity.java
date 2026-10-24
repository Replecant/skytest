package com.replecant.skytest.entity;

import com.replecant.skytest.combat.CombatEvents;
import com.replecant.skytest.combat.ParryReactive;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Скелет-узник: вместо лука — ржавый меч, и дерётся по правилам боёвки мода.
 * <pre>
 *  CHASE ──близко──▶ WINDUP (замах; на пике — вспышка: сигнал к парированию) ─▶ STRIKE (шаг и удар)
 *                                                          ─▶ RECOVERY (открыт) ─▶ CHASE
 *  парирование ─▶ PARRIED (шатается, долго открыт — время для рипоста)
 *  добивание комбо / удар с разгона ─▶ STAGGER (сбит с ног)
 * </pre>
 * Медленнее и слабее Стража, зато ходит группами. На солнце не горит.
 */
public class PrisonSkeletonEntity extends Skeleton implements ParryReactive {
    private enum State { CHASE, WINDUP, STRIKE, RECOVERY, STAGGER, PARRIED }

    private static final int WINDUP_TICKS = 14;
    private static final int PARRY_CUE = 4;
    private static final int STRIKE_TICKS = 5;
    private static final int RECOVERY_TICKS = 18;
    private static final int WHIFF_BONUS = 10;
    private static final int STAGGER_TICKS = 26;
    private static final int PARRIED_TICKS = 40;
    private static final double STRIKE_RANGE = 2.7;
    private static final float STRIKE_DAMAGE = 4.0F;

    private State state = State.CHASE;
    private int stateTicks;
    private int cooldown;
    private boolean landed;

    public PrisonSkeletonEntity(EntityType<? extends PrisonSkeletonEntity> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Skeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, STRIKE_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Лука нет — ванильный выбор «лук или меч» не нужен. */
    @Override
    public void reassessWeaponGoal() {
    }

    /** Узники тюрьмы не горят на свету из проломов: огонь гасится сразу, урона нет. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (getRemainingFireTicks() > 0 && !isInLava()) {
            clearFire();
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    private void setState(State next) {
        state = next;
        stateTicks = 0;
        setAggressive(next == State.WINDUP);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        stateTicks++;
        if (cooldown > 0) {
            cooldown--;
        }
        LivingEntity target = getTarget();
        boolean engaged = target != null && target.isAlive()
                && !(target instanceof Player p && (p.isCreative() || p.isSpectator()));
        switch (state) {
            case CHASE -> {
                if (!engaged) {
                    return;
                }
                getLookControl().setLookAt(target, 40.0F, 30.0F);
                double distance = distanceTo(target);
                if (distance > STRIKE_RANGE - 0.6) {
                    getNavigation().moveTo(target, 1.1);
                } else {
                    getNavigation().stop();
                }
                if (cooldown <= 0 && distance < STRIKE_RANGE && hasLineOfSight(target)) {
                    setState(State.WINDUP);
                    playSound(SoundEvents.SKELETON_AMBIENT, 1.0F, 0.6F);
                }
            }
            case WINDUP -> {
                getNavigation().stop();
                if (target != null && stateTicks < WINDUP_TICKS - PARRY_CUE) {
                    turnTowards(target, 20.0F);
                }
                if (stateTicks == WINDUP_TICKS - PARRY_CUE) {
                    Vec3 blade = position().add(0.0, 2.1, 0.0);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, blade.x, blade.y, blade.z, 6, 0.1, 0.2, 0.1, 0.04);
                    playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 0.7F, 1.9F);
                }
                if (stateTicks >= WINDUP_TICKS) {
                    setState(State.STRIKE);
                    swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).scale(0.35);
                    setDeltaMovement(getDeltaMovement().add(forward.x, 0.0, forward.z));
                    hurtMarked = true;
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.2F);
                }
            }
            case STRIKE -> {
                getNavigation().stop();
                if (stateTicks == 2) {
                    strike(level, target);
                }
                if (stateTicks >= STRIKE_TICKS) {
                    setState(State.RECOVERY);
                }
            }
            case RECOVERY -> {
                getNavigation().stop();
                if (stateTicks >= RECOVERY_TICKS + (landed ? 0 : WHIFF_BONUS)) {
                    cooldown = 10 + random.nextInt(20);
                    setState(State.CHASE);
                }
            }
            case STAGGER, PARRIED -> {
                getNavigation().stop();
                if (stateTicks >= (state == State.STAGGER ? STAGGER_TICKS : PARRIED_TICKS)) {
                    cooldown = 8;
                    setState(State.CHASE);
                }
            }
        }
    }

    private void turnTowards(LivingEntity target, float maxStep) {
        float wanted = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
        float yaw = Mth.approachDegrees(getYRot(), wanted, maxStep);
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    private void strike(ServerLevel level, LivingEntity target) {
        landed = false;
        if (target == null || distanceTo(target) > STRIKE_RANGE + 0.3) {
            return; // увернулся
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, yBodyRot);
        Vec3 to = target.position().subtract(position());
        if (forward.dot(new Vec3(to.x, 0.0, to.z).normalize()) < 0.3) {
            return;
        }
        // Парирование отменит урон и вызовет onParried.
        if (target.hurtServer(level, damageSources().mobAttack(this), STRIKE_DAMAGE)) {
            landed = true;
            target.knockback(0.5, getX() - target.getX(), getZ() - target.getZ());
        }
    }

    @Override
    public void onParried(ServerPlayer player) {
        setState(State.PARRIED);
        knockback(0.7, player.getX() - getX(), player.getZ() - getZ());
        playSound(SoundEvents.SKELETON_HURT, 1.0F, 0.6F);
        playSound(SoundEvents.SHIELD_BLOCK.value(), 0.8F, 1.3F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player attacker
                && (CombatEvents.comboStep(attacker) == 3 || CombatEvents.momentum(attacker, this) >= 0.7F)
                && state != State.STAGGER) {
            // Добивание комбо или удар с разгона сбивает с ног и прерывает замах.
            setState(State.STAGGER);
            playSound(SoundEvents.SKELETON_HURT, 1.0F, 0.5F);
        } else if (state == State.WINDUP && amount >= 4.0F) {
            // Сильный удар в замах сбивает атаку.
            setState(State.RECOVERY);
        }
        return super.hurtServer(level, source, amount);
    }
}
