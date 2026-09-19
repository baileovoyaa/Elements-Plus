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
    public boolean keepRatio;


    public TranslatedImage(int x, int y, int width, int height, ResourceLocation image, int textureWidth, int textureHeight, boolean keepRatio) {
        super(x, y, width, height, null);
        this.image = image;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.keepRatio = keepRatio;
    }

    public TranslatedImage(int x, int y, int width, int height, ResourceLocation image, int textureWidth, int textureHeight) {
        this(x, y, width, height, image, textureWidth, textureHeight, false);
    }

    public TranslatedImage(int x, int y, int width, int height, ResourceLocation image) {
        this(x, y, width, height, image, width, height);
    }

    public TranslatedImage(int x, int y, int width, ResourceLocation image, int textureWidth, int textureHeight) {
        this(x, y, width, 0, image, textureWidth, textureHeight, true);
        this.height = heightOfWidth(width);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        if (keepRatio) {
            int width = Math.min(this.getWidth(), textureWidth);
            int height = heightOfWidth(width);
            guiGraphics.blit(ClientResourceHelper.getLocalizedTexture(image), this.getX(), this.getY(), 0, 0, width, height, width, height);
        } else {
            guiGraphics.blit(ClientResourceHelper.getLocalizedTexture(image), this.getX(), this.getY(), 0, 0, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    public int heightOfWidth(int width) {
        if (keepRatio) {
            return textureHeight * width / textureWidth;
        } else {
            return this.getHeight();
        }
    }
}
