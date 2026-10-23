package com.replecant.skytest.item;

import java.util.List;

import com.replecant.skytest.entity.GrapplingHookEntity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Крюк-хват: ПКМ — выстрелить скелетной рукой, ПКМ ещё раз (или присесть) — отпустить.
 */
public class GrapplingHookItem extends Item {
    public static final float LAUNCH_SPEED = 2.2F;

    public GrapplingHookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            List<GrapplingHookEntity> hooks = GrapplingHookEntity.findHooksOf(player);
            if (!hooks.isEmpty()) {
                hooks.forEach(GrapplingHookEntity::discard);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SKELETON_STEP, SoundSource.PLAYERS, 0.8F, 1.4F);
            } else {
                GrapplingHookEntity hook = new GrapplingHookEntity(level, player);
                hook.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, LAUNCH_SPEED, 0.0F);
                level.addFreshEntity(hook);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SKELETON_SHOOT, SoundSource.PLAYERS, 0.8F, 0.6F);
                stack.hurtAndBreak(1, player,
                        hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
        }
        player.getCooldowns().addCooldown(stack, 5);
        return InteractionResult.SUCCESS;
    }
}
