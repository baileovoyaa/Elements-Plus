package com.elementsplus.client.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;

public abstract class AbstractWidgetWithChildren extends AbstractWidget {

    public List<AbstractWidget> children;

    public AbstractWidgetWithChildren(int i, int j, int k, int l) {
        super(i, j, k, l, Component.empty());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

}
