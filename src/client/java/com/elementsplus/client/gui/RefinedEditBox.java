package com.elementsplus.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.MutableComponent;

public class RefinedEditBox extends EditBox {
    public RefinedEditBox(Font font, int x, int y, int width, int height, MutableComponent message) {
        super(font, x, y, width, height, message);
    }

    // AbstractWidget默认响应所有拖动操作，导致滑块无法正常拖动，这里重写mouseDragged方法只在鼠标真正位于EditBox上时才响应拖动操作
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isHovered()) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return false;
    }
}
