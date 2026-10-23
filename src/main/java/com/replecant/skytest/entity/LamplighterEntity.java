package com.replecant.skytest.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * за игроком и разговаривает по ПКМ.
 */
public class LamplighterEntity extends PathfinderMob {
    public static final String[] DIALOGUE = {
            "dialogue.skytest.lamplighter.1",
            "dialogue.skytest.lamplighter.2",
            "dialogue.skytest.lamplighter.3",
            "dialogue.skytest.lamplighter.4",
            "dialogue.skytest.lamplighter.5",
            "dialogue.skytest.lamplighter.6",
    };
    public static final String GREETING = "dialogue.skytest.lamplighter.greeting";

    private final Map<UUID, Integer> dialogueProgress = new HashMap<>();

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
        if (!level().isClientSide()) {
            int index = dialogueProgress.getOrDefault(player.getUUID(), 0);
            say(player, DIALOGUE[index]);
            dialogueProgress.put(player.getUUID(), Math.min(index + 1, DIALOGUE.length - 1));
        }
        return InteractionResult.SUCCESS;
    }

    /** Реплика Фонарщика в чат конкретному игроку. */
    public void say(Player player, String key) {
        player.displayClientMessage(Component.empty()
                .append(Component.translatable("entity.skytest.lamplighter").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)), false);
        playSound(SoundEvents.SOUL_ESCAPE.value(), 0.8F, 0.6F + random.nextFloat() * 0.2F);
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
