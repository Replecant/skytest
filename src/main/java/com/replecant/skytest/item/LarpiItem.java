package com.replecant.skytest.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Ларпи у игрока за пазухой. ПКМ — она возмущённо пищит. */
public class LarpiItem extends Item {
    public LarpiItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.playSound(SoundEvents.SILVERFISH_AMBIENT, 0.6F, 1.9F);
        player.getCooldowns().addCooldown(player.getItemInHand(hand), 20);
        return InteractionResult.SUCCESS;
    }
}
