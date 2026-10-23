package com.replecant.skytest.item;

import com.replecant.skytest.soul.SoulCharge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Магический предмет, который на каждое применение тратит заряд душ. */
public abstract class SoulCasterItem extends Item {
    private final int cost;
    private final int cooldown;

    protected SoulCasterItem(Properties properties, int cost, int cooldown) {
        super(properties);
        this.cost = cost;
        this.cooldown = cooldown;
    }

    public int cost() {
        return cost;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        // Заряд синхронизирован с клиентом, так что обе стороны решают одинаково.
        if (!player.isCreative() && SoulCharge.get(player) < cost) {
            player.getCooldowns().addCooldown(stack, 10);
            if (level instanceof ServerLevel) {
                player.displayClientMessage(Component.translatable("magic.skytest.no_souls", cost)
                        .withStyle(ChatFormatting.DARK_PURPLE), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.PLAYERS, 0.4F, 1.6F);
            }
            return InteractionResult.FAIL;
        }
        player.getCooldowns().addCooldown(stack, cooldown);
        if (level instanceof ServerLevel serverLevel) {
            if (!player.isCreative()) {
                SoulCharge.spend(player, cost);
            }
            cast(serverLevel, player, hand);
        }
        return InteractionResult.SUCCESS;
    }

    protected abstract void cast(ServerLevel level, Player player, InteractionHand hand);

    /** Примерное положение ладони держащей руки в мире — отсюда вылетает заклинание. */
    public static Vec3 palm(Player player, InteractionHand hand) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        double side = arm == HumanoidArm.RIGHT ? 0.38 : -0.38;
        return player.getEyePosition().add(look.scale(0.7)).add(right.scale(side)).add(0.0, -0.3, 0.0);
    }
}
