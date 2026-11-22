package com.elementsplus.client.gui;

import com.elementsplus.core.experiment.ProgrammingFailureInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * 编程实验 WA 数据对话框：左侧输入，右侧实际输出。
 * 输入中已读取的字节为白色，未读取为灰色；输出第一个错误字节为红色。
 */
public class ProgrammingTestDataDialog {
    private static final int PADDING = 8;
    private static final int PADDING_BOTTOM = 28;
    private static final int HEADER = 18;
    private static final int ROW_HEIGHT = 11;
    private static final int COLUMNS = 8;
    private static final int CLOSE_W = 48;
    private static final int CLOSE_H = 18;
    private static final int COLOR_READ = 0xFFFFFFFF;
    private static final int COLOR_UNREAD = 0xFF808080;
    private static final int COLOR_WRONG = 0xFFFF5555;
    private static final int COLOR_TEXT = 0xFFE0E0E0;

    private final ProgrammingFailureInfo info;
    private final Font font;
    private final Runnable onClose;
    private final int winX, winY, winW, winH;
    private final int leftX, rightX, panelWidth;
    private final int listTop, listBottom;
    private final int closeX, closeY;
    private int inputScroll;
    private int outputScroll;

    public ProgrammingTestDataDialog(ProgrammingFailureInfo info, Font font,
                                     int screenWidth, int screenHeight, Runnable onClose) {
        this.info = info;
        this.font = font;
        this.onClose = onClose;
        this.winW = Math.min(460, screenWidth - 20);
        this.winH = Math.min(300, screenHeight - 20);
        this.winX = (screenWidth - winW) / 2;
        this.winY = (screenHeight - winH) / 2;
        this.panelWidth = (winW - 3 * PADDING) / 2;
        this.leftX = winX + PADDING;
        this.rightX = leftX + panelWidth + PADDING;
        this.listTop = winY + HEADER + 22;
        this.listBottom = winY + winH - PADDING_BOTTOM - 8;
        this.closeX = winX + winW - CLOSE_W - 8;
        this.closeY = winY + winH - CLOSE_H - 6;
    }

    public int getWindowX() {
        return winX;
    }

    public int getWindowWidth() {
        return winW;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        GuiUtil.raisePose(graphics);
        graphics.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), 0x80000000);
        GuiUtil.drawMainPanel(graphics, winX, winY, winX + winW, winY + winH);

        graphics.drawCenteredString(font, Component.translatable(
                        "gui.elements-plus.experiment_table.programming_data"), winX + winW / 2, winY + 6, 0xFFFFFFFF);
        drawPanel(graphics, leftX, Component.translatable(
                        "gui.elements-plus.experiment_table.programming_input"),
                info.input(), info.inputRead(), -1, inputScroll, true, mouseX, mouseY);
        drawPanel(graphics, rightX, Component.translatable(
                        "gui.elements-plus.experiment_table.programming_output"),
                info.actualOutput(), Integer.MAX_VALUE, info.wrongIndex(), outputScroll,
                false, mouseX, mouseY);
        boolean hoverClose = mouseX >= closeX && mouseX < closeX + CLOSE_W
                && mouseY >= closeY && mouseY < closeY + CLOSE_H;
        GuiUtil.renderButton(graphics, closeX, closeY, CLOSE_W, CLOSE_H,
                hoverClose, 0xFF808080, Component.translatable("gui.elements-plus.experiment_table.sequential_close"));
        graphics.drawString(font, Component.translatable(
                        "gui.elements-plus.experiment_table.programming_wrong_byte",
                        info.wrongIndex() + 1),
                winX + PADDING, winY + winH - 16, COLOR_WRONG, false);
    }

    private void drawPanel(GuiGraphics graphics, int x, Component title, byte[] data,
                           int read, int wrong, int scroll, boolean input,
                           int mouseX, int mouseY) {
        graphics.fill(x, winY + HEADER + ROW_HEIGHT + 4, x + panelWidth, winY + winH - PADDING_BOTTOM, 0xFF303030);
        graphics.renderOutline(x, winY + HEADER + ROW_HEIGHT + 4, panelWidth, winH - HEADER - PADDING_BOTTOM - ROW_HEIGHT - 4, 0xFF707070);
        graphics.drawString(font, title, x + 4, winY + HEADER + 4, 0xFF3F3F3F, false);
        int rows = Math.max(1, (data.length + COLUMNS - 1) / COLUMNS);
        int visible = Math.max(1, (listBottom - listTop) / ROW_HEIGHT);
        int maxScroll = Math.max(0, rows - visible);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        for (int row = 0; row < visible; row++) {
            int sourceRow = row + scroll;
            int y = listTop + row * ROW_HEIGHT;
            for (int col = 0; col < COLUMNS; col++) {
                int index = sourceRow * COLUMNS + col;
                if (index >= data.length) break;
                int color;
                if (wrong >= 0 && index == wrong) {
                    color = COLOR_WRONG;
                } else if (input && index >= read) {
                    color = COLOR_UNREAD;
                } else {
                    color = COLOR_READ;
                }
                String text = String.format("%02X", data[index] & 0xFF);
                graphics.drawString(font, text, x + 6 + col * 20, y, color, false);
            }
        }
        int trackX = x + panelWidth - 6;
        int trackTop = listTop;
        int trackBottom = listBottom;
        graphics.fill(trackX, trackTop, trackX + 3, trackBottom, 0xFF202020);
        if (maxScroll > 0) {
            int handleHeight = Math.max(8, (trackBottom - trackTop) * visible / rows);
            int handleY = trackTop + (trackBottom - trackTop - handleHeight) * scroll / maxScroll;
            graphics.fill(trackX, handleY, trackX + 3, handleY + handleHeight, 0xFFA0A0A0);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= closeX && mouseX < closeX + CLOSE_W
                && mouseY >= closeY && mouseY < closeY + CLOSE_H) {
            onClose.run();
            return true;
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int delta = (int) Math.signum(vertical);
        if (delta == 0) return true;
        if (mouseX >= leftX && mouseX < leftX + panelWidth
                && mouseY >= winY + HEADER && mouseY < winY + winH) {
            inputScroll = Math.max(0, inputScroll - delta);
        } else if (mouseX >= rightX && mouseX < rightX + panelWidth
                && mouseY >= winY + HEADER && mouseY < winY + winH) {
            outputScroll = Math.max(0, outputScroll - delta);
        }
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
            onClose.run();
            return true;
        }
        return true;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return true;
    }
}
