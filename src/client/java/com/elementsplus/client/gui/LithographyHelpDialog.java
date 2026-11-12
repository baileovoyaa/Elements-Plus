package com.elementsplus.client.gui;

import com.elementsplus.ElementsPlus;
import com.elementsplus.client.ClientResourceHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 光刻机帮助页：黑色模态面板、图片画廊和文档链接。
 */
public class LithographyHelpDialog {

    private static final String DOCS_URL =
            "https://baileovoyaa.github.io/Elements-Plus/user/ui/lithography-machine.html";

    private static final List<ResourceLocation> GALLERY = List.of(
            ElementsPlus.id("textures/gui/lithography_machine/gallery/intro.png"),
            ElementsPlus.id("textures/gui/lithography_machine/gallery/put_circuit.png"),
            ElementsPlus.id("textures/gui/lithography_machine/gallery/to_be_continued.png")
    );

    private static final int MARGIN = 12;
    private static final int TITLE_HEIGHT = 20;
    private static final int NAV_WIDTH = 22;
    private static final int BUTTON_HEIGHT = 18;

    private final Map<ResourceLocation, ResourceLocation> localizedTextures = new HashMap<>();
    private final Map<ResourceLocation, int[]> rawTextureSizes = new HashMap<>();
    private final int winX;
    private final int winY;
    private final int winW;
    private final int winH;
    private final int imageX;
    private final int imageY;
    private final int imageW;
    private final int imageH;
    private final int leftX;
    private final int leftY;
    private final int rightX;
    private final int rightY;
    private final int docsX;
    private final int docsY;
    private final int docsW;
    private final int closeX;
    private final int closeY;
    private final int closeW;
    private final Runnable onClose;

    private int page;

