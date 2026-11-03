package com.elementsplus.client.gui;

import com.elementsplus.client.config.ClientConfig;
import com.elementsplus.client.config.NumberFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        GuiUtil.raisePose(g);
        g.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), 0x80000000);
        GuiUtil.drawMainPanel(g, winX, winY, winX + winW, winY + winH);
        g.drawCenteredString(font, title, winX + winW / 2, winY + 8, 0xFFFFFFFF);

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
                    color = isCurrent ? 0xFF000000 : (isSelected ? 0xFFFFFFFF : 0xFFFFFFFF);
                }
                int textX = cellX + (CELL_WIDTH - 1 - font.width(value)) / 2;
                g.drawString(font, value, textX, cellY + 2, color, false);
                if (editing && index == focus && textSelected) {
                    g.fill(textX, cellY + 1, textX + font.width(value), cellY + 1 + font.lineHeight, 0x4000A0FF);
                }
            }
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

    private void renderButton(GuiGraphics g, int x, int y, Component label, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + BUTTON_WIDTH && mouseY >= y && mouseY < y + BUTTON_HEIGHT;
        g.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, hovered ? 0xFFFFFFFF : 0xFF000000);
        GuiUtil.drawSubPanel(g, x + 1, y + 1, x + BUTTON_WIDTH - 1, y + BUTTON_HEIGHT - 1, 0xFF808080, GuiUtil.SubPanelType.CONVEX);
        if (hovered) {
            g.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, 0x30FFFFFF);
        }
        g.drawCenteredString(font, label, x + BUTTON_WIDTH / 2, y + 4, 0xFFFFFFFF);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
        if (button != 0) {
            return true;
        }
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
    }

    private void beginEdit(int index) {
        focus = index;
        selectionOnly(index);
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

    private int indexAt(double mouseX, double mouseY) {
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
