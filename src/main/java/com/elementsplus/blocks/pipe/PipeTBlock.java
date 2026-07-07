package com.elementsplus.blocks.pipe;

import com.elementsplus.ModMth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PipeTBlock extends AbstractPipeBlock {
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;


    public PipeTBlock(Properties properties, PipeMaterial pipeMaterial) {
        super(properties, pipeMaterial);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_FACING);
    }

    @Override
    public @NotNull BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        Direction.Axis axis = blockPlaceContext.getClickedFace().getAxis();
        Player player = blockPlaceContext.getPlayer();
        List<Direction> directions = getAxisBasedDirections(axis);
        Direction realDirection = ModMth.orderedByNearest(player, directions).getFirst().getOpposite();
        Direction mappedDirection = getHorizontalDirections().get(directions.indexOf(realDirection));
        return this.defaultBlockState().setValue(AXIS, axis).setValue(HORIZONTAL_FACING, mappedDirection);
    }

    private List<Direction> getAxisBasedDirections(Direction.Axis axis) {
        return switch (axis) {
            case X -> List.of(Direction.UP, Direction.SOUTH, Direction.DOWN, Direction.NORTH);
            case Y -> List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
            case Z -> List.of(Direction.UP, Direction.WEST, Direction.DOWN, Direction.EAST);
        };
    }

    public static List<Direction> getHorizontalDirections() {
        return List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        Direction.Axis axis = blockState.getValue(RotatedPillarBlock.AXIS);
        VoxelShape shape = switch (axis) {
            case X -> Shapes.or(
                    Block.box(1.0, 6.0, 6.0, 15.0, 10.0, 10.0),  // base
                    Block.box(0.0, 5.0, 5.0, 1.0, 11.0, 11.0),  // joint1
                    Block.box(15.0, 5.0, 5.0, 16.0, 11.0, 11.0)  // joint2
            );
            case Y -> Shapes.or(
                    Block.box(6.0, 1.0, 6.0, 10.0, 15.0, 10.0),  // base
                    Block.box(5.0, 0.0, 5.0, 11.0, 1.0, 11.0),  // joint_bottom
                    Block.box(5.0, 15.0, 5.0, 11.0, 16.0, 11.0)  // joint_top
            );
            case Z -> Shapes.or(
                    Block.box(6.0, 6.0, 1.0, 10.0, 10.0, 15.0),  // base
                    Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 1.0),  // joint1
                    Block.box(5.0, 5.0, 15.0, 11.0, 11.0, 16.0)  // joint2
            );
        };
        Direction facing = blockState.getValue(HORIZONTAL_FACING);
        Direction realFacing = getAxisBasedDirections(axis).get(getHorizontalDirections().indexOf(facing));
        shape = Shapes.or(shape, switch (realFacing) {
                    case DOWN -> Shapes.or(
                            Block.box(6.0, 1.0, 6.0, 10.0, 8.0, 10.0),  // base
                            Block.box(5.0, 0.0, 5.0, 11.0, 1.0, 11.0)  // joint
                    );
                    case UP -> Shapes.or(
                            Block.box(6.0, 8.0, 6.0, 10.0, 15.0, 10.0),  // base
                            Block.box(5.0, 15.0, 5.0, 11.0, 16.0, 11.0)  // joint
                    );
                    case NORTH -> Shapes.or(
                            Block.box(6.0, 6.0, 1.0, 10.0, 10.0, 8.0),  // base
                            Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 1.0)  // joint
                    );
                    case SOUTH -> Shapes.or(
                            Block.box(6.0, 6.0, 8.0, 10.0, 10.0, 15.0),  // base
                            Block.box(5.0, 5.0, 15.0, 11.0, 11.0, 16.0)  // joint
                    );
                    case WEST -> Shapes.or(
                            Block.box(0.0, 6.0, 6.0, 8.0, 10.0, 10.0),  // base
                            Block.box(0.0, 5.0, 5.0, 1.0, 11.0, 11.0)  // joint
                    );
                    case EAST -> Shapes.or(
                            Block.box(8.0, 6.0, 6.0, 16.0, 10.0, 10.0),  // base
                            Block.box(15.0, 5.0, 5.0, 16.0, 11.0, 11.0)  // joint
                    );
                }
        );
        shape = Shapes.join(shape, switch (axis) {
            case X -> Block.box(0.0, 7.0, 7.0, 16.0, 9.0, 9.0);
            case Y -> Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);
            case Z -> Block.box(7.0, 7.0, 0.0, 9.0, 9.0, 16.0);
        }, BooleanOp.ONLY_FIRST);
        shape = Shapes.join(shape, switch (realFacing) {
            case DOWN -> Block.box(7.0, 0.0, 7.0, 9.0, 8.0, 9.0);
            case UP -> Block.box(7.0, 8.0, 7.0, 9.0, 16.0, 9.0);
            case NORTH -> Block.box(7.0, 7.0, 0.0, 9.0, 9.0, 8.0);
            case SOUTH -> Block.box(7.0, 7.0, 8.0, 9.0, 9.0, 16.0);
            case WEST -> Block.box(0.0, 7.0, 7.0, 8.0, 9.0, 9.0);
            case EAST -> Block.box(8.0, 7.0, 7.0, 16.0, 9.0, 9.0);
        }, BooleanOp.ONLY_FIRST);
        return shape;
    }


    @Override
    public boolean isSideConnectable(BlockState blockState, Direction direction) {
        Direction mappedDirection = blockState.getValue(HORIZONTAL_FACING);
        Direction realDirection = getAxisBasedDirections(blockState.getValue(RotatedPillarBlock.AXIS)).get(getHorizontalDirections().indexOf(mappedDirection));

        return direction == realDirection.getOpposite() || switch (direction) {
            case EAST, WEST -> blockState.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X;
            case NORTH, SOUTH -> blockState.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Z;
            case UP, DOWN -> blockState.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y;
        };
    }
}
