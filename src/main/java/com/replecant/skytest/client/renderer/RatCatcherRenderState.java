package com.replecant.skytest.client.renderer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class RatCatcherRenderState extends LivingEntityRenderState {
    /** Крысолов сейчас разговаривает с игроком. */
    public boolean talking;
    /** Сколько тиков (с дробной частью) длится текущий режим: разговор или «возня с крысами». */
    public float modeTime;
}
