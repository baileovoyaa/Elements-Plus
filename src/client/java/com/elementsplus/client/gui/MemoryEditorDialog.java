package com.elementsplus.client.gui;

import com.elementsplus.client.config.ClientConfig;
import com.elementsplus.client.config.NumberFormat;
import com.elementsplus.core.experiment.ArchitectureInstruction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntSupplier;

/**
 * RAM 查看 / ROM 编辑对话框。
 * <p>
 * ROM 编辑支持多选、复制粘贴、连续十六进制输入和对话框内独立撤销/重做；
 * RAM 模式复用同一套网格布局，但只读并高亮当前地址 D 对应的数据。
 */
public class MemoryEditorDialog {
    private static final int MARGIN = 8;
    private static final int ADDRESS_WIDTH = 30;
    private static final int CELL_WIDTH = 17;
    private static final int CELL_HEIGHT = 11;
    private static final int HEADER_HEIGHT = 28;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_WIDTH = 44;
    private static final int MAX_HISTORY = 100;
    private static final int ROW_HEIGHT = 12;
    private static final int ROW_HEADER_HEIGHT = 12;
    private static final int ROW_ADDRESS_WIDTH = 30;
    private static final int ROW_VALUE_WIDTH = 34;
    private static final int SCROLLBAR_WIDTH = 6;

    public interface Callback {
        void onSave(byte[] data);

        void onCancel();
    }

    private final Font font;
    private final Component title;
    private final boolean editable;
    private final int capacity;
    private final int columns;
    private final int rows;
    private final IntSupplier currentAddressSupplier;
    private final Callback callback;
    private final byte[] values;
    private final Deque<byte[]> undoStack = new ArrayDeque<>();
    private final Deque<byte[]> redoStack = new ArrayDeque<>();
    private final Set<Integer> selected = new LinkedHashSet<>();

    private final int winX;
    private final int winY;
    private final int winW;
    private final int winH;
    private final int gridX;
    private final int gridY;
    private final int cancelX;
    private final int okX;
    private final int undoX;
    private final int redoX;
    private final int buttonY;
    private final int toggleX;
    private final int toggleY;
    private final int toggleW;
    private final int rowHeaderY;
    private final int rowTableX;
    private final int rowTableY;
    private final int rowTableRight;
    private final int rowTableBottom;
    private final int rowValueX;
    private final int rowValueWidth;
    private final int rowAssemblyX;
    private final int rowVisibleRows;

    private boolean rowView;
    private int rowScroll;
    private double rowDragMouseX;
    private double rowDragMouseY;
    private boolean draggingRowScrollbar;
    private double rowScrollbarGrabOffset;

    private int focus = -1;
    private int anchor = -1;
    private boolean editing;
    private boolean textSelected;
    private boolean ctrlATextSelected;
    private String editBuffer = "";
    private boolean dragSelecting;
    private boolean dragMoved;
    private int dragStart = -1;
    private boolean middleDragging;
    private int middleStart = -1;

    public MemoryEditorDialog(Component title, byte[] data, int capacity, int currentAddress,
                              boolean editable, Font font, int screenWidth, int screenHeight,
                              Callback callback) {
        this(title, data, capacity, () -> currentAddress, editable, font, screenWidth, screenHeight, callback);
    }

