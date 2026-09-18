package com.elementsplus.client.screen;

import com.elementsplus.client.gui.ButtonGroup;
import com.elementsplus.client.gui.CollapseButton;
import com.elementsplus.client.gui.GroupButton;
import com.elementsplus.client.gui.GuiUtil;
import com.elementsplus.client.gui.ListEntryButton;
import com.elementsplus.client.gui.Point;
import com.elementsplus.client.gui.ScrollPanelWidget;
import com.elementsplus.client.gui.SlotPositionProvider;
import com.elementsplus.client.gui.TabButton;
import com.elementsplus.core.experiment.BaseExperiment;
import com.elementsplus.core.experiment.BuiltinExperiments;
import com.elementsplus.menu.ExperimentTableMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExperimentTableScreen extends AbstractContainerScreen<ExperimentTableMenu> implements SlotPositionProvider {
    public TabButton tabButtonExperiment;
    public CollapseButton collapseButtonInventory;
    public boolean inventoryActive = true;
    public Map<Integer, Point> slotPosition;

    public ScrollPanelWidget experimentWidget;
    public ButtonGroup experimentGroup;
    public BaseExperiment selectedExperiment;

    public ExperimentTableScreen(ExperimentTableMenu experimentTableMenu, Inventory inventory, Component component) {
        super(experimentTableMenu, inventory, component);
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
                if (point != null) {
                    GuiUtil.drawSlot(guiGraphics, this.leftPos + point.x() - 1, this.topPos + point.y() - 1);
                }
            }
        }

        Point point = getSlotPosition(this.menu.extraSlot);
        GuiUtil.drawSlot(guiGraphics, this.leftPos + point.x() - 1, this.topPos + point.y() - 1);
    }

    @Override
    protected void init() {
        updateScreenSize();
        super.init();

        this.titleLabelX = 6;
        this.addRenderableWidget(tabButtonExperiment = new TabButton(this.leftPos + this.font.width(this.title) + 10, this.topPos, 50, 22, Component.translatable("gui.elements-plus.experiment_table.tab")));
        tabButtonExperiment.active = true;

        this.addRenderableWidget(collapseButtonInventory = new CollapseButton(this.leftPos + 5, inventoryActive ? this.topPos + this.imageHeight - 188 : this.topPos + this.imageHeight - 23, 79, 18, Component.nullToEmpty("物品栏")) {
            @Override
            public void onClick(double d, double e) {
                super.onClick(d, e);
                inventoryActive = active;
                this.setY(inventoryActive ? topPos + imageHeight - 188 : topPos + imageHeight - 23);
                experimentWidget.setHeight(inventoryActive ? imageHeight - 218 : imageHeight - 53);
            }
        });
        collapseButtonInventory.active = inventoryActive;

        this.addRenderableWidget(experimentWidget = new ScrollPanelWidget(this.leftPos + 5, this.topPos + 25, 79, imageHeight - 218, GuiUtil.SubPanelType.BORDERED, 0xFFA0A0A0));
        experimentWidget.setHeight(inventoryActive ? imageHeight - 218 : imageHeight - 53);
        experimentWidget.overflowBehaviorX = ScrollPanelWidget.OverflowBehavior.CLIP;

        List<GroupButton> experimentButtons = new ArrayList<>();
        int entryY = 2;
        for (BaseExperiment experiment : BuiltinExperiments.BUILTIN_EXPERIMENTS) {
            ExperimentEntryButton button = experimentWidget.addChild(new ExperimentEntryButton(1, entryY, experimentWidget.getWidth() - 2, 18, experiment));
            experimentButtons.add(button);
            entryY += 18;
        }
        if (!experimentButtons.isEmpty()) {
            selectedExperiment = ((ExperimentEntryButton) experimentButtons.get(0)).experiment;
            experimentGroup = new ButtonGroup(experimentButtons.toArray(new GroupButton[0]));
        }

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
        this.imageWidth = Math.max(220, this.width - 80);
        this.imageHeight = Math.max(210, this.height - 40);
        this.topPos = this.height / 2 - this.imageHeight / 2;
        this.leftPos = this.width / 2 - this.imageWidth / 2;
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        Point point = getSlotPosition(slot);
        if (point != null) {
            super.renderSlot(guiGraphics, new Slot(slot.container, slot.getContainerSlot(), point.x(), point.y()));
        }
    }

    private static Component experimentLabel(BaseExperiment experiment) {
        String name = experiment.getName();
        return name == null ? Component.literal("?") : Component.translatable("experiment.elements-plus." + name);
    }

    private class ExperimentEntryButton extends ListEntryButton {
        private final BaseExperiment experiment;

        ExperimentEntryButton(int x, int y, int width, int height, BaseExperiment experiment) {
            super(x, y, width, height, experimentLabel(experiment));
            this.experiment = experiment;
        }

        @Override
        public void onClick(double d, double e) {
            super.onClick(d, e);
            selectedExperiment = experiment;
        }

        @Override
        public void renderString(GuiGraphics guiGraphics, Font font, int i) {
            int left = this.getX() + 3;
            int right = this.getX() + this.getWidth() - 3;
            guiGraphics.enableScissor(left, this.getY(), right, this.getY() + this.getHeight());
            guiGraphics.drawString(font, this.getMessage(), left, this.getY() + (this.getHeight() - 9) / 2, i);
            guiGraphics.disableScissor();
        }
    }
}