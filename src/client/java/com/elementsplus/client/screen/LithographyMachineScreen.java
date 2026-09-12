package com.elementsplus.client.screen;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModItems;
import com.elementsplus.client.ElementsPlusClient;
import com.elementsplus.client.gui.*;
import com.elementsplus.client.gui.TabButton;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.CircuitComponentToolbox;
import com.elementsplus.menu.LithographyMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
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

public class LithographyMachineScreen extends AbstractContainerScreen<LithographyMachineMenu> implements SlotPositionProvider {
    public TabButton tabButtonDesign;
    public TabButton tabButtonManufacture;
    public ButtonGroup buttonGroup;

    public CollapseButton collapseButtonInventory;
    public boolean inventoryActive = true;

    public ScrollPanelWidget componentWidget;
    public ScrollPanelWidget toolbarWidget;

    public CircuitDiagramPanel circuitPanel;

    public List<ComponentCategoryWidget> componentCategories = new ArrayList<>();

    public Map<Integer, Point> slotPosition;

    public boolean isPlaying = false;

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
        if (buttonGroup.getSelected() == tabButtonDesign) {
            GuiUtil.drawSubPanel(guiGraphics, leftPos + imageWidth - 5 - 79, topPos + 25, leftPos + imageWidth - 5, topPos + imageHeight - 5, 0xFFA0A0A0);
            GuiUtil.drawSubPanel(guiGraphics, leftPos + 10 + 79 + 18 + 5, topPos + 25, leftPos + imageWidth - 10 - 79, topPos + 43, 0xFFA0A0A0);
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

        this.addRenderableWidget(circuitPanel = new CircuitDiagramPanel(this.menu,
                () -> buttonGroup.getSelected() == tabButtonDesign,
                leftPos + 10 + 80, topPos + 49,
                Math.max(1, imageWidth - 180), Math.max(1, imageHeight - 55)));

        this.addRenderableWidget(toolbarWidget = new ScrollPanelWidget(leftPos + 10 + 79 + 18 + 5, topPos + 25, imageWidth - 10 - 79 - 10 - 79 - 18 - 5, 18, GuiUtil.SubPanelType.BORDERED, 0xFFA0A0A0));

        // 播放/暂停
        toolbarWidget.addChild(new IconButton(0, 0, 16, 16, ElementsPlus.id("textures/gui/widget.png"), 8, 0, button -> {
            if (isPlaying) {
                ((IconButton) button).u = 8;
                isPlaying = false;
            } else {
                ((IconButton) button).u = 8 + 16;
                isPlaying = true;
            }
        }));

        // 单步
        toolbarWidget.addChild(new IconButton(16, 0, 16, 16, ElementsPlus.id("textures/gui/widget.png"), 8 + 32, 0, button -> {

        }));

        // 复位
        toolbarWidget.addChild(new IconButton(32, 0, 16, 16, ElementsPlus.id("textures/gui/widget.png"), 8 + 48, 0, button -> {

        }));

        // TODO: 速度滑块

        // 速度输入框
        toolbarWidget.addChild(new EditBox(font, 100, 0, 50, 16, Component.empty()));

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
        boolean suppressVirtual = circuitPanel.hasDiagram() && !circuitPanel.isReadOnly()
                && buttonGroup.getSelected() == tabButtonDesign
                && circuitPanel.contains(mouseX, mouseY);
        if (virtual != null && !suppressVirtual) {
            drawVirtualCursor(guiGraphics, virtual, mouseX, mouseY);
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (isOverSlot(mouseX, mouseY) || button == 1) {
            circuitPanel.setVirtualComponent(null);
        }
        return handled;
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
