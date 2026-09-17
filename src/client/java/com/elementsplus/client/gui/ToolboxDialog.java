package com.elementsplus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 模态对话框：全屏半透明遮罩 + 主面板窗体。由 Screen 直接渲染与分发输入。
 */
public class ToolboxDialog {
    public enum Mode {RENAME, DELETE_CONFIRM}

    public interface Callback {
        void onOk(ToolboxDialog dialog);

        void onCancel();
    }

    private static final int BTN_W = 44;
    private static final int BTN_H = 18;

    private final Mode mode;
    private final Component title;
    @Nullable
    private final Component message;
    private final Callback callback;

    @Nullable
    private EditBox editBox;

    private final int winX, winY;
    private final int winW;
    private final int winH;
    private int okX, okY;
    private int cancelX, cancelY;

    public ToolboxDialog(Mode mode, Component title, @Nullable Component message, @Nullable String initialValue,
                         Font font, int screenWidth, int screenHeight, Callback callback) {
        this.mode = mode;
        this.title = title;
        this.message = message;
        this.callback = callback;
        this.winW = 220;
        this.winH = mode == Mode.RENAME ? 96 : 88;
        this.winX = (screenWidth - winW) / 2;
        this.winY = (screenHeight - winH) / 2;
        int by = winY + winH - BTN_H - 8;
        this.cancelX = winX + winW - 2 * BTN_W - 12;
        this.okX = winX + winW - BTN_W - 6;
        this.cancelY = by;
        this.okY = by;
        if (mode == Mode.RENAME) {
            this.editBox = new EditBox(font, winX + 20, winY + 40, winW - 40, 16, Component.empty());
            this.editBox.setMaxLength(24);
            String value = initialValue == null ? "" : initialValue;
            this.editBox.setValue(value);
            this.editBox.setCursorPosition(value.length());
            this.editBox.setFocused(true);
        }
    }

    public String getText() {
        return editBox != null ? editBox.getValue() : "";
    }

    public void tick() {
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        GuiUtil.raisePose(g);
        g.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), 0x80000000);
        GuiUtil.drawMainPanel(g, winX, winY, winX + winW, winY + winH);
        g.drawCenteredString(font, title, winX + winW / 2, winY + 10, 0xFFFFFFFF);

        if (mode == Mode.RENAME) {
            if (editBox != null) editBox.render(g, mouseX, mouseY, partialTick);
        } else if (mode == Mode.DELETE_CONFIRM) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(message, winW - 20);
            int ly = winY + 32;
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                g.drawCenteredString(font, line, winX + winW / 2, ly, 0xFFFFFFFF);
                ly += 10;
            }
        }
        renderButton(g, font, cancelX, cancelY, Component.translatable("gui.elements-plus.toolbox.dialog.cancel"), mouseX, mouseY, false);
        renderButton(g, font, okX, okY, Component.translatable("gui.elements-plus.toolbox.dialog.ok"), mouseX, mouseY, mode == Mode.DELETE_CONFIRM);
    }

    private static void renderButton(GuiGraphics g, Font font, int x, int y, Component label, int mouseX, int mouseY, boolean danger) {
        boolean hovered = mouseX >= x && mouseX < x + BTN_W && mouseY >= y && mouseY < y + BTN_H;
        g.fill(x, y, x + BTN_W, y + BTN_H, hovered ? 0xFFFFFFFF : 0xFF000000);
        GuiUtil.drawSubPanel(g, x + 1, y + 1, x + BTN_W - 1, y + BTN_H - 1, danger ? 0xFF800000 : 0xFF808080, GuiUtil.SubPanelType.CONVEX);
        if (hovered) {
            g.fill(x, y, x + BTN_W, y + BTN_H, 0x30FFFFFF);
        }
        g.drawCenteredString(font, label, x + BTN_W / 2, y + (BTN_H - 9) / 2, 0xFFFFFFFF);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        if (editBox != null && inRect(mouseX, mouseY, editBox.getX(), editBox.getY(), editBox.getWidth(), editBox.getHeight())) {
            editBox.setFocused(true);
            return true;
        }
        if (editBox != null) {
            editBox.setFocused(false);
        }
        if (inRect(mouseX, mouseY, cancelX, cancelY, BTN_W, BTN_H)) {
            cancel();
            return true;
        }
        if (inRect(mouseX, mouseY, okX, okY, BTN_W, BTN_H)) {
            if (mode == Mode.RENAME) {
                if (!getText().trim().isEmpty()) {
                    callback.onOk(this);
                }
            } else {
                callback.onOk(this);
            }
            return true;
        }
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editBox != null) {
            if (editBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                if (!getText().trim().isEmpty()) {
                    callback.onOk(this);
                }
                return true;
            }
        }
        return true;
    }

    public boolean charTyped(char c, int modifiers) {
        return editBox != null && editBox.charTyped(c, modifiers);
    }

    public void cancel() {
        callback.onCancel();
    }

    private static boolean inRect(double x, double y, int rx, int ry, int rw, int rh) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }
}