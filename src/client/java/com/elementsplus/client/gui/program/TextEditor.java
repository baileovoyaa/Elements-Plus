package com.elementsplus.client.gui.program;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 多行文本编辑器：左侧行号、选区、撤销外的常用编辑键、协作光标。
 */
public final class TextEditor {
    public static final int ROW_HEIGHT = 10;
    private static final int TAB_SIZE = 4;

    public record RemoteCursor(UUID player, String name, int start, int end, int color,
                               long lastActive) {
    }

    private final StringBuilder content = new StringBuilder();
    private int caret;
    private int anchor;
    private int scrollLine;
    private int scrollX;
    private boolean followCaretX = true;
    private boolean focused;
    private boolean editable = true;
    private int modCount;
    private int maxBytes = Integer.MAX_VALUE;
    private boolean limitReached;
    private List<RemoteCursor> remote = List.of();
    private int highlightLine = -1;
    private int[] lineStarts = new int[]{0};

    // 最近一次渲染的位置，供鼠标换算使用。
    private int renderX;
    private int renderY;
    private int renderWidth;
    private int renderHeight;
    private int textX;
    private int textY;
    private int visibleLines = 1;
    private int scrollBarX;
    private int scrollBarY;
    private int scrollBarHeight;
    private int scrollBarThumbY;
    private int scrollBarThumbHeight;
    private int scrollbarGrabOffset = -1;
    private boolean draggingScrollbar;
    private int hScrollBarX;
    private int hScrollBarY;
    private int hScrollBarWidth;
    private int hScrollBarThumbX;
    private int hScrollBarThumbWidth;
    private int hScrollbarGrabOffset = -1;
    private boolean draggingHScrollbar;
    private double mouseX;
    private double mouseY;

    public void setContent(String text, int caret) {
        content.setLength(0);
        if (text != null) {
            content.append(text);
        }
        rebuildLines();
        this.caret = clampIndex(caret);
        this.anchor = this.caret;
        ensureCaretVisible();
    }

    public String getContent() {
        return content.toString();
    }

    /** 应用远端内容并让本地光标/选区跟随文本流平移，避免前方编辑造成漂移。 */
    public void applyRemoteContent(String text) {
        String before = content.toString();
        String after = text == null ? "" : text;
        int prefix = 0;
        int max = Math.min(before.length(), after.length());
        while (prefix < max && before.charAt(prefix) == after.charAt(prefix)) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < max - prefix
                && before.charAt(before.length() - 1 - suffix)
                == after.charAt(after.length() - 1 - suffix)) {
            suffix++;
        }
        int oldEnd = before.length() - suffix;
        int newEnd = after.length() - suffix;
        this.caret = adjustRemoteIndex(caret, prefix, oldEnd, newEnd, after.length());
        this.anchor = adjustRemoteIndex(anchor, prefix, oldEnd, newEnd, after.length());
        content.setLength(0);
        content.append(after);
        rebuildLines();
        ensureCaretVisible();
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

    public boolean isEmpty() {
        return content.isEmpty();
    }

    public int getCaret() {
        return caret;
    }

    /** 内容修改计数，用于轻量判断按键是否改变了文本。 */
    public int modCount() {
        return modCount;
    }

    public int getSelectionStart() {
        return Math.min(caret, anchor);
    }

    public int getSelectionEnd() {
        return Math.max(caret, anchor);
    }

    public boolean hasSelection() {
        return caret != anchor;
    }

    public int getLineNumber() {
        return lineOf(caret) + 1;
    }

    public int getColumnNumber() {
        return columnOf(caret) + 1;
    }

    public int lineCount() {
        return lineStarts.length;
    }

    public void setMaxBytes(int maxBytes) {
        this.maxBytes = Math.max(0, maxBytes);
    }

    public boolean isLimitReached() {
        return limitReached;
    }

    public void clearLimitFlag() {
        limitReached = false;
    }

    public void setRemote(List<RemoteCursor> remote) {
        this.remote = remote == null ? List.of() : remote;
    }

    public void setHighlightLine(int line) {
        this.highlightLine = line;
    }

    public void focus() {
        this.focused = true;
    }

