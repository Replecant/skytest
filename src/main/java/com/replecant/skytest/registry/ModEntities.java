package com.replecant.skytest.registry;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.entity.GrapplingHookEntity;
import com.replecant.skytest.entity.LamplighterEntity;
import com.replecant.skytest.entity.StrangerEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, SkyTest.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<GrapplingHookEntity>> GRAPPLING_HOOK =
            ENTITY_TYPES.register("grappling_hook", id -> EntityType.Builder
                    .<GrapplingHookEntity>of(GrapplingHookEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** Фонарщик — сгорбленный тюремщик с одним светящимся глазом. */
    public static final DeferredHolder<EntityType<?>, EntityType<LamplighterEntity>> LAMPLIGHTER =
            ENTITY_TYPES.register("lamplighter", id -> EntityType.Builder
                    .<LamplighterEntity>of(LamplighterEntity::new, MobCategory.MISC)
                    .sized(0.8F, 2.3F)
                    .eyeHeight(2.15F)
                    .clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** Незнакомец в охотничьей шляпе. */
    public static final DeferredHolder<EntityType<?>, EntityType<StrangerEntity>> STRANGER =
            ENTITY_TYPES.register("stranger", id -> EntityType.Builder
                    .<StrangerEntity>of(StrangerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(LAMPLIGHTER.get(), LamplighterEntity.createAttributes().build());
        event.put(STRANGER.get(), StrangerEntity.createAttributes().build());
    }

    private ModEntities() {}
}
