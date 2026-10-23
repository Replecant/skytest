package com.replecant.skytest.world;

import java.util.ArrayList;
import java.util.List;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.entity.LamplighterEntity;
import com.replecant.skytest.entity.PrisonWardenEntity;
import com.replecant.skytest.entity.RatEntity;
import com.replecant.skytest.entity.StrangerEntity;
import com.replecant.skytest.registry.ModBlocks;
import com.replecant.skytest.registry.ModEntities;
import com.replecant.skytest.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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
 * Камеры закрыты тюремными воротами; открыты только N1 (камера игрока)
 * и S2 (там лежат Сапоги циркача). На уступе зала — Крюк-хват и Незнакомец,
 * в южной стене зала — спуск вниз, с балкона — ход в конечную комнату.
 */
public final class LowerPrison {
    public static final int FLOOR = 64;
    public static final Vec3 PLAYER_SPAWN = new Vec3(1.5, FLOOR, 1.5);
    public static final float PLAYER_YAW = -53.0F;
    public static final Vec3 LAMPLIGHTER_POS = new Vec3(3.5, FLOOR, 3.0);
    public static final float LAMPLIGHTER_YAW = 127.0F;
    /** Конечная комната тестовой локации. */
    public static final AABB END_ROOM = new AABB(30, 74, -12, 39, 85, -5);
    /** Зал: при входе сюда с игроком заговаривает Незнакомец. */
    public static final AABB HALL = new AABB(31, 63, 0, 48, 71, 15);
    public static final Vec3 STRANGER_POS = new Vec3(45.5, FLOOR + 2, 9.5);
    public static final float STRANGER_YAW = 90.0F;
    /** Проход в восточной стене, куда убегает Незнакомец. */
    public static final Vec3 STRANGER_EXIT = new Vec3(51.5, FLOOR + 2, 10.5);
    /** Низ спуска, откуда поднимается скелет. */
    public static final Vec3 DESCENT_BOTTOM = new Vec3(38.5, 57, 23.5);
    /** Начало моста над бездной — сюда возвращает падение в пропасть. */
    public static final Vec3 BRIDGE_START = new Vec3(38.5, 57, 33.5);
    public static final AABB BRIDGE = new AABB(32, 55, 34, 45, 62, 92);
    /** Страж патрулирует мост между тоннелем и провалом. */
    public static final Vec3 WARDEN_POS = new Vec3(38.5, 57, 47.5);
    /** Зал Регулятора во второй скале, куда ведёт мост. */
    public static final AABB REGULATOR_HALL = new AABB(21, 55, 97, 56, 83, 134);
    public static final Vec3 REGULATOR_POS = new Vec3(38.5, 58, 126.5);
    /** Ниже этой высоты игрок считается упавшим в пропасть. */
    public static final double ABYSS_Y = 10.0;
    private static final int BRIDGE_Y = 56;
    /** Насколько глубоко (в среднем) свисает скала под островом. */
    private static final double ISLAND_HANG = 44.0;
    private static final int GAP_START = 60;
    private static final int GAP_END = 62;
    private static final AABB WHOLE_AREA = new AABB(-40, -10, -40, 86, 106, 160);
    /** Всё, что внутри этой коробки, — биом тюрьмы (тёмный туман); снаружи — небо над бездной. */
    private static final AABB PRISON_BIOME = new AABB(-16, 48, -20, 60, 99, 26);
    private static final AABB REGULATOR_BIOME = new AABB(10, 40, 96, 66, 95, 146);

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

