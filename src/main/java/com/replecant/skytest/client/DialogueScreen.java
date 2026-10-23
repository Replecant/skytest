package com.replecant.skytest.client;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.replecant.skytest.network.DialogueClosedPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Диалоговое окно: камера плавно наводится на лицо собеседника и слегка
 * приближается, сверху и снизу выезжают кинематографические полосы, HUD
 * скрывается, а реплики печатаются в панели внизу экрана.
 * ЛКМ / Пробел / Enter — допечатать реплику или перейти к следующей.
 */
public class DialogueScreen extends Screen {
    private static final float CHARS_PER_TICK = 1.6F;
    private static final float ZOOM = 0.82F;
    private static final double MAX_DISTANCE = 12.0;
    private static final int PANEL_COLOR = 0xE00D0B09;
    private static final int BORDER_COLOR = 0xFF5E4C33;
    private static final int NAME_COLOR = 0xFFE8B04A;
    private static final int TEXT_COLOR = 0xFFE6DFD2;

    private static DialogueScreen active;

    private final int speakerId;
    private final Component speakerName;
    private final List<String> lines = new ArrayList<>();
    private int index;
    private float shown;
    private int ticksOnLine;
    private int ticksOpen;
    private boolean closing;
    private int closingTicks;
    private boolean previousHideGui;
    private long lastFrameNanos;

    public DialogueScreen(int speakerId, Component speakerName, List<Component> lines) {
        super(speakerName);
        this.speakerId = speakerId;
        this.speakerName = speakerName;
        lines.forEach(line -> this.lines.add(line.getString()));
    }

    /** Множитель FOV для плавного приближения камеры (1 — без приближения). */
    public static float fovModifier() {
        if (active == null) {
            return 1.0F;
        }
        float t = active.closing ? 1.0F - active.closingTicks / 8.0F : active.ticksOpen / 12.0F;
        return Mth.lerp(smooth(t), 1.0F, ZOOM);
    }

    @Override
    protected void init() {
        if (active != this) {
            previousHideGui = minecraft.options.hideGui;
            minecraft.options.hideGui = true;
            active = this;
            lastFrameNanos = System.nanoTime();
            ClientSmoke.onDialogueOpened();
        }
    }

    @Override
    public void removed() {
        minecraft.options.hideGui = previousHideGui;
        if (active == this) {
            active = null;
        }
        ClientPacketDistributor.sendToServer(new DialogueClosedPayload(speakerId));
        ClientSmoke.onDialogueClosed();
    }

    @Override
    public void tick() {
        ticksOpen++;
        if (closing) {
            if (++closingTicks >= 8) {
                minecraft.setScreen(null);
            }
            return;
        }
        Entity speaker = speaker();
        if (speaker == null || minecraft.player == null || speaker.distanceTo(minecraft.player) > MAX_DISTANCE) {
            close();
            return;
        }
        String line = currentLine();
        if (shown < line.length()) {
            int before = (int) shown;
            shown = Math.min(line.length(), shown + CHARS_PER_TICK);
            // «Голос» собеседника: глухие щелчки на каждом третьем символе.
            for (int i = before; i < (int) shown; i++) {
                if (i % 3 == 0 && !Character.isWhitespace(line.charAt(i))) {
                    blip();
                }
            }
        } else {
            ticksOnLine++;
            if (ClientSmoke.ENABLED && ticksOnLine > 30) {
                advance();
            }
        }
    }

    private void advance() {
        String line = currentLine();
        if (shown < line.length()) {
            shown = line.length();
            return;
        }
        if (index + 1 >= lines.size()) {
            close();
        } else {
            index++;
            shown = 0;
            ticksOnLine = 0;
        }
    }

    private void close() {
        if (!closing) {
            closing = true;
            closingTicks = 0;
        }
    }

