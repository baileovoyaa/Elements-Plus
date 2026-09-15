package com.elementsplus.client.gui;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModDataComponents;
import com.elementsplus.ModItems;
import com.elementsplus.core.circuit.CircuitSimulator;
import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.CircuitComponent.PinType;
import com.elementsplus.core.circuit.component.InputComponentInstance;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.menu.LithographyMachineMenu;
import com.elementsplus.network.ReturnCarriedPayload;
import com.elementsplus.network.UpdateCircuitDiagramPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

public class CircuitDiagramPanel extends AbstractWidget {

    public static boolean sound = false;

    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 32.0;
    private static final double DEFAULT_ZOOM = 16.0;
    private static final int DOT_MIN_GAP = 4;
    private static final int SCROLL_PIXELS = 48;

    public static int colorBackground = 0xFF2B2B28;
    public static int colorDot = 0xFF4A4A44;
    private static final int COLOR_COPPER = 0xFFB06030;
    private static final int COLOR_GOLD = 0xFFFFD700;
    private static final int COLOR_COMPONENT_FILL = 0xFF909090;
    private static final int COLOR_COMPONENT_BORDER = 0xFF555555;
    private static final int COLOR_PIN_INPUT = 0xFF4488FF;
    private static final int COLOR_PIN_OUTPUT = 0xFF44FF88;
    private static final int COLOR_GHOST_VALID = 0x4010E0B0;
    private static final int COLOR_GHOST_INVALID = 0x60E03030;
    private static final int COLOR_GHOST_BORDER = 0xFFFFFFFF;
    private static final int COLOR_SELECTION = 0xFFFFC900;
    private static final int COLOR_CYCLE_BANNER = 0xCCB02020;
    private static final int COLOR_SELBOX = 0xFF40E0D0;
    private static final int COLOR_BOX_FILL = 0x3030D0C0;
    private static final int COLOR_BUS_8 = 0xFFFF00FF;
    private static final int COLOR_BUS_16 = 0xFFA000FF;
    private static final int COLOR_BUS_32 = 0xFF8000FF;
    private static final int COLOR_BUS_64 = 0xFF0000FF;

    private static final double DRAG_THRESHOLD = 4.0;

    private static final ResourceLocation CYCLE_TEXTURE = ElementsPlus.id("textures/gui/cycle.png");

    private final LithographyMachineMenu menu;
    private final BooleanSupplier activeSupplier;

    private CircuitSimulator simulator;
    private boolean simulationDirty;

    private double offsetX = -6;
    private double offsetY = -6;
    private double zoom = DEFAULT_ZOOM;
    private boolean dragging;
    private boolean erasing;

    private Direction rotation = Direction.NORTH;
    private ItemStack previewStack = ItemStack.EMPTY;
    private CircuitComponent virtualComponent;
    private CircuitDiagram.Wire.WireMaterial virtualWire;

    private CircuitDiagram.Wire.WireMaterial draggingWire;
    private int lastWireX;
    private int lastWireY;

    private int selectedX = Integer.MIN_VALUE;
    private int selectedY = Integer.MIN_VALUE;

    private Integer lastMouseX;
    private Integer lastMouseY;

    private long lastInputClickTime = 0;
    private CircuitDiagram.Component lastInputClicked;

    private enum DragMode {
        NONE, MOVE, COPY, BOX_SELECT
    }

    private final Set<Long> selectedCells = new HashSet<>();
    private int selBoxX, selBoxY, selBoxW, selBoxH;
    private int selectionVersion;

    private boolean pressActive;
    private double pressScreenX, pressScreenY;
    private int pressCellX, pressCellY;
    private boolean pressOnComponent;
    private boolean pressSelected;
    private boolean pressCtrl;

    private DragMode dragMode = DragMode.NONE;
    private final Set<Long> dragSelected = new HashSet<>();
    private int dragBoxX, dragBoxY, dragBoxW, dragBoxH;
    private final Set<Long> dragOwnedCells = new HashSet<>();
    private final List<CircuitDiagram.Component> dragComps = new ArrayList<>();
    private final List<CircuitDiagram.Wire> dragWires = new ArrayList<>();
    private int dispDx, dispDy;

    private int boxSelX1, boxSelY1, boxSelX2, boxSelY2;

    public void setSimulator(CircuitSimulator simulator) {
        this.simulator = simulator;
        this.simulationDirty = true;
    }

    public boolean consumeSimulationDirty() {
        boolean dirty = simulationDirty;
        simulationDirty = false;
        return dirty;
    }