    /** Стереть всё построенное (для /skytest rebuild): старая скала не должна остаться под новой. */
    public static void clear(ServerLevel level) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed((int) WHOLE_AREA.minX, (int) WHOLE_AREA.minY, (int) WHOLE_AREA.minZ,
                (int) WHOLE_AREA.maxX, (int) WHOLE_AREA.maxY, (int) WHOLE_AREA.maxZ)) {
            if (!level.getBlockState(pos).isAir()) {
                level.removeBlockEntity(pos);
                level.setBlock(pos, air, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    public static void build(ServerLevel level) {
        long start = System.currentTimeMillis();
        new LowerPrison(level).buildAll();
        level.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, BlockPos.containing(PLAYER_SPAWN), PLAYER_YAW, 0.0F));
        SkyTest.LOGGER.info("Lower Prison built in {} ms", System.currentTimeMillis() - start);
    }

    private void buildAll() {
        // Парящая скала, в которой вырублено всё остальное.
        buildIsland(-12, 56, -16, 27, 52, 94, 0);
        buildIsland(14, 62, 92, 140, 50, 88, 10);
        set(MARKER, Blocks.BARRIER.defaultBlockState());

        // Камеры и коридор.
        for (int x : CELL_X) {
            room(x, FLOOR, 0, x + 4, FLOOR + 3, 4);
            room(x, FLOOR, 10, x + 4, FLOOR + 3, 14);
        }
        room(-6, FLOOR, 6, 29, FLOOR + 4, 8);
        for (int x : CELL_X) {
            // Ворота открыты только у камеры игрока и у камеры с сапогами.
            bars(x, 5, Direction.NORTH, x == 0);
            bars(x, 9, Direction.SOUTH, x == 6);
        }

        // Зал.
        room(31, FLOOR, 0, 47, FLOOR + 15, 14);
        fill(30, FLOOR, 6, 30, FLOOR + 2, 8, Blocks.AIR.defaultBlockState()); // проём из коридора
        decorateHall();

        buildDescent();
        buildStrangerExit();
        buildBridge();
        buildRegulatorHall();

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

        spawnNpcs();
        paintBiomes();
    }

    // ---------------------------------------------------------------- геометрия

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

    /** Решётка камеры по линии z с тюремными воротами посередине. */
    private void bars(int cellX, int z, Direction facing, boolean open) {
        int gateX = cellX + 2;
        for (int x = cellX; x <= cellX + 4; x++) {
            for (int y = FLOOR; y <= FLOOR + 2; y++) {
                if (x == gateX && y <= FLOOR + 1) {
                    continue;
                }
                connectable(new BlockPos(x, y, z), Blocks.IRON_BARS.defaultBlockState());
            }
        }
        gate(new BlockPos(gateX, FLOOR, z), facing, open);
    }

    private void gate(BlockPos pos, Direction facing, boolean open) {
        BlockState gate = ModBlocks.PRISON_GATE.get().defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                .setValue(DoorBlock.OPEN, open);
        set(pos, gate.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(pos.above(), gate.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
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
        // Огонь душ вместо костров: тот же свет, но без столбов дыма из частиц.
        for (int z : new int[] {4, 10}) {
            set(new BlockPos(31, FLOOR - 1, z), Blocks.SOUL_SOIL.defaultBlockState());
            set(new BlockPos(31, FLOOR, z), Blocks.SOUL_FIRE.defaultBlockState());
        }
        fill(31, FLOOR, 13, 33, FLOOR, 14, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        remains(32, 10);
    }

    /** Спуск вниз в южной стене зала: лестница во тьму, внизу завал. */
    private void buildDescent() {
        fill(36, 56, 15, 40, 68, 25, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        for (int i = 0; i <= 6; i++) {
            int z = 15 + i;
            int y = 63 - i;
            fill(37, y + 1, z, 39, y + 4, z, Blocks.AIR.defaultBlockState());
            fill(37, y, z, 39, y, z, Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        }
        // Площадка внизу и тоннель наружу — к мосту над бездной.
        fill(37, 57, 22, 39, 60, 24, Blocks.AIR.defaultBlockState());
        fill(37, 56, 22, 39, 56, 24, pick(Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, 0.4F));
        set(new BlockPos(37, 57, 24), Blocks.SOUL_LANTERN.defaultBlockState());
        cobweb(39, 60, 22);
        cobweb(37, 60, 23);
    }

    // ---------------------------------------------------------------- остров и мост

    /**
     * Парящая скала вокруг прямоугольника {@code minX..maxX × minZ..maxZ}: в плане —
     * скруглённый «суперэллипс» с неровным краем, стены изрезаны объёмным шумом
     * (уступы, навесы), книзу скала сужается в свисающий конус с сосульками.
     */
    private void buildIsland(int minX, int maxX, int minZ, int maxZ, int coreBottom, int coreTop, int seed) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double cx = (minX + maxX) / 2.0;
        double cz = (minZ + maxZ) / 2.0;
        double rx = (maxX - minX) / 2.0 * 1.26 + 4.0;
        double rz = (maxZ - minZ) / 2.0 * 1.26 + 4.0;
        for (int x = (int) (cx - rx - 6); x <= (int) (cx + rx + 6); x++) {
            for (int z = (int) (cz - rz - 6); z <= (int) (cz + rz + 6); z++) {
                double nx = Math.abs(x - cx) / rx;
                double nz = Math.abs(z - cz) / rz;
                double d = Math.cbrt(nx * nx * nx + nz * nz * nz) / (1.0 + 0.24 * (noise(x, z, 11.0, seed + 1) - 0.5));
                if (d > 1.15) {
                    continue;
                }
                int top = coreTop + (int) (4.0 * noise(x, z, 6.0, seed + 2)) - (int) (Math.max(0.0, d - 0.7) * 40.0);
                double hang = ISLAND_HANG * (0.75 + 0.5 * noise(x, z, 9.0, seed + 3));
                boolean spike = noise(x, z, 2.0, seed + 4) > 0.84;
                for (int y = (int) (coreBottom - hang - 14); y <= top; y++) {
                    double limit;
                    double t = (coreBottom - y) / hang;
                    if (y >= coreBottom) {
                        limit = 1.0;
                    } else if (t < 1.0) {
                        limit = Math.pow(1.0 - t, 0.75);
                    } else {
                        limit = -1.0;
                    }
                    double wobble = 0.22 * (noise3(x, y, z, 5.0, seed + 5) - 0.5);
                    boolean icicle = spike && y < coreBottom && t < 1.25 && d < Math.max(limit, 0.0) + 0.12 + wobble;
                    if (d < limit + wobble || icicle) {
                        set(pos.set(x, y, z), rock());
                    }
                }
            }
        }
    }

    /**
     * Портал тоннеля в скальной стене: кирпичная рамка ровно там, где кончается камень,
     * а за ней — несколько слоёв скалы, чтобы рамка не висела в воздухе.
     */
    private void portal(int z, int inward) {
        int y = BRIDGE_Y;
        for (int i = 1; i <= 6; i++) {
            for (int x = 34; x <= 42; x++) {
                for (int dy = -3; dy <= 7; dy++) {
                    BlockPos pos = new BlockPos(x, y + dy, z + i * inward);
                    if (level.getBlockState(pos).isAir()) {
                        set(pos, rock());
                    }
                }
            }
        }
        fill(36, y - 1, z, 40, y + 5, z, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        set(new BlockPos(38, y + 5, z), Blocks.CHISELED_DEEPSLATE.defaultBlockState());
    }

    /** Есть ли камень в проёме тоннеля на данном z. */
    private boolean rockAcross(int z) {
        for (int x = 36; x <= 40; x++) {
            for (int y = BRIDGE_Y + 1; y <= BRIDGE_Y + 5; y++) {
                if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Объёмный шум: двумерный, плавно смешанный по слоям y. */
    private static double noise3(int x, int y, int z, double scale, int seed) {
        double fy = y / scale;
        int iy = (int) Math.floor(fy);
        double ty = fy - iy;
        ty = ty * ty * (3 - 2 * ty);
        double a = noise(x, z + iy * 1031, scale, seed);
        double b = noise(x, z + (iy + 1) * 1031, scale, seed);
        return a + (b - a) * ty;
    }

    /**
     * Мост над бездной: тоннели из обеих скал выходят порталами ровно в каменной
     * стене; между ними — перила, цепи и фонари, провал посередине и руины ближе к залу.
     */
    private void buildBridge() {
        int y = BRIDGE_Y;
        int exitZ = 26;
        for (int z = 26; z < GAP_START; z++) {
            if (rockAcross(z)) {
                exitZ = z;
            }
        }
        int entryZ = 97;
        for (int z = 96; z > GAP_END; z--) {
            if (rockAcross(z)) {
                entryZ = z;
            }
        }
        SkyTest.LOGGER.info("Bridge portals at z={} and z={}", exitZ, entryZ);
        portal(exitZ, -1);
        portal(entryZ, 1);
        // Тоннели сквозь скалу: от спуска к мосту и от моста к залу Регулятора.
        fill(37, y + 1, 25, 39, y + 3, exitZ, Blocks.AIR.defaultBlockState());
        fill(37, y + 1, entryZ, 39, y + 4, 96, Blocks.AIR.defaultBlockState());
        set(new BlockPos(38, y + 4, exitZ), Blocks.DEEPSLATE_BRICKS.defaultBlockState());

        int ruinsStart = GAP_END + 6;
        int ruinsEnd = Math.min(ruinsStart + 7, entryZ - 4);
        for (int z = 25; z <= 96; z++) {
            boolean gap = z >= GAP_START && z <= GAP_END;
            boolean ruins = z >= ruinsStart && z <= ruinsEnd;
            if (gap) {
                continue;
            }
            for (int x = 37; x <= 39; x++) {
                set(new BlockPos(x, y, z), pick(Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, 0.25F));
            }
            if (z <= exitZ || z >= entryZ) {
                continue;
            }
            // Над открытым мостом — ничего лишнего (наплывы скалы, сосульки).
            fill(35, y + 1, z, 41, y + 5, z, Blocks.AIR.defaultBlockState());
            if (ruins) {
                continue;
            }
            // Перила с разрушенными участками рядом с провалом.
            boolean brokenRail = (z >= GAP_START - 3 && z <= GAP_END + 3) && random.nextFloat() < 0.6F;
            if (!brokenRail) {
                connectable(new BlockPos(36, y + 1, z), Blocks.DEEPSLATE_TILE_WALL.defaultBlockState());
                connectable(new BlockPos(40, y + 1, z), Blocks.DEEPSLATE_TILE_WALL.defaultBlockState());
            }
            set(new BlockPos(36, y, z), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            set(new BlockPos(40, y, z), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            // Опоры: балка снизу и свисающие в пропасть цепи; фонари на столбах.
            if (z % 8 == 0 && z > exitZ + 2 && z < entryZ - 2) {
                fill(35, y - 1, z, 41, y - 1, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                for (int x : new int[] {35, 41}) {
                    chain(x, y - 2, z, 4 + random.nextInt(6));
                }
                for (int x : new int[] {36, 40}) {
                    connectable(new BlockPos(x, y + 1, z), Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState());
                    set(new BlockPos(x, y + 2, z), Blocks.SOUL_LANTERN.defaultBlockState());
                }
            }
        }
        // Полуразрушенная площадка на полпути ко второй скале: обломки стен по краям
        // и остов арки; середина свободна для прохода.
        for (int x = 33; x <= 43; x++) {
            for (int z = ruinsStart; z <= ruinsEnd; z++) {
                boolean edge = x == 33 || x == 43;
                if (random.nextFloat() < (edge ? 0.55F : 0.9F) || (x >= 37 && x <= 39)) {
                    set(new BlockPos(x, y, z), pick(Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, 0.4F));
                }
                if (edge && random.nextFloat() < 0.45F) {
                    int height = 1 + random.nextInt(3);
                    fill(x, y + 1, z, x, y + height, z, pick(Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS, 0.5F));
                }
            }
        }
        int arch = (ruinsStart + ruinsEnd) / 2;
        fill(35, y + 1, arch, 35, y + 5, arch, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        fill(41, y + 1, arch, 41, y + 3, arch, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        fill(35, y + 6, arch, 38, y + 6, arch, Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState());
        set(new BlockPos(42, y + 1, arch + 2), Blocks.SOUL_LANTERN.defaultBlockState());
        set(new BlockPos(34, y + 1, arch - 2), Blocks.SOUL_LANTERN.defaultBlockState());
    }

    // ---------------------------------------------------------------- зал Регулятора

    /**
     * Вторая скала и зал Регулятора: множество свисающих цепей, ряды рычагов
     * на стенах, медные механизмы, шестерни, помост, на котором сидит Регулятор.
     */
    private void buildRegulatorHall() {
        int floor = 57;
        room(22, floor, 98, 54, 81, 132);

        // Вход: арка в стене зала (проход сквозь скалу пробит в buildBridge).
        fill(35, floor, 97, 41, floor + 5, 97, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        fill(37, floor, 97, 39, floor + 3, 97, Blocks.AIR.defaultBlockState());
        set(new BlockPos(38, floor + 4, 97), Blocks.CHISELED_DEEPSLATE.defaultBlockState());

        // Пол: полированный глубинный сланец, ковровая дорожка плиткой к помосту.
        fill(22, floor - 1, 98, 54, floor - 1, 132, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        fill(36, floor - 1, 98, 40, floor - 1, 119, Blocks.DEEPSLATE_TILES.defaultBlockState());
        fill(35, floor - 1, 98, 35, floor - 1, 119, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
        fill(41, floor - 1, 98, 41, floor - 1, 119, Blocks.CHISELED_DEEPSLATE.defaultBlockState());

        // Колонны с медными лампами.
        for (int z = 104; z <= 128; z += 8) {
            for (int x : new int[] {26, 50}) {
                fill(x, floor, z, x, 81, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                int inner = x == 26 ? x + 1 : x - 1;
                set(new BlockPos(inner, floor + 6, z), Blocks.WAXED_COPPER_BULB.defaultBlockState()
                        .setValue(BlockStateProperties.LIT, true));
                set(new BlockPos(x, floor + 12, z), Blocks.WAXED_OXIDIZED_CHISELED_COPPER.defaultBlockState());
            }
        }

        // Ряды рычагов и медные решётки на боковых стенах.
        for (int z = 100; z <= 130; z++) {
            for (int side = 0; side < 2; side++) {
                int x = side == 0 ? 22 : 54;
                Direction facing = side == 0 ? Direction.EAST : Direction.WEST;
                int wall = side == 0 ? 21 : 55;
                if (z % 8 == 0) {
                    continue; // у колонн
                }
                for (int y = floor + 8; y <= floor + 11; y++) {
                    set(new BlockPos(wall, y, z), Blocks.WAXED_OXIDIZED_COPPER_GRATE.defaultBlockState());
                }
                if (z % 2 == 0) {
                    for (int y : new int[] {floor + 2, floor + 4}) {
                        set(new BlockPos(x, y, z), Blocks.LEVER.defaultBlockState()
                                .setValue(BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                                .setValue(BlockStateProperties.POWERED, random.nextFloat() < 0.4F));
                    }
                }
            }
        }

        // Огромная шестерня на стене за помостом.
        gear(38, 70, 132, 7);
        gear(28, 66, 132, 3);
        gear(48, 75, 132, 4);

        // Помост с лестницей и жаровнями.
        fill(30, floor, 120, 46, floor, 131, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        fill(34, floor, 119, 42, floor, 119, Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        for (int[] c : new int[][] {{31, 121}, {45, 121}}) {
            set(new BlockPos(c[0], floor, c[1]), Blocks.SOUL_SOIL.defaultBlockState());
            set(new BlockPos(c[0], floor + 1, c[1]), Blocks.SOUL_FIRE.defaultBlockState());
        }
        // Главный рычаг механизма рядом с Регулятором.
        fill(44, floor + 1, 129, 44, floor + 4, 129, Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState());
        for (int y = floor + 1; y <= floor + 4; y++) {
            connectables.add(new BlockPos(44, y, 129));
        }
        set(new BlockPos(44, floor + 5, 129), Blocks.IRON_BLOCK.defaultBlockState());
        set(new BlockPos(43, floor + 5, 129), Blocks.IRON_BLOCK.defaultBlockState());

        // Множество свисающих цепей; часть с фонарями.
        for (int i = 0; i < 70; i++) {
            int x = 23 + random.nextInt(31);
            int z = 99 + random.nextInt(33);
            if (x == 26 || x == 50 || (z >= 118 && x >= 29 && x <= 47)) {
                continue;
            }
            boolean walkway = x >= 34 && x <= 42;
            int maxLength = walkway ? 81 - (floor + 4) : 20;
            int length = 3 + random.nextInt(Math.max(1, maxLength - 3));
            if (!level.getBlockState(new BlockPos(x, 81, z)).isAir()) {
                continue;
            }
            chain(x, 81, z, length);
            if (random.nextFloat() < 0.25F && 81 - length > floor + 2) {
                set(new BlockPos(x, 81 - length, z), Blocks.SOUL_LANTERN.defaultBlockState()
                        .setValue(BlockStateProperties.HANGING, true));
            }
        }
        cobweb(23, 81, 99);
        cobweb(53, 81, 131);
    }

    /** Шестерня-барельеф на стене (плоскость z). */
    private void gear(int cx, int cy, int z, int radius) {
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dy = -radius - 1; dy <= radius + 1; dy++) {
                double r = Math.sqrt(dx * dx + dy * dy);
                double angle = Math.atan2(dy, dx);
                boolean tooth = r > radius - 0.5 && r <= radius + 1.2 && Math.cos(angle * Math.max(6, radius + 3)) > 0.3;
                boolean ring = r > radius - 1.5 && r <= radius - 0.5;
                boolean hub = r <= Math.max(1.0, radius / 3.0);
                boolean spoke = r < radius - 1 && (Math.abs(dx) < 0.6 || Math.abs(dy) < 0.6);
                if (tooth || ring || hub || spoke) {
                    BlockState state = hub ? Blocks.WAXED_OXIDIZED_CUT_COPPER.defaultBlockState()
                            : Blocks.WAXED_WEATHERED_CUT_COPPER.defaultBlockState();
                    set(new BlockPos(cx + dx, cy + dy, z), state);
                }
            }
        }
    }

    /** Окрашивает биомы: внутри скалы — тюрьма, снаружи — небо над бездной. */
    private void paintBiomes() {
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Holder<Biome> prison = biomes.getOrThrow(ModBiomes.LOWER_PRISON);
        Holder<Biome> abyss = biomes.getOrThrow(ModBiomes.SKY_ABYSS);
        int minChunkX = SectionPos.blockToSectionCoord((int) WHOLE_AREA.minX);
        int maxChunkX = SectionPos.blockToSectionCoord((int) WHOLE_AREA.maxX);
        int minChunkZ = SectionPos.blockToSectionCoord((int) WHOLE_AREA.minZ);
        int maxChunkZ = SectionPos.blockToSectionCoord((int) WHOLE_AREA.maxZ);
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                ChunkAccess chunk = level.getChunk(cx, cz);
                chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> {
                    Vec3 center = new Vec3(QuartPos.toBlock(qx) + 2, QuartPos.toBlock(qy) + 2, QuartPos.toBlock(qz) + 2);
                    return PRISON_BIOME.contains(center) || REGULATOR_BIOME.contains(center) ? prison : abyss;
                }, level.getChunkSource().randomState().sampler());
                chunk.markUnsaved();
            }
        }
    }

    /** Гладкий шум 0..1 (значения в узлах сетки, билинейная интерполяция). */
    private static double noise(int x, int z, double scale, int seed) {
        double fx = x / scale;
        double fz = z / scale;
        int ix = (int) Math.floor(fx);
        int iz = (int) Math.floor(fz);
        double tx = fx - ix;
        double tz = fz - iz;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        double a = hash(ix, iz, seed);
        double b = hash(ix + 1, iz, seed);
        double c = hash(ix, iz + 1, seed);
        double d = hash(ix + 1, iz + 1, seed);
        return (a + (b - a) * tx) + ((c + (d - c) * tx) - (a + (b - a) * tx)) * tz;
    }

    private static double hash(int x, int z, int seed) {
        long h = x * 341873128712L + z * 132897987541L + seed * 2654435761L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }

    private BlockState rock() {
        float r = random.nextFloat();
        return r < 0.70F ? Blocks.DEEPSLATE.defaultBlockState()
                : r < 0.85F ? Blocks.COBBLED_DEEPSLATE.defaultBlockState()
                : Blocks.TUFF.defaultBlockState();
    }

    /** Проход в восточной стене зала на уровне уступа. Дальше — обвал: зал «сдвинулся». */
    private void buildStrangerExit() {
        fill(48, FLOOR + 2, 9, 53, FLOOR + 4, 11, Blocks.AIR.defaultBlockState());
        fill(48, FLOOR + 1, 9, 53, FLOOR + 1, 11, Blocks.DEEPSLATE_TILES.defaultBlockState());
        fill(53, FLOOR + 2, 9, 53, FLOOR + 4, 11, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        fill(52, FLOOR + 2, 9, 52, FLOOR + 2, 11, Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState());
        cobweb(51, FLOOR + 4, 9);
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

    private void spawnNpcs() {
        level.getEntitiesOfClass(LamplighterEntity.class, WHOLE_AREA).forEach(LamplighterEntity::discard);
        level.getEntitiesOfClass(StrangerEntity.class, WHOLE_AREA).forEach(StrangerEntity::discard);
        level.getEntitiesOfClass(PrisonWardenEntity.class, WHOLE_AREA).forEach(PrisonWardenEntity::discard);
        level.getEntitiesOfClass(RatEntity.class, WHOLE_AREA).forEach(RatEntity::discard);
        level.getEntitiesOfClass(com.replecant.skytest.entity.RegulatorEntity.class, WHOLE_AREA)
                .forEach(com.replecant.skytest.entity.RegulatorEntity::discard);
        spawn(ModEntities.LAMPLIGHTER.get().create(level, EntitySpawnReason.STRUCTURE), LAMPLIGHTER_POS, LAMPLIGHTER_YAW);
        spawn(ModEntities.STRANGER.get().create(level, EntitySpawnReason.STRUCTURE), STRANGER_POS, STRANGER_YAW);
        spawn(ModEntities.PRISON_WARDEN.get().create(level, EntitySpawnReason.STRUCTURE), WARDEN_POS, 180.0F);
        spawn(ModEntities.REGULATOR.get().create(level, EntitySpawnReason.STRUCTURE), REGULATOR_POS, 180.0F);
        RatSpawner.populate(level);
    }

    private void spawn(Mob mob, Vec3 pos, float yaw) {
        if (mob != null) {
            mob.snapTo(pos.x, pos.y, pos.z, yaw, 0.0F);
            mob.setYHeadRot(yaw);
            mob.setYBodyRot(yaw);
            level.addFreshEntity(mob);
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
