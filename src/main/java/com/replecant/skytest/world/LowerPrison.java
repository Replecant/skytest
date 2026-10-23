package com.replecant.skytest.world;

import java.util.ArrayList;
import java.util.List;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.entity.LamplighterEntity;
import com.replecant.skytest.registry.ModEntities;
import com.replecant.skytest.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Первая тестовая локация — Нижняя тюрьма. Строится кодом один раз
 * в новом мире приключения.
 *
 * <pre>
 *  z
 *  ^   [N1][N2][N3][N4][N5]           +----------- зал ----------+
 *  |   ===== коридор =====  -> проём  |  балкон (крюк)   уступ   |
 *  |   [S1][S2][S3][S4][S5]           |                (2 блока) |
 *  +--> x                             +--------------------------+
 * </pre>
 * N1 — камера игрока; в S2 открыта решётка и лежат Сапоги циркача;
 * на уступе зала — Крюк-хват; с балкона ход в конечную комнату.
 */
public final class LowerPrison {
    public static final int FLOOR = 64;
    public static final Vec3 PLAYER_SPAWN = new Vec3(1.5, FLOOR, 1.5);
    public static final float PLAYER_YAW = -53.0F;
    public static final Vec3 LAMPLIGHTER_POS = new Vec3(3.5, FLOOR, 3.0);
    public static final float LAMPLIGHTER_YAW = 127.0F;
    /** Конечная комната тестовой локации. */
    public static final AABB END_ROOM = new AABB(30, 74, -12, 39, 85, -5);

    private static final BlockPos MARKER = new BlockPos(-12, 56, -16);
    private static final int[] CELL_X = {0, 6, 12, 18, 24};

    private final ServerLevel level;
    private final RandomSource random = RandomSource.create(1337L);
    private final List<BlockPos> connectables = new ArrayList<>();

    private LowerPrison(ServerLevel level) {
        this.level = level;
    }

    public static boolean isBuilt(ServerLevel level) {
        return level.getBlockState(MARKER).is(Blocks.BARRIER);
    }

