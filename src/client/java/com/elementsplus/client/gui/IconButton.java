package com.elementsplus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IconButton extends Button {

    @Nullable
    public ResourceLocation icon;
    public int textureWidth;
    public int textureHeight;
    public Font font = Minecraft.getInstance().font;
    @Nullable
    private Component message;

    public IconButton(int x, int y, int width, int height, @Nullable ResourceLocation icon, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.icon = icon;
        this.textureWidth = width;
        this.textureHeight = height;
    }

    public IconButton(int x, int y, int width, int height, @Nullable Component component, Button.OnPress onPress) {
        super(x, y, width, height, component, onPress, DEFAULT_NARRATION);
        this.message = component;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        if (icon != null) {
            guiGraphics.blit(icon, this.getX(), this.getY(), 0, 0, this.getWidth(), this.getHeight(), textureWidth, textureHeight);
        }
        if (message != null) {
            GuiUtil.drawCenteredShadowString(guiGraphics, this.message, this.getX() + this.getWidth() / 2, this.getY() + this.getHeight() / 2 - font.lineHeight / 2, 16777215 | Mth.ceil(this.alpha * 255.0F) << 24);
        }
        if (this.isHovered) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x80FFFFFF);
        }

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int k = this.active ? 16777215 : 10526880;
    }

    @Override
    public boolean mouseDragged(double d, double e, int i, double f, double g) {
        if (this.isHovered && this.isValidClickButton(i)) {
            this.onDrag(d, e, f, g);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public @NotNull Component getMessage() {
        if (message != null) {
            return message;
        }
        return Component.empty();
    }

    @Override
    public void setMessage(Component component) {
        super.setMessage(component);
        this.message = component;
    }
}
