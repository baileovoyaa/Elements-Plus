package com.elementsplus.client.screen;

import com.elementsplus.menu.CrystallizerMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import static com.elementsplus.ElementsPlus.MOD_ID;

@Environment(EnvType.CLIENT)
public class CrystallizerScreen extends AbstractContainerScreen<CrystallizerMenu> {
    private static final ResourceLocation LIT_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final ResourceLocation BURN_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/crystallizer.png");

    public CrystallizerScreen(CrystallizerMenu furnaceMenu, Inventory inventory, Component component) {
        super(furnaceMenu, inventory, component);
        this.imageHeight = 174;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }


    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        int k = this.leftPos;
        int l = this.topPos;
        guiGraphics.blit(TEXTURE, k, l, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.isLit()) {
            int m = 14;
            int n = Mth.ceil(this.menu.getLitProgress() * 13.0F) + 1;
            guiGraphics.blitSprite(LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - n, k + 56, l + 36 + 14 - n, 14, n);
        }

        int m = 24;
        int n = Mth.ceil(this.menu.getBurnProgress() * 24.0F);
        guiGraphics.blitSprite(BURN_PROGRESS_SPRITE, 24, 16, 0, 0, k + 79, l + 34, n, 16);
    }
}
