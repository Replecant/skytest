package com.replecant.skytest.combat;

import net.minecraft.server.level.ServerPlayer;

/** Враг со своей реакцией на парирование (например, Страж падает на колено). */
public interface ParryReactive {
    void onParried(ServerPlayer player);
}
