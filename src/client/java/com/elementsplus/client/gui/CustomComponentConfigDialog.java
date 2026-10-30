package com.elementsplus.client.gui;

import com.elementsplus.client.config.NumberFormat;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.CustomComponentCompiler;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.core.circuit.storage.StoredCircuit;
import com.elementsplus.network.CustomComponentCompilePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 自定义元件配置窗口。表单中的引脚布局与元件实际边缘坐标一致，
 * 支持自动分配、拖动/移除引脚和未使用引脚列表。
 */
public class CustomComponentConfigDialog {
    private static final int SIDE_ORDER = 4;
    private static final int ROW = 13;

    private final Font font;
    private final BlockPos tablePos;
    private final Runnable onClose;
    private final Consumer<CustomComponentCompilePayload> onSubmit;
    private final int winX;
    private final int winY;
    private final int winW;
    private final int winH;
    private final int previewX, previewY, previewW, previewH;
    private final int unusedX, unusedY, unusedW, unusedH;
    private final int submitX, submitY, closeX, closeY;
    private final int autoX, autoY;
    private final EditBox nameBox;
    private final EditBox descriptionBox;
    private final EditBox iconCharBox;
    private final EditBox colorBox;
    private final EditBox widthBox;
    private final EditBox heightBox;

    private final CircuitDiagram diagram;
    private final List<CustomComponentCompiler.PinCandidate> candidates;
    private final List<StoredCircuit.PinBinding> pins = new ArrayList<>();
    private ResourceLocation iconItem;
    private String error = "";
    private int unusedScroll;
    private int dragPin = -1;
    private CustomComponentCompiler.PinCandidate dragCandidate;
    private double dragStartX, dragStartY;
    private boolean pinMoved;
    private int firstUnused = 0;

    private record PinTarget(Direction side, int offset) {
    }

    private final Component COLOR_TEXT = Component.translatable("gui.elements-plus.custom_component.color");
    private final Component WIDTH_TEXT = Component.translatable("gui.elements-plus.custom_component.width");
    private final Component HEIGHT_TEXT = Component.translatable("gui.elements-plus.custom_component.height");

    public CustomComponentConfigDialog(BlockPos tablePos, CircuitDiagram diagram, Font font,
                                       int screenWidth, int screenHeight,
                                       Consumer<CustomComponentCompilePayload> onSubmit, Runnable onClose) {
        this.tablePos = tablePos;
        this.diagram = diagram;
        this.font = font;
        this.onSubmit = onSubmit;
        this.onClose = onClose;
        this.candidates = diagram == null ? List.of() : CustomComponentCompiler.candidates(diagram);

        this.winW = Math.min(430, screenWidth - 20);
        this.winH = Math.min(300, screenHeight - 20);
        this.winX = (screenWidth - winW) / 2;
        this.winY = (screenHeight - winH) / 2;
        this.previewX = winX + 10;
        this.previewY = winY + 112;
        this.previewW = Math.min(180, winW / 2 - 16);
        this.previewH = winH - 154;
        this.unusedX = previewX + previewW + 14;
        this.unusedY = previewY;
        this.unusedW = winW - (unusedX - winX) - 10;
        this.unusedH = previewH;
        this.submitX = winX + winW - 110;
        this.submitY = winY + winH - 24;
        this.closeX = winX + winW - 54;
        this.closeY = submitY;
        this.autoX = winX + 10;
        this.autoY = winY + winH - 24;

        int fieldX = winX + 48;
        nameBox = new EditBox(font, fieldX, winY + 26, winW - 48 - 10 - 100 - 10 - font.width(COLOR_TEXT), 16, Component.empty());
        nameBox.setMaxLength(64);
        descriptionBox = new EditBox(font, fieldX, winY + 48, Math.max(30, winW - (fieldX - winX) - 10), 16, Component.empty());
        descriptionBox.setMaxLength(256);
        iconCharBox = new EditBox(font, fieldX, winY + 70, 40, 16, Component.empty());
        iconCharBox.setMaxLength(4);
        colorBox = new EditBox(font, winX + winW - 10 - 100, winY + 26, 100, 16, Component.empty());
        colorBox.setMaxLength(8);
        colorBox.setValue("808080");
        widthBox = new EditBox(font, winX + winW - 10 - 34 - 10 - font.width(HEIGHT_TEXT) - 34, winY + 70, 34, 16, Component.empty());
        widthBox.setMaxLength(2);
        heightBox = new EditBox(font, winX + winW - 10 - 34, winY + 70, 34, 16, Component.empty());
        heightBox.setMaxLength(2);

        if (diagram != null) {
            CustomComponentCompiler.Layout layout = CustomComponentCompiler.autoLayout(diagram);
            pins.addAll(layout.pins());
            widthBox.setValue(Integer.toString(layout.width()));
            heightBox.setValue(Integer.toString(layout.height()));
        } else {
            widthBox.setValue("1");
            heightBox.setValue("1");
        }
        if (!candidates.isEmpty()) {
            nameBox.setValue("");
        }
        nameBox.setFocused(true);
    }

