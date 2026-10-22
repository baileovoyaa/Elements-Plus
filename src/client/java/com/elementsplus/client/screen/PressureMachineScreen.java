package com.elementsplus.client.screen;

import com.elementsplus.ElementsPlus;
import com.elementsplus.client.gui.GuiUtil;
import com.elementsplus.menu.PressureMachineMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

@Environment(EnvType.CLIENT)
public class PressureMachineScreen extends AbstractContainerScreen<PressureMachineMenu> {
    private static final ResourceLocation LIT_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final ResourceLocation TEXTURE = ElementsPlus.id("textures/gui/pressure_machine.png");
    private static final int TEXT_COLOR = 0xFF404040;

    public PressureMachineScreen(PressureMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.isLit()) {
            int n = Mth.ceil(this.menu.getLitProgress() * 13.0F) + 1;
            guiGraphics.blitSprite(LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - n,
                    this.leftPos + 150, this.topPos + 36 + 14 - n, 14, n);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                TEXT_COLOR, false);
        this.drawInfo(guiGraphics);
    }

    private void drawInfo(GuiGraphics guiGraphics) {
        int x = 8;
        int y = 18;
        guiGraphics.drawString(this.font,
                Component.translatable("gui.elements-plus.pressure_machine.flow", format(this.menu.getFlow())),
                x, y, TEXT_COLOR, false);

        y += 10;
        guiGraphics.drawString(this.font, sideLine("gui.elements-plus.pressure_machine.input",
                        this.menu.getInputPressure(), this.menu.getInputVolume(), this.menu.isInputSealed()),
                x, y, TEXT_COLOR, false);

        y += 10;
        guiGraphics.drawString(this.font, sideLine("gui.elements-plus.pressure_machine.output",
                        this.menu.getOutputPressure(), this.menu.getOutputVolume(), this.menu.isOutputSealed()),
                x, y, TEXT_COLOR, false);

        y += 10;
        Component status = this.menu.isSameSpace()
                ? Component.translatable("gui.elements-plus.pressure_machine.same_space")
                : Component.translatable(this.menu.isWorking()
                ? "gui.elements-plus.pressure_machine.working"
                : "gui.elements-plus.pressure_machine.idle");
        guiGraphics.drawString(this.font, status, x, y, TEXT_COLOR, false);
    }

    private static MutableComponent sideLine(String labelKey, double pressure, int volume, boolean sealed) {
        return Component.translatable(labelKey).copy()
                .append(Component.literal(": " + format(pressure) + " atm, " + volume + " B ("))
                .append(Component.translatable(sealed
                        ? "gui.elements-plus.pressure_machine.sealed"
                        : "gui.elements-plus.pressure_machine.open"))
                .append(Component.literal(")"));
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
