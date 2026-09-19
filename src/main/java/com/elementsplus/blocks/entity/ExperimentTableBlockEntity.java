package com.elementsplus.blocks.entity;

import com.elementsplus.ModBlockEntityTypes;
import com.elementsplus.menu.ExperimentTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * 实验桌方块实体：额外槽位会持久保存在这里。
 */
public class ExperimentTableBlockEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private String selectedChapter = null;

    public ExperimentTableBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntityTypes.EXPERIMENT_TABLE, blockPos, blockState);
    }

    public String getSelectedChapter() {
        return selectedChapter;
    }

    public void setSelectedChapter(String selectedChapter) {
        if (!Objects.equals(this.selectedChapter, selectedChapter)) {
            this.selectedChapter = selectedChapter;
            this.setChanged();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag, provider);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compoundTag, this.items, provider);
        this.selectedChapter = compoundTag.contains("SelectedChapter") ? compoundTag.getString("SelectedChapter") : null;
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.saveAdditional(compoundTag, provider);
        ContainerHelper.saveAllItems(compoundTag, this.items, provider);
        if (this.selectedChapter != null) {
            compoundTag.putString("SelectedChapter", this.selectedChapter);
        }
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public void setItem(int index, ItemStack itemStack) {
        this.getItems().set(index, itemStack);
        if (!itemStack.isEmpty()) {
            itemStack.setCount(Math.min(this.getMaxStackSize(), itemStack.getCount()));
        }
        this.setChanged();
    }

    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonNullList) {
        this.items = nonNullList;
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.elements-plus.experiment_table");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return new ExperimentTableMenu(i, inventory, this, ContainerLevelAccess.create(this.level, this.worldPosition));
    }
}