package com.replecant.skytest.story;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Для визуальной проверки на CI: водит игрока по точкам обзора, а затем
 * заводит в зал, где срабатывает встреча с Незнакомцем.
 */
public final class SmokeTour {
    public static final boolean ENABLED = "true".equals(System.getenv("SKYTEST_CLIENT_SMOKE"));

    private record Stop(int tick, Vec3 pos, float yaw, float pitch) {}

    private static final Stop[] STOPS = {
            new Stop(600, new Vec3(-3.5, 64, 7.5), -90.0F, 0.0F),   // коридор с воротами; разгон/торможение
            new Stop(720, new Vec3(8.5, 64, 7.0), 180.0F, 5.0F),    // закрытые ворота камеры N2
            new Stop(780, new Vec3(31.5, 64, 7.5), -90.0F, -5.0F),  // вход в зал → встреча
    };

    /** Точки обзора моста — отсчёт от появления скелета. */
    private static final Stop[] BRIDGE_STOPS = {
            new Stop(190, new Vec3(38.5, 57, 35.0), 0.0F, 8.0F),       // мост уходит в небо
            new Stop(230, new Vec3(38.5, 57, 84.0), 180.0F, -28.0F),   // тюрьма-скала над мостом
            new Stop(270, new Vec3(39.5, 57, 50.0), 70.0F, 55.0F),     // взгляд в бездну
    };

    private static final Map<UUID, Integer> TOURS = new HashMap<>();
    private static final Map<UUID, Integer> BRIDGE_TOURS = new HashMap<>();

    static void begin(ServerPlayer player) {
        if (ENABLED) {
            TOURS.put(player.getUUID(), 0);
        }
    }

    /** Повернуть игрока к поднимающемуся скелету, чтобы он попал в кадр. */
    static void onSkeletonSpawned(ServerPlayer player, Entity skeleton) {
        if (!ENABLED) {
            return;
        }
        BRIDGE_TOURS.put(player.getUUID(), 0);
        // Встать у края спуска и смотреть вниз по лестнице.
        Vec3 viewpoint = new Vec3(38.5, 64, 11.0);
        Vec3 d = skeleton.position().add(0, 1.0, 0).subtract(viewpoint.add(0, player.getEyeHeight(), 0));
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        player.teleport(new TeleportTransition(player.level().getServer().overworld(), viewpoint, Vec3.ZERO,
                yaw, pitch, TeleportTransition.DO_NOTHING));
    }

    static void tick(MinecraftServer server) {
        runStops(server, BRIDGE_TOURS, BRIDGE_STOPS);
        runStops(server, TOURS, STOPS);
    }

    private static void runStops(MinecraftServer server, Map<UUID, Integer> tours, Stop[] stops) {
        Iterator<Map.Entry<UUID, Integer>> it = tours.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }
            int ticks = entry.getValue() + 1;
            entry.setValue(ticks);
            for (Stop stop : stops) {
                if (ticks == stop.tick) {
                    player.teleport(new TeleportTransition(server.overworld(), stop.pos, Vec3.ZERO,
                            stop.yaw, stop.pitch, TeleportTransition.DO_NOTHING));
                }
            }
        }
    }

    private SmokeTour() {}
}
