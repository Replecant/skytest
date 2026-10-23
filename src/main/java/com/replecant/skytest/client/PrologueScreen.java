package com.replecant.skytest.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Видение: во тьме загорается столб света, и голос произносит три фразы.
 * Затем свет вспыхивает, заливает экран и гаснет.
 */
public class PrologueScreen extends Screen {
    private static final String[] LINES = {
            "prologue.skytest.line1",
            "prologue.skytest.line2",
            "prologue.skytest.line3",
    };
    private static final int BEAM_IN = 50;
    private static final int FIRST_LINE = 60;
    private static final int CHARS_PER_TICK = 1;
    private static final int LINE_HOLD = 35;
    private static final int TEXT_COLOR = 0xF4E6BE;

    private final int[] lineStart = new int[LINES.length];
    private final String[] lineText = new String[LINES.length];
    private final float[][] motes = new float[60][3];
    private final RandomSource random = RandomSource.create();
    private int flareStart;
    private int end;
    private int ticks;
    private boolean finished;

    public PrologueScreen() {
        super(Component.translatable("prologue.skytest.title"));
        int t = FIRST_LINE;
        for (int i = 0; i < LINES.length; i++) {
            lineText[i] = Component.translatable(LINES[i]).getString();
            lineStart[i] = t;
            t += lineText[i].length() / CHARS_PER_TICK + LINE_HOLD;
        }
        flareStart = t + 20;
        end = flareStart + 90;
        for (float[] mote : motes) {
            resetMote(mote, true);
        }
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks == 2) {
            playSound(SoundEvents.BEACON_AMBIENT, 0.5F, 0.6F);
        }
        for (int i = 0; i < LINES.length; i++) {
            if (ticks == lineStart[i]) {
                playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 0.5F + i * 0.1F);
            }
        }
        if (ticks == flareStart) {
            playSound(SoundEvents.BEACON_ACTIVATE, 0.5F, 1.0F);
        }
        for (float[] mote : motes) {
            mote[1] -= mote[2];
            if (mote[1] < 0) {
                resetMote(mote, false);
            }
        }
        if (ticks >= end && !finished) {
            finished = true;
            onClose();
        }
    }

    @Override
    public void onClose() {
        if (!finished) {
            return; // видение нельзя пропустить
        }
        super.onClose();
        PrologueController.onVisionFinished();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        float time = ticks + partialTick;
        float beam = Mth.clamp(time / BEAM_IN, 0.0F, 1.0F);
        float flare = Mth.clamp((time - flareStart) / 30.0F, 0.0F, 1.0F);

        renderBeam(graphics, time, beam, flare);
        renderMotes(graphics, beam * (1.0F - flare));
        renderText(graphics, time, 1.0F - flare);

        // Вспышка: белое заливает экран, затем всё гаснет в черноту.
        if (time > flareStart) {
            float white = Mth.clamp((time - flareStart - 15) / 25.0F, 0.0F, 1.0F);
            float black = Mth.clamp((time - flareStart - 45) / 35.0F, 0.0F, 1.0F);
            graphics.fill(0, 0, width, height, argb(white * (1.0F - black), 0xFFFFFF));
            graphics.fill(0, 0, width, height, argb(black, 0x000000));
        }
    }

    private void renderBeam(GuiGraphics graphics, float time, float strength, float flare) {
        if (strength <= 0) {
            return;
        }
        int cx = width / 2;
        int bottom = (int) (height * 0.62F);
        float pulse = 1.0F + 0.06F * Mth.sin(time * 0.12F);
        float baseWidth = (10.0F + 30.0F * strength + flare * width) * pulse;

        // Ореол: несколько полупрозрачных слоёв, к центру всё ярче.
        int layers = 12;
        for (int i = 0; i < layers; i++) {
            float k = 1.0F - i / (float) layers;
            int half = Math.max(1, (int) (baseWidth * k * k * 1.6F));
            graphics.fill(cx - half, 0, cx + half, bottom, argb(0.10F * strength, 0xFFF2C8));
        }
        int core = Math.max(1, (int) (baseWidth * 0.12F));
        graphics.fill(cx - core, 0, cx + core, bottom, argb(0.85F * strength, 0xFFFFF4));

        // Низ столба растворяется во тьме, чтобы под ним читался текст.
        int fadeTop = (int) (height * 0.42F);
        graphics.fillGradient(0, fadeTop, width, bottom, 0x00000000, argb(1.0F - flare, 0x000000));
        // Свечение у основания.
        graphics.fillGradient(cx - (int) baseWidth * 3, fadeTop, cx + (int) baseWidth * 3, bottom,
                0x00000000, argb(0.12F * strength, 0xFFE9A8));
    }

    private void renderMotes(GuiGraphics graphics, float alpha) {
        if (alpha <= 0) {
            return;
        }
        int cx = width / 2;
        for (float[] mote : motes) {
            int x = cx + (int) mote[0];
            int y = (int) (mote[1] * height);
            float fade = Mth.clamp(mote[1] * 3.0F, 0.0F, 1.0F) * Mth.clamp((0.62F - mote[1]) * 6.0F, 0.0F, 1.0F);
            graphics.fill(x, y, x + 1, y + 1, argb(alpha * fade * 0.8F, 0xFFF2C8));
        }
    }

    private void resetMote(float[] mote, boolean anywhere) {
        mote[0] = (random.nextFloat() - 0.5F) * 120.0F;
        mote[1] = anywhere ? random.nextFloat() * 0.62F : 0.62F;
        mote[2] = 0.001F + random.nextFloat() * 0.004F;
    }

    private void renderText(GuiGraphics graphics, float time, float alpha) {
        float scale = 1.5F;
        int lineHeight = (int) (font.lineHeight * scale) + 8;
        int top = (int) (height * 0.66F);
        for (int i = 0; i < LINES.length; i++) {
            if (time < lineStart[i]) {
                break;
            }
            int shown = Math.min(lineText[i].length(), (int) ((time - lineStart[i]) * CHARS_PER_TICK));
            String text = lineText[i].substring(0, shown);
            // Старые строки чуть тускнеют.
            boolean current = i == LINES.length - 1 || time < lineStart[i + 1];
            float a = alpha * (current ? 1.0F : 0.55F);
            if (a <= 0.02F) {
                continue;
            }
            graphics.pose().pushMatrix();
            graphics.pose().translate(width / 2.0F, top + i * lineHeight);
            graphics.pose().scale(scale, scale);
            int textWidth = font.width(lineText[i]);
            graphics.drawString(font, text, -textWidth / 2, 0, argb(a, TEXT_COLOR), false);
            graphics.pose().popMatrix();
        }
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private void playSound(net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound, float volume, float pitch) {
        playSound(sound.value(), volume, pitch);
    }

    private static int argb(float alpha, int rgb) {
        int a = (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        return a << 24 | (rgb & 0xFFFFFF);
    }
}