    public MemoryEditorDialog(Component title, byte[] data, int capacity, IntSupplier currentAddressSupplier,
                              boolean editable, Font font, int screenWidth, int screenHeight,
                              Callback callback) {
        this.title = title;
        this.font = font;
        this.capacity = Math.max(1, capacity);
        this.columns = this.capacity <= 4 ? 4 : 16;
        this.rows = Math.max(1, (this.capacity + columns - 1) / columns);
        this.currentAddressSupplier = currentAddressSupplier;
        this.editable = editable;
        this.callback = callback;
        this.values = new byte[this.capacity];
        if (data != null) {
            System.arraycopy(data, 0, values, 0, Math.min(data.length, values.length));
        }

        this.winW = ADDRESS_WIDTH + columns * CELL_WIDTH + MARGIN * 2;
        this.winH = HEADER_HEIGHT + rows * CELL_HEIGHT + 30;
        this.winX = Math.max(2, (screenWidth - winW) / 2);
        this.winY = Math.max(2, (screenHeight - winH) / 2);
        this.gridX = winX + MARGIN + ADDRESS_WIDTH;
        this.gridY = winY + HEADER_HEIGHT;
        this.buttonY = winY + winH - BUTTON_HEIGHT - 6;
        this.undoX = winX + MARGIN;
        this.redoX = undoX + BUTTON_WIDTH + 4;
        this.cancelX = winX + winW - BUTTON_WIDTH * 2 - MARGIN - 4;
        this.okX = winX + winW - BUTTON_WIDTH - MARGIN;

        Component rowViewLabel = Component.translatable("gui.elements-plus.memory.switch_row_view");
        Component gridViewLabel = Component.translatable("gui.elements-plus.memory.switch_grid_view");
        this.toggleW = Math.max(86, Math.max(font.width(rowViewLabel), font.width(gridViewLabel)) + 10);
        this.toggleX = winX + winW - MARGIN - toggleW;
        this.toggleY = winY + 4;
        this.rowHeaderY = winY + HEADER_HEIGHT;
        this.rowTableX = winX + MARGIN;
        this.rowTableY = rowHeaderY + ROW_HEADER_HEIGHT;
        this.rowTableRight = winX + winW - MARGIN - SCROLLBAR_WIDTH;
        this.rowTableBottom = buttonY - 6;
        this.rowValueX = rowTableX + ROW_ADDRESS_WIDTH;
        this.rowValueWidth = ROW_VALUE_WIDTH;
        this.rowAssemblyX = rowValueX + rowValueWidth;
        this.rowVisibleRows = Math.max(1, (rowTableBottom - rowTableY) / ROW_HEIGHT);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        GuiUtil.raisePose(g);
        g.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), 0x80000000);
        GuiUtil.drawMainPanel(g, winX, winY, winX + winW, winY + winH);
        g.drawCenteredString(font, title, winX + winW / 2, winY + 8, 0xFFFFFFFF);
        if (editable) {
            Component toggleLabel = Component.translatable(rowView
                    ? "gui.elements-plus.memory.switch_grid_view"
                    : "gui.elements-plus.memory.switch_row_view");
            renderButton(g, toggleX, toggleY, toggleW, BUTTON_HEIGHT, toggleLabel, mouseX, mouseY);
        }

        if (rowView) {
            renderRowView(g);
        } else {
            renderGridView(g);
        }

        if (editable) {
            renderButton(g, undoX, buttonY, Component.translatable("gui.elements-plus.memory.undo"), mouseX, mouseY);
            renderButton(g, redoX, buttonY, Component.translatable("gui.elements-plus.memory.redo"), mouseX, mouseY);
            renderButton(g, cancelX, buttonY, Component.translatable("gui.elements-plus.toolbox.dialog.cancel"), mouseX, mouseY);
        }
        renderButton(g, okX, buttonY,
                Component.translatable(editable ? "gui.elements-plus.toolbox.dialog.ok" : "gui.elements-plus.memory.close"),
                mouseX, mouseY);
    }

    private void renderGridView(GuiGraphics g) {
        NumberFormat format = ClientConfig.get().digitalNumberFormat;
        int currentAddress = currentAddressSupplier == null ? -1 : currentAddressSupplier.getAsInt();
        for (int row = 0; row < rows; row++) {
            int rowY = gridY + row * CELL_HEIGHT;
            int rowAddress = row * columns;
            String addressText = format.formatDigital(rowAddress);
            g.drawString(font, addressText, winX + MARGIN + ADDRESS_WIDTH - 2 - font.width(addressText),
                    rowY + 2, 0xFF808080, false);
            for (int col = 0; col < columns; col++) {
                int index = rowAddress + col;
                if (index >= capacity) {
                    break;
                }
                int cellX = gridX + col * CELL_WIDTH;
                int cellY = rowY;
                boolean isSelected = selected.contains(index);
                boolean isCurrent = index == currentAddress;
                if (isCurrent) {
                    g.fill(cellX, cellY, cellX + CELL_WIDTH - 1, cellY + CELL_HEIGHT - 1, 0xFFFFD700);
                } else if (isSelected) {
                    g.fill(cellX, cellY, cellX + CELL_WIDTH - 1, cellY + CELL_HEIGHT - 1, 0xFF3A6EC5);
                } else {
                    g.fill(cellX, cellY, cellX + CELL_WIDTH - 1, cellY + CELL_HEIGHT - 1, 0xFF202020);
                }
                String value;
                int color;
                if (editing && index == focus) {
                    value = editBuffer;
                    color = 0xFFFFFFFF;
                } else {
                    value = format.formatDigital(values[index] & 0xFF);
                    color = isCurrent ? 0xFF000000 : 0xFFFFFFFF;
                }
                int textX = cellX + (CELL_WIDTH - 1 - font.width(value)) / 2;
                g.drawString(font, value, textX, cellY + 2, color, false);
                if (editing && index == focus && textSelected) {
                    g.fill(textX, cellY + 1, textX + font.width(value), cellY + 1 + font.lineHeight, 0x4000A0FF);
                }
            }
        }
    }

    private void renderRowView(GuiGraphics g) {
        NumberFormat format = ClientConfig.get().digitalNumberFormat;
        int currentAddress = currentAddressSupplier == null ? -1 : currentAddressSupplier.getAsInt();
        g.fill(rowTableX, rowHeaderY, rowTableRight, rowHeaderY + ROW_HEADER_HEIGHT, 0x50000000);
        g.drawString(font, Component.translatable("gui.elements-plus.memory.address"),
                rowTableX + 2, rowHeaderY + 2, 0xFFFFE066, false);
        g.drawString(font, Component.translatable("gui.elements-plus.memory.value"),
                rowValueX + 2, rowHeaderY + 2, 0xFFFFE066, false);
        g.drawString(font, Component.translatable("gui.elements-plus.memory.assembly"),
                rowAssemblyX + 2, rowHeaderY + 2, 0xFFFFE066, false);

        for (int i = 0; i < rowVisibleRows; i++) {
            int index = rowScroll + i;
            if (index >= capacity) {
                break;
            }
            int rowY = rowTableY + i * ROW_HEIGHT;
            boolean isSelected = selected.contains(index);
            boolean isCurrent = index == currentAddress;
            if (isCurrent) {
                g.fill(rowTableX, rowY, rowTableRight, rowY + ROW_HEIGHT - 1, 0xFFFFD700);
            } else if (isSelected) {
                g.fill(rowTableX, rowY, rowTableRight, rowY + ROW_HEIGHT - 1, 0xFF3A6EC5);
            } else {
                g.fill(rowTableX, rowY, rowTableRight, rowY + ROW_HEIGHT - 1, 0xFF202020);
            }

            int textColor = isCurrent ? 0xFF000000 : 0xFFFFFFFF;
            String addressText = format.formatDigital(index);
            g.drawString(font, addressText, rowTableX + ROW_ADDRESS_WIDTH - 2 - font.width(addressText),
                    rowY + 2, isCurrent ? 0xFF000000 : 0xFF808080, false);

            String value = editing && index == focus
                    ? editBuffer
                    : format.formatDigital(values[index] & 0xFF);
            g.drawString(font, value, rowValueX + 2, rowY + 2, textColor, false);
            if (editing && index == focus && textSelected) {
                int textX = rowValueX + 2;
                g.fill(textX, rowY + 1, textX + font.width(value), rowY + 1 + font.lineHeight, 0x4000A0FF);
            }

            g.drawString(font, ArchitectureInstruction.assembly(values[index] & 0xFF),
                    rowAssemblyX + 2, rowY + 2, textColor, false);
        }

        int maxScroll = maxRowScroll();
        if (maxScroll > 0) {
            int barX = rowScrollbarX();
            g.fill(barX, rowTableY, barX + 3, rowTableBottom, 0xFF606060);
            int thumbY = rowScrollbarThumbY();
            g.fill(barX, thumbY, barX + 3, thumbY + rowScrollbarThumbHeight(), 0xFFC0C0C0);
        }
    }

    private void renderButton(GuiGraphics g, int x, int y, Component label, int mouseX, int mouseY) {
        renderButton(g, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, label, mouseX, mouseY);
    }

    private void renderButton(GuiGraphics g, int x, int y, int width, int height,
                              Component label, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        g.fill(x, y, x + width, y + height, hovered ? 0xFFFFFFFF : 0xFF000000);
        GuiUtil.drawSubPanel(g, x + 1, y + 1, x + width - 1, y + height - 1, 0xFF808080, GuiUtil.SubPanelType.CONVEX);
        if (hovered) {
            g.fill(x, y, x + width, y + height, 0x30FFFFFF);
        }
        g.drawCenteredString(font, label, x + width / 2, y + (height - 9) / 2, 0xFFFFFFFF);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 2) {
            return true;
        }
        if (editable && button == 0
                && inRect(mouseX, mouseY, toggleX, toggleY, toggleW, BUTTON_HEIGHT)) {
            toggleView();
            return true;
        }
        if (button == 0) {
            if (inRect(mouseX, mouseY, cancelX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                cancel();
                return true;
            }
            if (inRect(mouseX, mouseY, okX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                save();
                return true;
            }
            if (editable && inRect(mouseX, mouseY, undoX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                undo();
                return true;
            }
            if (editable && inRect(mouseX, mouseY, redoX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                redo();
                return true;
            }
        }

        if (rowView) {
            if (button == 0 && maxRowScroll() > 0 && isOverRowScrollbar(mouseX, mouseY)) {
                int thumbTop = rowScrollbarThumbY();
                int thumbBottom = thumbTop + rowScrollbarThumbHeight();
                if (mouseY >= thumbTop && mouseY < thumbBottom) {
                    draggingRowScrollbar = true;
                    rowScrollbarGrabOffset = mouseY - thumbTop;
                } else if (mouseY < thumbTop) {
                    rowScroll = Mth.clamp(rowScroll - rowVisibleRows, 0, maxRowScroll());
                } else {
                    rowScroll = Mth.clamp(rowScroll + rowVisibleRows, 0, maxRowScroll());
                }
                return true;
            }
            int index = rowIndexAt(mouseX, mouseY, false);
            if (index >= 0) {
                commitCurrent();
                selectionOnly(index);
                focus = index;
                dragSelecting = true;
                dragMoved = false;
                dragStart = index;
                rowDragMouseX = mouseX;
                rowDragMouseY = mouseY;
                if ((button == 0 || button == 2) && editable && inRowValueColumn(mouseX)) {
                    beginEdit(index);
                } else {
                    editing = false;
                    textSelected = false;
                }
            }
            return true;
        }

        if (button == 2) {
            int index = indexAt(mouseX, mouseY);
            if (index >= 0) {
                commitCurrent();
                editing = false;
                textSelected = false;
                selectionOnly(index);
                focus = index;
                middleStart = index;
                middleDragging = true;
            }
            return true;
        }

        int index = indexAt(mouseX, mouseY);
        if (index >= 0) {
            commitCurrent();
            selected.clear();
            selected.add(index);
            focus = index;
            anchor = index;
            dragSelecting = true;
            dragMoved = false;
            dragStart = index;
            beginEdit(index);
            return true;
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (rowView) {
            if (draggingRowScrollbar && button == 0) {
                int barTop = rowTableY;
                int barBottom = rowTableBottom;
                int thumbHeight = rowScrollbarThumbHeight();
                int travel = Math.max(1, barBottom - barTop - thumbHeight);
                double thumbTop = Mth.clamp(mouseY - rowScrollbarGrabOffset, barTop, barBottom - thumbHeight);
                rowScroll = (int) Math.round(thumbTop - barTop) * maxRowScroll() / travel;
                rowScroll = Mth.clamp(rowScroll, 0, maxRowScroll());
                return true;
            }
            if ((button == 0 || button == 2) && dragSelecting) {
                rowDragMouseX = mouseX;
                rowDragMouseY = mouseY;
                int index = rowIndexAt(mouseX, mouseY, true);
                if (index >= 0 && index != dragStart) {
                    dragMoved = true;
                    editing = false;
                    textSelected = false;
                    selectLinear(dragStart, index);
                    focus = index;
                }
            }
            return true;
        }
        if (button == 0 && dragSelecting) {
            int index = indexAt(mouseX, mouseY);
            if (index >= 0 && index != dragStart) {
                dragMoved = true;
                editing = false;
                textSelected = false;
                // 左键拖动：像文本选择一样按行优先连续选择。
                selectLinear(dragStart, index);
                focus = index;
            }
            return true;
        }
        if (button == 2 && middleDragging) {
            int index = indexAt(mouseX, mouseY);
            if (index >= 0) {
                editing = false;
                textSelected = false;
                // 中键拖动：保留原来的矩形选择。
                selectRectangle(middleStart, index);
                focus = index;
            }
            return true;
        }
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (rowView) {
            if (draggingRowScrollbar && button == 0) {
                draggingRowScrollbar = false;
                return true;
            }
            if ((button == 0 || button == 2) && dragSelecting) {
                dragSelecting = false;
            }
            return true;
        }
        if (button == 2) {
            middleDragging = false;
            middleStart = -1;
            return true;
        }
        if (button == 0 && dragSelecting) {
            dragSelecting = false;
            if (!dragMoved && editable && focus >= 0) {
                beginEdit(focus);
            } else {
                editing = false;
            }
            return true;
        }
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancel();
            return true;
        }
        boolean control = Screen.hasControlDown();
        boolean shift = Screen.hasShiftDown();
        if (control && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            save();
            return true;
        }
        if (control && keyCode == GLFW.GLFW_KEY_Z) {
            if (shift) {
                redo();
            } else {
                undo();
            }
            return true;
        }
        if (control && keyCode == GLFW.GLFW_KEY_Y) {
            redo();
            return true;
        }
        if (control && keyCode == GLFW.GLFW_KEY_A) {
            if (editing) {
                if (!ctrlATextSelected) {
                    textSelected = true;
                    ctrlATextSelected = true;
                } else {
                    selectAll();
                }
            } else {
                selectAll();
            }
            return true;
        }
        if (control && keyCode == GLFW.GLFW_KEY_C) {
            copySelection();
            return true;
        }
        if (control && keyCode == GLFW.GLFW_KEY_V) {
            paste();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB || keyCode == GLFW.GLFW_KEY_SPACE) {
            moveLinear(shift ? -1 : 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            moveToNextRowStart();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            moveGrid(-1, 0);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            moveGrid(1, 0);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            moveGrid(0, -1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            moveGrid(0, 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            deleteSelection();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            backspace();
            return true;
        }
        return true;
    }

    public boolean charTyped(char c, int modifiers) {
        if (!editable || !editing || selected.size() != 1 || focus < 0 || Screen.hasControlDown()) {
            return false;
        }
        NumberFormat format = ClientConfig.get().digitalNumberFormat;
        String candidate = (textSelected ? "" : editBuffer) + c;
        if (!format.acceptsPartial(candidate)) {
            return false;
        }
        editBuffer = candidate;
        textSelected = false;
        ctrlATextSelected = false;
        if (format == NumberFormat.HEX && editBuffer.length() >= 2) {
            int current = focus;
            commitCurrent();
            beginEdit((current + 1) % capacity);
        }
        return true;
    }

    public void tick() {
        if (!rowView || !dragSelecting) {
            return;
        }
        int delta = 0;
        if (rowDragMouseY < rowTableY) {
            delta = -1;
        } else if (rowDragMouseY >= rowTableBottom) {
            delta = 1;
        }
        if (delta != 0) {
            int before = rowScroll;
            rowScroll = Mth.clamp(rowScroll + delta, 0, maxRowScroll());
            if (rowScroll != before) {
                int index = rowIndexAt(rowDragMouseX, rowDragMouseY, true);
                if (index >= 0) {
                    dragMoved = true;
                    editing = false;
                    textSelected = false;
                    selectLinear(dragStart, index);
                    focus = index;
                }
            }
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!rowView || verticalAmount == 0) {
            return true;
        }
        rowScroll = Mth.clamp(rowScroll - (int) Math.signum(verticalAmount), 0, maxRowScroll());
        return true;
    }

    private void beginEdit(int index) {
        focus = index;
        selectionOnly(index);
        ensureFocusVisible();
        if (!editable) {
            editing = false;
            return;
        }
        editing = true;
        textSelected = true;
        ctrlATextSelected = false;
        editBuffer = ClientConfig.get().digitalNumberFormat.formatDigital(values[index] & 0xFF);
    }

    private void selectionOnly(int index) {
        selected.clear();
        selected.add(index);
        anchor = index;
    }

    private void commitCurrent() {
        if (!editing || !editable || focus < 0) {
            editing = false;
            return;
        }
        NumberFormat format = ClientConfig.get().digitalNumberFormat;
        try {
            int value = format.parseDigital(editBuffer);
            if ((values[focus] & 0xFF) != value) {
                pushUndo();
                values[focus] = (byte) value;
            }
        } catch (NumberFormatException ignored) {
            // 无效输入不写入，保持原值。
        }
        editing = false;
        textSelected = false;
    }

    private void moveLinear(int delta) {
        if (focus < 0) {
            beginEdit(0);
            return;
        }
        commitCurrent();
        int next = Math.floorMod(focus + delta, capacity);
        beginEdit(next);
    }

    private void moveGrid(int rowDelta, int colDelta) {
        if (focus < 0) {
            beginEdit(0);
            return;
        }
        commitCurrent();
        if (rowView) {
            int delta = rowDelta != 0 ? rowDelta : colDelta;
            int next = Math.floorMod(focus + delta, capacity);
            beginEdit(next);
            return;
        }
        int row = focus / columns;
        int col = focus % columns;
        row = Math.max(0, Math.min(rows - 1, row + rowDelta));
        col = Math.max(0, Math.min(columns - 1, col + colDelta));
        int next = row * columns + col;
        if (next >= capacity) {
            next = capacity - 1;
        }
        beginEdit(next);
    }

    private void moveToNextRowStart() {
        if (focus < 0) {
            beginEdit(0);
            return;
        }
        commitCurrent();
        if (rowView) {
            beginEdit((focus + 1) % capacity);
            return;
        }
        int row = focus / columns;
        int next = ((row + 1) % rows) * columns;
        if (next >= capacity) {
            next = 0;
        }
        beginEdit(next);
    }

    private void selectAll() {
        commitCurrent();
        selected.clear();
        for (int i = 0; i < capacity; i++) {
            selected.add(i);
        }
        focus = 0;
        editing = false;
        textSelected = false;
        if (rowView) {
            rowScroll = 0;
        }
    }

    private void selectLinear(int from, int to) {
        int start = Math.min(from, to);
        int end = Math.max(from, to);
        selected.clear();
        for (int index = start; index <= end && index < capacity; index++) {
            selected.add(index);
        }
        anchor = from;
    }

    private void selectRectangle(int from, int to) {
        int fromRow = from / columns, fromCol = from % columns;
        int toRow = to / columns, toCol = to % columns;
        int minRow = Math.min(fromRow, toRow), maxRow = Math.max(fromRow, toRow);
        int minCol = Math.min(fromCol, toCol), maxCol = Math.max(fromCol, toCol);
        selected.clear();
        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                int index = row * columns + col;
                if (index < capacity) {
                    selected.add(index);
                }
            }
        }
        anchor = from;
    }

    private void copySelection() {
        List<Integer> indices = selectedIndices();
        if (indices.isEmpty()) {
            return;
        }
        NumberFormat format = ClientConfig.get().digitalNumberFormat;
        StringBuilder sb = new StringBuilder();
        for (int index : indices) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(format.formatDigital(values[index] & 0xFF));
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(sb.toString());
    }

    private void paste() {
        if (!editable) {
            return;
        }
        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clipboard == null || clipboard.isBlank()) {
            return;
        }
        NumberFormat format = ClientConfig.get().digitalNumberFormat;
        String[] parts = clipboard.trim().split("[\\s,]+");
        List<Integer> parsed = new ArrayList<>(parts.length);
        for (String part : parts) {
            try {
                parsed.add(format.parseDigital(part));
            } catch (NumberFormatException e) {
                return;
            }
        }
        if (parsed.isEmpty()) {
            return;
        }

        if (parsed.size() == 1 && editing && selected.size() == 1 && focus >= 0) {
            editBuffer = format.formatDigital(parsed.get(0));
            textSelected = true;
            ctrlATextSelected = false;
            return;
        }

        List<Integer> targets = new ArrayList<>();
        if (parsed.size() == 1) {
            targets.addAll(selectedIndices());
            if (targets.isEmpty()) {
                targets.add(Math.max(0, focus));
            }
        } else {
            int start = selectedIndices().isEmpty() ? Math.max(0, focus) : Collections.min(selectedIndices());
            for (int i = 0; i < parsed.size() && start + i < capacity; i++) {
                targets.add(start + i);
            }
        }
        if (targets.isEmpty()) {
            return;
        }

        pushUndo();
        if (parsed.size() == 1) {
            int value = parsed.get(0);
            for (int index : targets) {
                values[index] = (byte) value;
            }
        } else {
            for (int i = 0; i < targets.size() && i < parsed.size(); i++) {
                values[targets.get(i)] = (byte) (int) parsed.get(i);
            }
        }
        selected.clear();
        selected.addAll(targets);
        focus = targets.get(targets.size() - 1);
        editing = false;
        textSelected = false;
    }

    private void deleteSelection() {
        if (!editable) {
            return;
        }
        if (selected.size() > 1) {
            pushUndo();
            for (int index : selected) {
                values[index] = 0;
            }
            editing = false;
            textSelected = false;
            return;
        }
        if (editing && focus >= 0) {
            if (textSelected) {
                editBuffer = "";
            } else if (!editBuffer.isEmpty()) {
                editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
            }
            textSelected = false;
            return;
        }
        if (focus >= 0) {
            pushUndo();
            values[focus] = 0;
        }
    }

    private void backspace() {
        if (!editable) {
            return;
        }
        if (editing && focus >= 0) {
            if (textSelected) {
                editBuffer = "";
            } else if (!editBuffer.isEmpty()) {
                editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
            }
            textSelected = false;
            return;
        }
        if (focus >= 0) {
            pushUndo();
            values[focus] = 0;
        }
    }

    private List<Integer> selectedIndices() {
        List<Integer> indices = new ArrayList<>(selected);
        Collections.sort(indices);
        return indices;
    }

    private void pushUndo() {
        undoStack.addLast(values.clone());
        while (undoStack.size() > MAX_HISTORY) {
            undoStack.removeFirst();
        }
        redoStack.clear();
    }

    private void undo() {
        if (!editable || undoStack.isEmpty()) {
            return;
        }
        redoStack.addLast(values.clone());
        byte[] previous = undoStack.removeLast();
        System.arraycopy(previous, 0, values, 0, values.length);
        editing = false;
        textSelected = false;
    }

    private void redo() {
        if (!editable || redoStack.isEmpty()) {
            return;
        }
        undoStack.addLast(values.clone());
        byte[] next = redoStack.removeLast();
        System.arraycopy(next, 0, values, 0, values.length);
        editing = false;
        textSelected = false;
    }

    private void save() {
        commitCurrent();
        callback.onSave(values.clone());
    }

    private void cancel() {
        callback.onCancel();
    }

    private void toggleView() {
        commitCurrent();
        rowView = !rowView;
        editing = false;
        textSelected = false;
        if (rowView) {
            int target = focus >= 0 ? focus : 0;
            rowScroll = Mth.clamp(target - rowVisibleRows / 2, 0, maxRowScroll());
        } else {
            rowScroll = 0;
        }
    }

    private int maxRowScroll() {
        return Math.max(0, capacity - rowVisibleRows);
    }

    private int rowScrollbarX() {
        return winX + winW - MARGIN - 3;
    }

    private int rowScrollbarThumbHeight() {
        int height = rowTableBottom - rowTableY;
        return Math.max(8, height * rowVisibleRows / capacity);
    }

    private int rowScrollbarThumbY() {
        int maxScroll = maxRowScroll();
        if (maxScroll <= 0) {
            return rowTableY;
        }
        int travel = Math.max(1, rowTableBottom - rowTableY - rowScrollbarThumbHeight());
        return rowTableY + travel * rowScroll / maxScroll;
    }

    private boolean isOverRowScrollbar(double mouseX, double mouseY) {
        int hitX = rowScrollbarX() - 3;
        return mouseX >= hitX && mouseX < hitX + 6
                && mouseY >= rowTableY && mouseY < rowTableBottom;
    }

    private void ensureFocusVisible() {
        if (!rowView || focus < 0) {
            return;
        }
        if (focus < rowScroll) {
            rowScroll = focus;
        } else if (focus >= rowScroll + rowVisibleRows) {
            rowScroll = focus - rowVisibleRows + 1;
        }
        rowScroll = Mth.clamp(rowScroll, 0, maxRowScroll());
    }

    private int rowIndexAt(double mouseX, double mouseY, boolean clampToTable) {
        if (mouseX < rowTableX || mouseX >= rowTableRight) {
            return -1;
        }
        if (mouseY < rowTableY) {
            return clampToTable ? rowScroll : -1;
        }
        if (mouseY >= rowTableBottom) {
            return clampToTable ? Math.min(capacity - 1, rowScroll + rowVisibleRows - 1) : -1;
        }
        int row = rowScroll + (int) ((mouseY - rowTableY) / ROW_HEIGHT);
        return row >= 0 && row < capacity ? row : -1;
    }

    private boolean inRowValueColumn(double mouseX) {
        return mouseX >= rowValueX && mouseX < rowValueX + rowValueWidth;
    }

    private int indexAt(double mouseX, double mouseY) {
        if (rowView) {
            return rowIndexAt(mouseX, mouseY, false);
        }
        if (mouseX < gridX || mouseY < gridY) {
            return -1;
        }
        int col = (int) ((mouseX - gridX) / CELL_WIDTH);
        int row = (int) ((mouseY - gridY) / CELL_HEIGHT);
        if (col < 0 || col >= columns || row < 0 || row >= rows) {
            return -1;
        }
        int index = row * columns + col;
        return index < capacity ? index : -1;
    }

    private static boolean inRect(double x, double y, int rx, int ry, int rw, int rh) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }
}