    public void blur() {
        this.focused = false;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void setCaret(int index) {
        this.caret = clampIndex(index);
        this.anchor = this.caret;
        ensureCaretVisible();
    }

    /* ============================================================
     *  编辑
     * ============================================================ */

    public boolean charTyped(char c) {
        if (!focused || !editable) {
            return false;
        }
        if (c == '\t') {
            return insert("    ");
        }
        if (c < 0x20 && c != '\n') {
            return false;
        }
        return insert(String.valueOf(c));
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) {
            return false;
        }
        boolean control = net.minecraft.client.gui.screens.Screen.hasControlDown();
        boolean shift = net.minecraft.client.gui.screens.Screen.hasShiftDown();
        boolean mutating = keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE
                || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_TAB
                || (control && (keyCode == GLFW.GLFW_KEY_V || keyCode == GLFW.GLFW_KEY_X));
        if (!editable && mutating) {
            return true;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (caret > 0) {
                    int previous = content.codePointBefore(caret);
                    int remove = Character.charCount(previous);
                    replace(caret - remove, caret, "");
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (caret < content.length()) {
                    int next = Character.charCount(content.codePointAt(caret));
                    replace(caret, caret + next, "");
                }
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                return insert("\n");
            }
            case GLFW.GLFW_KEY_TAB -> {
                return insert("    ");
            }
            case GLFW.GLFW_KEY_LEFT -> {
                moveCaret(control ? wordLeft() : previousIndex(caret), shift);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                moveCaret(control ? wordRight() : nextIndex(caret), shift);
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                moveCaret(verticalTarget(-1), shift);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveCaret(verticalTarget(1), shift);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                int line = lineOf(caret);
                moveCaret(control ? 0 : lineStarts[line], shift);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                int line = lineOf(caret);
                moveCaret(control ? content.length() : lineEnd(line), shift);
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_UP -> {
                moveCaret(verticalTarget(-visibleLines), shift);
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_DOWN -> {
                moveCaret(verticalTarget(visibleLines), shift);
                return true;
            }
            default -> {
            }
        }
        if (control && keyCode == GLFW.GLFW_KEY_A) {
            anchor = 0;
            caret = content.length();
            ensureCaretVisible();
            return true;
        }
        if (control && keyCode == GLFW.GLFW_KEY_C) {
            copySelection();
            return true;
        }
        if (control && keyCode == GLFW.GLFW_KEY_X) {
            copySelection();
            deleteSelection();
            return true;
        }
        if (control && keyCode == GLFW.GLFW_KEY_V) {
            return insert(Minecraft.getInstance().keyboardHandler.getClipboard());
        }
        return false;
    }

    private boolean insert(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String clean = text.replace("\r\n", "\n").replace('\r', '\n');
        int start = getSelectionStart();
        int end = getSelectionEnd();
        int newBytes = bytesFor(content, start, end, clean);
        if (newBytes > maxBytes) {
            limitReached = true;
            return true;
        }
        replace(start, end, clean);
        return true;
    }

    private int bytesFor(StringBuilder current, int removeStart, int removeEnd, String insert) {
        String prefix = current.substring(0, removeStart);
        String suffix = current.substring(removeEnd);
        return (prefix + insert + suffix).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }

    private void replace(int start, int end, String text) {
        content.replace(start, end, text);
        modCount++;
        rebuildLines();
        caret = start + text.length();
        anchor = caret;
        ensureCaretVisible();
    }

    private void deleteSelection() {
        if (hasSelection()) {
            replace(getSelectionStart(), getSelectionEnd(), "");
        }
    }

    private void moveCaret(int index, boolean select) {
        caret = clampIndex(index);
        if (!select) {
            anchor = caret;
        }
        ensureCaretVisible();
    }

    private int previousIndex(int index) {
        if (index <= 0) {
            return 0;
        }
        return index - Character.charCount(content.codePointBefore(index));
    }

    private int nextIndex(int index) {
        if (index >= content.length()) {
            return content.length();
        }
        return index + Character.charCount(content.codePointAt(index));
    }

    private int wordLeft() {
        int index = caret;
        while (index > 0 && Character.isWhitespace(content.charAt(index - 1))) {
            index--;
        }
        while (index > 0 && !Character.isWhitespace(content.charAt(index - 1))) {
            index--;
        }
        return index;
    }

    private int wordRight() {
        int index = caret;
        while (index < content.length() && !Character.isWhitespace(content.charAt(index))) {
            index++;
        }
        while (index < content.length() && Character.isWhitespace(content.charAt(index))) {
            index++;
        }
        return index;
    }

    private int verticalTarget(int deltaLines) {
        int line = lineOf(caret);
        int column = caret - lineStarts[line];
        int targetLine = Math.max(0, Math.min(lineStarts.length - 1, line + deltaLines));
        int target = lineStarts[targetLine] + column;
        return Math.min(lineEnd(targetLine), target);
    }

    private int clampIndex(int index) {
        return Math.max(0, Math.min(content.length(), index));
    }

    private int lineEnd(int line) {
        if (line + 1 < lineStarts.length) {
            return lineStarts[line + 1] - 1;
        }
        return content.length();
    }

    private int lineOf(int index) {
        int low = 0;
        int high = lineStarts.length - 1;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (lineStarts[mid] <= index) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    private int columnOf(int index) {
        return index - lineStarts[lineOf(index)];
    }

    private void rebuildLines() {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                starts.add(i + 1);
            }
        }
        lineStarts = new int[starts.size()];
        for (int i = 0; i < starts.size(); i++) {
            lineStarts[i] = starts.get(i);
        }
    }

    private void ensureCaretVisible() {
        int line = lineOf(caret);
        if (line < scrollLine) {
            scrollLine = line;
        } else if (line >= scrollLine + visibleLines) {
            scrollLine = line - visibleLines + 1;
        }
        scrollLine = Math.max(0, Math.min(scrollLine, maxScrollLine()));
        followCaretX = true;
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
        int numberWidth = Math.max(20, font.width(String.valueOf(lineStarts.length)) + 6);
        this.textX = x + numberWidth + 2;
        this.textY = y + 1;
        this.visibleLines = Math.max(1, (height - 6) / ROW_HEIGHT);
        this.scrollLine = Math.max(0, Math.min(scrollLine, maxScrollLine()));
        graphics.fill(x + numberWidth, y, x + numberWidth + 1, y + height, 0xFF404040);
        graphics.enableScissor(x, y, x + width, y + height);

        long time = Util.getMillis();
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();

        // 水平滚动：保证光标在可视区域内；手动滚动后不强制拉回。
        int available = Math.max(10, width - numberWidth - 6);
        int maxScrollX = Math.max(0, maxLineWidth(font) - available);
        int caretLine = lineOf(caret);
        int caretOffset = widthOf(font, lineStarts[caretLine], caret);
        if (followCaretX) {
            if (caretOffset - scrollX > available) {
                scrollX = caretOffset - available;
            } else if (caretOffset < scrollX) {
                scrollX = Math.max(0, caretOffset);
            }
            followCaretX = false;
        }
        scrollX = Math.max(0, Math.min(scrollX, maxScrollX));

        for (int row = 0; row < visibleLines; row++) {
            int line = scrollLine + row;
            if (line >= lineStarts.length) {
                break;
            }
            if (line == highlightLine) {
                int rowY = textY + row * ROW_HEIGHT;
                graphics.fill(x, rowY, x + width, rowY + ROW_HEIGHT, ProgramStyle.ERROR_BG);
            }
        }

        // 文本区域单独裁剪：横向滚动时不会盖住左侧行号。
        graphics.enableScissor(textX, y, x + width, y + height - 4);
        for (int row = 0; row < visibleLines; row++) {
            int line = scrollLine + row;
            if (line >= lineStarts.length) {
                break;
            }
            int rowY = textY + row * ROW_HEIGHT;
            int lineStart = lineStarts[line];
            int lineEnd = lineEnd(line);
            int drawEnd = Math.min(lineEnd, lengthForWidth(font, lineStart, lineEnd, width - numberWidth - 6));
            String text = displayText(content, lineStart, drawEnd);
            graphics.drawString(font, text, textX + 2 - scrollX, rowY + 1, ProgramStyle.EDITOR_TEXT, false);

            if (selectionStart != selectionEnd) {
                int start = Math.max(selectionStart, lineStart);
                int end = Math.min(selectionEnd, lineEnd);
                if (start < end) {
                    int startX = textX + 2 - scrollX + widthOf(font, lineStart, start);
                    int endX = textX + 2 - scrollX + widthOf(font, lineStart, end);
                    graphics.fill(startX, rowY + 1, endX, rowY + 1 + font.lineHeight, 0x803A6EC5);
                }
            }

            for (RemoteCursor cursor : remote) {
                if (cursor.start() < lineStart || cursor.start() > lineEnd) {
                    continue;
                }
                int cursorX = textX + 2 - scrollX + widthOf(font, lineStart, cursor.start());
                if (cursor.start() < cursor.end()) {
                    int selectionFrom = Math.max(cursor.start(), lineStart);
                    int selectionTo = Math.min(cursor.end(), lineEnd);
                    if (selectionFrom < selectionTo) {
                        int fromX = textX + 2 - scrollX + widthOf(font, lineStart, selectionFrom);
                        int toX = textX + 2 - scrollX + widthOf(font, lineStart, selectionTo);
                        graphics.fill(fromX, rowY + 1, toX, rowY + 1 + font.lineHeight,
                                (cursor.color() & 0x00FFFFFF) | 0x40000000);
                    }
                }
                graphics.fill(cursorX, rowY + 1, cursorX + 1, rowY + 1 + font.lineHeight, cursor.color());
                boolean near = mouseX >= cursorX - 12 && mouseX <= cursorX + 110
                        && mouseY >= rowY - 10 && mouseY <= rowY + ROW_HEIGHT + 2;
                if (time - cursor.lastActive() < 2000L || near) {
                    String name = ProgramStyle.fit(font, cursor.name(), 90);
                    int flagWidth = font.width(name) + 4;
                    int flagY = rowY - 8;
                    graphics.fill(cursorX, flagY, cursorX + flagWidth, flagY + 8, cursor.color());
                    graphics.drawString(font, name, cursorX + 2, flagY, 0xFFFFFFFF, false);
                }
            }
        }
        graphics.disableScissor();

        for (int row = 0; row < visibleLines; row++) {
            int line = scrollLine + row;
            if (line >= lineStarts.length) {
                break;
            }
            int rowY = textY + row * ROW_HEIGHT;
            String number = String.valueOf(line + 1);
            graphics.drawString(font, number, x + numberWidth - 2 - font.width(number), rowY + 1,
                    ProgramStyle.LINE_NUMBER, false);
        }

        if (focused && (time / 500L) % 2 == 0) {
            int line = lineOf(caret);
            int row = line - scrollLine;
            if (row >= 0 && row < visibleLines) {
                int rowY = textY + row * ROW_HEIGHT;
                int caretX = textX + 2 - scrollX + widthOf(font, lineStarts[line], caret);
                if (caretX >= textX && caretX < x + width) {
                    graphics.fill(caretX, rowY + 1, caretX + 1, rowY + 1 + font.lineHeight, 0xFFFFFFFF);
                }
            }
        }
        int maxScrollLine = maxScrollLine();
        this.scrollBarX = x + width - 5;
        this.scrollBarY = y + 1;
        this.scrollBarHeight = Math.max(8, height - 7);
        this.scrollBarThumbHeight = Math.max(8,
                scrollBarHeight * Math.min(visibleLines, lineStarts.length)
                        / Math.max(1, lineStarts.length));
        this.scrollBarThumbY = scrollBarY + (maxScrollLine <= 0 ? 0
                : (scrollBarHeight - scrollBarThumbHeight) * scrollLine / maxScrollLine);
        if (maxScrollLine > 0) {
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 4,
                    scrollBarY + scrollBarHeight, 0x30000000);
            graphics.fill(scrollBarX, scrollBarThumbY, scrollBarX + 4,
                    scrollBarThumbY + scrollBarThumbHeight, 0xFF808080);
        }
        this.hScrollBarX = textX;
        this.hScrollBarY = y + height - 4;
        this.hScrollBarWidth = Math.max(10, available);
        this.hScrollBarThumbWidth = maxScrollX <= 0 ? hScrollBarWidth
                : Math.max(12, hScrollBarWidth * available / Math.max(1, available + maxScrollX));
        this.hScrollBarThumbX = hScrollBarX + (maxScrollX <= 0 ? 0
                : (hScrollBarWidth - hScrollBarThumbWidth) * scrollX / maxScrollX);
        if (maxScrollX > 0) {
            graphics.fill(hScrollBarX, hScrollBarY, hScrollBarX + hScrollBarWidth,
                    hScrollBarY + 3, 0x30000000);
            graphics.fill(hScrollBarThumbX, hScrollBarY,
                    hScrollBarThumbX + hScrollBarThumbWidth, hScrollBarY + 3, 0xFF808080);
        }
        graphics.disableScissor();
    }

