package com.replecant.skytest.client;

import com.replecant.skytest.network.PrologueFinishedPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Состояние пролога на клиенте: ожидание (пока грузится мир — экран чёрный),
 * сам экран видения и «пробуждение» с открывающимися веками.
 */
public final class PrologueController {
    private static final long WAKE_DURATION_MS = 4500L;

    private static boolean pending;
    private static long wakeStart = -1L;

    static void requestPrologue() {
        pending = true;
        wakeStart = -1L;
    }

    /** Каждый тик клиента: открыть экран, как только мир загрузился. */
    static void tick(Minecraft mc) {
        if (pending && mc.player != null && mc.level != null && mc.screen == null) {
            pending = false;
            mc.setScreen(new PrologueScreen());
        }
    }

    static void onVisionFinished() {
        ClientPacketDistributor.sendToServer(PrologueFinishedPayload.INSTANCE);
        wakeStart = System.currentTimeMillis();
    }

    /** Поверх HUD: чёрный экран в ожидании и веки при пробуждении. */
    static void renderOverlay(GuiGraphics graphics) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        if (pending) {
            graphics.fill(0, 0, w, h, 0xFF000000);
            return;
        }
        if (wakeStart < 0) {
            return;
        }
        float t = (System.currentTimeMillis() - wakeStart) / (float) WAKE_DURATION_MS;
        if (t >= 1.0F) {
            wakeStart = -1L;
            return;
        }
        float open = eyeOpenness(t);
        int lid = (int) ((1.0F - open) * h / 2.0F) + 1;
        // Мягкий край век.
        graphics.fill(0, 0, w, lid, 0xFF000000);
        graphics.fill(0, h - lid, w, h, 0xFF000000);
        graphics.fillGradient(0, lid, w, lid + 24, 0xFF000000, 0x00000000);
        graphics.fillGradient(0, h - lid - 24, w, h - lid, 0x00000000, 0xFF000000);
        // Общее затемнение, которое постепенно уходит.
        int dim = (int) (Mth.clamp(1.0F - t * 1.2F, 0.0F, 1.0F) * 200);
        graphics.fill(0, 0, w, h, dim << 24);
    }

    /** Глаза: закрыты → приоткрываются → моргание → открываются полностью. */
    private static float eyeOpenness(float t) {
        if (t < 0.15F) {
            return 0.0F;
        } else if (t < 0.35F) {
            return smooth((t - 0.15F) / 0.2F) * 0.35F;
        } else if (t < 0.45F) {
            return 0.35F * (1.0F - smooth((t - 0.35F) / 0.1F));
        } else if (t < 0.55F) {
            return 0.0F;
        } else {
            return smooth((t - 0.55F) / 0.45F);
        }
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    private PrologueController() {}
}
