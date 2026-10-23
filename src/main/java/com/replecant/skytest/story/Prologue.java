package com.replecant.skytest.story;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.replecant.skytest.entity.LamplighterEntity;
import com.replecant.skytest.network.StartProloguePayload;
import com.replecant.skytest.registry.ModAttachments;
import com.replecant.skytest.world.LowerPrison;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Пролог: столб света с голосом, затем пробуждение в камере.
 * Показывается каждому игроку один раз (флаг {@link ModAttachments#PROLOGUE_SEEN}).
 */
public final class Prologue {
    private static final int TITLE_DELAY = 70;
    private static final int GREETING_DELAY = 170;

    /** Игроки, проснувшиеся недавно: сколько тиков прошло после пробуждения. */
    private static final Map<UUID, Integer> AWAKENING = new HashMap<>();

    public static boolean isSeen(ServerPlayer player) {
        return player.getData(ModAttachments.PROLOGUE_SEEN);
    }

    public static void start(ServerPlayer player) {
        AWAKENING.remove(player.getUUID());
        teleportToCell(player);
        if (player.gameMode() == GameType.SURVIVAL) {
            player.setGameMode(GameType.ADVENTURE);
        }
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 60, 0, false, false));
        PacketDistributor.sendToPlayer(player, StartProloguePayload.INSTANCE);
    }

    /** Клиент досмотрел видение — игрок просыпается. */
    public static void finish(ServerPlayer player) {
        if (isSeen(player)) {
            return;
        }
        player.setData(ModAttachments.PROLOGUE_SEEN, true);
        player.removeEffect(MobEffects.BLINDNESS);
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 6, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 20 * 4, 0, false, false));
        AWAKENING.put(player.getUUID(), 0);
        SmokeTour.begin(player);
    }

    public static void teleportToCell(ServerPlayer player) {
        Vec3 pos = LowerPrison.PLAYER_SPAWN;
        player.teleport(new TeleportTransition(player.level().getServer().overworld(), pos, Vec3.ZERO,
                LowerPrison.PLAYER_YAW, 10.0F, TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
    }

    public static void tick(MinecraftServer server) {
        SmokeTour.tick(server);
        Iterator<Map.Entry<UUID, Integer>> it = AWAKENING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }
            int ticks = entry.getValue() + 1;
            entry.setValue(ticks);
            if (ticks == TITLE_DELAY) {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(30, 70, 30));
                player.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.translatable("area.skytest.lower_prison.subtitle").withStyle(ChatFormatting.GRAY)));
                player.connection.send(new ClientboundSetTitleTextPacket(
                        Component.translatable("area.skytest.lower_prison").withStyle(ChatFormatting.GOLD)));
            } else if (ticks >= GREETING_DELAY) {
                player.level().getEntitiesOfClass(LamplighterEntity.class, player.getBoundingBox().inflate(16.0))
                        .stream().findFirst()
                        .ifPresent(keeper -> keeper.talkTo(player));
                it.remove();
            }
        }
    }

    public static void reset(ServerPlayer player) {
        player.setData(ModAttachments.PROLOGUE_SEEN, false);
        player.setData(ModAttachments.LAMPLIGHTER_STAGE, 0);
    }

    private Prologue() {}
}
