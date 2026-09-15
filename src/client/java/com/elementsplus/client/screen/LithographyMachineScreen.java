package com.elementsplus.client.screen;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModDataComponents;
import com.elementsplus.ModItems;
import com.elementsplus.client.ElementsPlusClient;
import com.elementsplus.client.gui.ButtonGroup;
import com.elementsplus.core.circuit.CircuitSimulator;
import com.elementsplus.client.gui.*;
import com.elementsplus.client.gui.TabButton;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.CircuitComponentToolbox;
import com.elementsplus.core.circuit.component.CircuitComponentInstance;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.menu.LithographyMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import javax.swing.*;

public class LithographyMachineScreen extends AbstractContainerScreen<LithographyMachineMenu> implements SlotPositionProvider {
    public TabButton tabButtonDesign;
    public TabButton tabButtonManufacture;
    public ButtonGroup buttonGroup;

    public CollapseButton collapseButtonInventory;
    public boolean inventoryActive = true;

    public ScrollPanelWidget componentWidget;
    public ScrollPanelWidget toolbarWidget;

    public ScrollPanelWidget attributeWidget;
    private int lastAttrX = Integer.MIN_VALUE;
    private int lastAttrY = Integer.MIN_VALUE;
    private CircuitDiagram.Component lastAttrComponent;
    private int lastSelectionVersion = -1;

    public IntSliderWidget speedSlider;
    public EditBox speedEditBox;
    public boolean syncingSpeed = false;

    public CircuitDiagramPanel circuitPanel;

    public List<ComponentCategoryWidget> componentCategories = new ArrayList<>();

    public Map<Integer, Point> slotPosition;

    public boolean isPlaying = false;
    public final CircuitSimulator simulator = new CircuitSimulator();
    private CircuitDiagram lastSimDiagram;
    private double simAccum = 0;
    private IconButton playButton;
    private IconButton bitWidthButton;
    private IconButton themeButton;
    private IconButton muteButton;

    public boolean darkMode = true;
    public boolean muted = false;

    public int bitWidth = 1;

    public static final ResourceLocation TEXTURE_PLAY = ElementsPlus.id("textures/gui/play.png");
    public static final ResourceLocation TEXTURE_PAUSE = ElementsPlus.id("textures/gui/pause.png");
    public static final ResourceLocation TEXTURE_LIGHT = ElementsPlus.id("textures/gui/light.png");
    public static final ResourceLocation TEXTURE_DARK = ElementsPlus.id("textures/gui/dark.png");

    public CircuitDiagram getDiagram() {
        ItemStack stack = menu.slots.get(36).getItem();
        return stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
    }

    private class ComponentEntryButton extends ListEntryButton {
        private final CircuitComponent component;

        ComponentEntryButton(CircuitComponent component) {
            super(0, 0, 0, ComponentCategoryWidget.ENTRY_HEIGHT, component.getName(), component.getIcon());
            this.component = component;
        }

        @Override
        public void onClick(double d, double e) {
            super.onClick(d, e);
            if (menu.getCarried().isEmpty() && !circuitPanel.isReadOnly()) {
                circuitPanel.setVirtualWire(null);
                circuitPanel.setVirtualComponent(component);
            }
        }
    }

    public LithographyMachineScreen(LithographyMachineMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        GuiUtil.drawMainPanel(guiGraphics, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF404040);
        GuiUtil.drawMainPanel(guiGraphics, leftPos, topPos + 18, leftPos + imageWidth, topPos + imageHeight, 0xFFFFFFFF);
        if (inventoryActive) {
            GuiUtil.drawSubPanel(guiGraphics, leftPos + 5, topPos + imageHeight - 188, leftPos + 5 + 79, topPos + imageHeight - 5, 0xFFA0A0A0);
        }

        if (collapseButtonInventory.active) {
            for (Slot slot : this.menu.slots) {
                Point point = getSlotPosition(slot);
                GuiUtil.drawSlot(guiGraphics, this.leftPos + point.x() - 1, this.topPos + point.y() - 1);
            }
        }

        Point point = getSlotPosition(this.menu.diagramSlot);
        GuiUtil.drawSlot(guiGraphics, this.leftPos + point.x() - 1, this.topPos + point.y() - 1);
    }

