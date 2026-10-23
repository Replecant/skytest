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
    /** Этапы «Найти Ларпи»: ищем → Крысолов дал приманку → Ларпи у игрока. */
    public static final int LARPI_SEARCH = 0;
    public static final int LARPI_CATCH = 1;
    public static final int LARPI_CARRY = 2;

    /**
     * Активное задание хранится строкой {@code id} или {@code id/этап};
     * трекер берёт цель этапа из ключа {@code quest.skytest.<id>.objective[.<этап>]}.
     */
    public static String active(ServerPlayer player) {
        String value = player.getData(ModAttachments.ACTIVE_QUEST);
        int slash = value.indexOf('/');
        return slash < 0 ? value : value.substring(0, slash);
    }

    public static boolean isActive(ServerPlayer player, String quest) {
        return quest.equals(active(player));
    }

    public static int stage(ServerPlayer player) {
        String value = player.getData(ModAttachments.ACTIVE_QUEST);
        int slash = value.indexOf('/');
        return slash < 0 ? 0 : Integer.parseInt(value.substring(slash + 1));
    }

    public static void setStage(ServerPlayer player, String quest, int stage) {
        if (isActive(player, quest) && stage(player) < stage) {
            player.setData(ModAttachments.ACTIVE_QUEST, stage == 0 ? quest : quest + "/" + stage);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4F, 1.4F);
        }
    }

    public static boolean isDone(ServerPlayer player, String quest) {
        return HallEvent.hasFlag(player, "done_" + quest);
    }

    /** Задание выполнено: трекер гаснет, надпись «Задание выполнено». */
    public static void complete(ServerPlayer player, String quest) {
        if (isDone(player, quest)) {
            return;
        }
        HallEvent.setFlag(player, "done_" + quest);
        if (isActive(player, quest)) {
            player.setData(ModAttachments.ACTIVE_QUEST, "");
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(15, 60, 25));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable("quest.skytest." + quest).withStyle(ChatFormatting.GOLD)));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("quest.skytest.done").withStyle(ChatFormatting.GRAY)));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6F, 1.0F);
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
