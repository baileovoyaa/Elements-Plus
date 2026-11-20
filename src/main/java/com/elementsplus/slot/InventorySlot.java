package com.elementsplus.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/**
 * 可在界面上整体启用的玩家物品栏槽位。编程标签页会隐藏物品栏，禁用这些槽位
 * 可以避免隐藏槽位的悬停提示与点击。
 */
public class InventorySlot extends Slot {
    private boolean enabled = true;

    public InventorySlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    public void setSlotActive(boolean active) {
        this.enabled = active;
    }

    @Override
    public boolean isActive() {
        return enabled;
    }
}
