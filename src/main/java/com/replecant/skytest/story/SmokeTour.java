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
            new Stop(600, new Vec3(-3.5, 64, 7.5), -90.0F, 0.0F),   // коридор с воротами
            new Stop(660, new Vec3(8.5, 64, 7.0), 180.0F, 5.0F),    // закрытые ворота камеры N2
            new Stop(720, new Vec3(31.5, 64, 7.5), -90.0F, -5.0F),  // вход в зал → встреча
    };

    private static final Map<UUID, Integer> TOURS = new HashMap<>();

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
        Vec3 from = player.getEyePosition();
        Vec3 d = skeleton.position().add(0, 1.0, 0).subtract(from);
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        player.teleport(new TeleportTransition(player.level().getServer().overworld(), player.position(), Vec3.ZERO,
                yaw, pitch, TeleportTransition.DO_NOTHING));
    }

    static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Integer>> it = TOURS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }
            int ticks = entry.getValue() + 1;
            entry.setValue(ticks);
            for (Stop stop : STOPS) {
                if (ticks == stop.tick) {
                    player.teleport(new TeleportTransition(server.overworld(), stop.pos, Vec3.ZERO,
                            stop.yaw, stop.pitch, TeleportTransition.DO_NOTHING));
                }
            }
        }
    }

    private SmokeTour() {}
}
