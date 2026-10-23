package com.replecant.skytest.client;

import com.replecant.skytest.SkyTest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Визуальная проверка на CI (SKYTEST_CLIENT_SMOKE=true): снимает скриншоты
 * пролога, диалогов, тура по локации и события в зале, затем закрывает игру.
 * Тур ведёт сервер ({@link com.replecant.skytest.story.SmokeTour}).
 */
public final class ClientSmoke {
    public static final boolean ENABLED = "true".equals(System.getenv("SKYTEST_CLIENT_SMOKE"));

    private static final int[] WAKE_SHOTS = {25, 120, 630, 690};
    private static final String[] WAKE_NAMES = {"04_waking", "05_cell_awake", "07_corridor_gates", "08_gate_closed"};

    private static int clientTicks;
    private static int wakeTicks = -1;
    private static int dialogues;
    private static int strangerOpened = -1;
    private static int strangerClosed = -1;

    static void onPrologueTick(int ticks) {
        if (!ENABLED) {
            return;
        }
        switch (ticks) {
            case 45 -> shot("01_prologue_beam");
            case 255 -> shot("03_prologue_text_full");
            default -> { }
        }
    }

    static void onWake() {
        if (ENABLED) {
            wakeTicks = 0;
        }
    }

    static void onDialogueOpened() {
        if (ENABLED) {
            dialogues++;
            if (dialogues == 2) {
                strangerOpened = clientTicks;
            }
        }
    }

    static void onDialogueClosed() {
        if (ENABLED && dialogues == 2 && strangerClosed < 0) {
            strangerClosed = clientTicks;
        }
    }

    static void tick(Minecraft mc) {
        if (!ENABLED) {
            return;
        }
        clientTicks++;
        if (clientTicks % 200 == 0) {
            SkyTest.LOGGER.info("SKYTEST DEBUG t={} screen={} dialogues={}", clientTicks,
                    mc.screen == null ? "none" : mc.screen.getClass().getSimpleName(), dialogues);
        }
        if (clientTicks == 9000) {
            SkyTest.LOGGER.info("SKYTEST CLIENT SMOKE TIMEOUT");
            mc.stop();
        }
        if (wakeTicks >= 0) {
            wakeTicks++;
            for (int i = 0; i < WAKE_SHOTS.length; i++) {
                if (wakeTicks == WAKE_SHOTS[i]) {
                    shot(WAKE_NAMES[i]);
                }
            }
            if (wakeTicks == 200) {
                shot("06_lamplighter_dialogue");
            }
        }
        if (strangerOpened >= 0) {
            int t = clientTicks - strangerOpened;
            if (t == 30) {
                shot("09_stranger_dialogue");
            } else if (t == 80 && mc.player != null) {
                // Ночное зрение только на клиенте — чтобы рассмотреть модель.
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
            } else if (t == 100) {
                shot("09b_stranger_nightvision");
            } else if (t == 110 && mc.player != null) {
                mc.player.removeEffect(MobEffects.NIGHT_VISION);
            } else if (t == 220) {
                shot("09c_stranger_dialogue_late");
            }
        }
        if (strangerClosed >= 0) {
            int t = clientTicks - strangerClosed;
            switch (t) {
                case 6 -> shot("10_sword_thrown");
                case 25 -> shot("11_stranger_leaving");
                case 75 -> shot("12_skeleton");
                case 115 -> shot("13_skeleton_closer");
                case 135 -> {
                    SkyTest.LOGGER.info("SKYTEST CLIENT SMOKE DONE");
                    mc.stop();
                }
                default -> { }
            }
        }
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1,
                message -> SkyTest.LOGGER.info("Screenshot {}: {}", name, message.getString()));
    }

    private ClientSmoke() {}
}
