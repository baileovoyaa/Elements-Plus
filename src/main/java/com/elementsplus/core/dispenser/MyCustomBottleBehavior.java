package com.elementsplus.core.dispenser;

import com.elementsplus.ModBlocks;
import com.elementsplus.ModItems;
import com.elementsplus.blocks.entity.MetalCatalystBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

public class MyCustomBottleBehavior extends OptionalDispenseItemBehavior {
    @Override
    protected @NotNull ItemStack execute(BlockSource blockSource, ItemStack stack) {
        // 1. 默认认为操作失败
        this.setSuccess(false);

        // 2. 获取发射器前方的方块
        Direction facing = blockSource.state().getValue(DispenserBlock.FACING);
        BlockPos targetPos = blockSource.pos().relative(facing);
        Level level = blockSource.level();
        BlockState targetBlockState = level.getBlockState(targetPos);

        // 3. 检查目标方块是否是自定义方块
        if (targetBlockState.is(ModBlocks.CHARGED_LIGHTNING_ROD)) {
            // 4. 执行你的自定义逻辑 (例如, 对方块进行操作)
            // ...
            level.setBlockAndUpdate(targetPos, Blocks.LIGHTNING_ROD.withPropertiesOf(targetBlockState));
            // 假设操作成功
            level.gameEvent(null, GameEvent.FLUID_PICKUP, blockSource.pos());
            this.setSuccess(true);
            // 返回使用后的物品，例如一个空瓶
            return this.consumeWithRemainder(blockSource, stack, ModItems.LIGHTNING_BOTTLE.getDefaultInstance());
        }

        if (targetBlockState.is(ModBlocks.METAL_CATALYST)) {
            BlockEntity blockEntity = level.getBlockEntity(targetPos);
            if (blockEntity instanceof MetalCatalystBlockEntity metalCatalyst && metalCatalyst.getWaste() >= 50) {
                metalCatalyst.extractWaste(50);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, blockSource.pos());
                this.setSuccess(true);
                return this.consumeWithRemainder(blockSource, stack, ModItems.WASTE_BOTTLE.getDefaultInstance());
            }
        }

        // 5. 如果不是自定义方块，执行水瓶的默认行为
        return super.execute(blockSource, stack);
    }
}