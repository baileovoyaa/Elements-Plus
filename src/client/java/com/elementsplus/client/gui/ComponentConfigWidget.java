package com.elementsplus.client.gui;

import com.elementsplus.core.circuit.component.CircuitComponentInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ComponentConfigWidget extends AbstractWidget {
    private static final int KNOB_WIDTH = 8;
    private static final int KNOB_HEIGHT = 10;
    private static final int RAIL_HEIGHT = 4;

    private final CircuitComponentInstance instance;
    private final CircuitComponentInstance.Config config;
    private final Runnable onCommit;
    private final boolean interactive;

    private boolean dragging;
    private double grabOffsetX;

    public ComponentConfigWidget(int x, int y, int width, int height,
                                 CircuitComponentInstance instance,
                                 CircuitComponentInstance.Config config,
                                 boolean interactive,
                                 Runnable onCommit) {
        super(x, y, width, height, Component.empty());
        this.instance = instance;
        this.config = config;
        this.interactive = interactive;
        this.onCommit = onCommit;
    }

    private boolean isIntConfig() {
        return config instanceof CircuitComponentInstance.IntConfig;
    }

    private int intMin() {
        return ((CircuitComponentInstance.IntConfig) config).min;
    }

    private int intMax() {
        return ((CircuitComponentInstance.IntConfig) config).max;
    }

    private int intStep() {
        return ((CircuitComponentInstance.IntConfig) config).step;
    }

    private float floatMin() {
        return ((CircuitComponentInstance.FloatConfig) config).min;
    }

    private float floatMax() {
        return ((CircuitComponentInstance.FloatConfig) config).max;
    }

    private String valueText() {
        if (isIntConfig()) {
            return String.valueOf(instance.getInt(config.key));
        }
        return String.format("%.2f", instance.getFloat(config.key));
    }

    private float t() {
        if (isIntConfig()) {
            int value = instance.getInt(config.key);
            return (value - intMin()) / (float) Math.max(1, intMax() - intMin());
        }
        float value = instance.getFloat(config.key);
        return Mth.clamp((value - floatMin()) / Math.max(floatMax() - floatMin(), 1e-6f), 0f, 1f);
    }

    private int trackStart() {
        return getX() + 2;
    }

    private int trackEnd() {
        return getX() + getWidth() - 2;
    }

    private int trackWidth() {
        return Math.max(1, trackEnd() - trackStart() - KNOB_WIDTH);
    }

    private double knobCenter() {
        return trackStart() + KNOB_WIDTH / 2.0 + t() * trackWidth();
    }

    private void setValueFromMouse(double mouseX) {
        double t = Mth.clamp((mouseX - trackStart() - KNOB_WIDTH / 2.0) / trackWidth(), 0, 1);
        if (isIntConfig()) {
            int raw = (int) Math.round(t * (intMax() - intMin())) + intMin();
            int stepped = intMin() + Math.round((raw - intMin()) / (float) intStep()) * intStep();
            instance.setInt(config.key, Math.clamp(stepped, intMin(), intMax()));
        } else {
            float value = floatMin() + (float) t * (floatMax() - floatMin());
            instance.setFloat(config.key, value);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        String label = config.name.getString();
        g.drawString(font, label, getX() + 2, getY(), 0xFFFFFFFF, false);
        String value = valueText();
        g.drawString(font, value, getX() + getWidth() - 2 - font.width(value), getY(), 0xFFFFE066, false);

        if (!interactive) {
            return;
        }

        int railY = getY() + getHeight() - RAIL_HEIGHT - 2;
        int x1 = trackStart();
        int x2 = trackEnd();
        int fillEnd = x1 + (int) Math.round(t() * trackWidth());
        g.fill(x1, railY, x2, railY + RAIL_HEIGHT, 0xFF8B8B8B);
        g.fill(x1, railY, x2, railY + 1, 0xFF373737);
        g.fill(x1, railY, x1 + 1, railY + RAIL_HEIGHT, 0xFF373737);
        g.fill(x1, railY + RAIL_HEIGHT - 1, x2, railY + RAIL_HEIGHT, 0xFFFFFFFF);
        g.fill(x2 - 1, railY, x2, railY + RAIL_HEIGHT, 0xFFFFFFFF);
        g.fill(x1 + 1, railY + 1, fillEnd, railY + RAIL_HEIGHT - 1, 0xFF3A6EC5);

        int knobX = (int) knobCenter() - KNOB_WIDTH / 2;
        int knobY = railY - (KNOB_HEIGHT - RAIL_HEIGHT) / 2;
        int color = (this.isHovered || this.dragging) ? 0xFFFFFFFF : 0xFFC6C6C6;
        g.fill(knobX, knobY, knobX + KNOB_WIDTH, knobY + KNOB_HEIGHT, color);
        g.fill(knobX, knobY, knobX + KNOB_WIDTH, knobY + 1, 0xFF000000);
        g.fill(knobX, knobY, knobX + 1, knobY + KNOB_HEIGHT, 0xFF000000);
        g.fill(knobX, knobY + KNOB_HEIGHT - 1, knobX + KNOB_WIDTH, knobY + KNOB_HEIGHT, 0xFF555555);
        g.fill(knobX + KNOB_WIDTH - 1, knobY, knobX + KNOB_WIDTH, knobY + KNOB_HEIGHT, 0xFF555555);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || !interactive) {
            return false;
        }
        if (button != 0) {
            return false;
        }
        if (mouseX < getX() || mouseX > getX() + getWidth() || mouseY < getY() || mouseY > getY() + getHeight()) {
            return false;
        }
        this.dragging = true;
        this.grabOffsetX = knobCenter() - mouseX;
        setValueFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.dragging) {
            return false;
        }
        setValueFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.dragging) {
            return false;
        }
        this.dragging = false;
        if (onCommit != null) {
            onCommit.run();
        }
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}