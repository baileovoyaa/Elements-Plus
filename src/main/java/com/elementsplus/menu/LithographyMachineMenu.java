package com.elementsplus.menu;

import com.elementsplus.ModBlocks;
import com.elementsplus.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LithographyMachineMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    final Slot diagramSlot;
    Runnable slotUpdateListener = () -> {
    };
    public final Container container = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
            slotUpdateListener.run();
        }
    };

    public LithographyMachineMenu(int i, Inventory inventory) {
        this(i, inventory, ContainerLevelAccess.NULL);
    }

    public LithographyMachineMenu(int i, Inventory inventory, ContainerLevelAccess containerLevelAccess) {
        super(ModMenuTypes.LITHOGRAPHY_MACHINE, i);
        this.access = containerLevelAccess;

        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 9; k++) {
                this.addSlot(new Slot(inventory, k + j * 9 + 9, 6 + j * 18, 8 + k * 18));
            }
        }

        for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(inventory, j, 6 + 3 * 18 + 3, 8 + j * 18));
        }

        diagramSlot = this.addSlot(new Slot(this.container, 0, 90, 26) {
            @Override
            public void onTake(Player player, ItemStack itemStack) {
                super.onTake(player, itemStack);
            }
        });
    }


    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.LITHOGRAPHY_MACHINE);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
    }

    public void onDiagramChanged() {
        this.container.setChanged();
    }

    public void returnCarriedToInventory() {
        ItemStack carried = this.getCarried();
        if (carried.isEmpty()) {
            return;
        }
        this.moveItemStackTo(carried, 0, 36, false);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        // 如果有结果/输出槽，通常不返还，先清空，防止复制物品
        // this.resultContainer.removeItemNoUpdate(1);

        // 返还输入容器中的物品
        this.access.execute((level, blockPos) -> this.clearContainer(player, this.container));
    }
}
