package com.elementsplus.menu;

import com.elementsplus.ModMenuTypes;
import com.elementsplus.blocks.entity.PressureMachineBlockEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import org.jetbrains.annotations.NotNull;

public class PressureMachineMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public PressureMachineMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(1),
                new SimpleContainerData(PressureMachineBlockEntity.DATA_COUNT));
    }

    public PressureMachineMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenuTypes.PRESSURE_MACHINE, containerId);
        checkContainerSize(container, 1);
        checkContainerDataCount(data, PressureMachineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        // 燃料槽
        this.addSlot(new Slot(container, 0, 150, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isFuel(stack);
            }
        });

        // 玩家物品栏
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == 0) {
                if (!this.moveItemStackTo(stack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (isFuel(stack)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 1 && index < 28) {
                if (!this.moveItemStackTo(stack, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 28 && index < 37 && !this.moveItemStackTo(stack, 1, 28, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return result;
    }

    public static boolean isFuel(ItemStack stack) {
        return FurnaceBlockEntity.isFuel(stack);
    }

    // ---- GUI 数据 ----

    public boolean isLit() {
        return this.data.get(PressureMachineBlockEntity.DATA_LIT_TIME) > 0;
    }

    public float getLitProgress() {
        int duration = this.data.get(PressureMachineBlockEntity.DATA_LIT_DURATION);
        if (duration == 0) {
            duration = 200;
        }
        return Mth.clamp((float) this.data.get(PressureMachineBlockEntity.DATA_LIT_TIME) / duration, 0.0F, 1.0F);
    }

    public double getFlow() {
        return this.data.get(PressureMachineBlockEntity.DATA_FLOW) / 1000.0;
    }

    public double getInputPressure() {
        return this.data.get(PressureMachineBlockEntity.DATA_INPUT_PRESSURE) / 1000.0;
    }

    public double getOutputPressure() {
        return this.data.get(PressureMachineBlockEntity.DATA_OUTPUT_PRESSURE) / 1000.0;
    }

    public int getInputVolume() {
        return this.data.get(PressureMachineBlockEntity.DATA_INPUT_VOLUME);
    }

    public int getOutputVolume() {
        return this.data.get(PressureMachineBlockEntity.DATA_OUTPUT_VOLUME);
    }

    public boolean isInputSealed() {
        return this.data.get(PressureMachineBlockEntity.DATA_INPUT_SEALED) != 0;
    }

    public boolean isOutputSealed() {
        return this.data.get(PressureMachineBlockEntity.DATA_OUTPUT_SEALED) != 0;
    }

    public boolean isWorking() {
        return this.data.get(PressureMachineBlockEntity.DATA_WORKING) != 0;
    }

    public boolean isSameSpace() {
        return this.data.get(PressureMachineBlockEntity.DATA_SAME_SPACE) != 0;
    }
}
