package com.elementsplus.blocks;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SteelPipeBlock extends AbstractPipeBlock implements WeatheringRustSteelPipe {
    private final WeatheringRustSteelPipe.WeatherState weatherState;
    public static final MapCodec<SteelPipeBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    WeatheringRustSteelPipe.WeatherState.CODEC.fieldOf("weathering_state")
                            .forGetter(SteelPipeBlock::getAge),
                    propertiesCodec()
            ).apply(instance, SteelPipeBlock::new)
    );

    public SteelPipeBlock(WeatheringRustSteelPipe.WeatherState weatherState, BlockBehaviour.Properties properties) {
        super(properties);
        this.weatherState = weatherState;
    }

    @Override
    protected @NotNull MapCodec<? extends Block> codec() {
        return CODEC;
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
