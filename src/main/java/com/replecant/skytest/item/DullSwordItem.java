package com.replecant.skytest.item;

import com.replecant.skytest.combat.CombatEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Меч, который умеет парировать: ПКМ открывает короткое окно парирования. */
public class DullSwordItem extends Item {
    /** Клиентский отклик (поза парирования); выставляет клиентская часть мода. */
    public static Runnable clientParryHook = () -> { };

    public DullSwordItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        player.getCooldowns().addCooldown(stack, CombatEvents.PARRY_COOLDOWN);
        if (player instanceof ServerPlayer serverPlayer) {
            CombatEvents.startParry(serverPlayer);
        } else if (level.isClientSide()) {
            clientParryHook.run();
        }
        return InteractionResult.SUCCESS;
    }
}
