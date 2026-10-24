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
    /** Незнакомец вещает из пролома в восточной стене зала, над головой у игрока. */
    public static final Vec3 STRANGER_POS = new Vec3(48.5, FLOOR + 2, 10.5);
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
    /** Верхний ярус: пол зала и ось входа (z), куда позже придёт мост. */
    public static final int UPPER_FLOOR = 74;
    /** Нижние камеры и крысятник Крысолова. */
    public static final int LOWER_FLOOR = 55;
    /** Точка на руинах посреди моста — сюда возвращает падение, если пролёта к камерам нет. */
    public static final Vec3 RUINS_POINT = new Vec3(38.5, BRIDGE_Y + 1, 72.5);
    /** Вход в зал Регулятора — здесь возрождается игрок, если камеры отрезаны. */
    public static final Vec3 REGULATOR_ENTRANCE = new Vec3(38.5, 57, 101.5);
    /** Тайник Копателя: высота лаза и область, где «тайник найден». */
    public static final int DIG_Y = 47;
    public static final AABB DIGGERS_STASH = new AABB(12, DIG_Y - 1, 29, 17, DIG_Y + 3, 42);
    public static final AABB RAT_ROOM = new AABB(20, 54, 4, 32, 60, 20);
    public static final Vec3 RAT_CATCHER_POS = new Vec3(28.5, LOWER_FLOOR, 15.5);
    private static final int NEST_RATS = 11;
    /** Основной блок камер — здесь показывается название уровня. */
    public static final AABB CELL_BLOCK = new AABB(-8, 62, -2, 30, 70, 16);
    public static final int UPPER_LANE = 71;
    private static final int UPPER_HALL_X0 = 88;
    private static final int UPPER_HALL_X1 = 104;
    public static final AABB UPPER_HALL = new AABB(87, 73, 61, 106, 86, 82);
    private static final AABB UPPER_BIOME = new AABB(86, 70, 60, 108, 88, 84);
    /** Провала в мосте больше нет (диапазон пуст); перила посередине всё равно побиты. */
    private static final int GAP_START = 61;
    private static final int GAP_END = 60;
    private static final AABB WHOLE_AREA = new AABB(-40, -10, -40, 132, 130, 160);
    /** Всё, что внутри этой коробки, — биом тюрьмы (тёмный туман); снаружи — небо над бездной. */
    private static final AABB PRISON_BIOME = new AABB(-22, 48, -20, 60, 99, 26);
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
        buildIsland(88, 104, 61, 81, 70, 96, 20, 28.0);
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
        buildLowerCells();
        buildStrangerExit();
        buildBridge();
        buildRegulatorHall();
        buildUpperTier();

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
        set(new BlockPos(6, FLOOR, 14), Blocks.SKELETON_SKULL.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 6));
        fill(9, FLOOR, 14, 10, FLOOR, 14, Blocks.RED_CARPET.defaultBlockState());
    }

    private void decorateCorridor() {
        for (int x = -3; x <= 27; x += 5) {
            hangingLantern(x, FLOOR + 4, 7, 0);
        }
        // Остатки разобранного завала у спуска в нижние камеры.
        set(new BlockPos(-5, FLOOR, 6), Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        set(new BlockPos(-6, FLOOR, 8), Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState());
        set(new BlockPos(-4, FLOOR, 8), Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState());
        cobweb(-4, FLOOR + 4, 6);
        cobweb(29, FLOOR + 4, 8);
    }

    private void decorateHall() {
        set(new BlockPos(44, FLOOR, 3), Blocks.SOUL_LANTERN.defaultBlockState());
        set(new BlockPos(44, FLOOR, 11), Blocks.SOUL_LANTERN.defaultBlockState());

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

    // ---------------------------------------------------------------- нижние камеры

    /**
     * Нижние камеры под основным блоком: из западного тупика коридора лестница
     * уходит вниз, к ещё одному ряду камер. В дальней, самой большой, живёт
     * Крысолов со своими крысами (и, похоже, с Ларпи).
     */
    private void buildLowerCells() {
        int y = LOWER_FLOOR;
        // Лестница на запад, вниз на 9 ступеней.
        // Сначала пробиваем все пролёты, потом кладём ступени (иначе стены соседнего пролёта их затрут).
        for (int i = 1; i <= 8; i++) {
            room(-6 - i, FLOOR - i, 6, -6 - i, FLOOR + 3 - i, 8);
        }
        for (int i = 1; i <= 8; i++) {
            int x = -6 - i;
            fill(x, FLOOR - 1 - i, 6, x, FLOOR - 1 - i, 8, Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        }
        hangingLantern(-10, FLOOR + 1, 7, 1);
        // Площадка внизу и коридор под южными камерами.
        room(-18, y, 5, -15, y + 4, 13);
        room(-14, y, 11, 19, y + 3, 13);
        cobweb(-18, y + 4, 5);
        for (int x = -10; x <= 16; x += 8) {
            hangingLantern(x, y + 3, 12, 0);
        }
        int[] cellX = {-4, 2, 8, 14};
        for (int i = 0; i < cellX.length; i++) {
            int x0 = cellX[i];
            room(x0, y, 6, x0 + 4, y + 3, 9);
            room(x0, y, 15, x0 + 4, y + 3, 18);
            lowerBars(x0, 10, Direction.NORTH, i == 1);
            lowerBars(x0, 14, Direction.SOUTH, i == 2);
            cobweb(x0 + random.nextInt(5), y + 3, 6);
            cobweb(x0 + random.nextInt(5), y + 3, 18);
        }
        // Обжитые и заброшенные камеры.
        set(new BlockPos(-3, y, 7), Blocks.SKELETON_SKULL.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 3));
        fill(2, y, 6, 3, y, 7, Blocks.HAY_BLOCK.defaultBlockState());
        set(new BlockPos(5, y, 6), Blocks.BONE_BLOCK.defaultBlockState());
        chain(10, y + 3, 7, 2);
        set(new BlockPos(10, y, 7), Blocks.SKELETON_SKULL.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 8));
        fill(14, y, 6, 16, y + 1, 8, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        set(new BlockPos(17, y, 6), Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState());
        chest(9, y, 17, Direction.NORTH, new ItemStack(ModItems.RAW_RAT_MEAT.get(), 3));
        set(new BlockPos(3, y, 16), Blocks.SKELETON_SKULL.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 12));

        buildRatRoom();
        buildDiggersTunnel();
    }

    /**
     * Тайник «Подкоп Копателя». Крайняя южная камера заперта, но в углу решётки
     * не хватает двух прутьев. Внутри — щебень и лежанка, под ней люк; под ним шахта
     * с лестницей и извилистый лаз до самого края скалы, где в стене дыра
     * в небо. Там — давние кости и сундучок: два зелья лечения и хлеб.
     */
    private void buildDiggersTunnel() {
        int y = LOWER_FLOOR;
        // Отогнутые прутья в углу у стены — можно пролезть.
        fill(18, y, 14, 18, y + 1, 14, Blocks.AIR.defaultBlockState());
        // Выкопанный щебень и лежанка.
        set(new BlockPos(14, y, 15), Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        set(new BlockPos(14, y, 16), Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState());
        set(new BlockPos(15, y, 15), Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState());
        set(new BlockPos(14, y + 1, 15), Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState());
        // Люк вровень с полом — под лежанкой из ковра: снаружи просто подстилка.
        int shaftX = 16;
        int shaftZ = 17;
        set(new BlockPos(shaftX, y - 1, shaftZ), Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
                .setValue(BlockStateProperties.HALF, net.minecraft.world.level.block.state.properties.Half.TOP)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        fill(15, y, 16, 17, y, 18, Blocks.BROWN_CARPET.defaultBlockState());
        int bottom = DIG_Y;
        // Лестница держится за северную стенку шахты; внизу — пусто, спрыгнуть два блока.
        fill(shaftX, bottom, shaftZ, shaftX, bottom + 1, shaftZ, Blocks.AIR.defaultBlockState());
        for (int yy = bottom + 2; yy <= y - 2; yy++) {
            set(new BlockPos(shaftX, yy, shaftZ), Blocks.LADDER.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        }
        // Лаз на юг, к краю скалы: петляет, как копал человек.
        int z = shaftZ + 1;
        int prevX = shaftX;
        int face = -1;
        while (z < 60) {
            int x = z <= 22 ? 16 : z <= 27 ? 15 : 14;
            fill(Math.min(x, prevX), bottom, z, Math.max(x, prevX), bottom + 1, z, Blocks.AIR.defaultBlockState());
            prevX = x;
            BlockPos ahead = new BlockPos(x, bottom, z + 4);
            if (level.getBlockState(ahead).isAir() && level.getBlockState(ahead.above()).isAir()) {
                face = z + 4;
                break;
            }
            if (z % 5 == 0) {
                // Подпорки из досок нар.
                set(new BlockPos(x, bottom + 2, z), Blocks.SPRUCE_SLAB.defaultBlockState()
                        .setValue(BlockStateProperties.SLAB_TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP));
            }
            if (z % 7 == 3) {
                set(new BlockPos(x, bottom, z), Blocks.GLOW_LICHEN.defaultBlockState().setValue(BlockStateProperties.DOWN, true));
            }
            z++;
        }
        if (face < 0) {
            SkyTest.LOGGER.warn("Digger's tunnel found no cliff face");
            return;
        }
        // Камерка у самого края и дыра в небо.
        int cx = 14;
        fill(cx - 1, bottom, face - 4, cx + 1, bottom + 2, face - 2, Blocks.AIR.defaultBlockState());
        fill(cx, bottom, face - 1, cx, bottom + 1, face - 1, Blocks.AIR.defaultBlockState());
        cobweb(cx + 1, bottom + 2, face - 4);
        // От Копателя остался только череп — смотрит на дыру в небо.
        set(new BlockPos(cx, bottom, face - 2), Blocks.SKELETON_SKULL.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 8));
        set(new BlockPos(cx - 1, bottom, face - 4), Blocks.CANDLE.defaultBlockState());
        BlockPos chestPos = new BlockPos(cx + 1, bottom, face - 4);
        set(chestPos, Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setItem(11, net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                    net.minecraft.world.item.Items.POTION, net.minecraft.world.item.alchemy.Potions.HEALING));
            chest.setItem(13, new ItemStack(net.minecraft.world.item.Items.BREAD));
            chest.setItem(15, net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                    net.minecraft.world.item.Items.POTION, net.minecraft.world.item.alchemy.Potions.HEALING));
        }
        SkyTest.LOGGER.info("Digger's tunnel reaches the cliff at z={}", face);
    }

    /** Решётка нижней камеры по линии z с воротами посередине. */
    private void lowerBars(int cellX, int z, Direction facing, boolean open) {
        int gateX = cellX + 2;
        for (int x = cellX; x <= cellX + 4; x++) {
            for (int yy = LOWER_FLOOR; yy <= LOWER_FLOOR + 2; yy++) {
                if (x == gateX && yy <= LOWER_FLOOR + 1) {
                    continue;
                }
                connectable(new BlockPos(x, yy, z), Blocks.IRON_BARS.defaultBlockState());
            }
        }
        gate(new BlockPos(gateX, LOWER_FLOOR, z), facing, open);
    }

    /** Крысятник: большая камера в конце коридора. Сено, бочки, клетки, лежанка старика. */
    private void buildRatRoom() {
        int y = LOWER_FLOOR;
        room(21, y, 5, 31, y + 4, 19);
        // Вход из коридора — распахнутые ворота в решётке.
        fill(20, y, 11, 20, y + 2, 13, Blocks.AIR.defaultBlockState());
        for (int z : new int[] {11, 13}) {
            for (int yy = y; yy <= y + 2; yy++) {
                connectable(new BlockPos(20, yy, z), Blocks.IRON_BARS.defaultBlockState());
            }
        }
        gate(new BlockPos(20, y, 12), Direction.EAST, true);
        fill(21, y - 1, 5, 31, y - 1, 19, pick(Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, 0.3F));
        // Сено и подстилки, где спят крысы.
        for (int[] h : new int[][] {{29, 5}, {29, 6}, {30, 7}, {21, 18}, {22, 19}, {21, 19}, {25, 5}}) {
            set(new BlockPos(h[0], y, h[1]), Blocks.HAY_BLOCK.defaultBlockState());
        }
        set(new BlockPos(21, y + 1, 19), Blocks.HAY_BLOCK.defaultBlockState());
        // Бочки и клетки-ловушки.
        for (int[] b : new int[][] {{31, 10}, {31, 11}, {22, 5}, {23, 5}}) {
            set(new BlockPos(b[0], y, b[1]), Blocks.BARREL.defaultBlockState()
                    .setValue(BlockStateProperties.FACING, Direction.UP));
        }
        set(new BlockPos(31, y + 1, 10), Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        for (int[] c : new int[][] {{21, 7}, {21, 8}, {31, 14}}) {
            connectable(new BlockPos(c[0], y, c[1]), Blocks.IRON_BARS.defaultBlockState());
        }
        set(new BlockPos(24, y, 19), Blocks.CAULDRON.defaultBlockState());
        // Лежанка старика.
        fill(29, y, 16, 31, y, 18, Blocks.BROWN_CARPET.defaultBlockState());
        set(new BlockPos(31, y, 19), Blocks.SOUL_LANTERN.defaultBlockState());
        hangingLantern(26, y + 4, 12, 1);
        hangingLantern(23, y + 4, 16, 2);
        chain(29, y + 4, 9, 2);
        for (int[] w : new int[][] {{21, 5}, {31, 5}, {31, 19}, {26, 19}}) {
            cobweb(w[0], y + 4, w[1]);
        }
        set(new BlockPos(27, y, 7), Blocks.BONE_BLOCK.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
    }

    /** Крысолов и его крысы; где-то среди них — Ларпи. */
    private void spawnRatRoom() {
        spawn(ModEntities.RAT_CATCHER.get().create(level, EntitySpawnReason.STRUCTURE), RAT_CATCHER_POS, 135.0F);
        for (int i = 0; i < NEST_RATS; i++) {
            RatEntity rat = ModEntities.RAT.get().create(level, EntitySpawnReason.STRUCTURE);
            if (rat != null) {
                rat.setNest(true);
                spawn(rat, new Vec3(22.5 + random.nextInt(8), LOWER_FLOOR, 8.5 + random.nextInt(10)), random.nextFloat() * 360.0F);
            }
        }
        RatEntity larpi = ModEntities.RAT.get().create(level, EntitySpawnReason.STRUCTURE);
        if (larpi != null) {
            larpi.setLarpi(true);
            larpi.setNest(true);
            spawn(larpi, new Vec3(30.5, LOWER_FLOOR, 5.5), 200.0F);
        }
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
        buildIsland(minX, maxX, minZ, maxZ, coreBottom, coreTop, seed, ISLAND_HANG);
    }

    private void buildIsland(int minX, int maxX, int minZ, int maxZ, int coreBottom, int coreTop, int seed, double hangDepth) {
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
                double hang = hangDepth * (0.75 + 0.5 * noise(x, z, 9.0, seed + 3));
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
     * Портал тоннеля: кирпичная рамка там, где скала уже сплошная со всех сторон,
     * — тогда рамка нигде не торчит наружу, а к ней ведёт естественная выемка в скале.
     */
    private void portal(int z) {
        int y = BRIDGE_Y;
        fill(36, y - 1, z, 40, y + 5, z, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        set(new BlockPos(38, y + 5, z), Blocks.CHISELED_DEEPSLATE.defaultBlockState());
    }

    /** Скала вокруг рамки портала сплошная (с запасом в блок). */
    private boolean solidAround(int z) {
        for (int x = 34; x <= 42; x++) {
            for (int y = BRIDGE_Y - 2; y <= BRIDGE_Y + 7; y++) {
                if (level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Поперечный пролёт открытого моста на данном z: настил, бортики, перила; на опорах — балка, цепи и фонари. */
    private void spanColumn(int z, boolean supportsAllowed) {
        int y = BRIDGE_Y;
        for (int x = 37; x <= 39; x++) {
            set(new BlockPos(x, y, z), pick(Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, 0.25F));
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
        if (z % 8 == 0 && supportsAllowed) {
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

    /**
     * Разобрать поперечный срез моста: настил срывается и падает в бездну
     * (падающие блоки), остальное — перила, фонари, цепи — исчезает.
     */
    private void dismantle(int x0, int x1, int y0, int y1, int z0, int z1, int deckY) {
        for (BlockPos pos : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (pos.getY() >= deckY - 1 && pos.getY() <= deckY && random.nextFloat() < 0.55F) {
                var falling = net.minecraft.world.entity.item.FallingBlockEntity.fall(level, pos.immutable(), state);
                falling.dropItem = false;
                falling.setDeltaMovement((random.nextFloat() - 0.5F) * 0.15, 0.05, (random.nextFloat() - 0.5F) * 0.15);
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Проход сквозь край скалы шириной с мост: настил и кирпичные бортики, над ними пусто. */
    private void notch(int z) {
        int y = BRIDGE_Y;
        fill(36, y + 1, z, 40, y + 4, z, Blocks.AIR.defaultBlockState());
        for (int x = 37; x <= 39; x++) {
            set(new BlockPos(x, y, z), pick(Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, 0.3F));
        }
        set(new BlockPos(36, y, z), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        set(new BlockPos(40, y, z), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
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
        int exitPortal = exitZ;
        while (exitPortal > 27 && !solidAround(exitPortal)) {
            exitPortal--;
        }
        int entryPortal = entryZ;
        while (entryPortal < 95 && !solidAround(entryPortal)) {
            entryPortal++;
        }
        SkyTest.LOGGER.info("Bridge faces z={}/{}, portals z={}/{}", exitZ, entryZ, exitPortal, entryPortal);
        portal(exitPortal);
        portal(entryPortal);
        // Тоннели сквозь скалу: от спуска к мосту и от моста к залу Регулятора.
        fill(37, y + 1, 25, 39, y + 3, exitPortal, Blocks.AIR.defaultBlockState());
        fill(37, y + 1, entryPortal, 39, y + 4, 96, Blocks.AIR.defaultBlockState());
        // Выемки в скале между порталом и открытым мостом.
        for (int z = exitPortal + 1; z <= exitZ; z++) {
            notch(z);
        }
        for (int z = entryZ; z < entryPortal; z++) {
            notch(z);
        }

        int ruinsStart = UPPER_LANE - 3;
        int ruinsEnd = Math.min(UPPER_LANE + 4, entryZ - 4);
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
            spanColumn(z, z > exitZ + 2 && z < entryZ - 2);
        }
        // Полуразрушенная площадка на полпути ко второй скале: пол с выщерблинами,
        // низкий обломанный парапет, остов арки. Восточный край посередине открыт —
        // сюда Регулятор потом перекинет мост к Верхнему ярусу; пока там торчит обломок.
        for (int x = 33; x <= 43; x++) {
            for (int z = ruinsStart; z <= ruinsEnd; z++) {
                boolean edge = x == 33 || x == 43 || z == ruinsStart || z == ruinsEnd;
                boolean walkway = (x >= 37 && x <= 39) || (z >= UPPER_LANE - 1 && z <= UPPER_LANE + 1);
                if (walkway || random.nextFloat() < (edge ? 0.7F : 0.92F)) {
                    set(new BlockPos(x, y, z), pick(Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, 0.4F));
                }
                boolean parapet = (x == 33 || x == 43) && !(z >= UPPER_LANE - 2 && z <= UPPER_LANE + 2);
                if (parapet && random.nextFloat() < 0.6F) {
                    fill(x, y + 1, z, x, y + 1 + random.nextInt(2), z,
                            pick(Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS, 0.5F));
                }
            }
        }
        int arch = ruinsStart + 1;
        fill(35, y + 1, arch, 35, y + 5, arch, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        fill(41, y + 1, arch, 41, y + 3, arch, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        fill(35, y + 6, arch, 38, y + 6, arch, Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState());
        set(new BlockPos(34, y + 1, ruinsEnd), Blocks.SOUL_LANTERN.defaultBlockState());
        set(new BlockPos(42, y + 1, ruinsStart), Blocks.SOUL_LANTERN.defaultBlockState());
        // Обломок старого пролёта к Верхнему ярусу.
        for (int dz = -1; dz <= 1; dz++) {
            fill(44, y, UPPER_LANE + dz, 44 + 2 - Math.abs(dz) - (dz == 1 ? 1 : 0), y, UPPER_LANE + dz,
                    Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState());
        }
        chain(45, y - 1, UPPER_LANE, 5);
    }

    // ---------------------------------------------------------------- Верхний ярус

    /**
     * Верхний ярус: третья скала к востоку от моста, на 14 блоков выше. Вход —
     * портал в западной стене, пока забранный решёткой; со скалы свисает обломок
     * моста. Внутри — зал с камерами по обе стороны и запертыми воротами дальше,
     * на вершине — дозорная башня с огнями, видная с моста.
     */
    private void buildUpperTier() {
        int floor = UPPER_FLOOR;
        int lane = UPPER_LANE;
        int faceX = UPPER_HALL_X0;
        for (int x = 60; x < UPPER_HALL_X0; x++) {
            if (rockAcrossX(x)) {
                faceX = x;
                break;
            }
        }
        int portalX = faceX;
        while (portalX < UPPER_HALL_X0 - 2 && !solidAroundX(portalX)) {
            portalX++;
        }
        SkyTest.LOGGER.info("Upper tier face x={}, portal x={}", faceX, portalX);

        room(UPPER_HALL_X0, floor, 62, UPPER_HALL_X1, floor + 10, 80);
        // Выемка в скале, портал и тоннель в зал.
        for (int x = faceX; x < portalX; x++) {
            fill(x, floor + 1, lane - 2, x, floor + 4, lane + 2, Blocks.AIR.defaultBlockState());
            fill(x, floor, lane - 1, x, floor, lane + 1, pick(Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, 0.3F));
            set(new BlockPos(x, floor, lane - 2), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            set(new BlockPos(x, floor, lane + 2), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        }
        fill(portalX, floor - 1, lane - 2, portalX, floor + 5, lane + 2, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        set(new BlockPos(portalX, floor + 5, lane), Blocks.CHISELED_DEEPSLATE.defaultBlockState());
        fill(portalX + 1, floor + 1, lane - 1, UPPER_HALL_X0 - 1, floor + 3, lane + 1, Blocks.AIR.defaultBlockState());
        fill(portalX, floor + 1, lane - 1, portalX, floor + 3, lane + 1, Blocks.AIR.defaultBlockState());
        fill(portalX, floor, lane - 1, UPPER_HALL_X0 - 1, floor, lane + 1, Blocks.DEEPSLATE_TILES.defaultBlockState());
        // Пока моста нет — вход забран решёткой.
        for (int z = lane - 1; z <= lane + 1; z++) {
            for (int yy = floor + 1; yy <= floor + 3; yy++) {
                connectable(new BlockPos(portalX, yy, z), Blocks.IRON_BARS.defaultBlockState());
            }
        }
        set(new BlockPos(portalX - 1, floor + 1, lane + 1), Blocks.SOUL_LANTERN.defaultBlockState());
        // Обломок моста, свисающий с края скалы.
        for (int dz = -1; dz <= 1; dz++) {
            int length = dz == 0 ? 4 : 2 + (dz + 1) / 2;
            fill(faceX - length, floor, lane + dz, faceX - 1, floor, lane + dz, Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState());
        }
        chain(faceX - 3, floor - 1, lane, 6);
        chain(faceX - 1, floor - 1, lane - 1, 3);

        // Пол: полированный сланец, по центру — дорожка из плитки.
        fill(UPPER_HALL_X0, floor - 1, 62, UPPER_HALL_X1, floor - 1, 80, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        fill(UPPER_HALL_X0, floor - 1, lane - 1, UPPER_HALL_X1, floor - 1, lane + 1, Blocks.DEEPSLATE_TILES.defaultBlockState());
        // Камеры вдоль северной и южной стен.
        for (int x : new int[] {93, 99}) {
            fill(x, floor, 62, x, floor + 3, 65, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            fill(x, floor, 77, x, floor + 3, 80, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        }
        for (int x = UPPER_HALL_X0; x <= UPPER_HALL_X1; x++) {
            for (int yy = floor; yy <= floor + 3; yy++) {
                connectable(new BlockPos(x, yy, 66), Blocks.IRON_BARS.defaultBlockState());
                connectable(new BlockPos(x, yy, 76), Blocks.IRON_BARS.defaultBlockState());
            }
        }
        for (int x : new int[] {90, 96, 102}) {
            fill(x, floor, 66, x, floor + 1, 66, Blocks.AIR.defaultBlockState());
            fill(x, floor, 76, x, floor + 1, 76, Blocks.AIR.defaultBlockState());
            gate(new BlockPos(x, floor, 66), Direction.NORTH, false);
            gate(new BlockPos(x, floor, 76), Direction.SOUTH, false);
            set(new BlockPos(x + (random.nextBoolean() ? 1 : -1), floor, 63 + random.nextInt(2)),
                    Blocks.SKELETON_SKULL.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, random.nextInt(16)));
            cobweb(x + 1, floor + 3, 79);
        }
        // Дальше — запертые ворота вглубь скалы.
        int east = UPPER_HALL_X1 + 1;
        fill(east, floor, lane - 1, east, floor + 2, lane + 1, Blocks.AIR.defaultBlockState());
        for (int z : new int[] {lane - 1, lane + 1}) {
            for (int yy = floor; yy <= floor + 2; yy++) {
                connectable(new BlockPos(east, yy, z), Blocks.IRON_BARS.defaultBlockState());
            }
        }
        gate(new BlockPos(east, floor, lane), Direction.EAST, false);
        fill(east + 1, floor, lane - 1, east + 2, floor + 2, lane + 1, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        // Свет: фонари на цепях над дорожкой и огонь душ у входа.
        for (int x = 90; x <= 102; x += 6) {
            hangingLantern(x, floor + 10, lane, 3 + random.nextInt(3));
        }
        for (int z : new int[] {lane - 3, lane + 3}) {
            set(new BlockPos(UPPER_HALL_X0, floor - 1, z), Blocks.SOUL_SOIL.defaultBlockState());
            set(new BlockPos(UPPER_HALL_X0, floor, z), Blocks.SOUL_FIRE.defaultBlockState());
        }
        for (int i = 0; i < 9; i++) {
            chain(UPPER_HALL_X0 + random.nextInt(17), floor + 10, 67 + random.nextInt(9), 2 + random.nextInt(4));
        }

        buildWatchtower();
    }

    /** Дозорная башня на вершине Верхнего яруса: окна светятся, на шпиле — огонь. */
    private void buildWatchtower() {
        int cx = 96;
        int cz = UPPER_LANE;
        int base = UPPER_FLOOR + 12;
        while (base < 120 && !level.getBlockState(new BlockPos(cx, base + 1, cz)).isAir()) {
            base++;
        }
        int top = base + 16;
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                boolean wall = Math.abs(x - cx) == 3 || Math.abs(z - cz) == 3;
                boolean corner = Math.abs(x - cx) == 3 && Math.abs(z - cz) == 3;
                for (int y = base - 3; y <= top; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (corner) {
                        set(pos, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                    } else if (wall) {
                        set(pos, pick(Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS, 0.15F));
                    } else {
                        set(pos, Blocks.AIR.defaultBlockState());
                    }
                }
                if (wall && !corner && (x + z) % 2 == 0) {
                    set(new BlockPos(x, top + 1, z), Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState());
                    connectables.add(new BlockPos(x, top + 1, z));
                }
            }
        }
        fill(cx - 2, top, cz - 2, cx + 2, top, cz + 2, Blocks.DEEPSLATE_TILES.defaultBlockState());
        // Узкие окна на все стороны, за ними — свет.
        for (int y : new int[] {base + 5, base + 11}) {
            for (int[] w : new int[][] {{cx - 3, cz}, {cx + 3, cz}, {cx, cz - 3}, {cx, cz + 3}}) {
                fill(w[0], y, w[1], w[0], y + 1, w[1], Blocks.AIR.defaultBlockState());
            }
            set(new BlockPos(cx, y, cz), Blocks.SOUL_LANTERN.defaultBlockState());
            fill(cx, y - 1, cz, cx, y - 1, cz, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            set(new BlockPos(cx + 1, y + 1, cz), Blocks.LIGHT.defaultBlockState().setValue(BlockStateProperties.LEVEL, 13));
        }
        // Шпиль с огнём душ.
        fill(cx, top + 1, cz, cx, top + 4, cz, Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState());
        for (int y = top + 1; y <= top + 4; y++) {
            connectables.add(new BlockPos(cx, y, cz));
        }
        set(new BlockPos(cx, top + 5, cz), Blocks.SOUL_SOIL.defaultBlockState());
        set(new BlockPos(cx, top + 6, cz), Blocks.SOUL_FIRE.defaultBlockState());
    }

    /** Есть ли скала (именно камень, не постройки) в проёме входа Верхнего яруса на данном x. */
    private boolean rockAcrossX(int x) {
        for (int z = UPPER_LANE - 2; z <= UPPER_LANE + 2; z++) {
            for (int y = UPPER_FLOOR + 1; y <= UPPER_FLOOR + 5; y++) {
                BlockState state = level.getBlockState(new BlockPos(x, y, z));
                if (state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLED_DEEPSLATE) || state.is(Blocks.TUFF)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean solidAroundX(int x) {
        for (int z = UPPER_LANE - 4; z <= UPPER_LANE + 4; z++) {
            for (int y = UPPER_FLOOR - 2; y <= UPPER_FLOOR + 7; y++) {
                if (level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Регулятор перекидывает мост: от руин на середине Небесного моста ступенями
     * вверх к Верхнему ярусу; решётка на входе поднимается. Сразу целиком —
     * для команды {@code /skytest upperbridge}; в катсцене мост собирается
     * по пролёту через {@link #startUpperBridge}.
     */
    public static boolean buildUpperBridge(ServerLevel level) {
        UpperBridge bridge = startUpperBridge(level);
        if (bridge == null) {
            return false;
        }
        while (bridge.step()) {
            // пролёт за пролётом
        }
        bridge.finish();
        return true;
    }

    /** Мост к Верхнему ярусу сейчас перекинут. */
    public static boolean upperBridgeBuilt(ServerLevel level) {
        for (int y = BRIDGE_Y; y <= UPPER_FLOOR; y++) {
            if (!level.getBlockState(new BlockPos(56, y, UPPER_LANE)).isAir()) {
                return true;
            }
        }
        return false;
    }

    /** Пролёт от руин к нижнему ярусу (камерам) сейчас на месте. */
    public static boolean mainSpanBuilt(ServerLevel level) {
        return !level.getBlockState(new BlockPos(38, BRIDGE_Y, 50)).isAir();
    }

    /** Пролёт моста между скалой с камерами и руинами (с провалом посередине). */
    public static MainSpan mainSpan(ServerLevel level) {
        LowerPrison prison = new LowerPrison(level);
        int first = 40;
        // Первый открытый срез после выемки в скале: по бокам моста — ни камня.
        for (int z = 30; z < GAP_START; z++) {
            boolean open = true;
            for (int y = BRIDGE_Y - 2; y <= BRIDGE_Y + 6 && open; y++) {
                open = level.getBlockState(new BlockPos(33, y, z)).isAir() && level.getBlockState(new BlockPos(43, y, z)).isAir();
            }
            if (open) {
                first = z;
                break;
            }
        }
        return prison.new MainSpan(first, UPPER_LANE - 4);
    }

    /** Пролёт к камерам: строится и разбирается по срезу, от руин к скале. */
    public final class MainSpan {
        private final int first;
        private final int last;
        private int next;

        private MainSpan(int first, int last) {
            this.first = first;
            this.last = last;
            this.next = last;
        }

        public int length() {
            return last - first + 1 - (GAP_END - GAP_START + 1);
        }

        public Vec3 lastColumn() {
            return new Vec3(38.5, BRIDGE_Y + 0.5, Math.min(last, next + 1) + 0.5);
        }

        private int advance() {
            while (next >= GAP_START && next <= GAP_END) {
                next--;
            }
            return next >= first ? next-- : Integer.MIN_VALUE;
        }

        public boolean step() {
            int z = advance();
            if (z == Integer.MIN_VALUE) {
                return false;
            }
            int mark = connectables.size();
            spanColumn(z, z > first + 1);
            for (BlockPos pos : connectables.subList(Math.max(0, mark - 6), connectables.size())) {
                set(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos));
            }
            return next >= first;
        }

        public boolean unstep() {
            int z = advance();
            if (z == Integer.MIN_VALUE) {
                return false;
            }
            dismantle(34, 42, BRIDGE_Y - 12, BRIDGE_Y + 3, z, z, BRIDGE_Y);
            return next >= first;
        }

        public void finish() {
            for (BlockPos pos : connectables) {
                set(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos));
            }
        }
    }

    /** Постройка моста к Верхнему ярусу по шагам; null — Верхний ярус не найден. */
    public static UpperBridge startUpperBridge(ServerLevel level) {
        LowerPrison prison = new LowerPrison(level);
        int lane = UPPER_LANE;
        int portalX = -1;
        for (int x = 50; x < UPPER_HALL_X0; x++) {
            if (level.getBlockState(new BlockPos(x, UPPER_FLOOR + 5, lane)).is(Blocks.CHISELED_DEEPSLATE)) {
                portalX = x;
                break;
            }
        }
        if (portalX < 0) {
            return null;
        }
        int faceX = portalX;
        for (int x = 60; x < portalX; x++) {
            if (prison.rockAcrossX(x)) {
                faceX = x;
                break;
            }
        }
        // Пролёт кончается у обломка, торчащего из скалы (4 блока до её края).
        return prison.new UpperBridge(portalX, 44, faceX - 5);
    }

    /** Мост к Верхнему ярусу: каждый {@link #step()} кладёт один поперечный пролёт. */
    public final class UpperBridge {
        private final int portalX;
        private final int startX;
        private final int endX;
        private int next;

        private UpperBridge(int portalX, int startX, int endX) {
            this.portalX = portalX;
            this.startX = startX;
            this.endX = endX;
            this.next = startX;
        }

        public int length() {
            return endX - startX + 1;
        }

        /** Высота настила на данном x. */
        private int deckY(int x) {
            int run = Math.max(1, endX - startX);
            return BRIDGE_Y + Math.round((float) (x - startX) / run * (UPPER_FLOOR - BRIDGE_Y));
        }

        /** Середина последнего уложенного пролёта (для частиц и звука). */
        public Vec3 lastColumn() {
            int x = Math.max(startX, next - 1);
            return new Vec3(x + 0.5, deckY(x) + 0.5, UPPER_LANE + 0.5);
        }

        /** Уложить следующий пролёт; false — мост уже готов. */
        public boolean step() {
            if (next > endX) {
                return false;
            }
            int x = next++;
            int lane = UPPER_LANE;
            int y = deckY(x);
            int prev = x == startX ? BRIDGE_Y : deckY(x - 1);
            int mark = connectables.size();
            fill(x, y + 1, lane - 2, x, y + 5, lane + 2, Blocks.AIR.defaultBlockState());
            for (int z = lane - 1; z <= lane + 1; z++) {
                BlockState step = y > prev
                        ? Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                        : pick(Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, 0.2F);
                set(new BlockPos(x, y, z), step);
                set(new BlockPos(x, y - 1, z), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            }
            for (int z : new int[] {lane - 2, lane + 2}) {
                set(new BlockPos(x, y, z), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
                connectable(new BlockPos(x, y + 1, z), Blocks.DEEPSLATE_TILE_WALL.defaultBlockState());
            }
            if ((x - startX) % 7 == 3) {
                fill(x, y - 2, lane - 3, x, y - 2, lane + 3, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                chain(x, y - 3, lane - 3, 3 + random.nextInt(5));
                chain(x, y - 3, lane + 3, 3 + random.nextInt(5));
                for (int z : new int[] {lane - 2, lane + 2}) {
                    connectable(new BlockPos(x, y + 1, z), Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState());
                    set(new BlockPos(x, y + 2, z), Blocks.SOUL_LANTERN.defaultBlockState());
                }
            }
            // Стенки перил связываются и с только что уложенным пролётом, и с предыдущим.
            for (BlockPos pos : connectables.subList(Math.max(0, mark - 6), connectables.size())) {
                set(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos));
            }
            return next <= endX;
        }

        /** Обратно: снять следующий пролёт, начиная от Верхнего яруса; false — снят весь. */
        public boolean unstep() {
            if (next < startX) {
                return false;
            }
            int x = next--;
            int y = deckY(x);
            dismantle(x, x, y - 12, y + 3, UPPER_LANE - 4, UPPER_LANE + 4, y);
            return next >= startX;
        }

        /** Пролёт снят — решётка опускается, у руин снова торчит обломок. */
        public void finishRemoval() {
            for (int z = UPPER_LANE - 1; z <= UPPER_LANE + 1; z++) {
                for (int yy = UPPER_FLOOR + 1; yy <= UPPER_FLOOR + 3; yy++) {
                    connectable(new BlockPos(portalX, yy, z), Blocks.IRON_BARS.defaultBlockState());
                }
            }
            for (int dz = -1; dz <= 1; dz++) {
                fill(44, BRIDGE_Y, UPPER_LANE + dz, 44 + 2 - Math.abs(dz) - (dz == 1 ? 1 : 0), BRIDGE_Y, UPPER_LANE + dz,
                        Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState());
            }
            for (BlockPos pos : connectables) {
                set(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos));
            }
        }

        /** Для разборки — начать с дальнего конца уже построенного моста. */
        public UpperBridge fromEnd() {
            next = endX;
            return this;
        }

        /** Мост дошёл до скалы — решётка на входе поднимается. */
        public void finish() {
            fill(portalX, UPPER_FLOOR + 1, UPPER_LANE - 1, portalX, UPPER_FLOOR + 3, UPPER_LANE + 1, Blocks.AIR.defaultBlockState());
            for (BlockPos pos : connectables) {
                set(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos));
            }
        }

        public Vec3 portal() {
            return new Vec3(portalX + 0.5, UPPER_FLOOR + 2, UPPER_LANE + 0.5);
        }
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
                    return PRISON_BIOME.contains(center) || REGULATOR_BIOME.contains(center) || UPPER_BIOME.contains(center)
                            ? prison : abyss;
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
        level.getEntitiesOfClass(com.replecant.skytest.entity.RatCatcherEntity.class, WHOLE_AREA)
                .forEach(com.replecant.skytest.entity.RatCatcherEntity::discard);
        RatSpawner.populate(level);
        spawnRatRoom();
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
