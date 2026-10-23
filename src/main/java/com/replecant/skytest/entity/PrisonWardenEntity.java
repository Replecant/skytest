package com.replecant.skytest.entity;

import com.replecant.skytest.combat.CombatEvents;
import com.replecant.skytest.combat.ParryReactive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Страж Тюрьмы: широкий воин в ламеллярном доспехе, с ростовым щитом и мечом.
 *
 * <p>ИИ — конечный автомат:
 * <pre>
 *  PATROL ──видит игрока──▶ GUARD ──близко──▶ WINDUP ─▶ STRIKE ─▶ RECOVERY ─▶ GUARD
 *                             │  └─вплотную──▶ BASH ───────────────┘
 *                             └─удар с разгона / в спину──▶ STAGGER ─▶ GUARD
 *   парирование удара или толчка ──▶ PARRIED (падает на колено) ─▶ GUARD
 * </pre>
 * Под боевую систему игрока:
 * <ul>
 *   <li>на пике замаха клинок вспыхивает со звоном — это сигнал к парированию;</li>
 *   <li>парированный, он падает на колено и надолго открыт (у игрока — рипост);</li>
 *   <li>если удар ушёл в пустоту (игрок увернулся), отдышка дольше;</li>
 *   <li>третий удар комбо пробивает стойку; три заблокированных удара подряд —
 *       и он сам контратакует толчком щита.</li>
 * </ul>
 * В стойке щит закрывает его спереди: такие удары блокируются и отталкивают
 * атакующего. Уязвим сбоку, сзади, во время замаха и «отдышки» после удара.
 * Удар с разгона пробивает стойку и оглушает.
 *
 * <p>Рядовой противник нижних уровней тюрьмы, не босс.
 */
public class PrisonWardenEntity extends PathfinderMob implements ParryReactive {
    public enum State {
        PATROL(0), GUARD(0), WINDUP(16), STRIKE(7), RECOVERY(26), BASH(12), STAGGER(32), PARRIED(44);

        /** Длительность в тиках (0 — без ограничения). */
        public final int duration;

        State(int duration) {
            this.duration = duration;
        }
    }

    private static final EntityDataAccessor<Byte> STATE =
            SynchedEntityData.defineId(PrisonWardenEntity.class, EntityDataSerializers.BYTE);

    private static final double STRIKE_RANGE = 3.3;
    private static final double BASH_RANGE = 2.2;
    private static final float STRIKE_DAMAGE = 6.0F;
    private static final float BASH_DAMAGE = 2.0F;
    /** Полуугол щита: удары внутри этого конуса спереди блокируются. */
    private static final double SHIELD_COS = Math.cos(Math.toRadians(70));
    private static final float GUARD_BREAK_MOMENTUM = 0.7F;
    private static final int PATROL_REACH = 7;
    /** За сколько тиков до конца замаха вспыхивает клинок (сигнал к парированию). */
    private static final int PARRY_CUE = 4;
    private static final int WHIFF_RECOVERY_BONUS = 14;
    private static final int BLOCKS_BEFORE_COUNTER = 3;

    private int stateTicks;
    private int attackCooldown;
    private int patrolPause;
    private boolean strikeLanded;
    private int blockStreak;
    private long lastBlockTick;
    private boolean patrolForward = true;
    private Vec3 patrolCenter;
    /** Клиент: тик, когда сменилось состояние, — для анимаций. */
    private int clientStateStart;

