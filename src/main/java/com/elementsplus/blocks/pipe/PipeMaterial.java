package com.elementsplus.blocks.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class PipeMaterial {
    public void onRandomTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
    }

    public boolean isRandomlyTicking(BlockState blockState) {
        return false;
    }
}
