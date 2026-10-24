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
 *  CHASE (кружит боком) ──близко──▶ WINDUP (короткий замах, без подсказки) ─▶ STRIKE (шаг и удар)
 *                        ─▶ иногда сразу второй удар (замах ещё короче) ─▶ RECOVERY (короткая отдышка) ─▶ CHASE
 *  парирование ─▶ PARRIED (шатается, долго открыт — время для рипоста)
 *  добивание комбо / удар с разгона ─▶ STAGGER (сбит с ног)
 * </pre>
 * Медленнее и слабее Стража, зато ходит группами. На солнце не горит.
 */
public class PrisonSkeletonEntity extends Skeleton implements ParryReactive {
    private enum State { CHASE, WINDUP, STRIKE, RECOVERY, STAGGER, PARRIED }

    private static final int WINDUP_TICKS = 10;
    /** Второй удар серии — замах короче. */
    private static final int FOLLOWUP_WINDUP_TICKS = 6;
    private static final int STRIKE_TICKS = 4;
    private static final int RECOVERY_TICKS = 7;
    private static final int WHIFF_BONUS = 6;
    private static final int STAGGER_TICKS = 24;
    private static final int PARRIED_TICKS = 36;
    private static final double STRIKE_RANGE = 2.7;
    private static final float STRIKE_DAMAGE = 4.0F;
    private static final float FOLLOWUP_CHANCE = 0.45F;

    private State state = State.CHASE;
    private int stateTicks;
    private int cooldown;
    private boolean landed;
    /** Сейчас идёт второй удар серии. */
    private boolean followup;
    private int strafeDir = 1;
    private int strafeTicks;

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
                turnTowards(target, 25.0F);
                double distance = distanceTo(target);
                if (cooldown > 0 && distance < 4.0) {
                    // Между атаками не стоит, а кружит боком, держа дистанцию.
                    getNavigation().stop();
                    if (--strafeTicks <= 0) {
                        strafeTicks = 15 + random.nextInt(20);
                        strafeDir = random.nextBoolean() ? 1 : -1;
                    }
                    getMoveControl().strafe(distance < 2.0 ? -0.6F : 0.0F, strafeDir * 0.8F);
                } else if (distance > STRIKE_RANGE - 0.6) {
                    getNavigation().moveTo(target, 1.2);
                } else {
                    getNavigation().stop();
                }
                if (cooldown <= 0 && distance < STRIKE_RANGE && hasLineOfSight(target)) {
                    followup = false;
                    setState(State.WINDUP);
                    playSound(SoundEvents.SKELETON_AMBIENT, 0.8F, 0.6F);
                }
            }
            case WINDUP -> {
                getNavigation().stop();
                int windup = followup ? FOLLOWUP_WINDUP_TICKS : WINDUP_TICKS;
                // Доворачивается за игроком почти до самого удара — увернуться надо в последний момент.
                if (target != null && stateTicks < windup - 2) {
                    turnTowards(target, 30.0F);
                }
                if (stateTicks >= windup) {
                    setState(State.STRIKE);
                    swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).scale(0.4);
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
                    if (!followup && engaged && random.nextFloat() < FOLLOWUP_CHANCE) {
                        // Серия: сразу второй удар, с коротким замахом.
                        followup = true;
                        setState(State.WINDUP);
                    } else {
                        setState(State.RECOVERY);
                    }
                }
            }
            case RECOVERY -> {
                getNavigation().stop();
                if (stateTicks >= RECOVERY_TICKS + (landed ? 0 : WHIFF_BONUS)) {
                    cooldown = 4 + random.nextInt(10);
                    setState(State.CHASE);
                }
            }
            case STAGGER, PARRIED -> {
                getNavigation().stop();
                if (stateTicks >= (state == State.STAGGER ? STAGGER_TICKS : PARRIED_TICKS)) {
                    cooldown = 6;
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
        } else if (state == State.CHASE && source.getEntity() != null && distanceTo(source.getEntity()) < STRIKE_RANGE) {
            // Получив удар вблизи, тут же огрызается, не дожидаясь паузы.
            followup = false;
            setState(State.WINDUP);
        }
        return super.hurtServer(level, source, amount);
    }
}
