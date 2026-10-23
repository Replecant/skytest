package com.replecant.skytest.entity;

import com.replecant.skytest.dialogue.Dialogues;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Регулятор: огромное сидящее существо в маске, хранитель механизма тюрьмы.
 * Двигает залы рычагами, но сейчас грустит: сбежала его крыса Ларпи.
 * Не ходит; поднимает голову и жестикулирует, пока разговаривает.
 */
public class RegulatorEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> TALKING =
            SynchedEntityData.defineId(RegulatorEntity.class, EntityDataSerializers.BOOLEAN);
    /** Сколько тиков держать «разговорную» позу после реплики. */
    private static final int TALK_TICKS = 400;

    private int talkTicks;
    private int clientModeStart;

    public RegulatorEntity(EntityType<? extends RegulatorEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 16.0F, 1.0F));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TALKING, false);
    }

    public boolean isTalking() {
        return entityData.get(TALKING);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (TALKING.equals(accessor)) {
            clientModeStart = tickCount;
        }
    }

    /** Клиент: сколько тиков длится текущий режим (разговор/грусть). */
    public float modeTime(float partialTick) {
        return tickCount - clientModeStart + partialTick;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            talkTo(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    public void talkTo(ServerPlayer player) {
        entityData.set(TALKING, true);
        talkTicks = TALK_TICKS;
        getLookControl().setLookAt(player, 10.0F, 10.0F);
        playSound(SoundEvents.RAVAGER_AMBIENT, 0.6F, 0.5F);
        Dialogues.talkToRegulator(player, this);
    }

    /** Разговор окончен: после знакомства начинается задание «Найти Ларпи». */
    public void onDialogueClosed(ServerPlayer player) {
        talkTicks = Math.min(talkTicks, 30);
        if (!com.replecant.skytest.story.Quests.isStarted(player, com.replecant.skytest.story.Quests.FIND_LARPI)) {
            com.replecant.skytest.story.Quests.start(player, com.replecant.skytest.story.Quests.FIND_LARPI);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && talkTicks > 0 && --talkTicks == 0) {
            entityData.set(TALKING, false);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        // Тяжёлые вздохи огромной грудной клетки.
        return isTalking() ? null : SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 260;
    }

    @Override
    public float getVoicePitch() {
        return 0.45F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
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

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
