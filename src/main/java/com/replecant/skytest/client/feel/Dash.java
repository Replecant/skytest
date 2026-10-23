package com.replecant.skytest.client.feel;

import org.lwjgl.glfw.GLFW;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.network.DashPayload;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Рывок-уклонение: короткий резкий бросок в сторону движения (без ввода — вперёд).
 * Работает и в воздухе (один раз до приземления), на время рывка высота держится.
 * Сервер даёт несколько тиков неуязвимости к ударам существ ({@link com.replecant.skytest.combat.CombatEvents}).
 */
@EventBusSubscriber(modid = SkyTest.MODID, value = Dist.CLIENT)
public final class Dash {
    public static final KeyMapping KEY = new KeyMapping("key.skytest.dash", GLFW.GLFW_KEY_LEFT_ALT, KeyMapping.Category.MOVEMENT);

    private static final int DASH_TICKS = 5;
    private static final double SPEED = 0.85;
    private static final int COOLDOWN = 22;

    private static int dashTicks;
    private static int cooldown;
    private static boolean airDashUsed;
    private static Vec3 direction = Vec3.ZERO;

    public static boolean isDashing() {
        return dashTicks > 0;
    }

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KEY);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (cooldown > 0) {
            cooldown--;
        }
        while (KEY.consumeClick()) {
            tryDash(mc);
        }
    }

    /** Попытаться сделать рывок по текущему вводу WASD; true — получилось. */
    public static boolean tryDash(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }
        Vec2 input = mc.player.input.getMoveVector();
        return tryDash(mc, input.x, input.y);
    }

    /** Рывок в направлении ввода (strafe — влево+, forward — вперёд+); без ввода — вперёд. */
    public static boolean tryDash(Minecraft mc, double strafe, double forward) {
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || cooldown > 0 || player.getAbilities().flying
                || player.isPassenger() || player.isInWater() || player.isSpectator()) {
            return false;
        }
        if (!player.onGround()) {
            if (airDashUsed) {
                return false;
            }
            airDashUsed = true;
        }
        // Направление из ввода, как это делает ванильный moveRelative.
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        if (strafe * strafe + forward * forward < 0.01) {
            forward = 1.0;
        }
        double dx = strafe * Mth.cos(yaw) - forward * Mth.sin(yaw);
        double dz = forward * Mth.cos(yaw) + strafe * Mth.sin(yaw);
        direction = new Vec3(dx, 0.0, dz).normalize();

        dashTicks = DASH_TICKS;
        cooldown = COOLDOWN;
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_JUMP,
                SoundSource.PLAYERS, 0.6F, 1.5F, false);
        MovementFeel.kick(-25.0F, (float) (-strafe * 60.0));
        ClientPacketDistributor.sendToServer(DashPayload.INSTANCE);
        return true;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        if (player.onGround()) {
            airDashUsed = false;
        }
        if (dashTicks <= 0) {
            return;
        }
        dashTicks--;
        Vec3 v = player.getDeltaMovement();
        double y = player.onGround() ? v.y : Math.max(v.y, 0.0) * 0.5;
        double speed = dashTicks == 0 ? SPEED * 0.4 : SPEED; // резкий, но не обрубленный конец
        player.setDeltaMovement(direction.x * speed, y, direction.z * speed);
        player.resetFallDistance();
    }

    @SubscribeEvent
    static void onFov(ComputeFovModifierEvent event) {
        if (dashTicks > 0) {
            event.setNewFovModifier(event.getNewFovModifier() * 1.08F);
        }
    }

    /** Полоска перезарядки под прицелом. */
    @SubscribeEvent
    static void onHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (cooldown <= 0 || mc.options.hideGui || mc.screen != null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int cx = g.guiWidth() / 2;
        int y = g.guiHeight() / 2 + 9;
        int full = 16;
        int filled = (int) (full * (1.0F - cooldown / (float) COOLDOWN));
        g.fill(cx - full / 2, y, cx + full / 2, y + 1, 0x60000000);
        g.fill(cx - full / 2, y, cx - full / 2 + filled, y + 1, 0xC0E6DFD2);
    }

    private Dash() {}
}
