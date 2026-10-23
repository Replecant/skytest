package com.replecant.skytest.entity;

import com.replecant.skytest.dialogue.Dialogues;
import com.replecant.skytest.registry.ModItems;
import com.replecant.skytest.story.HallEvent;
import com.replecant.skytest.world.LowerPrison;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Незнакомец: охотничья шляпа, шарф до глаз, кожаный плащ; лица не видно,
 * только два белых светящихся глаза. Ждёт на уступе зала, заговаривает
 * с вошедшим, бросает ему затупленный меч и убегает.
 */
public class StrangerEntity extends PathfinderMob {
    private static final double RUN_SPEED = 1.6;
    private static final int MAX_LEAVE_TICKS = 120;

    private boolean leaving;
    private int leaveTicks;

    public StrangerEntity(EntityType<? extends StrangerEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 24.0F, 1.0F));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public boolean isLeaving() {
        return leaving;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || leaving) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            HallEvent.meet(serverPlayer, this);
        }
        return InteractionResult.SUCCESS;
    }

    public void talkTo(ServerPlayer player) {
        getLookControl().setLookAt(player, 30.0F, 30.0F);
        Dialogues.open(player, this, Dialogues.STRANGER_MEETING);
    }

    /** Разговор окончен: бросить игроку меч и убежать. */
    public void onDialogueClosed(ServerPlayer player) {
        if (leaving) {
            return;
        }
        throwSword(player);
        leaving = true;
        leaveTicks = 0;
        Vec3 exit = LowerPrison.STRANGER_EXIT;
        getNavigation().moveTo(exit.x, exit.y, exit.z, RUN_SPEED);
        HallEvent.onStrangerLeft(player);
    }

    private void throwSword(ServerPlayer player) {
        swing(InteractionHand.MAIN_HAND);
        Vec3 from = getEyePosition().add(0.0, -0.3, 0.0);
        Vec3 to = player.position().add(0.0, 0.5, 0.0);
        Vec3 d = to.subtract(from);
        // Баллистика предмета: сопротивление 0.98 за тик, гравитация 0.04.
        int flightTicks = 18;
        double drag = (1.0 - Math.pow(0.98, flightTicks)) / 0.02;
        Vec3 velocity = new Vec3(d.x / drag, (d.y + 0.04 * flightTicks * flightTicks * 0.45) / drag, d.z / drag);
        ItemEntity sword = new ItemEntity(level(), from.x, from.y, from.z, new ItemStack(ModItems.DULL_SWORD.get()),
                velocity.x, velocity.y, velocity.z);
        sword.setPickUpDelay(10);
        sword.setUnlimitedLifetime();
        level().addFreshEntity(sword);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 0.7F, 1.4F);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !leaving) {
            return;
        }
        leaveTicks++;
        Vec3 exit = LowerPrison.STRANGER_EXIT;
        if (leaveTicks % 20 == 0 && getNavigation().isDone()) {
            getNavigation().moveTo(exit.x, exit.y, exit.z, RUN_SPEED);
        }
        if (position().distanceToSqr(exit) < 1.5 || leaveTicks > MAX_LEAVE_TICKS) {
            vanish();
        }
    }

    private void vanish() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 25, 0.3, 0.6, 0.3, 0.02);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.NEUTRAL, 0.6F, 0.6F);
        }
        discard();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
