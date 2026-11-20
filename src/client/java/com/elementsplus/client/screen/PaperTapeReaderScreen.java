package com.elementsplus.client.screen;

import com.elementsplus.client.gui.GuiUtil;
import com.elementsplus.menu.PaperTapeReaderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 纸带阅读器界面：一个打孔文件槽位 + 玩家物品栏。
 */
public class PaperTapeReaderScreen extends AbstractContainerScreen<PaperTapeReaderMenu> {
    public PaperTapeReaderScreen(PaperTapeReaderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiUtil.drawMainPanel(graphics, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight);
        for (var slot : this.menu.slots) {
            GuiUtil.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX,
                this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
