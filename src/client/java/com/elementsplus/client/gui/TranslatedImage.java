package com.elementsplus.client.gui;

import com.elementsplus.client.ClientResourceHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.resources.ResourceLocation;

public class TranslatedImage extends AbstractWidget {
    public ResourceLocation image;
    public int textureWidth;
    public int textureHeight;


    public TranslatedImage(int x, int y, int width, int height, ResourceLocation image, int textureWidth, int textureHeight) {
        super(x, y, width, height, null);
        this.image = image;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public TranslatedImage(int x, int y, int width, int height, ResourceLocation image) {
        this(x, y, width, height, image, width, height);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.blit(ClientResourceHelper.getLocalizedTexture(image), this.getX(), this.getY(), 0, 0, this.getWidth(), this.getHeight(), textureWidth, textureHeight);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
