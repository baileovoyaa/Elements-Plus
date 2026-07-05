package com.elementsplus.slot;

import com.elementsplus.menu.CrystallizerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PressurizeSlot extends Slot {
    private final CrystallizerMenu menu;

    public PressurizeSlot(CrystallizerMenu crystallizerMenu, Container container, int i, int j, int k) {
        super(container, i, j, k);
        this.menu = crystallizerMenu;
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return this.menu.isPressurizer(itemStack);
    }
}
