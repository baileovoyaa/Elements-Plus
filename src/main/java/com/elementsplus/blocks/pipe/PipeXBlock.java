package com.elementsplus.blocks.pipe;

import com.elementsplus.ModMth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PipeXBlock extends AbstractPipeBlock {

    public PipeXBlock(Properties properties, PipeMaterial pipeMaterial) {
        super(properties, pipeMaterial);
    }

    @Override
    public @NotNull BlockState getStateForPlacement(BlockPlaceContext context) {
        // 与 L 型管相同：一侧连接被点击的面，另一侧取玩家相对方向。
        Direction clickedSide = context.getClickedFace().getOpposite();
        Set<Direction> candidates = ModMth.getAxisParallelDirections(clickedSide.getAxis());
        Direction playerSide = ModMth.orderedByNearest(context.getPlayer(), candidates)
                .getFirst().getOpposite();
        // X 型管的轴垂直于选定的两条支臂。
        Direction.Axis axis = perpendicularAxis(clickedSide.getAxis(), playerSide.getAxis());
        return this.defaultBlockState().setValue(AXIS, axis);
    }

    private static Direction.Axis perpendicularAxis(Direction.Axis first, Direction.Axis second) {
        for (Direction.Axis axis : Direction.Axis.values()) {
            if (axis != first && axis != second) {
                return axis;
            }
        }
        throw new IllegalArgumentException("The two sides of the pipe cannot be located on the same axis: " + first + ", " + second);
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        Direction.Axis axis = blockState.getValue(RotatedPillarBlock.AXIS);
        return switch (axis) {
            case X -> Shapes.join(Shapes.or(
                    Block.box(6.0, 1.0, 6.0, 10.0, 15.0, 10.0),  // base Y
                    Block.box(5.0, 0.0, 5.0, 11.0, 1.0, 11.0),  // joint Y1
                    Block.box(5.0, 15.0, 5.0, 11.0, 16.0, 11.0),  // joint Y2
                    Block.box(6.0, 6.0, 1.0, 10.0, 10.0, 15.0),  // base Z
                    Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 1.0),  // joint Z1
                    Block.box(5.0, 5.0, 15.0, 11.0, 11.0, 16.0)  // joint Z2
            ), Shapes.or(
                    Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0),
                    Block.box(7.0, 7.0, 0.0, 9.0, 9.0, 16.0)
            ), BooleanOp.ONLY_FIRST);
            case Y -> Shapes.join(Shapes.or(
                    Block.box(1.0, 6.0, 6.0, 15.0, 10.0, 10.0),  // base X
                    Block.box(0.0, 5.0, 5.0, 1.0, 11.0, 11.0),  // joint X1
                    Block.box(15.0, 5.0, 5.0, 16.0, 11.0, 11.0),  // joint X2
                    Block.box(6.0, 6.0, 1.0, 10.0, 10.0, 15.0),  // base Z
                    Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 1.0),  // joint Z1
                    Block.box(5.0, 5.0, 15.0, 11.0, 11.0, 16.0)  // joint Z2
            ), Shapes.or(
                    Block.box(0.0, 7.0, 7.0, 16.0, 9.0, 9.0),
                    Block.box(7.0, 7.0, 0.0, 9.0, 9.0, 16.0)
            ), BooleanOp.ONLY_FIRST);
            case Z -> Shapes.join(Shapes.or(
                    Block.box(1.0, 6.0, 6.0, 15.0, 10.0, 10.0),  // base X
                    Block.box(0.0, 5.0, 5.0, 1.0, 11.0, 11.0),  // joint X1
                    Block.box(15.0, 5.0, 5.0, 16.0, 11.0, 11.0),  // joint X2
                    Block.box(6.0, 1.0, 6.0, 10.0, 15.0, 10.0),  // base Y
                    Block.box(5.0, 0.0, 5.0, 11.0, 1.0, 11.0),  // joint Y1
                    Block.box(5.0, 15.0, 5.0, 11.0, 16.0, 11.0)  // joint Y2
            ), Shapes.or(
                    Block.box(0.0, 7.0, 7.0, 16.0, 9.0, 9.0),
                    Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0)
            ), BooleanOp.ONLY_FIRST);
        };
    }


    @Override
    public boolean isSideConnectable(BlockState blockState, Direction direction) {
        if (direction == null) return false;

        return switch (direction) {
            case EAST, WEST -> blockState.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.X;
            case UP, DOWN -> blockState.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y;
            case NORTH, SOUTH -> blockState.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Z;
        };
    }
}
