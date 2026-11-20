package com.elementsplus.client.gui.program;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * 单行文本输入：搜索框、新建/重命名/另存为对话框共用。
 */
public final class SingleLineInput {
    private String value = "";
    private int caret;
    private int maxLength = 64;
    private boolean focused;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value == null ? "" : value;
        this.caret = this.value.length();
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = Math.max(1, maxLength);
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean isFocused() {
        return focused;
    }

    public void clear() {
        this.value = "";
        this.caret = 0;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean control = net.minecraft.client.gui.screens.Screen.hasControlDown();
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (caret > 0) {
                    value = value.substring(0, caret - 1) + value.substring(caret);
                    caret--;
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (caret < value.length()) {
                    value = value.substring(0, caret) + value.substring(caret + 1);
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                caret = Math.max(0, caret - 1);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                caret = Math.min(value.length(), caret + 1);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                caret = 0;
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                caret = value.length();
                return true;
            }
            default -> {
            }
        }
        if (control && keyCode == GLFW.GLFW_KEY_A) {
            caret = value.length();
            return true;
        }
        if (control && keyCode == GLFW.GLFW_KEY_C) {
            Minecraft.getInstance().keyboardHandler.setClipboard(value);
            return true;
        }
        if (control && (keyCode == GLFW.GLFW_KEY_V || keyCode == GLFW.GLFW_KEY_X)) {
            String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
            insert(clipboard);
            if (keyCode == GLFW.GLFW_KEY_X) {
                Minecraft.getInstance().keyboardHandler.setClipboard("");
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(char c) {
        if (c < 0x20 || c == 0x7F) {
            return false;
        }
        insert(String.valueOf(c));
        return true;
    }

    private void insert(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        StringBuilder builder = new StringBuilder(text);
        for (int i = builder.length() - 1; i >= 0; i--) {
            char c = builder.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                builder.deleteCharAt(i);
            }
        }
        String clean = builder.toString();
        if (clean.isEmpty()) {
            return;
        }
        int room = maxLength - value.length();
        if (room <= 0) {
            return;
        }
        if (clean.length() > room) {
            clean = clean.substring(0, room);
        }
        value = value.substring(0, caret) + clean + value.substring(caret);
        caret += clean.length();
    }

    /** 返回是否点击到了输入框内部。 */
    public boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width, int height) {
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            focused = true;
            caret = value.length();
            return true;
        }
        focused = false;
        return false;
    }

    public void render(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                       boolean hovered, Component hint) {
        ProgramStyle.drawInputField(graphics, font, x, y, width, height, value, caret,
                focused, hint, hovered);
    }

    /** 光标可视位置（用于测试/调试）。 */
    public int caret() {
        return caret;
    }

    public static int clampCaret(int caret, int length) {
        return Mth.clamp(caret, 0, length);
    }
}
