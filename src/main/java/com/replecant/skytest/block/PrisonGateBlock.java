package com.replecant.skytest.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Тюремные ворота. Ведут себя как железная дверь, но закрытая створка стоит
 * посередине блока — в одной плоскости с решёткой камеры, а не у края, как у двери.
 */
public class PrisonGateBlock extends DoorBlock {
    private static final VoxelShape CLOSED_X = Block.box(6.5, 0.0, 0.0, 9.5, 16.0, 16.0);
    private static final VoxelShape CLOSED_Z = Block.box(0.0, 0.0, 6.5, 16.0, 16.0, 9.5);

    public PrisonGateBlock(BlockSetType type, Properties properties) {
        super(type, properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(OPEN)) {
            return super.getShape(state, level, pos, context);
        }
        return state.getValue(FACING).getAxis() == net.minecraft.core.Direction.Axis.X ? CLOSED_X : CLOSED_Z;
    }

    /** Закрытые ворота — опора для соседних прутьев решётки: те примыкают к ним, а не обрываются столбиком. */
    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(OPEN) ? super.getBlockSupportShape(state, level, pos) : Shapes.block();
    }
}