    public int getWindowX() {
        return winX;
    }

    public int getWindowWidth() {
        return winW;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        GuiUtil.raisePose(g);
        g.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), 0x80000000);
        GuiUtil.drawMainPanel(g, winX, winY, winX + winW, winY + winH);
        g.drawCenteredString(font, Component.translatable("gui.elements-plus.custom_component.title"),
                winX + winW / 2, winY + 6, 0xFFFFFFFF);

        g.drawString(font, Component.translatable("gui.elements-plus.custom_component.name"), winX + 10, winY + 30, 0xFF3F3F3F, false);
        g.drawString(font, Component.translatable("gui.elements-plus.custom_component.description"), winX + 10, winY + 52, 0xFF3F3F3F, false);
        g.drawString(font, Component.translatable("gui.elements-plus.custom_component.icon"), winX + 10, winY + 74, 0xFF3F3F3F, false);
        g.drawString(font, WIDTH_TEXT, winX + winW - 10 - 34 - 5 - font.width(HEIGHT_TEXT) - 5 - 34 - 5 - font.width(WIDTH_TEXT), winY + 74, 0xFF3F3F3F, false);
        g.drawString(font, HEIGHT_TEXT, winX + winW - 10 - 34 - 5 - font.width(HEIGHT_TEXT), winY + 74, 0xFF3F3F3F, false);

        nameBox.render(g, mouseX, mouseY, partialTick);
        descriptionBox.render(g, mouseX, mouseY, partialTick);
        iconCharBox.render(g, mouseX, mouseY, partialTick);
        colorBox.render(g, mouseX, mouseY, partialTick);
        widthBox.render(g, mouseX, mouseY, partialTick);
        heightBox.render(g, mouseX, mouseY, partialTick);

        g.drawString(font, COLOR_TEXT, winX + winW - 10 - 100 - 5 - font.width(COLOR_TEXT), winY + 30, 0xFF3F3F3F, false);
        g.drawString(font, Component.translatable("gui.elements-plus.custom_component.icon_item"), winX + 10, winY + 96, 0xFF3F3F3F, false);
        renderButton(g, winX + 48, winY + 93, 70, 14, Component.translatable("gui.elements-plus.custom_component.use_hand"), mouseX, mouseY);
        g.drawString(font, iconItem == null ? "-" : iconItem.toString(), winX + 48 + 70 + 5, winY + 96, 0xFFAAAAAA);

        drawPreview(g, mouseX, mouseY);
        drawUnused(g, mouseX, mouseY);

        renderButton(g, submitX, submitY, 50, 16, Component.translatable("gui.elements-plus.custom_component.compile"), mouseX, mouseY, 0xFF558055);
        renderButton(g, autoX, autoY, 80, 16, Component.translatable("gui.elements-plus.custom_component.auto_layout"), mouseX, mouseY);
        renderButton(g, closeX, closeY, 48, 16, Component.translatable("gui.elements-plus.custom_component.close"), mouseX, mouseY);

        if (!error.isEmpty()) {
            g.drawString(font, Component.literal(error), winX + 10, submitY - 4 - font.lineHeight, 0xFFFF5555, true);
        }
    }

    private void drawPreview(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(previewX, previewY, previewX + previewW, previewY + previewH, 0x50000000);
        int w = parsed(widthBox, 1, 16);
        int h = parsed(heightBox, 1, 16);
        if (w <= 0 || h <= 0) return;
        double scale = Math.min((previewW - 30) / (double) w, (previewH - 30) / (double) h);
        double left = previewX + (previewW - w * scale) / 2.0;
        double top = previewY + (previewH - h * scale) / 2.0;
        double right = left + w * scale;
        double bottom = top + h * scale;
        int color = colorValue();
        g.fill((int) left, (int) top, (int) Math.ceil(right), (int) Math.ceil(bottom), color);
        g.fill((int) left, (int) top, (int) Math.ceil(right), (int) top + 1, 0xFF202020);
        g.fill((int) left, (int) Math.floor(bottom - 1), (int) Math.ceil(right), (int) Math.ceil(bottom), 0xFF202020);
        g.fill((int) left, (int) top, (int) left + 1, (int) Math.ceil(bottom), 0xFF202020);
        g.fill((int) Math.floor(right - 1), (int) top, (int) Math.ceil(right), (int) Math.ceil(bottom), 0xFF202020);

        int hoveredPin = dragPin < 0 && dragCandidate == null ? pinAt(mouseX, mouseY) : -1;
        for (int i = 0; i < pins.size(); i++) {
            StoredCircuit.PinBinding pin = pins.get(i);
            double[] p = pinPoint(pin, left, top, scale, w, h);
            int colorPin = pinTypeColor(pin.type());
            g.fill((int) p[0] - 2, (int) p[1] - 2, (int) p[0] + 2, (int) p[1] + 2, colorPin);
            if (i == hoveredPin) {
                g.fill((int) p[0] - 4, (int) p[1] - 4, (int) p[0] + 5, (int) p[1] - 3, 0xFFFFFFFF);
                g.fill((int) p[0] - 4, (int) p[1] + 4, (int) p[0] + 5, (int) p[1] + 5, 0xFFFFFFFF);
                g.fill((int) p[0] - 4, (int) p[1] - 3, (int) p[0] - 3, (int) p[1] + 4, 0xFFFFFFFF);
                g.fill((int) p[0] + 4, (int) p[1] - 3, (int) p[0] + 5, (int) p[1] + 4, 0xFFFFFFFF);
            }
            String label = pin.label();
            String shown = font.plainSubstrByWidth(label, 34);
            int tx;
            int ty;
            if (pin.side() == Direction.WEST) {
                tx = (int) p[0] - font.width(shown) - 5;
                ty = (int) p[1] - 4;
            } else if (pin.side() == Direction.EAST) {
                tx = (int) p[0] + 5;
                ty = (int) p[1] - 4;
            } else if (pin.side() == Direction.NORTH) {
                tx = (int) p[0] - font.width(shown) / 2;
                ty = (int) p[1] - 12;
            } else {
                tx = (int) p[0] - font.width(shown) / 2;
                ty = (int) p[1] + 5;
            }
            g.drawString(font, shown, tx, ty, bitWidthColor(pin.bitWidth()), false);
        }

        if (pinMoved && (dragPin >= 0 || dragCandidate != null)) {
            PinTarget target = targetAt(mouseX, mouseY);
            if (target != null) {
                double[] p = pinPoint(new StoredCircuit.PinBinding(target.side(), target.offset(),
                        dragPin >= 0 ? pins.get(dragPin).type() : dragCandidate.type(),
                        dragPin >= 0 ? pins.get(dragPin).bitWidth() : dragCandidate.bitWidth(),
                        ""), left, top, scale, w, h);
                int ghostColor = dragPin >= 0 ? pinTypeColor(pins.get(dragPin).type())
                        : pinTypeColor(dragCandidate.type());
                g.fill((int) p[0] - 3, (int) p[1] - 3, (int) p[0] + 4, (int) p[1] + 4, 0xAA000000);
                g.fill((int) p[0] - 2, (int) p[1] - 2, (int) p[0] + 3, (int) p[1] + 3, ghostColor);
            }
            String dragLabel = dragPin >= 0 ? pins.get(dragPin).label() : dragCandidate.label();
            int dragWidth = dragPin >= 0 ? pins.get(dragPin).bitWidth() : dragCandidate.bitWidth();
            String shown = font.plainSubstrByWidth(dragLabel, 100);
            int tx = mouseX + 10;
            int ty = mouseY - 6;
            g.fill(tx - 2, ty - 2, tx + font.width(shown) + 2, ty + 10, 0xB0000000);
            g.drawString(font, shown, tx, ty, bitWidthColor(dragWidth), false);
        }
    }

    private static int pinTypeColor(CircuitComponent.PinType type) {
        return type == CircuitComponent.PinType.INPUT ? 0xFF4488FF : 0xFF44FF88;
    }

    private static int bitWidthColor(int bitWidth) {
        return bitWidth >= 8 ? 0xFFFF55FF : 0xFFAAAAAA;
    }

    private void drawUnused(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, Component.translatable("gui.elements-plus.custom_component.unused"), unusedX, unusedY, 0xFF3F3F3F, false);
        List<CustomComponentCompiler.PinCandidate> unused = unusedCandidates();
        int visible = Math.max(1, (unusedH - 16) / ROW);
        int maxScroll = Math.max(0, unused.size() - visible);
        unusedScroll = Mth.clamp(unusedScroll, 0, maxScroll);
        int y = unusedY + 14;
        for (int i = 0; i < visible && unusedScroll + i < unused.size(); i++) {
            CustomComponentCompiler.PinCandidate pin = unused.get(unusedScroll + i);
            boolean rowHovered = mouseX >= unusedX && mouseX <= unusedX + unusedW
                    && mouseY >= y && mouseY < y + ROW - 1;
            g.fill(unusedX, y, unusedX + unusedW, y + ROW - 1, rowHovered ? 0x60000000 : 0x40000000);
            if (!pin.duplicate()) {
                g.fill(unusedX + 3, y + 4, unusedX + 8, y + 9, pinTypeColor(pin.type()));
                if (rowHovered) {
                    g.fill(unusedX + 2, y + 3, unusedX + 9, y + 4, 0xFFFFFFFF);
                    g.fill(unusedX + 2, y + 9, unusedX + 9, y + 10, 0xFFFFFFFF);
                    g.fill(unusedX + 2, y + 3, unusedX + 3, y + 10, 0xFFFFFFFF);
                    g.fill(unusedX + 8, y + 3, unusedX + 9, y + 10, 0xFFFFFFFF);
                }
            }
            int color = pin.duplicate() ? 0xFF666666 : bitWidthColor(pin.bitWidth());
            g.drawString(font, pin.label(), unusedX + 12, y + 2, color, false);
            y += ROW;
        }
    }

    private List<CustomComponentCompiler.PinCandidate> unusedCandidates() {
        Set<String> used = new HashSet<>();
        for (StoredCircuit.PinBinding pin : pins) used.add(pin.label());
        List<CustomComponentCompiler.PinCandidate> result = new ArrayList<>();
        for (CustomComponentCompiler.PinCandidate pin : candidates) {
            if (!used.contains(pin.label())) result.add(pin);
        }
        return result;
    }

    private static double[] pinPoint(StoredCircuit.PinBinding pin, double left, double top,
                                     double scale, int width, int height) {
        return switch (pin.side()) {
            case NORTH -> new double[]{left + (pin.offset() + 0.5) * scale, top};
            case EAST -> new double[]{left + width * scale, top + (pin.offset() + 0.5) * scale};
            case SOUTH -> new double[]{left + (width - 0.5 - pin.offset()) * scale, top + height * scale};
            default -> new double[]{left, top + (height - 0.5 - pin.offset()) * scale};
        };
    }

    private void renderButton(GuiGraphics g, int x, int y, int w, int h, Component text, int mouseX, int mouseY, int color) {
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        g.fill(x, y, x + w, y + h, hovered ? 0xFFFFFFFF : 0xFF000000);
        GuiUtil.drawSubPanel(g, x + 1, y + 1, x + w - 1, y + h - 1, color, GuiUtil.SubPanelType.CONVEX);
        g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }

    private void renderButton(GuiGraphics g, int x, int y, int w, int h, Component text, int mouseX, int mouseY) {
        renderButton(g, x, y, w, h, text, mouseX, mouseY, 0xFF808080);
    }

    private int parsed(EditBox box, int defaultVal, int max) {
        try {
            int value = Integer.parseInt(box.getValue().trim());
            return Math.clamp(value, 1, max);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private int colorValue() {
        try {
            String raw = colorBox.getValue().trim().replace("#", "").replace("0x", "");
            return 0xFF000000 | (Integer.parseInt(raw, 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return 0xFF808080;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (EditBox box : List.of(nameBox, descriptionBox, iconCharBox, colorBox, widthBox, heightBox)) {
            box.setFocused(false);
        }
        for (EditBox box : List.of(nameBox, descriptionBox, iconCharBox, colorBox, widthBox, heightBox)) {
            if (mouseX >= box.getX() && mouseX < box.getX() + box.getWidth()
                    && mouseY >= box.getY() && mouseY < box.getY() + box.getHeight()) {
                box.setFocused(true);
                box.mouseClicked(mouseX, mouseY, button);
                return true;
            }
        }
        if (mouseX >= autoX && mouseX < autoX + 80 && mouseY >= autoY && mouseY < autoY + 16) {
            if (diagram != null) {
                CustomComponentCompiler.Layout layout = CustomComponentCompiler.autoLayout(diagram);
                pins.clear();
                pins.addAll(layout.pins());
                widthBox.setValue(Integer.toString(layout.width()));
                heightBox.setValue(Integer.toString(layout.height()));
                error = "";
            }
            return true;
        }
        if (mouseX >= closeX && mouseX < closeX + 48 && mouseY >= closeY && mouseY < closeY + 16) {
            onClose.run();
            return true;
        }
        if (mouseX >= submitX && mouseX < submitX + 50 && mouseY >= submitY && mouseY < submitY + 16) {
            submit();
            return true;
        }
        if (mouseX >= winX + 48 && mouseX < winX + 48 + 70 && mouseY >= winY + 93 && mouseY < winY + 93 + 14) {
            ItemStack carried = Minecraft.getInstance().player == null ? ItemStack.EMPTY
                    : Minecraft.getInstance().player.containerMenu.getCarried();
            iconItem = carried.isEmpty() ? null : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(carried.getItem());
            return true;
        }
        if (mouseX >= unusedX && mouseX <= unusedX + unusedW && mouseY >= unusedY + 14 && mouseY < unusedY + unusedH) {
            int index = unusedScroll + (int) ((mouseY - unusedY - 14) / ROW);
            List<CustomComponentCompiler.PinCandidate> unused = unusedCandidates();
            if (index >= 0 && index < unused.size()) {
                CustomComponentCompiler.PinCandidate candidate = unused.get(index);
                if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                    assignFirst(candidate);
                } else if (!candidate.duplicate()) {
                    dragCandidate = candidate;
                    dragStartX = mouseX;
                    dragStartY = mouseY;
                    pinMoved = false;
                }
            }
            return true;
        }
        int hit = pinAt(mouseX, mouseY);
        if (hit >= 0) {
            if (button == 1 || net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                pins.remove(hit);
            } else {
                dragPin = hit;
                dragStartX = mouseX;
                dragStartY = mouseY;
                pinMoved = false;
            }
            return true;
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if ((dragPin >= 0 || dragCandidate != null)
                && Math.hypot(mouseX - dragStartX, mouseY - dragStartY) > 3) {
            pinMoved = true;
        }
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragPin >= 0) {
            if (pinMoved) movePin(dragPin, mouseX, mouseY);
            dragPin = -1;
            pinMoved = false;
            dragCandidate = null;
            return true;
        }
        if (dragCandidate != null) {
            if (pinMoved) placeCandidate(dragCandidate, targetAt(mouseX, mouseY));
            dragCandidate = null;
            pinMoved = false;
            return true;
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= unusedY && mouseY < unusedY + unusedH) {
            unusedScroll = Math.max(0, unusedScroll - (int) Math.signum(verticalAmount));
        }
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose.run();
            return true;
        }
        for (EditBox box : List.of(nameBox, descriptionBox, iconCharBox, colorBox, widthBox, heightBox)) {
            if (box.isFocused() && box.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        return true;
    }

    public boolean charTyped(char c, int modifiers) {
        for (EditBox box : List.of(nameBox, descriptionBox, iconCharBox, colorBox, widthBox, heightBox)) {
            if (box.isFocused() && box.charTyped(c, modifiers)) return true;
        }
        return true;
    }

    private void submit() {
        int width = parsed(widthBox, 1, 16);
        int height = parsed(heightBox, 1, 16);
        pins.removeIf(pin -> pin.offset() >= (pin.side() == Direction.NORTH || pin.side() == Direction.SOUTH
                ? width : height));
        List<Component> reasons = CustomComponentCompiler.validate(diagram, nameBox.getValue(), width, height, pins);
        if (!reasons.isEmpty()) {
            error = reasons.get(0).getString();
            return;
        }
        error = "";
        String charIcon = iconCharBox.getValue().isBlank() ? null : iconCharBox.getValue();
        onSubmit.accept(new CustomComponentCompilePayload(tablePos, nameBox.getValue(), descriptionBox.getValue(),
                iconItem, charIcon, colorValue(), width, height, new ArrayList<>(pins)));
        onClose.run();
    }

    private int pinAt(double mouseX, double mouseY) {
        int w = parsed(widthBox, 1, 16);
        int h = parsed(heightBox, 1, 16);
        double scale = Math.min((previewW - 30) / (double) w, (previewH - 30) / (double) h);
        double left = previewX + (previewW - w * scale) / 2.0;
        double top = previewY + (previewH - h * scale) / 2.0;
        for (int i = 0; i < pins.size(); i++) {
            double[] p = previewPoint(pins.get(i), left, top, scale, w, h);
            if (Math.hypot(mouseX - p[0], mouseY - p[1]) <= 7) return i;
        }
        return -1;
    }

    private PinTarget targetAt(double mouseX, double mouseY) {
        int width = parsed(widthBox, 1, 16);
        int height = parsed(heightBox, 1, 16);
        double scale = Math.min((previewW - 30) / (double) width, (previewH - 30) / (double) height);
        double left = previewX + (previewW - width * scale) / 2.0;
        double top = previewY + (previewH - height * scale) / 2.0;
        double right = left + width * scale;
        double bottom = top + height * scale;
        if (mouseX < left - 10 || mouseX > right + 10 || mouseY < top - 10 || mouseY > bottom + 10) {
            return null;
        }
        double dl = Math.abs(mouseX - left);
        double dr = Math.abs(mouseX - right);
        double dt = Math.abs(mouseY - top);
        double db = Math.abs(mouseY - bottom);
        double min = Math.min(Math.min(dl, dr), Math.min(dt, db));
        if (min == dt) {
            return new PinTarget(Direction.NORTH, Mth.clamp((int) ((mouseX - left) / scale), 0, width - 1));
        }
        if (min == db) {
            return new PinTarget(Direction.SOUTH, Mth.clamp((int) ((right - mouseX) / scale), 0, width - 1));
        }
        if (min == dl) {
            return new PinTarget(Direction.WEST, Mth.clamp((int) ((bottom - mouseY) / scale), 0, height - 1));
        }
        return new PinTarget(Direction.EAST, Mth.clamp((int) ((mouseY - top) / scale), 0, height - 1));
    }

    private void movePin(int index, double mouseX, double mouseY) {
        if (index < 0 || index >= pins.size()) return;
        PinTarget target = targetAt(mouseX, mouseY);
        if (target == null) {
            pins.remove(index);
            return;
        }
        placePinAt(index, target);
    }

    /** 将已有引脚移动到 target；默认拒绝占用位置，Shift 覆盖。 */
    private void placePinAt(int index, PinTarget target) {
        for (int i = pins.size() - 1; i >= 0; i--) {
            StoredCircuit.PinBinding pin = pins.get(i);
            if (i != index && pin.side() == target.side() && pin.offset() == target.offset()) {
                if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                    pins.remove(i);
                    if (i < index) index--;
                } else {
                    return;
                }
            }
        }
        StoredCircuit.PinBinding old = pins.get(index);
        pins.set(index, new StoredCircuit.PinBinding(target.side(), target.offset(),
                old.type(), old.bitWidth(), old.label()));
    }

    /** 从未使用列表拖动到 preview。 */
    private void placeCandidate(CustomComponentCompiler.PinCandidate candidate, PinTarget target) {
        if (target == null || candidate.duplicate()) return;
        for (StoredCircuit.PinBinding pin : pins) {
            if (pin.label().equals(candidate.label())) return;
        }
        for (int i = pins.size() - 1; i >= 0; i--) {
            StoredCircuit.PinBinding pin = pins.get(i);
            if (pin.side() == target.side() && pin.offset() == target.offset()) {
                if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                    pins.remove(i);
                } else {
                    return;
                }
            }
        }
        pins.add(new StoredCircuit.PinBinding(target.side(), target.offset(),
                candidate.type(), candidate.bitWidth(), candidate.label()));
    }

    private void assignFirst(CustomComponentCompiler.PinCandidate candidate) {
        if (candidate.duplicate()) return;
        for (StoredCircuit.PinBinding pin : pins) if (pin.label().equals(candidate.label())) return;
        int width = parsed(widthBox, 1, 16);
        int height = parsed(heightBox, 1, 16);
        for (Direction side : Direction.values()) {
            int capacity = side == Direction.NORTH || side == Direction.SOUTH ? width
                    : side == Direction.EAST || side == Direction.WEST ? height : 0;
            for (int offset = 0; offset < capacity; offset++) {
                boolean occupied = false;
                for (StoredCircuit.PinBinding pin : pins) {
                    if (pin.side() == side && pin.offset() == offset) {
                        occupied = true;
                        break;
                    }
                }
                if (!occupied) {
                    pins.add(new StoredCircuit.PinBinding(side, offset, candidate.type(), candidate.bitWidth(), candidate.label()));
                    return;
                }
            }
        }
    }

    private static double[] previewPoint(StoredCircuit.PinBinding pin, double left, double top,
                                         double scale, int width, int height) {
        return pinPoint(pin, left, top, scale, width, height);
    }

}
