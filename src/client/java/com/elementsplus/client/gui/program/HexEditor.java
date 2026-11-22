package com.elementsplus.client.gui.program;

import com.elementsplus.core.file.FileText;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 16 进制编辑器：左侧地址 + 十六进制网格，右侧 ASCII 对照。
 */
public final class HexEditor {
    public static final int ROW_HEIGHT = 10;
    private static final int ADDRESS_WIDTH = 34;

    private byte[] data = new byte[0];
    private int maxBytes = 256;
    private boolean focused;
    private boolean editable = true;
    private int modCount;
    private int caret;
    private int nibble;
    private int selectionAnchor = -1;
    private boolean draggingSelection;
    private boolean draggingScrollbar;
    private boolean asciiMode;
    private int scrollRow;
    private int columns = 16;
    private boolean followCaret = true;
    private List<RemoteCursor> remote = List.of();
    private double mouseX;
    private double mouseY;

    public record RemoteCursor(UUID player, String name, int start, int end, int color,
                               long lastActive) {
    }

    private int renderX;
    private int renderY;
    private int renderWidth;
    private int renderHeight;
    private int gridX;
    private int gridY;
    private int asciiX;
    private int cellWidth;
    private int visibleRows;
    private int scrollBarX;
    private int scrollBarY;
    private int scrollBarHeight;
    private int scrollBarThumbY;
    private int scrollBarThumbHeight;
    private int scrollbarGrabOffset = -1;

    public void setData(byte[] data, int caret) {
        this.data = data == null ? new byte[0] : data.clone();
        this.caret = Math.max(0, Math.min(this.data.length, caret));
        this.nibble = 0;
        this.selectionAnchor = -1;
        this.draggingSelection = false;
        this.followCaret = true;
    }

    /**
     * 服务端回显/协作同步时更新内容：若仍是同一字节且内容长度不变，保留当前半字节输入位置，
     * 否则输入第一个十六进制字符后会被回显重置到字节开头。
     */
    public void updateData(byte[] data, int caret) {
        byte[] next = data == null ? new byte[0] : data.clone();
        int nextCaret = Math.max(0, Math.min(next.length, caret));
        boolean samePosition = nextCaret == this.caret && next.length == this.data.length;
        this.data = next;
        this.caret = nextCaret;
        if (!samePosition) {
            this.nibble = 0;
            this.selectionAnchor = -1;
        }
    }

    public byte[] getData() {
        return data.clone();
    }

