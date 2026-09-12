package com.elementsplus.client.gui;

import com.elementsplus.ModDataComponents;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.menu.LithographyMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.function.BooleanSupplier;

public class CircuitDiagramPanel extends AbstractWidget {

    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 16.0;
    private static final double DEFAULT_ZOOM = 8.0;
    private static final int DOT_MIN_GAP = 4;
    private static final int SCROLL_PIXELS = 48;

    private static final int COLOR_BACKGROUND = 0xFFE0E0E0;
    private static final int COLOR_DOT = 0xFFA0A0A0;
    private static final int COLOR_COPPER = 0xFFB06030;
    private static final int COLOR_GOLD = 0xFFFFD700;
    private static final int COLOR_COMPONENT_FILL = 0xFF909090;
    private static final int COLOR_COMPONENT_BORDER = 0xFF555555;

    private final LithographyMachineMenu menu;
    private final BooleanSupplier activeSupplier;

    private double offsetX = -6;
    private double offsetY = -6;
    private double zoom = DEFAULT_ZOOM;
    private boolean dragging;

    private Integer lastMouseX;
    private Integer lastMouseY;

    public CircuitDiagramPanel(LithographyMachineMenu menu, BooleanSupplier activeSupplier, int x, int y, int width, int height) {
        super(x, y, width, height, net.minecraft.network.chat.Component.empty());
        this.menu = menu;
        this.activeSupplier = activeSupplier;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean show = activeSupplier.getAsBoolean();
        this.active = show;
        this.visible = show;
        if (!show) {
            return;
        }
        GuiUtil.drawSubPanel(guiGraphics, getX() - 1, getY() - 1, getX() + getWidth() + 1, getY() + getHeight() + 1, 0xFFE0E0E0);
        guiGraphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), COLOR_BACKGROUND);
        drawDotGrid(guiGraphics);
        CircuitDiagram diagram = getDiagram();
        if (diagram != null) {
            drawComponents(guiGraphics, diagram);
            drawWires(guiGraphics, diagram);
        }
        guiGraphics.disableScissor();

        if (this.lastMouseX == null) {
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
        } else {
            double dx = mouseX - this.lastMouseX;
            double dy = mouseY - this.lastMouseY;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            if (dx != 0 || dy != 0) {
                this.mouseMoveDelta(mouseX, mouseY, dx, dy);
            }
        }
    }

    private CircuitDiagram getDiagram() {
        ItemStack stack = menu.slots.get(36).getItem();
        return stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
    }

    private void drawDotGrid(GuiGraphics guiGraphics) {
        int startGX = (int) Math.floor(offsetX);
        int endGX = (int) Math.ceil(offsetX + getWidth() / zoom);
        int startGY = (int) Math.floor(offsetY);
        int endGY = (int) Math.ceil(offsetY + getHeight() / zoom);
        int step = Math.max(1, (int) Math.ceil(DOT_MIN_GAP / zoom));
        int size = Math.max(1, (int) Math.round(zoom * 0.22));
        for (int gx = startGX; gx <= endGX; gx += step) {
            for (int gy = startGY; gy <= endGY; gy += step) {
                int sx = getX() + (int) Math.floor((gx - offsetX) * zoom);
                int sy = getY() + (int) Math.floor((gy - offsetY) * zoom);
                guiGraphics.fill(sx, sy, sx + size, sy + size, COLOR_DOT);
            }
        }
    }

    private void drawComponents(GuiGraphics guiGraphics, CircuitDiagram diagram) {
        for (Map.Entry<Long, CircuitDiagram.Chunk> entry : diagram.chunks.entrySet()) {
            CircuitDiagram.Chunk chunk = entry.getValue();
            for (CircuitDiagram.Component component : chunk.components) {
                drawComponent(guiGraphics, component);
            }
        }
    }

    private void drawComponent(GuiGraphics guiGraphics, CircuitDiagram.Component component) {
        int width = component.getWidth();
        int height = component.getHeight();
        double left = getX() + (component.x - offsetX) * zoom;
        double top = getY() + (component.y - offsetY) * zoom;
        double right = left + width * zoom;
        double bottom = top + height * zoom;
        if (right < getX() || left > getX() + getWidth() || bottom < getY() || top > getY() + getHeight()) {
            return;
        }
        int lx = (int) Math.floor(left);
        int ty = (int) Math.floor(top);
        int rx = (int) Math.ceil(right);
        int by = (int) Math.ceil(bottom);
        guiGraphics.fill(lx, ty, rx, by, COLOR_COMPONENT_FILL);
        if (zoom >= 3) {
            guiGraphics.fill(lx, ty, rx, ty + 1, COLOR_COMPONENT_BORDER);
            guiGraphics.fill(lx, by - 1, rx, by, COLOR_COMPONENT_BORDER);
            guiGraphics.fill(lx, ty, lx + 1, by, COLOR_COMPONENT_BORDER);
            guiGraphics.fill(rx - 1, ty, rx, by, COLOR_COMPONENT_BORDER);
        }
    }

    private void drawWires(GuiGraphics guiGraphics, CircuitDiagram diagram) {
        for (Map.Entry<Long, CircuitDiagram.Chunk> entry : diagram.chunks.entrySet()) {
            CircuitDiagram.Chunk chunk = entry.getValue();
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    CircuitDiagram.Wire wire = chunk.wires[i][j];
                    if (wire != null) {
                        drawWire(guiGraphics, wire);
                    }
                }
            }
        }
    }

    private void drawWire(GuiGraphics guiGraphics, CircuitDiagram.Wire wire) {
        double centerX = getX() + (wire.x + 0.5 - offsetX) * zoom;
        double centerY = getY() + (wire.y + 0.5 - offsetY) * zoom;
        if (centerX < getX() - 8 || centerX > getX() + getWidth() + 8 || centerY < getY() - 8 || centerY > getY() + getHeight() + 8) {
            return;
        }
        int thickness = Math.max(1, (int) Math.round(zoom * 0.22));
        if (wire.north != null) {
            drawSegment(guiGraphics, centerX, centerY, getX() + (wire.x + 0.5 - offsetX) * zoom, getY() + (wire.y - 0.5 - offsetY) * zoom, thickness, wireColor(wire.north));
        }
        if (wire.east != null) {
            drawSegment(guiGraphics, centerX, centerY, getX() + (wire.x + 1.5 - offsetX) * zoom, getY() + (wire.y + 0.5 - offsetY) * zoom, thickness, wireColor(wire.east));
        }
        if (wire.south != null) {
            drawSegment(guiGraphics, centerX, centerY, getX() + (wire.x + 0.5 - offsetX) * zoom, getY() + (wire.y + 1.5 - offsetY) * zoom, thickness, wireColor(wire.south));
        }
        if (wire.west != null) {
            drawSegment(guiGraphics, centerX, centerY, getX() + (wire.x - 0.5 - offsetX) * zoom, getY() + (wire.y + 0.5 - offsetY) * zoom, thickness, wireColor(wire.west));
        }
        int color = wireColor(firstMaterial(wire));
        int s = Math.max(1, (int) Math.round(zoom * 0.3));
        int cxs = (int) Math.round(centerX);
        int cys = (int) Math.round(centerY);
        guiGraphics.fill(cxs - s, cys - s, cxs + s + 1, cys + s + 1, color);
    }

    private int wireColor(CircuitDiagram.Wire.WireMaterial material) {
        return material == CircuitDiagram.Wire.WireMaterial.GOLD ? COLOR_GOLD : COLOR_COPPER;
    }

    private CircuitDiagram.Wire.WireMaterial firstMaterial(CircuitDiagram.Wire wire) {
        if (wire.north != null) return wire.north;
        if (wire.east != null) return wire.east;
        if (wire.south != null) return wire.south;
        if (wire.west != null) return wire.west;
        return CircuitDiagram.Wire.WireMaterial.COPPER;
    }

    private void drawSegment(GuiGraphics guiGraphics, double x1, double y1, double x2, double y2, int thickness, int color) {
        int t = Math.max(1, thickness);
        if (Math.abs(x2 - x1) < Math.abs(y2 - y1)) {
            int cx = (int) Math.round(x1);
            guiGraphics.fill(cx - t, (int) Math.min(y1, y2), cx + t + 1, (int) Math.ceil(Math.max(y1, y2)), color);
        } else {
            int cy = (int) Math.round(y1);
            guiGraphics.fill((int) Math.min(x1, x2), cy - t, (int) Math.ceil(Math.max(x1, x2)), cy + t + 1, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (button == 2) {
            dragging = true;
        }
        return true;
    }

    @Override
    protected boolean isValidClickButton(int i) {
        return i == 0 || i == 1 || i == 2;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 2) {
            dragging = false;
        }
        return this.active && this.visible && isMouseOver(mouseX, mouseY);
    }

    public void mouseMoveDelta(double mouseX, double mouseY, double dx, double dy) {
        if (dragging) {
            offsetX -= dx / zoom;
            offsetY -= dy / zoom;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.active || !this.visible || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (Screen.hasControlDown()) {
            zoomAt(mouseX, mouseY, Math.pow(1.1, scrollY));
            return true;
        }
        double sx = scrollX != 0 ? scrollX : 0;
        double sy = scrollY;
        if (Screen.hasShiftDown()) {
            sx = scrollX != 0 ? scrollX : scrollY;
            sy = 0;
        }
        offsetX -= sx * SCROLL_PIXELS / zoom;
        offsetY -= sy * SCROLL_PIXELS / zoom;
        return true;
    }

    private void zoomAt(double mouseX, double mouseY, double factor) {
        double oldZoom = this.zoom;
        double newZoom = Mth.clamp(oldZoom * factor, MIN_ZOOM, MAX_ZOOM);
        double gx = offsetX + (mouseX - getX()) / oldZoom;
        double gy = offsetY + (mouseY - getY()) / oldZoom;
        this.zoom = newZoom;
        this.offsetX = gx - (mouseX - getX()) / newZoom;
        this.offsetY = gy - (mouseY - getY()) / newZoom;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}