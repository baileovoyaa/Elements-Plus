package com.elementsplus.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ScrollPanelWidget extends AbstractWidget {
    protected final List<AbstractWidget> children = new ArrayList<>();
    protected double scrollAmount = 0;
    protected double maxScroll = 0;
    protected int scrollStep = 16;
    protected int scrollBarWidth = 6;
    protected GuiUtil.SubPanelType subPanelType;
    protected int backgroundColor;

    @Nullable
    protected AbstractWidget focusedChild;

    public ScrollPanelWidget(int x, int y, int width, int height, GuiUtil.SubPanelType subPanelType, int backgroundColor) {
        super(x, y, width, height, Component.empty());
        this.subPanelType = subPanelType;
        this.backgroundColor = backgroundColor;
    }

    public <T extends AbstractWidget> T addChild(T child) {
        children.add(child);
        return child;
    }

    public void removeChild(AbstractWidget child) {
        children.remove(child);
        if (focusedChild == child) focusedChild = null;
    }

    public void clearChildren() {
        children.clear();
        focusedChild = null;
    }

    /**
     * 内容总高度，默认取所有子 widget 的下边界最大值
     */
    protected int getContentHeight() {
        int max = 0;
        for (AbstractWidget w : children) {
            max = Math.max(max, w.getY() + w.getHeight() - this.getY());
        }
        return max;
    }

    protected void updateScrollBounds() {
        this.maxScroll = Math.max(0, getContentHeight() - this.getHeight());
        this.scrollAmount = Mth.clamp(this.scrollAmount, 0, this.maxScroll);
    }

    // ---------- 渲染 ----------

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        updateScrollBounds();

        // 面板背景
        GuiUtil.drawSubPanel(g, getX(), getY(), getX() + getWidth(), getY() + getHeight(), backgroundColor, subPanelType);

        // 裁剪到面板区域
        g.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
//        g.pose().pushPose();
//        g.pose().translate(getX(), (float) getY() - scrollAmount, 0.0F);

        // 鼠标在"内容坐标系"里的位置
        double localX = mouseX - getX();
        double localY = mouseY + scrollAmount - getY();

        for (AbstractWidget child : children) {
            int contentX = child.getX();          // 面板内容空间坐标
            int contentY = child.getY();
            int screenX = getX() + contentX;      // 屏幕坐标
            int screenY = getY() + contentY - (int) scrollAmount;

            // 可见性剔除
            if (screenY + child.getHeight() < getY()) continue;
            if (screenY > getY() + getHeight()) continue;

            // 临时把 child 搬到屏幕空间
            child.setX(screenX);
            child.setY(screenY);
            try {
                child.render(g, mouseX, mouseY, partialTick);  // 传屏幕鼠标坐标
            } finally {
                child.setX(contentX);
                child.setY(contentY);
            }
        }

//        g.pose().popPose();
        g.disableScissor();

        renderScrollbar(g);
    }

    protected void renderScrollbar(GuiGraphics g) {
        if (maxScroll <= 0) return;
        int trackTop = getY();
        int trackBottom = getY() + getHeight();
        int trackHeight = trackBottom - trackTop;
        int thumbHeight = Math.max(8, (int) (trackHeight * (getHeight() / (double) getContentHeight())));
        int thumbY = trackTop + (int) ((trackHeight - thumbHeight) * (scrollAmount / maxScroll));
        int barX = getX() + getWidth() - scrollBarWidth;
        g.fill(barX, trackTop, barX + scrollBarWidth, trackBottom, 0x40000000);
        g.fill(barX, thumbY, barX + scrollBarWidth, thumbY + thumbHeight, 0xFFAAAAAA);
    }

    // ---------- 事件转发 ----------

    /**
     * 把屏幕坐标转换成"内容坐标系"坐标
     */
    protected double toContentY(double mouseY) {
        return mouseY + scrollAmount;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible) return false;
        if (!isMouseOver(mouseX, mouseY)) return false;

        double localX = mouseX - getX();
        double localY = toContentY(mouseY) - getY();

        // 从后往前，后添加的在上面
        for (int i = children.size() - 1; i >= 0; i--) {
            AbstractWidget child = children.get(i);
            if (child.mouseClicked(localX, localY, button)) {
                focusedChild = child;
                child.setFocused(true);
                return true;
            }
        }
        if (focusedChild != null) {
            focusedChild.setFocused(false);
            focusedChild = null;
        }
        return true; // 面板内点击被吃掉，避免穿透
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double localX = mouseX - getX();
        double localY = toContentY(mouseY) - getY();
        boolean handled = false;
        for (AbstractWidget child : children) {
            if (child.mouseReleased(localX, localY, button)) handled = true;
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double localX = mouseX - getX();
        double localY = toContentY(mouseY) - getY();
        for (AbstractWidget child : children) {
            if (child.mouseDragged(localX, localY, button, dragX, dragY)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // 1.20.2+ 是 (double,double,double,double)，1.20/1.20.1 是 (double,double,double)
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!visible || !isMouseOver(mouseX, mouseY)) return false;

        // 先让子控件处理（支持嵌套滚动）
        double localX = mouseX - getX();
        double localY = toContentY(mouseY) - getY();
        for (int i = children.size() - 1; i >= 0; i--) {
            AbstractWidget child = children.get(i);
            if (child.isMouseOver(localX, localY)
                    && child.mouseScrolled(localX, localY, scrollX, scrollY)) {
                return true;
            }
        }

        // 子控件没消费，就滚动自己
        if (maxScroll > 0) {
            scrollAmount = Mth.clamp(scrollAmount - scrollY * scrollStep, 0, maxScroll);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedChild != null && focusedChild.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (focusedChild != null && focusedChild.charTyped(c, modifiers)) {
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // 根据需要递归调用子 widget 的 narration
    }
}