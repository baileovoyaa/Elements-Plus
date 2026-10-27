package com.elementsplus.client.gui;

import com.elementsplus.client.config.NumberFormat;
import com.elementsplus.core.experiment.ExperimentFailureInfo;
import com.elementsplus.core.experiment.SequentialFailureInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 时序测例失败数据的模态窗口：可滚动的逐 tick 表格。
 */
public class SequentialTestDataDialog {

    private enum Kind {TICK, INPUT, EXPECTED, ACTUAL}

    private record Column(Kind kind, String label, String header, int x, int width) {
    }

    private static final int ROW_HEIGHT = 12;
    private static final int HEADER_HEIGHT = 14;
    private static final int PADDING = 8;
    private static final int CLOSE_W = 48;
    private static final int CLOSE_H = 18;
    private static final int COLOR_MUTED = 0xFFAAAAAA;
    private static final int COLOR_ACTUAL_OK = 0xFF55FF55;
    private static final int COLOR_ACTUAL_BAD = 0xFFFF5555;

    private final SequentialFailureInfo info;
    private final Font font;
    private final Runnable onClose;
    private final int winX, winY, winW, winH;
    private final int rowsTop;
    private final int rowsBottom;
    private final int visibleRows;
    private final int maxScroll;
    private final int closeX, closeY;
    private final List<Column> columns = new ArrayList<>();
    private int scrollRow;

    public SequentialTestDataDialog(SequentialFailureInfo info, Font font, int screenWidth, int screenHeight, Runnable onClose) {
        this.info = info;
        this.font = font;
        this.onClose = onClose;

        List<SequentialFailureInfo.Row> rows = info.rows();
        SequentialFailureInfo.Row first = rows.isEmpty() ? null : rows.get(0);
        buildColumns(first, rows);

        int tableWidth = 0;
        for (Column column : columns) {
            tableWidth += column.width();
        }
        this.winW = Math.min(Math.max(260, tableWidth + 2 * PADDING), screenWidth - 20);
        this.winH = Math.min(280, screenHeight - 20);
        this.winX = (screenWidth - winW) / 2;
        this.winY = (screenHeight - winH) / 2;

        this.closeX = winX + winW - CLOSE_W - 6;
        this.closeY = winY + winH - CLOSE_H - 6;
        this.rowsTop = winY + 30 + HEADER_HEIGHT;
        this.rowsBottom = closeY - 6;
        this.visibleRows = Math.max(1, (rowsBottom - rowsTop) / ROW_HEIGHT);
        this.maxScroll = Math.max(0, rows.size() - visibleRows);
    }

    private void buildColumns(SequentialFailureInfo.Row first, List<SequentialFailureInfo.Row> rows) {
        if (first == null) {
            return;
        }
        int x = 0;
        Column tickColumn = makeColumn(Kind.TICK, "", Component.translatable("gui.elements-plus.experiment_table.sequential_tick").getString(),
                x, rows, "");
        columns.add(tickColumn);
        x += tickColumn.width();

        for (ExperimentFailureInfo.Pin pin : first.inputs()) {
            String label = pin.label();
            String header = Component.translatable("gui.elements-plus.experiment_table.sequential_input", label).getString();
            Column column = makeColumn(Kind.INPUT, label, header, x, rows, label);
            columns.add(column);
            x += column.width();
        }
        for (ExperimentFailureInfo.Pin pin : first.expected()) {
            String label = pin.label();
            String header = Component.translatable("gui.elements-plus.experiment_table.sequential_expected", label).getString();
            Column column = makeColumn(Kind.EXPECTED, label, header, x, rows, label);
            columns.add(column);
            x += column.width();
        }
        for (ExperimentFailureInfo.Pin pin : first.actual()) {
            String label = pin.label();
            String header = Component.translatable("gui.elements-plus.experiment_table.sequential_actual", label).getString();
            Column column = makeColumn(Kind.ACTUAL, label, header, x, rows, label);
            columns.add(column);
            x += column.width();
        }
    }

    private Column makeColumn(Kind kind, String label, String header, int x, List<SequentialFailureInfo.Row> rows, String pinLabel) {
        int width = font.width(header);
        for (SequentialFailureInfo.Row row : rows) {
            String value = valueFor(row, kind, pinLabel);
            if (value != null) {
                width = Math.max(width, font.width(value));
            }
        }
        return new Column(kind, label, header, x, width + 2 * PADDING);
    }

    private static String valueFor(SequentialFailureInfo.Row row, Kind kind, String label) {
        List<ExperimentFailureInfo.Pin> pins = switch (kind) {
            case TICK -> null;
            case INPUT -> row.inputs();
            case EXPECTED -> row.expected();
            case ACTUAL -> row.actual();
        };
        if (kind == Kind.TICK) {
            return Integer.toString(row.tick());
        }
        ExperimentFailureInfo.Pin pin = findPin(pins, label);
        return pin == null ? "" : formatPin(pin);
    }

