package com.elementsplus.blocks.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public interface IPipeBlock {
    PipeMaterial getPipeMaterial();

    default void onRandomTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
        this.getPipeMaterial().onRandomTick(blockState, serverLevel, blockPos, randomSource);
    }

    default boolean onIsRandomlyTicking(BlockState blockState) {
        return this.getPipeMaterial().isRandomlyTicking(blockState);
    }
}
