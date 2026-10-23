package com.replecant.skytest;

import net.neoforged.neoforge.common.ModConfigSpec;

/** config/skytest-common.toml */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PROLOGUE_ENABLED = BUILDER
            .comment("Show the prologue (the vision of the pillar of light) on a player's first login.",
                    "When disabled, players simply start in their cell. '/skytest prologue' still plays it.")
            .define("prologueEnabled", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
