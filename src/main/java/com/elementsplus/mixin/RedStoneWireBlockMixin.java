package com.elementsplus.mixin;

import com.elementsplus.blocks.pipe.AbstractPipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.world.level.block.RedStoneWireBlock.POWER;

@Mixin(RedStoneWireBlock.class)
public class RedStoneWireBlockMixin {

    @Shadow
    private boolean shouldSignal;

    @Inject(at = @At("RETURN"), method = "shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z", cancellable = true)
    private static void shouldConnectTo(BlockState blockState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (blockState.getBlock() instanceof AbstractPipeBlock pipeBlock) {
            cir.setReturnValue(pipeBlock.isSideConnectable(blockState, direction));
        }
    }

    @Redirect(
            method = {"updateIndirectNeighbourShapes", "getWireSignal", "checkCornerChangeAt"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            )
    )
    private boolean redirectIs(BlockState blockState, Block block) {
        return blockState.is(block) || (block == Blocks.REDSTONE_WIRE && blockState.getBlock() instanceof AbstractPipeBlock);
    }

    /**
     * @author ElementsPlus
     * @reason Overwrite the calculateTargetStrength method to use the custom wire signal calculation.
     */
    @Overwrite
    private int calculateTargetStrength(Level level, BlockPos blockPos) {
        this.shouldSignal = false;
        int i = level.getBestNeighborSignal(blockPos);
        this.shouldSignal = true;
        int j = 0;
        if (i < 15) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos blockPos2 = blockPos.relative(direction);
                BlockState blockState = level.getBlockState(blockPos2);
                j = Math.max(j, AbstractPipeBlock.getWireSignal(blockState, direction));
                BlockPos blockPos3 = blockPos.above();
                if (blockState.isRedstoneConductor(level, blockPos2) && !level.getBlockState(blockPos3).isRedstoneConductor(level, blockPos3)) {
                    j = Math.max(j, AbstractPipeBlock.getWireSignal(level.getBlockState(blockPos2.above()), direction, true));
                } else if (!blockState.isRedstoneConductor(level, blockPos2)) {
                    j = Math.max(j, AbstractPipeBlock.getWireSignal(level.getBlockState(blockPos2.below()), direction, true));
                }
            }
            BlockPos blockPos2 = blockPos.relative(Direction.UP);
            BlockState blockState = level.getBlockState(blockPos2);
            j = Math.max(j, AbstractPipeBlock.getWireSignal(blockState, Direction.UP));
        }

        return Math.max(i, j - 1);
    }
}
