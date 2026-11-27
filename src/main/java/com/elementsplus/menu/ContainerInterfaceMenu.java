package com.elementsplus.menu;

import com.elementsplus.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 容器接口菜单：沿用红石接口的 8 个槽位布局，并在右侧增加 IS 中转槽。
 * 红石接口父菜单只看到前 8 格，因此用一个 8 格视图包装真实 9 格容器。
 */
public class ContainerInterfaceMenu extends RedstoneInterfaceMenu {
    public static final int IS_SLOT_INDEX = 44;

    private final Container container;

    public ContainerInterfaceMenu(int id, Inventory inventory) {
        this(ModMenuTypes.CONTAINER_INTERFACE, id, inventory,
                new SimpleContainer(9), new SimpleContainerData(12));
    }

    public ContainerInterfaceMenu(int id, Inventory inventory, Container container,
                                  ContainerData data) {
        this(ModMenuTypes.CONTAINER_INTERFACE, id, inventory, container, data);
    }

    protected ContainerInterfaceMenu(net.minecraft.world.inventory.MenuType<?> type, int id,
                                     Inventory inventory, Container container, ContainerData data) {
        super(type, id, inventory, new EightSlotView(container), data);
        this.container = container;
        this.addSlot(new Slot(container, 8, 180, 20));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        if (index == IS_SLOT_INDEX) {
            Slot slot = this.slots.get(index);
            if (!slot.hasItem()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = slot.getItem();
            ItemStack result = stack.copy();
            if (!this.moveItemStackTo(stack, 8, 44, true)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return result;
        }
        return super.quickMoveStack(player, index);
    }

    private record EightSlotView(Container delegate) implements Container {
        @Override
        public int getContainerSize() {
            return 8;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < 8; i++) {
                if (!delegate.getItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public @NotNull ItemStack getItem(int slot) {
            return slot >= 0 && slot < 8 ? delegate.getItem(slot) : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack removeItem(int slot, int amount) {
            return delegate.removeItem(slot, amount);
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot) {
            return delegate.removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, @NotNull ItemStack stack) {
            delegate.setItem(slot, stack);
        }

        @Override
        public int getMaxStackSize() {
            return delegate.getMaxStackSize();
        }

        @Override
        public void setChanged() {
            delegate.setChanged();
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return delegate.stillValid(player);
        }

        @Override
        public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
            return slot >= 0 && slot < 8 && delegate.canPlaceItem(slot, stack);
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < 8; i++) {
                delegate.setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
