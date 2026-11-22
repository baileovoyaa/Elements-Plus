package com.elementsplus.client.gui;

import com.elementsplus.client.config.ClientConfig;
import com.elementsplus.client.config.NumberFormat;
import com.elementsplus.core.experiment.ArchitectureInstruction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 光刻机设计页的 8 位基础单片机状态窗口。
 * 左侧显示寄存器与输入输出引脚，右侧显示 256 字节内存，并高亮 R1 指向的地址。
 * 内存区域复用 RAM 查看对话框的只读网格显示与选择/复制交互。
 */
public class McuStatusDialog {
    /** 客户端渲染用的只读快照。 */
    public record Snapshot(int[] memory, int pc, int tr, int r0, int r1,
                           int vi, int di, int ro, int ins,
                           int ri, int vo, int doValue) {
        public Snapshot {
            memory = memory == null ? new int[256] : memory.clone();
        }

        @Override
        public int[] memory() {
            return memory.clone();
        }
    }

    private static final int MARGIN = 6;
    private static final int HEADER = 26;
    private static final int LEFT_WIDTH = 90;
    private static final int ADDRESS_WIDTH = 30;
    private static final int COLUMNS = 16;
    private static final int ROWS = 16;
    private static final int CELL_WIDTH = 17;
    private static final int CELL_HEIGHT = 11;
    private static final int CLOSE_WIDTH = 48;
    private static final int CLOSE_HEIGHT = 18;
    private static final int COLOR_TEXT = 0xFFE0E0E0;
    private static final int COLOR_LABEL = 0xFFFFE066;
    private static final int COLOR_MUTED = 0xFF909090;
    private static final int COLOR_CELL = 0xFF202020;
    private static final int COLOR_SELECTED = 0xFF3A6EC5;
    private static final int COLOR_HIGHLIGHT = 0xFFFFD700;

    private final Supplier<Snapshot> snapshotSupplier;
    private final Font font;
    private final Runnable onClose;
    private final int winX;
    private final int winY;
    private final int winW;
    private final int winH;
    private final int gridX;
    private final int gridY;
    private final int closeX;
    private final int closeY;

    private final Set<Integer> selected = new LinkedHashSet<>();
    private int focus = -1;
    private int anchor = -1;
    private int dragStart = -1;
    private boolean dragSelecting;
    private boolean dragMoved;
    private int dragButton;

    public McuStatusDialog(Supplier<Snapshot> snapshotSupplier, Font font,
                           int screenWidth, int screenHeight, Runnable onClose) {
        this.snapshotSupplier = snapshotSupplier;
        this.font = font;
        this.onClose = onClose;
        this.winW = Math.max(360, ADDRESS_WIDTH + LEFT_WIDTH + MARGIN * 2
                + COLUMNS * CELL_WIDTH + 20);
        this.winH = 240;
        this.winX = Math.max(2, (screenWidth - winW) / 2);
        this.winY = Math.max(2, (screenHeight - winH) / 2);
        this.gridX = winX + MARGIN + LEFT_WIDTH + ADDRESS_WIDTH;
        this.gridY = winY + HEADER + 10;
        this.closeX = winX + winW - CLOSE_WIDTH - MARGIN;
        this.closeY = winY + winH - CLOSE_HEIGHT - MARGIN;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        GuiUtil.raisePose(graphics);
        graphics.fill(0, 0, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(), 0x80000000);
        GuiUtil.drawMainPanel(graphics, winX, winY, winX + winW, winY + winH);
        graphics.drawCenteredString(font, Component.translatable(
                "gui.elements-plus.mcu_status.title"), winX + winW / 2, winY + 7, 0xFFFFFFFF);

        Snapshot snapshot = snapshotSupplier == null ? null : snapshotSupplier.get();
        if (snapshot == null) {
            graphics.drawCenteredString(font, Component.translatable(
                            "gui.elements-plus.mcu_status.unavailable"),
                    winX + winW / 2, winY + winH / 2, COLOR_MUTED);
            renderClose(graphics, mouseX, mouseY);
            return;
        }

        renderRegisters(graphics, snapshot);
        renderMemory(graphics, snapshot);
        renderAssembly(graphics, snapshot);
        renderClose(graphics, mouseX, mouseY);
    }

