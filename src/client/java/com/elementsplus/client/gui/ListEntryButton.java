package com.elementsplus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.awt.*;

public class ListEntryButton extends GroupButton {

    public ResourceLocation icon;

    public ListEntryButton(int i, int j, int k, int l, Component component, ResourceLocation icon) {
        super(i, j, k, l, component);
        this.icon = icon;
    }

    public ListEntryButton(int i, int j, int k, int l, Component component) {
        this(i, j, k, l, component, null);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.active ? 0xFFE0E0E0 : this.isHovered ? 0xFFA0A0A0 : 0xFF808080);

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (this.icon != null) {
            guiGraphics.blit(this.icon, this.getX() + 5, this.getY() + 1, 0, 0, 16, 16, 16, 16);
        }
        this.renderString(guiGraphics, minecraft.font, 0xFFFFFFFF);
    }

    @Override
    public void renderString(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, int i) {
        this.renderScrollingString(guiGraphics, font, 2, i);
    }

    @Override
    protected void renderScrollingString(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, int i, int j) {
        int k = this.getX() + i;
        int l = this.getX() + this.getWidth() - i;
        renderScrollingString(guiGraphics, font, this.getMessage(), k, this.getY(), l, this.getY() + this.getHeight(), j);
    }

    protected static void renderScrollingString(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, Component component, int i, int j, int k, int l, int m) {
        renderScrollingString(guiGraphics, font, component, (i + k) / 2, i, j, k, l, m);
    }

    protected static void renderScrollingString(GuiGraphics guiGraphics, Font font, Component component, int i, int j, int k, int l, int m, int n) {
        int o = font.width(component);
        int p = (k + m - 9) / 2 + 1;
        int q = l - j;
        if (o > q) {
            int r = o - q;
            double d = Util.getMillis() / 1000.0;
            double e = Math.max(r * 0.5, 3.0);
            double f = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * d / e)) / 2.0 + 0.5;
            double g = Mth.lerp(f, 0.0, r);
            guiGraphics.enableScissor(j, k, l, m);
            guiGraphics.drawString(font, component, j - (int) g, p, n);
            guiGraphics.disableScissor();
        } else {
            guiGraphics.drawString(font, component, j + 25, p, n);
        }
    }

}
