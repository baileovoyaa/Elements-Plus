package com.elementsplus.client.gui;

import net.minecraft.world.inventory.Slot;

public interface SlotPositionProvider {
    Point getSlotPosition(Slot slot);
}