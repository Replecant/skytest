package com.replecant.skytest.entity;

import com.replecant.skytest.registry.ModItems;
import com.replecant.skytest.story.Quests;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Тюремная крыса: снуёт по углам, удирает от игрока, при ударе паникует.
 * С неё падает сырое мясо — основная еда на этом уровне.
 *
 * <p>Крысы Крысолова ({@link #isNest() из крысятника}) на приманку сами бегут
 * к игроку. Ларпи — гордая: к чужой руке не идёт, держится в стороне;
 * её выдаёт белое пятнышко на ухе. ПКМ по крысе с приманкой: обычная
 * утащит приманку и куснёт, Ларпи — даст себя взять.
 */
public class RatEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> LARPI =
            SynchedEntityData.defineId(RatEntity.class, EntityDataSerializers.BOOLEAN);
    private boolean nest;

    public RatEntity(EntityType<? extends RatEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LARPI, false);
    }

    public boolean isLarpi() {
        return entityData.get(LARPI);
    }

    public void setLarpi(boolean larpi) {
        entityData.set(LARPI, larpi);
        if (larpi) {
            setPersistenceRequired();
        }
    }

    /** Крыса из крысятника Крысолова: не входит в общий счёт крыс тюрьмы. */
    public boolean isNest() {
        return nest || isLarpi();
    }

    public void setNest(boolean nest) {
        this.nest = nest;
        setPersistenceRequired();
    }

    private static boolean isBait(ItemStack stack) {
        return stack.is(ModItems.RAT_BAIT.get());
    }

    private static boolean holdsBait(LivingEntity entity) {
        return isBait(entity.getMainHandItem()) || isBait(entity.getOffhandItem());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.7));
        // Ларпи держится подальше от чужака, особенно если у него приманка.
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 6.0F, 1.0, 1.3) {
            @Override
            public boolean canUse() {
                return isLarpi() && super.canUse();
            }
        });
        // Остальные бегут к приманке…
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, RatEntity::isBait, false) {
            @Override
            public boolean canUse() {
                return !isLarpi() && super.canUse();
            }
        });
        // …а без неё удирают от игрока.
        goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Player.class, 4.5F, 1.1, 1.5) {
            @Override
            public boolean canUse() {
                return !isLarpi() && super.canUse() && !(toAvoid != null && holdsBait(toAvoid));
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isBait(stack)) {
            return super.mobInteract(player, hand);
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !(level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (isLarpi()) {
            // Попалась! За пазуху — и к Регулятору.
            ItemStack larpi = new ItemStack(ModItems.LARPI.get());
            if (!serverPlayer.getInventory().add(larpi)) {
                serverPlayer.drop(larpi, false);
            }
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY(0.5), getZ(), 8, 0.3, 0.3, 0.3, 0.0);
            playSound(SoundEvents.SILVERFISH_AMBIENT, 0.8F, 2.0F);
            serverPlayer.displayClientMessage(Component.translatable("quest.skytest.find_larpi.caught")
                    .withStyle(ChatFormatting.GOLD), true);
            Quests.setStage(serverPlayer, Quests.FIND_LARPI, Quests.LARPI_CARRY);
            if (com.replecant.skytest.story.SmokeTour.ENABLED) {
                com.replecant.skytest.SkyTest.LOGGER.info("SKYTEST LARPI caught");
            }
            discard();
        } else {
            // Не она: утащила приманку и цапнула за палец.
            stack.consume(1, player);
            playSound(SoundEvents.GENERIC_EAT.value(), 0.5F, 1.8F);
            playSound(SoundEvents.SILVERFISH_HURT, 0.6F, 1.7F);
            player.hurtServer(level, damageSources().mobAttack(this), 1.0F);
            serverPlayer.displayClientMessage(Component.translatable("quest.skytest.find_larpi.wrong_rat")
                    .withStyle(ChatFormatting.GRAY), true);
            if (com.replecant.skytest.story.SmokeTour.ENABLED) {
                com.replecant.skytest.SkyTest.LOGGER.info("SKYTEST LARPI wrong rat, bait left {}", stack.getCount());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isLarpi()) {
            // Ларпи не трогать: только возмущённо пищит и отскакивает.
            playSound(SoundEvents.SILVERFISH_HURT, 0.6F, 2.0F);
            if (source.getEntity() != null) {
                knockback(0.6, source.getEntity().getX() - getX(), source.getEntity().getZ() - getZ());
            }
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Larpi", isLarpi());
        output.putBoolean("Nest", nest);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setLarpi(input.getBooleanOr("Larpi", false));
        nest = input.getBooleanOr("Nest", false);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !isNest() && super.removeWhenFarAway(distanceToClosestPlayer);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.6F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.35F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SILVERFISH_STEP, 0.08F, 1.6F);
    }
}
