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
        check(failures, !level.getBlockState(new BlockPos(38, 56, 61)).isAir(), "no gap in the bridge");
        check(failures, level.getBiome(new BlockPos(2, 65, 2)).is(ModBiomes.LOWER_PRISON), "prison biome in the cell");
        check(failures, level.getBiome(new BlockPos(38, 58, 50)).is(ModBiomes.SKY_ABYSS), "sky biome over the bridge");
        check(failures, level.getBlockState(new BlockPos(0, LowerPrison.FLOOR, 5)).is(Blocks.IRON_BARS), "cell bars");
        AABB area = new AABB(-12, 52, -16, 56, 94, 27);
        int keepers = level.getEntitiesOfClass(LamplighterEntity.class, area).size();
        check(failures, keepers == 1, "exactly one lamplighter (found " + keepers + ")");
        int wardens = level.getEntitiesOfClass(com.replecant.skytest.entity.PrisonWardenEntity.class,
                new AABB(-20, -10, -24, 64, 100, 96)).size();
        check(failures, wardens == 1, "exactly one prison warden on the bridge (found " + wardens + ")");
        int rats = level.getEntitiesOfClass(com.replecant.skytest.entity.RatEntity.class, area, r -> !r.isNest()).size();
        check(failures, rats > 0 && rats <= com.replecant.skytest.world.RatSpawner.MAX_RATS, "a few rats (found " + rats + ")");
        int regulators = level.getEntitiesOfClass(com.replecant.skytest.entity.RegulatorEntity.class,
                new AABB(-20, -10, -24, 70, 100, 148)).size();
        check(failures, regulators == 1, "exactly one regulator (found " + regulators + ")");
        check(failures, level.getBlockState(new BlockPos(38, 66, 110)).isAir(), "regulator hall is hollow");
        check(failures, level.getBlockState(new BlockPos(22, 59, 100)).is(Blocks.LEVER), "levers on the hall wall");
        check(failures, level.getBlockState(new BlockPos(38, 58, 90)).isAir(), "entrance from the bridge");
        check(failures, level.getBlockState(new BlockPos(7, LowerPrison.FLOOR, 5))
                .getValue(net.minecraft.world.level.block.CrossCollisionBlock.EAST), "cell bars join the closed gate");
        // Души вместо опыта: смерть крысы выпускает сгусток души.
        var rat = com.replecant.skytest.registry.ModEntities.RAT.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (rat != null) {
            rat.snapTo(38.5, 57.0, 50.5, 0.0F, 0.0F);
            level.addFreshEntity(rat);
            rat.hurtServer(level, level.damageSources().generic(), 100.0F);
            int orbs = level.getEntitiesOfClass(com.replecant.skytest.entity.SoulOrbEntity.class, rat.getBoundingBox().inflate(4.0)).size();
            check(failures, orbs == 1, "rat releases one soul orb (found " + orbs + ")");
        }
        // Мост сплошной (кроме провала) и над ним ничего не нависает.
        int holes = 0;
        int blocked = 0;
        for (int z = 38; z <= 80; z++) {
            if (level.getBlockState(new BlockPos(38, 56, z)).isAir()) {
                holes++;
            }
            if (!level.getBlockState(new BlockPos(38, 57, z)).isAir() || !level.getBlockState(new BlockPos(38, 58, z)).isAir()) {
                blocked++;
            }
        }
        check(failures, holes == 0 && blocked == 0, "bridge deck solid and clear (holes " + holes + ", blocked " + blocked + ")");
        check(failures, level.getBlockState(new BlockPos(97, LowerPrison.UPPER_FLOOR + 4, LowerPrison.UPPER_LANE)).isAir(),
                "upper tier hall is hollow");
        check(failures, level.getBlockState(new BlockPos(96, LowerPrison.UPPER_FLOOR, 66)).is(ModBlocks.PRISON_GATE.get()),
                "upper tier cell gates");
        boolean barred = false;
        for (int x = 60; x < 88; x++) {
            barred |= level.getBlockState(new BlockPos(x, LowerPrison.UPPER_FLOOR + 2, LowerPrison.UPPER_LANE)).is(Blocks.IRON_BARS);
        }
        check(failures, barred, "upper tier entrance is barred until the bridge is moved");
        check(failures, level.getBlockState(new BlockPos(-8, 61, 7)).is(Blocks.DEEPSLATE_BRICK_STAIRS), "stairs to the lower cells");
        check(failures, level.getBlockState(new BlockPos(0, LowerPrison.LOWER_FLOOR + 1, 12)).isAir(), "lower cells corridor");
        int catchers = level.getEntitiesOfClass(com.replecant.skytest.entity.RatCatcherEntity.class, area).size();
        check(failures, catchers == 1, "exactly one rat catcher (found " + catchers + ")");
        var nest = level.getEntitiesOfClass(com.replecant.skytest.entity.RatEntity.class, LowerPrison.RAT_ROOM.inflate(2.0));
        long larpis = nest.stream().filter(com.replecant.skytest.entity.RatEntity::isLarpi).count();
        check(failures, larpis == 1 && nest.size() >= 10, "rat room: " + nest.size() + " rats, larpi " + larpis);
        check(failures, level.getBlockState(new BlockPos(18, LowerPrison.LOWER_FLOOR, 14)).isAir()
                && level.getBlockState(new BlockPos(16, LowerPrison.LOWER_FLOOR - 1, 17)).is(Blocks.SPRUCE_TRAPDOOR)
                && level.getBlockState(new BlockPos(16, LowerPrison.LOWER_FLOOR, 17)).is(Blocks.BROWN_CARPET),
                "digger's cell: bent bars and a trapdoor hidden under the bedroll");
        boolean stash = false;
        for (BlockPos pos : BlockPos.betweenClosed((int) LowerPrison.DIGGERS_STASH.minX, (int) LowerPrison.DIGGERS_STASH.minY,
                (int) LowerPrison.DIGGERS_STASH.minZ, (int) LowerPrison.DIGGERS_STASH.maxX, (int) LowerPrison.DIGGERS_STASH.maxY,
                (int) LowerPrison.DIGGERS_STASH.maxZ)) {
            if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                int potions = 0;
                boolean bread = false;
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    potions += chest.getItem(i).is(net.minecraft.world.item.Items.POTION) ? 1 : 0;
                    bread |= chest.getItem(i).is(net.minecraft.world.item.Items.BREAD);
                }
                stash |= potions == 2 && bread;
            }
        }
        check(failures, stash, "digger's stash: 2 potions and bread");
        for (int yy : new int[] {47, 49, 52}) {
            StringBuilder row = new StringBuilder();
            for (int z = 12; z <= 56; z++) {
                var st = level.getBlockState(new BlockPos(-16, yy, z));
                row.append(st.isAir() ? '.' : st.is(Blocks.DEEPSLATE) || st.is(Blocks.COBBLED_DEEPSLATE) || st.is(Blocks.TUFF) ? '#'
                        : st.is(Blocks.DEEPSLATE_BRICKS) ? 'B' : 'x');
            }
            SkyTest.LOGGER.info("SKYTEST PROFILE x=-16 y={} z12..56 {}", yy, row);
        }
        // Разрушенный ярус: три этажа, броня стража в тайниках, скелеты-узники.
        check(failures, level.getBlockState(new BlockPos(-27, LowerPrison.ATRIUM_Y + 3, 64)).isAir()
                && level.getBlockState(new BlockPos(-27, LowerPrison.GARDEN_Y + 2, 60)).isAir(), "ruined tier is hollow");
        check(failures, level.getBlockState(new BlockPos(-20, LowerPrison.FLOODED_Y, 58)).is(Blocks.WATER), "flooded floor has water");
        java.util.Set<net.minecraft.world.item.Item> armor = new java.util.HashSet<>();
        AABB tier = LowerPrison.RUINED_TIER;
        for (BlockPos pos : BlockPos.betweenClosed((int) tier.minX, (int) tier.minY, (int) tier.minZ, (int) tier.maxX, (int) tier.maxY, (int) tier.maxZ)) {
            if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    armor.add(chest.getItem(i).getItem());
                }
            }
        }
        check(failures, armor.contains(ModItems.WARDEN_TURBAN.get()) && armor.contains(ModItems.WARDEN_CHESTPLATE.get())
                && armor.contains(ModItems.WARDEN_LEGGINGS.get()), "warden armor stashed in the ruined tier");
        int skeletons = level.getEntitiesOfClass(com.replecant.skytest.entity.PrisonSkeletonEntity.class, tier).size();
        check(failures, skeletons == 6, "prisoner skeletons in the ruined tier (found " + skeletons + ")");
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
