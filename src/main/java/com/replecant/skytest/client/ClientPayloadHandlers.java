package com.replecant.skytest.client;

/** Точка входа для пакетов сервер → клиент. Вызывается только на клиенте. */
public final class ClientPayloadHandlers {
    public static void startPrologue() {
        PrologueController.requestPrologue();
    }

    private ClientPayloadHandlers() {}
}
