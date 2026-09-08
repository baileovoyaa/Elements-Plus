package com.elementsplus.menu;

import com.elementsplus.ModItems;
import com.elementsplus.ModMenuTypes;
import com.elementsplus.blocks.entity.MetalCatalystBlockEntity;
import com.elementsplus.slot.CatalystSlot;
import com.elementsplus.slot.CrystallizerFuelSlot;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.jetbrains.annotations.NotNull;

public class MetalCatalystMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;
    protected final Level level;

    public MetalCatalystMenu(int i, Inventory inventory) {
        this(ModMenuTypes.METAL_CATALYST, i, inventory);
    }

    public MetalCatalystMenu(int i, Inventory inventory, Container container, ContainerData containerData) {
        this(ModMenuTypes.METAL_CATALYST, i, inventory, container, containerData);
    }

    protected MetalCatalystMenu(MenuType<?> menuType, int i, Inventory inventory) {
        this(menuType, i, inventory, new SimpleContainer(5), new SimpleContainerData(5));
    }

    protected MetalCatalystMenu(
            MenuType<?> menuType,
            int i,
            Inventory inventory,
            Container container,
            ContainerData containerData
    ) {
        super(menuType, i);
        checkContainerSize(container, 5);
        checkContainerDataCount(containerData, 5);
        this.container = container;
        this.data = containerData;
        this.level = inventory.player.level();

        this.addSlot(new Slot(container, 0, 56, 17));
        this.addSlot(new Slot(container, 1, 74, 17));
        this.addSlot(new CrystallizerFuelSlot(null, container, 2, 56, 53));
        this.addSlot(new CatalystSlot(this, container, 3, 26, 61));
        this.addSlot(new net.minecraft.world.inventory.FurnaceResultSlot(inventory.player, container, 4, 116, 35));

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

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public boolean clickMenuButton(Player player, int i) {
        if (i == 0) {
            if (this.container instanceof MetalCatalystBlockEntity blockEntity) {
                blockEntity.clearWaste();
            }
            return true;
        }
        return super.clickMenuButton(player, i);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);
        if (slot.hasItem()) {
            ItemStack slotItem = slot.getItem();
            itemStack = slotItem.copy();
            if (i == 4) {
                if (!this.moveItemStackTo(slotItem, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotItem, itemStack);
            } else if (i != 2 && i != 0 && i != 1 && i != 3) {
                if (isFuel(slotItem)) {
                    if (!this.moveItemStackTo(slotItem, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isCatalyst(slotItem)) {
                    if (!this.moveItemStackTo(slotItem, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (i >= 5 && i < 32) {
                    if (!this.moveItemStackTo(slotItem, 32, 41, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (i >= 32 && i < 41 && !this.moveItemStackTo(slotItem, 5, 32, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(slotItem, 5, 41, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotItem.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotItem.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotItem);
        }

        return itemStack;
    }

    public static boolean isFuel(ItemStack itemStack) {
        return AbstractFurnaceBlockEntity.isFuel(itemStack);
    }

    public boolean isCatalyst(ItemStack itemStack) {
        return itemStack.is(ModItems.CATALYST);
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

    public float getWasteProgress() {
        int i = this.data.get(4);
        return Mth.clamp((float) i / 100, 0.0F, 1.0F);
    }

    public boolean isLit() {
        return this.data.get(0) > 0;
    }
}