    @Override
    protected void init() {
        updateScreenSize();
        super.init();
        this.titleLabelX = 6;
        this.addRenderableWidget(tabButtonDesign = new TabButton(this.leftPos + this.font.width(this.title) + 10, this.topPos, 50, 22, Component.translatable("gui.elements-plus.lithography_machine.design")));
        this.addRenderableWidget(tabButtonManufacture = new TabButton(this.leftPos + this.font.width(this.title) + 60, this.topPos, 50, 22, Component.translatable("gui.elements-plus.lithography_machine.manufacture")));
        this.buttonGroup = new ButtonGroup(tabButtonDesign, tabButtonManufacture);

        this.addRenderableWidget(collapseButtonInventory = new CollapseButton(this.leftPos + 5, inventoryActive ? this.topPos + this.imageHeight - 188 : this.topPos + this.imageHeight - 23, 79, 18, Component.nullToEmpty("物品栏")) {
            @Override
            public void onClick(double d, double e) {
                super.onClick(d, e);
                inventoryActive = active;
                this.setY(inventoryActive ? topPos + imageHeight - 188 : topPos + imageHeight - 23);
                componentWidget.setHeight(inventoryActive ? imageHeight - 218 : imageHeight - 53);
            }
        });
        collapseButtonInventory.active = inventoryActive;

        this.addRenderableWidget(componentWidget = new ScrollPanelWidget(leftPos + 5, topPos + 25, 79, imageHeight - 218, GuiUtil.SubPanelType.BORDERED, 0xFFA0A0A0));
        componentWidget.setHeight(inventoryActive ? imageHeight - 218 : imageHeight - 53);
        componentWidget.overflowBehaviorX = ScrollPanelWidget.OverflowBehavior.CLIP;

        this.addRenderableWidget(circuitPanel = new CircuitDiagramPanel(this.menu,
                () -> buttonGroup.getSelected() == tabButtonDesign,
                leftPos + 10 + 80, topPos + 49,
                Math.max(1, imageWidth - 180), Math.max(1, imageHeight - 55)));
        circuitPanel.setSimulator(simulator);

        // 属性面板
        this.addRenderableWidget(attributeWidget = new ScrollPanelWidget(leftPos + imageWidth - 5 - 79, topPos + 25, 79, imageHeight - 30, GuiUtil.SubPanelType.BORDERED, 0xFFA0A0A0));
        attributeWidget.overflowBehaviorX = ScrollPanelWidget.OverflowBehavior.CLIP;

        this.addRenderableWidget(toolbarWidget = new ScrollPanelWidget(leftPos + 10 + 79 + 18 + 5, topPos + 25, imageWidth - 10 - 79 - 10 - 79 - 18 - 5, 18, GuiUtil.SubPanelType.BORDERED, 0xFFA0A0A0));

        int toolbarX = 0;

        // 播放/暂停
        playButton = toolbarWidget.addChild(new IconButton(toolbarX, 1, 16, 16, ElementsPlus.id("textures/gui/play.png"), button -> {
            if (isPlaying) {
                playButton.icon = TEXTURE_PLAY;
                isPlaying = false;
            } else {
                if (simulator.hasCycle()) {
                    return;
                }
                playButton.icon = TEXTURE_PAUSE;
                isPlaying = true;
                simAccum = 0;
            }
        }));

        // 单步
        toolbarWidget.addChild(new IconButton(toolbarX += 16, 1, 16, 16, ElementsPlus.id("textures/gui/step.png"), button -> {
            isPlaying = false;
            if (playButton != null) {
                playButton.icon = TEXTURE_PLAY;
            }
            simulator.step();
        }));

        // 复位
        toolbarWidget.addChild(new IconButton(toolbarX += 16, 1, 16, 16, ElementsPlus.id("textures/gui/stop.png"), button -> {
            isPlaying = false;
            if (playButton != null) {
                playButton.icon = TEXTURE_PLAY;
            }
            simulator.reset();
        }));

        // 速度滑块（范围 1~20，与输入框同步；输入超出范围时滑块停在两端）
        speedEditBox = new EditBox(font, toolbarX += 16 + 50, 1, 50, 16, Component.empty());
        speedSlider = toolbarWidget.addChild(new IntSliderWidget(toolbarX -= 50, 1, 50, 16, 1, 20, 20, value -> {
            if (syncingSpeed) return;
            syncingSpeed = true;
            speedEditBox.setValue(String.valueOf(value));
            syncingSpeed = false;
        }));
        speedEditBox.setResponder(text -> {
            if (syncingSpeed) return;
            int parsed;
            try {
                parsed = Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                return;
            }
            speedSlider.setValue(parsed);
        });
        speedEditBox.setValue("20");
        toolbarWidget.addChild(speedEditBox);

        // 工具栏分隔线（纯视觉）
        toolbarWidget.addChild(new ToolbarSeparator(toolbarX += 50 + 50 + 5, 2, 1, 14));

        // 铜线
        toolbarWidget.addChild(new IconButton(toolbarX += 5, 1, 16, 16, ElementsPlus.id("textures/item/copper_wire.png"), button -> {
            if (menu.getCarried().isEmpty() && !circuitPanel.isReadOnly()) {
                circuitPanel.setVirtualComponent(null);
                circuitPanel.setVirtualWire(CircuitDiagram.Wire.WireMaterial.COPPER);
            }
        }));

        // 金线
        toolbarWidget.addChild(new IconButton(toolbarX += 16, 1, 16, 16, ElementsPlus.id("textures/item/gold_wire.png"), button -> {
            if (menu.getCarried().isEmpty() && !circuitPanel.isReadOnly()) {
                circuitPanel.setVirtualComponent(null);
                circuitPanel.setVirtualWire(CircuitDiagram.Wire.WireMaterial.GOLD);
            }
        }));

        // 位宽
        toolbarWidget.addChild(bitWidthButton = new IconButton(toolbarX += 16, 1, 16, 16, Component.nullToEmpty(String.valueOf(bitWidth)), button -> {
            if (bitWidth == 1) {
                bitWidth = 8;
            } else {
                bitWidth = 1;
            }
            bitWidthButton.setMessage(Component.nullToEmpty(String.valueOf(bitWidth)));
        }));

        toolbarWidget.addChild(new ToolbarSeparator(toolbarX += 16 + 5, 2, 1, 14));

        // 明暗切换
        CircuitDiagramPanel.colorBackground = darkMode ? 0xFF2B2B28 : 0xFFE0E0E0;
        CircuitDiagramPanel.colorDot = darkMode ? 0xFF4A4A44 : 0xFFA0A0A0;
        toolbarWidget.addChild(themeButton = new IconButton(toolbarX += 5, 1, 16, 16, darkMode ? ElementsPlus.id("textures/gui/light.png") : ElementsPlus.id("textures/gui/dark.png"), button -> {
            if (darkMode) {
                darkMode = false;
                themeButton.icon = ElementsPlus.id("textures/gui/dark.png");
            } else {
                darkMode = true;
                themeButton.icon = ElementsPlus.id("textures/gui/light.png");
            }
            CircuitDiagramPanel.colorBackground = darkMode ? 0xFF2B2B28 : 0xFFE0E0E0;
            CircuitDiagramPanel.colorDot = darkMode ? 0xFF4A4A44 : 0xFFA0A0A0;
        }));

        CircuitDiagramPanel.sound = !muted;
        toolbarWidget.addChild(muteButton = new IconButton(toolbarX += 16, 1, 16, 16, muted ? ElementsPlus.id("textures/gui/sound_muted.png") : ElementsPlus.id("textures/gui/sound.png"), button -> {
            if (muted) {
                muted = false;
                muteButton.icon = ElementsPlus.id("textures/gui/sound.png");
            } else {
                muted = true;
                muteButton.icon = ElementsPlus.id("textures/gui/sound_muted.png");
            }
            CircuitDiagramPanel.sound = !muted;
        }));

        initComponentList();

        slotPosition = new HashMap<>();

        for (int l = 0; l < 3; l++) {
            for (int k = 0; k < 9; k++) {
                slotPosition.put(l * 9 + k + 9, Point.of(8 + l * 18, this.imageHeight - 6 - 9 * 18 + k * 18));
            }
        }

        for (int l = 0; l < 9; l++) {
            slotPosition.put(l, Point.of(11 + 3 * 18, this.imageHeight - 6 - 9 * 18 + l * 18));
        }
    }

