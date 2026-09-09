package com.elementsplus.client.screen;

import net.minecraft.client.gui.GuiGraphics;

public class GuiUtil {
    public static class FillContext {
        GuiGraphics guiGraphics;
        public int x1, y1, x2, y2;

        public FillContext(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2) {
            this.guiGraphics = guiGraphics;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2 + 1;
            this.y2 = y2 + 1;
        }

        public void fill(int color) {
            guiGraphics.fill(x1, y1, x2 - 1, y2 - 1, color);
        }

        public void fillRelative(int rx1, int ry1, int rx2, int ry2, int color) {
            int nx1 = (rx1 < 0) ? x2 + rx1 : x1 + rx1;
            int ny1 = (ry1 < 0) ? y2 + ry1 : y1 + ry1;
            int nx2 = (rx2 < 0) ? x2 + rx2 : x1 + rx2;
            int ny2 = (ry2 < 0) ? y2 + ry2 : y1 + ry2;
            guiGraphics.fill(nx1, ny1, nx2, ny2, color);
        }
    }

    public static int multiplyColor(int color1, int color2) {
        // 1. 提取各通道（含Alpha）
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        // 2. 正片叠底公式（这里除法在乘法之后，但数值已控制在0-255，不会溢出）
        int r = (r1 * r2) / 255;
        int g = (g1 * g2) / 255;
        int b = (b1 * b2) / 255;

        // 3. 保留color1的Alpha（也可以按需求设为 0xFF 不透明）
        int a = a1;

        // 4. 重新组装
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void drawMainPanel(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2) {
        drawMainPanel(guiGraphics, x1, y1, x2, y2, 0xFFFFFFFF);
    }

    public static void drawMainPanel(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int colorMultiply) {

        FillContext fillContext = new FillContext(guiGraphics, x1, y1, x2, y2);

        int color1 = multiplyColor(0xFFC6C6C6, colorMultiply);
        int color2 = multiplyColor(0xFF555555, colorMultiply);

        // 内部背景
        fillContext.fillRelative(4, 4, -5, -5, color1);

        // 黑色边框
        fillContext.fillRelative(2, 0, -4, 1, 0xFF000000);
        fillContext.fillRelative(1, 1, 2, 2, 0xFF000000);
        fillContext.fillRelative(0, 2, 1, -4, 0xFF000000);
        fillContext.fillRelative(1, -4, 2, -3, 0xFF000000);
        fillContext.fillRelative(2, -3, 3, -2, 0xFF000000);
        fillContext.fillRelative(3, -2, -3, -1, 0xFF000000);
        fillContext.fillRelative(-4, 1, -3, 2, 0xFF000000);
        fillContext.fillRelative(-3, 2, -2, 3, 0xFF000000);
        fillContext.fillRelative(-2, 3, -1, -3, 0xFF000000);
        fillContext.fillRelative(-3, -3, -2, -2, 0xFF000000);

        // 内边框
        fillContext.fillRelative(2, 1, -4, 3, colorMultiply);
        fillContext.fillRelative(3, 3, 4, 4, colorMultiply);
        fillContext.fillRelative(1, 2, 3, -4, colorMultiply);
        fillContext.fillRelative(4, 3, -4, 4, color1);
        fillContext.fillRelative(3, 4, 4, -4, color1);
        fillContext.fillRelative(3, -5, -5, -4, color1);
        fillContext.fillRelative(-5, 3, -4, -5, color1);
        fillContext.fillRelative(2, -4, 3, -3, color1);
        fillContext.fillRelative(-4, 2, -3, 3, color1);
        fillContext.fillRelative(-4, 3, -2, -3, color2);
        fillContext.fillRelative(3, -4, -3, -2, color2);
        fillContext.fillRelative(-5, -5, -4, -4, color2);
    }
}
