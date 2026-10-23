package com.replecant.skytest.client;

import java.nio.file.Path;

import com.replecant.skytest.SkyTest;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;

/**
 * Визуальная проверка на CI (SKYTEST_CLIENT_SMOKE=true): снимает скриншоты
 * пролога, диалога и тура по локации, затем замеряет FPS и пишет профиль JFR.
 * Тур ведёт сервер ({@link com.replecant.skytest.story.SmokeTour}); отсчёт
 * у обоих идёт от момента пробуждения.
 */
public final class ClientSmoke {
    public static final boolean ENABLED = "true".equals(System.getenv("SKYTEST_CLIENT_SMOKE"));

    private static final int[] WAKE_SHOTS = {25, 120, 200, 330, 630, 685, 715, 780, 840, 900};
    private static final String[] WAKE_NAMES = {
            "04_waking", "05_cell_awake", "06_dialogue_open", "06b_dialogue_mid",
            "07_lamplighter_front", "08_lamplighter_side", "08b_lamplighter_nightvision",
            "09_corridor", "10_hall", "11_end_room"};
    /** Замеры производительности: [начало, конец, название]. */
    private static final int PERF_START = 950;
    private static final int[][] PERF_PHASES = {{950, 1140}, {1160, 1340}, {1360, 1540}};
    private static final String[] PERF_NAMES = {"cell", "corridor", "hall"};
    private static final int QUIT_AT = 1570;

    private static int wakeTicks = -1;
    private static int clientTicks;
    private static Recording recording;
    private static long fpsSum;
    private static int fpsSamples;
    private static int fpsMin = Integer.MAX_VALUE;

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
        if (!ENABLED) {
            return;
        }
        clientTicks++;
        if (clientTicks % 200 == 0) {
            SkyTest.LOGGER.info("SKYTEST DEBUG t={} screen={} level={}", clientTicks,
                    mc.screen == null ? "none" : mc.screen.getClass().getName(), mc.level != null);
        }
        if (clientTicks == 600) {
            shot("00_debug_600");
        }
        if (clientTicks == 9000) {
            SkyTest.LOGGER.info("SKYTEST CLIENT SMOKE TIMEOUT");
            mc.stop();
        }
        if (wakeTicks < 0) {
            return;
        }
        wakeTicks++;
        for (int i = 0; i < WAKE_SHOTS.length; i++) {
            if (wakeTicks == WAKE_SHOTS[i]) {
                shot(WAKE_NAMES[i]);
            }
        }
        if (wakeTicks == PERF_START) {
            startRecording();
        }
        for (int i = 0; i < PERF_PHASES.length; i++) {
            int[] phase = PERF_PHASES[i];
            if (wakeTicks == phase[0] + 40) {
                fpsSum = 0;
                fpsSamples = 0;
                fpsMin = Integer.MAX_VALUE;
            }
            if (wakeTicks > phase[0] + 40 && wakeTicks <= phase[1] && wakeTicks % 10 == 0) {
                int fps = mc.getFps();
                fpsSum += fps;
                fpsSamples++;
                fpsMin = Math.min(fpsMin, fps);
            }
            if (wakeTicks == phase[1]) {
                IntegratedServer server = mc.getSingleplayerServer();
                double mspt = server == null ? -1 : server.getAverageTickTimeNanos() / 1.0E6;
                SkyTest.LOGGER.info("SKYTEST PERF {}: avg {} fps, min {} fps, server {} ms/tick, entities {}, particles {}",
                        PERF_NAMES[i], fpsSamples == 0 ? 0 : fpsSum / fpsSamples, fpsMin,
                        String.format("%.2f", mspt), mc.level.getEntityCount(), mc.particleEngine.countParticles());
                shot("12_perf_" + PERF_NAMES[i]);
            }
        }
        if (wakeTicks == QUIT_AT - 10) {
            stopRecording(mc);
        }
        if (wakeTicks == QUIT_AT) {
            SkyTest.LOGGER.info("SKYTEST CLIENT SMOKE DONE");
            mc.stop();
        }
    }

    private static void startRecording() {
        try {
            recording = new Recording(Configuration.getConfiguration("profile"));
            recording.start();
        } catch (Exception e) {
            SkyTest.LOGGER.error("SKYTEST could not start JFR", e);
        }
    }

    private static void stopRecording(Minecraft mc) {
        if (recording == null) {
            return;
        }
        try {
            recording.stop();
            recording.dump(Path.of(mc.gameDirectory.getAbsolutePath(), "perf.jfr"));
            SkyTest.LOGGER.info("SKYTEST JFR written");
        } catch (Exception e) {
            SkyTest.LOGGER.error("SKYTEST could not write JFR", e);
        }
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1,
                message -> SkyTest.LOGGER.info("Screenshot {}: {}", name, message.getString()));
    }

    private ClientSmoke() {}
}
