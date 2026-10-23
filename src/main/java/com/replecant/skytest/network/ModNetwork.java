package com.replecant.skytest.network;

import com.replecant.skytest.client.ClientPayloadHandlers;
import com.replecant.skytest.entity.StrangerEntity;
import com.replecant.skytest.registry.ModItems;
import com.replecant.skytest.story.Prologue;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(DoubleJumpPayload.TYPE, DoubleJumpPayload.STREAM_CODEC, ModNetwork::handleDoubleJump);
        registrar.playToServer(PrologueFinishedPayload.TYPE, PrologueFinishedPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        Prologue.finish(player);
                    }
                });
        registrar.playToServer(DialogueClosedPayload.TYPE, DialogueClosedPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) {
                        return;
                    }
                    var speaker = player.level().getEntity(payload.speakerId());
                    if (speaker instanceof StrangerEntity stranger && stranger.distanceTo(player) < 32.0F) {
                        stranger.onDialogueClosed(player);
                    } else if (speaker instanceof com.replecant.skytest.entity.RegulatorEntity regulator
                            && regulator.distanceTo(player) < 40.0F) {
                        regulator.onDialogueClosed(player);
                    } else if (speaker instanceof com.replecant.skytest.entity.RatCatcherEntity catcher
                            && catcher.distanceTo(player) < 32.0F) {
                        catcher.onDialogueClosed(player);
                    }
                });
        registrar.playToServer(DashPayload.TYPE, DashPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                com.replecant.skytest.combat.CombatEvents.onDash(player);
            }
        });
        registrar.playToClient(HitstopPayload.TYPE, HitstopPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandlers.hitstop(payload));
        registrar.playToClient(StartProloguePayload.TYPE, StartProloguePayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandlers.startPrologue());
        registrar.playToClient(CutscenePayload.TYPE, CutscenePayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandlers.cutscene(payload));
        registrar.playToClient(OpenDialoguePayload.TYPE, OpenDialoguePayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandlers.openDialogue(payload));
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
