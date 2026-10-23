package com.replecant.skytest.dialogue;

import com.replecant.skytest.registry.ModItems;
import com.replecant.skytest.story.Quests;

import java.util.List;

import com.replecant.skytest.network.OpenDialoguePayload;
import com.replecant.skytest.registry.ModAttachments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/** Все разговоры игры и их порядок. */
public final class Dialogues {
    /** Разговоры Фонарщика по порядку; последний повторяется. */
    public static final List<Conversation> LAMPLIGHTER = List.of(
            new Conversation("lamplighter", "awakening", 6),
            new Conversation("lamplighter", "prison", 4),
            new Conversation("lamplighter", "path", 6),
            new Conversation("lamplighter", "farewell", 2));

    /** Встреча с Незнакомцем в зале. */
    public static final Conversation STRANGER_MEETING = new Conversation("stranger", "meeting", 7);

    /** Регулятор: знакомство (запускает задание «Найти Ларпи») и подсказки, пока Ларпи не найдена. */
    public static final Conversation REGULATOR_MEETING = new Conversation("regulator", "meeting", 9);
    public static final Conversation REGULATOR_WAITING = new Conversation("regulator", "waiting", 4);

    /** Регулятор: первая встреча — знакомство и задание, потом — подсказки. */
    public static final Conversation REGULATOR_LARPI = new Conversation("regulator", "larpi", 6);
    public static final Conversation REGULATOR_AFTER = new Conversation("regulator", "after", 2);

    public static final Conversation RAT_CATCHER_IDLE = new Conversation("rat_catcher", "idle", 3);
    public static final Conversation RAT_CATCHER_MEETING = new Conversation("rat_catcher", "meeting", 8);
    public static final Conversation RAT_CATCHER_WAITING = new Conversation("rat_catcher", "waiting", 3);
    public static final Conversation RAT_CATCHER_THANKS = new Conversation("rat_catcher", "thanks", 3);
    public static final Conversation RAT_CATCHER_AFTER = new Conversation("rat_catcher", "after", 2);

    public static void talkToRegulator(ServerPlayer player, Entity regulator) {
        if (Quests.isDone(player, Quests.FIND_LARPI)) {
            open(player, regulator, REGULATOR_AFTER);
        } else if (player.getInventory().contains(stack -> stack.is(ModItems.LARPI.get()))) {
            open(player, regulator, REGULATOR_LARPI);
        } else if (Quests.isStarted(player, Quests.FIND_LARPI)) {
            open(player, regulator, REGULATOR_WAITING);
        } else {
            open(player, regulator, REGULATOR_MEETING);
        }
    }

    public static void talkToRatCatcher(ServerPlayer player, Entity catcher) {
        if (Quests.isDone(player, Quests.FIND_LARPI)) {
            open(player, catcher, RAT_CATCHER_AFTER);
        } else if (!Quests.isActive(player, Quests.FIND_LARPI)) {
            open(player, catcher, RAT_CATCHER_IDLE);
        } else {
            switch (Quests.stage(player)) {
                case Quests.LARPI_SEARCH -> open(player, catcher, RAT_CATCHER_MEETING);
                case Quests.LARPI_CATCH -> open(player, catcher, RAT_CATCHER_WAITING);
                default -> open(player, catcher, RAT_CATCHER_THANKS);
            }
        }
    }

    /** Начать следующий по порядку разговор Фонарщика с игроком. */
    public static void talkToLamplighter(ServerPlayer player, Entity speaker) {
        int stage = player.getData(ModAttachments.LAMPLIGHTER_STAGE);
        Conversation conversation = LAMPLIGHTER.get(Math.min(stage, LAMPLIGHTER.size() - 1));
        player.setData(ModAttachments.LAMPLIGHTER_STAGE, Math.min(stage + 1, LAMPLIGHTER.size() - 1));
        open(player, speaker, conversation);
    }

    public static void open(ServerPlayer player, Entity speaker, Conversation conversation) {
        PacketDistributor.sendToPlayer(player, new OpenDialoguePayload(
                speaker.getId(), speaker.getType().getDescriptionId(), conversation.lineKeys()));
    }

    private Dialogues() {}
}
