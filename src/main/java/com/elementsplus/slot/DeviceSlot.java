package com.elementsplus.slot;

import com.elementsplus.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 实验桌外接设备槽位：只接受磁盘、电路图、纸、文本文件、打孔文件和文件夹，且每次只放 1 个。
 */
public class DeviceSlot extends Slot {
    public DeviceSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(ModItems.DISK)
                || stack.is(ModItems.CIRCUIT_DIAGRAM)
                || stack.is(Items.PAPER)
                || stack.is(ModItems.TEXT_FILE)
                || stack.is(ModItems.PUNCHED_FILE)
                || stack.is(ModItems.FILE_FOLDER);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}
