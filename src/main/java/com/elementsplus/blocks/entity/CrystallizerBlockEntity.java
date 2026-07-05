package com.elementsplus.blocks.entity;

import com.elementsplus.ModBlockEntityTypes;
import com.elementsplus.menu.CrystallizerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class CrystallizerBlockEntity extends AbstractFurnaceBlockEntity {
    public CrystallizerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntityTypes.COUNTER_BLOCK_ENTITY, blockPos, blockState, RecipeType.SMELTING);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.elements-plus.crystallizer");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return new CrystallizerMenu(i, inventory, this, this.dataAccess);
    }
}
