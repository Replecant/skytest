package com.replecant.skytest.client;

import java.util.List;

import com.replecant.skytest.network.OpenDialoguePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Точка входа для пакетов сервер → клиент. Вызывается только на клиенте. */
public final class ClientPayloadHandlers {
    public static void startPrologue() {
        PrologueController.requestPrologue();
    }

    public static void openDialogue(OpenDialoguePayload payload) {
        List<Component> lines = payload.lines().stream().<Component>map(Component::translatable).toList();
        Minecraft.getInstance().setScreen(
                new DialogueScreen(payload.speakerId(), Component.translatable(payload.speakerName()), lines));
    }

    public static void hitstop(com.replecant.skytest.network.HitstopPayload payload) {
        com.replecant.skytest.client.feel.ClientHitstop.handle(payload);
    }

    private ClientPayloadHandlers() {}
}