    @Override
    public void onClose() {
        close();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT || event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            advance();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER
                || key == GLFW.GLFW_KEY_E) {
            advance();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Без затемнения и размытия: мир остаётся виден.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        focusCamera(partialTick);

        float open = closing ? 1.0F - (closingTicks + partialTick) / 8.0F : (ticksOpen + partialTick) / 8.0F;
        open = smooth(open);
        int bar = (int) (height * 0.11F * open);
        graphics.fill(0, 0, width, bar, 0xFF000000);
        graphics.fill(0, height - bar, width, height, 0xFF000000);
        if (open < 0.05F) {
            return;
        }
        int alpha = (int) (open * 255.0F);

        int panelWidth = Math.min(width - 40, 400);
        int textWidth = panelWidth - 24;
        List<String> wrapped = wrap(currentLine(), textWidth);
        int lineHeight = font.lineHeight + 2;
        int panelHeight = Math.max(3, wrapped.size()) * lineHeight + 18;
        int x = (width - panelWidth) / 2;
        int y = height - bar - panelHeight - 10;

        // Панель с рамкой.
        graphics.fill(x, y, x + panelWidth, y + panelHeight, withAlpha(PANEL_COLOR, alpha));
        graphics.renderOutline(x, y, panelWidth, panelHeight, withAlpha(BORDER_COLOR, alpha));
        graphics.renderOutline(x + 2, y + 2, panelWidth - 4, panelHeight - 4, withAlpha(0x40E8B04A, alpha));

        // Табличка с именем собеседника.
        int nameWidth = font.width(speakerName) + 14;
        int nameY = y - 13;
        graphics.fill(x + 10, nameY, x + 10 + nameWidth, y + 1, withAlpha(PANEL_COLOR, alpha));
        graphics.renderOutline(x + 10, nameY, nameWidth, 14, withAlpha(BORDER_COLOR, alpha));
        graphics.drawString(font, speakerName, x + 17, nameY + 3, withAlpha(NAME_COLOR, alpha), false);

        // Текст печатается посимвольно.
        int remaining = (int) shown;
        for (int i = 0; i < wrapped.size() && remaining > 0; i++) {
            String row = wrapped.get(i);
            String visible = row.substring(0, Math.min(row.length(), remaining));
            remaining -= row.length() + 1;
            graphics.drawString(font, visible, x + 12, y + 10 + i * lineHeight, withAlpha(TEXT_COLOR, alpha), false);
        }

        // Индикатор «дальше» и счётчик реплик.
        if (shown >= currentLine().length() && !closing) {
            boolean blink = (ticksOpen / 8) % 2 == 0;
            String marker = index + 1 < lines.size() ? "▼" : "■";
            if (blink) {
                graphics.drawString(font, marker, x + panelWidth - 14, y + panelHeight - 12, withAlpha(NAME_COLOR, alpha), false);
            }
        }
        String counter = (index + 1) + "/" + lines.size();
        graphics.drawString(font, counter, x + panelWidth - 8 - font.width(counter), nameY + 3,
                withAlpha(0xFF7A6A55, alpha), false);

        Component hint = Component.translatable("dialogue.skytest.hint");
        graphics.drawString(font, hint, width - font.width(hint) - 12, height - bar / 2 - 4,
                withAlpha(0xFF6E6658, alpha), false);
    }

    /** Плавно поворачивает камеру игрока к лицу собеседника. */
    private void focusCamera(float partialTick) {
        long now = System.nanoTime();
        float dt = Math.min(0.1F, (now - lastFrameNanos) / 1.0E9F);
        lastFrameNanos = now;
        LocalPlayer player = minecraft.player;
        Entity speaker = speaker();
        if (player == null || speaker == null || closing) {
            return;
        }
        Vec3 from = player.getEyePosition(partialTick);
        Vec3 to = speaker.getEyePosition(partialTick);
        Vec3 d = to.subtract(from);
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        float targetYaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        // Лицо чуть выше центра: его не перекрывают ни панель внизу, ни полоса сверху.
        float targetPitch = (float) (-(Mth.atan2(d.y, horizontal) * Mth.RAD_TO_DEG)) + 1.5F;

        float k = 1.0F - (float) Math.exp(-dt * 6.0F);
        float yaw = player.getYRot() + Mth.wrapDegrees(targetYaw - player.getYRot()) * k;
        float pitch = Mth.clamp(player.getXRot() + (targetPitch - player.getXRot()) * k, -90.0F, 90.0F);
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
    }

    private Entity speaker() {
        return minecraft.level == null ? null : minecraft.level.getEntity(speakerId);
    }

    private String currentLine() {
        return lines.isEmpty() ? "" : lines.get(index);
    }

    private void blip() {
        float pitch = 0.55F + minecraft.level.getRandom().nextFloat() * 0.1F;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), pitch, 0.25F));
    }

    /** Перенос по словам (по ширине в пикселях). */
    private List<String> wrap(String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = row.isEmpty() ? word : row + " " + word;
            if (font.width(candidate) > maxWidth && !row.isEmpty()) {
                result.add(row.toString());
                row = new StringBuilder(word);
            } else {
                row = new StringBuilder(candidate);
            }
        }
        result.add(row.toString());
        return result;
    }

    private static int withAlpha(int argb, int alpha) {
        int a = ((argb >>> 24) * alpha) / 255;
        return a << 24 | (argb & 0xFFFFFF);
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
