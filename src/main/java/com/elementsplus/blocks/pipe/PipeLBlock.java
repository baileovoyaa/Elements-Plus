package com.elementsplus.blocks.pipe;

import com.elementsplus.ModMth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PipeLBlock extends AbstractPipeBlock {
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

    public PipeLBlock(Properties properties, PipeMaterial pipeMaterial) {
        super(properties, pipeMaterial);
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_FACING);
    }

    @Override
    public @NotNull BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        Direction clickedFace = blockPlaceContext.getClickedFace().getOpposite();
        Set<Direction> axisParallelDirections = ModMth.getAxisParallelDirections(clickedFace.getAxis());
        Direction nearestSide = ModMth.orderedByNearest(blockPlaceContext.getPlayer(), axisParallelDirections).getFirst().getOpposite();
        LShapePipeState state = LShapePipeState.fromDirectionSet(Set.of(clickedFace, nearestSide));
        return this.defaultBlockState().setValue(AXIS, state.axis).setValue(HORIZONTAL_FACING, state.startDirection);
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        LShapePipeState state = LShapePipeState.fromAxisAndStartDirection(blockState.getValue(AXIS), blockState.getValue(HORIZONTAL_FACING));
        Set<Direction> directions = state.getDirections();
        VoxelShape shape = Shapes.empty();
        if (directions.contains(Direction.DOWN)) {
            shape = Shapes.or(
                    shape,
                    Block.box(6.0D, 1.0D, 6.0D, 10.0D, 10.0D, 10.0D),
                    Block.box(5.0D, 0.0D, 5.0D, 11.0D, 1.0D, 11.0D)
            );
        }
        if (directions.contains(Direction.UP)) {
            shape = Shapes.or(
                    shape,
                    Block.box(6.0D, 6.0D, 6.0D, 10.0D, 15.0D, 10.0D),
                    Block.box(5.0D, 15.0D, 5.0D, 11.0D, 16.0D, 11.0D)
            );
        }
        if (directions.contains(Direction.NORTH)) {
            shape = Shapes.or(
                    shape,
                    Block.box(6.0D, 6.0D, 1.0D, 10.0D, 10.0D, 10.0D),
                    Block.box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 1.0D)
            );
        }
        if (directions.contains(Direction.SOUTH)) {
            shape = Shapes.or(
                    shape,
                    Block.box(6.0D, 6.0D, 6.0D, 10.0D, 10.0D, 15.0D),
                    Block.box(5.0D, 5.0D, 15.0D, 11.0D, 11.0D, 16.0D)
            );
        }
        if (directions.contains(Direction.WEST)) {
            shape = Shapes.or(
                    shape,
                    Block.box(1.0D, 6.0D, 6.0D, 10.0D, 10.0D, 10.0D),
                    Block.box(0.0D, 5.0D, 5.0D, 1.0D, 11.0D, 11.0D)
            );
        }
        if (directions.contains(Direction.EAST)) {
            shape = Shapes.or(
                    shape,
                    Block.box(6.0D, 6.0D, 6.0D, 15.0D, 10.0D, 10.0D),
                    Block.box(15.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D)
            );
        }
        if (directions.contains(Direction.DOWN)) {
            shape = Shapes.join(
                    shape,
                    Block.box(7.0D, 0.0D, 7.0D, 9.0D, 9.0D, 9.0D),
                    BooleanOp.ONLY_FIRST
            );
        }
        if (directions.contains(Direction.UP)) {
            shape = Shapes.join(
                    shape,
                    Block.box(7.0D, 7.0D, 7.0D, 9.0D, 16.0D, 9.0D),
                    BooleanOp.ONLY_FIRST
            );
        }
        if (directions.contains(Direction.NORTH)) {
            shape = Shapes.join(
                    shape,
                    Block.box(7.0D, 7.0D, 0.0D, 9.0D, 9.0D, 9.0D),
                    BooleanOp.ONLY_FIRST
            );
        }
        if (directions.contains(Direction.SOUTH)) {
            shape = Shapes.join(
                    shape,
                    Block.box(7.0D, 7.0D, 7.0D, 9.0D, 9.0D, 16.0D),
                    BooleanOp.ONLY_FIRST
            );
        }
        if (directions.contains(Direction.WEST)) {
            shape = Shapes.join(
                    shape,
                    Block.box(0.0D, 7.0D, 7.0D, 9.0D, 9.0D, 9.0D),
                    BooleanOp.ONLY_FIRST
            );
        }
        if (directions.contains(Direction.EAST)) {
            shape = Shapes.join(
                    shape,
                    Block.box(7.0D, 7.0D, 7.0D, 16.0D, 9.0D, 9.0D),
                    BooleanOp.ONLY_FIRST
            );
        }
        return shape;
    }

    @Override
    public boolean isSideConnectable(BlockState blockState, Direction direction) {
        if (direction == null) return false;
        LShapePipeState state = LShapePipeState.fromAxisAndStartDirection(blockState.getValue(AXIS), blockState.getValue(HORIZONTAL_FACING));
        Set<Direction> directions = state.getDirections();
        return directions.contains(direction.getOpposite());
    }
}
