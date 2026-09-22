package com.elementsplus.client.gui;

import com.elementsplus.ElementsPlus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CheckBox extends AbstractWidget {

    public boolean checked;

    public CheckBox(int x, int y) {
        super(x, y, 17, 10, Component.empty());
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float f) {
        ResourceLocation icon;
        if (this.isHovered) {
            if (this.checked) {
                icon = ElementsPlus.id("textures/gui/checkbox/on_highlight.png");
            } else {
                icon = ElementsPlus.id("textures/gui/checkbox/off_highlight.png");
            }
        } else {
            if (this.checked) {
                icon = ElementsPlus.id("textures/gui/checkbox/on.png");
            } else {
                icon = ElementsPlus.id("textures/gui/checkbox/off.png");
            }
        }
        guiGraphics.blit(icon, this.getX(), this.getY(), 0, 0, 17, 10, 17, 10);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    @Override
    public void onClick(double d, double e) {
        super.onClick(d, e);
        this.checked = !this.checked;
    }
}
