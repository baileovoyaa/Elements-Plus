package com.elementsplus.client.screen;

import com.elementsplus.client.gui.*;
import com.elementsplus.menu.LithographyMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.HashMap;
import java.util.Map;

public class LithographyMachineScreen extends AbstractContainerScreen<LithographyMachineMenu> implements SlotPositionProvider {
    public TabButton tabButtonDesign;
    public TabButton tabButtonManufacture;
    public ButtonGroup buttonGroup;

    public CollapseButton collapseButtonInventory;
    public boolean inventoryActive = true;

    public ScrollPanelWidget componentWidget;

    public Map<Integer, Point> slotPosition;

    public LithographyMachineScreen(LithographyMachineMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        GuiUtil.drawMainPanel(guiGraphics, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF404040);
        GuiUtil.drawMainPanel(guiGraphics, leftPos, topPos + 18, leftPos + imageWidth, topPos + imageHeight, 0xFFFFFFFF);
        if (inventoryActive) {
            GuiUtil.drawSubPanel(guiGraphics, leftPos + 5, topPos + 25, leftPos + 5 + 79, topPos + imageHeight - 193, 0xFFA0A0A0);
            GuiUtil.drawSubPanel(guiGraphics, leftPos + 5, topPos + imageHeight - 188, leftPos + 5 + 79, topPos + imageHeight - 5, 0xFFA0A0A0);
        } else {
            GuiUtil.drawSubPanel(guiGraphics, leftPos + 5, topPos + 25, leftPos + 5 + 79, topPos + imageHeight - 28, 0xFFA0A0A0);
        }
        if (buttonGroup.getSelected() == tabButtonDesign) {
            GuiUtil.drawSubPanel(guiGraphics, leftPos + imageWidth - 5 - 79, topPos + 25, leftPos + imageWidth - 5, topPos + imageHeight - 5, 0xFFA0A0A0);
            GuiUtil.drawSubPanel(guiGraphics, leftPos + 10 + 79 + 18 + 5, topPos + 25, leftPos + imageWidth - 10 - 79, topPos + 43, 0xFFA0A0A0);
            GuiUtil.drawSlot(guiGraphics, leftPos + 10 + 79, topPos + 25);
            GuiUtil.drawSubPanel(guiGraphics, leftPos + 10 + 79, topPos + 48, leftPos + imageWidth - 10 - 79, topPos + imageHeight - 5, 0xFFE0E0E0);
        }

        if (collapseButtonInventory.active) {
            for (Slot slot : this.menu.slots) {
                Point point = getSlotPosition(slot);
                GuiUtil.drawSlot(guiGraphics, this.leftPos + point.x() - 1, this.topPos + point.y() - 1);
            }
        }
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
            }
        });
        collapseButtonInventory.active = inventoryActive;

        this.addRenderableWidget(componentWidget = new ScrollPanelWidget(leftPos + 5, topPos + 25, 79, imageHeight - 218, GuiUtil.SubPanelType.BORDERED, 0xFFA0A0A0));

        // 示例
        componentWidget.addChild(new CollapseButton(0, 0, 79, 18, Component.nullToEmpty("基础元件")));
        componentWidget.addChild(new ListEntryButton(1, 18, 77, 18, Component.nullToEmpty("与门")));
        componentWidget.addChild(new ListEntryButton(1, 18 * 2, 77, 18, Component.nullToEmpty("或门")));
        componentWidget.addChild(new ListEntryButton(1, 18 * 3, 77, 18, Component.nullToEmpty("非门")));
        componentWidget.addChild(new CollapseButton(0, 18 * 4, 79, 18, Component.nullToEmpty("高级元件")));
        componentWidget.addChild(new ListEntryButton(1, 18 * 5, 77, 18, Component.nullToEmpty("加法器")));
        componentWidget.addChild(new ListEntryButton(1, 18 * 6, 77, 18, Component.nullToEmpty("位移器")));
        componentWidget.addChild(new ListEntryButton(1, 18 * 7, 77, 18, Component.nullToEmpty("乘法器")));
        componentWidget.addChild(new ListEntryButton(1, 18 * 8, 77, 18, Component.nullToEmpty("寄存器")));
        componentWidget.addChild(new ListEntryButton(1, 18 * 9, 77, 18, Component.nullToEmpty("计数器")));


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

    @Override
    public Point getSlotPosition(Slot slot) {
        if (slot.getContainerSlot() >= 0 && slot.getContainerSlot() <= 35 && !collapseButtonInventory.active) {
            return null;
        }
        return slotPosition.getOrDefault(slot.getContainerSlot(), Point.of(slot.x, slot.y));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFFFF, false);
    }

    private void updateScreenSize() {
        this.imageWidth = Math.max(176, this.width - 80);
        this.imageHeight = Math.max(230, this.height - 40);
        this.topPos = this.height / 2 - this.imageHeight / 2;
        this.leftPos = this.width / 2 - this.imageWidth / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        Point point = getSlotPosition(slot);
        if (point != null) {
            super.renderSlot(guiGraphics, new Slot(slot.container, slot.getContainerSlot(), point.x(), point.y()));
        }
    }


}
