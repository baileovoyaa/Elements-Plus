package com.elementsplus.menu;

import com.elementsplus.ModItems;
import com.elementsplus.ModMenuTypes;
import com.elementsplus.core.circuit.ChipManufacture;
import com.elementsplus.core.circuit.CircuitItemData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RedstoneInterfaceMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public RedstoneInterfaceMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(8), new SimpleContainerData(12));
    }

    public RedstoneInterfaceMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModMenuTypes.REDSTONE_INTERFACE, id);
        checkContainerSize(container, 8);
        checkContainerDataCount(data, 12);
        this.container = container;
        this.data = data;

        this.addSlot(new Slot(container, 0, 8, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isChipCarrier(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSlot(new Slot(container, 1, 30, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.AMETHYST_BATTERY);
            }
        });
        for (int i = 0; i < 6; i++) {
            this.addSlot(new Slot(container, 2 + i, 210, 52 + i * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ModItems.DISK);
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 143 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 201));
        }
        this.addDataSlots(data);
    }

    private static boolean isChipCarrier(ItemStack stack) {
        if (stack.isEmpty() || stack.is(ModItems.CIRCUIT_DIAGRAM)) {
            // 电路图不是实际电路载体，不能放进电路槽。
            return false;
        }
        if (ChipManufacture.isCreativeComponent(stack)) {
            return true;
        }
        if (stack.getItem() instanceof com.elementsplus.item.ChipItem) {
            return CircuitItemData.diagram(stack) != null
                    || CircuitItemData.compiledId(stack) != null
                    || CircuitItemData.componentId(stack) != null;
        }
        // 内置元件物品和六种紫水晶元件带有等效元件 id。
        return CircuitItemData.componentId(stack) != null;
    }

    public BlockPos getBlockPos() {
        return new BlockPos(data.get(9), data.get(10), data.get(11));
    }

    public double getAverageMs() {
        return data.get(2) / 1000.0;
    }

    public double getLastStepMs() {
        return data.get(3) / 1000.0;
    }

    public boolean isRunning() {
        return data.get(0) != 0;
    }

    public boolean isHibernating() {
        return data.get(1) != 0;
    }

    public boolean hasCircuit() {
        return data.get(4) != 0;
    }

    public boolean hasBattery() {
        return data.get(5) != 0;
    }

    public int getAvgLimit() {
        return data.get(6);
    }

    public int getMaxLimit() {
        return data.get(7);
    }

    public int getSampleTicks() {
        return data.get(8);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < 8) {
            if (!this.moveItemStackTo(stack, 8, 44, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (stack.is(ModItems.AMETHYST_BATTERY)) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (isChipCarrier(stack)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModItems.DISK)) {
                if (!this.moveItemStackTo(stack, 2, 8, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 8 && index < 35) {
                if (!this.moveItemStackTo(stack, 35, 44, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 35 && index < 44 && !this.moveItemStackTo(stack, 8, 35, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }
}
