package com.elementsplus.mixin;

import com.elementsplus.blocks.pipe.AbstractPipeBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RedStoneWireBlock.class)
public class RedStoneWireBlockMixin {
    @Inject(at = @At("RETURN"), method = "shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z", cancellable = true)
    private static void shouldConnectTo(BlockState blockState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            if (blockState.getBlock() instanceof AbstractPipeBlock pipeBlock) {
                cir.setReturnValue(pipeBlock.isSideConnectable(blockState, direction));
            }
        }
    }
}
