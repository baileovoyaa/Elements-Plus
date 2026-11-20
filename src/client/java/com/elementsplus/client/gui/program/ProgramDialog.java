package com.elementsplus.client.gui.program;

import com.elementsplus.client.gui.GuiUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 编程页的模态对话框：消息、确认、输入、多按钮选择共用。
 */
public final class ProgramDialog {
    public static final int BUTTON_HEIGHT = 18;
    private static final int MARGIN = 10;

    public record Button(Component label, Runnable action) {
    }

    private final Component title;
    private final List<Component> message;
    private final List<Button> buttons = new ArrayList<>();
    private final List<Integer> buttonWidths = new ArrayList<>();
    private final SingleLineInput input;
    private final Component inputHint;
    private final boolean danger;
    private int winX, winY, winW, winH;
    private int buttonY;
    private final List<int[]> buttonBounds = new ArrayList<>();
    private int enterButtonIndex = -1;
    private Component inlineError;
    private Runnable onCancel;

    public ProgramDialog(Component title, Component message) {
        this(title, message, false, null);
    }

    public ProgramDialog(Component title, Component message, boolean danger, SingleLineInput input) {
        this(title, message, danger, input, null);
    }

    public ProgramDialog(Component title, Component message, boolean danger, SingleLineInput input,
                         Component inputHint) {
        this(title, message == null ? List.of() : List.of(message), danger, input, inputHint);
    }

    public ProgramDialog(Component title, List<Component> message, boolean danger,
                         SingleLineInput input, Component inputHint) {
        this.title = title;
        this.message = message == null ? List.of() : message;
        this.danger = danger;
        this.input = input;
        this.inputHint = inputHint;
    }

    public ProgramDialog button(Component label, Runnable action) {
        buttons.add(new Button(label, action));
        return this;
    }

    public ProgramDialog cancel(Component label) {
        return button(label, this::close);
    }

    public ProgramDialog onCancel(Runnable onCancel) {
        this.onCancel = onCancel;
        return this;
    }

    /** 让当前最后一个按钮响应 Enter（用于删除确认等无输入框对话框）。 */
    public ProgramDialog confirmOnEnter() {
        this.enterButtonIndex = buttons.size() - 1;
        return this;
    }

    /** 在输入框与按钮之间显示校验错误。 */
    public void setInlineError(Component error) {
        this.inlineError = error;
        layout(Minecraft.getInstance().font,
                Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                Minecraft.getInstance().getWindow().getGuiScaledHeight());
    }

    public void clearInlineError() {
        if (inlineError != null) {
            setInlineError(null);
        }
    }

    public SingleLineInput input() {
        return input;
    }

    public void close() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    public void layout(Font font, int screenWidth, int screenHeight) {
        int textWidth = 0;
        for (Component line : message) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        if (inlineError != null) {
            textWidth = Math.max(textWidth, font.width(inlineError));
        }
        int buttonTotal = 0;
        buttonWidths.clear();
        for (Button button : buttons) {
            int buttonWidth = Math.max(52, font.width(button.label()) + 12);
            buttonWidths.add(buttonWidth);
            buttonTotal += buttonWidth;
        }
        buttonTotal += Math.max(0, buttons.size() - 1) * 6;
        int width = Math.max(180, Math.min(screenWidth - 20,
                Math.max(Math.max(textWidth, 150) + MARGIN * 2, buttonTotal + MARGIN * 2)));
        if (input != null) {
            width = Math.max(width, 220);
        }
        this.winW = width;
        int errorHeight = inlineError == null ? 0 : wrappedHeight(font, width, inlineError) + 2;
        this.winH = 30 + wrappedHeight(font, width)
                + (input != null ? 26 : 0) + errorHeight + 30;
        this.winX = (screenWidth - winW) / 2;
        this.winY = (screenHeight - winH) / 2;
        this.buttonY = winY + winH - BUTTON_HEIGHT - 8;
        buttonBounds.clear();
        int total = 0;
        for (int buttonWidth : buttonWidths) {
            total += buttonWidth;
        }
        total += Math.max(0, buttons.size() - 1) * 6;
        int x = winX + winW - MARGIN - total;
        for (int i = 0; i < buttons.size(); i++) {
            int buttonWidth = buttonWidths.get(i);
            buttonBounds.add(new int[]{x, buttonY, buttonWidth, BUTTON_HEIGHT});
            x += buttonWidth + 6;
        }
        if (input != null) {
            input.setFocused(true);
        }
    }

