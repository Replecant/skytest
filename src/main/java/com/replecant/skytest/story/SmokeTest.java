package com.replecant.skytest.story;

import java.util.ArrayList;
import java.util.List;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.entity.LamplighterEntity;
import com.replecant.skytest.entity.StrangerEntity;
import com.replecant.skytest.registry.ModBlocks;
import com.replecant.skytest.registry.ModItems;
import com.replecant.skytest.world.AdventureWorld;
import com.replecant.skytest.world.LowerPrison;
import com.replecant.skytest.world.ModBiomes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
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
        check(failures, gateOpen(level, new BlockPos(2, LowerPrison.FLOOR, 5), true), "player cell gate open");
        check(failures, gateOpen(level, new BlockPos(8, LowerPrison.FLOOR, 5), false), "N2 gate closed");
        check(failures, gateOpen(level, new BlockPos(8, LowerPrison.FLOOR, 9), true), "S2 (boots) gate open");
        check(failures, level.getBlockState(new BlockPos(38, 57, 21)).is(Blocks.DEEPSLATE_BRICK_STAIRS), "descent stairs");
        check(failures, level.getBlockState(new BlockPos(38, 58, 30)).isAir(), "tunnel to the bridge");
        check(failures, !level.getBlockState(new BlockPos(38, 56, 50)).isAir(), "bridge deck");
        check(failures, level.getBlockState(new BlockPos(38, 56, 61)).isAir(), "gap in the bridge");
        check(failures, level.getBiome(new BlockPos(2, 65, 2)).is(ModBiomes.LOWER_PRISON), "prison biome in the cell");
        check(failures, level.getBiome(new BlockPos(38, 58, 50)).is(ModBiomes.SKY_ABYSS), "sky biome over the bridge");
        check(failures, level.getBlockState(new BlockPos(0, LowerPrison.FLOOR, 5)).is(Blocks.IRON_BARS), "cell bars");
        check(failures, chestHas(level, new BlockPos(8, LowerPrison.FLOOR, 13), ModItems.CIRCUS_BOOTS.get()), "boots chest");
        check(failures, chestHas(level, new BlockPos(45, LowerPrison.FLOOR + 2, 7), ModItems.GRAPPLING_HOOK.get()), "hook chest");
        AABB area = new AABB(-12, 52, -16, 56, 94, 27);
        int keepers = level.getEntitiesOfClass(LamplighterEntity.class, area).size();
        check(failures, keepers == 1, "exactly one lamplighter (found " + keepers + ")");
        int wardens = level.getEntitiesOfClass(com.replecant.skytest.entity.PrisonWardenEntity.class,
                new AABB(-20, -10, -24, 64, 100, 96)).size();
        check(failures, wardens == 1, "exactly one prison warden on the bridge (found " + wardens + ")");
        int rats = level.getEntitiesOfClass(com.replecant.skytest.entity.RatEntity.class, area).size();
        check(failures, rats > 0 && rats <= com.replecant.skytest.world.RatSpawner.MAX_RATS, "a few rats (found " + rats + ")");
        int strangers = level.getEntitiesOfClass(StrangerEntity.class, area).size();
        check(failures, strangers == 1, "exactly one stranger (found " + strangers + ")");

        if (failures.isEmpty()) {
            SkyTest.LOGGER.info("SKYTEST SMOKE OK");
        } else {
            failures.forEach(f -> SkyTest.LOGGER.error("SKYTEST SMOKE FAIL: {}", f));
        }
        server.halt(false);
    }

    private static boolean gateOpen(ServerLevel level, BlockPos pos, boolean open) {
        var lower = level.getBlockState(pos);
        var upper = level.getBlockState(pos.above());
        return lower.is(ModBlocks.PRISON_GATE.get()) && upper.is(ModBlocks.PRISON_GATE.get())
                && lower.getValue(DoorBlock.OPEN) == open;
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
