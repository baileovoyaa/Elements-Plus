package com.elementsplus.menu;

import com.elementsplus.ModDataComponents;
import com.elementsplus.ModItems;
import com.elementsplus.ModMenuTypes;
import com.elementsplus.item.PaperTapeReaderItem;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 纸带阅读器界面：一个只能放入打孔文件的槽位，内容保存在阅读器物品的数据组件中。
 */
public class PaperTapeReaderMenu extends AbstractContainerMenu {
    private final ItemStack reader;
    private final Container container;
    /** 主手打开时锁定的快捷栏格，防止把阅读器移走/替换。 */
    private int lockedInventoryIndex = -1;

    public PaperTapeReaderMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public PaperTapeReaderMenu(int containerId, Inventory inventory, @Nullable InteractionHand hand) {
        super(ModMenuTypes.PAPER_TAPE_READER, containerId);
        this.reader = findReader(inventory.player);
        if (hand == InteractionHand.MAIN_HAND
                || (hand == null && inventory.player.getMainHandItem().is(ModItems.PAPER_TAPE_READER))) {
            this.lockedInventoryIndex = inventory.selected;
        }
        this.container = new ReaderContainer(this.reader);
        this.addSlot(new Slot(this.container, 0, 80, 32) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.PUNCHED_FILE);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(playerSlot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(playerSlot(inventory, column, 8 + column * 18, 142));
        }
    }

    private Slot playerSlot(Inventory inventory, int inventoryIndex, int x, int y) {
        return new Slot(inventory, inventoryIndex, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return inventoryIndex != lockedInventoryIndex;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return inventoryIndex != lockedInventoryIndex;
            }
        };
    }

    private static ItemStack findReader(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(ModItems.PAPER_TAPE_READER)) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (off.is(ModItems.PAPER_TAPE_READER)) {
            return off;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.PAPER_TAPE_READER)) {
                return stack;
            }
        }
        return main;
    }

    public boolean hasReader() {
        return this.reader.is(ModItems.PAPER_TAPE_READER);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            if (index != 0) {
                int inventoryIndex = index <= 27 ? index + 8 : index - 28;
                if (inventoryIndex == lockedInventoryIndex) {
                    return ItemStack.EMPTY;
                }
            }
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == 0) {
                if (!this.moveItemStackTo(stack, 1, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, 1, false)) {
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.reader.is(ModItems.PAPER_TAPE_READER);
    }

    /** 读写阅读器物品数据组件的容器。 */
    private static final class ReaderContainer implements Container {
        private final ItemStack reader;

        private ReaderContainer(ItemStack reader) {
            this.reader = reader;
        }

        private boolean isReader() {
            return !this.reader.isEmpty() && this.reader.is(ModItems.PAPER_TAPE_READER);
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return !isReader() || PaperTapeReaderItem.getContents(this.reader).isEmpty();
        }

        @Override
        public @NotNull ItemStack getItem(int index) {
            if (!isReader()) {
                return ItemStack.EMPTY;
            }
            return index == 0 ? PaperTapeReaderItem.getContents(this.reader) : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack removeItem(int index, int count) {
            ItemStack current = getItem(index);
            if (current.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack result = current.split(count);
            setItem(index, current);
            return result;
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int index) {
            ItemStack current = getItem(index);
            this.reader.remove(ModDataComponents.PAPER_TAPE_CONTENTS);
            return current;
        }

        @Override
        public void setItem(int index, @NotNull ItemStack stack) {
            if (!isReader() || index != 0) {
                return;
            }
            if (stack.isEmpty()) {
                this.reader.remove(ModDataComponents.PAPER_TAPE_CONTENTS);
            } else {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                this.reader.set(ModDataComponents.PAPER_TAPE_CONTENTS, copy);
            }
            setChanged();
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return isReader();
        }

        @Override
        public void clearContent() {
            this.reader.remove(ModDataComponents.PAPER_TAPE_CONTENTS);
        }
    }
}