    private void renderRegisters(GuiGraphics graphics, Snapshot snapshot) {
        int x = winX + MARGIN + 10;
        int y = winY + HEADER;
        int line = 11;
        drawLine(graphics, x, y, Component.translatable("gui.elements-plus.mcu_status.registers"));
        y += line + 1;
        y = drawValueLine(graphics, x, y, "PC", snapshot.pc(), line);
        y = drawValueLine(graphics, x, y, "TR", snapshot.tr(), line);
        y = drawValueLine(graphics, x, y, "R0", snapshot.r0(), line);
        y = drawValueLine(graphics, x, y, "R1", snapshot.r1(), line);

        y += 5;
        drawLine(graphics, x, y, Component.translatable("gui.elements-plus.mcu_status.inputs"));
        y += line + 1;
        y = drawBoolLine(graphics, x, y, "VI", snapshot.vi(), line);
        y = drawValueLine(graphics, x, y, "DI", snapshot.di(), line);
        y = drawBoolLine(graphics, x, y, "RO", snapshot.ro(), line);
        y = drawValueLine(graphics, x, y, "INS", snapshot.ins(), line);

        y += 5;
        drawLine(graphics, x, y, Component.translatable("gui.elements-plus.mcu_status.outputs"));
        y += line + 1;
        drawBoolLine(graphics, x, y, "RI", snapshot.ri(), line);
        drawBoolLine(graphics, x, y + line, "VO", snapshot.vo(), line);
        drawValueLine(graphics, x, y + line * 2, "DO", snapshot.doValue(), line);
    }

    private int drawValueLine(GuiGraphics graphics, int x, int y, String label, int value, int line) {
        graphics.fill(x - 3, y - 1, x + 60, y + 8, 0xFF808080);
        graphics.drawString(font, label, x, y, COLOR_LABEL, false);
        graphics.drawString(font, String.format("0x%02X", value & 0xFF),
                x + 34, y, COLOR_TEXT, false);
        return y + line + 1;
    }

    private int drawBoolLine(GuiGraphics graphics, int x, int y, String label, int value, int line) {
        graphics.fill(x - 3, y - 1, x + 60, y + 8, 0xFF808080);
        graphics.drawString(font, label, x, y, COLOR_LABEL, false);
        graphics.drawString(font, value == 0 ? "0" : "1", x + 34, y, COLOR_TEXT, false);
        return y + line + 1;
    }

    private void drawLine(GuiGraphics graphics, int x, int y, Component text) {
        graphics.drawString(font, text, x, y, 0xFF3F3F3F, false);
    }

    private void renderMemory(GuiGraphics graphics, Snapshot snapshot) {
        int[] memory = snapshot.memory();
        NumberFormat format = ClientConfig.get().digitalNumberFormat;
        graphics.drawString(font, Component.translatable("gui.elements-plus.mcu_status.memory"), gridX - 16, winY + HEADER, 0xFF3F3F3F, false);
        for (int row = 0; row < ROWS; row++) {
            int rowY = gridY + row * CELL_HEIGHT;
            int rowAddress = row * COLUMNS;
            if (rowY + CELL_HEIGHT > closeY) {
                break;
            }
            String address = format.formatDigital(rowAddress);
            graphics.drawString(font, address,
                    gridX - 4 - font.width(address), rowY + 2, COLOR_MUTED, false);
            for (int col = 0; col < COLUMNS; col++) {
                int index = rowAddress + col;
                if (index >= memory.length) {
                    break;
                }
                int cellX = gridX + col * CELL_WIDTH;
                boolean highlighted = index == snapshot.r1();
                boolean isSelected = selected.contains(index);
                if (highlighted) {
                    graphics.fill(cellX, rowY, cellX + CELL_WIDTH - 1,
                            rowY + CELL_HEIGHT - 1, COLOR_HIGHLIGHT);
                } else if (isSelected) {
                    graphics.fill(cellX, rowY, cellX + CELL_WIDTH - 1,
                            rowY + CELL_HEIGHT - 1, COLOR_SELECTED);
                } else {
                    graphics.fill(cellX, rowY, cellX + CELL_WIDTH - 1,
                            rowY + CELL_HEIGHT - 1, COLOR_CELL);
                }
                String value = format.formatDigital(memory[index] & 0xFF);
                int color = highlighted ? 0xFF000000 : 0xFFFFFFFF;
                graphics.drawString(font, value,
                        cellX + (CELL_WIDTH - 1 - font.width(value)) / 2,
                        rowY + 2, color, false);
            }
        }
    }

