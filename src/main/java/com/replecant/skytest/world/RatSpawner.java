package com.replecant.skytest.world;

import java.util.List;

import com.replecant.skytest.entity.RatEntity;
import com.replecant.skytest.registry.ModEntities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Крысы тюрьмы: немного и только там, куда игрок может дойти.
 * Убитые постепенно восполняются в норах подальше от глаз игрока.
 */
public final class RatSpawner {
    /** Не больше стольких крыс на всю тюрьму. */
    public static final int MAX_RATS = 6;
    private static final int INTERVAL_TICKS = 600;
    private static final double MIN_PLAYER_DISTANCE = 12.0;
    private static final AABB PRISON = new AABB(-12, 52, -16, 56, 94, 27);

    /** «Норы» — места появления крыс (все доступны игроку). */
    static final List<Vec3> BURROWS = List.of(
            new Vec3(-3.5, 64, 7.5),    // западный конец коридора
            new Vec3(14.5, 64, 7.5),    // коридор
            new Vec3(26.5, 64, 6.5),    // коридор у зала
            new Vec3(9.5, 64, 12.5),    // открытая камера S2
            new Vec3(33.5, 64, 2.5),    // угол зала
            new Vec3(35.5, 64, 11.5),   // зал у спуска
            new Vec3(38.5, 57, 23.5));  // площадка внизу спуска

    /** Начальное население при постройке тюрьмы. */
    static void populate(ServerLevel level) {
        for (int i = 0; i < BURROWS.size() && i < MAX_RATS - 1; i++) {
            spawnAt(level, BURROWS.get(i));
        }
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % INTERVAL_TICKS != 0) {
            return;
        }
        int alive = level.getEntitiesOfClass(RatEntity.class, PRISON, rat -> !rat.isNest()).size();
        if (alive >= MAX_RATS) {
            return;
        }
        // Нора вне поля зрения: подальше от всех игроков.
        List<Vec3> hidden = BURROWS.stream()
                .filter(pos -> level.isLoaded(net.minecraft.core.BlockPos.containing(pos)))
                .filter(pos -> level.players().stream().allMatch(p -> p.position().distanceTo(pos) > MIN_PLAYER_DISTANCE))
                .toList();
        if (!hidden.isEmpty()) {
            spawnAt(level, hidden.get(level.getRandom().nextInt(hidden.size())));
        }
    }

    private static void spawnAt(ServerLevel level, Vec3 pos) {
        RatEntity rat = ModEntities.RAT.get().create(level, EntitySpawnReason.EVENT);
        if (rat != null) {
            rat.snapTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
            rat.setPersistenceRequired();
            level.addFreshEntity(rat);
        }
    }

    private RatSpawner() {}
}
