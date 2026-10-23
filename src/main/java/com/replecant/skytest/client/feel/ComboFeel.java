package com.replecant.skytest.client.feel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.replecant.skytest.SkyTest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * Клиентская подача комбо и парирования:
 * <ul>
 *   <li>у каждого удара комбо свой взмах руки: справа, слева, сверху вниз;</li>
 *   <li>отметки комбо под прицелом;</li>
 *   <li>поза парирования — клинок поперёк перед собой;</li>
 *   <li>вспышка экрана при удачном парировании.</li>
 * </ul>
 * Ритм комбо считается тут по тем же правилам, что и на сервере (только для отображения).
 */
@EventBusSubscriber(modid = SkyTest.MODID, value = Dist.CLIENT)
public final class ComboFeel {
    private static final int COMBO_WINDOW = 28;
    private static final int PARRY_POSE_TICKS = 8;

    private static int ticks;
    private static int combo;
    private static int swingStep;
    private static int lastSwing = -100;
    private static int parryStart = -100;
    private static int flashStart = -100;

    /** Вызывается при ПКМ мечом. */
    public static void startParry() {
        parryStart = ticks;
    }

    /** Удачное парирование — вспышка и сильный толчок камеры. */
    public static void parried() {
        flashStart = ticks;
        MovementFeel.kick(-60.0F, 0.0F);
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        ticks++;
    }

    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        if (player.getAttackStrengthScale(0.5F) < 0.7F) {
            combo = 0;
            swingStep = 1;
        } else {
            combo = ticks - lastSwing <= COMBO_WINDOW && combo < 3 ? combo + 1 : 1;
            swingStep = combo;
            if (combo == 3) {
                combo = 0;
            }
        }
        lastSwing = ticks;
    }

    /** Свой взмах для каждой ступени комбо и поза парирования. */
    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        float parry = parryAmount(event.getPartialTick());
        if (parry > 0.0F) {
            // Клинок поперёк перед собой, рукоять вправо.
            pose.translate(-0.25F * parry, 0.15F * parry, -0.1F * parry);
            pose.mulPose(Axis.ZP.rotationDegrees(-70.0F * parry));
            pose.mulPose(Axis.XP.rotationDegrees(-15.0F * parry));
            return;
        }
        float swing = event.getSwingProgress();
        if (swing <= 0.0F || ticks - lastSwing > 10) {
            return;
        }
        float arc = Mth.sin(swing * Mth.PI);
        switch (swingStep) {
            case 2 -> {
                // Обратный взмах: справа налево.
                pose.translate(-0.35F * arc, 0.05F * arc, 0.0F);
                pose.mulPose(Axis.ZP.rotationDegrees(35.0F * arc));
                pose.mulPose(Axis.YP.rotationDegrees(-25.0F * arc));
            }
            case 3 -> {
                // Добивающий: рубящий удар сверху вниз.
                pose.translate(-0.1F * arc, 0.25F * (1.0F - swing) * arc, -0.15F * arc);
                pose.mulPose(Axis.XP.rotationDegrees(-70.0F * Mth.sin((1.0F - swing) * Mth.HALF_PI) * arc));
            }
            default -> { }
        }
    }

    private static float parryAmount(float partialTick) {
        float t = ticks - parryStart + partialTick;
        if (t < 0.0F || t > PARRY_POSE_TICKS) {
            return 0.0F;
        }
        float in = Mth.clamp(t / 2.0F, 0.0F, 1.0F);
        float out = Mth.clamp((PARRY_POSE_TICKS - t) / 3.0F, 0.0F, 1.0F);
        return Math.min(in, out);
    }

    @SubscribeEvent
    static void onHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int cx = g.guiWidth() / 2;
        int cy = g.guiHeight() / 2;

        // Вспышка удачного парирования.
        int flash = ticks - flashStart;
        if (flash >= 0 && flash < 6) {
            int alpha = (int) (110 * (1.0F - flash / 6.0F));
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), alpha << 24 | 0xFFF6E0);
        }

        // Отметки комбо: три ромбика под прицелом, пока ритм не потерян.
        int since = ticks - lastSwing;
        if (since > COMBO_WINDOW || lastSwing < 0) {
            return;
        }
        int shown = swingStep;
        int alpha = since > COMBO_WINDOW - 8 ? (int) (255 * (COMBO_WINDOW - since) / 8.0F) : 255;
        for (int i = 0; i < 3; i++) {
            int x = cx - 8 + i * 6;
            int y = cy + 13;
            int color = i < shown ? (i == 2 ? 0xF0C040 : 0xE6DFD2) : 0x404040;
            diamond(g, x, y, (alpha << 24) | color);
        }
    }

    private static void diamond(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 1, y, x + 2, y + 3, color);
        g.fill(x, y + 1, x + 3, y + 2, color);
    }

    private ComboFeel() {}
}
