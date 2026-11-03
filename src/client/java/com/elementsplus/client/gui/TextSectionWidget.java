package com.elementsplus.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class TextSectionWidget extends AbstractWidget {

    private static final String BULLET = "•";
    private static final int PARAGRAPH_SPACING = 5;

    private final List<Component> text;

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
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int y = this.getY();
        for (Component component : this.text) {
            Layout layout = layout(font, component);
            for (int i = 0; i < layout.lines().size(); i++) {
                if (i == 0 && layout.bullet() != null) {
                    guiGraphics.drawString(font, layout.bullet(), this.getX(), y, 0xFFFFFF, false);
                }
                guiGraphics.drawString(font, layout.lines().get(i), this.getX() + layout.indent(), y, 0xFFFFFF, false);
                y += font.lineHeight;
            }
            y += PARAGRAPH_SPACING;
        }
    }

    public int textHeight() {
        Font font = Minecraft.getInstance().font;
        int height = 0;
        for (Component component : text) {
            height += layout(font, component).lines().size() * font.lineHeight;
            height += PARAGRAPH_SPACING;
        }
        return height;
    }

    /**
     * 把一行文本排成若干行。以“•”开头的行按列表项处理：圆点单独绘制，
     * 正文从圆点右侧开始并整体缩进，换行后的内容也不会回到最左侧。
     */
    private Layout layout(Font font, Component component) {
        String plain = component.getString();
        if (!plain.startsWith(BULLET)) {
            return new Layout(split(font, component, 0), null, 0);
        }

        int prefixLength = 1;
        while (prefixLength < plain.length() && Character.isSpaceChar(plain.charAt(prefixLength))) {
            prefixLength++;
        }

        List<Component> flat = component.toFlatList();
        Style bulletStyle = flat.isEmpty() ? Style.EMPTY : flat.get(0).getStyle();
        Component bullet = Component.literal(BULLET).withStyle(bulletStyle);
        int indent = bulletIndent(font);
        Component content = stripPrefix(flat, prefixLength);
        return new Layout(split(font, content, indent), bullet, indent);
    }

    /** 所有列表项共用同一个缩进，避免加粗或彩色圆点导致正文左右不齐。 */
    private static int bulletIndent(Font font) {
        return font.width(BULLET) + font.width(" ");
    }

    private List<FormattedCharSequence> split(Font font, Component component, int indent) {
        if (this.getWidth() <= 0) {
            return List.of(component.getVisualOrderText());
        }
        List<FormattedCharSequence> lines = font.split(component, Math.max(1, this.getWidth() - indent));
        return lines.isEmpty() ? List.of(FormattedCharSequence.EMPTY) : lines;
    }

    private static Component stripPrefix(List<Component> flat, int count) {
        MutableComponent result = Component.empty();
        int remaining = count;
        for (Component part : flat) {
            String value = part.getString();
            if (remaining >= value.length()) {
                remaining -= value.length();
                continue;
            }
            String rest = value.substring(remaining);
            remaining = 0;
            if (!rest.isEmpty()) {
                result.append(Component.literal(rest).withStyle(part.getStyle()));
            }
        }
        return result;
    }

    private record Layout(List<FormattedCharSequence> lines, Component bullet, int indent) {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
