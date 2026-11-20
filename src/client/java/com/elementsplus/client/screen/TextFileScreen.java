package com.elementsplus.client.screen;

import com.elementsplus.client.gui.GuiUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** 文本文件只读阅读界面：一页可滚动、自动换行的文本，无物品槽位。 */
public class TextFileScreen extends Screen {
    private static final int LINE_GAP = 2;
    private static final int PADDING = 10;

    private final String fileName;
    private final String text;
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int contentLeft;
    private int contentTop;
    private int contentWidth;
    private int contentHeight;
    private int lineHeight;
    private double scroll;
    private boolean draggingScrollbar;
    private double scrollbarGrab;

    public TextFileScreen(String fileName, String text) {
        super(Component.literal(fileName == null ? "" : fileName));
        this.fileName = fileName == null ? "" : fileName;
        this.text = text == null ? "" : text;
    }

    @Override
    protected void init() {
        super.init();
        this.panelWidth = Math.max(180, Math.min(360, this.width - 40));
        this.panelHeight = Math.max(120, Math.min(260, this.height - 40));
        this.left = (this.width - this.panelWidth) / 2;
        this.top = (this.height - this.panelHeight) / 2;
        this.contentLeft = this.left + PADDING;
        this.contentTop = this.top + 28;
        this.contentWidth = this.panelWidth - PADDING * 2;
        this.contentHeight = this.panelHeight - 38;
        this.lineHeight = this.font.lineHeight + LINE_GAP;
        buildLines();
        clampScroll();
    }

    private void buildLines() {
        lines.clear();
        String normalized = this.text.replace("\r\n", "\n").replace('\r', '\n');
        for (String raw : normalized.split("\n", -1)) {
            List<FormattedCharSequence> wrapped = this.font.split(
                    Component.literal(raw), Math.max(1, this.contentWidth));
            if (wrapped.isEmpty()) {
                lines.add(Component.empty().getVisualOrderText());
            } else {
                lines.addAll(wrapped);
            }
        }
    }

    private int maxScroll() {
        return Math.max(0, lines.size() * lineHeight - contentHeight);
    }

    private int scrollbarTrackX() {
        return contentLeft + contentWidth - 3;
    }

    private int scrollbarThumbHeight() {
        if (maxScroll() <= 0) {
            return 0;
        }
        return Math.max(12, contentHeight * contentHeight / (lines.size() * lineHeight));
    }

    private int scrollbarThumbY() {
        int thumbHeight = scrollbarThumbHeight();
        int range = Math.max(0, contentHeight - thumbHeight);
        return contentTop + (int) Math.round(range * scroll / maxScroll());
    }

    private void scrollFromThumb(double thumbTop) {
        int thumbHeight = scrollbarThumbHeight();
        int range = contentHeight - thumbHeight;
        if (range <= 0) {
            scroll = 0;
            return;
        }
        double relative = Math.max(0, Math.min(range, thumbTop - contentTop));
        scroll = relative / range * maxScroll();
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);
        GuiUtil.drawMainPanel(graphics, left, top, left + panelWidth, top + panelHeight);

        Font font = this.font;
        int titleWidth = this.panelWidth - PADDING * 2;
        String title = font.width(fileName) > titleWidth
                ? font.plainSubstrByWidth(fileName, Math.max(1, titleWidth - font.width("..."))) + "..."
                : fileName;
        graphics.drawString(font, title, left + PADDING, top + 8, 0xFF202020, false);

        graphics.fill(contentLeft, contentTop, contentLeft + contentWidth,
                contentTop + contentHeight, 0xFFFFFFFF);
        graphics.enableScissor(contentLeft, contentTop,
                contentLeft + contentWidth, contentTop + contentHeight);
        int first = (int) Math.max(0, scroll / lineHeight);
        int last = Math.min(lines.size(), first + contentHeight / lineHeight + 2);
        for (int i = first; i < last; i++) {
            int y = (int) (contentTop + i * lineHeight - scroll);
            graphics.drawString(font, lines.get(i), contentLeft + 2, y + 1, 0xFF202020, false);
        }
        graphics.disableScissor();

        if (maxScroll() > 0) {
            int trackX = scrollbarTrackX();
            graphics.fill(trackX, contentTop, trackX + 3, contentTop + contentHeight, 0x30000000);
            int thumbHeight = scrollbarThumbHeight();
            int thumbY = scrollbarThumbY();
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight,
                    draggingScrollbar ? 0xFF505050 : 0xFF808080);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && maxScroll() > 0) {
            int trackX = scrollbarTrackX();
            if (mouseX >= trackX - 2 && mouseX <= trackX + 4
                    && mouseY >= contentTop && mouseY <= contentTop + contentHeight) {
                int thumbY = scrollbarThumbY();
                int thumbHeight = scrollbarThumbHeight();
                this.draggingScrollbar = true;
                if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                    this.scrollbarGrab = mouseY - thumbY;
                } else {
                    this.scrollbarGrab = thumbHeight / 2.0;
                    scrollFromThumb(mouseY - scrollbarGrab);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (draggingScrollbar) {
            scrollFromThumb(mouseY - scrollbarGrab);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                double verticalAmount) {
        scroll = Math.clamp(scroll - verticalAmount * lineHeight, 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            scroll = Math.min(maxScroll(), scroll + contentHeight - lineHeight);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            scroll = Math.max(0, scroll - contentHeight + lineHeight);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            scroll = 0;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            scroll = maxScroll();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
