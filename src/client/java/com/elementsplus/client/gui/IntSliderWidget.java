package com.elementsplus.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class IntSliderWidget extends AbstractWidget {
    private static final int KNOB_WIDTH = 8;
    private static final int KNOB_HEIGHT = 10;
    private static final int RAIL_HEIGHT = 4;

    protected final int min;
    protected final int max;
    protected int value;
    protected boolean isDragging;
    protected double grabOffsetX;

    @Nullable
    protected Consumer<Integer> onChange;

    public IntSliderWidget(int x, int y, int width, int height, int min, int max, int value, Consumer<Integer> onChange) {
        super(x, y, width, height, Component.empty());
        this.min = min;
        this.max = max;
        this.value = Mth.clamp(value, min, max);
        this.onChange = onChange;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int getValue() {
        return value;
    }

    /** 程序化设置值，不触发 onChange */
    public void setValue(int value) {
        this.value = Mth.clamp(value, min, max);
    }

    /** 用户输入引发的值变化，触发 onChange */
    protected void setValueFromUser(int value) {
        int clamped = Mth.clamp(value, min, max);
        if (clamped != this.value) {
            this.value = clamped;
            if (onChange != null) {
                onChange.accept(clamped);
            }
        }
    }

    protected double knobCenterX() {
        double t = (value - min) / (double) Math.max(1, max - min);
        return getX() + (double) KNOB_WIDTH / 2 + t * (getWidth() - KNOB_WIDTH);
    }

    protected void setValueFromMouse(double mouseX) {
        double t = (mouseX - getX() - (double) KNOB_WIDTH / 2) / Math.max(1, getWidth() - KNOB_WIDTH);
        setValueFromUser(min + (int) Math.round(t * (max - min)));
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int railY = getY() + getHeight() / 2 - RAIL_HEIGHT / 2;
        int x1 = getX();
        int x2 = getX() + getWidth();
        int knobCenter = (int) knobCenterX();

        // 轨道（凹陷）
        g.fill(x1, railY, x2, railY + RAIL_HEIGHT, 0xFF8B8B8B);
        g.fill(x1, railY, x2, railY + 1, 0xFF373737);
        g.fill(x1, railY, x1 + 1, railY + RAIL_HEIGHT, 0xFF373737);
        g.fill(x1, railY + RAIL_HEIGHT - 1, x2, railY + RAIL_HEIGHT, 0xFFFFFFFF);
        g.fill(x2 - 1, railY, x2, railY + RAIL_HEIGHT, 0xFFFFFFFF);

        // 已填充部分
        g.fill(x1 + 1, railY + 1, knobCenter, railY + RAIL_HEIGHT - 1, 0xFF3A6EC5);

        // 滑块（凸起）
        int knobX = knobCenter - KNOB_WIDTH / 2;
        int knobY = railY - (KNOB_HEIGHT - RAIL_HEIGHT) / 2;
        int color = (this.isHovered || this.isDragging) ? 0xFFFFFFFF : 0xFFC6C6C6;
        g.fill(knobX, knobY, knobX + KNOB_WIDTH, knobY + KNOB_HEIGHT, color);
        g.fill(knobX, knobY, knobX + KNOB_WIDTH, knobY + 1, 0xFF000000);
        g.fill(knobX, knobY, knobX + 1, knobY + KNOB_HEIGHT, 0xFF000000);
        g.fill(knobX, knobY + KNOB_HEIGHT - 1, knobX + KNOB_WIDTH, knobY + KNOB_HEIGHT, 0xFF555555);
        g.fill(knobX + KNOB_WIDTH - 1, knobY, knobX + KNOB_WIDTH, knobY + KNOB_HEIGHT, 0xFF555555);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;
        if (button != 0) return false;
        if (mouseX < getX() || mouseX > getX() + getWidth() || mouseY < getY() || mouseY > getY() + getHeight()) {
            return false;
        }
        this.isDragging = true;
        this.grabOffsetX = knobCenterX() - mouseX;
        setValueFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.isDragging) return false;
        setValueFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.isDragging) return false;
        this.isDragging = false;
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}