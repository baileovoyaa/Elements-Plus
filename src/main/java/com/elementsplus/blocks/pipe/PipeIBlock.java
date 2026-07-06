package com.elementsplus.blocks.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class PipeIBlock extends RotatedPillarBlock implements IPipeBlock {
    private final PipeMaterial pipeMaterial;

    public PipeIBlock(Properties properties, PipeMaterial pipeMaterial) {
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

    @Override
    protected @NotNull VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        Direction.Axis axis = blockState.getValue(RotatedPillarBlock.AXIS);
        return switch (axis) {
            case X -> Shapes.join(Shapes.or(
                    Block.box(1.0, 6.0, 6.0, 15.0, 10.0, 10.0),  // base
                    Block.box(0.0, 5.0, 5.0, 1.0, 11.0, 11.0),  // joint1
                    Block.box(15.0, 5.0, 5.0, 16.0, 11.0, 11.0)  // joint2
            ), Block.box(0.0, 7.0, 7.0, 16.0, 9.0, 9.0), BooleanOp.ONLY_FIRST);
            case Y -> Shapes.join(Shapes.or(
                    Block.box(6.0, 1.0, 6.0, 10.0, 15.0, 10.0),  // base
                    Block.box(5.0, 0.0, 5.0, 11.0, 1.0, 11.0),  // joint_bottom
                    Block.box(5.0, 15.0, 5.0, 11.0, 16.0, 11.0)  // joint_top
            ), Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0), BooleanOp.ONLY_FIRST);
            case Z -> Shapes.join(Shapes.or(
                    Block.box(6.0, 6.0, 1.0, 10.0, 10.0, 15.0),  // base
                    Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 1.0),  // joint1
                    Block.box(5.0, 5.0, 15.0, 11.0, 11.0, 16.0)  // joint2
            ), Block.box(7.0, 7.0, 0.0, 9.0, 9.0, 16.0), BooleanOp.ONLY_FIRST);
        };
    }

}
