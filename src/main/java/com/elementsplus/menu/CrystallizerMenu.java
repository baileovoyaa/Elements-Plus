package com.elementsplus.menu;

import com.elementsplus.ModMenuTypes;
import com.elementsplus.slot.CrystallizerFuelSlot;
import com.elementsplus.slot.PressurizeSlot;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.jetbrains.annotations.NotNull;

public class CrystallizerMenu extends AbstractContainerMenu {
    public boolean isPressurizer(ItemStack itemStack) {
        return itemStack.is(Items.PISTON);
    }

    private final Container container;
    private final ContainerData data;
    protected final Level level;

    public CrystallizerMenu(int i, Inventory inventory) {
        this(ModMenuTypes.CRYSTALLIZER, i, inventory);
    }

    public CrystallizerMenu(int i, Inventory inventory, Container container, ContainerData containerData) {
        this(ModMenuTypes.CRYSTALLIZER, i, inventory, container, containerData);
    }

    protected CrystallizerMenu(MenuType<?> menuType, int i, Inventory inventory) {
        this(menuType, i, inventory, new SimpleContainer(3), new SimpleContainerData(4));
    }

    protected CrystallizerMenu(
            MenuType<?> menuType,
            int i,
            Inventory inventory,
            Container container,
            ContainerData containerData
    ) {
        super(menuType, i);
        checkContainerSize(container, 3);
        checkContainerDataCount(containerData, 4);
        this.container = container;
        this.data = containerData;
        this.level = inventory.player.level();
        this.addSlot(new Slot(container, 0, 56, 17));
        this.addSlot(new CrystallizerFuelSlot(this, container, 1, 56, 53));
        this.addSlot(new PressurizeSlot(this, container, 2, 26, 61));
        this.addSlot(new FurnaceResultSlot(inventory.player, container, 3, 116, 35));

        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 9; k++) {
                this.addSlot(new Slot(inventory, k + j * 9 + 9, 8 + k * 18, 92 + j * 18));
            }
        }

        for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(inventory, j, 8 + j * 18, 150));
        }

        this.addDataSlots(containerData);
    }

    public void fillCraftSlotsStackedContents(StackedContents stackedContents) {
        if (this.container instanceof StackedContentsCompatible) {
            ((StackedContentsCompatible) this.container).fillStackedContents(stackedContents);
        }
    }


    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);
        if (slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (i == 2) {
                if (!this.moveItemStackTo(itemStack2, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack2, itemStack);
            } else if (i != 1 && i != 0) {
                if (this.canSmelt(itemStack2)) {
                    if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.isFuel(itemStack2)) {
                    if (!this.moveItemStackTo(itemStack2, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (i >= 3 && i < 30) {
                    if (!this.moveItemStackTo(itemStack2, 30, 39, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (i >= 30 && i < 39 && !this.moveItemStackTo(itemStack2, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 3, 39, false)) {
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

    protected boolean canSmelt(ItemStack itemStack) {
        return itemStack.is(Items.SAND);
    }

    public boolean isFuel(ItemStack itemStack) {
        return AbstractFurnaceBlockEntity.isFuel(itemStack);
    }

    public float getBurnProgress() {
        int i = this.data.get(2);
        int j = this.data.get(3);
        return j != 0 && i != 0 ? Mth.clamp((float) i / j, 0.0F, 1.0F) : 0.0F;
    }

    public float getLitProgress() {
        int i = this.data.get(1);
        if (i == 0) {
            i = 200;
        }

        return Mth.clamp((float) this.data.get(0) / i, 0.0F, 1.0F);
    }

    public boolean isLit() {
        return this.data.get(0) > 0;
    }
}