    public static void build(ServerLevel level) {
        long start = System.currentTimeMillis();
        new LowerPrison(level).buildAll();
        level.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, BlockPos.containing(PLAYER_SPAWN), PLAYER_YAW, 0.0F));
        SkyTest.LOGGER.info("Lower Prison built in {} ms", System.currentTimeMillis() - start);
    }

    private void buildAll() {
        // Скальный массив, в котором вырублено всё остальное.
        fillRock(-12, 56, -16, 52, 94, 22);
        set(MARKER, Blocks.BARRIER.defaultBlockState());

        // Камеры и коридор.
        for (int x : CELL_X) {
            room(x, FLOOR, 0, x + 4, FLOOR + 3, 4);
            room(x, FLOOR, 10, x + 4, FLOOR + 3, 14);
        }
        room(-6, FLOOR, 6, 29, FLOOR + 4, 8);
        for (int x : CELL_X) {
            bars(x, 5, x == 0 ? 2 : Integer.MIN_VALUE);
            bars(x, 9, x == 6 ? 8 : Integer.MIN_VALUE);
        }

        // Зал.
        room(31, FLOOR, 0, 47, FLOOR + 15, 14);
        fill(30, FLOOR, 6, 30, FLOOR + 2, 8, Blocks.AIR.defaultBlockState()); // проём из коридора
        decorateHall();

        // Ход с балкона и конечная комната.
        fill(33, 74, -5, 35, 76, -1, Blocks.AIR.defaultBlockState());
        room(31, 74, -12, 38, 84, -6);
        fill(33, 74, -6, 35, 76, -6, Blocks.AIR.defaultBlockState());
        for (int y = 74; y <= 84; y++) {
            set(new BlockPos(34, y, -9), Blocks.END_ROD.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        }

        decorateCorridor();
        decorateCells();
        decoratePlayerCell();

        // Дорисовать соединения решёток и цепей.
        for (BlockPos pos : connectables) {
            BlockState state = level.getBlockState(pos);
            set(pos, Block.updateFromNeighbourShapes(state, level, pos));
        }

        spawnLamplighter();
    }

    // ---------------------------------------------------------------- геометрия

    private void fillRock(int x0, int y0, int z0, int x1, int y1, int z1) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    float r = random.nextFloat();
                    BlockState state = r < 0.70F ? Blocks.DEEPSLATE.defaultBlockState()
                            : r < 0.85F ? Blocks.COBBLED_DEEPSLATE.defaultBlockState()
                            : Blocks.TUFF.defaultBlockState();
                    set(pos.set(x, y, z), state);
                }
            }
        }
    }

    /** Комната: кладка вокруг, плитка на полу, пустота внутри (границы включительно). */
    private void room(int x0, int y0, int z0, int x1, int y1, int z1) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int y = y0 - 1; y <= y1 + 1; y++) {
                for (int z = z0 - 1; z <= z1 + 1; z++) {
                    boolean inside = x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
                    pos.set(x, y, z);
                    if (inside) {
                        set(pos, Blocks.AIR.defaultBlockState());
                    } else if (level.getBlockState(pos).isAir()) {
                        continue; // не замуровываем соседние комнаты
                    } else if (y == y0 - 1) {
                        set(pos, pick(Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, 0.2F));
                    } else {
                        set(pos, pick(Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS, 0.25F));
                    }
                }
            }
        }
    }

    /** Решётка камеры по линии z; gapX — открытая дверь (или MIN_VALUE). */
    private void bars(int cellX, int z, int gapX) {
        for (int x = cellX; x <= cellX + 4; x++) {
            for (int y = FLOOR; y <= FLOOR + 2; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (x == gapX && y <= FLOOR + 1) {
                    set(pos, Blocks.AIR.defaultBlockState());
                } else {
                    connectable(pos, Blocks.IRON_BARS.defaultBlockState());
                }
            }
        }
    }

    private void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (BlockPos pos : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
            set(pos, state);
        }
    }

    // ---------------------------------------------------------------- декор

    private void decoratePlayerCell() {
        // Подстилка, на которой просыпается игрок.
        fill(0, FLOOR, 1, 1, FLOOR, 2, Blocks.BROWN_CARPET.defaultBlockState());
        set(new BlockPos(0, FLOOR, 3), Blocks.HAY_BLOCK.defaultBlockState());
        cobweb(0, FLOOR + 3, 0);
        cobweb(4, FLOOR + 3, 0);
        chain(2, FLOOR + 3, 0, 2);
        // Свет «глаза» Фонарщика.
        set(new BlockPos(3, FLOOR + 2, 3), Blocks.LIGHT.defaultBlockState().setValue(BlockStateProperties.LEVEL, 12));
    }

    private void decorateCells() {
        for (int i = 1; i < CELL_X.length; i++) {
            int x = CELL_X[i];
            remains(x + 1 + random.nextInt(3), 1 + random.nextInt(2));
            cobweb(x + random.nextInt(5), FLOOR + 3, 0);
            if (i % 2 == 0) {
                chain(x + 2, FLOOR + 3, 2, 2);
            }
        }
        for (int i = 0; i < CELL_X.length; i++) {
            int x = CELL_X[i];
            if (i != 1) {
                remains(x + 1 + random.nextInt(3), 12 + random.nextInt(2));
            }
            cobweb(x + random.nextInt(5), FLOOR + 3, 14);
        }
        // Обрушенная камера N3.
        fill(12, FLOOR, 0, 14, FLOOR + 1, 2, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        set(new BlockPos(12, FLOOR + 2, 0), Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        set(new BlockPos(16, FLOOR + 3, 4), Blocks.HANGING_ROOTS.defaultBlockState());
        // Открытая камера S2 — сапоги циркача.
        chest(8, FLOOR, 13, Direction.NORTH, new ItemStack(ModItems.CIRCUS_BOOTS.get()));
        set(new BlockPos(6, FLOOR, 14), Blocks.SKELETON_SKULL.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 6));
        fill(9, FLOOR, 14, 10, FLOOR, 14, Blocks.RED_CARPET.defaultBlockState());
    }

    private void decorateCorridor() {
        for (int x = -3; x <= 27; x += 5) {
            hangingLantern(x, FLOOR + 4, 7, 0);
        }
        // Завал в западном тупике.
        fill(-6, FLOOR, 6, -5, FLOOR + 2, 8, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        fill(-4, FLOOR, 6, -4, FLOOR, 8, Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState());
        cobweb(-4, FLOOR + 4, 6);
        cobweb(29, FLOOR + 4, 8);
    }

    private void decorateHall() {
        // Уступ в 2 блока: обычным прыжком не забраться, двойным — можно.
        fill(41, FLOOR, 0, 47, FLOOR + 1, 14, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        fill(41, FLOOR + 1, 0, 41, FLOOR + 1, 14, Blocks.DEEPSLATE_TILES.defaultBlockState());
        chest(45, FLOOR + 2, 7, Direction.WEST, new ItemStack(ModItems.GRAPPLING_HOOK.get()));
        set(new BlockPos(44, FLOOR + 2, 3), Blocks.SOUL_LANTERN.defaultBlockState());
        set(new BlockPos(44, FLOOR + 2, 11), Blocks.SOUL_LANTERN.defaultBlockState());

        // Балкон под потолком: добраться только крюком.
        fill(31, 72, 0, 40, 73, 3, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        fill(31, 74, 3, 40, 74, 3, Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState());
        fill(33, 74, 3, 35, 74, 3, Blocks.AIR.defaultBlockState());
        for (int x = 31; x <= 40; x++) {
            connectables.add(new BlockPos(x, 74, 3));
        }
        set(new BlockPos(32, 74, 1), Blocks.SOUL_LANTERN.defaultBlockState());

        // Колонны и свет.
        for (int z : new int[] {0, 14}) {
            fill(36, FLOOR, z, 36, FLOOR + 15, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        }
        hangingLantern(35, 79, 8, 5);
        hangingLantern(39, 79, 10, 3);
        hangingLantern(33, 79, 12, 6);
        hangingLantern(44, 79, 4, 4);
        hangingLantern(44, 79, 11, 7);
        for (int z : new int[] {4, 10}) {
            set(new BlockPos(31, FLOOR, z), Blocks.SOUL_CAMPFIRE.defaultBlockState());
        }
        fill(31, FLOOR, 13, 33, FLOOR, 14, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        remains(32, 10);
    }

    private void remains(int x, int z) {
        BlockPos pos = new BlockPos(x, FLOOR, z);
        set(pos, Blocks.SKELETON_SKULL.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, random.nextInt(16)));
        BlockPos bone = pos.offset(random.nextBoolean() ? 1 : -1, 0, 0);
        if (level.getBlockState(bone).isAir()) {
            set(bone, Blocks.BONE_BLOCK.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        }
    }

    private void cobweb(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).isAir()) {
            set(pos, Blocks.COBWEB.defaultBlockState());
        }
    }

    /** Цепь, свисающая с потолка (y — самый верхний блок цепи). */
    private void chain(int x, int y, int z, int length) {
        for (int i = 0; i < length; i++) {
            connectable(new BlockPos(x, y - i, z), Blocks.IRON_CHAIN.defaultBlockState());
        }
    }

    /** Фонарь на цепи: y — блок под потолком, chainLength — сколько цепи над фонарём. */
    private void hangingLantern(int x, int y, int z, int chainLength) {
        chain(x, y, z, chainLength);
        set(new BlockPos(x, y - chainLength, z), Blocks.SOUL_LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
    }

    private void chest(int x, int y, int z, Direction facing, ItemStack loot) {
        BlockPos pos = new BlockPos(x, y, z);
        set(pos, Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            chest.setItem(13, loot);
        }
    }

    private void spawnLamplighter() {
        AABB area = new AABB(-12, 56, -16, 52, 94, 22);
        level.getEntitiesOfClass(LamplighterEntity.class, area).forEach(LamplighterEntity::discard);
        LamplighterEntity keeper = ModEntities.LAMPLIGHTER.get().create(level, EntitySpawnReason.STRUCTURE);
        if (keeper != null) {
            keeper.snapTo(LAMPLIGHTER_POS.x, LAMPLIGHTER_POS.y, LAMPLIGHTER_POS.z, LAMPLIGHTER_YAW, 0.0F);
            keeper.setYHeadRot(LAMPLIGHTER_YAW);
            keeper.setYBodyRot(LAMPLIGHTER_YAW);
            level.addFreshEntity(keeper);
        }
    }

    // ---------------------------------------------------------------- утилиты

    private BlockState pick(Block common, Block rare, float rareChance) {
        return (random.nextFloat() < rareChance ? rare : common).defaultBlockState();
    }

    private void connectable(BlockPos pos, BlockState state) {
        set(pos, state);
        connectables.add(pos.immutable());
    }

    private void set(BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
}