    private void initComponentList() {
        componentWidget.clearChildren();
        componentCategories.clear();
        CircuitComponentToolbox toolbox = ElementsPlusClient.getToolbox();
        if (toolbox == null || toolbox.categories == null) return;
        for (CircuitComponentToolbox.Category category : toolbox.categories) {
            ComponentCategoryWidget widget = new ComponentCategoryWidget(0, 0, 79, category.name)
                    .setCollapseListener(this::layoutComponentList);
            if (category.components != null) {
                for (CircuitComponent component : category.components) {
                    widget.addEntry(new ComponentEntryButton(component));
                }
            }
            componentWidget.addChild(widget);
            componentCategories.add(widget);
        }
        layoutComponentList();
    }

    private void layoutComponentList() {
        int y = 0;
        for (ComponentCategoryWidget category : componentCategories) {
            category.setY(y);
            y += category.getHeight();
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        boolean designTab = buttonGroup.getSelected() == tabButtonDesign;
        attributeWidget.visible = designTab;
        toolbarWidget.visible = designTab;
        if (!designTab) {
            return;
        }
        int sx = circuitPanel.getSelectedX();
        int sy = circuitPanel.getSelectedY();
        CircuitDiagram.Component current = circuitPanel.getSelectedComponent();
        int selVer = circuitPanel.getSelectionVersion();
        if (selVer != lastSelectionVersion || sx != lastAttrX || sy != lastAttrY || current != lastAttrComponent) {
            lastSelectionVersion = selVer;
            lastAttrX = sx;
            lastAttrY = sy;
            lastAttrComponent = current;
            rebuildAttributes();
        }

        // 电路模拟生命周期：图引用变化 -> 重建依赖图；运行则按速度滑块推进时序，否则仅做组合求值
        CircuitDiagram diagram = getDiagram();
        boolean simDirty = circuitPanel.consumeSimulationDirty();
        if (lastSimDiagram != diagram || simDirty) {
            lastSimDiagram = diagram;
            simulator.setDiagram(diagram);
            simAccum = 0;
        }
        if (diagram != null) {
            if (isPlaying && simulator.hasCycle()) {
                isPlaying = false;
                if (playButton != null) {
                    playButton.icon = TEXTURE_PLAY;
                }
            }
            if (isPlaying) {
                simAccum += 50; // 一个游戏刻 = 50ms
                double period = 1000.0 / Math.max(1, speedSlider.getValue());
                while (simAccum >= period) {
                    simAccum -= period;
                    simulator.step();
                }
            } else {
                simAccum = 0;
                simulator.evaluate();
            }
        }
    }

    /**
     * 根据当前选中的元件，重建其属性字段控件（每个配置一行）。
     * 只读模式下仍显示名称与当前值，但不提供控件。
     */
    private void rebuildAttributes() {
        attributeWidget.clearChildren();
        attributeWidget.scrollToTop();
        if (circuitPanel.isMultiSelected()) {
            attributeWidget.addChild(new AbstractWidget(0, 1, attributeWidget.getWidth(), 20, Component.empty()) {
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                    guiGraphics.drawString(font,
                            Component.translatable("gui.elements-plus.lithography_machine.multiple_selected"),
                            this.getX() + 5, this.getY() + 5, 0xFF00E5FF, false);
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                }

                @Override
                public boolean mouseDragged(double d, double e, int i, double f, double g) {
                    return false;
                }
            });
            return;
        }
        CircuitDiagram.Component selected = circuitPanel.getSelectedComponent();
        if (selected == null) {
            return;
        }
        boolean readOnly = circuitPanel.isReadOnly();
        attributeWidget.addChild(new AbstractWidget(0, 1, attributeWidget.getWidth(), 20, Component.empty()) {
            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                ResourceLocation icon = selected.component.getIcon();
                if (icon != null) {
                    guiGraphics.blit(icon, this.getX() + 5, this.getY() + 1, 0, 0, 16, 16, 16, 16);
                }
                guiGraphics.drawString(font, selected.component.getName(), this.getX() + 22, this.getY() + 4, 0xFFFFFFFF, false);
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

            }

            @Override
            public boolean mouseDragged(double d, double e, int i, double f, double g) {
                return false;
            }
        });
        int y = 22;
        for (CircuitComponentInstance.Config config : selected.instance.getConfigs()) {
            ComponentConfigWidget widget = attributeWidget.addChild(
                    new ComponentConfigWidget(2, y, attributeWidget.getWidth() - 4, 22,
                            selected.instance, config, !readOnly, this::commitAttributes));
            y += widget.getHeight() + 2;
        }
    }

    private void commitAttributes() {
        circuitPanel.commitDiagram();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_DELETE && buttonGroup.getSelected() == tabButtonDesign
                && (speedEditBox == null || !speedEditBox.isFocused())
                && (circuitPanel.hasSelection() || circuitPanel.isMultiSelected())) {
            circuitPanel.deleteSelection();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public Point getSlotPosition(Slot slot) {
        if (slot.container instanceof Inventory && slot.getContainerSlot() >= 0 && slot.getContainerSlot() <= 35) {
            if (collapseButtonInventory.active) {
                return slotPosition.getOrDefault(slot.getContainerSlot(), Point.of(slot.x, slot.y));
            } else {
                return null;
            }
        } else {
            return new Point(slot.x, slot.y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFFFF, false);
    }

    private void updateScreenSize() {
        this.imageWidth = Math.max(250, this.width - 80);
        this.imageHeight = Math.max(230, this.height - 40);
        this.topPos = this.height / 2 - this.imageHeight / 2;
        this.leftPos = this.width / 2 - this.imageWidth / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        ItemStack carried = this.menu.getCarried();
        boolean hideCarried = !carried.isEmpty() && buttonGroup.getSelected() == tabButtonDesign
                && circuitPanel.isPlaceable(carried) && circuitPanel.contains(mouseX, mouseY)
                && circuitPanel.hasDiagram() && !circuitPanel.isReadOnly();
        if (hideCarried) {
            circuitPanel.setPreviewStack(carried);
            this.menu.setCarried(ItemStack.EMPTY);
        }
        super.render(guiGraphics, mouseX, mouseY, delta);
        if (hideCarried) {
            this.menu.setCarried(carried);
            circuitPanel.setPreviewStack(ItemStack.EMPTY);
        }
        CircuitComponent virtual = circuitPanel.getVirtualComponent();
        CircuitDiagram.Wire.WireMaterial virtualWire = circuitPanel.getVirtualWire();
        boolean suppressVirtual = circuitPanel.hasDiagram() && !circuitPanel.isReadOnly()
                && buttonGroup.getSelected() == tabButtonDesign
                && circuitPanel.contains(mouseX, mouseY);
        if (virtual != null && !suppressVirtual) {
            drawVirtualCursor(guiGraphics, virtual, mouseX, mouseY);
        }
        if (virtualWire != null && !suppressVirtual) {
            drawVirtualWireCursor(guiGraphics, virtualWire, mouseX, mouseY);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawVirtualCursor(GuiGraphics guiGraphics, CircuitComponent component, int mouseX, int mouseY) {
        ResourceLocation icon = component.getIcon();
        if (icon != null) {
            guiGraphics.blit(icon, mouseX - 8, mouseY - 8, 0, 0, 16, 16, 16, 16);
        } else {
            guiGraphics.renderItem(new ItemStack(ModItems.SMALL_CHIP), mouseX - 8, mouseY - 8);
        }
    }

    private void drawVirtualWireCursor(GuiGraphics guiGraphics, CircuitDiagram.Wire.WireMaterial material, int mouseX, int mouseY) {
        ItemStack stack = new ItemStack(material == CircuitDiagram.Wire.WireMaterial.GOLD ? ModItems.GOLD_WIRE : ModItems.COPPER_WIRE);
        guiGraphics.renderItem(stack, mouseX - 8, mouseY - 8);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        speedEditBox.setFocused(false);
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (isOverSlot(mouseX, mouseY) || button == 1) {
            circuitPanel.setVirtualComponent(null);
            circuitPanel.setVirtualWire(null);
        }
        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 2) {
            circuitPanel.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverSlot(double mouseX, double mouseY) {
        for (Slot slot : this.menu.slots) {
            Point point = getSlotPosition(slot);
            if (point == null) continue;
            int sx = this.leftPos + point.x() - 1;
            int sy = this.topPos + point.y() - 1;
            if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        Point point = getSlotPosition(slot);
        if (point != null) {
            super.renderSlot(guiGraphics, new Slot(slot.container, slot.getContainerSlot(), point.x(), point.y()));
        }
    }

}