    private void renderAssembly(GuiGraphics graphics, Snapshot snapshot) {
        String assembly = ArchitectureInstruction.assembly(snapshot.ins());
        String text = Component.translatable("gui.elements-plus.mcu_status.assembly").getString() + ": " + assembly;
        int available = closeX - (winX + MARGIN) - 4;
        if (font.width(text) > available) {
            text = font.plainSubstrByWidth(text, Math.max(1, available - font.width("..."))) + "...";
        }
        graphics.drawString(font, text, winX + MARGIN + 4, winY + winH - 22, 0xFF3F3F3F, false);
    }

    private void renderClose(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean hovered = mouseX >= closeX && mouseX < closeX + CLOSE_WIDTH
                && mouseY >= closeY && mouseY < closeY + CLOSE_HEIGHT;
        graphics.fill(closeX, closeY, closeX + CLOSE_WIDTH, closeY + CLOSE_HEIGHT,
                hovered ? 0xFFFFFFFF : 0xFF000000);
        GuiUtil.drawSubPanel(graphics, closeX + 1, closeY + 1,
                closeX + CLOSE_WIDTH - 1, closeY + CLOSE_HEIGHT - 1,
                0xFF808080, GuiUtil.SubPanelType.CONVEX);
        graphics.drawCenteredString(font, Component.translatable("gui.elements-plus.memory.close"),
                closeX + CLOSE_WIDTH / 2, closeY + (CLOSE_HEIGHT - 9) / 2, 0xFFFFFFFF);
    }

    private int indexAt(double mouseX, double mouseY) {
        if (mouseX < gridX || mouseX >= gridX + COLUMNS * CELL_WIDTH
                || mouseY < gridY || mouseY >= gridY + ROWS * CELL_HEIGHT) {
            return -1;
        }
        int col = (int) ((mouseX - gridX) / CELL_WIDTH);
        int row = (int) ((mouseY - gridY) / CELL_HEIGHT);
        int index = row * COLUMNS + col;
        return index >= 0 && index < COLUMNS * ROWS ? index : -1;
    }

    private void selectLinear(int from, int to) {
        selected.clear();
        int start = Math.min(from, to);
        int end = Math.max(from, to);
        for (int index = start; index <= end && index < COLUMNS * ROWS; index++) {
            selected.add(index);
        }
        anchor = from;
    }

    private void copySelection() {
        List<Integer> indices = new ArrayList<>(selected);
        if (indices.isEmpty()) {
            return;
        }
        Collections.sort(indices);
        NumberFormat format = ClientConfig.get().digitalNumberFormat;
        StringBuilder text = new StringBuilder();
        for (int index : indices) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(format.formatDigital(reportedValue(index)));
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(text.toString());
    }

    private int reportedValue(int index) {
        Snapshot snapshot = snapshotSupplier == null ? null : snapshotSupplier.get();
        if (snapshot == null) {
            return 0;
        }
        int[] memory = snapshot.memory();
        return index >= 0 && index < memory.length ? memory[index] & 0xFF : 0;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= closeX && mouseX < closeX + CLOSE_WIDTH
                && mouseY >= closeY && mouseY < closeY + CLOSE_HEIGHT) {
            onClose.run();
            return true;
        }
        if (button != 0 && button != 2) {
            return true;
        }
        int index = indexAt(mouseX, mouseY);
        if (index >= 0) {
            selected.clear();
            selected.add(index);
            focus = index;
            anchor = index;
            dragStart = index;
            dragButton = button;
            dragSelecting = true;
            dragMoved = false;
            return true;
        }
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragSelecting = false;
        dragMoved = false;
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (dragSelecting) {
            int index = indexAt(mouseX, mouseY);
            if (index >= 0 && index != dragStart) {
                dragMoved = true;
                selectLinear(dragStart, index);
                focus = index;
            }
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (net.minecraft.client.gui.screens.Screen.hasControlDown()
                && keyCode == GLFW.GLFW_KEY_A) {
            selected.clear();
            for (int i = 0; i < COLUMNS * ROWS; i++) {
                selected.add(i);
            }
            focus = 0;
            anchor = 0;
            return true;
        }
        if (net.minecraft.client.gui.screens.Screen.hasControlDown()
                && keyCode == GLFW.GLFW_KEY_C) {
            copySelection();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
            onClose.run();
        }
        return true;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return true;
    }
}
