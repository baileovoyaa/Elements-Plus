package com.elementsplus.client.gui;

import com.elementsplus.ModDataComponents;
import com.elementsplus.ModItems;
import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.menu.LithographyMachineMenu;
import com.elementsplus.network.ReturnCarriedPayload;
import com.elementsplus.network.UpdateCircuitDiagramPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.function.BooleanSupplier;

public class CircuitDiagramPanel extends AbstractWidget {

    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 32.0;
    private static final double DEFAULT_ZOOM = 16.0;
    private static final int DOT_MIN_GAP = 4;
    private static final int SCROLL_PIXELS = 48;

    private static final int COLOR_BACKGROUND = 0xFF2B2B28;
    private static final int COLOR_DOT = 0xFF4A4A44;
    private static final int COLOR_COPPER = 0xFFB06030;
    private static final int COLOR_GOLD = 0xFFFFD700;
    private static final int COLOR_COMPONENT_FILL = 0xFF909090;
    private static final int COLOR_COMPONENT_BORDER = 0xFF555555;
    private static final int COLOR_GHOST_VALID = 0x4010E0B0;
    private static final int COLOR_GHOST_INVALID = 0x60E03030;
    private static final int COLOR_GHOST_BORDER = 0xFFFFFFFF;

    private final LithographyMachineMenu menu;
    private final BooleanSupplier activeSupplier;

    private double offsetX = -6;
    private double offsetY = -6;
    private double zoom = DEFAULT_ZOOM;
    private boolean dragging;
    private boolean erasing;

    private Direction rotation = Direction.NORTH;
    private ItemStack previewStack = ItemStack.EMPTY;
    private CircuitComponent virtualComponent;

