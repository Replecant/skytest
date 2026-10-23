package com.replecant.skytest.dialogue;

import java.util.ArrayList;
import java.util.List;

/**
 * Разговор — последовательность реплик собеседника. Протагонист немой,
 * поэтому реплик игрока и выбора ответов нет: говорит только NPC.
 * Реплики — ключи перевода {@code dialogue.skytest.<npc>.<id>.<n>}, n = 1..lines.
 */
public record Conversation(String npc, String id, int lines) {
    public List<String> lineKeys() {
        List<String> keys = new ArrayList<>(lines);
        for (int i = 1; i <= lines; i++) {
            keys.add("dialogue.skytest." + npc + "." + id + "." + i);
        }
        return keys;
    }
}
