package com.elementsplus.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ComponentCategoryWidget extends AbstractWidget {
    public static final int HEADER_HEIGHT = 18;
    public static final int ENTRY_HEIGHT = 18;

    private final CollapseButton header;
    private final List<ListEntryButton> entries = new ArrayList<>();
    private boolean expanded = true;
    private Runnable onCollapse;

    public ComponentCategoryWidget(int x, int y, int width, Component title) {
        super(x, y, width, HEADER_HEIGHT, Component.empty());
        this.header = new CollapseButton(0, 0, width, HEADER_HEIGHT, title) {
            @Override
            public void onClick(double d, double e) {
                super.onClick(d, e);
                setExpanded(active);
            }
        };
        this.header.active = true;
    }

    public ComponentCategoryWidget setCollapseListener(Runnable onCollapse) {
        this.onCollapse = onCollapse;
        return this;
    }

    public ListEntryButton addEntry(Component label) {
        ListEntryButton entry = new ListEntryButton(1, HEADER_HEIGHT + entries.size() * ENTRY_HEIGHT, getWidth() - 2, ENTRY_HEIGHT, label);
        entries.add(entry);
        updateHeight();
        return entry;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        this.header.active = expanded;
        updateHeight();
        if (onCollapse != null) {
            onCollapse.run();
        }
    }

    private void updateHeight() {
        this.height = expanded ? HEADER_HEIGHT + entries.size() * ENTRY_HEIGHT : HEADER_HEIGHT;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int screenX = getX();
        int screenY = getY();
        renderChild(header, screenX, screenY, g, mouseX, mouseY, partialTick);
        if (!expanded) return;
        for (AbstractWidget entry : entries) {
            renderChild(entry, screenX, screenY, g, mouseX, mouseY, partialTick);
        }
    }

    private void renderChild(AbstractWidget child, int screenX, int screenY, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int contentX = child.getX();
        int contentY = child.getY();
        child.setX(screenX + contentX);
        child.setY(screenY + contentY);
        try {
            child.render(g, mouseX, mouseY, partialTick);
        } finally {
            child.setX(contentX);
            child.setY(contentY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible) return false;
        double localX = mouseX - getX();
        double localY = mouseY - getY();
        if (header.mouseClicked(localX, localY, button)) {
            return true;
        }
        if (!expanded) return false;
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).mouseClicked(localX, localY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double localX = mouseX - getX();
        double localY = mouseY - getY();
        boolean handled = false;
        if (header.mouseReleased(localX, localY, button)) handled = true;
        if (!expanded) return handled;
        for (AbstractWidget entry : entries) {
            if (entry.mouseReleased(localX, localY, button)) handled = true;
        }
        return handled;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}