package com.elementsplus.client.gui.program;

import com.elementsplus.ElementsPlus;
import com.elementsplus.client.gui.GuiUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 编程页的配色与通用绘制方法。图标统一为 16x16 的 GUI 贴图。
 */
public final class ProgramStyle {
    public static final int TEXT = 0xFF202020;
    public static final int MUTED = 0xFF606060;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int SELECTION = 0xFF3A6EC5;
    public static final int SELECTED_TEXT = 0xFFFFFFFF;
    public static final int HOVER = 0x30FFFFFF;
    public static final int EDITOR_BG = 0xFF202020;
    public static final int EDITOR_TEXT = 0xFFE0E0E0;
    public static final int LINE_NUMBER = 0xFF808080;
    public static final int ACTIVE_EDITOR = 0xFF40C060;
    public static final int ERROR_BG = 0x80A00000;
    public static final int ERROR_TEXT = 0xFFFF5555;
    public static final int WARN_TEXT = 0xFFFFAA00;
    public static final int HEADER_BG = 0xFFB0B0B0;
    public static final int STATUS_BG = 0xFFB0B0B0;
    public static final int FIELD_BG = 0xFFF0F0F0;

    /** 工具栏图标。 */
    public static ResourceLocation icon(String name) {
        return ElementsPlus.id("textures/gui/file_system/" + name + ".png");
    }

    private ProgramStyle() {
    }

    public static void drawPanel(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        GuiUtil.drawSubPanel(graphics, x1, y1, x2, y2, 0xFFD0D0D0, GuiUtil.SubPanelType.BORDERED);
    }

    public static void drawInputField(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                      String value, int caret, boolean focused, Component hint, boolean hovered) {
        int border = focused ? 0xFF000000 : (hovered ? 0xFF404040 : 0xFF808080);
        graphics.fill(x, y, x + width, y + height, border);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, FIELD_BG);
        int textY = y + (height - font.lineHeight) / 2;
        String shown = value == null ? "" : value;
        if (shown.isEmpty() && hint != null) {
            graphics.drawString(font, hint, x + 3, textY, 0xFF909090, false);
            if (focused && (net.minecraft.Util.getMillis() / 500L) % 2 == 0) {
                graphics.fill(x + 3, textY - 1, x + 4, textY + font.lineHeight, TEXT);
            }
        } else {
            int room = width - 6;
            int offset = 0;
            if (focused) {
                int caretWidth = font.width(shown.substring(0, Math.min(caret, shown.length())));
                if (caretWidth > room) {
                    offset = caretWidth - room;
                }
            }
            graphics.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);
            graphics.drawString(font, shown, x + 3 - offset, textY, TEXT, false);
            if (focused && (net.minecraft.Util.getMillis() / 500L) % 2 == 0) {
                int caretX = x + 3 - offset + font.width(shown.substring(0, Math.min(caret, shown.length())));
                graphics.fill(caretX, textY - 1, caretX + 1, textY + font.lineHeight, TEXT);
            }
            graphics.disableScissor();
        }
    }

    /** 带边框的普通文字按钮；返回是否悬停。 */
    public static boolean drawButton(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                     Component label, int mouseX, int mouseY, boolean enabled, int color) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        if (hovered) {
            graphics.fill(x, y, x + width, y + height, 0xFFFFFFFF);
        } else {
            graphics.fill(x, y, x + width, y + height, 0xFF000000);
        }
        GuiUtil.drawSubPanel(graphics, x + 1, y + 1, x + width - 1, y + height - 1,
                enabled ? color : 0xFF505050,
                GuiUtil.SubPanelType.CONVEX);
        if (hovered && enabled) {
            graphics.fill(x, y, x + width, y + height, HOVER);
        }
        graphics.drawCenteredString(font, label, x + width / 2, y + (height - font.lineHeight) / 2 + 1,
                enabled ? WHITE : 0xFF808080);
        return hovered;
    }

    /** 16x16 图标按钮；返回是否悬停。 */
    public static boolean drawIconButton(GuiGraphics graphics, int x, int y, int size,
                                         ResourceLocation icon, int mouseX, int mouseY,
                                         boolean enabled, boolean active) {
        boolean hovered = mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
        if (active) {
            graphics.fill(x, y, x + size, y + size, 0x8040C060);
        }
        if (icon != null) {
            if (enabled) {
                graphics.blit(icon, x, y, 0, 0, size, size, size, size);
            } else {
                // 不可用按钮显示半透明图标，而不是灰色纯色块。
                RenderSystem.enableBlend();
                graphics.setColor(1.0F, 1.0F, 1.0F, 0.3F);
                graphics.blit(icon, x, y, 0, 0, size, size, size, size);
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.disableBlend();
            }
        }
        if (enabled && hovered) {
            graphics.fill(x, y, x + size, y + size, HOVER);
        }
        return hovered;
    }

    /** 文件夹/文件行前面的小方块图标（没有贴图时的占位）。 */
    public static void drawEntryIcon(GuiGraphics graphics, int x, int y, boolean folder) {
        if (folder) {
            graphics.blit(ElementsPlus.id("textures/gui/file_system/folder.png"), x + 2, y, 0, 0, 8, 8, 8, 8);
        } else {
            graphics.blit(ElementsPlus.id("textures/gui/file_system/file.png"), x + 2, y, 0, 0, 8, 8, 8, 8);
        }
    }

    public static void drawStatusBar(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                     Component left, Component right, int fill, int fillColor) {
        graphics.fill(x, y, x + width, y + height, STATUS_BG);
        graphics.fill(x, y, x + width, y + 1, 0xFF808080);
        if (fill > 0 && width > 4) {
            int barWidth = Math.max(0, Math.min(width - 2, (width - 2) * fill / 100));
            graphics.fill(x + 1, y + height - 3, x + 1 + barWidth, y + height - 1, fillColor);
        }
        if (left != null) {
            GuiUtil.drawTruncatedText(graphics, font, left, x + 2, y + 2,
                    Math.max(1, width - (right == null ? 4 : font.width(right) + 10)), TEXT);
        }
        if (right != null) {
            graphics.drawString(font, right, x + width - 2 - font.width(right), y + 2, MUTED, false);
        }
    }

    /** 将文本裁剪到指定宽度。 */
    public static String fit(Font font, String text, int width) {
        if (text == null || width <= 0) {
            return "";
        }
        if (font.width(text) <= width) {
            return text;
        }
        if (width <= font.width("...")) {
            return font.plainSubstrByWidth(text, width);
        }
        return font.plainSubstrByWidth(text, width - font.width("...")) + "...";
    }

    /** 玩家光标/旗帜颜色：按 UUID 稳定取色。 */
    public static int cursorColor(java.util.UUID uuid) {
        int[] colors = {
                0xFFFF2BD6, 0xFFFF7300, 0xFF008CFF, 0xFF00A884,
                0xFF7C4DFF, 0xFFE83E8C, 0xFFC58B00
        };
        return colors[Math.floorMod(uuid.hashCode(), colors.length)];
    }
}
