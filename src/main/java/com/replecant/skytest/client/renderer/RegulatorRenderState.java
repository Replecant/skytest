package com.replecant.skytest.client.renderer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class RegulatorRenderState extends LivingEntityRenderState {
    /** Регулятор сейчас разговаривает с игроком (поднимает голову, жестикулирует). */
    public boolean talking;
    /** Сколько тиков (с дробной частью) длится текущий режим: разговор или грусть. Для плавных переходов. */
    public float modeTime;
    /** Катсцена: Регулятор встал и тянет цепи. */
    public boolean pulling;
    /** Сколько тиков (с дробной частью) прошло с начала или конца рывков (смены флага pulling). */
    public float pullTime;
}
