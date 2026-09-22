package com.elementsplus.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class TextSectionWidget extends AbstractWidget {

    List<Component> text;

    public TextSectionWidget(int x, int y, int w, List<Component> text) {
        super(x, y, w, 0, Component.empty());
        this.text = text;
        this.setHeight(textHeight());
    }

    public TextSectionWidget(int x, int y, List<Component> text) {
        this(x, y, 0, text);
    }

    public TextSectionWidget(int x, int y, Component text) {
        this(x, y, 0, List.of(text));
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        int y = this.getY();
        Font font = Minecraft.getInstance().font;
        for (Component component : text) {
            if (this.getWidth() == 0) {
                guiGraphics.drawString(font, component, this.getX(), y, 0xFFFFFF, false);
                y += font.lineHeight;
            } else {
                for (FormattedCharSequence line : font.split(component, this.getWidth())) {
                    guiGraphics.drawString(font, line, this.getX(), y, 0xFFFFFF, false);
                    y += font.lineHeight;
                }
            }
            y += 5;
        }
    }

    public int textHeight() {
        int height = 0;
        Font font = Minecraft.getInstance().font;
        for (Component component : text) {
            for (FormattedCharSequence line : font.split(component, this.getWidth())) {
                height += font.lineHeight;
            }
            height += 5;
        }
        return height;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
