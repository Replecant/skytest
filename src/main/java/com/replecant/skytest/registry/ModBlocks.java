package com.replecant.skytest.registry;

import com.replecant.skytest.SkyTest;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SkyTest.MODID);

    /**
     * Тюремные ворота: решётчатая дверь в два блока. Как железную дверь, рукой
     * их не открыть — только сигналом или сценарием. Неразрушимы.
     */
    public static final DeferredBlock<DoorBlock> PRISON_GATE = BLOCKS.register("prison_gate",
            id -> new DoorBlock(BlockSetType.IRON, BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.METAL)
                    .strength(-1.0F, 3600000.0F)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    private ModBlocks() {}
}
