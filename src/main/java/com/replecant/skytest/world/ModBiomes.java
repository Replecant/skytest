package com.replecant.skytest.world;

import com.replecant.skytest.SkyTest;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/** Биомы мира приключения: от них зависят туман, цвет неба и музыка. */
public final class ModBiomes {
    /** Внутри скалы: густой тёмный туман, мрачная музыка. */
    public static final ResourceKey<Biome> LOWER_PRISON = key("lower_prison");
    /** Снаружи: открытое небо, облака далеко внизу, бездна. */
    public static final ResourceKey<Biome> SKY_ABYSS = key("sky_abyss");

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(SkyTest.MODID, name));
    }

    private ModBiomes() {}
}