    private CircuitDiagram.Wire.WireMaterial draggingWire;
    private int lastWireX;
    private int lastWireY;

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
        if (!show) {
            return;
        }
        GuiUtil.drawSubPanel(guiGraphics, getX() - 1, getY() - 1, getX() + getWidth() + 1, getY() + getHeight() + 1, 0xFFE0E0E0);
        guiGraphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), COLOR_BACKGROUND);
        CircuitDiagram diagram = getDiagram();
        if (diagram != null) {
            drawDotGrid(guiGraphics);
            drawComponents(guiGraphics, diagram);
            drawWires(guiGraphics, diagram);
        }
        drawGhost(guiGraphics, mouseX, mouseY);
        drawWirePreview(guiGraphics, mouseX, mouseY);
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

    public boolean isReadOnly() {
        ItemStack stack = menu.slots.get(36).getItem();
        return stack.has(ModDataComponents.EQUIVALENT_COMPONENT);
    }

    public boolean hasDiagram() {
        return getDiagram() != null;
    }

    public void setPreviewStack(ItemStack stack) {
        this.previewStack = stack;
    }

    public CircuitComponent getVirtualComponent() {
        return virtualComponent;
    }

    public void setVirtualComponent(CircuitComponent component) {
        this.virtualComponent = component;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX < getX() + getWidth()
                && mouseY >= getY() && mouseY < getY() + getHeight();
    }

    public boolean isPlaceable(ItemStack stack) {
        return placeComponent(stack) != null || stack.is(ModItems.COPPER_WIRE) || stack.is(ModItems.GOLD_WIRE);
    }

    private CircuitComponent placeComponent(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation id = stack.get(ModDataComponents.EQUIVALENT_COMPONENT);
        if (id == null) {
            return null;
        }
        return BuiltinCircuitComponents.byId(id);
    }

    private ItemStack displayCarried() {
        return previewStack.isEmpty() ? menu.getCarried() : previewStack;
    }

    private CircuitComponent activeComponent() {
        CircuitComponent carried = placeComponent(displayCarried());
        return carried != null ? carried : virtualComponent;
    }

    private int cellX(double mouseX) {
        return (int) Math.floor(offsetX + (mouseX - getX()) / zoom);
    }

    private int cellY(double mouseY) {
        return (int) Math.floor(offsetY + (mouseY - getY()) / zoom);
    }

    private int placementWidth(CircuitComponent component) {
        return rotation == Direction.NORTH || rotation == Direction.SOUTH ? component.getWidth() : component.getHeight();
    }

    private int placementHeight(CircuitComponent component) {
        return rotation == Direction.NORTH || rotation == Direction.SOUTH ? component.getHeight() : component.getWidth();
    }

    private void drawGhost(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        CircuitComponent component = (hasDiagram() && !isReadOnly()) ? activeComponent() : null;
        if (component == null) {
            return;
        }
        int gx = cellX(mouseX);
        int gy = cellY(mouseY);
        int w = placementWidth(component);
        int h = placementHeight(component);
        double left = getX() + (gx - offsetX) * zoom;
        double top = getY() + (gy - offsetY) * zoom;
        double right = left + w * zoom;
        double bottom = top + h * zoom;
        int lx = (int) Math.floor(left);
        int ty = (int) Math.floor(top);
        int rx = (int) Math.ceil(right);
        int by = (int) Math.ceil(bottom);
        boolean conflict = diagramHasConflict(gx, gy, w, h);
        guiGraphics.fill(lx, ty, rx, by, conflict ? COLOR_GHOST_INVALID : COLOR_GHOST_VALID);
        if (zoom >= 3) {
            guiGraphics.fill(lx, ty, rx, ty + 1, COLOR_GHOST_BORDER);
            guiGraphics.fill(lx, by - 1, rx, by, COLOR_GHOST_BORDER);
            guiGraphics.fill(lx, ty, lx + 1, by, COLOR_GHOST_BORDER);
            guiGraphics.fill(rx - 1, ty, rx, by, COLOR_GHOST_BORDER);
        }
    }

    private boolean diagramHasConflict(int gx, int gy, int w, int h) {
        CircuitDiagram diagram = getDiagram();
        if (diagram == null) {
            return false;
        }
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                if (diagram.getBlock(gx + dx, gy + dy) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private void tryPlace(int gx, int gy, CircuitComponent component) {
        ItemStack stack = menu.slots.get(36).getItem();
        CircuitDiagram diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null) {
            return;
        }
        boolean force = Screen.hasShiftDown();
        int w = placementWidth(component);
        int h = placementHeight(component);
        if (!force) {
            for (int dx = 0; dx < w; dx++) {
                for (int dy = 0; dy < h; dy++) {
                    if (diagram.getBlock(gx + dx, gy + dy) != null) {
                        return;
                    }
                }
            }
        }
        CircuitDiagram editor = diagram.copy();
        editor.setBlock(gx, gy, new CircuitDiagram.Component(component, rotation), force);
        commitDiagram(stack, editor);
    }

    private void tryErase(int gx, int gy) {
        ItemStack stack = menu.slots.get(36).getItem();
        CircuitDiagram diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null) {
            return;
        }
        if (diagram.getBlock(gx, gy) == null) {
            return;
        }
        CircuitDiagram editor = diagram.copy();
        editor.setBlock(gx, gy, null);
        commitDiagram(stack, editor);
    }

    private void commitDiagram(ItemStack stack, CircuitDiagram editor) {
        stack.set(ModDataComponents.CIRCUIT_DIAGRAM, editor);
        menu.onDiagramChanged();
        ClientPlayNetworking.send(new UpdateCircuitDiagramPayload(editor));
    }

    private CircuitDiagram.Wire.WireMaterial wireMaterial() {
        ItemStack carried = displayCarried();
        if (carried.is(ModItems.COPPER_WIRE)) {
            return CircuitDiagram.Wire.WireMaterial.COPPER;
        }
        if (carried.is(ModItems.GOLD_WIRE)) {
            return CircuitDiagram.Wire.WireMaterial.GOLD;
        }
        return null;
    }

    private void drawWirePreview(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!hasDiagram() || isReadOnly()) {
            return;
        }
        CircuitDiagram.Wire.WireMaterial material = wireMaterial();
        if (material == null) {
            return;
        }
        int gx = cellX(mouseX);
        int gy = cellY(mouseY);
        double centerX = getX() + (gx + 0.5 - offsetX) * zoom;
        double centerY = getY() + (gy + 0.5 - offsetY) * zoom;
        int size = Math.max(2, (int) Math.round(zoom * 0.4));
        int color = (wireColor(material) & 0x00FFFFFF) | 0x80000000;
        guiGraphics.fill(
                (int) Math.floor(centerX - size / 2.0),
                (int) Math.floor(centerY - size / 2.0),
                (int) Math.ceil(centerX + size / 2.0),
                (int) Math.ceil(centerY + size / 2.0),
                color);
    }

    private void stepWireDrag(int toX, int toY) {
        if (draggingWire == null) {
            return;
        }
        int walkX = lastWireX;
        int walkY = lastWireY;
        while (walkX != toX || walkY != toY) {
            int dx = Integer.signum(toX - walkX);
            int dy = Integer.signum(toY - walkY);
            if (dx != 0) {
                commitConnection(walkX, walkY, walkX + dx, walkY, draggingWire);
                walkX += dx;
            } else {
                commitConnection(walkX, walkY, walkX, walkY + dy, draggingWire);
                walkY += dy;
            }
        }
        lastWireX = toX;
        lastWireY = toY;
    }

    private void commitConnection(int fromX, int fromY, int toX, int toY, CircuitDiagram.Wire.WireMaterial material) {
        ItemStack stack = menu.slots.get(36).getItem();
        CircuitDiagram diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null) {
            return;
        }
        CircuitDiagram editor = diagram.copy();
        applyConnection(editor, fromX, fromY, toX, toY, material);
        commitDiagram(stack, editor);
    }

    private void applyConnection(CircuitDiagram diagram, int fromX, int fromY, int toX, int toY, CircuitDiagram.Wire.WireMaterial material) {
        int dx = toX - fromX;
        int dy = toY - fromY;
        if (Math.abs(dx) + Math.abs(dy) != 1) {
            return;
        }
        Direction fromSide = dx > 0 ? Direction.EAST : (dx < 0 ? Direction.WEST : (dy > 0 ? Direction.SOUTH : Direction.NORTH));
        boolean force = Screen.hasShiftDown();
        CircuitDiagram.Block toBlock = diagram.getBlock(toX, toY);
        if (toBlock instanceof CircuitDiagram.Component && !force) {
            setWireSide(diagram, fromX, fromY, fromSide, material);
            return;
        }
        if (toBlock instanceof CircuitDiagram.Component) {
            diagram.setBlock(toX, toY, null);
        }
        setWireSide(diagram, fromX, fromY, fromSide, material);
        setWireSide(diagram, toX, toY, fromSide.getOpposite(), material);
    }

    private void setWireSide(CircuitDiagram diagram, int x, int y, Direction side, CircuitDiagram.Wire.WireMaterial material) {
        CircuitDiagram.Block block = diagram.getBlock(x, y);
        CircuitDiagram.Wire wire;
        if (block instanceof CircuitDiagram.Wire existing) {
            wire = existing;
        } else if (block == null) {
            wire = new CircuitDiagram.Wire(null, null, null, null);
            wire.x = x;
            wire.y = y;
            diagram.setBlock(x, y, wire);
        } else {
            return;
        }
        switch (side) {
            case NORTH -> wire.north = material;
            case EAST -> wire.east = material;
            case SOUTH -> wire.south = material;
            case WEST -> wire.west = material;
        }
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

    private CircuitDiagram.Wire.WireMaterial materialOf(CircuitDiagram.Wire w, Direction d) {
        return switch (d) {
            case DOWN, UP -> null;
            case NORTH -> w.north;
            case EAST -> w.east;
            case SOUTH -> w.south;
            case WEST -> w.west;
        };
    }

    private void drawSegmentForDir(GuiGraphics g, Direction d, double centerX, double centerY, double baseX, double baseY, double half, double zoom, double gap, int thickness, int color) {
        switch (d) {
            case NORTH -> drawSegment(g, centerX, centerY + gap, baseX + half, baseY, thickness, color);
            case EAST -> drawSegment(g, centerX - gap, centerY, baseX + zoom, baseY + half, thickness, color);
            case SOUTH -> drawSegment(g, centerX, centerY - gap, baseX + half, baseY + zoom, thickness, color);
            case WEST -> drawSegment(g, centerX + gap, centerY, baseX, baseY + half, thickness, color);
        }
    }

    private void drawWire(GuiGraphics guiGraphics, CircuitDiagram.Wire wire) {
        double centerX = getX() + (wire.x + 0.5 - offsetX) * zoom;
        double centerY = getY() + (wire.y + 0.5 - offsetY) * zoom;
        if (centerX < getX() - 8 || centerX > getX() + getWidth() + 8 || centerY < getY() - 8 || centerY > getY() + getHeight() + 8) {
            return;
        }
        int thickness = Math.max(1, (int) Math.round(zoom * 0.2));
        double baseX = getX() + (wire.x - offsetX) * zoom;
        double baseY = getY() + (wire.y - offsetY) * zoom;
        double half = 0.5 * zoom;
        double gap = 0.03 * zoom;

        var materials = new CircuitDiagram.Wire.WireMaterial[]{
                CircuitDiagram.Wire.WireMaterial.COPPER,
                CircuitDiagram.Wire.WireMaterial.GOLD
        };

        for (var target : materials) {
            // 彩色线
            for (Direction d : Direction.Plane.HORIZONTAL) {
                var m = materialOf(wire, d);
                if (m != target) continue;

                drawSegmentForDir(
                        guiGraphics, d, centerX, centerY,
                        baseX, baseY, half, zoom, gap,
                        thickness, wireColor(m)
                );
            }

            // 黑色描边，保持原来“先彩后黑”的顺序
            if (thickness > 1) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    var m = materialOf(wire, d);
                    if (m != target) continue;

                    drawSegmentForDir(
                            guiGraphics, d, centerX, centerY,
                            baseX, baseY, half, zoom, gap,
                            thickness - 1, 0xFF000000
                    );
                }
            }
        }
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
            return true;
        }
        if (button == 0) {
            if (!isReadOnly()) {
                CircuitDiagram.Wire.WireMaterial wire = wireMaterial();
                if (wire != null) {
                    draggingWire = wire;
                    lastWireX = cellX(mouseX);
                    lastWireY = cellY(mouseY);
                } else {
                    CircuitComponent component = activeComponent();
                    if (component != null) {
                        tryPlace(cellX(mouseX), cellY(mouseY), component);
                    }
                }
            }
            return true;
        }
        if (button == 1) {
            if (virtualComponent != null) {
                virtualComponent = null;
                return true;
            }
            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty()) {
                ClientPlayNetworking.send(new ReturnCarriedPayload());
            } else if (!isReadOnly()) {
                erasing = true;
                tryErase(cellX(mouseX), cellY(mouseY));
            }
            return true;
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
        if (button == 1) {
            erasing = false;
        }
        if (button == 0) {
            draggingWire = null;
        }
        return this.active && this.visible && isMouseOver(mouseX, mouseY);
    }

    public void mouseMoveDelta(double mouseX, double mouseY, double dx, double dy) {
        if (dragging) {
            offsetX -= dx / zoom;
            offsetY -= dy / zoom;
        }
        if (erasing) {
            tryErase(cellX(mouseX), cellY(mouseY));
        }
        if (draggingWire != null) {
            stepWireDrag(cellX(mouseX), cellY(mouseY));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.active || !this.visible || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (Screen.hasControlDown()) {
            zoomAt(mouseX, mouseY, Math.pow(2, scrollY / 5));
            return true;
        }
        if (Screen.hasAltDown() && activeComponent() != null && !isReadOnly()) {
            rotation = scrollY > 0 ? rotation.getClockWise() : rotation.getCounterClockWise();
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