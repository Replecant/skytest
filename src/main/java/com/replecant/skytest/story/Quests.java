package com.replecant.skytest.story;

import java.util.HashSet;
import java.util.Set;

import com.replecant.skytest.registry.ModAttachments;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Задания. Активное задание хранится у игрока и синхронизируется клиенту
 * (оттуда его рисует трекер в углу экрана). Ключи — id задания: quest.skytest.&lt;id&gt;.
 */
public final class Quests {
    public static final String FIND_LARPI = "find_larpi";

    public static String active(ServerPlayer player) {
        return player.getData(ModAttachments.ACTIVE_QUEST);
    }

    public static boolean isActive(ServerPlayer player, String quest) {
        return quest.equals(active(player));
    }

    public static boolean isStarted(ServerPlayer player, String quest) {
        return HallEvent.hasFlag(player, "quest_" + quest);
    }

    public static void start(ServerPlayer player, String quest) {
        if (isStarted(player, quest)) {
            return;
        }
        Set<String> flags = new HashSet<>(player.getData(ModAttachments.STORY_FLAGS));
        flags.add("quest_" + quest);
        player.setData(ModAttachments.STORY_FLAGS, flags);
        player.setData(ModAttachments.ACTIVE_QUEST, quest);
        player.connection.send(new ClientboundSetTitlesAnimationPacket(15, 60, 25));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable("quest.skytest." + quest).withStyle(ChatFormatting.GOLD)));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("quest.skytest.new").withStyle(ChatFormatting.GRAY)));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5F, 0.8F);
    }

    public static void reset(ServerPlayer player) {
        player.setData(ModAttachments.ACTIVE_QUEST, "");
    }

    private Quests() {}
}
