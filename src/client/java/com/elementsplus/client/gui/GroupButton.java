package com.elementsplus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class GroupButton extends Button {
    public boolean active;
    public ButtonGroup buttonGroup;


    public GroupButton(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component, button -> {
        }, DEFAULT_NARRATION);
    }


    @Override
    public void onClick(double d, double e) {
        super.onClick(d, e);
        if (this.buttonGroup != null) {
            this.buttonGroup.onButtonClick(this);
        } else {
            this.active = !this.active;
        }
    }
}
