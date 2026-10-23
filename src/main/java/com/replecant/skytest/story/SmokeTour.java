package com.replecant.skytest.story;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Для визуальной проверки на CI: водит игрока по точкам обзора локации. */
public final class SmokeTour {
    public static final boolean ENABLED = "true".equals(System.getenv("SKYTEST_CLIENT_SMOKE"));

    private record Stop(int tick, Vec3 pos, float yaw, float pitch) {}

    private static final Stop[] STOPS = {
            new Stop(200, new Vec3(2.0, 64, 1.2), -40.0F, -12.0F),   // Фонарщик анфас
            new Stop(260, new Vec3(0.6, 64, 3.2), -90.0F, -8.0F),    // Фонарщик сбоку
            new Stop(290, new Vec3(0.6, 64, 3.2), -90.0F, -8.0F),    // он же под ночным зрением
            new Stop(350, new Vec3(-3.5, 64, 7.5), -90.0F, 0.0F),    // коридор
            new Stop(410, new Vec3(46.0, 66, 13.0), 135.0F, -12.0F), // зал с уступа
            new Stop(470, new Vec3(34.5, 74, -6.0), 180.0F, -20.0F), // конечная комната
    };

    private static final Map<UUID, Integer> TOURS = new HashMap<>();

    static void begin(ServerPlayer player) {
        if (ENABLED) {
            TOURS.put(player.getUUID(), 0);
        }
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
                    // Третья остановка — Фонарщик под ночным зрением, чтобы оценить силуэт модели.
                    if (stop == STOPS[2]) {
                        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, false, false));
                    } else if (stop == STOPS[3]) {
                        player.removeEffect(MobEffects.NIGHT_VISION);
                    }
                }
            }
        }
    }

    private SmokeTour() {}
}
