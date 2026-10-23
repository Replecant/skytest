package com.replecant.skytest.client;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.registry.ModAttachments;
import com.replecant.skytest.soul.SoulCharge;
import com.replecant.skytest.world.AdventureWorld;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Полоса заряда душ на месте полосы опыта: тёмная рамка, фиолетовая заливка
 * с делениями по десять, число над ней. При пополнении — короткая вспышка.
 */
@EventBusSubscriber(modid = SkyTest.MODID, value = Dist.CLIENT)
public final class SoulHud {
    private static final int WIDTH = 182;
    private static int lastCharge = -1;
    private static int flash;
    private static float shown;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            lastCharge = -1;
            return;
        }
        int charge = mc.player.getData(ModAttachments.SOUL_CHARGE);
        if (lastCharge >= 0 && charge > lastCharge) {
            flash = 12;
        }
        lastCharge = charge;
        if (flash > 0) {
            flash--;
        }
        // Заливка плавно догоняет настоящее значение.
        shown += (charge - shown) * 0.25F;
        if (Math.abs(charge - shown) < 0.05F) {
            shown = charge;
        }
    }

    @SubscribeEvent
    static void onHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (com.replecant.skytest.client.cutscene.CutscenePlayer.isActive()) {
            return;
        }
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.player.isCreative()
                || mc.player.isSpectator() || !AdventureWorld.isAdventure(mc.level)) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int x = g.guiWidth() / 2 - WIDTH / 2;
        int y = g.guiHeight() - 29;
        int charge = mc.player.getData(ModAttachments.SOUL_CHARGE);

        g.fill(x - 1, y - 1, x + WIDTH + 1, y + 6, 0xFF0B0612);
        g.fill(x, y, x + WIDTH, y + 5, 0xFF1E1230);
        int filled = Mth.floor(WIDTH * Mth.clamp(shown / SoulCharge.MAX, 0.0F, 1.0F));
        if (filled > 0) {
            g.fillGradient(x, y, x + filled, y + 5, 0xFFC08CFF, 0xFF6A2FC8);
            g.fill(x, y, x + filled, y + 1, 0x60FFFFFF);
        }
        for (int i = 1; i < 10; i++) {
            int tx = x + WIDTH * i / 10;
            g.fill(tx, y, tx + 1, y + 5, 0x800B0612);
        }
        if (flash > 0) {
            int alpha = flash * 255 / 12 / 2;
            g.fill(x - 1, y - 1, x + WIDTH + 1, y + 6, (alpha << 24) | 0xD9B3FF);
        }
        if (charge > 0) {
            String text = String.valueOf(charge);
            int tx = x + WIDTH / 2 - mc.font.width(text) / 2;
            int ty = y - 6;
            for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                g.drawString(mc.font, text, tx + o[0], ty + o[1], 0xFF000000, false);
            }
            g.drawString(mc.font, text, tx, ty, flash > 0 ? 0xFFF0DDFF : 0xFFB57CFF, false);
        }
    }

    private SoulHud() {}
}
