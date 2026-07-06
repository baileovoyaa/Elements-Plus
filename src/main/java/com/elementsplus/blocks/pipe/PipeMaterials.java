package com.elementsplus.blocks.pipe;

import com.elementsplus.ModBlocks;
import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class PipeMaterials {

    public static final PipeMaterial RUST = new PipeMaterial();
    public static final PipeMaterial STEEL = new PipeMaterial() {
        private static final Supplier<BiMap<Block, Block>> NEXT_BY_BLOCK = Suppliers.memoize(
                () -> ImmutableBiMap.<Block, Block>builder()
                        .put(ModBlocks.STEEL_PIPE_I, ModBlocks.RUST_STEEL_PIPE_I)
                        .put(ModBlocks.STEEL_PIPE_L, ModBlocks.RUST_STEEL_PIPE_L)
                        .put(ModBlocks.STEEL_PIPE_T, ModBlocks.RUST_STEEL_PIPE_T)
                        .put(ModBlocks.STEEL_PIPE_X, ModBlocks.RUST_STEEL_PIPE_X)
                        .build()
        );

        @Override
        public void onRandomTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
            float f = 0.05688889F;
            if (randomSource.nextFloat() < 0.05688889F) {
                serverLevel.setBlockAndUpdate(blockPos, NEXT_BY_BLOCK.get().get(blockState.getBlock()).withPropertiesOf(blockState));
            }
        }

        @Override
        public boolean isRandomlyTicking(BlockState blockState) {
            return true;
        }
    };
    public static final PipeMaterial SILVER = new PipeMaterial();
}