    /** 应用远端内容并平移本地光标/选区，避免字节插入/删除造成漂移。 */
    public void applyRemoteContent(byte[] nextData) {
        byte[] after = nextData == null ? new byte[0] : nextData.clone();
        byte[] before = data;
        int prefix = 0;
        int max = Math.min(before.length, after.length);
        while (prefix < max && before[prefix] == after[prefix]) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < max - prefix
                && before[before.length - 1 - suffix] == after[after.length - 1 - suffix]) {
            suffix++;
        }
        int oldEnd = before.length - suffix;
        int newEnd = after.length - suffix;
        this.caret = adjustRemoteIndex(caret, prefix, oldEnd, newEnd, after.length);
        if (selectionAnchor >= 0) {
            this.selectionAnchor = adjustRemoteIndex(selectionAnchor, prefix, oldEnd, newEnd, after.length);
        }
        this.data = after;
        this.followCaret = true;
    }

    private static int adjustRemoteIndex(int index, int prefix, int oldEnd, int newEnd, int newLength) {
        if (index < prefix) {
            return index;
        }
        if (index >= oldEnd) {
            return Math.max(0, Math.min(newLength, index + (newEnd - oldEnd)));
        }
        return Math.max(0, Math.min(newLength, newEnd));
    }

    public int size() {
        return data.length;
    }

    public int getCaret() {
        return caret;
    }

    public int getSelectionStart() {
        return selectionAnchor < 0 ? caret : Math.min(caret, selectionAnchor);
    }

    public int getSelectionEnd() {
        return selectionAnchor < 0 ? caret : Math.max(caret, selectionAnchor);
    }

    /** 内容修改计数，用于轻量判断按键是否改变了内容。 */
    public int modCount() {
        return modCount;
    }

    public boolean isAsciiMode() {
        return asciiMode;
    }

    public void setMaxBytes(int maxBytes) {
        this.maxBytes = Math.max(0, maxBytes);
    }

    public void focus() {
        focused = true;
    }

    public void blur() {
        focused = false;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void setRemote(List<RemoteCursor> remote) {
        this.remote = remote == null ? List.of() : new ArrayList<>(remote);
    }

    public void setMouse(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    /** 字节数（状态栏）。 */
    public String statusBytes() {
        return Integer.toString(data.length);
    }

    /* ============================================================
     *  编辑
     * ============================================================ */

    public boolean charTyped(char c) {
        if (!focused || !editable) {
            return false;
        }
        if (asciiMode) {
            if (c < 0x20 || c > 0x7E) {
                return false;
            }
            if (caret < data.length) {
                data[caret] = (byte) c;
                modCount++;
                caret = Math.min(data.length, caret + 1);
                selectionAnchor = caret;
                followCaret = true;
                return true;
            }
            return append((byte) c);
        }
        int value = Character.digit(c, 16);
        if (value < 0) {
            return false;
        }
        if (caret >= data.length) {
            if (data.length >= maxBytes) {
                return false;
            }
            // 在末尾追加一个新字节，并把它作为当前编辑目标。
            byte[] next = new byte[data.length + 1];
            System.arraycopy(data, 0, next, 0, data.length);
            data = next;
            modCount++;
            caret = data.length - 1;
            nibble = 0;
        }
        int current = data[caret] & 0xFF;
        if (nibble == 0) {
            data[caret] = (byte) ((current & 0x0F) | (value << 4));
            nibble = 1;
        } else {
            data[caret] = (byte) ((current & 0xF0) | value);
            nibble = 0;
            caret = Math.min(data.length, caret + 1);
        }
        selectionAnchor = caret;
        followCaret = true;
        modCount++;
        return true;
    }

    private boolean append(byte value) {
        if (data.length >= maxBytes) {
            return false;
        }
        byte[] next = new byte[data.length + 1];
        System.arraycopy(data, 0, next, 0, data.length);
        next[data.length] = value;
        data = next;
        caret = data.length;
        selectionAnchor = caret;
        nibble = 0;
        followCaret = true;
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) {
            return false;
        }
        boolean control = net.minecraft.client.gui.screens.Screen.hasControlDown();
        if (!control && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT
                || keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN
                || keyCode == GLFW.GLFW_KEY_HOME || keyCode == GLFW.GLFW_KEY_END
                || keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_PAGE_DOWN)) {
            // 键盘移动退出拖拽选择状态，否则每次移动都会保留旧选区。
            selectionAnchor = caret;
        }
        boolean mutating = keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE
                || keyCode == GLFW.GLFW_KEY_INSERT || (control && keyCode == GLFW.GLFW_KEY_V);
        if (!editable && mutating) {
            return true;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> {
                if (control) {
                    moveCaret(0);
                } else if (nibble == 1 && !asciiMode) {
                    nibble = 0;
                } else {
                    moveCaret(caret - 1);
                    nibble = 1;
                }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (control) {
                    moveCaret(data.length);
                } else if (nibble == 0 && !asciiMode) {
                    nibble = 1;
                } else {
                    moveCaret(caret + 1);
                    nibble = 0;
                }
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                moveCaret(caret - columns);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveCaret(caret + columns);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                if (control) {
                    moveCaret(0);
                } else {
                    moveCaret(caret - Math.floorMod(caret, columns));
                }
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                if (control) {
                    moveCaret(data.length);
                } else {
                    moveCaret(Math.min(data.length, caret - Math.floorMod(caret, columns) + columns));
                }
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_UP -> {
                moveCaret(caret - Math.max(1, visibleRows) * columns);
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_DOWN -> {
                moveCaret(caret + Math.max(1, visibleRows) * columns);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (caret > 0) {
                    deleteAt(caret - 1);
                    moveCaret(caret - 1);
                    nibble = 0;
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (caret < data.length) {
                    deleteAt(caret);
                }
                return true;
            }
            case GLFW.GLFW_KEY_INSERT -> {
                insertZero();
                return true;
            }
            default -> {
            }
        }
        if (control && keyCode == GLFW.GLFW_KEY_C) {
            int start = selectionStart();
            int end = selectionEnd();
            if (start < end) {
                Minecraft.getInstance().keyboardHandler.setClipboard(toHex(data, start, end));
            } else {
                Minecraft.getInstance().keyboardHandler.setClipboard(toHex(data, 0, data.length));
            }
            return true;
        }
        if (control && keyCode == GLFW.GLFW_KEY_A) {
            selectionAnchor = 0;
            caret = data.length;
            return true;
        }
        if (control && keyCode == GLFW.GLFW_KEY_X) {
            int start = selectionStart();
            int end = selectionEnd();
            if (start < end) {
                Minecraft.getInstance().keyboardHandler.setClipboard(toHex(data, start, end));
                deleteSelection();
            }
            return true;
        }
        if (control && keyCode == GLFW.GLFW_KEY_V) {
            pasteHex(Minecraft.getInstance().keyboardHandler.getClipboard());
            return true;
        }
        return false;
    }

    private void moveCaret(int index) {
        caret = Math.max(0, Math.min(maxBytes, index));
        // 键盘移动不保留拖拽选区，否则会一直“选择文本”。
        selectionAnchor = caret;
        followCaret = true;
    }

    private boolean hasSelection() {
        return selectionAnchor >= 0 && selectionStart() < selectionEnd();
    }

    private void deleteSelection() {
        int start = selectionStart();
        int end = selectionEnd();
        if (start >= end) {
            return;
        }
        byte[] next = new byte[data.length - (end - start)];
        System.arraycopy(data, 0, next, 0, start);
        System.arraycopy(data, end, next, start, data.length - end);
        data = next;
        caret = start;
        selectionAnchor = start;
        nibble = 0;
        followCaret = true;
        modCount++;
    }

    private void deleteAt(int index) {
        if (index < 0 || index >= data.length) {
            return;
        }
        byte[] next = new byte[data.length - 1];
        System.arraycopy(data, 0, next, 0, index);
        System.arraycopy(data, index + 1, next, index, data.length - index - 1);
        data = next;
        modCount++;
        caret = Math.min(caret, data.length);
    }

    private void insertZero() {
        if (data.length >= maxBytes) {
            return;
        }
        byte[] next = new byte[data.length + 1];
        int index = Math.min(caret, data.length);
        System.arraycopy(data, 0, next, 0, index);
        System.arraycopy(data, index, next, index + 1, data.length - index);
        data = next;
        modCount++;
        caret = index;
    }

    private void pasteHex(String clipboard) {
        if (clipboard == null) {
            return;
        }
        String clean = clipboard.replaceAll("[^0-9A-Fa-f]", "");
        if (clean.length() % 2 == 1) {
            clean = clean.substring(0, clean.length() - 1);
        }
        int count = clean.length() / 2;
        if (count == 0) {
            return;
        }
        int index = Math.min(caret, data.length);
        int allowed = Math.max(0, Math.min(count, maxBytes - (data.length - 0)));
        if (allowed <= 0) {
            return;
        }
        byte[] next = new byte[data.length + allowed];
        System.arraycopy(data, 0, next, 0, index);
        for (int i = 0; i < allowed; i++) {
            next[index + i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        }
        System.arraycopy(data, index, next, index + allowed, data.length - index);
        data = next;
        modCount++;
        caret = index + allowed;
    }

    private static String toHex(byte[] data) {
        return toHex(data, 0, data.length);
    }

    private static String toHex(byte[] data, int start, int end) {
        StringBuilder builder = new StringBuilder(Math.max(0, end - start) * 3);
        for (int index = Math.max(0, start); index < Math.min(end, data.length); index++) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(String.format(Locale.ROOT, "%02X", data[index] & 0xFF));
        }
        return builder.toString();
    }

    /* ============================================================
     *  渲染
     * ============================================================ */

    public void render(GuiGraphics graphics, Font font, int x, int y, int width, int height) {
        this.renderX = x;
        this.renderY = y;
        this.renderWidth = width;
        this.renderHeight = height;
        graphics.fill(x, y, x + width, y + height, ProgramStyle.EDITOR_BG);
        this.cellWidth = font.width("00") + 3;
        this.columns = width >= 420 ? 16 : 8;
        this.gridX = x + ADDRESS_WIDTH;
        this.gridY = y + 1;
        this.asciiX = gridX + columns * cellWidth + 6;
        this.visibleRows = Math.max(1, (height - 2) / ROW_HEIGHT);
        int rows = Math.max(1, (data.length + columns - 1) / columns);
        if (followCaret) {
            if (caret / columns < scrollRow) {
                scrollRow = caret / columns;
            } else if (caret / columns >= scrollRow + visibleRows) {
                scrollRow = caret / columns - visibleRows + 1;
            }
            followCaret = false;
        }
        scrollRow = Math.max(0, Math.min(scrollRow, Math.max(0, rows - visibleRows)));

        graphics.fill(x + ADDRESS_WIDTH - 4, y, x + ADDRESS_WIDTH - 3, y + height, 0xFF404040);
        graphics.enableScissor(x, y, x + width, y + height);
        int selectionStart = selectionStart();
        int selectionEnd = selectionEnd();
        long time = Util.getMillis();
        for (int row = 0; row < visibleRows; row++) {
            int rowIndex = scrollRow + row;
            int rowY = gridY + row * ROW_HEIGHT;
            int address = rowIndex * columns;
            if (address >= data.length && rowIndex > 0) {
                break;
            }
            String addressText = String.format(Locale.ROOT, "%04X", address);
            graphics.drawString(font, addressText, x + ADDRESS_WIDTH - 6 - font.width(addressText),
                    rowY + 1, ProgramStyle.LINE_NUMBER, false);
            for (int column = 0; column < columns; column++) {
                int index = address + column;
                if (index >= data.length) {
                    break;
                }
                int cellX = gridX + column * cellWidth;
                int asciiCellX = asciiX + column * font.width("M");
                for (RemoteCursor remoteCursor : remote) {
                    if (index >= remoteCursor.start() && index < remoteCursor.end()) {
                        int color = (remoteCursor.color() & 0x00FFFFFF) | 0x40000000;
                        graphics.fill(cellX - 1, rowY, cellX + cellWidth - 1,
                                rowY + ROW_HEIGHT - 1, color);
                        graphics.fill(asciiCellX, rowY, asciiCellX + font.width("M"),
                                rowY + ROW_HEIGHT - 1, color);
                    }
                }
                boolean selected = index >= selectionStart && index < selectionEnd;
                if (selected) {
                    graphics.fill(cellX - 1, rowY, cellX + cellWidth - 1, rowY + ROW_HEIGHT - 1,
                            0x803A6EC5);
                }
                boolean active = index == caret && !asciiMode;
                if (active) {
                    graphics.fill(cellX - 1, rowY, cellX + cellWidth - 1, rowY + ROW_HEIGHT - 1,
                            ProgramStyle.SELECTION);
                }
                String hex = String.format(Locale.ROOT, "%02X", data[index] & 0xFF);
                graphics.drawString(font, hex, cellX, rowY + 1,
                        active ? ProgramStyle.SELECTED_TEXT : ProgramStyle.EDITOR_TEXT, false);
                if (selected) {
                    graphics.fill(asciiCellX, rowY, asciiCellX + font.width("M"),
                            rowY + ROW_HEIGHT - 1, 0x803A6EC5);
                }
                graphics.drawString(font, String.valueOf(FileText.printableAscii(data[index] & 0xFF)),
                        asciiCellX, rowY + 1,
                        index == caret && asciiMode ? 0xFFFFAA00 : ProgramStyle.LINE_NUMBER, false);
                for (RemoteCursor remoteCursor : remote) {
                    if (remoteCursor.start() == index) {
                        graphics.fill(cellX, rowY, cellX + 1, rowY + ROW_HEIGHT - 1,
                                remoteCursor.color());
                        boolean near = mouseX >= cellX - 12 && mouseX <= cellX + 110
                                && mouseY >= rowY - 10 && mouseY <= rowY + ROW_HEIGHT + 2;
                        if (time - remoteCursor.lastActive() < 2000L || near) {
                            String name = ProgramStyle.fit(font, remoteCursor.name(), 90);
                            int flagWidth = font.width(name) + 4;
                            int flagY = rowY - 8;
                            graphics.fill(cellX, flagY, cellX + flagWidth, flagY + 8,
                                    remoteCursor.color());
                            graphics.drawString(font, name, cellX + 2, flagY, 0xFFFFFFFF, false);
                        }
                    }
                }
            }
        }
        if (focused && (time / 500L) % 2 == 0) {
            int row = caret / columns - scrollRow;
            int column = caret % columns;
            if (row >= 0 && row < visibleRows) {
                int rowY = gridY + row * ROW_HEIGHT;
                if (asciiMode) {
                    int xPos = asciiX + column * font.width("M");
                    graphics.fill(xPos, rowY + 1, xPos + 1, rowY + 1 + font.lineHeight, 0xFFFFFFFF);
                } else {
                    int cellX = gridX + column * cellWidth;
                    int offset = nibble == 0 ? 0 : font.width("0") + 1;
                    graphics.fill(cellX + offset, rowY + 1 + font.lineHeight,
                            cellX + offset + font.width("0"), rowY + 2 + font.lineHeight, 0xFFFFFFFF);
                }
            }
        }
        int totalRows = Math.max(1, (data.length + columns - 1) / columns);
        int maxScroll = Math.max(0, totalRows - visibleRows);
        this.scrollBarX = x + width - 5;
        this.scrollBarY = y + 1;
        this.scrollBarHeight = Math.max(8, height - 2);
        this.scrollBarThumbHeight = Math.max(8,
                scrollBarHeight * Math.min(visibleRows, totalRows) / Math.max(1, totalRows));
        this.scrollBarThumbY = scrollBarY + (maxScroll <= 0 ? 0
                : (scrollBarHeight - scrollBarThumbHeight) * scrollRow / maxScroll);
        if (maxScroll > 0) {
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + scrollBarHeight,
                    0x30000000);
            graphics.fill(scrollBarX, scrollBarThumbY, scrollBarX + 4,
                    scrollBarThumbY + scrollBarThumbHeight, 0xFF808080);
        }
        graphics.disableScissor();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < renderX || mouseX >= renderX + renderWidth
                || mouseY < renderY || mouseY >= renderY + renderHeight) {
            return false;
        }
        if (isOnScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            if (mouseY >= scrollBarThumbY && mouseY < scrollBarThumbY + scrollBarThumbHeight) {
                scrollbarGrabOffset = (int) (mouseY - scrollBarThumbY);
            } else {
                scrollbarGrabOffset = -1;
                scrollToMouse(mouseY);
            }
            return true;
        }
        focused = true;
        int index = caretAt(mouseX, mouseY);
        caret = index;
        selectionAnchor = index;
        nibble = 0;
        draggingSelection = true;
        followCaret = true;
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (draggingScrollbar) {
            scrollToMouse(mouseY);
            followCaret = false;
            return true;
        }
        if (draggingSelection && button == 0) {
            // 拖出上下边界时继续滚动，光标可以停在不可见的位置。
            int rows = Math.max(1, (data.length + columns - 1) / columns);
            if (mouseY < gridY && scrollRow > 0) {
                scrollRow--;
            } else if (mouseY >= gridY + visibleRows * ROW_HEIGHT
                    && scrollRow < Math.max(0, rows - visibleRows)) {
                scrollRow++;
            }
            caret = caretAt(mouseX, mouseY);
            followCaret = false;
            return true;
        }
        return false;
    }

    public void mouseReleased(int button) {
        if (button == 0) {
            draggingSelection = false;
        }
        draggingScrollbar = false;
        scrollbarGrabOffset = -1;
    }

    /** 根据鼠标位置计算字节索引，并更新 ascii 模式。 */
    private int caretAt(double mouseX, double mouseY) {
        int row = (int) ((mouseY - gridY) / ROW_HEIGHT);
        int rowIndex = scrollRow + Math.max(0, row);
        if (mouseX >= asciiX) {
            asciiMode = true;
            int offset = (int) ((mouseX - asciiX) / Math.max(1, Minecraft.getInstance().font.width("M")));
            return Math.min(data.length, rowIndex * columns + Math.max(0, Math.min(columns - 1, offset)));
        }
        if (mouseX >= gridX) {
            asciiMode = false;
            int column = (int) ((mouseX - gridX) / Math.max(1, cellWidth));
            column = Math.max(0, Math.min(columns - 1, column));
            return Math.min(data.length, rowIndex * columns + column);
        }
        return Math.min(data.length, rowIndex * columns);
    }

    private int selectionStart() {
        if (selectionAnchor < 0) {
            return caret;
        }
        return Math.min(caret, selectionAnchor);
    }

    private int selectionEnd() {
        if (selectionAnchor < 0) {
            return caret;
        }
        return Math.max(caret, selectionAnchor);
    }

    private boolean isOnScrollbar(double mouseX, double mouseY) {
        int rows = Math.max(1, (data.length + columns - 1) / columns);
        return rows > visibleRows
                && mouseX >= scrollBarX && mouseX < scrollBarX + 4
                && mouseY >= scrollBarY && mouseY < scrollBarY + scrollBarHeight;
    }

    private void scrollToMouse(double mouseY) {
        int rows = Math.max(1, (data.length + columns - 1) / columns);
        int maxScroll = Math.max(0, rows - visibleRows);
        if (maxScroll <= 0) {
            scrollRow = 0;
            return;
        }
        int track = Math.max(1, scrollBarHeight - scrollBarThumbHeight);
        double position = scrollbarGrabOffset >= 0
                ? mouseY - scrollBarY - scrollbarGrabOffset
                : mouseY - scrollBarY - scrollBarThumbHeight / 2.0;
        double t = Math.max(0.0, Math.min(1.0, position / track));
        scrollRow = (int) Math.round(t * maxScroll);
        followCaret = false;
    }

    public boolean mouseScrolled(double amount) {
        int rows = Math.max(1, (data.length + columns - 1) / columns);
        scrollRow = Math.max(0, Math.min(Math.max(0, rows - visibleRows),
                scrollRow - (int) Math.signum(amount)));
        followCaret = false;
        return true;
    }
}
