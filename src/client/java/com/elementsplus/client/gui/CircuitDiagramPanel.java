package com.elementsplus.client.gui;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModDataComponents;
import com.elementsplus.ModItems;
import com.elementsplus.core.circuit.CircuitSimulator;
import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.CircuitComponent.PinType;
import com.elementsplus.core.circuit.component.CircuitComponentInstance;
import com.elementsplus.core.circuit.component.Input8ComponentInstance;
import com.elementsplus.core.circuit.component.InputComponentInstance;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.menu.LithographyMachineMenu;
import com.elementsplus.network.ReturnCarriedPayload;
import com.elementsplus.network.UpdateCircuitDiagramPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

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
    private static final int COLOR_MISMATCH_BANNER = 0xCCB0A020;
    private static final int COLOR_SELBOX = 0xFF40E0D0;
    private static final int COLOR_BOX_FILL = 0x3030D0C0;
    private static final int COLOR_BUS_8 = 0xFFFF00FF;
    private static final int COLOR_BUS_16 = 0xFFA000FF;
    private static final int COLOR_BUS_32 = 0xFF8000FF;
    private static final int COLOR_BUS_64 = 0xFF0000FF;

    /** 检查 tooltip 文本颜色（RGB）。 */
    private static final int COLOR_TEXT_NAME = 0xFFD700;
    private static final int COLOR_TEXT_BUS = 0xFF00FF;
    private static final int COLOR_TEXT_1BIT = 0xAAAAAA;
    private static final int COLOR_TEXT_COPPER = 0xB06030;
    private static final int COLOR_TEXT_GOLD = 0xFFD700;

    private static final Direction[] CLOCKWISE_SIDES = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    private static final double DRAG_THRESHOLD = 4.0;
    private static final double PAN_KEEP_VISIBLE = 16.0;
    private static final int COLOR_BOUNDS_BORDER = 0xFFB8B8B0;

    private static final ResourceLocation CYCLE_TEXTURE = ElementsPlus.id("textures/gui/cycle.png");
    private static final ResourceLocation MISMATCH_TEXTURE = ElementsPlus.id("textures/gui/bit_width_mismatch.png");

    private final LithographyMachineMenu menu;
    private final BooleanSupplier activeSupplier;

    private CircuitSimulator simulator;
    private boolean simulationDirty;

    /** 编译后（只读）模式下对「输入」元件信号值的客户端预览副本；绝不写回物品数据。 */
    private CircuitDiagram previewDiagram;
    private CircuitDiagram previewSource;

    private double offsetX = -6;
    private double offsetY = -6;
    private double zoom = DEFAULT_ZOOM;
    private boolean dragging;
    private boolean erasing;
    private int lastEraseX = Integer.MIN_VALUE;
    private int lastEraseY = Integer.MIN_VALUE;

    private Direction rotation = Direction.NORTH;
    private boolean placeMirrored = false;
    private ItemStack previewStack = ItemStack.EMPTY;
    private CircuitComponent virtualComponent;
    private CircuitDiagram.Wire.WireMaterial virtualWire;

    private CircuitDiagram.Wire.WireMaterial draggingWire;
    private int lastWireX;
    private int lastWireY;
    private boolean straightWireDrag;
    private int straightWireStartX;
    private int straightWireStartY;
    private int straightWireEndX;
    private int straightWireEndY;
    private boolean suppressWireSound;

    private int selectedX = Integer.MIN_VALUE;
    private int selectedY = Integer.MIN_VALUE;

    private int wirePlaceBitWidth = 1;

    public void setWireBitWidth(int bitWidth) {
        this.wirePlaceBitWidth = bitWidth > 1 ? 8 : 1;
    }

    private Integer lastMouseX;
    private Integer lastMouseY;

    private long lastInputClickTime = 0;
    private CircuitDiagram.Component lastInputClicked;

    private Consumer<CircuitDiagram.Component> onInput8DoubleClick = component -> {
    };

    /** 双击「8位输入」元件时的回调（由宿主 Screen 弹数值对话框）。 */
    public void setOnInput8DoubleClick(Consumer<CircuitDiagram.Component> consumer) {
        this.onInput8DoubleClick = consumer;
    }

    private enum DragMode {
        NONE, MOVE, COPY, BOX_SELECT
    }

    private final Set<Long> selectedCells = new HashSet<>();
    private int selBoxX, selBoxY, selBoxW, selBoxH;
    private boolean boxSelectionActive;
    private int selectionVersion;

    private static final int MAX_HISTORY = 100;
    private final Deque<CircuitDiagram> undoStack = new ArrayDeque<>();
    private final Deque<CircuitDiagram> redoStack = new ArrayDeque<>();
    private ItemStack historyStack = ItemStack.EMPTY;
    private CircuitDiagram historyStored;
    private CircuitDiagram historyBaseline;
    private boolean historyTransactionActive;
    private CircuitDiagram historyTransactionStart;
    private long historyGraceUntil;

    private boolean pressActive;
    private double pressScreenX, pressScreenY;
    private int pressCellX, pressCellY;
    private boolean pressOnComponent;
    private boolean pressSelected;
    private boolean pressOnSelectedWire;
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
    public final SoundInstance MIRROR_SOUND = SimpleSoundInstance.forUI(SoundEvents.COMPARATOR_CLICK, 0.6f, 1);

    public CircuitDiagramPanel(LithographyMachineMenu menu, BooleanSupplier activeSupplier, int x, int y, int width, int height) {
        super(x, y, width, height, net.minecraft.network.chat.Component.empty());
        this.menu = menu;
        this.activeSupplier = activeSupplier;
        if (menu.slots.size() > 36) {
            ensureHistory(menu.slots.get(36).getItem());
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean show = activeSupplier.getAsBoolean();
        this.active = show;
        if (!show) {
            return;
        }
        if (menu.slots.size() > 36) {
            ensureHistory(menu.slots.get(36).getItem());
        }

        if ((pressActive || dragMode != DragMode.NONE) && !isLeftMouseDown()) {
            finalizePress(lastMouseX != null ? lastMouseX : mouseX, lastMouseY != null ? lastMouseY : mouseY);
        }
        // 鼠标在控件外松开时可能收不到 mouseReleased，补一次导线提交/历史事务结束。
        if (draggingWire != null && !isLeftMouseDown()) {
            if (straightWireDrag) {
                updateStraightWireDrag(cellX(mouseX), cellY(mouseY));
                commitStraightWire();
            }
            draggingWire = null;
            straightWireDrag = false;
            endHistoryTransaction();
        }
        if (erasing && !isRightMouseDown()) {
            erasing = false;
            lastEraseX = Integer.MIN_VALUE;
            lastEraseY = Integer.MIN_VALUE;
            endHistoryTransaction();
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
            if (isMultiSelected() || hasSelectionBox() || dragMode != DragMode.NONE) {
                if (dragMode == DragMode.MOVE) {
                    drawSelBoxOutline(guiGraphics, selBoxX + dispDx, selBoxY + dispDy, selBoxW, selBoxH);
                } else {
                    drawSelBoxOutline(guiGraphics, selBoxX, selBoxY, selBoxW, selBoxH);
                }
            }
            drawBoundsBorder(guiGraphics);
            if (simulator != null && simulator.hasMismatchWarning()) {
                drawMismatchIcons(guiGraphics);
            }
            if (simulator != null && simulator.hasCycle()) {
                drawCycleBanner(guiGraphics);
            }
            if (simulator != null && simulator.hasMismatchWarning()) {
                drawMismatchBanner(guiGraphics);
            }
        }
        if (dragMode == DragMode.BOX_SELECT) {
            drawBoxSelection(guiGraphics);
        }
        drawGhost(guiGraphics, mouseX, mouseY);
        drawWirePreview(guiGraphics, mouseX, mouseY);
        guiGraphics.disableScissor();
    }

    private CircuitDiagram storedDiagram() {
        ItemStack stack = menu.slots.get(36).getItem();
        return stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
    }

    /**
     * 界面当前展示的电路图：只读模式下存在预览副本时优先返回预览，否则返回物品上存储的电路图。
     */
    private CircuitDiagram getDiagram() {
        CircuitDiagram stored = storedDiagram();
        if (previewDiagram != null && previewSource != stored) {
            previewDiagram = null;
            previewSource = null;
        }
        return previewDiagram != null ? previewDiagram : stored;
    }

    /** 供外部（如模拟器绑定）获取与界面展示一致的电路图。 */
    public CircuitDiagram getDisplayDiagram() {
        return getDiagram();
    }

    /**
     * 确保存在基于当前存储电路图的预览副本（编译后修改输入信号时调用）。
     * 副本与存储图是独立对象，后续修改只会影响显示与模拟，不会写回物品。
     */
    public void ensurePreview() {
        CircuitDiagram stored = storedDiagram();
        if (previewDiagram == null || previewSource != stored) {
            previewDiagram = stored == null ? null : stored.copy();
            previewSource = stored;
            simulationDirty = true;
        }
    }

    /** 预览内容已就地修改，要求下一帧重新绑定模拟器。 */
    public void requestPreviewResimulate() {
        simulationDirty = true;
    }

    private CircuitDiagram.Scale diagramScale() {
        CircuitDiagram diagram = getDiagram();
        return diagram == null ? null : diagram.scale;
    }

    /** 可用范围边长（单元格）；无限规模返回 -1。 */
    private int boundsSize() {
        CircuitDiagram.Scale scale = diagramScale();
        return scale == null ? -1 : scale.sizeInCells();
    }

    private boolean hasBounds() {
        int size = boundsSize();
        return size > 0;
    }

    private boolean inBounds(int x, int y) {
        int size = boundsSize();
        return size < 0 || (x >= 0 && y >= 0 && x < size && y < size);
    }

    private boolean placementInBounds(int gx, int gy, int w, int h) {
        int size = boundsSize();
        return size < 0 || (gx >= 0 && gy >= 0 && gx + w <= size && gy + h <= size);
    }

    /**
     * 平移（中键拖动/滚轮）后把视图钳制回可用范围内，保证可用范围不会完全
     * 从视野中消失；无限规模不受限。
     */
    private void clampPan() {
        int size = boundsSize();
        if (size < 0) {
            return;
        }
        double minX = -(getWidth() - PAN_KEEP_VISIBLE) / zoom;
        double maxX = size - PAN_KEEP_VISIBLE / zoom;
        double minY = -(getHeight() - PAN_KEEP_VISIBLE) / zoom;
        double maxY = size - PAN_KEEP_VISIBLE / zoom;
        if (minX > maxX) {
            double midX = (minX + maxX) / 2.0;
            minX = midX;
            maxX = midX;
        }
        if (minY > maxY) {
            double midY = (minY + maxY) / 2.0;
            minY = midY;
            maxY = midY;
        }
        offsetX = Mth.clamp(offsetX, minX, maxX);
        offsetY = Mth.clamp(offsetY, minY, maxY);
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

    /** 是否存在框选区域（可能只包含导线，此时 selectedCells 为空）。 */
    public boolean hasSelectionBox() {
        return boxSelectionActive && selBoxW > 0 && selBoxH > 0;
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

    /** 当前选框内是否至少有一格导线。 */
    private boolean hasWireInBox(CircuitDiagram diagram) {
        if (diagram == null || selBoxW <= 0 || selBoxH <= 0) {
            return false;
        }
        for (Map.Entry<Long, CircuitDiagram.Chunk> entry : diagram.chunks.entrySet()) {
            CircuitDiagram.Chunk chunk = entry.getValue();
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    CircuitDiagram.Wire wire = chunk.wires[i][j];
                    if (wire != null && insideBox(wire.x, wire.y, selBoxX, selBoxY, selBoxW, selBoxH)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void bumpSelection() {
        selectionVersion++;
    }

    private void clearSelection() {
        selectedCells.clear();
        boxSelectionActive = false;
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
        boxSelectionActive = false;
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
     * 若当前是框选，则保留原拖拽矩形并与元件范围取并集，避免选框收缩后忽略外侧导线。
     */
    private void updateSelBox() {
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
            if (!boxSelectionActive) {
                selBoxW = 0;
                selBoxH = 0;
            }
            return;
        }
        if (boxSelectionActive && selBoxW > 0 && selBoxH > 0) {
            minX = Math.min(minX, selBoxX);
            minY = Math.min(minY, selBoxY);
            maxX = Math.max(maxX, selBoxX + selBoxW - 1);
            maxY = Math.max(maxY, selBoxY + selBoxH - 1);
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
            if (component.component == BuiltinCircuitComponents.INPUT || component.component == BuiltinCircuitComponents.INPUT_8) {
                long now = Util.getMillis();
                if (lastInputClicked == component && now - lastInputClickTime <= 300) {
                    if (component.component == BuiltinCircuitComponents.INPUT) {
                        toggleInputSignal(component);
                    } else {
                        onInput8DoubleClick.accept(component);
                    }
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
        if (isReadOnly()) {
            ensurePreview();
            CircuitDiagram diagram = getDiagram();
            if (diagram == null) {
                return;
            }
            CircuitDiagram.Block b = diagram.getBlock(component.x, component.y);
            if (b instanceof CircuitDiagram.Component ec && ec.instance instanceof InputComponentInstance in) {
                in.setSignal(in.getSignal() == 0 ? 15 : 0);
            }
            setSelectionSingle(component.x, component.y);
            simulationDirty = true;
            return;
        }
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

    /** 界面当前展示的电路图中「8位输入」元件的 0~255 值。 */
    public int getInput8Value(CircuitDiagram.Component component) {
        CircuitDiagram diagram = getDiagram();
        if (diagram == null) {
            return 0;
        }
        CircuitDiagram.Block b = diagram.getBlock(component.x, component.y);
        if (b instanceof CircuitDiagram.Component ec && ec.instance instanceof Input8ComponentInstance in8) {
            return in8.getValue();
        }
        return 0;
    }

    /** 把 0~255 值写入「8位输入」元件：只读模式仅改预览副本，编辑模式写回物品并提交。 */
    public void applyInput8Value(CircuitDiagram.Component component, int value) {
        if (isReadOnly()) {
            ensurePreview();
            CircuitDiagram diagram = getDiagram();
            if (diagram == null) {
                return;
            }
            CircuitDiagram.Block b = diagram.getBlock(component.x, component.y);
            if (b instanceof CircuitDiagram.Component ec && ec.instance instanceof Input8ComponentInstance in8) {
                in8.setValue(value);
            }
            setSelectionSingle(component.x, component.y);
            simulationDirty = true;
            return;
        }
        ItemStack stack = menu.slots.get(36).getItem();
        CircuitDiagram diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null) {
            return;
        }
        CircuitDiagram editor = diagram.copy();
        CircuitDiagram.Block b = editor.getBlock(component.x, component.y);
        if (b instanceof CircuitDiagram.Component ec && ec.instance instanceof Input8ComponentInstance in8) {
            in8.setValue(value);
        }
        setSelectionSingle(component.x, component.y);
        commitDiagram(stack, editor);
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
        boolean conflict = !placementInBounds(gx, gy, w, h) || diagramHasConflict(gx, gy, w, h);
        guiGraphics.fill(lx, ty, rx, by, conflict ? COLOR_GHOST_INVALID : COLOR_GHOST_VALID);
        if (zoom >= 3) {
            guiGraphics.fill(lx, ty, rx, ty + 1, COLOR_GHOST_BORDER);
            guiGraphics.fill(lx, by - 1, rx, by, COLOR_GHOST_BORDER);
            guiGraphics.fill(lx, ty, lx + 1, by, COLOR_GHOST_BORDER);
            guiGraphics.fill(rx - 1, ty, rx, by, COLOR_GHOST_BORDER);
        }
        int ghostColor = conflict ? COLOR_GHOST_INVALID : COLOR_GHOST_VALID;
        float ghostAlpha = ((ghostColor >> 24) & 0xFF) / 255.0F;
        drawPins(guiGraphics, component, rotation, placeMirrored, left, top, ghostAlpha);
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
        if (!placementInBounds(gx, gy, w, h)) {
            playSound(INVALID_SOUND);
            return;
        }
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
        CircuitDiagram.Component placed = new CircuitDiagram.Component(component, rotation);
        placed.mirrored = placeMirrored;
        if (editor.setBlock(gx, gy, placed, force)) {
            playSound(COMPONENT_PLACE_SOUND);
            setSelectionSingle(gx, gy);
        }
        commitDiagram(stack, editor);
    }

    private void tryErase(int gx, int gy, double mouseX, double mouseY) {
        ItemStack stack = menu.slots.get(36).getItem();
        CircuitDiagram diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null) {
            return;
        }
        CircuitDiagram.Block block = diagram.getBlock(gx, gy);
        if (block == null) {
            return;
        }
        if (block instanceof CircuitDiagram.Wire && Screen.hasShiftDown()) {
            eraseWireNet(stack, diagram, gx, gy, mouseX, mouseY);
            return;
        }
        CircuitDiagram editor = diagram.copy();
        if (block instanceof CircuitDiagram.Component compBlock) {
            boolean wasSelected = selectedCells.remove(cellKey(compBlock.x, compBlock.y));
            if (wasSelected) {
                if (selectedCells.isEmpty()) {
                    setSelected(Integer.MIN_VALUE, Integer.MIN_VALUE);
                    if (!boxSelectionActive || !hasWireInBox(editor)) {
                        selBoxW = 0;
                        selBoxH = 0;
                        boxSelectionActive = false;
                    }
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
     * Shift+右键导线：沿同线材、同位宽的连接关系删除整个线网。
     * 同一格可能有铜线/金线（甚至不同位宽）交叉，按鼠标最近的导线侧决定要删除的网络。
     */
    private void eraseWireNet(ItemStack stack, CircuitDiagram diagram, int gx, int gy, double mouseX, double mouseY) {
        CircuitDiagram.Block startBlock = diagram.getBlock(gx, gy);
        if (!(startBlock instanceof CircuitDiagram.Wire startWire)) {
            return;
        }
        Direction startSide = nearestWireSide(startWire, mouseX, mouseY, gx, gy);
        if (startSide == null) {
            return;
        }
        CircuitDiagram.Wire.WireMaterial material = materialOf(startWire, startSide);
        if (material == null) {
            return;
        }
        int bitWidth = startWire.getBitWidth(material);
        CircuitDiagram editor = diagram.copy();

        Set<Long> visited = new HashSet<>();
        List<Long> queue = new ArrayList<>();
        long startKey = cellKey(gx, gy);
        visited.add(startKey);
        queue.add(startKey);
        for (int i = 0; i < queue.size(); i++) {
            long key = queue.get(i);
            int x = cellXOf(key);
            int y = cellYOf(key);
            CircuitDiagram.Wire wire = wireAt(editor, x, y);
            if (wire == null || !wire.hasMaterial(material) || wire.getBitWidth(material) != bitWidth) {
                continue;
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (materialOf(wire, d) != material) {
                    continue;
                }
                int nx = x + d.getStepX();
                int ny = y + d.getStepZ();
                CircuitDiagram.Wire neighbor = wireAt(editor, nx, ny);
                if (neighbor == null
                        || neighbor.getBitWidth(material) != bitWidth
                        || materialOf(neighbor, d.getOpposite()) != material) {
                    continue;
                }
                long neighborKey = cellKey(nx, ny);
                if (visited.add(neighborKey)) {
                    queue.add(neighborKey);
                }
            }
        }

        boolean removed = false;
        for (long key : visited) {
            int x = cellXOf(key);
            int y = cellYOf(key);
            CircuitDiagram.Wire wire = wireAt(editor, x, y);
            if (wire == null) {
                continue;
            }
            if (wire.north == material) {
                wire.north = null;
                removed = true;
            }
            if (wire.east == material) {
                wire.east = null;
                removed = true;
            }
            if (wire.south == material) {
                wire.south = null;
                removed = true;
            }
            if (wire.west == material) {
                wire.west = null;
                removed = true;
            }
            if (wire.north == null && wire.east == null && wire.south == null && wire.west == null) {
                editor.setBlock(x, y, null);
            }
        }
        if (!removed) {
            return;
        }
        playSound(WIRE_ERASE_SOUND);
        commitDiagram(stack, editor);
    }

    private static CircuitDiagram.Wire wireAt(CircuitDiagram diagram, int x, int y) {
        CircuitDiagram.Block block = diagram.getBlock(x, y);
        return block instanceof CircuitDiagram.Wire wire ? wire : null;
    }

    /**
     * 根据右键位置选择该格中距离鼠标最近的、实际有导线的方向。
     */
    private Direction nearestWireSide(CircuitDiagram.Wire wire, double mouseX, double mouseY, int gx, int gy) {
        double centerX = getX() + (gx + 0.5 - offsetX) * zoom;
        double centerY = getY() + (gy + 0.5 - offsetY) * zoom;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        Direction best = null;
        double bestDot = Double.NEGATIVE_INFINITY;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (materialOf(wire, d) == null) {
                continue;
            }
            double dot = dx * d.getStepX() + dy * d.getStepZ();
            if (dot > bestDot) {
                bestDot = dot;
                best = d;
            }
        }
        return best;
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
        if (editor == null) {
            return;
        }
        ensureHistory(stack);
        stack.set(ModDataComponents.CIRCUIT_DIAGRAM, editor);
        if (!historyTransactionActive) {
            recordHistory(editor);
        }
        historyStored = editor;
        historyGraceUntil = Util.getMillis() + 250L;
        simulationDirty = true;
        menu.onDiagramChanged();
        ClientPlayNetworking.send(new UpdateCircuitDiagramPayload(editor));
    }

    /** 当物品栈或存储的电路图对象发生变化时，维护/清空历史，避免撤销到另一张电路图。 */
    private void ensureHistory(ItemStack stack) {
        CircuitDiagram current = stack == null || stack.isEmpty()
                ? null
                : stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (stack == historyStack && current == historyStored) {
            return;
        }
        // 服务端可能把同一张图同步回来（新的 ItemStack / CircuitDiagram 对象）。
        // 内容仍等于最近一次本地状态或最近一次提交状态时，保留历史。
        boolean sameItemType = !historyStack.isEmpty() && stack != null && !stack.isEmpty()
                && stack.is(historyStack.getItem());
        if (sameItemType && (Util.getMillis() < historyGraceUntil
                || historyTransactionActive
                || sameDiagram(current, historyStored)
                || sameDiagram(current, historyBaseline))) {
            historyStack = stack;
            historyStored = current;
            return;
        }
        historyStack = stack;
        historyStored = current;
        historyBaseline = current == null ? null : current.copy();
        historyGraceUntil = 0L;
        undoStack.clear();
        redoStack.clear();
        historyTransactionActive = false;
        historyTransactionStart = null;
    }

    private void recordHistory(CircuitDiagram editor) {
        if (editor == null) {
            return;
        }
        if (historyBaseline == null) {
            historyBaseline = editor.copy();
            return;
        }
        if (sameDiagram(historyBaseline, editor)) {
            historyBaseline = editor.copy();
            return;
        }
        pushUndo(historyBaseline);
        historyBaseline = editor.copy();
        redoStack.clear();
    }

    private void beginHistoryTransaction() {
        if (historyTransactionActive) {
            return;
        }
        ItemStack stack = menu.slots.get(36).getItem();
        ensureHistory(stack);
        historyTransactionActive = true;
        historyTransactionStart = historyBaseline == null ? null : historyBaseline.copy();
    }

    private void endHistoryTransaction() {
        if (!historyTransactionActive) {
            return;
        }
        historyTransactionActive = false;
        historyGraceUntil = Util.getMillis() + 250L;
        CircuitDiagram start = historyTransactionStart;
        historyTransactionStart = null;
        ItemStack stack = menu.slots.get(36).getItem();
        CircuitDiagram current = stack.isEmpty() ? null : stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        historyStored = current;
        if (current == null) {
            return;
        }
        if (start == null) {
            historyBaseline = current.copy();
            return;
        }
        if (sameDiagram(start, current)) {
            historyBaseline = current.copy();
            return;
        }
        pushUndo(start);
        historyBaseline = current.copy();
        redoStack.clear();
    }

    private void pushUndo(CircuitDiagram state) {
        if (state == null) {
            return;
        }
        undoStack.addLast(state);
        while (undoStack.size() > MAX_HISTORY) {
            undoStack.removeFirst();
        }
    }

    private void pushRedo(CircuitDiagram state) {
        if (state == null) {
            return;
        }
        redoStack.addLast(state);
        while (redoStack.size() > MAX_HISTORY) {
            redoStack.removeFirst();
        }
    }

    private static boolean sameDiagram(CircuitDiagram a, CircuitDiagram b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        try {
            return Arrays.equals(diagramSignature(a), diagramSignature(b));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** 与序列化顺序无关的电路图内容签名，用于判断服务端同步/空提交是否真的改变了电路。 */
    private static byte[] diagramSignature(CircuitDiagram diagram) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeVarInt(diagram.scale.ordinal());
            List<Map.Entry<Long, CircuitDiagram.Chunk>> chunks = new ArrayList<>(diagram.chunks.entrySet());
            chunks.sort(Comparator.comparingLong(entry -> entry.getKey()));
            buf.writeVarInt(chunks.size());
            for (Map.Entry<Long, CircuitDiagram.Chunk> entry : chunks) {
                long key = entry.getKey();
                buf.writeVarInt((int) (key >> 32));
                buf.writeVarInt((int) (key & 0xFFFFFFFFL));
                CircuitDiagram.Chunk chunk = entry.getValue();

                List<CircuitDiagram.Wire> wires = new ArrayList<>();
                for (CircuitDiagram.Wire[] row : chunk.wires) {
                    for (CircuitDiagram.Wire wire : row) {
                        if (wire != null) {
                            wires.add(wire);
                        }
                    }
                }
                wires.sort(Comparator.comparingInt((CircuitDiagram.Wire wire) -> wire.x)
                        .thenComparingInt(wire -> wire.y));
                buf.writeVarInt(wires.size());
                for (CircuitDiagram.Wire wire : wires) {
                    buf.writeVarInt(wire.x);
                    buf.writeVarInt(wire.y);
                    writeSignatureMaterial(buf, wire.north);
                    writeSignatureMaterial(buf, wire.east);
                    writeSignatureMaterial(buf, wire.south);
                    writeSignatureMaterial(buf, wire.west);
                    buf.writeVarInt(wire.copperBitWidth);
                    buf.writeVarInt(wire.goldBitWidth);
                }

                List<CircuitDiagram.Component> components = new ArrayList<>(chunk.components);
                components.sort(Comparator.comparingInt((CircuitDiagram.Component component) -> component.x)
                        .thenComparingInt(component -> component.y)
                        .thenComparing(component -> component.component.getId() == null
                                ? ""
                                : component.component.getId().toString()));
                buf.writeVarInt(components.size());
                for (CircuitDiagram.Component component : components) {
                    buf.writeVarInt(component.x);
                    buf.writeVarInt(component.y);
                    buf.writeVarInt(component.direction.ordinal());
                    buf.writeBoolean(component.mirrored);
                    ResourceLocation id = component.component.getId();
                    buf.writeUtf(id == null ? "" : id.toString());
                    List<CircuitComponentInstance.Config> configs = new ArrayList<>(component.instance.getConfigs());
                    configs.sort(Comparator.comparing(config -> config.key));
                    buf.writeVarInt(configs.size());
                    for (CircuitComponentInstance.Config config : configs) {
                        buf.writeUtf(config.key);
                        if (config instanceof CircuitComponentInstance.IntConfig) {
                            buf.writeByte(0);
                            buf.writeInt(component.instance.getInt(config.key));
                        } else if (config instanceof CircuitComponentInstance.FloatConfig) {
                            buf.writeByte(1);
                            buf.writeInt(Float.floatToIntBits(component.instance.getFloat(config.key)));
                        } else if (config instanceof CircuitComponentInstance.StringConfig) {
                            buf.writeByte(2);
                            buf.writeUtf(component.instance.getString(config.key));
                        } else {
                            buf.writeByte(3);
                        }
                    }
                }
            }
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } finally {
            buf.release();
        }
    }

    private static void writeSignatureMaterial(FriendlyByteBuf buf, CircuitDiagram.Wire.WireMaterial material) {
        buf.writeByte(material == null ? -1 : material.ordinal());
    }

    public boolean undo() {
        if (isReadOnly() || pressActive || draggingWire != null || erasing
                || isLeftMouseDown() || isRightMouseDown()) {
            return false;
        }
        if (historyTransactionActive) {
            endHistoryTransaction();
        }
        ItemStack stack = menu.slots.get(36).getItem();
        ensureHistory(stack);
        if (undoStack.isEmpty()) {
            return false;
        }
        CircuitDiagram current = stack.isEmpty() ? null : stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (current == null) {
            return false;
        }
        if (historyBaseline == null || !sameDiagram(current, historyBaseline)) {
            // 当前显示状态已不是历史基线（换图/外部同步/未提交修改），放弃旧历史，避免撤错图。
            historyBaseline = current.copy();
            historyStored = current;
            undoStack.clear();
            redoStack.clear();
            return false;
        }
        CircuitDiagram currentState = historyBaseline.copy();
        CircuitDiagram target = undoStack.removeLast();
        pushRedo(currentState);
        applyHistoryState(stack, target);
        return true;
    }

    public boolean redo() {
        if (isReadOnly() || pressActive || draggingWire != null || erasing
                || isLeftMouseDown() || isRightMouseDown()) {
            return false;
        }
        if (historyTransactionActive) {
            endHistoryTransaction();
        }
        ItemStack stack = menu.slots.get(36).getItem();
        ensureHistory(stack);
        if (redoStack.isEmpty()) {
            return false;
        }
        CircuitDiagram current = stack.isEmpty() ? null : stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (current == null) {
            return false;
        }
        if (historyBaseline == null || !sameDiagram(current, historyBaseline)) {
            historyBaseline = current.copy();
            historyStored = current;
            undoStack.clear();
            redoStack.clear();
            return false;
        }
        CircuitDiagram currentState = historyBaseline.copy();
        CircuitDiagram target = redoStack.removeLast();
        pushUndo(currentState);
        applyHistoryState(stack, target);
        return true;
    }

    private void applyHistoryState(ItemStack stack, CircuitDiagram state) {
        CircuitDiagram copy = state.copy();
        stack.set(ModDataComponents.CIRCUIT_DIAGRAM, copy);
        historyStored = copy;
        historyBaseline = copy.copy();
        historyGraceUntil = Util.getMillis() + 250L;
        simulationDirty = true;
        clearSelection();
        menu.onDiagramChanged();
        ClientPlayNetworking.send(new UpdateCircuitDiagramPayload(copy));
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
        int color = wirePlaceBitWidth == 8 ? COLOR_BUS_8 : wireColor(material);
        color = (color & 0x00FFFFFF) | 0x80000000;
        int size = Math.max(2, (int) Math.round(zoom * 0.4));
        int gx;
        int gy;
        if (straightWireDrag) {
            if (!inBounds(straightWireStartX, straightWireStartY)) {
                return;
            }
            gx = straightWireEndX;
            gy = straightWireEndY;
            if (!inBounds(gx, gy)) {
                return;
            }
            if (gx != straightWireStartX || gy != straightWireStartY) {
                double startX = getX() + (straightWireStartX + 0.5 - offsetX) * zoom;
                double startY = getY() + (straightWireStartY + 0.5 - offsetY) * zoom;
                double endX = getX() + (gx + 0.5 - offsetX) * zoom;
                double endY = getY() + (gy + 0.5 - offsetY) * zoom;
                // 与导线虚影点使用相同的线宽和半透明颜色。
                drawSegment(guiGraphics, startX, startY, endX, endY, Math.max(1, size / 2), color);
            }
        } else {
            gx = cellX(mouseX);
            gy = cellY(mouseY);
            if (!inBounds(gx, gy)) {
                return;
            }
        }
        double centerX = getX() + (gx + 0.5 - offsetX) * zoom;
        double centerY = getY() + (gy + 0.5 - offsetY) * zoom;
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

    private void updateStraightWireDrag(int toX, int toY) {
        int size = boundsSize();
        if (size > 0) {
            toX = Mth.clamp(toX, 0, size - 1);
            toY = Mth.clamp(toY, 0, size - 1);
        }
        int dx = toX - straightWireStartX;
        int dy = toY - straightWireStartY;
        if (Math.abs(dx) >= Math.abs(dy)) {
            straightWireEndX = toX;
            straightWireEndY = straightWireStartY;
        } else {
            straightWireEndX = straightWireStartX;
            straightWireEndY = toY;
        }
    }

    private void commitStraightWire() {
        if (draggingWire == null
                || (straightWireStartX == straightWireEndX && straightWireStartY == straightWireEndY)
                || !inBounds(straightWireStartX, straightWireStartY)
                || !inBounds(straightWireEndX, straightWireEndY)) {
            return;
        }
        ItemStack stack = menu.slots.get(36).getItem();
        CircuitDiagram diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null) {
            return;
        }
        CircuitDiagram editor = diagram.copy();
        boolean previousSuppress = suppressWireSound;
        suppressWireSound = true;
        try {
            int x = straightWireStartX;
            int y = straightWireStartY;
            while (x != straightWireEndX || y != straightWireEndY) {
                int dx = Integer.signum(straightWireEndX - x);
                int dy = Integer.signum(straightWireEndY - y);
                if (dx != 0 && dy != 0) {
                    if (Math.abs(straightWireEndX - x) >= Math.abs(straightWireEndY - y)) {
                        dy = 0;
                    } else {
                        dx = 0;
                    }
                }
                applyConnection(editor, x, y, x + dx, y + dy, draggingWire);
                x += dx;
                y += dy;
            }
        } finally {
            suppressWireSound = previousSuppress;
        }
        playSound(WIRE_PLACE_SOUND);
        commitDiagram(stack, editor);
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
        if (!inBounds(fromX, fromY) || !inBounds(toX, toY)) {
            return;
        }
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
        if (!suppressWireSound) {
            playSound(WIRE_PLACE_SOUND);
        }
        // 位宽按材质分别记录：仅在该材质首次接入本格时写入当前放置位宽，
        // 这样 8 位铜线与 1 位金线可以在同一格内共存。
        boolean newMaterial = material != null && !wire.hasMaterial(material);
        switch (side) {
            case NORTH -> wire.north = material;
            case EAST -> wire.east = material;
            case SOUTH -> wire.south = material;
            case WEST -> wire.west = material;
        }
        if (newMaterial) {
            wire.setBitWidth(material, wirePlaceBitWidth);
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
                if (!inBounds(gx, gy)) {
                    continue;
                }
                int sx = getX() + (int) Math.floor((gx - offsetX) * zoom);
                int sy = getY() + (int) Math.floor((gy - offsetY) * zoom);
                guiGraphics.fill(sx, sy, sx + size, sy + size, colorDot);
            }
        }
    }

    /**
     * 绘制可用范围边框：单元格 (0,0)~(size,size) 围成的矩形四边。
     */
    private void drawBoundsBorder(GuiGraphics guiGraphics) {
        int size = boundsSize();
        if (size < 0) {
            return;
        }
        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + getWidth();
        int y1 = y0 + getHeight();
        double left = x0 + (0.0 - offsetX) * zoom;
        double top = y0 + (0.0 - offsetY) * zoom;
        double right = x0 + (size - offsetX) * zoom;
        double bottom = y0 + (size - offsetY) * zoom;
        int lx = Mth.clamp((int) Math.floor(left), x0, x1);
        int ty = Mth.clamp((int) Math.floor(top), y0, y1);
        int rx = Mth.clamp((int) Math.ceil(right), x0, x1);
        int by = Mth.clamp((int) Math.ceil(bottom), y0, y1);
        if (rx - lx < 1 || by - ty < 1) {
            return;
        }
        int c = COLOR_BOUNDS_BORDER;
        guiGraphics.fill(lx, ty, rx, ty + 2, c);
        guiGraphics.fill(lx, by - 2, rx, by, c);
        guiGraphics.fill(lx, ty, lx + 2, by, c);
        guiGraphics.fill(rx - 2, ty, rx, by, c);
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
        drawPins(guiGraphics, component.component, component.direction, component.mirrored, left, top, 1.0F);
        drawIcon(guiGraphics, component.component, (left + right) / 2.0, (top + bottom) / 2.0, right - left, bottom - top, 1.0F);
        var font = Minecraft.getInstance().font;
        if (dx == 0 && dy == 0) {
            if (component.component == BuiltinCircuitComponents.INPUT || component.component == BuiltinCircuitComponents.OUTPUT) {
                int value = simulator != null ? simulator.getComponentValue(component.x, component.y) : 0;
                String text = String.valueOf(value);
                guiGraphics.drawString(font, Component.nullToEmpty(text),
                        (int) Math.round((left + right) / 2.0 - font.width(text) / 2.0),
                        (int) Math.round((top + bottom) / 2.0 - 4),
                        0xFFFFFFFF);
            } else if (component.component == BuiltinCircuitComponents.INPUT_8 || component.component == BuiltinCircuitComponents.OUTPUT_8) {
                int value = simulator != null ? simulator.getComponentBusValue(component.x, component.y) : 0;
                String text = String.valueOf(value);
                guiGraphics.drawString(font, Component.nullToEmpty(text),
                        (int) Math.round((left + right) / 2.0 - font.width(text) / 2.0),
                        (int) Math.round((top + bottom) / 2.0 - 4),
                        COLOR_BUS_8);
            }
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
        drawPins(guiGraphics, component.component, component.direction, component.mirrored, left, top, alpha);
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
     * 先按 direction 旋转（NORTH=原始方向，EAST/SOUTH/WEST=顺时针 90/180/270 度），
     * 再按 mirrored 在屏幕上做水平（左右）镜像（只影响引脚，不影响图标）。
     */
    private void drawPins(GuiGraphics g, CircuitComponent component, Direction direction,
                          boolean mirrored, double left, double top, float alpha) {
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
                double[] local = CircuitComponent.rotatedPinOffset(w0, h0, direction, mirrored, side, k);
                double rx = local[0];
                double ry = local[1];
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

            // 内层：有信号变红（0~15 对应不同亮度），坏网保持黑色；
            // 8 位总线内芯始终为总线色，不随信号变化
            if (thickness > 1) {
                int innerColor;
                if (bad) {
                    innerColor = 0xFF000000;
                } else if (wire.getBitWidth(target) == 8) {
                    innerColor = COLOR_BUS_8;
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
        guiGraphics.drawString(Minecraft.getInstance().font, text, getX() + 6, y + 3, 0xFFFFFFFF);
    }

    /**
     * 位宽不匹配告警：在 1 位/8 位引脚或导线宽度冲突的接缝处叠加提示贴图。
     * 接缝中点由 simulator 编码为 seam key（见 CircuitSimulator#seamKey）：
     * 水平接缝中点 (x0+0.5, y0)，垂直接缝中点 (x0, y0+0.5)。
     */
    private void drawMismatchIcons(GuiGraphics guiGraphics) {
        Set<Long> seams = simulator.getMismatchCells();
        if (seams.isEmpty()) {
            return;
        }
        int size = Mth.clamp((int) Math.round(zoom * 0.6F), 6, 16);
        for (Long seam : seams) {
            int sx = (int) (seam >> 33);
            int sy = (int) ((seam >> 1) & 0xFFFFFFFFL);
            boolean vert = (seam & 1) == 1;
            double cx = getX() + (sx + (vert ? 0.0 : 0.5) - offsetX) * zoom;
            double cy = getY() + (sy + (vert ? 0.5 : 0.0) - offsetY) * zoom;
            if (cx < getX() - size || cx > getX() + getWidth() + size
                    || cy < getY() - size || cy > getY() + getHeight() + size) {
                continue;
            }
            int px = (int) Math.round(cx - size / 2.0);
            int py = (int) Math.round(cy - size / 2.0);
            guiGraphics.blit(MISMATCH_TEXTURE, px, py, size, size, 0.0F, 0.0F, 16, 16, 16, 16);
        }
    }

    private void drawMismatchBanner(GuiGraphics guiGraphics) {
        int bannerHeight = 14;
        int y = getY() + getHeight() - bannerHeight - 2;
        if (simulator.hasCycle()) {
            y -= bannerHeight + 2;
        }
        guiGraphics.fill(getX() + 2, y, getX() + getWidth() - 2, y + bannerHeight, COLOR_MISMATCH_BANNER);
        Component text = Component.translatable("gui.elements-plus.lithography_machine.bit_width_mismatch");
        guiGraphics.drawString(Minecraft.getInstance().font, text, getX() + 6, y + 3, 0xFFFFFFFF);
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
                        beginHistoryTransaction();
                        straightWireDrag = Screen.hasControlDown();
                        if (straightWireDrag) {
                            straightWireStartX = cellX(mouseX);
                            straightWireStartY = cellY(mouseY);
                            straightWireEndX = straightWireStartX;
                            straightWireEndY = straightWireStartY;
                        } else {
                            lastWireX = cellX(mouseX);
                            lastWireY = cellY(mouseY);
                        }
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
            CircuitDiagram pressDiagram = getDiagram();
            pressOnSelectedWire = hasSelectionBox()
                    && insideBox(gx, gy, selBoxX, selBoxY, selBoxW, selBoxH)
                    && pressDiagram != null
                    && wireAt(pressDiagram, gx, gy) != null;
            pressCtrl = Screen.hasControlDown();
            dragMode = DragMode.NONE;
            dispDx = 0;
            dispDy = 0;
            return true;
        }
        if (button == 1) {
            // Shift+右键导线优先进入整网擦除，即使当前仍选中了导线/元件工具。
            if (Screen.hasShiftDown() && !isReadOnly()) {
                CircuitDiagram diagram = getDiagram();
                if (diagram != null) {
                    int gx = cellX(mouseX);
                    int gy = cellY(mouseY);
                    if (diagram.getBlock(gx, gy) instanceof CircuitDiagram.Wire) {
                        erasing = true;
                        beginHistoryTransaction();
                        lastEraseX = gx;
                        lastEraseY = gy;
                        tryErase(gx, gy, mouseX, mouseY);
                        return true;
                    }
                }
            }
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
                int gx = cellX(mouseX);
                int gy = cellY(mouseY);
                erasing = true;
                beginHistoryTransaction();
                lastEraseX = gx;
                lastEraseY = gy;
                tryErase(gx, gy, mouseX, mouseY);
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
            lastEraseX = Integer.MIN_VALUE;
            lastEraseY = Integer.MIN_VALUE;
            endHistoryTransaction();
        }
        if (button == 0) {
            if (draggingWire != null && straightWireDrag) {
                updateStraightWireDrag(cellX(mouseX), cellY(mouseY));
                commitStraightWire();
            }
            draggingWire = null;
            straightWireDrag = false;
            if (pressActive || dragMode != DragMode.NONE) {
                finalizePress((int) mouseX, (int) mouseY);
            }
            endHistoryTransaction();
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
            clampPan();
        }
        if (erasing) {
            int eraseX = cellX(mouseX);
            int eraseY = cellY(mouseY);
            if (eraseX != lastEraseX || eraseY != lastEraseY) {
                lastEraseX = eraseX;
                lastEraseY = eraseY;
                tryErase(eraseX, eraseY, mouseX, mouseY);
            }
        }
        if (draggingWire != null) {
            if (straightWireDrag) {
                updateStraightWireDrag(cellX(mouseX), cellY(mouseY));
            } else {
                stepWireDrag(cellX(mouseX), cellY(mouseY));
            }
        }
    }

    private boolean isLeftMouseDown() {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT)
                == GLFW.GLFW_PRESS;
    }

    private boolean isRightMouseDown() {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                == GLFW.GLFW_PRESS;
    }

    private void startDrag() {
        if (isReadOnly()) {
            return;
        }
        boolean groupPress = (pressOnComponent && (pressCtrl || pressSelected)) || pressOnSelectedWire;
        if (groupPress) {
            beginGroupDrag();
            if (dragComps.isEmpty() && dragWires.isEmpty()) {
                dragMode = DragMode.NONE;
                return;
            }
            dragMode = pressCtrl ? DragMode.COPY : DragMode.MOVE;
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
                    if (!inBounds(x, y)) {
                        return false;
                    }
                    if (!dragOwnedCells.contains(cellKey(x, y)) && diagram.getBlock(x, y) != null) {
                        return false;
                    }
                }
            }
        }
        for (CircuitDiagram.Wire w : dragWires) {
            int x = w.x + dx;
            int y = w.y + dy;
            if (!inBounds(x, y)) {
                return false;
            }
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
                    int x = c.x + dx0 + dx;
                    int y = c.y + dy0 + dy;
                    if (!inBounds(x, y)) {
                        return false;
                    }
                    if (diagram.getBlock(x, y) != null) {
                        return false;
                    }
                }
            }
        }
        for (CircuitDiagram.Wire w : dragWires) {
            int x = w.x + dx;
            int y = w.y + dy;
            if (!inBounds(x, y)) {
                return false;
            }
            if (diagram.getBlock(x, y) != null) {
                return false;
            }
        }
        return true;
    }

    private void updateDragOffset(double mouseX, double mouseY) {
        int dx = cellX(mouseX) - pressCellX;
        int dy = cellY(mouseY) - pressCellY;
        if (Screen.hasShiftDown()) {
            int[] clamped = clampGroupOffset(dx, dy);
            dispDx = clamped[0];
            dispDy = clamped[1];
            return;
        }
        if (groupMoveValid(dx, dy)) {
            dispDx = dx;
            dispDy = dy;
        }
    }

    private void updateCopyOffset(double mouseX, double mouseY) {
        int dx = cellX(mouseX) - pressCellX;
        int dy = cellY(mouseY) - pressCellY;
        if (Screen.hasShiftDown()) {
            int[] clamped = clampGroupOffset(dx, dy);
            dispDx = clamped[0];
            dispDy = clamped[1];
            return;
        }
        dispDx = dx;
        dispDy = dy;
    }

    /**
     * 把选中框整体位移限制在可用范围内，保证拖动（含 Shift）不会越界。
     */
    private int[] clampGroupOffset(int dx, int dy) {
        if (!hasBounds()) {
            return new int[]{dx, dy};
        }
        dx = clampAxis(dx, dragBoxX, dragBoxW);
        dy = clampAxis(dy, dragBoxY, dragBoxH);
        return new int[]{dx, dy};
    }

    private int clampAxis(int d, int boxStart, int boxSpan) {
        int size = boundsSize();
        if (size < 0 || boxSpan <= 0) {
            return d;
        }
        int min = -boxStart;
        int max = size - boxStart - boxSpan;
        if (max < min) {
            return d;
        }
        return Mth.clamp(d, min, max);
    }

    private void finalizePress(int mouseX, int mouseY) {
        int dx = cellX(mouseX) - pressCellX;
        int dy = cellY(mouseY) - pressCellY;
        DragMode mode = dragMode;
        pressActive = false;
        dragMode = DragMode.NONE;
        if (mode == DragMode.MOVE) {
            if (Screen.hasShiftDown()) {
                int[] clamped = clampGroupOffset(dx, dy);
                commitMove(clamped[0], clamped[1]);
            } else if (groupMoveValid(dx, dy)) {
                commitMove(dx, dy);
            } else {
                commitMove(dispDx, dispDy);
            }
        } else if (mode == DragMode.COPY) {
            if (Screen.hasShiftDown()) {
                int[] clamped = clampGroupOffset(dx, dy);
                commitCopy(clamped[0], clamped[1]);
            } else if (copyPlacementValid(dx, dy)) {
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
        pressOnSelectedWire = false;
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
            CircuitDiagram.Wire nw = new CircuitDiagram.Wire(w.north, w.east, w.south, w.west, w.copperBitWidth, w.goldBitWidth);
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
        playSound(dragComps.isEmpty() ? WIRE_PLACE_SOUND : COMPONENT_MOVE_SOUND);
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
        boolean copiedAny = false;
        List<CircuitDiagram.Component> copies = new ArrayList<>();
        for (CircuitDiagram.Wire w : dragWires) {
            CircuitDiagram.Wire nw = new CircuitDiagram.Wire(w.north, w.east, w.south, w.west, w.copperBitWidth, w.goldBitWidth);
            nw.x = w.x + dx;
            nw.y = w.y + dy;
            if (editor.setBlock(nw.x, nw.y, nw, force)) {
                copiedAny = true;
            }
        }
        for (CircuitDiagram.Component c : dragComps) {
            CircuitDiagram.Component nc = c.copy();
            nc.x = c.x + dx;
            nc.y = c.y + dy;
            if (editor.setBlock(nc.x, nc.y, nc, force)) {
                copies.add(nc);
                copiedAny = true;
            }
        }
        if (!copiedAny) {
            return;
        }
        playSound(copies.isEmpty() ? WIRE_PLACE_SOUND : COMPONENT_PLACE_SOUND);
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

        // 保留拖拽矩形；若选中的元件超出矩形，则与元件范围取并集，而不是收缩到元件最小矩形。
        setSelBox(minX, minY, maxX - minX + 1, maxY - minY + 1);
        if (selectedCells.isEmpty() && (diagram == null || !hasWireInBox(diagram))) {
            clearSelection();
            clearDragState();
            bumpSelection();
            return;
        }
        boxSelectionActive = true;
        if (!selectedCells.isEmpty()) {
            updateSelBox();
        }
        syncSelectionAnchor();
        clearDragState();
        bumpSelection();
    }

    public void deleteSelection() {
        boolean hasBox = hasSelectionBox();
        if (isReadOnly() || historyTransactionActive || pressActive || draggingWire != null || erasing
                || (selectedCells.isEmpty() && !hasBox)) {
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
        if (!hasBox && selectedCells.size() == 1) {
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
            // 框选：删除选中的元件（selectedCells），以及选框内的所有导线。
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
                    if (!inBounds(c.x + dx, c.y + dy)) {
                        return false;
                    }
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

    /**
     * 镜像入口（由宿主 Screen 的按键绑定调用）：
     * 鼠标拿起元件时翻转放置预览（虚影）的镜像状态；未拿起元件时，对鼠标悬停的元件原地镜像
     * （整体逻辑与旋转一致，只是镜像不改变元件占用的尺寸，因此不会产生冲突）。
     * <p>
     * 垂直镜像等价于“水平镜像 + 旋转 180 度”，因此这里同时翻转镜像布尔量与旋转方向；
     * 二者最终都只改变“引脚位置”，不改变元件占用的格子尺寸。
     */
    public void applyMirror(boolean horizontal) {
        if (isReadOnly() || !hasDiagram()) {
            return;
        }
        if (activeComponent() != null) {
            if (!horizontal) {
                rotation = CircuitComponent.verticalMirrorDirection(rotation);
            }
            placeMirrored = !placeMirrored;
            playSound(MIRROR_SOUND);
            return;
        }
        if (lastMouseX == null || lastMouseY == null || !contains(lastMouseX, lastMouseY)) {
            return;
        }
        tryMirrorAt(cellX(lastMouseX), cellY(lastMouseY), horizontal);
    }

    private void tryMirrorAt(int gx, int gy, boolean horizontal) {
        CircuitDiagram.Component under = blockComponent(gx, gy);
        if (under == null) {
            return;
        }
        if (!selectedCells.contains(cellKey(under.x, under.y))) {
            setSelectionSingle(under.x, under.y);
        }
        if (tryMirrorGroup(horizontal)) {
            playSound(MIRROR_SOUND);
        }
    }

    private boolean tryMirrorGroup(boolean horizontal) {
        if (selectedCells.isEmpty()) {
            return false;
        }
        ItemStack stack = menu.slots.get(36).getItem();
        CircuitDiagram diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null) {
            return false;
        }
        CircuitDiagram editor = diagram.copy();
        boolean any = false;
        for (Map.Entry<Long, CircuitDiagram.Chunk> entry : editor.chunks.entrySet()) {
            for (CircuitDiagram.Component c : entry.getValue().components) {
                if (selectedCells.contains(cellKey(c.x, c.y))) {
                    if (horizontal) {
                        c.mirrorHorizontal();
                    } else {
                        c.mirrorVertical();
                    }
                    any = true;
                }
            }
        }
        if (!any) {
            return false;
        }
        commitDiagram(stack, editor);
        updateSelBox();
        bumpSelection();
        return true;
    }

    /* ============================================================
     *  检查（inspect）tooltip
     * ============================================================ */

    /** 一个引脚在检查 tooltip 中的展示信息。 */
    private record PinView(Direction actualSide, double order, String name, PinType type,
                           Direction side, int offset) {
    }

    /**
     * 构建鼠标悬停元件/导线的检查信息行；未悬停到对象时返回 {@code null}。
     * 由宿主 Screen 在开启检查模式时调用。
     */
    public List<Component> buildInspectTooltip(int mouseX, int mouseY) {
        if (simulator == null || !contains(mouseX, mouseY)) {
            return null;
        }
        CircuitDiagram diagram = getDiagram();
        if (diagram == null) {
            return null;
        }
        CircuitDiagram.Block block = diagram.getBlock(cellX(mouseX), cellY(mouseY));
        if (block instanceof CircuitDiagram.Component component) {
            return buildComponentInspect(component);
        }
        if (block instanceof CircuitDiagram.Wire wire) {
            return buildWireInspect(wire);
        }
        return null;
    }

    private List<Component> buildComponentInspect(CircuitDiagram.Component component) {
        List<Component> lines = new ArrayList<>();
        lines.add(component.component.getName().copy()
                .withStyle(style -> style.withColor(TextColor.fromRgb(COLOR_TEXT_NAME))));

        int degrees = switch (component.direction) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
        MutableComponent rotation = Component.translatable("gui.elements-plus.inspect.rotation", degrees);
        if (component.mirrored) {
            rotation.append(Component.translatable("gui.elements-plus.inspect.mirrored_suffix"));
        }
        lines.add(rotation);

        List<PinView> pins = collectPinViews(component);
        Map<Long, Integer> rank = new HashMap<>();
        Map<Direction, Integer> counts = new EnumMap<>(Direction.class);
        for (Direction side : CLOCKWISE_SIDES) {
            List<PinView> onSide = new ArrayList<>();
            for (PinView pin : pins) {
                if (pin.actualSide() == side) {
                    onSide.add(pin);
                }
            }
            if (onSide.isEmpty()) {
                continue;
            }
            onSide.sort(Comparator.comparingDouble(PinView::order));
            counts.put(side, onSide.size());
            for (int i = 0; i < onSide.size(); i++) {
                PinView pin = onSide.get(i);
                rank.put(pinKey(pin.side(), pin.offset()), i);
            }
        }
        pins.sort(Comparator.comparingInt((PinView pin) -> sideOrder(pin.actualSide()))
                .thenComparingDouble(PinView::order));

        List<PinView> inputs = new ArrayList<>();
        List<PinView> outputs = new ArrayList<>();
        for (PinView pin : pins) {
            if (pin.type() == PinType.INPUT) {
                inputs.add(pin);
            } else {
                outputs.add(pin);
            }
        }
        appendPinSection(lines, "gui.elements-plus.inspect.inputs", inputs, rank, counts, component);
        appendPinSection(lines, "gui.elements-plus.inspect.outputs", outputs, rank, counts, component);
        return lines;
    }

    private List<PinView> collectPinViews(CircuitDiagram.Component component) {
        CircuitComponent cc = component.component;
        int w0 = cc.getWidth();
        int h0 = cc.getHeight();
        List<PinView> pins = new ArrayList<>();
        for (Direction side : CLOCKWISE_SIDES) {
            int count = (side == Direction.NORTH || side == Direction.SOUTH) ? w0 : h0;
            for (int offset = 0; offset < count; offset++) {
                PinType type = cc.getPin(side, offset);
                if (type == null || type == PinType.NONE) {
                    continue;
                }
                double[] pos = CircuitComponent.rotatedPinOffset(w0, h0, component.direction,
                        component.mirrored, side, offset);
                Direction actual = CircuitComponent.rotatedPinSide(w0, h0, component.direction,
                        component.mirrored, side, offset);
                double order = switch (actual) {
                    case NORTH -> pos[0];
                    case EAST -> pos[1];
                    case SOUTH -> -pos[0];
                    case WEST -> -pos[1];
                    default -> 0;
                };
                pins.add(new PinView(actual, order, cc.getPinName(side, offset), type, side, offset));
            }
        }
        return pins;
    }

    private void appendPinSection(List<Component> lines, String headerKey, List<PinView> pins,
                                  Map<Long, Integer> rank, Map<Direction, Integer> counts,
                                  CircuitDiagram.Component component) {
        if (pins.isEmpty()) {
            return;
        }
        lines.add(Component.translatable(headerKey));
        for (PinView pin : pins) {
            int count = counts.getOrDefault(pin.actualSide(), 1);
            int index = rank.getOrDefault(pinKey(pin.side(), pin.offset()), 0);
            MutableComponent direction = Component.translatable(directionKey(pin.actualSide()));
            if (count > 1) {
                direction.append(String.valueOf(index));
            }
            MutableComponent line = Component.literal("- ").append(direction);
            String name = pin.name();
            if (name != null && !name.isEmpty()) {
                line.append(Component.literal(" (" + name + ")"));
            }
            int[] lanes = simulator.getPinLanes(component, pin.side(), pin.offset());
            line.append(Component.literal(": ")).append(formatLanes(lanes));
            lines.add(line);
        }
    }

    private List<Component> buildWireInspect(CircuitDiagram.Wire wire) {
        List<Component> lines = new ArrayList<>();
        CircuitDiagram.Wire.WireMaterial[] materials = {
                CircuitDiagram.Wire.WireMaterial.COPPER, CircuitDiagram.Wire.WireMaterial.GOLD
        };
        for (CircuitDiagram.Wire.WireMaterial material : materials) {
            if (!wire.hasMaterial(material)) {
                continue;
            }
            boolean copper = material == CircuitDiagram.Wire.WireMaterial.COPPER;
            MutableComponent label = Component.translatable(copper
                            ? "gui.elements-plus.wire.copper"
                            : "gui.elements-plus.wire.gold")
                    .withStyle(style -> style.withColor(
                            TextColor.fromRgb(copper ? COLOR_TEXT_COPPER : COLOR_TEXT_GOLD)));
            int[] lanes = simulator.getWireLanes(wire.x, wire.y, material);
            lines.add(Component.empty()
                    .append(label)
                    .append(Component.translatable("gui.elements-plus.inspect.wire_sep"))
                    .append(formatLanes(lanes)));
        }
        return lines;
    }

    /**
     * 值格式化：8 位总线显示 {@code 十进制(通道0 ... 通道7)}（全 0 只显示 0），
     * 1 位信号只显示强度。总线用总线颜色，1 位用灰色。
     */
    private Component formatLanes(int[] lanes) {
        if (lanes != null && lanes.length >= 8) {
            int packed = 0;
            for (int b = 0; b < 8; b++) {
                if ((lanes[b] & 0xF) != 0) {
                    packed |= 1 << b;
                }
            }
            String text;
            if (packed == 0) {
                text = "0";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(packed).append('(');
                for (int b = 0; b < 8; b++) {
                    if (b > 0) {
                        sb.append(' ');
                    }
                    sb.append(lanes[b] & 0xF);
                }
                sb.append(')');
                text = sb.toString();
            }
            return Component.literal(text)
                    .withStyle(style -> style.withColor(TextColor.fromRgb(COLOR_TEXT_BUS)));
        }
        int value = lanes == null || lanes.length == 0 ? 0 : lanes[0] & 0xF;
        return Component.literal(String.valueOf(value))
                .withStyle(style -> style.withColor(TextColor.fromRgb(COLOR_TEXT_1BIT)));
    }

    private static long pinKey(Direction side, int offset) {
        return ((long) side.ordinal() << 32) | (offset & 0xFFFFFFFFL);
    }

    private static int sideOrder(Direction side) {
        return switch (side) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 4;
        };
    }

    private static String directionKey(Direction side) {
        return "gui.elements-plus.direction." + side.getSerializedName();
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
        clampPan();
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