package com.replecant.skytest.entity;

import com.replecant.skytest.dialogue.Dialogues;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Фонарщик: сгорбленное существо в лохмотьях с длинной обмотанной шеей
 * и единственным глазом, светящимся как фонарь. Не двигается, следит взглядом
 * за игроком и разговаривает по ПКМ (см. {@link Dialogues#LAMPLIGHTER}).
 */
public class LamplighterEntity extends PathfinderMob {
    public LamplighterEntity(EntityType<? extends LamplighterEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 12.0F, 1.0F));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
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
        getLookControl().setLookAt(player, 30.0F, 30.0F);
        playSound(SoundEvents.SOUL_ESCAPE.value(), 0.8F, 0.6F + random.nextFloat() * 0.2F);
        Dialogues.talkToLamplighter(player, this);
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

    // Неуязвим; без @Override, чтобы не зависеть от иерархии.
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
