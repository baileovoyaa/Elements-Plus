package com.elementsplus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class TabButton extends GroupButton {

    public TabButton(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        GuiUtil.drawTab(guiGraphics, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.active ? 0xFFFFFFFF : 0xFF808080, this.active);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        this.renderString(guiGraphics, minecraft.font, 0xFFFFFFFF);
    }

    @Override
    protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
        int k = this.getX() + i;
        int l = this.getX() + this.getWidth() - i;
        renderScrollingString(guiGraphics, font, this.getMessage(), k, this.active ? this.getY() : this.getY() + 2, l, this.getY() + this.getHeight(), j);
    }
}
