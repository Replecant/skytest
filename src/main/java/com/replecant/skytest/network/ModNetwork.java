package com.replecant.skytest.network;

import com.replecant.skytest.registry.ModItems;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(DoubleJumpPayload.TYPE, DoubleJumpPayload.STREAM_CODEC, ModNetwork::handleDoubleJump);
    }

    private static void handleDoubleJump(DoubleJumpPayload payload, IPayloadContext context) {
        Player player = context.player();
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(ModItems.CIRCUS_BOOTS.get()) || player.onGround()) {
            return;
        }
        player.resetFallDistance();
        boots.hurtAndBreak(1, player, EquipmentSlot.FEET);

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(),
                    10, 0.3, 0.05, 0.3, 0.02);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.6F, 1.6F);
        }
    }

    private ModNetwork() {}
}
