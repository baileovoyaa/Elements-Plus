package com.elementsplus.client.screen;

import com.elementsplus.menu.CrystallizerMenu;
import com.elementsplus.menu.LithographyMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class LithographyMachineScreen extends AbstractContainerScreen<LithographyMachineMenu> {
    public LithographyMachineScreen(LithographyMachineMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        GuiUtil.drawMainPanel(guiGraphics, this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF404040);
        GuiUtil.drawMainPanel(guiGraphics, this.leftPos, this.topPos + 18, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFFFFFFF);
    }

    @Override
    protected void init() {
        updateScreenSize();
        super.init();
        this.titleLabelX = 6;
        this.addRenderableWidget(Button.builder(Component.nullToEmpty("Hello World"), button -> {
        }).bounds(this.leftPos + this.font.width(this.title), this.topPos + 6, 50, 18).build());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    private void updateScreenSize() {
        this.imageWidth = Math.max(176, this.width - 80);
        this.imageHeight = Math.max(166, this.height - 40);
        this.topPos = this.height / 2 - this.imageHeight / 2;
        this.leftPos = this.width / 2 - this.imageWidth / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        updateScreenSize();
    }
}
