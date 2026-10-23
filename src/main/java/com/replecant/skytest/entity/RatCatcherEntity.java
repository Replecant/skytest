package com.replecant.skytest.entity;

import com.replecant.skytest.dialogue.Dialogues;
import com.replecant.skytest.registry.ModItems;
import com.replecant.skytest.story.Quests;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Крысолов: старик, который когда-то ловил крыс для стражи, а теперь живёт
 * с ними в камере под тюрьмой. Сидит, не ходит. Даёт приманку, с которой
 * можно найти Ларпи среди его крыс.
 */
public class RatCatcherEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> TALKING =
            SynchedEntityData.defineId(RatCatcherEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int TALK_TICKS = 400;
    private static final int BAIT_GIFT = 6;

    private int talkTicks;
    private int clientModeStart;

    public RatCatcherEntity(EntityType<? extends RatCatcherEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F, 1.0F));
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
        getLookControl().setLookAt(player, 30.0F, 30.0F);
        playSound(SoundEvents.SILVERFISH_AMBIENT, 0.5F, 1.4F);
        Dialogues.talkToRatCatcher(player, this);
    }

    /** После разговора: на задании — приманка (и пополнение, если кончилась). */
    public void onDialogueClosed(ServerPlayer player) {
        talkTicks = Math.min(talkTicks, 30);
        if (!Quests.isActive(player, Quests.FIND_LARPI)) {
            return;
        }
        int stage = Quests.stage(player);
        if (stage == Quests.LARPI_SEARCH) {
            give(player, BAIT_GIFT);
            Quests.setStage(player, Quests.FIND_LARPI, Quests.LARPI_CATCH);
        } else if (stage == Quests.LARPI_CATCH && countBait(player) < 2) {
            give(player, 4);
        }
    }

    private static int countBait(ServerPlayer player) {
        int count = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ModItems.RAT_BAIT.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void give(ServerPlayer player, int count) {
        ItemStack bait = new ItemStack(ModItems.RAT_BAIT.get(), count);
        if (!player.getInventory().add(bait)) {
            player.drop(bait, false);
        }
        player.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.0F);
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
        return null;
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
