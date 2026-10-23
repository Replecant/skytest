package com.replecant.skytest.client;

import com.replecant.skytest.SkyTest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/**
 * Визуальная проверка на CI (SKYTEST_CLIENT_SMOKE=true): снимает скриншоты
 * пролога и тура по локации, затем закрывает игру. Тур ведёт сервер
 * ({@link com.replecant.skytest.story.SmokeTour}).
 */
public final class ClientSmoke {
    public static final boolean ENABLED = "true".equals(System.getenv("SKYTEST_CLIENT_SMOKE"));
    /** Тик после пробуждения → имя скриншота. Совпадает с остановками SmokeTour + ~30 тиков. */
    private static final int[] WAKE_SHOTS = {25, 120, 230, 290, 350, 410, 470};
    private static final String[] WAKE_NAMES = {
            "04_waking", "05_cell_awake", "06_lamplighter_front", "07_lamplighter_side",
            "08_corridor", "09_hall", "10_end_room"};
    private static final int QUIT_AT = 500;

    private static int wakeTicks = -1;

    static void onPrologueTick(int ticks) {
        if (!ENABLED) {
            return;
        }
        switch (ticks) {
            case 45 -> shot("01_prologue_beam");
            case 150 -> shot("02_prologue_text");
            case 255 -> shot("03_prologue_text_full");
            default -> { }
        }
    }

    static void onWake() {
        if (ENABLED) {
            wakeTicks = 0;
        }
    }

    static void tick(Minecraft mc) {
        if (!ENABLED || wakeTicks < 0) {
            return;
        }
        wakeTicks++;
        for (int i = 0; i < WAKE_SHOTS.length; i++) {
            if (wakeTicks == WAKE_SHOTS[i]) {
                shot(WAKE_NAMES[i]);
            }
        }
        if (wakeTicks == QUIT_AT) {
            SkyTest.LOGGER.info("SKYTEST CLIENT SMOKE DONE");
            mc.stop();
        }
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1,
                message -> SkyTest.LOGGER.info("Screenshot {}: {}", name, message.getString()));
    }

    private ClientSmoke() {}
}