    public LithographyHelpDialog(Font font, int screenWidth, int screenHeight, Runnable onClose) {
        this.onClose = onClose;

        this.winW = Math.min(Math.max(260, screenWidth - 20), 560);
        this.winH = Math.min(Math.max(200, screenHeight - 20), 420);
        this.winX = (screenWidth - winW) / 2;
        this.winY = (screenHeight - winH) / 2;

        this.imageX = winX + MARGIN + NAV_WIDTH;
        this.imageY = winY + TITLE_HEIGHT;
        this.imageW = Math.max(1, winW - 2 * (MARGIN + NAV_WIDTH));
        this.imageH = Math.max(1, winH - TITLE_HEIGHT - BUTTON_HEIGHT - 2 * MARGIN);

        this.leftX = winX + MARGIN;
        this.leftY = imageY + (imageH - BUTTON_HEIGHT) / 2;
        this.rightX = winX + winW - MARGIN - NAV_WIDTH;
        this.rightY = leftY;

        Component docs = Component.translatable("gui.elements-plus.lithography_machine.help_docs");
        Component close = Component.translatable("gui.elements-plus.lithography_machine.help_close");
        this.docsW = Math.max(80, font.width(docs) + 12);
        this.closeW = Math.max(44, font.width(close) + 12);
        int buttonGap = 8;
        int buttonTotal = docsW + buttonGap + closeW;
        this.docsX = winX + (winW - buttonTotal) / 2;
        this.docsY = winY + winH - MARGIN - BUTTON_HEIGHT;
        this.closeX = docsX + docsW + buttonGap;
        this.closeY = docsY;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        GuiUtil.raisePose(g);
        g.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), 0x80000000);
        g.fill(winX, winY, winX + winW, winY + winH, 0xFF000000);

        // 简单的对话框边框
        g.fill(winX, winY, winX + winW, winY + 1, 0xFF505050);
        g.fill(winX, winY + winH - 1, winX + winW, winY + winH, 0xFF505050);
        g.fill(winX, winY, winX + 1, winY + winH, 0xFF505050);
        g.fill(winX + winW - 1, winY, winX + winW, winY + winH, 0xFF505050);

        g.drawCenteredString(font, Component.translatable("gui.elements-plus.lithography_machine.help_title"),
                winX + winW / 2, winY + 6, 0xFFFFFFFF);

        drawGallery(g, font);
        drawNavButton(g, font, leftX, leftY, "<", mouseX, mouseY);
        drawNavButton(g, font, rightX, rightY, ">", mouseX, mouseY);
        drawButton(g, font, docsX, docsY, docsW, BUTTON_HEIGHT,
                Component.translatable("gui.elements-plus.lithography_machine.help_docs"), mouseX, mouseY, 0xFF2266DD);
        drawButton(g, font, closeX, closeY, closeW, BUTTON_HEIGHT,
                Component.translatable("gui.elements-plus.lithography_machine.help_close"), mouseX, mouseY, 0xFF808080);

        String pageText = (page + 1) + "/" + GALLERY.size();
        g.drawCenteredString(font, Component.literal(pageText),
                winX + winW / 2, winY + winH - MARGIN - BUTTON_HEIGHT - 12, 0xFFA0A0A0);
    }

    private void drawGallery(GuiGraphics g, Font font) {
        ResourceLocation base = GALLERY.get(page);
        ResourceLocation texture = localizedTextures.computeIfAbsent(base, ClientResourceHelper::getLocalizedTexture);
        int[] raw = rawTextureSizes.computeIfAbsent(texture, ClientResourceHelper::getRawTextureSize);
        if (raw == null || raw[0] <= 0 || raw[1] <= 0) {
            g.drawCenteredString(font, Component.literal("?"),
                    imageX + imageW / 2, imageY + imageH / 2, 0xFFFF5555);
            return;
        }
        double scale = Math.min(imageW / (double) raw[0], imageH / (double) raw[1]);
        int drawW = Math.max(1, (int) Math.round(raw[0] * scale));
        int drawH = Math.max(1, (int) Math.round(raw[1] * scale));
        int x = imageX + (imageW - drawW) / 2;
        int y = imageY + (imageH - drawH) / 2;
        g.blit(texture, x, y, drawW, drawH, 0.0F, 0.0F, raw[0], raw[1], raw[0], raw[1]);
    }

    private static void drawNavButton(GuiGraphics g, Font font, int x, int y, String text,
                                      int mouseX, int mouseY) {
        boolean hovered = inRect(mouseX, mouseY, x, y, NAV_WIDTH, BUTTON_HEIGHT);
        g.fill(x, y, x + NAV_WIDTH, y + BUTTON_HEIGHT, hovered ? 0xFFFFFFFF : 0xFF000000);
        GuiUtil.drawSubPanel(g, x + 1, y + 1, x + NAV_WIDTH - 1, y + BUTTON_HEIGHT - 1,
                0xFF808080, GuiUtil.SubPanelType.CONVEX);
        g.drawCenteredString(font, Component.literal(text),
                x + NAV_WIDTH / 2, y + (BUTTON_HEIGHT - 9) / 2, 0xFFFFFFFF);
    }

    private static void drawButton(GuiGraphics g, Font font, int x, int y, int width, int height,
                                   Component label, int mouseX, int mouseY, int color) {
        boolean hovered = inRect(mouseX, mouseY, x, y, width, height);
        g.fill(x, y, x + width, y + height, hovered ? 0xFFFFFFFF : 0xFF000000);
        GuiUtil.drawSubPanel(g, x + 1, y + 1, x + width - 1, y + height - 1,
                color, GuiUtil.SubPanelType.CONVEX);
        g.drawCenteredString(font, label, x + width / 2, y + (height - 9) / 2, 0xFFFFFFFF);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return true;
        }
        if (inRect(mouseX, mouseY, leftX, leftY, NAV_WIDTH, BUTTON_HEIGHT)) {
            page = (page + GALLERY.size() - 1) % GALLERY.size();
            return true;
        }
        if (inRect(mouseX, mouseY, rightX, rightY, NAV_WIDTH, BUTTON_HEIGHT)) {
            page = (page + 1) % GALLERY.size();
            return true;
        }
        if (inRect(mouseX, mouseY, docsX, docsY, docsW, BUTTON_HEIGHT)) {
            ConfirmLinkScreen.confirmLinkNow(Minecraft.getInstance().screen, DOCS_URL);
            return true;
        }
        if (inRect(mouseX, mouseY, closeX, closeY, closeW, BUTTON_HEIGHT)) {
            onClose.run();
            return true;
        }
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            // ESC 关闭帮助页与点击“关闭”按钮等价，同样视为已经看过帮助。
            onClose.run();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            page = (page + GALLERY.size() - 1) % GALLERY.size();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            page = (page + 1) % GALLERY.size();
            return true;
        }
        return true;
    }

    public boolean charTyped(char c, int modifiers) {
        return true;
    }

    private static boolean inRect(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
