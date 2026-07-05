package com.elementsplus.slot;

import com.elementsplus.menu.CrystallizerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CrystallizerFuelSlot extends Slot {
    private final CrystallizerMenu menu;

    public CrystallizerFuelSlot(CrystallizerMenu crystallizerMenu, Container container, int i, int j, int k) {
        super(container, i, j, k);
        this.menu = crystallizerMenu;
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return this.menu.isFuel(itemStack) || isBucket(itemStack);
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        return isBucket(itemStack) ? 1 : super.getMaxStackSize(itemStack);
    }

    public static boolean isBucket(ItemStack itemStack) {
        return itemStack.is(Items.BUCKET);
    }
}
