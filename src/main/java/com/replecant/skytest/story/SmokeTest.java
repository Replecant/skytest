package com.replecant.skytest.story;

import java.util.ArrayList;
import java.util.List;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.entity.LamplighterEntity;
import com.replecant.skytest.registry.ModItems;
import com.replecant.skytest.world.AdventureWorld;
import com.replecant.skytest.world.LowerPrison;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;

/** Проверка на CI: запускает выделенный сервер, проверяет мир и выключает его. */
final class SmokeTest {
    static void run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        List<String> failures = new ArrayList<>();
        check(failures, AdventureWorld.isAdventure(level), "overworld uses the depths dimension type");
        check(failures, LowerPrison.isBuilt(level), "prison marker present");
        check(failures, level.getBlockState(BlockPos.containing(LowerPrison.PLAYER_SPAWN)).is(Blocks.BROWN_CARPET), "player mat in cell");
        check(failures, level.getBlockState(new BlockPos(2, LowerPrison.FLOOR, 5)).isAir(), "cell door open");
        check(failures, level.getBlockState(new BlockPos(0, LowerPrison.FLOOR, 5)).is(Blocks.IRON_BARS), "cell bars");
        check(failures, chestHas(level, new BlockPos(8, LowerPrison.FLOOR, 13), ModItems.CIRCUS_BOOTS.get()), "boots chest");
        check(failures, chestHas(level, new BlockPos(45, LowerPrison.FLOOR + 2, 7), ModItems.GRAPPLING_HOOK.get()), "hook chest");
        int keepers = level.getEntitiesOfClass(LamplighterEntity.class, new AABB(-12, 56, -16, 52, 94, 22)).size();
        check(failures, keepers == 1, "exactly one lamplighter (found " + keepers + ")");

        if (failures.isEmpty()) {
            SkyTest.LOGGER.info("SKYTEST SMOKE OK");
        } else {
            failures.forEach(f -> SkyTest.LOGGER.error("SKYTEST SMOKE FAIL: {}", f));
        }
        server.halt(false);
    }

    private static boolean chestHas(ServerLevel level, BlockPos pos, net.minecraft.world.item.Item item) {
        return level.getBlockEntity(pos) instanceof ChestBlockEntity chest && chest.getItem(13).is(item);
    }

    private static void check(List<String> failures, boolean ok, String what) {
        if (!ok) {
            failures.add(what);
        }
    }

    private SmokeTest() {}
}
