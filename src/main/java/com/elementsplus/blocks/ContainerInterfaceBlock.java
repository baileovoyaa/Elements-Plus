package com.elementsplus.blocks;

import com.elementsplus.blocks.entity.ContainerInterfaceBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** 容器接口：红石接口 + 相邻容器访问 + IS 槽位。 */
public class ContainerInterfaceBlock extends RedstoneInterfaceBlock {
    public static final MapCodec<ContainerInterfaceBlock> CODEC =
            simpleCodec(ContainerInterfaceBlock::new);

    public ContainerInterfaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends RedstoneInterfaceBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ContainerInterfaceBlockEntity(pos, state);
    }
}
