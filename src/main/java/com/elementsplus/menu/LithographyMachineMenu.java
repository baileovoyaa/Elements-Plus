package com.elementsplus.menu;

import com.elementsplus.ModBlocks;
import com.elementsplus.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
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
