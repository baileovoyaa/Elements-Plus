package com.elementsplus.menu;

import com.elementsplus.ModBlocks;
import com.elementsplus.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ExperimentTableMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    public final Container container;
    public final Slot extraSlot;

    public ExperimentTableMenu(int i, Inventory inventory) {
        this(i, inventory, new SimpleContainer(1), ContainerLevelAccess.NULL);
    }

    public ExperimentTableMenu(int i, Inventory inventory, Container container, ContainerLevelAccess containerLevelAccess) {
        super(ModMenuTypes.EXPERIMENT_TABLE, i);
        this.access = containerLevelAccess;
        this.container = container;
        extraSlot = this.addSlot(new Slot(container, 0, 90, 26));

        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 9; k++) {
                this.addSlot(new Slot(inventory, k + j * 9 + 9, 6 + j * 18, 8 + k * 18));
            }
        }

        for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(inventory, j, 6 + 3 * 18 + 3, 8 + j * 18));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (i < 36) {
                if (!this.moveItemStackTo(itemStack2, 36, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 0, 36, false)) {
                return ItemStack.EMPTY;
            }
            if (itemStack2.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, itemStack2);
        }
        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.EXPERIMENT_TABLE);
    }
}