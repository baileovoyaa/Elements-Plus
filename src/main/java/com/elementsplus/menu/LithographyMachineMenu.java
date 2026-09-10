package com.elementsplus.menu;

import com.elementsplus.ModBlocks;
import com.elementsplus.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LithographyMachineMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;

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
    }


    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.LITHOGRAPHY_MACHINE);
    }
}
