package com.elementsplus.client.gui;

import com.elementsplus.client.config.ClientConfig;
import com.elementsplus.client.config.NumberFormat;
import com.elementsplus.core.experiment.ArchitectureFailureInfo;
import com.elementsplus.core.experiment.ArchitectureInstruction;
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

    private enum Kind {
        TICK, INPUT, EXPECTED, ACTUAL,
        ARCH_PC, ARCH_INS, ARCH_ASM, ARCH_VI, ARCH_RI, ARCH_DI, ARCH_VO, ARCH_RO, ARCH_DO
    }

    private record Column(Kind kind, String label, String header, int x, int width) {
    }

    private static final int ROW_HEIGHT = 12;
    private static final int HEADER_HEIGHT = 14;
    private static final int PADDING = 8;
    private static final int ARCH_PADDING = 4;
    private static final int CLOSE_W = 48;
    private static final int CLOSE_H = 18;
    private static final int COPY_W = 72;
    private static final int COLOR_MUTED = 0xFFAAAAAA;
    private static final int COLOR_ACTUAL_OK = 0xFF55FF55;
    private static final int COLOR_ACTUAL_BAD = 0xFFFF5555;

    private final SequentialFailureInfo info;
    private final ArchitectureFailureInfo architecture;
    private final Font font;
    private final Runnable onClose;
    private final int winX, winY, winW, winH;
    private final int rowsTop;
    private final int rowsBottom;
    private final int visibleRows;
    private final int maxScroll;
    private final int closeX, closeY;
    private final int copyX, copyY;
    private final List<Column> columns = new ArrayList<>();
    private int scrollRow;

    public SequentialTestDataDialog(SequentialFailureInfo info, Font font, int screenWidth, int screenHeight, Runnable onClose) {
        this.info = info;
        this.architecture = info.architecture();
        this.font = font;
        this.onClose = onClose;

        if (architecture != null) {
            buildArchitectureColumns(architecture.rows());
        } else {
            List<SequentialFailureInfo.Row> rows = info.rows();
            SequentialFailureInfo.Row first = rows.isEmpty() ? null : rows.get(0);
            buildColumns(first, rows);
        }

        int tableWidth = 0;
        for (Column column : columns) {
            tableWidth += column.width();
        }
        this.winW = Math.min(Math.max(260, tableWidth + 2 * PADDING), screenWidth - 20);
        this.winH = Math.min(architecture == null ? 280 : 300, screenHeight - 20);
        this.winX = (screenWidth - winW) / 2;
        this.winY = (screenHeight - winH) / 2;

        this.closeX = winX + winW - CLOSE_W - 6;
        this.closeY = winY + winH - CLOSE_H - 6;
        this.copyX = closeX - 6 - COPY_W;
        this.copyY = closeY;
        this.rowsTop = winY + 30 + HEADER_HEIGHT;
        this.rowsBottom = closeY - 6;
        this.visibleRows = Math.max(1, (rowsBottom - rowsTop) / ROW_HEIGHT);
        int rowCount = architecture != null ? architecture.rows().size() : info.rows().size();
        this.maxScroll = Math.max(0, rowCount - visibleRows);
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

    private void buildArchitectureColumns(List<ArchitectureFailureInfo.Row> rows) {
        int x = 0;
        x = addArchitectureColumn(Kind.TICK, "tick",
                Component.translatable("gui.elements-plus.experiment_table.sequential_tick").getString(), x, rows);
        x = addArchitectureColumn(Kind.ARCH_PC, "PC", "PC", x, rows);
        x = addArchitectureColumn(Kind.ARCH_INS, "INS", "INS", x, rows);
        x = addArchitectureColumn(Kind.ARCH_ASM, "ASM",
                Component.translatable("gui.elements-plus.experiment_table.sequential_assembly").getString(), x, rows);

        String[] inputHeaders = {"VI", "RI", "DI", "VO", "RO", "DO"};
        Kind[] inputKinds = {
                Kind.ARCH_VI, Kind.ARCH_RI, Kind.ARCH_DI,
                Kind.ARCH_VO, Kind.ARCH_RO, Kind.ARCH_DO
        };
        for (int i = 0; i < inputHeaders.length; i++) {
            x = addArchitectureColumn(inputKinds[i], inputHeaders[i], inputHeaders[i], x, rows);
        }
    }

    private int addArchitectureColumn(Kind kind, String label, String header, int x,
                                      List<ArchitectureFailureInfo.Row> rows) {
        int width = font.width(header);
        for (ArchitectureFailureInfo.Row row : rows) {
            String actual = architectureActualText(row, kind);
            width = Math.max(width, font.width(actual));
            if (architectureIsOutput(kind) && !architectureMatches(row, kind)) {
                width = Math.max(width, font.width(actual + " (" + architectureExpectedText(row, kind) + ")"));
            }
        }
        Column column = new Column(kind, label, header, x, width + 2 * ARCH_PADDING);
        columns.add(column);
        return x + column.width();
    }

    private static String architectureActualText(ArchitectureFailureInfo.Row row, Kind kind) {
        NumberFormat digital = ClientConfig.get().digitalNumberFormat;
        return switch (kind) {
            case TICK -> Integer.toString(row.tick());
            case ARCH_PC -> digital.formatDigital(row.pc());
            case ARCH_INS -> digital.formatDigital(row.ins());
            case ARCH_ASM -> ArchitectureInstruction.assembly(row.ins());
            case ARCH_VI -> Integer.toString(row.vi());
            case ARCH_RI -> Integer.toString(row.ri());
            case ARCH_DI -> digital.formatDigital(row.di());
            case ARCH_VO -> Integer.toString(row.vo());
            case ARCH_RO -> Integer.toString(row.ro());
            case ARCH_DO -> digital.formatDigital(row.doValue());
            default -> "";
        };
    }

    private static String architectureExpectedText(ArchitectureFailureInfo.Row row, Kind kind) {
        NumberFormat digital = ClientConfig.get().digitalNumberFormat;
        return switch (kind) {
            case ARCH_PC -> digital.formatDigital(row.expectedPc());
            case ARCH_INS -> digital.formatDigital(row.expectedIns());
            case ARCH_RI -> Integer.toString(row.expectedRi());
            case ARCH_VO -> Integer.toString(row.expectedVo());
            case ARCH_DO -> digital.formatDigital(row.expectedDo());
            default -> "";
        };
    }

    private static boolean architectureIsOutput(Kind kind) {
        return kind == Kind.ARCH_PC || kind == Kind.ARCH_INS
                || kind == Kind.ARCH_RI || kind == Kind.ARCH_VO || kind == Kind.ARCH_DO;
    }

    private static boolean architectureMatches(ArchitectureFailureInfo.Row row, Kind kind) {
        return switch (kind) {
            case ARCH_PC -> row.pc() == row.expectedPc();
            case ARCH_INS -> row.ins() == row.expectedIns();
            case ARCH_RI -> row.ri() == row.expectedRi();
            case ARCH_VO -> row.vo() == row.expectedVo();
            case ARCH_DO -> row.doValue() == row.expectedDo();
            default -> true;
        };
    }

    private static String valueFor(SequentialFailureInfo.Row row, Kind kind, String label) {
        List<ExperimentFailureInfo.Pin> pins = switch (kind) {
            case TICK -> null;
            case INPUT -> row.inputs();
            case EXPECTED -> row.expected();
            case ACTUAL -> row.actual();
            default -> null;
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

    public int getWindowX() {
        return winX;
    }

    public int getWindowWidth() {
        return winW;
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

        int rowCount = architecture != null ? architecture.rows().size() : info.rows().size();
        g.enableScissor(tableX, rowsTop, tableX + winW - 2 * PADDING, rowsBottom);
        for (int i = 0; i < visibleRows; i++) {
            int rowIndex = scrollRow + i;
            if (rowIndex >= rowCount) {
                break;
            }
            int y = rowsTop + i * ROW_HEIGHT;
            if (architecture != null) {
                ArchitectureFailureInfo.Row row = architecture.rows().get(rowIndex);
                for (Column column : columns) {
                    int cellX = tableX + column.x() + 2;
                    if (architectureIsOutput(column.kind())) {
                        drawArchitectureOutputCell(g, row, column.kind(), cellX, y + 2);
                    } else {
                        int color = column.kind() == Kind.TICK ? 0xFFFFFFFF : COLOR_MUTED;
                        if (column.kind() == Kind.ARCH_ASM) {
                            color = 0xFFFFFFFF;
                        }
                        g.drawString(font, architectureActualText(row, column.kind()), cellX, y + 2, color, false);
                    }
                }
            } else {
                SequentialFailureInfo.Row row = info.rows().get(rowIndex);
                for (Column column : columns) {
                    String text = valueFor(row, column.kind(), column.label());
                    int color = colorFor(column, row);
                    g.drawString(font, text, tableX + column.x() + 2, y + 2, color, false);
                }
            }
        }
        g.disableScissor();

        if (maxScroll > 0) {
            int barX = winX + winW - 6;
            int barTop = rowsTop;
            int barBottom = rowsBottom;
            g.fill(barX, barTop, barX + 3, barBottom, 0xFF606060);
            int thumbHeight = Math.max(8, (barBottom - barTop) * visibleRows / Math.max(1, rowCount));
            int thumbY = barTop + (barBottom - barTop - thumbHeight) * scrollRow / maxScroll;
            g.fill(barX, thumbY, barX + 3, thumbY + thumbHeight, 0xFFC0C0C0);
        }

        if (architecture != null) {
            boolean hoveredCopy = mouseX >= copyX && mouseX < copyX + COPY_W
                    && mouseY >= copyY && mouseY < copyY + CLOSE_H;
            g.fill(copyX, copyY, copyX + COPY_W, copyY + CLOSE_H, hoveredCopy ? 0xFFFFFFFF : 0xFF000000);
            GuiUtil.drawSubPanel(g, copyX + 1, copyY + 1, copyX + COPY_W - 1, copyY + CLOSE_H - 1,
                    0xFF808080, GuiUtil.SubPanelType.CONVEX);
            g.drawCenteredString(font, Component.translatable("gui.elements-plus.experiment_table.sequential_copy_program"),
                    copyX + COPY_W / 2, copyY + (CLOSE_H - 9) / 2, 0xFFFFFFFF);
        }

        boolean hoveredClose = mouseX >= closeX && mouseX < closeX + CLOSE_W && mouseY >= closeY && mouseY < closeY + CLOSE_H;
        g.fill(closeX, closeY, closeX + CLOSE_W, closeY + CLOSE_H, hoveredClose ? 0xFFFFFFFF : 0xFF000000);
        GuiUtil.drawSubPanel(g, closeX + 1, closeY + 1, closeX + CLOSE_W - 1, closeY + CLOSE_H - 1,
                0xFF808080, GuiUtil.SubPanelType.CONVEX);
        g.drawCenteredString(font, Component.translatable("gui.elements-plus.experiment_table.sequential_close"),
                closeX + CLOSE_W / 2, closeY + (CLOSE_H - 9) / 2, 0xFFFFFFFF);
    }

    private void drawArchitectureOutputCell(GuiGraphics g, ArchitectureFailureInfo.Row row,
                                            Kind kind, int x, int y) {
        String actual = architectureActualText(row, kind);
        if (architectureMatches(row, kind)) {
            g.drawString(font, actual, x, y, COLOR_ACTUAL_OK, false);
            return;
        }
        g.drawString(font, actual, x, y, COLOR_ACTUAL_BAD, false);
        String expected = " (" + architectureExpectedText(row, kind) + ")";
        g.drawString(font, expected, x + font.width(actual), y, COLOR_ACTUAL_OK, false);
    }

    private static int colorFor(Column column, SequentialFailureInfo.Row row) {
        if (column.kind() == Kind.TICK) {
            return 0xFFFFFFFF;
        }
        if (column.kind() != Kind.ACTUAL) {
            return COLOR_MUTED;
        }
        if (row.expected().isEmpty()) {
            // 交互式测例的逐 tick 记录没有逐 tick 预期输出，正常显示实际输出。
            return 0xFFFFFFFF;
        }
        ExperimentFailureInfo.Pin expected = findPin(row.expected(), column.label());
        ExperimentFailureInfo.Pin actual = findPin(row.actual(), column.label());
        return sameValue(expected, actual) ? COLOR_ACTUAL_OK : COLOR_ACTUAL_BAD;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && architecture != null
                && mouseX >= copyX && mouseX < copyX + COPY_W
                && mouseY >= copyY && mouseY < copyY + CLOSE_H) {
            copyProgram();
            return true;
        }
        if (button == 0 && mouseX >= closeX && mouseX < closeX + CLOSE_W
                && mouseY >= closeY && mouseY < closeY + CLOSE_H) {
            onClose.run();
        }
        return true;
    }

    private void copyProgram() {
        byte[] program = architecture.program();
        int count = architecture.copyWholeProgram()
                ? program.length
                : Math.min(program.length, info.failedTick() + 1);
        NumberFormat format = ClientConfig.get().digitalNumberFormat;
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(format.formatDigital(program[i] & 0xFF));
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(text.toString());
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
