package com.elementsplus.blocks.pipe;

import com.google.common.collect.Sets;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractPipeBlock extends RotatedPillarBlock {
    private final PipeMaterial pipeMaterial;

    private static final IntegerProperty POWER = BlockStateProperties.POWER;
    private static final Vec3[] COLORS = Util.make(new Vec3[16], vec3s -> {
        for (int i = 0; i <= 15; i++) {
            float f = i / 15.0F;
            float g = f * 0.6F + (f > 0.0F ? 0.4F : 0.3F);
            float h = Mth.clamp(f * f * 0.7F - 0.5F, 0.0F, 1.0F);
            float j = Mth.clamp(f * f * 0.6F - 0.7F, 0.0F, 1.0F);
            vec3s[i] = new Vec3(g, h, j);
        }
    });

    protected AbstractPipeBlock(Properties properties, PipeMaterial pipeMaterial) {
        super(properties);
        this.pipeMaterial = pipeMaterial;
    }

    public PipeMaterial getPipeMaterial() {
        return pipeMaterial;
    }

    @Override
    public void randomTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
        this.getPipeMaterial().onRandomTick(blockState, serverLevel, blockPos, randomSource);
    }

    @Override
    public boolean isRandomlyTicking(BlockState blockState) {
        return this.getPipeMaterial().isRandomlyTicking(blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER);
    }

    @Override
    public int getSignal(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, Direction direction) {
        if (!this.shouldSignal) return 0;
        return isSideConnectable(blockState, direction) ? blockState.getValue(POWER) : 0;
    }

    public abstract boolean isSideConnectable(BlockState blockState, Direction direction);

    private void updatePowerStrength(Level level, BlockPos blockPos, BlockState blockState) {
        int i = this.calculateTargetStrength(blockState, level, blockPos);
        if (blockState.getValue(POWER) != i) {
            if (level.getBlockState(blockPos) == blockState) {
                level.setBlock(blockPos, blockState.setValue(POWER, i), 2);
            }

            Set<BlockPos> set = Sets.newHashSet();
            set.add(blockPos);

            for (Direction direction : Direction.values()) {
                set.add(blockPos.relative(direction));
            }

            for (BlockPos blockPos2 : set) {
                level.updateNeighborsAt(blockPos2, this);
            }
        }
    }

    private boolean shouldSignal = false;

    private int calculateTargetStrength(BlockState blockState, Level level, BlockPos blockPos) {
        this.shouldSignal = false;
        int i = level.getBestNeighborSignal(blockPos);
        this.shouldSignal = true;
        int j = 0;
        if (i < 15) {
            for (Direction direction : Direction.values()) {
                if (isSideConnectable(blockState, direction)) {
                    BlockPos blockPos2 = blockPos.relative(direction);
                    BlockState blockState2 = level.getBlockState(blockPos2);
                    j = Math.max(j, this.getWireSignal(blockState2));
                    BlockPos blockPos3 = blockPos.above();
                    if (blockState2.isRedstoneConductor(level, blockPos2) && !level.getBlockState(blockPos3).isRedstoneConductor(level, blockPos3)) {
                        j = Math.max(j, this.getWireSignal(level.getBlockState(blockPos2.above())));
                    } else if (!blockState2.isRedstoneConductor(level, blockPos2)) {
                        j = Math.max(j, this.getWireSignal(level.getBlockState(blockPos2.below())));
                    }
                }
            }
        }

        return Math.max(i, j - 1);
    }

    private int getWireSignal(BlockState blockState) {
        return (blockState.is(Blocks.REDSTONE_WIRE) || (blockState.getBlock() instanceof AbstractPipeBlock)) ? blockState.getValue(POWER) : 0;
    }


    @Override
    public void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        if (!blockState2.is(blockState.getBlock()) && !level.isClientSide) {
            this.updatePowerStrength(level, blockPos, blockState);

            for (Direction direction : Direction.values()) {
                if (isSideConnectable(blockState, direction)) level.updateNeighborsAt(blockPos.relative(direction), this);
            }

            this.updateNeighborsOfNeighboringWires(blockState, level, blockPos);
        }
    }

    @Override
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        if (!bl && !blockState.is(blockState2.getBlock())) {
            super.onRemove(blockState, level, blockPos, blockState2, bl);
            if (!level.isClientSide) {
                for (Direction direction : Direction.values()) {
                    level.updateNeighborsAt(blockPos.relative(direction), this);
                }

                this.updatePowerStrength(level, blockPos, blockState);
                this.updateNeighborsOfNeighboringWires(blockState, level, blockPos);
            }
        }
    }

    private void updateNeighborsOfNeighboringWires(BlockState blockState, Level level, BlockPos blockPos) {
        for (Direction direction : Direction.values()) {
            if (isSideConnectable(blockState, direction)) this.checkCornerChangeAt(level, blockPos.relative(direction));
        }

        for (Direction direction : Direction.values()) {
            if (isSideConnectable(blockState, direction)) {
                BlockPos blockPos2 = blockPos.relative(direction);
                if (level.getBlockState(blockPos2).isRedstoneConductor(level, blockPos2)) {
                    this.checkCornerChangeAt(level, blockPos2.above());
                } else {
                    this.checkCornerChangeAt(level, blockPos2.below());
                }
            }
        }
    }

    private void checkCornerChangeAt(Level level, BlockPos blockPos) {
        if (level.getBlockState(blockPos).is(this)) {
            level.updateNeighborsAt(blockPos, this);

            for (Direction direction : Direction.values()) {
                level.updateNeighborsAt(blockPos.relative(direction), this);
            }
        }
    }

    @Override
    protected int getDirectSignal(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, Direction direction) {
        return !this.shouldSignal ? 0 : blockState.getSignal(blockGetter, blockPos, direction);
    }


    @Override
    protected boolean isSignalSource(BlockState blockState) {
        return this.shouldSignal;
    }
}
