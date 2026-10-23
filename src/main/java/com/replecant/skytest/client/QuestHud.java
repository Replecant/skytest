package com.replecant.skytest.client;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.registry.ModAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Трекер активного задания в левом верхнем углу экрана. */
@EventBusSubscriber(modid = SkyTest.MODID, value = Dist.CLIENT)
public final class QuestHud {
    @SubscribeEvent
    static void onHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) {
            return;
        }
        String quest = mc.player.getData(ModAttachments.ACTIVE_QUEST);
        if (quest.isEmpty()) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        Component header = Component.translatable("quest.skytest.header");
        Component name = Component.translatable("quest.skytest." + quest);
        Component objective = Component.translatable("quest.skytest." + quest + ".objective");
        int width = Math.max(mc.font.width(name), mc.font.width(objective)) + 16;
        int x = 6;
        int y = 6;
        g.fill(x, y, x + width, y + 36, 0x90100D0B);
        g.fill(x, y, x + 2, y + 36, 0xFFE8B04A);
        g.drawString(mc.font, header, x + 8, y + 4, 0xFF8A7E6A, false);
        g.drawString(mc.font, name, x + 8, y + 14, 0xFFE8B04A, false);
        g.drawString(mc.font, objective, x + 8, y + 25, 0xFFCFC6B6, false);
    }

    private QuestHud() {}
}
