package com.replecant.skytest.client.cutscene;

import java.util.List;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.story.RegulatorCutscene;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Катсцены на движке игры. Камера — невидимая сущность-маркер, которая летит
 * по планам ({@link Shot}): плавный проезд от одной точки к другой, взгляд на
 * заданную точку. Между планами — склейки и затемнения, сверху и снизу
 * кинематографичные полосы, интерфейс и рука спрятаны, управление заблокировано.
 */
@EventBusSubscriber(modid = SkyTest.MODID, value = Dist.CLIENT)
public final class CutscenePlayer {
    /** План: с tick {@code start} по {@code end} камера едет из from в to, глядя с fromLook на toLook. */
    record Shot(int start, int end, Vec3 from, Vec3 fromLook, Vec3 to, Vec3 toLook) {}

    /** Затемнение: на отрезке [start, end] непрозрачность чёрного экрана идёт от a к b. */
    record Fade(int start, int end, float a, float b) {}

    /** Толчок камеры в момент tick. */
    record Shake(int tick, float strength) {}

    record Scene(int length, List<Shot> shots, List<Fade> fades, List<Shake> shakes, int rumbleFrom, int rumbleTo) {}

    private static Scene scene;
    private static int t;
    private static Entity camera;
    private static float shake;
    private static final RandomSource RANDOM = RandomSource.create();

    public static boolean isActive() {
        return scene != null;
    }

    public static void play(String id) {
        Scene next = switch (id) {
            case RegulatorCutscene.ID -> regulatorBridge();
            default -> null;
        };
        if (next == null) {
            SkyTest.LOGGER.warn("Unknown cutscene {}", id);
            return;
        }
        scene = next;
        t = 0;
        shake = 0.0F;
    }

    /**
     * Регулятор встаёт и дёргает цепи; снаружи мост собирается к Верхнему ярусу.
     * Тайминги совпадают с серверной раскадровкой {@link RegulatorCutscene}.
     */
    private static Scene regulatorBridge() {
        Vec3 regulator = new Vec3(38.5, 58.0, 126.5);
        return new Scene(RegulatorCutscene.LENGTH,
                List.of(
                        // Снизу у помоста: он встаёт, камера медленно подъезжает и задирается вверх, к цепям.
                        new Shot(0, 100, new Vec3(35.0, 58.6, 110.0), regulator.add(0, 6.0, 0),
                                new Vec3(36.5, 58.4, 115.5), regulator.add(0, 11.0, 0)),
                        // Снаружи: общий план руин и Верхнего яруса, мост растёт ступенями вверх.
                        new Shot(105, 225, new Vec3(62.0, 69.0, 46.0), new Vec3(60.0, 64.0, 71.0),
                                new Vec3(55.0, 67.0, 49.0), new Vec3(63.0, 66.0, 71.0))),
                List.of(new Fade(0, 8, 1.0F, 0.0F), new Fade(92, 104, 0.0F, 1.0F), new Fade(105, 114, 1.0F, 0.0F),
                        new Fade(212, 224, 0.0F, 1.0F), new Fade(225, 238, 1.0F, 0.0F)),
                List.of(new Shake(RegulatorCutscene.IMPACTS[0], 0.25F), new Shake(RegulatorCutscene.IMPACTS[1], 0.4F),
                        new Shake(RegulatorCutscene.GATE_OPEN, 0.15F)),
                RegulatorCutscene.BUILD_START, RegulatorCutscene.BUILD_END);
    }

    @SubscribeEvent
    static void onTickPre(ClientTickEvent.Pre event) {
        if (scene != null) {
            // Управление заблокировано: ни шагу, ни удара.
            KeyMapping.releaseAll();
        }
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (scene == null) {
            return;
        }
        if (mc.level == null || mc.player == null) {
            stop(mc);
            return;
        }
        t++;
        for (Shake s : scene.shakes()) {
            if (s.tick() == t) {
                shake = Math.max(shake, s.strength());
            }
        }
        if (t >= scene.rumbleFrom() && t <= scene.rumbleTo()) {
            shake = Math.max(shake, 0.035F);
        }
        Shot shot = currentShot();
        if (shot != null) {
            moveCamera(mc, shot);
        } else if (camera != null && t > lastShotEnd()) {
            mc.setCameraEntity(mc.player);
            camera = null;
        }
        shake *= 0.82F;
        if (t >= scene.length()) {
            stop(mc);
        }
    }