    public final SoundInstance COMPONENT_PLACE_SOUND = SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_PLACE, 1, 1);
    public final SoundInstance COMPONENT_ERASE_SOUND = SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_BREAK, 1, 1);
    public final SoundInstance COMPONENT_MOVE_SOUND = SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_HIT, 1, 1);
    public final SoundInstance WIRE_PLACE_SOUND = SimpleSoundInstance.forUI(SoundEvents.STONE_PLACE, 1, 1);
    public final SoundInstance WIRE_ERASE_SOUND = SimpleSoundInstance.forUI(SoundEvents.STONE_BREAK, 1, 1);
    public final SoundInstance INVALID_SOUND = SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HARP.value(), 0.5f, 1);
    public final SoundInstance ROTATE_CLOCKWISE_SOUND = SimpleSoundInstance.forUI(SoundEvents.COMPARATOR_CLICK, 0.55f, 1);
    public final SoundInstance ROTATE_ANTICLOCKWISE_SOUND = SimpleSoundInstance.forUI(SoundEvents.COMPARATOR_CLICK, 0.5f, 1);

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

        if ((pressActive || dragMode != DragMode.NONE) && !isLeftMouseDown()) {
            finalizePress(lastMouseX != null ? lastMouseX : mouseX, lastMouseY != null ? lastMouseY : mouseY);
        }

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

        GuiUtil.drawSubPanel(guiGraphics, getX() - 1, getY() - 1, getX() + getWidth() + 1, getY() + getHeight() + 1, 0xFFE0E0E0);
        guiGraphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), colorBackground);
        CircuitDiagram diagram = getDiagram();
        if (diagram != null) {
            drawDotGrid(guiGraphics);
            if (dragMode == DragMode.MOVE) {
                drawComponents(guiGraphics, diagram);
                drawWires(guiGraphics, diagram);
                drawGroupAt(guiGraphics, dispDx, dispDy);
            } else {
                drawComponents(guiGraphics, diagram);
                drawWires(guiGraphics, diagram);
                if (dragMode == DragMode.COPY) {
                    drawCopyGhost(guiGraphics, dispDx, dispDy);
                }
            }
            if (isMultiSelected() || dragMode != DragMode.NONE) {
                if (dragMode == DragMode.MOVE) {
                    drawSelBoxOutline(guiGraphics, selBoxX + dispDx, selBoxY + dispDy, selBoxW, selBoxH);
                } else {
                    drawSelBoxOutline(guiGraphics, selBoxX, selBoxY, selBoxW, selBoxH);
                }
            }
            if (simulator != null && simulator.hasCycle()) {
                drawCycleBanner(guiGraphics);
            }
        }
        if (dragMode == DragMode.BOX_SELECT) {
            drawBoxSelection(guiGraphics);
        }
        drawGhost(guiGraphics, mouseX, mouseY);
        drawWirePreview(guiGraphics, mouseX, mouseY);
        guiGraphics.disableScissor();
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

    public CircuitDiagram.Wire.WireMaterial getVirtualWire() {
        return virtualWire;
    }

    public void setVirtualWire(CircuitDiagram.Wire.WireMaterial wire) {
        this.virtualWire = wire;
    }

    public int getSelectedX() {
        return selectedX;
    }

    public int getSelectedY() {
        return selectedY;
    }

    public boolean hasSelection() {
        return selectedX != Integer.MIN_VALUE && selectedY != Integer.MIN_VALUE;
    }

    public CircuitDiagram.Component getSelectedComponent() {
        if (!hasSelection()) {
            return null;
        }
        CircuitDiagram diagram = getDiagram();
        if (diagram == null) {
            return null;
        }
        CircuitDiagram.Block block = diagram.getBlock(selectedX, selectedY);
        return block instanceof CircuitDiagram.Component component ? component : null;
    }

    public void setSelected(int x, int y) {
        this.selectedX = x;
        this.selectedY = y;
    }

    public boolean isMultiSelected() {
        return selectedCells.size() > 1;
    }

    public int getSelectionVersion() {
        return selectionVersion;
    }

    private static long cellKey(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    private static int cellXOf(long key) {
        return (int) (key >> 32);
    }

    private static int cellYOf(long key) {
        return (int) (key & 0xFFFFFFFFL);
    }

    private boolean isSelected(int x, int y) {
        return selectedCells.contains(cellKey(x, y));
    }

    private CircuitDiagram.Component blockComponent(int gx, int gy) {
        CircuitDiagram diagram = getDiagram();
        if (diagram == null) {
            return null;
        }
        CircuitDiagram.Block block = diagram.getBlock(gx, gy);
        return block instanceof CircuitDiagram.Component component ? component : null;
    }

    private static boolean insideBox(int x, int y, int bx, int by, int bw, int bh) {
        return bw > 0 && bh > 0 && x >= bx && x < bx + bw && y >= by && y < by + bh;
    }

    private void bumpSelection() {
        selectionVersion++;
    }

    private void clearSelection() {
        selectedCells.clear();
        selBoxW = 0;
        selBoxH = 0;
        setSelected(Integer.MIN_VALUE, Integer.MIN_VALUE);
        bumpSelection();
    }

    /**
     * 只选中坐标 (gx,gy) 处的元件（单选框包围它）。
     */
    private void setSelectionSingle(int gx, int gy) {
        selectedCells.clear();
        selectedCells.add(cellKey(gx, gy));
        setSelected(gx, gy);
        updateSelBox();
        bumpSelection();
    }

    private void setSelBox(int x, int y, int w, int h) {
        selBoxX = x;
        selBoxY = y;
        selBoxW = w;
        selBoxH = h;
    }

    /**
     * 根据当前选中元件的包围盒刷新选框。
     */
    private void updateSelBox() {
        if (selectedCells.isEmpty()) {
            selBoxW = 0;
            selBoxH = 0;
            return;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        CircuitDiagram diagram = getDiagram();
        if (diagram != null) {
            for (Map.Entry<Long, CircuitDiagram.Chunk> entry : diagram.chunks.entrySet()) {
                for (CircuitDiagram.Component component : entry.getValue().components) {
                    if (!selectedCells.contains(cellKey(component.x, component.y))) {
                        continue;
                    }
                    minX = Math.min(minX, component.x);
                    minY = Math.min(minY, component.y);
                    maxX = Math.max(maxX, component.x + component.getWidth() - 1);
                    maxY = Math.max(maxY, component.y + component.getHeight() - 1);
                }
            }
        }
        if (minX == Integer.MAX_VALUE) {
            selBoxW = 0;
            selBoxH = 0;
            return;
        }
        setSelBox(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /**
     * 普通点击：选中元件或清空选择，并在输入元件上检测双击切换。
     */
    private void onClickReleased(int gx, int gy) {
        CircuitDiagram diagram = getDiagram();
        if (diagram == null) {
            clearSelection();
            return;
        }
        CircuitDiagram.Block block = diagram.getBlock(gx, gy);
        if (block instanceof CircuitDiagram.Component component) {
            setSelectionSingle(component.x, component.y);
            if (component.component == BuiltinCircuitComponents.INPUT) {
                long now = Util.getMillis();
                if (lastInputClicked == component && now - lastInputClickTime <= 300) {
                    toggleInputSignal(component);
                    lastInputClicked = null;
                    lastInputClickTime = 0;
                    return;
                }
                lastInputClicked = component;
                lastInputClickTime = now;
            } else {
                lastInputClicked = null;
                lastInputClickTime = 0;
            }
        } else {
            clearSelection();
            lastInputClicked = null;
            lastInputClickTime = 0;
        }
    }

    /**
     * 双击「输入」元件：在 0 和 15 之间快速切换信号强度。
     */
    private void toggleInputSignal(CircuitDiagram.Component component) {
        ItemStack stack = menu.slots.get(36).getItem();
        CircuitDiagram diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null) {
            return;
        }
        CircuitDiagram editor = diagram.copy();
        CircuitDiagram.Block b = editor.getBlock(component.x, component.y);
        if (b instanceof CircuitDiagram.Component ec) {
            InputComponentInstance instance = (InputComponentInstance) ec.instance;
            instance.setSignal(instance.getSignal() == 0 ? 15 : 0);
        }
        setSelectionSingle(component.x, component.y);
        commitDiagram(stack, editor);
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
        int ghostColor = conflict ? COLOR_GHOST_INVALID : COLOR_GHOST_VALID;
        float ghostAlpha = ((ghostColor >> 24) & 0xFF) / 255.0F;
        drawPins(guiGraphics, component, rotation, left, top, ghostAlpha);
        drawIcon(guiGraphics, component, (left + right) / 2.0, (top + bottom) / 2.0, right - left, bottom - top, ghostAlpha);
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

    private void playSound(SoundInstance soundInstance) {
        if (sound) {
            Minecraft.getInstance().getSoundManager().play(soundInstance);
        }
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
                        playSound(INVALID_SOUND);
                        return;
                    }
                }
            }
        }
        CircuitDiagram editor = diagram.copy();
        if (editor.setBlock(gx, gy, new CircuitDiagram.Component(component, rotation), force)) {
            playSound(COMPONENT_PLACE_SOUND);
            setSelectionSingle(gx, gy);
        }
        commitDiagram(stack, editor);
    }

    private void tryErase(int gx, int gy) {
        ItemStack stack = menu.slots.get(36).getItem();
        CircuitDiagram diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null) {
            return;
        }
        CircuitDiagram.Block block = diagram.getBlock(gx, gy);
        if (block == null) {
            return;
        }
        CircuitDiagram editor = diagram.copy();
        if (block instanceof CircuitDiagram.Component compBlock) {
            boolean wasSelected = selectedCells.remove(cellKey(compBlock.x, compBlock.y));
            if (wasSelected) {
                if (selectedCells.isEmpty()) {
                    setSelected(Integer.MIN_VALUE, Integer.MIN_VALUE);
                    selBoxW = 0;
                    selBoxH = 0;
                } else {
                    updateSelBox();
                }
                bumpSelection();
            } else if (hasSelection() && compBlock.x == selectedX && compBlock.y == selectedY) {
                setSelected(Integer.MIN_VALUE, Integer.MIN_VALUE);
            }
            playSound(COMPONENT_ERASE_SOUND);
        } else {
            playSound(WIRE_ERASE_SOUND);
        }
        editor.setBlock(gx, gy, null);
        commitDiagram(stack, editor);
    }

    /**
     * 已对元件实例做出修改后（读写属性面板），调用此方法把改动下发到服务端。
     * 仅在有电路图且非只读（未放置等价组件）时才真正提交。
     */
    public void commitDiagram() {
        ItemStack stack = menu.slots.get(36).getItem();
        CircuitDiagram diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null || isReadOnly()) {
            return;
        }
        commitDiagram(stack, diagram);
    }

    private void commitDiagram(ItemStack stack, CircuitDiagram editor) {
        stack.set(ModDataComponents.CIRCUIT_DIAGRAM, editor);
        simulationDirty = true;
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
        return virtualWire;
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
        playSound(WIRE_PLACE_SOUND);
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
                guiGraphics.fill(sx, sy, sx + size, sy + size, colorDot);
            }
        }
    }

    private void drawComponents(GuiGraphics guiGraphics, CircuitDiagram diagram) {
        boolean moving = dragMode == DragMode.MOVE;
        for (Map.Entry<Long, CircuitDiagram.Chunk> entry : diagram.chunks.entrySet()) {
            CircuitDiagram.Chunk chunk = entry.getValue();
            for (CircuitDiagram.Component component : chunk.components) {
                if (moving && dragComps.contains(component)) {
                    continue;
                }
                drawComponent(guiGraphics, component);
            }
        }
    }

    private void drawComponent(GuiGraphics guiGraphics, CircuitDiagram.Component component) {
        drawComponentAt(guiGraphics, component, 0, 0);
    }

    private void drawComponentAt(GuiGraphics guiGraphics, CircuitDiagram.Component component, int dx, int dy) {
        int width = component.getWidth();
        int height = component.getHeight();
        double left = getX() + (component.x + dx - offsetX) * zoom;
        double top = getY() + (component.y + dy - offsetY) * zoom;
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
        drawPins(guiGraphics, component.component, component.direction, left, top, 1.0F);
        drawIcon(guiGraphics, component.component, (left + right) / 2.0, (top + bottom) / 2.0, right - left, bottom - top, 1.0F);
        var font = Minecraft.getInstance().font;
        if (dx == 0 && dy == 0 && (component.component == BuiltinCircuitComponents.INPUT || component.component == BuiltinCircuitComponents.OUTPUT)) {
            int value = simulator != null ? simulator.getComponentValue(component.x, component.y) : 0;
            String text = String.valueOf(value);
            guiGraphics.drawString(font, text,
                    (int) Math.round((left + right) / 2.0 - font.width(text) / 2.0),
                    (int) Math.round((top + bottom) / 2.0 - 4),
                    0xFFFFFFFF, true);
        }
        if (isSelected(component.x, component.y)) {
            guiGraphics.fill(lx, ty, rx, ty + 1, COLOR_SELECTION);
            guiGraphics.fill(lx, by - 1, rx, by, COLOR_SELECTION);
            guiGraphics.fill(lx, ty, lx + 1, by, COLOR_SELECTION);
            guiGraphics.fill(rx - 1, ty, rx, by, COLOR_SELECTION);
        }
    }

    private void drawGroupAt(GuiGraphics guiGraphics, int dx, int dy) {
        if (dragComps.isEmpty() && dragWires.isEmpty()) {
            return;
        }
        for (CircuitDiagram.Wire wire : dragWires) {
            drawWireAt(guiGraphics, wire, dx, dy);
        }
        for (CircuitDiagram.Component component : dragComps) {
            drawComponentAt(guiGraphics, component, dx, dy);
        }
    }

    private void drawCopyGhost(GuiGraphics guiGraphics, int dx, int dy) {
        if (dragComps.isEmpty() && dragWires.isEmpty()) {
            return;
        }
        boolean valid = copyPlacementValid(dx, dy);
        int color = valid ? COLOR_GHOST_VALID : COLOR_GHOST_INVALID;
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        for (CircuitDiagram.Wire wire : dragWires) {
            drawWireAt(guiGraphics, wire, dx, dy);
        }
        for (CircuitDiagram.Component component : dragComps) {
            drawComponentGhostAt(guiGraphics, component, dx, dy, color, alpha);
        }
    }

    private void drawComponentGhostAt(GuiGraphics guiGraphics, CircuitDiagram.Component component, int dx, int dy, int color, float alpha) {
        int width = component.getWidth();
        int height = component.getHeight();
        double left = getX() + (component.x + dx - offsetX) * zoom;
        double top = getY() + (component.y + dy - offsetY) * zoom;
        double right = left + width * zoom;
        double bottom = top + height * zoom;
        if (right < getX() || left > getX() + getWidth() || bottom < getY() || top > getY() + getHeight()) {
            return;
        }
        int lx = (int) Math.floor(left);
        int ty = (int) Math.floor(top);
        int rx = (int) Math.ceil(right);
        int by = (int) Math.ceil(bottom);
        guiGraphics.fill(lx, ty, rx, by, color);
        if (zoom >= 3) {
            guiGraphics.fill(lx, ty, rx, ty + 1, COLOR_GHOST_BORDER);
            guiGraphics.fill(lx, by - 1, rx, by, COLOR_GHOST_BORDER);
            guiGraphics.fill(lx, ty, lx + 1, by, COLOR_GHOST_BORDER);
            guiGraphics.fill(rx - 1, ty, rx, by, COLOR_GHOST_BORDER);
        }
        drawPins(guiGraphics, component.component, component.direction, left, top, alpha);
        drawIcon(guiGraphics, component.component, (left + right) / 2.0, (top + bottom) / 2.0, right - left, bottom - top, alpha);
    }

    private void drawSelBoxOutline(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        double left = getX() + (x - offsetX) * zoom;
        double top = getY() + (y - offsetY) * zoom;
        double right = left + w * zoom;
        double bottom = top + h * zoom;
        int lx = (int) Math.floor(left);
        int ty = (int) Math.floor(top);
        int rx = (int) Math.ceil(right);
        int by = (int) Math.ceil(bottom);
        guiGraphics.fill(lx, ty, rx, ty + 1, COLOR_SELBOX);
        guiGraphics.fill(lx, by - 1, rx, by, COLOR_SELBOX);
        guiGraphics.fill(lx, ty, lx + 1, by, COLOR_SELBOX);
        guiGraphics.fill(rx - 1, ty, rx, by, COLOR_SELBOX);
    }

    private void drawBoxSelection(GuiGraphics guiGraphics) {
        int minX = Math.min(boxSelX1, boxSelX2);
        int maxX = Math.max(boxSelX1, boxSelX2);
        int minY = Math.min(boxSelY1, boxSelY2);
        int maxY = Math.max(boxSelY1, boxSelY2);
        double left = getX() + (minX - offsetX) * zoom;
        double top = getY() + (minY - offsetY) * zoom;
        double right = getX() + (maxX + 1 - offsetX) * zoom;
        double bottom = getY() + (maxY + 1 - offsetY) * zoom;
        int lx = (int) Math.floor(left);
        int ty = (int) Math.floor(top);
        int rx = (int) Math.ceil(right);
        int by = (int) Math.ceil(bottom);
        guiGraphics.fill(lx, ty, rx, by, COLOR_BOX_FILL);
        guiGraphics.fill(lx, ty, rx, ty + 1, COLOR_SELBOX);
        guiGraphics.fill(lx, by - 1, rx, by, COLOR_SELBOX);
        guiGraphics.fill(lx, ty, lx + 1, by, COLOR_SELBOX);
        guiGraphics.fill(rx - 1, ty, rx, by, COLOR_SELBOX);
    }

    /**
     * 渲染元件引脚：输入引脚为蓝色小方块，输出引脚为绿色小方块。
     * 引脚位置按元件的原始尺寸（component.getWidth()/getHeight()）定义在四条边上，
     * direction 指定旋转（NORTH=原始方向，EAST/SOUTH/WEST=顺时针 90/180/270 度）。
     */
    private void drawPins(GuiGraphics g, CircuitComponent component, Direction direction, double left, double top, float alpha) {
        int w0 = component.getWidth();
        int h0 = component.getHeight();
        if (w0 <= 0 || h0 <= 0) {
            return;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            int count = (side == Direction.NORTH || side == Direction.SOUTH) ? w0 : h0;
            for (int k = 0; k < count; k++) {
                PinType type = component.getPin(side, k);
                if (type == null || type == PinType.NONE) {
                    continue;
                }
                double nx, ny;
                switch (side) {
                    case NORTH -> {
                        nx = k + 0.5;
                        ny = 0;
                    }
                    case EAST -> {
                        nx = w0;
                        ny = k + 0.5;
                    }
                    case SOUTH -> {
                        nx = w0 - 0.5 - k;
                        ny = h0;
                    }
                    default -> { // WEST
                        nx = 0;
                        ny = h0 - 0.5 - k;
                    }
                }
                double rx, ry;
                switch (direction) {
                    case EAST -> {
                        rx = h0 - ny;
                        ry = nx;
                    }
                    case SOUTH -> {
                        rx = w0 - nx;
                        ry = h0 - ny;
                    }
                    case WEST -> {
                        rx = ny;
                        ry = w0 - nx;
                    }
                    default -> { // NORTH
                        rx = nx;
                        ry = ny;
                    }
                }
                double sx = left + rx * zoom;
                double sy = top + ry * zoom;
                int size = Mth.clamp((int) Math.round(zoom * 0.35), 2, 8);
                int color = type == PinType.INPUT ? COLOR_PIN_INPUT : COLOR_PIN_OUTPUT;
                if (alpha < 1.0F) {
                    color = (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, (int) (alpha * 255))) << 24);
                }
                // 引脚限定在组件边框内：只绘制朝元件内部的那半个正方形
                double rw = (direction == Direction.EAST || direction == Direction.WEST) ? h0 : w0;
                double rh = (direction == Direction.EAST || direction == Direction.WEST) ? w0 : h0;
                double half = size / 2.0;
                double x1, y1, x2, y2;
                if (ry <= 1e-6) {               // 上边：向内部（下）延伸
                    x1 = sx - half;
                    y1 = sy;
                    x2 = sx + half;
                    y2 = sy + half;
                } else if (ry >= rh - 1e-6) {   // 下边：向内部（上）延伸
                    x1 = sx - half;
                    y1 = sy - half;
                    x2 = sx + half;
                    y2 = sy;
                } else if (rx <= 1e-6) {        // 左边：向内部（右）延伸
                    x1 = sx;
                    y1 = sy - half;
                    x2 = sx + half;
                    y2 = sy + half;
                } else {                        // 右边：向内部（左）延伸
                    x1 = sx - half;
                    y1 = sy - half;
                    x2 = sx;
                    y2 = sy + half;
                }
                g.fill(
                        (int) Math.floor(x1),
                        (int) Math.floor(y1),
                        (int) Math.ceil(x2),
                        (int) Math.ceil(y2),
                        color);
            }
        }
    }

    private void drawIcon(GuiGraphics guiGraphics, CircuitComponent component, double centerX, double centerY, double boxWidth, double boxHeight, float alpha) {
        ResourceLocation icon = component.getIcon();
        if (icon == null) {
            return;
        }
        double fit = Math.min(boxWidth, boxHeight);
        int size = Mth.clamp((int) Math.floor(fit), 4, 16);
        int x = (int) Math.round(centerX - size / 2.0);
        int y = (int) Math.round(centerY - size / 2.0);
        if (alpha < 1.0F) {
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        }
        guiGraphics.blit(icon, x, y, size, size, 0.0F, 0.0F, 16, 16, 16, 16);
        if (alpha < 1.0F) {
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void drawWires(GuiGraphics guiGraphics, CircuitDiagram diagram) {
        boolean moving = dragMode == DragMode.MOVE;
        for (Map.Entry<Long, CircuitDiagram.Chunk> entry : diagram.chunks.entrySet()) {
            CircuitDiagram.Chunk chunk = entry.getValue();
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    CircuitDiagram.Wire wire = chunk.wires[i][j];
                    if (wire != null) {
                        if (moving && dragOwnedCells.contains(cellKey(wire.x, wire.y))) {
                            continue;
                        }
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
        drawWireAt(guiGraphics, wire, 0, 0);
    }

    private void drawWireAt(GuiGraphics guiGraphics, CircuitDiagram.Wire wire, int dx, int dy) {
        double centerX = getX() + (wire.x + dx + 0.5 - offsetX) * zoom;
        double centerY = getY() + (wire.y + dy + 0.5 - offsetY) * zoom;
        if (centerX < getX() - 8 || centerX > getX() + getWidth() + 8 || centerY < getY() - 8 || centerY > getY() + getHeight() + 8) {
            return;
        }
        int thickness = Math.max(1, (int) Math.round(zoom * 0.2));
        double baseX = getX() + (wire.x + dx - offsetX) * zoom;
        double baseY = getY() + (wire.y + dy - offsetY) * zoom;
        double half = 0.5 * zoom;
        double gap = 0.03 * zoom;

        var materials = new CircuitDiagram.Wire.WireMaterial[]{
                CircuitDiagram.Wire.WireMaterial.COPPER,
                CircuitDiagram.Wire.WireMaterial.GOLD
        };

        boolean anyBad = false;
        for (var target : materials) {
            // 彩色线外侧
            for (Direction d : Direction.Plane.HORIZONTAL) {
                var m = materialOf(wire, d);
                if (m != target) continue;

                drawSegmentForDir(
                        guiGraphics, d, centerX, centerY,
                        baseX, baseY, half, zoom, gap,
                        thickness, wireColor(m)
                );
            }

            boolean bad = dx == 0 && dy == 0 && simulator != null && simulator.isWireBad(wire.x, wire.y, target);
            anyBad |= bad;

            // 内层：有信号变红（0~15 对应不同亮度），坏网保持黑色
            if (thickness > 1) {
                int innerColor;
                if (bad) {
                    innerColor = 0xFF000000;
                } else {
                    int value = dx == 0 && dy == 0 && simulator != null ? simulator.getWireValue(wire.x, wire.y, target) : 0;
                    innerColor = wireSignalColor(value);
                }
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    var m = materialOf(wire, d);
                    if (m != target) continue;
                    drawSegmentForDir(
                            guiGraphics, d, centerX, centerY,
                            baseX, baseY, half, zoom, gap,
                            thickness - 1, innerColor
                    );
                }
            }
        }

        // 循环依赖：该导线网络所有格叠加 cycle 贴图
        if (anyBad) {
            int cellX = (int) Math.floor(baseX);
            int cellY = (int) Math.floor(baseY);
            int cellSize = (int) Math.ceil(zoom);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 0.55F);
            guiGraphics.blit(CYCLE_TEXTURE, cellX, cellY, cellSize, cellSize, 0.0F, 0.0F, 16, 16, 16, 16);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private int wireSignalColor(int value) {
        int v = Mth.clamp(value, 0, 15);
        int brightness = Math.round(v * 255.0F / 15.0F);
        return 0xFF000000 | (brightness << 16);
    }

    private void drawCycleBanner(GuiGraphics guiGraphics) {
        int bannerHeight = 14;
        int y = getY() + getHeight() - bannerHeight - 2;
        guiGraphics.fill(getX() + 2, y, getX() + getWidth() - 2, y + bannerHeight, COLOR_CYCLE_BANNER);
        Component text = Component.translatable("gui.elements-plus.lithography_machine.cycle_warning");
        guiGraphics.drawString(Minecraft.getInstance().font, text, getX() + 6, y + 3, 0xFFFFFFFF, true);
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
            boolean placing = wireMaterial() != null || activeComponent() != null;
            if (placing) {
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
            int gx = cellX(mouseX);
            int gy = cellY(mouseY);
            CircuitDiagram.Component under = blockComponent(gx, gy);
            pressActive = true;
            pressScreenX = mouseX;
            pressScreenY = mouseY;
            pressCellX = gx;
            pressCellY = gy;
            pressOnComponent = under != null;
            pressSelected = under != null && isSelected(under.x, under.y);
            pressCtrl = Screen.hasControlDown();
            dragMode = DragMode.NONE;
            dispDx = 0;
            dispDy = 0;
            return true;
        }
        if (button == 1) {
            if (virtualComponent != null) {
                virtualComponent = null;
                return true;
            }
            if (virtualWire != null) {
                virtualWire = null;
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
            if (pressActive || dragMode != DragMode.NONE) {
                finalizePress((int) mouseX, (int) mouseY);
            }
        }
        return this.active && this.visible;
    }

    public void mouseMoveDelta(double mouseX, double mouseY, double dx, double dy) {
        if (pressActive) {
            if (dragMode == DragMode.NONE) {
                if (Math.hypot(mouseX - pressScreenX, mouseY - pressScreenY) >= DRAG_THRESHOLD && !isReadOnly()) {
                    startDrag();
                }
            } else if (dragMode == DragMode.MOVE) {
                updateDragOffset(mouseX, mouseY);
            } else if (dragMode == DragMode.COPY) {
                updateCopyOffset(mouseX, mouseY);
            } else if (dragMode == DragMode.BOX_SELECT) {
                boxSelX2 = cellX(mouseX);
                boxSelY2 = cellY(mouseY);
            }
        }
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

    private boolean isLeftMouseDown() {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT)
                == GLFW.GLFW_PRESS;
    }

    private void startDrag() {
        if (isReadOnly()) {
            return;
        }
        if (pressOnComponent && pressCtrl) {
            beginGroupDrag();
            if (dragComps.isEmpty() && dragWires.isEmpty()) {
                dragMode = DragMode.NONE;
                return;
            }
            dragMode = DragMode.COPY;
            dispDx = 0;
            dispDy = 0;
        } else if (pressOnComponent && pressSelected) {
            beginGroupDrag();
            if (dragComps.isEmpty() && dragWires.isEmpty()) {
                dragMode = DragMode.NONE;
                return;
            }
            dragMode = DragMode.MOVE;
            dispDx = 0;
            dispDy = 0;
        } else {
            boxSelX1 = pressCellX;
            boxSelY1 = pressCellY;
            boxSelX2 = pressCellX;
            boxSelY2 = pressCellY;
            dragMode = DragMode.BOX_SELECT;
        }
    }

    private void beginGroupDrag() {
        dragSelected.clear();
        dragSelected.addAll(selectedCells);
        dragBoxX = selBoxX;
        dragBoxY = selBoxY;
        dragBoxW = selBoxW;
        dragBoxH = selBoxH;
        dragComps.clear();
        dragWires.clear();
        dragOwnedCells.clear();
        collectGroup(getDiagram(), dragComps, dragWires, dragOwnedCells);
    }

    private void collectGroup(CircuitDiagram diagram, List<CircuitDiagram.Component> comps, List<CircuitDiagram.Wire> wires, Set<Long> owned) {
        if (diagram == null) {
            return;
        }
        for (Map.Entry<Long, CircuitDiagram.Chunk> entry : diagram.chunks.entrySet()) {
            for (CircuitDiagram.Component component : entry.getValue().components) {
                if (dragSelected.contains(cellKey(component.x, component.y))) {
                    comps.add(component);
                }
            }
        }
        for (Map.Entry<Long, CircuitDiagram.Chunk> entry : diagram.chunks.entrySet()) {
            CircuitDiagram.Chunk chunk = entry.getValue();
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    CircuitDiagram.Wire wire = chunk.wires[i][j];
                    if (wire != null && insideBox(wire.x, wire.y, dragBoxX, dragBoxY, dragBoxW, dragBoxH)) {
                        wires.add(wire);
                    }
                }
            }
        }
        for (CircuitDiagram.Component component : comps) {
            for (int dx = 0; dx < component.getWidth(); dx++) {
                for (int dy = 0; dy < component.getHeight(); dy++) {
                    owned.add(cellKey(component.x + dx, component.y + dy));
                }
            }
        }
        for (CircuitDiagram.Wire wire : wires) {
            owned.add(cellKey(wire.x, wire.y));
        }
    }

    private boolean groupMoveValid(int dx, int dy) {
        if (dragComps.isEmpty() && dragWires.isEmpty()) {
            return false;
        }
        CircuitDiagram diagram = getDiagram();
        if (diagram == null) {
            return false;
        }
        for (CircuitDiagram.Component c : dragComps) {
            for (int dx0 = 0; dx0 < c.getWidth(); dx0++) {
                for (int dy0 = 0; dy0 < c.getHeight(); dy0++) {
                    int x = c.x + dx0 + dx;
                    int y = c.y + dy0 + dy;
                    if (!dragOwnedCells.contains(cellKey(x, y)) && diagram.getBlock(x, y) != null) {
                        return false;
                    }
                }
            }
        }
        for (CircuitDiagram.Wire w : dragWires) {
            int x = w.x + dx;
            int y = w.y + dy;
            if (!dragOwnedCells.contains(cellKey(x, y)) && diagram.getBlock(x, y) != null) {
                return false;
            }
        }
        return true;
    }

    private boolean copyPlacementValid(int dx, int dy) {
        if (dragComps.isEmpty() && dragWires.isEmpty()) {
            return false;
        }
        CircuitDiagram diagram = getDiagram();
        if (diagram == null) {
            return false;
        }
        for (CircuitDiagram.Component c : dragComps) {
            for (int dx0 = 0; dx0 < c.getWidth(); dx0++) {
                for (int dy0 = 0; dy0 < c.getHeight(); dy0++) {
                    if (diagram.getBlock(c.x + dx0 + dx, c.y + dy0 + dy) != null) {
                        return false;
                    }
                }
            }
        }
        for (CircuitDiagram.Wire w : dragWires) {
            if (diagram.getBlock(w.x + dx, w.y + dy) != null) {
                return false;
            }
        }
        return true;
    }

    private void updateDragOffset(double mouseX, double mouseY) {
        int dx = cellX(mouseX) - pressCellX;
        int dy = cellY(mouseY) - pressCellY;
        if (Screen.hasShiftDown()) {
            dispDx = dx;
            dispDy = dy;
            return;
        }
        if (groupMoveValid(dx, dy)) {
            dispDx = dx;
            dispDy = dy;
        }
    }

    private void updateCopyOffset(double mouseX, double mouseY) {
        dispDx = cellX(mouseX) - pressCellX;
        dispDy = cellY(mouseY) - pressCellY;
    }

    private void finalizePress(int mouseX, int mouseY) {
        int dx = cellX(mouseX) - pressCellX;
        int dy = cellY(mouseY) - pressCellY;
        DragMode mode = dragMode;
        pressActive = false;
        dragMode = DragMode.NONE;
        if (mode == DragMode.MOVE) {
            if (Screen.hasShiftDown()) {
                commitMove(dx, dy);
            } else if (groupMoveValid(dx, dy)) {
                commitMove(dx, dy);
            } else {
                commitMove(dispDx, dispDy);
            }
        } else if (mode == DragMode.COPY) {
            if (Screen.hasShiftDown() || copyPlacementValid(dx, dy)) {
                commitCopy(dx, dy);
            }
        } else if (mode == DragMode.BOX_SELECT) {
            finalizeBoxSelect();
        } else {
            onClickReleased(pressCellX, pressCellY);
        }
        clearDragState();
    }

    private void clearDragState() {
        dragSelected.clear();
        dragComps.clear();
        dragWires.clear();
        dragOwnedCells.clear();
        dispDx = 0;
        dispDy = 0;
        boxSelX1 = 0;
        boxSelY1 = 0;
        boxSelX2 = 0;
        boxSelY2 = 0;
    }

    private void commitMove(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return;
        }
        ItemStack stack = menu.slots.get(36).getItem();
        CircuitDiagram diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null || (dragComps.isEmpty() && dragWires.isEmpty())) {
            return;
        }
        CircuitDiagram editor = diagram.copy();
        boolean force = Screen.hasShiftDown();
        for (Long key : dragOwnedCells) {
            editor.setBlock(cellXOf(key), cellYOf(key), null);
        }
        for (CircuitDiagram.Wire w : dragWires) {
            CircuitDiagram.Wire nw = new CircuitDiagram.Wire(w.north, w.east, w.south, w.west);
            nw.x = w.x + dx;
            nw.y = w.y + dy;
            editor.setBlock(nw.x, nw.y, nw, force);
        }
        for (CircuitDiagram.Component c : dragComps) {
            CircuitDiagram.Component nc = c.copy();
            nc.x = c.x + dx;
            nc.y = c.y + dy;
            editor.setBlock(nc.x, nc.y, nc, force);
        }
        playSound(COMPONENT_MOVE_SOUND);
        commitDiagram(stack, editor);
        selectedCells.clear();
        for (Long key : dragSelected) {
            selectedCells.add(cellKey(cellXOf(key) + dx, cellYOf(key) + dy));
        }
        setSelBox(dragBoxX + dx, dragBoxY + dy, dragBoxW, dragBoxH);
        syncSelectionAnchor();
    }

    private void commitCopy(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return;
        }
        ItemStack stack = menu.slots.get(36).getItem();
        CircuitDiagram diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null || (dragComps.isEmpty() && dragWires.isEmpty())) {
            return;
        }
        CircuitDiagram editor = diagram.copy();
        boolean force = Screen.hasShiftDown();
        List<CircuitDiagram.Component> copies = new ArrayList<>();
        for (CircuitDiagram.Wire w : dragWires) {
            CircuitDiagram.Wire nw = new CircuitDiagram.Wire(w.north, w.east, w.south, w.west);
            nw.x = w.x + dx;
            nw.y = w.y + dy;
            editor.setBlock(nw.x, nw.y, nw, force);
        }
        for (CircuitDiagram.Component c : dragComps) {
            CircuitDiagram.Component nc = c.copy();
            nc.x = c.x + dx;
            nc.y = c.y + dy;
            if (editor.setBlock(nc.x, nc.y, nc, force)) {
                copies.add(nc);
            }
        }
        if (copies.isEmpty()) {
            return;
        }
        playSound(COMPONENT_PLACE_SOUND);
        commitDiagram(stack, editor);
        selectedCells.clear();
        for (CircuitDiagram.Component nc : copies) {
            selectedCells.add(cellKey(nc.x, nc.y));
        }
        setSelBox(dragBoxX + dx, dragBoxY + dy, dragBoxW, dragBoxH);
        syncSelectionAnchor();
    }

    private void syncSelectionAnchor() {
        if (selectedCells.size() == 1) {
            Long key = selectedCells.iterator().next();
            setSelected(cellXOf(key), cellYOf(key));
        } else if (selectedCells.isEmpty()) {
            setSelected(Integer.MIN_VALUE, Integer.MIN_VALUE);
        } else {
            setSelected(Integer.MIN_VALUE, Integer.MIN_VALUE);
        }
        bumpSelection();
    }

    private void finalizeBoxSelect() {
        int minX = Math.min(boxSelX1, boxSelX2);
        int maxX = Math.max(boxSelX1, boxSelX2);
        int minY = Math.min(boxSelY1, boxSelY2);
        int maxY = Math.max(boxSelY1, boxSelY2);
        if (minX == maxX && minY == maxY) {
            onClickReleased(pressCellX, pressCellY);
            return;
        }
        selectedCells.clear();
        CircuitDiagram diagram = getDiagram();
        if (diagram != null) {
            for (Map.Entry<Long, CircuitDiagram.Chunk> entry : diagram.chunks.entrySet()) {
                for (CircuitDiagram.Component c : entry.getValue().components) {
                    for (int dx0 = 0; dx0 < c.getWidth(); dx0++) {
                        for (int dy0 = 0; dy0 < c.getHeight(); dy0++) {
                            int x = c.x + dx0;
                            int y = c.y + dy0;
                            if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
                                selectedCells.add(cellKey(c.x, c.y));
                                dx0 = c.getWidth();
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (selectedCells.isEmpty()) {
            setSelected(Integer.MIN_VALUE, Integer.MIN_VALUE);
            selBoxW = 0;
            selBoxH = 0;
        } else {
            updateSelBox();
            syncSelectionAnchor();
        }
        clearDragState();
        bumpSelection();
    }

    public void deleteSelection() {
        if (isReadOnly() || selectedCells.isEmpty()) {
            return;
        }
        ItemStack stack = menu.slots.get(36).getItem();
        CircuitDiagram diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null) {
            return;
        }
        CircuitDiagram editor = diagram.copy();
        boolean removedComp = false;
        boolean removedWire = false;
        if (selectedCells.size() == 1) {
            Long key = selectedCells.iterator().next();
            CircuitDiagram.Block b = editor.getBlock(cellXOf(key), cellYOf(key));
            if (b != null) {
                editor.setBlock(cellXOf(key), cellYOf(key), null);
                if (b instanceof CircuitDiagram.Component) {
                    removedComp = true;
                } else {
                    removedWire = true;
                }
            }
        } else {
            for (Long key : selectedCells) {
                CircuitDiagram.Block b = editor.getBlock(cellXOf(key), cellYOf(key));
                if (b != null) {
                    editor.setBlock(cellXOf(key), cellYOf(key), null);
                    removedComp = true;
                }
            }
            for (Map.Entry<Long, CircuitDiagram.Chunk> entry : editor.chunks.entrySet()) {
                CircuitDiagram.Chunk chunk = entry.getValue();
                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j++) {
                        CircuitDiagram.Wire wire = chunk.wires[i][j];
                        if (wire != null && insideBox(wire.x, wire.y, selBoxX, selBoxY, selBoxW, selBoxH)) {
                            editor.setBlock(wire.x, wire.y, null);
                            removedWire = true;
                        }
                    }
                }
            }
        }
        if (!removedComp && !removedWire) {
            return;
        }
        playSound(removedComp ? COMPONENT_ERASE_SOUND : WIRE_ERASE_SOUND);
        commitDiagram(stack, editor);
        clearSelection();
    }

    /**
     * 元件的原位旋转：旋转方向，锚定左上角（与放置预览一致）。先试 90°，冲突时回退 180°。
     */
    private void tryRotateAt(int gx, int gy, boolean clockwise) {
        CircuitDiagram diagram = getDiagram();
        if (diagram == null || isReadOnly()) {
            return;
        }
        CircuitDiagram.Component under = blockComponent(gx, gy);
        if (under == null) {
            return;
        }
        if (!selectedCells.contains(cellKey(under.x, under.y))) {
            setSelectionSingle(under.x, under.y);
        }
        if (tryRotateGroup(clockwise)) {
            playSound(clockwise ? ROTATE_CLOCKWISE_SOUND : ROTATE_ANTICLOCKWISE_SOUND);
        }
    }

    private boolean tryRotateGroup(boolean clockwise) {
        if (selectedCells.isEmpty()) {
            return false;
        }
        ItemStack stack = menu.slots.get(36).getItem();
        CircuitDiagram diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null) {
            return false;
        }
        CircuitDiagram editor = diagram.copy();
        List<CircuitDiagram.Component> comps = new ArrayList<>();
        for (Map.Entry<Long, CircuitDiagram.Chunk> entry : editor.chunks.entrySet()) {
            for (CircuitDiagram.Component c : entry.getValue().components) {
                if (selectedCells.contains(cellKey(c.x, c.y))) {
                    comps.add(c);
                }
            }
        }
        if (comps.isEmpty()) {
            return false;
        }
        if (tryRotateApply(editor, comps, clockwise, false)) {
            commitDiagram(stack, editor);
            updateSelBox();
            bumpSelection();
            return true;
        }
        if (tryRotateApply(editor, comps, clockwise, true)) {
            commitDiagram(stack, editor);
            updateSelBox();
            bumpSelection();
            return true;
        }
        return false;
    }

    private boolean tryRotateApply(CircuitDiagram editor, List<CircuitDiagram.Component> comps, boolean clockwise, boolean opposite) {
        Set<Long> originals = new HashSet<>();
        for (CircuitDiagram.Component c : comps) {
            for (int dx = 0; dx < c.getWidth(); dx++) {
                for (int dy = 0; dy < c.getHeight(); dy++) {
                    originals.add(cellKey(c.x + dx, c.y + dy));
                }
            }
        }
        Set<Long> newOcc = new HashSet<>();
        for (CircuitDiagram.Component c : comps) {
            Direction nd = opposite ? c.direction.getOpposite() : (clockwise ? c.direction.getClockWise() : c.direction.getCounterClockWise());
            boolean ns = nd == Direction.NORTH || nd == Direction.SOUTH;
            int w = ns ? c.component.getWidth() : c.component.getHeight();
            int h = ns ? c.component.getHeight() : c.component.getWidth();
            for (int dx = 0; dx < w; dx++) {
                for (int dy = 0; dy < h; dy++) {
                    Long key = cellKey(c.x + dx, c.y + dy);
                    if (!newOcc.add(key)) {
                        return false;
                    }
                    if (!originals.contains(key) && editor.getBlock(c.x + dx, c.y + dy) != null) {
                        return false;
                    }
                }
            }
        }
        for (Long key : originals) {
            editor.setBlock(cellXOf(key), cellYOf(key), null);
        }
        for (CircuitDiagram.Component c : comps) {
            c.direction = opposite ? c.direction.getOpposite() : (clockwise ? c.direction.getClockWise() : c.direction.getCounterClockWise());
            editor.setBlock(c.x, c.y, c, true);
        }
        return true;
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
        if (Screen.hasAltDown() && !isReadOnly()) {
            if (activeComponent() != null) {
                if (scrollY > 0) {
                    rotation = rotation.getClockWise();
                    playSound(ROTATE_CLOCKWISE_SOUND);
                } else {
                    rotation = rotation.getCounterClockWise();
                    playSound(ROTATE_ANTICLOCKWISE_SOUND);
                }
                return true;
            }
            int gx = cellX(mouseX);
            int gy = cellY(mouseY);
            if (blockComponent(gx, gy) != null) {
                tryRotateAt(gx, gy, scrollY > 0);
                return true;
            }
        }
        double sx = scrollX;
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