    private int wrappedHeight(Font font, int width) {
        int height = 0;
        for (Component line : message) {
            height += font.split(line, width - MARGIN * 2).size() * (font.lineHeight + 2);
        }
        return Math.max(font.lineHeight + 2, height);
    }

    private int wrappedHeight(Font font, int width, Component line) {
        if (line == null) {
            return 0;
        }
        return Math.max(font.lineHeight + 2,
                font.split(line, width - MARGIN * 2).size() * (font.lineHeight + 2));
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        GuiUtil.raisePose(graphics);
        graphics.fill(0, 0, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(), 0x80000000);
        GuiUtil.drawMainPanel(graphics, winX, winY, winX + winW, winY + winH);
        graphics.drawCenteredString(font, title, winX + winW / 2, winY + 8, 0xFFFFFFFF);

        int y = winY + 26;
        for (Component line : message) {
            for (FormattedCharSequence wrapped : font.split(line, winW - MARGIN * 2)) {
                graphics.drawString(font, wrapped, winX + MARGIN, y, danger ? 0xFFFF0000 : 0xFF3F3F3F, false);
                y += font.lineHeight + 2;
            }
        }
        if (input != null) {
            input.render(graphics, font, winX + MARGIN, y + 2, winW - MARGIN * 2, 16, true, inputHint);
            y += 22;
        } else if (inlineError != null) {
            y += 2;
        }
        if (inlineError != null) {
            for (FormattedCharSequence wrapped : font.split(inlineError, winW - MARGIN * 2)) {
                graphics.drawString(font, wrapped, winX + MARGIN, y,
                        net.minecraft.ChatFormatting.RED.getColor(), false);
                y += font.lineHeight + 2;
            }
        }
        for (int i = 0; i < buttons.size(); i++) {
            int[] bounds = buttonBounds.get(i);
            ProgramStyle.drawButton(graphics, font, bounds[0], bounds[1], bounds[2], bounds[3],
                    buttons.get(i).label(), mouseX, mouseY, true, (buttons.size() == 2 && i == buttons.size() - 1) ? danger ? 0xFFFF0000 : 0xFF0080FF : 0xFF808080);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (input != null) {
            int y = winY + 26 + wrappedHeight(Minecraft.getInstance().font, winW) + 2;
            input.mouseClicked(mouseX, mouseY, winX + MARGIN, y, winW - MARGIN * 2, 16);
        }
        for (int i = 0; i < buttons.size(); i++) {
            int[] bounds = buttonBounds.get(i);
            if (mouseX >= bounds[0] && mouseX < bounds[0] + bounds[2]
                    && mouseY >= bounds[1] && mouseY < bounds[1] + bounds[3]) {
                Button clicked = buttons.get(i);
                clicked.action().run();
                return true;
            }
        }
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (input != null) {
            if (input.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (!buttons.isEmpty()) {
                    buttons.get(buttons.size() - 1).action().run();
                }
                return true;
            }
        } else if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && enterButtonIndex >= 0 && enterButtonIndex < buttons.size()) {
            buttons.get(enterButtonIndex).action().run();
            return true;
        }
        return true;
    }

    public boolean charTyped(char c, int modifiers) {
        return input != null && input.charTyped(c);
    }

    public int getWinX() {
        return winX;
    }

    public int getWinWidth() {
        return winW;
    }
}