    public PrisonWardenEntity(EntityType<? extends PrisonWardenEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55)
                .add(Attributes.ATTACK_DAMAGE, STRIKE_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, (byte) State.PATROL.ordinal());
    }

    public State state() {
        return State.values()[entityData.get(STATE)];
    }

    private void setState(State state) {
        if (com.replecant.skytest.story.SmokeTour.ENABLED) {
            com.replecant.skytest.SkyTest.LOGGER.info("SKYTEST WARDEN {} -> {} (hp {})", state(), state, getHealth());
        }
        entityData.set(STATE, (byte) state.ordinal());
        stateTicks = 0;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (STATE.equals(accessor)) {
            clientStateStart = tickCount;
        }
    }

    /** Клиент: сколько тиков (с дробной частью) длится текущее состояние. */
    public float stateTime(float partialTick) {
        return tickCount - clientStateStart + partialTick;
    }

    // ------------------------------------------------------------------ ИИ

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (patrolCenter == null) {
            patrolCenter = position();
        }
        stateTicks++;
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        LivingEntity target = getTarget();
        boolean engaged = target != null && target.isAlive() && distanceToSqr(target) < 24 * 24
                && !(target instanceof Player p && (p.isCreative() || p.isSpectator()));
        if (!engaged) {
            setTarget(null);
            if (state() != State.PATROL && state() != State.STAGGER) {
                setState(State.PATROL);
            }
        } else if (state() == State.PATROL) {
            setState(State.GUARD);
            playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.6F);
        }

        switch (state()) {
            case PATROL -> patrol();
            case GUARD -> guard(target);
            case WINDUP -> {
                stop(target);
                if (stateTicks == State.WINDUP.duration - PARRY_CUE) {
                    parryCue(level);
                }
                if (stateTicks >= State.WINDUP.duration) {
                    setState(State.STRIKE);
                    lunge(0.45);
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
                }
            }
            case STRIKE -> {
                stop(target);
                if (stateTicks == 3) {
                    strike(level, target);
                }
                if (stateTicks >= State.STRIKE.duration) {
                    setState(State.RECOVERY);
                }
            }
            case RECOVERY -> {
                getNavigation().stop();
                int recovery = State.RECOVERY.duration + (strikeLanded ? 0 : WHIFF_RECOVERY_BONUS);
                if (stateTicks >= recovery) {
                    attackCooldown = 15 + random.nextInt(20);
                    setState(engaged ? State.GUARD : State.PATROL);
                }
            }
            case BASH -> {
                stop(target);
                if (stateTicks == 7) {
                    lunge(0.35);
                    bash(level, target);
                }
                if (stateTicks >= State.BASH.duration) {
                    attackCooldown = 20;
                    setState(State.GUARD);
                }
            }
            case STAGGER -> {
                getNavigation().stop();
                if (stateTicks >= State.STAGGER.duration) {
                    setState(engaged ? State.GUARD : State.PATROL);
                }
            }
            case PARRIED -> {
                getNavigation().stop();
                if (stateTicks >= State.PARRIED.duration) {
                    attackCooldown = 10;
                    setState(engaged ? State.GUARD : State.PATROL);
                }
            }
        }
    }

    /** Мерно ходит по мосту туда-обратно и озирается на концах. */
    private void patrol() {
        if (patrolPause > 0) {
            patrolPause--;
            return;
        }
        Vec3 goal = patrolCenter.add(0, 0, patrolForward ? PATROL_REACH : -PATROL_REACH);
        if (position().distanceToSqr(goal) < 1.5) {
            patrolForward = !patrolForward;
            patrolPause = 50 + random.nextInt(40);
            getNavigation().stop();
        } else if (getNavigation().isDone()) {
            getNavigation().moveTo(goal.x, goal.y, goal.z, 0.6);
        }
    }

    /** Стойка со щитом: медленно наступает и выбирает атаку. */
    private void guard(LivingEntity target) {
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distance = distanceTo(target);
        if (distance > 2.2) {
            getNavigation().moveTo(target, 0.7);
        } else {
            getNavigation().stop();
        }
        if (attackCooldown > 0 || stateTicks < 10 || !hasLineOfSight(target)) {
            return;
        }
        if (distance < BASH_RANGE && random.nextFloat() < 0.45F) {
            setState(State.BASH);
            playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.2F, 0.7F);
        } else if (distance < STRIKE_RANGE - 0.3) {
            setState(State.WINDUP);
            playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 0.5F, 0.5F);
        }
    }

    private void stop(LivingEntity target) {
        getNavigation().stop();
        if (target != null) {
            getLookControl().setLookAt(target, 10.0F, 30.0F);
        }
    }

    private void lunge(double strength) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).scale(strength);
        setDeltaMovement(getDeltaMovement().add(forward.x, 0.0, forward.z));
        hurtMarked = true;
    }

    /** Вспышка на клинке и звон — игрок понимает, что пора парировать. */
    private void parryCue(ServerLevel level) {
        Vec3 right = Vec3.directionFromRotation(0.0F, yBodyRot - 90.0F);
        Vec3 blade = position().add(right.scale(0.4)).add(0.0, 2.9, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, blade.x, blade.y, blade.z, 6, 0.05, 0.25, 0.05, 0.01);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, blade.x, blade.y, blade.z, 10, 0.1, 0.3, 0.1, 0.05);
        playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F, 1.8F);
    }

    @Override
    public void onParried(ServerPlayer player) {
        setState(State.PARRIED);
        getNavigation().stop();
        Vec3 away = position().subtract(player.position());
        knockback(0.6, -away.x, -away.z);
        playSound(SoundEvents.SHIELD_BREAK.value(), 1.0F, 0.6F);
        playSound(SoundEvents.IRON_GOLEM_DAMAGE, 0.8F, 0.8F);
    }

    private void strike(ServerLevel level, LivingEntity target) {
        playSound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.6F, 1.4F);
        strikeLanded = false;
        if (target == null || distanceTo(target) > STRIKE_RANGE || facing(target) < 0.2) {
            return; // промах: игрок увернулся — отдышка будет дольше
        }
        // Парирование отменяет урон (hurtServer вернёт false) и переводит в PARRIED.
        if (target.hurtServer(level, damageSources().mobAttack(this), STRIKE_DAMAGE)) {
            strikeLanded = true;
            push(target, 0.7, 0.15);
            playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.7F);
        }
    }

    private void bash(ServerLevel level, LivingEntity target) {
        playSound(SoundEvents.SHIELD_BLOCK.value(), 1.2F, 0.6F);
        if (target == null || distanceTo(target) > BASH_RANGE + 0.6 || facing(target) < 0.3) {
            return;
        }
        target.hurtServer(level, damageSources().mobAttack(this), BASH_DAMAGE);
        // Толчок щитом: на мосту над бездной это опаснее самого удара.
        push(target, 1.5, 0.35);
        playSound(SoundEvents.MACE_SMASH_GROUND, 0.8F, 0.8F);
    }

    private void push(LivingEntity target, double strength, double lift) {
        target.knockback(strength, getX() - target.getX(), getZ() - target.getZ());
        target.setDeltaMovement(target.getDeltaMovement().add(0.0, lift, 0.0));
        target.hurtMarked = true;
    }

    /** Косинус угла между взглядом стража и направлением на точку (1 — прямо перед ним). */
    private double facing(Vec3 point) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, yBodyRot);
        Vec3 to = point.subtract(position());
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        return flat.lengthSqr() < 1.0E-4 ? 1.0 : forward.dot(flat.normalize());
    }

    private double facing(LivingEntity entity) {
        return facing(entity.position());
    }

    // ------------------------------------------------------------------ защита

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Vec3 from = source.getSourcePosition();
        boolean shieldUp = state() == State.GUARD || state() == State.PATROL || state() == State.BASH;
        boolean frontal = from != null && facing(from) > SHIELD_COS;
        Player attacker = source.getEntity() instanceof Player player ? player : null;
        float momentum = attacker != null ? CombatEvents.momentum(attacker, this) : 0.0F;

        if (shieldUp && frontal && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            boolean finisher = attacker != null && CombatEvents.comboStep(attacker) == 3;
            if (momentum >= GUARD_BREAK_MOMENTUM || finisher) {
                // Удар с разгона пробивает стойку.
                setState(State.STAGGER);
                playSound(SoundEvents.SHIELD_BREAK.value(), 1.0F, 0.8F);
                return super.hurtServer(level, source, amount * 0.5F);
            }
            playSound(SoundEvents.SHIELD_BLOCK.value(), 1.0F, 0.8F + random.nextFloat() * 0.2F);
            if (com.replecant.skytest.story.SmokeTour.ENABLED) {
                com.replecant.skytest.SkyTest.LOGGER.info("SKYTEST WARDEN blocked a frontal hit");
            }
            if (source.getDirectEntity() instanceof LivingEntity direct) {
                direct.knockback(0.6, getX() - direct.getX(), getZ() - direct.getZ());
                direct.hurtMarked = true;
            }
            if (attacker != null && getTarget() == null) {
                setTarget(attacker);
            }
            // Три заблокированных удара подряд — контратака толчком щита.
            long now = level.getGameTime();
            blockStreak = now - lastBlockTick > 60 ? 1 : blockStreak + 1;
            lastBlockTick = now;
            if (blockStreak >= BLOCKS_BEFORE_COUNTER && attacker != null && distanceTo(attacker) < BASH_RANGE + 0.8
                    && state() == State.GUARD) {
                blockStreak = 0;
                setState(State.BASH);
                playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.2F, 0.9F);
            }
            return false;
        }
        // Попадание в спину или в момент отдышки — оглушение.
        if (attacker != null && (facing(attacker) < -0.3 || (state() == State.RECOVERY && momentum > 0.4F))) {
            setState(State.STAGGER);
        }
        return super.hurtServer(level, source, amount);
    }

    // ------------------------------------------------------------------ звуки

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 0.45F, 1.3F);
        if (random.nextInt(3) == 0) {
            playSound(SoundEvents.CHAIN_STEP, 0.6F, 0.8F);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CHAIN_STEP;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    private void playSound(Holder<SoundEvent> sound, float volume, float pitch) {
        playSound(sound.value(), volume, pitch);
    }

    static float clampedProgress(float time, int duration) {
        return Mth.clamp(time / duration, 0.0F, 1.0F);
    }
}