    private static ExperimentFailureInfo.Pin findPin(List<ExperimentFailureInfo.Pin> pins, String label) {
        for (ExperimentFailureInfo.Pin pin : pins) {
            if (pin.label().equals(label)) {
                return pin;
            }
        }
        return null;
    }

    private static String formatPin(ExperimentFailureInfo.Pin pin) {
        byte[] values = pin.values();
        int[] lanes = new int[values == null ? 0 : values.length];
        for (int i = 0; i < lanes.length; i++) {
            lanes[i] = values[i] & 0xF;
        }
        return NumberFormat.formatLanes(lanes);
    }

    private static boolean sameValue(ExperimentFailureInfo.Pin a, ExperimentFailureInfo.Pin b) {
        return a != null && b != null && a.bitWidth() == b.bitWidth()
                && Arrays.equals(a.values(), b.values());
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        GuiUtil.raisePose(g);
        g.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), 0x80000000);
        GuiUtil.drawMainPanel(g, winX, winY, winX + winW, winY + winH);
        g.drawCenteredString(font, Component.translatable("gui.elements-plus.experiment_table.sequential_data_title"),
                winX + winW / 2, winY + 8, 0xFFFFFFFF);

        int tableX = winX + PADDING;
        int headerY = winY + 30;
        g.fill(tableX, headerY, tableX + winW - 2 * PADDING, headerY + HEADER_HEIGHT, 0x50000000);
        for (Column column : columns) {
            g.drawString(font, column.header(), tableX + column.x() + 2, headerY + 3, 0xFFFFE066, false);
        }

        g.enableScissor(tableX, rowsTop, tableX + winW - 2 * PADDING, rowsBottom);
        for (int i = 0; i < visibleRows; i++) {
            int rowIndex = scrollRow + i;
            if (rowIndex >= info.rows().size()) {
                break;
            }
            SequentialFailureInfo.Row row = info.rows().get(rowIndex);
            int y = rowsTop + i * ROW_HEIGHT;
            for (Column column : columns) {
                String text = valueFor(row, column.kind(), column.label());
                int color = colorFor(column, row);
                g.drawString(font, text, tableX + column.x() + 2, y + 2, color, false);
            }
        }
        g.disableScissor();

        if (maxScroll > 0) {
            int barX = winX + winW - 6;
            int barTop = rowsTop;
            int barBottom = rowsBottom;
            g.fill(barX, barTop, barX + 3, barBottom, 0xFF606060);
            int thumbHeight = Math.max(8, (barBottom - barTop) * visibleRows / Math.max(1, info.rows().size()));
            int thumbY = barTop + (barBottom - barTop - thumbHeight) * scrollRow / maxScroll;
            g.fill(barX, thumbY, barX + 3, thumbY + thumbHeight, 0xFFC0C0C0);
        }

        boolean hoveredClose = mouseX >= closeX && mouseX < closeX + CLOSE_W && mouseY >= closeY && mouseY < closeY + CLOSE_H;
        g.fill(closeX, closeY, closeX + CLOSE_W, closeY + CLOSE_H, hoveredClose ? 0xFFFFFFFF : 0xFF000000);
        GuiUtil.drawSubPanel(g, closeX + 1, closeY + 1, closeX + CLOSE_W - 1, closeY + CLOSE_H - 1,
                0xFF808080, GuiUtil.SubPanelType.CONVEX);
        g.drawCenteredString(font, Component.translatable("gui.elements-plus.experiment_table.sequential_close"),
                closeX + CLOSE_W / 2, closeY + (CLOSE_H - 9) / 2, 0xFFFFFFFF);
    }

    private static int colorFor(Column column, SequentialFailureInfo.Row row) {
        if (column.kind() == Kind.TICK) {
            return 0xFFFFFFFF;
        }
        if (column.kind() != Kind.ACTUAL) {
            return COLOR_MUTED;
        }
        ExperimentFailureInfo.Pin expected = findPin(row.expected(), column.label());
        ExperimentFailureInfo.Pin actual = findPin(row.actual(), column.label());
        return sameValue(expected, actual) ? COLOR_ACTUAL_OK : COLOR_ACTUAL_BAD;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= closeX && mouseX < closeX + CLOSE_W
                && mouseY >= closeY && mouseY < closeY + CLOSE_H) {
            onClose.run();
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0) {
            scrollRow = Mth.clamp(scrollRow - (int) Math.signum(verticalAmount), 0, maxScroll);
        }
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose.run();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            scrollRow = Mth.clamp(scrollRow + (keyCode == GLFW.GLFW_KEY_PAGE_DOWN ? visibleRows : 1), 0, maxScroll);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            scrollRow = Mth.clamp(scrollRow - (keyCode == GLFW.GLFW_KEY_PAGE_UP ? visibleRows : 1), 0, maxScroll);
            return true;
        }
        return true;
    }

    public boolean charTyped(char c, int modifiers) {
        return true;
    }
}
