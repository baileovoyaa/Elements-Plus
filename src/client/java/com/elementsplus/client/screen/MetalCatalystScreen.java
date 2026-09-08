package com.elementsplus.client.screen;

import com.elementsplus.menu.MetalCatalystMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import static com.elementsplus.ElementsPlus.MOD_ID;

@Environment(EnvType.CLIENT)
public class MetalCatalystScreen extends AbstractContainerScreen<MetalCatalystMenu> {
    private static final ResourceLocation LIT_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final ResourceLocation BURN_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/metal_catalyst.png");

    public MetalCatalystScreen(MetalCatalystMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageHeight = 174;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.elements-plus.metal_catalyst.clear_waste"),
                button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }
        ).bounds(this.leftPos + 79, this.topPos + 57, 50, 12).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.isLit()) {
            int n = Mth.ceil(this.menu.getLitProgress() * 13.0F) + 1;
            guiGraphics.blitSprite(LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - n, x + 56, y + 36 + 14 - n, 14, n);
        }

        int m = Mth.ceil(this.menu.getBurnProgress() * 24.0F);
        guiGraphics.blitSprite(BURN_PROGRESS_SPRITE, 24, 16, 0, 0, x + 79, y + 34, m, 16);

        int waste = Mth.ceil(this.menu.getWasteProgress() * 30.0F);
        if (waste > 0) {
            int r = Mth.lerpInt(this.menu.getWasteProgress(), 0x00, 0xFF);
            guiGraphics.fillGradient(
                    leftPos + 28, topPos + 44 - waste,
                    leftPos + 39, topPos + 44,
                    0xFF000000 | (r << 16), 0xFF000000
            );
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
