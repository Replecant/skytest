package com.replecant.skytest.entity;

import com.replecant.skytest.story.Quests;

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

    private static final EntityDataAccessor<Boolean> PULLING =
            SynchedEntityData.defineId(RegulatorEntity.class, EntityDataSerializers.BOOLEAN);

    private int talkTicks;
    private int clientModeStart;
    private int clientPullStart = -1000;

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
        builder.define(PULLING, false);
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
        if (PULLING.equals(accessor)) {
            clientPullStart = tickCount;
        }
    }

    /** Катсцена: встал и тянет цепи. */
    public boolean isPulling() {
        return entityData.get(PULLING);
    }

    public void setPulling(boolean pulling) {
        entityData.set(PULLING, pulling);
    }

    /** Клиент: сколько тиков прошло с начала или конца рывков. */
    public float pullTime(float partialTick) {
        return tickCount - clientPullStart + partialTick;
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
        if (com.replecant.skytest.story.RegulatorCutscene.isRunning(player)) {
            return;
        }
        if (!Quests.isStarted(player, Quests.FIND_LARPI)) {
            Quests.start(player, Quests.FIND_LARPI);
        } else if (!Quests.isDone(player, Quests.FIND_LARPI) && takeLarpi(player)) {
            returnLarpi(player);
        } else if (Quests.isDone(player, Quests.FIND_LARPI)) {
            // После задания он поворачивает мост туда, куда игроку нужно.
            boolean toLower = com.replecant.skytest.world.LowerPrison.upperBridgeBuilt((ServerLevel) level());
            com.replecant.skytest.story.RegulatorCutscene.start(player, this,
                    toLower ? com.replecant.skytest.story.RegulatorCutscene.Target.LOWER
                            : com.replecant.skytest.story.RegulatorCutscene.Target.UPPER, false);
        }
    }

    private static boolean takeLarpi(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(com.replecant.skytest.registry.ModItems.LARPI.get())) {
                inventory.removeItem(i, 1);
                return true;
            }
        }
        return false;
    }

    /**
     * Ларпи дома: она устраивается на помосте у Регулятора, а он рычагами
     * перекидывает мост к Верхнему ярусу. Задание выполнено.
     */
    private void returnLarpi(ServerPlayer player) {
        ServerLevel level = (ServerLevel) level();
        RatEntity larpi = com.replecant.skytest.registry.ModEntities.RAT.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (larpi != null) {
            larpi.setLarpi(true);
            larpi.setNest(true);
            larpi.snapTo(getX() + 2.0, getY(), getZ() - 2.5, getYRot(), 0.0F);
            level.addFreshEntity(larpi);
        }
        com.replecant.skytest.story.RegulatorCutscene.start(player, this);
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
