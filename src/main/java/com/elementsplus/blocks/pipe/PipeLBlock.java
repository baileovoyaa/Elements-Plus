package com.elementsplus.blocks.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PipeLBlock extends RotatedPillarBlock implements IPipeBlock {
    private static final VoxelShape BASE = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);
    private final PipeMaterial pipeMaterial;

    public PipeLBlock(Properties properties, PipeMaterial pipeMaterial) {
        super(properties);
        this.pipeMaterial = pipeMaterial;
    }

    @Override
    public PipeMaterial getPipeMaterial() {
        return pipeMaterial;
    }

    @Override
    protected void randomTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
        this.onRandomTick(blockState, serverLevel, blockPos, randomSource);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState blockState) {
        return this.onIsRandomlyTicking(blockState);
    }
}
