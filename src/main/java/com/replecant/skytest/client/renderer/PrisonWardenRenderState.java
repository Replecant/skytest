package com.replecant.skytest.client.renderer;

import com.replecant.skytest.entity.PrisonWardenEntity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class PrisonWardenRenderState extends LivingEntityRenderState {
    public PrisonWardenEntity.State state = PrisonWardenEntity.State.PATROL;
    /** Сколько тиков длится текущее состояние. */
    public float stateTime;
}
