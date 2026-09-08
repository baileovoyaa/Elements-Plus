package com.elementsplus.slot;

import com.elementsplus.menu.MetalCatalystMenu;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CatalystSlot extends Slot {
    private final MetalCatalystMenu menu;

    public CatalystSlot(MetalCatalystMenu menu, Container container, int index, int x, int y) {
        super(container, index, x, y);
        this.menu = menu;
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return this.menu.isCatalyst(itemStack);
    }
}
