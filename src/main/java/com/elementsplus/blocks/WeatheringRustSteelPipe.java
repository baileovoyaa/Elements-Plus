package com.elementsplus.blocks;

import com.elementsplus.ModBlocks;
import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public interface WeatheringRustSteelPipe extends ChangeOverTimeBlock<WeatheringRustSteelPipe.WeatherState> {

    // ===== 下一阶段映射（氧化） =====
    Supplier<BiMap<Block, Block>> NEXT_BY_BLOCK = Suppliers.memoize(
            () -> ImmutableBiMap.<Block, Block>builder()
                    .put(ModBlocks.STEEL_PIPE_I, ModBlocks.RUST_STEEL_PIPE_I)
                    .put(ModBlocks.STEEL_PIPE_L, ModBlocks.RUST_STEEL_PIPE_L)
                    .put(ModBlocks.STEEL_PIPE_T, ModBlocks.RUST_STEEL_PIPE_T)
                    .put(ModBlocks.STEEL_PIPE_X, ModBlocks.RUST_STEEL_PIPE_X)
                    .build()
    );

    Supplier<BiMap<Block, Block>> PREVIOUS_BY_BLOCK = Suppliers.memoize(() -> NEXT_BY_BLOCK.get().inverse());

    static Optional<Block> getPrevious(Block block) {
        return Optional.ofNullable(PREVIOUS_BY_BLOCK.get().get(block));
    }

    static Block getFirst(Block block) {
        Block block2 = block;
        for (Block block3 = PREVIOUS_BY_BLOCK.get().get(block2); block3 != null; block3 = PREVIOUS_BY_BLOCK.get().get(block2)) {
            block2 = block3;
        }
        return block2;
    }

    static Optional<BlockState> getPrevious(BlockState blockState) {
        return getPrevious(blockState.getBlock()).map(block -> block.withPropertiesOf(blockState));
    }

    static Optional<Block> getNext(Block block) {
        return Optional.ofNullable(NEXT_BY_BLOCK.get().get(block));
    }

    static BlockState getFirst(BlockState blockState) {
        return getFirst(blockState.getBlock()).withPropertiesOf(blockState);
    }

    @Override
    default @NotNull Optional<BlockState> getNext(BlockState blockState) {
        return getNext(blockState.getBlock()).map(block -> block.withPropertiesOf(blockState));
    }

    @Override
    default float getChanceModifier() {
        // UNAFFECTED 阶段氧化速度稍慢，与原版铜一致
        return this.getAge() == WeatherState.UNAFFECTED ? 0.75F : 1.0F;
    }

    // ===== 氧化阶段枚举 =====
    enum WeatherState implements StringRepresentable {
        UNAFFECTED("unaffected"),   // 崭新（轻度锈蚀）
        RUST("rust");    // 氧化（完全锈透）

        public static final Codec<WeatherState> CODEC = StringRepresentable.fromEnum(WeatherState::values);
        private final String name;

        WeatherState(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.name;
        }
    }
}