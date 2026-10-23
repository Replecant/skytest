package com.replecant.skytest.registry;

import com.replecant.skytest.SkyTest;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SkyTest.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SKYTEST_TAB = CREATIVE_MODE_TABS.register("skytest",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.skytest"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.GRAPPLING_HOOK.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.GRAPPLING_HOOK.get());
                        output.accept(ModItems.CIRCUS_BOOTS.get());
                        output.accept(ModItems.DULL_SWORD.get());
                        output.accept(ModItems.RAW_RAT_MEAT.get());
                        output.accept(ModItems.PRISON_GATE.get());
                        output.accept(ModItems.LIGHTNING_SPHERE.get());
                        output.accept(ModItems.PRIMORDIAL_SERPENT.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
