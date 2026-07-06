package com.elementsplus.blocks;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class RustSteelPipe extends Block implements WeatheringRustSteelPipe {
    public static final MapCodec<RustSteelPipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    WeatheringRustSteelPipe.WeatherState.CODEC.fieldOf("weathering_state")
                            .forGetter(ChangeOverTimeBlock::getAge),
                    propertiesCodec()
            ).apply(instance, RustSteelPipe::new)
    );

    private final WeatheringRustSteelPipe.WeatherState weatherState;

    @Override
    public @NotNull MapCodec<RustSteelPipe> codec() {
        return CODEC;
    }

    public RustSteelPipe(WeatheringRustSteelPipe.WeatherState weatherState, BlockBehaviour.Properties properties) {
        super(properties);
        this.weatherState = weatherState;
    }

    @Override
    protected void randomTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
        this.changeOverTime(blockState, serverLevel, blockPos, randomSource);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState blockState) {
        return WeatheringRustSteelPipe.getNext(blockState.getBlock()).isPresent();
    }

    @Override
    public WeatheringRustSteelPipe.@NotNull WeatherState getAge() {
        return this.weatherState;
    }
}