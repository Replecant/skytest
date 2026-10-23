package com.replecant.skytest;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.replecant.skytest.network.ModNetwork;
import com.replecant.skytest.registry.ModCreativeTabs;
import com.replecant.skytest.registry.ModEntities;
import com.replecant.skytest.registry.ModItems;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(SkyTest.MODID)
public class SkyTest {
    public static final String MODID = "skytest";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SkyTest(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(ModNetwork::register);
    }
}