    private static Shot currentShot() {
        for (Shot shot : scene.shots()) {
            if (t >= shot.start() && t <= shot.end()) {
                return shot;
            }
        }
        // Между планами (в затемнении) камера стоит, где была.
        return null;
    }

    private static int lastShotEnd() {
        return scene.shots().get(scene.shots().size() - 1).end();
    }

    private static void moveCamera(Minecraft mc, Shot shot) {
        boolean cut = camera == null || t == shot.start();
        if (camera == null) {
            camera = EntityType.MARKER.create(mc.level, EntitySpawnReason.TRIGGERED);
            if (camera == null) {
                return;
            }
            mc.setCameraEntity(camera);
        }
        float p = ease((float) (t - shot.start()) / Math.max(1, shot.end() - shot.start()));
        Vec3 pos = shot.from().lerp(shot.to(), p);
        Vec3 look = shot.fromLook().lerp(shot.toLook(), p);
        if (shake > 0.01F) {
            pos = pos.add((RANDOM.nextFloat() - 0.5F) * shake, (RANDOM.nextFloat() - 0.5F) * shake,
                    (RANDOM.nextFloat() - 0.5F) * shake);
        }
        Vec3 d = look.subtract(pos);
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        if (cut) {
            // Склейка: без интерполяции от прошлого положения.
            camera.setPos(pos);
            camera.xo = pos.x;
            camera.yo = pos.y;
            camera.zo = pos.z;
            camera.xOld = pos.x;
            camera.yOld = pos.y;
            camera.zOld = pos.z;
            camera.setYRot(yaw);
            camera.setXRot(pitch);
            camera.yRotO = yaw;
            camera.xRotO = pitch;
        } else {
            camera.xo = camera.getX();
            camera.yo = camera.getY();
            camera.zo = camera.getZ();
            camera.xOld = camera.getX();
            camera.yOld = camera.getY();
            camera.zOld = camera.getZ();
            camera.yRotO = camera.getYRot();
            camera.xRotO = camera.getXRot();
            camera.setPos(pos);
            camera.setYRot(camera.yRotO + Mth.wrapDegrees(yaw - camera.yRotO));
            camera.setXRot(pitch);
        }
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    private static void stop(Minecraft mc) {
        if (mc.player != null && mc.getCameraEntity() != mc.player) {
            mc.setCameraEntity(mc.player);
        }
        camera = null;
        scene = null;
    }

    private static float fade(float partial) {
        float time = t + partial;
        for (Fade f : scene.fades()) {
            if (time >= f.start() && time <= f.end()) {
                return Mth.lerp((time - f.start()) / Math.max(1, f.end() - f.start()), f.a(), f.b());
            }
        }
        return 0.0F;
    }

    @SubscribeEvent
    static void onHand(RenderHandEvent event) {
        if (scene != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onHudLayer(RenderGuiLayerEvent.Pre event) {
        if (scene != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        if (scene != null) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /** Полосы сверху и снизу и затемнение. */
    @SubscribeEvent
    static void onHud(RenderGuiEvent.Post event) {
        if (scene == null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = t + partial;
        float bars = Math.min(Math.min(time / 10.0F, (scene.length() - time) / 10.0F), 1.0F);
        int h = (int) (g.guiHeight() * 0.11F * Mth.clamp(bars, 0.0F, 1.0F));
        g.fill(0, 0, g.guiWidth(), h, 0xFF000000);
        g.fill(0, g.guiHeight() - h, g.guiWidth(), g.guiHeight(), 0xFF000000);
        float black = fade(partial);
        if (black > 0.0F) {
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), ((int) (black * 255) << 24));
        }
    }

    private CutscenePlayer() {}
}
