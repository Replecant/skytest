package com.replecant.skytest.client.feel;

import java.util.HashMap;
import java.util.Map;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.network.HitstopPayload;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Клиентская часть хитстопа: замораживает анимации цели и встряхивает камеру атакующего. */
@EventBusSubscriber(modid = SkyTest.MODID, value = Dist.CLIENT)
public final class ClientHitstop {
    private static final Map<Integer, Integer> FROZEN = new HashMap<>();
    private static int clientTicks;

    public static void handle(HitstopPayload payload) {
        FROZEN.put(payload.targetId(), clientTicks + payload.ticks());
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getId() == payload.attackerId()) {
            float strength = payload.ticks() / 3.0F;
            MovementFeel.kick(70.0F * strength, (mc.player.getRandom().nextBoolean() ? 1 : -1) * 60.0F * strength);
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        clientTicks++;
        if (!FROZEN.isEmpty()) {
            FROZEN.values().removeIf(until -> until <= clientTicks);
        }
    }

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Pre event) {
        if (!FROZEN.isEmpty() && event.getEntity().level().isClientSide()
                && FROZEN.containsKey(event.getEntity().getId())) {
            event.setCanceled(true);
        }
    }

    private ClientHitstop() {}
}
