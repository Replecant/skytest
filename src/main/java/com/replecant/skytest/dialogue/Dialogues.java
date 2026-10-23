package com.replecant.skytest.dialogue;

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
