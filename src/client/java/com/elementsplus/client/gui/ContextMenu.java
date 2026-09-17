package com.elementsplus.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 简单的右键按钮列表，由 Screen 直接渲染与分发输入。 */
public class ContextMenu extends AbstractWidget {
    public static final int ROW_HEIGHT = 18;

    public record Entry(Component label, Runnable action) {
    }

    private final List<Entry> entries = new ArrayList<>();

    public ContextMenu(int x, int y) {
        super(x, y, 90, 0, Component.empty());
    }

    public ContextMenu add(Component label, Runnable action) {
        entries.add(new Entry(label, action));
        this.height = entries.size() * ROW_HEIGHT + 4;
        return this;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        GuiUtil.raisePose(g);
        GuiUtil.drawSubPanel(g, x, y, x + w, y + h, 0xFFA0A0A0, GuiUtil.SubPanelType.BORDERED);
        for (int i = 0; i < entries.size(); i++) {
            int ry = y + 2 + i * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + ROW_HEIGHT;
            g.fill(x + 1, ry, x + w - 1, ry + ROW_HEIGHT, hovered ? 0xFFA0A0A0 : 0xFF808080);
            g.drawString(font, entries.get(i).label(), x + 5, ry + (ROW_HEIGHT - 9) / 2, 0xFFFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        int x = getX();
        int y = getY();
        if (mouseX < x || mouseX >= x + getWidth() || mouseY < y || mouseY >= y + getHeight()) {
            return false;
        }
        if (button == 0) {
            int row = (int) ((mouseY - y - 2) / ROW_HEIGHT);
            if (row >= 0 && row < entries.size()) {
                entries.get(row).action().run();
                return true;
            }
        }
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}