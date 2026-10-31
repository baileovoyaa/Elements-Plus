package com.elementsplus.blocks;

import com.elementsplus.ModBlockEntityTypes;
import com.elementsplus.blocks.entity.PressureMachineBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 加压机：前面（{@link #FACING}）为出口，后面为其反方向入口。
 * 当前为原型实现，参数与机制仍会在实测后调整。
 */
public class PressureMachineBlock extends BaseEntityBlock {
    public static final MapCodec<PressureMachineBlock> CODEC = simpleCodec(PressureMachineBlock::new);

    /** 方块正面（出口）的方向，支持 6 个朝向。 */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public PressureMachineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    protected @NotNull MapCodec<PressureMachineBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PressureMachineBlockEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null
                : createTickerHelper(blockEntityType, ModBlockEntityTypes.PRESSURE_MACHINE, PressureMachineBlockEntity::serverTick);
    }

    protected void openContainer(Level level, BlockPos blockPos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof PressureMachineBlockEntity) {
            player.openMenu((MenuProvider) blockEntity);
            player.awardStat(Stats.INTERACT_WITH_FURNACE);
        }
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos blockPos,
                                                        Player player, BlockHitResult blockHitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        this.openContainer(level, blockPos, player);
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 让正面朝向玩家。
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public void animateTick(BlockState blockState, Level level, BlockPos blockPos, RandomSource randomSource) {
        if (!blockState.getValue(LIT) || !level.isClientSide
                || !(level.getBlockEntity(blockPos) instanceof PressureMachineBlockEntity machine)) {
            return;
        }
        double flow = machine.getFlow();
        if (flow <= 0.0) {
            return;
        }

        Direction facing = blockState.getValue(FACING);
        double axisX = facing.getStepX();
        double axisY = facing.getStepY();
        double axisZ = facing.getStepZ();
        double centerX = blockPos.getX() + 0.5;
        double centerY = blockPos.getY() + 0.5;
        double centerZ = blockPos.getZ() + 0.5;

        double x = centerX + axisX * 0.52;
        double y = centerY + axisY * 0.52;
        double z = centerZ + axisZ * 0.52;
        if (randomSource.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }

        // 流量越大，灰尘越多、沿轴向速度越快。
        double strength = Math.min(1.0, flow / 8.0);
        int count = 1 + (int) Math.floor(strength * 3.0);
        double speed = 0.06 + strength * 0.20;

        for (int i = 0; i < count; i++) {
            // 去掉沿流速方向的分量，得到垂直于气流轴的随机散布。
            double offsetX = randomSource.nextDouble() - 0.5;
            double offsetY = randomSource.nextDouble() - 0.5;
            double offsetZ = randomSource.nextDouble() - 0.5;
            double projection = offsetX * axisX + offsetY * axisY + offsetZ * axisZ;
            offsetX = (offsetX - projection * axisX) * 0.35;
            offsetY = (offsetY - projection * axisY) * 0.35;
            offsetZ = (offsetZ - projection * axisZ) * 0.35;

            // 入口侧：从方块外侧向中心飞入。
            double entryDistance = 0.5 + randomSource.nextDouble() * 0.25;
            level.addParticle(ParticleTypes.DUST_PLUME,
                    centerX - axisX * entryDistance + offsetX,
                    centerY - axisY * entryDistance + offsetY,
                    centerZ - axisZ * entryDistance + offsetZ,
                    axisX * speed, axisY * speed - 0.08, axisZ * speed);

            // 出口侧：从中心附近向方块外侧飞出。
            double outputDistance = 0.15 + randomSource.nextDouble() * 0.25;
            level.addParticle(ParticleTypes.DUST_PLUME,
                    centerX + axisX * outputDistance + offsetX,
                    centerY + axisY * outputDistance + offsetY,
                    centerZ + axisZ * outputDistance + offsetZ,
                    axisX * speed, axisY * speed - 0.08, axisZ * speed);
        }
    }

    @Override
    protected void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        if (!blockState.is(blockState2.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity instanceof PressureMachineBlockEntity && level instanceof ServerLevel) {
                Containers.dropContents(level, blockPos, (PressureMachineBlockEntity) blockEntity);
            }
            super.onRemove(blockState, level, blockPos, blockState2, bl);
            level.updateNeighbourForOutputSignal(blockPos, this);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(blockPos));
    }

    @Override
    protected @NotNull RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull BlockState rotate(BlockState blockState, Rotation rotation) {
        return blockState.setValue(FACING, rotation.rotate(blockState.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(BlockState blockState, Mirror mirror) {
        return blockState.rotate(mirror.getRotation(blockState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }
}
