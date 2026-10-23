package com.replecant.skytest.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Мир приключения: верхний мир на типе измерения «глубины»
 * (задаётся пресетом minecraft:normal, который мод переопределяет).
 */
public final class AdventureWorld {
    /**
     * Тип измерения «глубины». Хранится под ванильным ключом minecraft:overworld_caves
     * (обычные миры его не используют): с собственным ключом игра считает мир
     * «экспериментальным» и показывает предупреждение при каждом входе.
     */
    public static final ResourceKey<DimensionType> DEPTHS = BuiltinDimensionTypes.OVERWORLD_CAVES;

    public static boolean isAdventure(Level level) {
        return level.dimensionTypeRegistration().is(DEPTHS);
    }

    public static boolean isAdventure(MinecraftServer server) {
        return isAdventure(server.overworld());
    }

    /** Отключаем ванильные системы, которые мешают приключению. */
    public static void applyGameRules(ServerLevel level) {
        MinecraftServer server = level.getServer();
        GameRules rules = level.getGameRules();
        set(rules, GameRules.ADVANCE_TIME, false, server);
        set(rules, GameRules.ADVANCE_WEATHER, false, server);
        set(rules, GameRules.SPAWN_MOBS, false, server);
        set(rules, GameRules.SPAWN_MONSTERS, false, server);
        set(rules, GameRules.SPAWN_PHANTOMS, false, server);
        set(rules, GameRules.SPAWN_PATROLS, false, server);
        set(rules, GameRules.SPAWN_WANDERING_TRADERS, false, server);
        set(rules, GameRules.SPAWN_WARDENS, false, server);
        set(rules, GameRules.KEEP_INVENTORY, true, server);
        set(rules, GameRules.MOB_GRIEFING, false, server);
        set(rules, GameRules.SHOW_ADVANCEMENT_MESSAGES, false, server);
        set(rules, GameRules.LOCATOR_BAR, false, server);
        set(rules, GameRules.SPREAD_VINES, false, server);
        level.setDayTime(18000L);
    }

    private static <T> void set(GameRules rules, GameRule<T> rule, T value, MinecraftServer server) {
        if (!value.equals(rules.get(rule))) {
            rules.set(rule, value, server);
        }
    }

    private AdventureWorld() {}
}