    public void setMouse(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    private int maxScrollLine() {
        return Math.max(0, lineStarts.length - visibleLines);
    }

    private static String displayText(CharSequence text, int start, int end) {
        StringBuilder builder = new StringBuilder(end - start);
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            if (c == '\t') {
                builder.append("    ");
            } else if (c >= ' ') {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private int widthOf(Font font, int lineStart, int index) {
        return font.width(displayText(content, lineStart, index));
    }

    private int lengthForWidth(Font font, int start, int end, int maxWidth) {
        int width = 0;
        int index = start;
        while (index < end) {
            char c = content.charAt(index);
            int charWidth = font.width(c == '\t' ? "    " : String.valueOf(c));
            if (width + charWidth > maxWidth + scrollX) {
                break;
            }
            width += charWidth;
            index++;
        }
        return index;
    }

    /* ============================================================
     *  鼠标
     * ============================================================ */

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!inside(mouseX, mouseY)) {
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
        if (isOnHScrollbar(mouseX, mouseY)) {
            draggingHScrollbar = true;
            if (mouseX >= hScrollBarThumbX
                    && mouseX < hScrollBarThumbX + hScrollBarThumbWidth) {
                hScrollbarGrabOffset = (int) (mouseX - hScrollBarThumbX);
            } else {
                hScrollbarGrabOffset = -1;
                scrollToMouseX(mouseX);
            }
            return true;
        }
        focused = true;
        int index = indexAt(mouseX, mouseY);
        caret = index;
        anchor = index;
        followCaretX = true;
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (draggingScrollbar) {
            scrollToMouse(mouseY);
            return true;
        }
        if (draggingHScrollbar) {
            scrollToMouseX(mouseX);
            return true;
        }
        if (!focused || button != 0) {
            return false;
        }
        // 拖出上下边界时继续滚动，支持向上/向下扩展选择。
        if (mouseY < textY && scrollLine > 0) {
            scrollLine--;
        } else if (mouseY >= textY + visibleLines * ROW_HEIGHT
                && scrollLine < maxScrollLine()) {
            scrollLine++;
        }
        caret = indexAt(mouseX, mouseY);
        followCaretX = true;
        return true;
    }

    public void mouseReleased(int button) {
        if (button == 0) {
            draggingScrollbar = false;
            draggingHScrollbar = false;
            scrollbarGrabOffset = -1;
            hScrollbarGrabOffset = -1;
        }
        // 选区保留；点击时 anchor 已设置。
    }

    private boolean isOnHScrollbar(double mouseX, double mouseY) {
        return maxScrollXValue() > 0
                && mouseX >= hScrollBarX && mouseX < hScrollBarX + hScrollBarWidth
                && mouseY >= hScrollBarY && mouseY < hScrollBarY + 4;
    }

    private int maxScrollXValue() {
        if (renderWidth <= 0) {
            return 0;
        }
        int numberWidth = Math.max(20, Minecraft.getInstance().font.width(
                String.valueOf(lineStarts.length)) + 6);
        int available = Math.max(10, renderWidth - numberWidth - 6);
        return Math.max(0, maxLineWidth(Minecraft.getInstance().font) - available);
    }

    private void scrollToMouseX(double mouseX) {
        int maxScroll = maxScrollXValue();
        if (maxScroll <= 0) {
            scrollX = 0;
            return;
        }
        int track = Math.max(1, hScrollBarWidth - hScrollBarThumbWidth);
        double position = hScrollbarGrabOffset >= 0
                ? mouseX - hScrollBarX - hScrollbarGrabOffset
                : mouseX - hScrollBarX - hScrollBarThumbWidth / 2.0;
        double t = Math.max(0.0, Math.min(1.0, position / track));
        scrollX = (int) Math.round(t * maxScroll);
        followCaretX = false;
    }

    private boolean isOnScrollbar(double mouseX, double mouseY) {
        return maxScrollLine() > 0
                && mouseX >= scrollBarX && mouseX < scrollBarX + 4
                && mouseY >= scrollBarY && mouseY < scrollBarY + scrollBarHeight;
    }

    private void scrollToMouse(double mouseY) {
        int maxScroll = maxScrollLine();
        if (maxScroll <= 0) {
            scrollLine = 0;
            return;
        }
        int track = Math.max(1, scrollBarHeight - scrollBarThumbHeight);
        double position = scrollbarGrabOffset >= 0
                ? mouseY - scrollBarY - scrollbarGrabOffset
                : mouseY - scrollBarY - scrollBarThumbHeight / 2.0;
        double t = Math.max(0.0, Math.min(1.0, position / track));
        scrollLine = (int) Math.round(t * maxScroll);
        followCaretX = false;
    }

    private boolean inside(double mouseX, double mouseY) {
        return mouseX >= renderX && mouseX < renderX + renderWidth
                && mouseY >= renderY && mouseY < renderY + renderHeight;
    }

    private int indexAt(double mouseX, double mouseY) {
        int row = (int) ((mouseY - textY) / ROW_HEIGHT);
        int line = Math.max(0, Math.min(lineStarts.length - 1, scrollLine + row));
        int lineStart = lineStarts[line];
        int lineEnd = lineEnd(line);
        int offsetX = (int) (mouseX - textX - 2 + scrollX);
        if (offsetX <= 0) {
            return lineStart;
        }
        int width = 0;
        for (int index = lineStart; index < lineEnd; index++) {
            char c = content.charAt(index);
            int charWidth = Minecraft.getInstance().font.width(
                    c == '\t' ? "    " : String.valueOf(c));
            if (width + charWidth / 2 >= offsetX) {
                return index;
            }
            width += charWidth;
        }
        return lineEnd;
    }

    public boolean mouseScrolled(double mouseY, double horizontalAmount, double verticalAmount) {
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()
                || Math.abs(horizontalAmount) > 0.001) {
            int numberWidth = Math.max(20, Minecraft.getInstance().font.width(
                    String.valueOf(lineStarts.length)) + 6);
            int available = Math.max(10, renderWidth - numberWidth - 6);
            int maxScrollX = Math.max(0, maxLineWidth(Minecraft.getInstance().font) - available);
            double delta = Math.abs(horizontalAmount) > 0.001 ? horizontalAmount : -verticalAmount;
            scrollX = Math.max(0, Math.min(maxScrollX,
                    scrollX + (int) Math.round(delta * 12)));
            followCaretX = false;
            return true;
        }
        scrollLine = Math.max(0, Math.min(maxScrollLine(),
                scrollLine - (int) Math.signum(verticalAmount)));
        return true;
    }

    private int maxLineWidth(Font font) {
        int max = 0;
        for (int line = 0; line < lineStarts.length; line++) {
            max = Math.max(max, font.width(displayText(content, lineStarts[line], lineEnd(line))));
        }
        return max;
    }

    private void copySelection() {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        if (start == end) {
            return;
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(content.substring(start, end));
    }

    /** 让外部在渲染前写入内容已不再需要；保留空实现避免调用方遗漏。 */
    public void prepareRender() {
    }

    public Component lineContent(int line) {
        if (line < 0 || line >= lineStarts.length) {
            return Component.empty();
        }
        return Component.literal(content.substring(lineStarts[line], lineEnd(line)));
    }

    public String text() {
        return content.toString();
    }

    /** 输入法组合输入期间仍接受字符；由 Screen.charTyped 转发。 */
    public static boolean isPrintable(char c) {
        return c >= 0x20 || c == '\n' || c == '\t';
    }

    public static int keypadEnter() {
        return InputConstants.KEY_RETURN;
    }
}
