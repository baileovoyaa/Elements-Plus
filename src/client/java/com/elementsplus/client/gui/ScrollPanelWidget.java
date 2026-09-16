package com.elementsplus.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ScrollPanelWidget extends AbstractWidget {
    protected final List<AbstractWidget> children = new ArrayList<>();
    protected double scrollAmount = 0;
    protected double maxScroll = 0;
    protected double horizontalScrollAmount = 0;
    protected double maxHorizontalScroll = 0;
    protected int scrollStep = 16;
    protected int scrollBarWidth = 6;
    protected GuiUtil.SubPanelType subPanelType;
    protected int backgroundColor;

    public OverflowBehavior overflowBehaviorX = OverflowBehavior.SCROLL;
    public OverflowBehavior overflowBehaviorY = OverflowBehavior.SCROLL;

    protected boolean draggingScrollbarX = false;
    protected boolean draggingScrollbarY = false;
    protected double dragGrabOffsetX = 0;
    protected double dragGrabOffsetY = 0;

    public enum OverflowBehavior {
        SCROLL,
        SCROLL_NO_BAR,
        CLIP
    }

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

    public double getScrollAmount() {
        return scrollAmount;
    }

    public double getHorizontalScrollAmount() {
        return horizontalScrollAmount;
    }

    public double getMaxScroll() {
        return maxScroll;
    }

    public void scrollBy(double delta) {
        scrollAmount = Mth.clamp(scrollAmount + delta, 0, maxScroll);
    }

    public boolean isScrollable() {
        return maxScroll > 0;
    }

    public void removeChild(AbstractWidget child) {
        children.remove(child);
        if (focusedChild == child) focusedChild = null;
    }

    public void clearChildren() {
        children.clear();
        focusedChild = null;
    }

    public void scrollToTop() {
        this.scrollAmount = 0;
        this.horizontalScrollAmount = 0;
    }

    /**
     * 内容总高度，默认取所有子 widget 的下边界最大值
     */
    protected int getContentHeight() {
        int max = 0;
        for (AbstractWidget w : children) {
            max = Math.max(max, w.getY() + w.getHeight());
        }
        return max + 1;
    }

    /**
     * 内容总宽度，默认取所有子 widget 的右边界最大值
     */
    protected int getContentWidth() {
        int max = 0;
        for (AbstractWidget w : children) {
            max = Math.max(max, w.getX() + w.getWidth());
        }
        return max + 1;
    }

    protected void updateScrollBounds() {
        if (overflowBehaviorX == OverflowBehavior.CLIP) {
            this.maxHorizontalScroll = 0;
        } else {
            this.maxHorizontalScroll = Math.max(0, getContentWidth() - this.getWidth());
        }
        this.horizontalScrollAmount = Mth.clamp(this.horizontalScrollAmount, 0, this.maxHorizontalScroll);

        if (overflowBehaviorY == OverflowBehavior.CLIP) {
            this.maxScroll = 0;
        } else {
            this.maxScroll = Math.max(0, getContentHeight() - this.getHeight());
        }
        this.scrollAmount = Mth.clamp(this.scrollAmount, 0, this.maxScroll);
    }

    protected boolean showScrollbarX() {
        return overflowBehaviorX == OverflowBehavior.SCROLL;
    }

    protected boolean showScrollbarY() {
        return overflowBehaviorY == OverflowBehavior.SCROLL;
    }

    protected int getVerticalScrollbarThumbHeight() {
        return Math.max(8, (int) (getHeight() * (getHeight() / (double) getContentHeight())));
    }

    protected int getVerticalScrollbarMaxThumbOffset() {
        return getHeight() - getVerticalScrollbarThumbHeight();
    }

    protected int getHorizontalScrollbarThumbWidth() {
        return Math.max(8, (int) (getWidth() * (getWidth() / (double) getContentWidth())));
    }

    protected int getHorizontalScrollbarMaxThumbOffset() {
        return getWidth() - getHorizontalScrollbarThumbWidth();
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
        double localX = mouseX + horizontalScrollAmount - getX();
        double localY = mouseY + scrollAmount - getY();

        for (AbstractWidget child : children) {
            int contentX = child.getX();          // 面板内容空间坐标
            int contentY = child.getY();
            int screenX = getX() + contentX - (int) horizontalScrollAmount; // 屏幕坐标
            int screenY = getY() + contentY - (int) scrollAmount;

            // 可见性剔除
            if (screenX + child.getWidth() < getX()) continue;
            if (screenX > getX() + getWidth()) continue;
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
        if (maxScroll > 0 && showScrollbarY()) {
            int thumbHeight = getVerticalScrollbarThumbHeight();
            int thumbY = getY() + (int) (getVerticalScrollbarMaxThumbOffset() * (scrollAmount / maxScroll));
            int barX = getX() + getWidth() - scrollBarWidth;
            g.fill(barX, getY(), barX + scrollBarWidth, getY() + getHeight(), 0x40000000);
            g.fill(barX, thumbY, barX + scrollBarWidth, thumbY + thumbHeight, 0xA0E0E0E0);
        }
        if (maxHorizontalScroll > 0 && showScrollbarX()) {
            int thumbWidth = getHorizontalScrollbarThumbWidth();
            int thumbX = getX() + (int) (getHorizontalScrollbarMaxThumbOffset() * (horizontalScrollAmount / maxHorizontalScroll));
            int barY = getY() + getHeight() - scrollBarWidth;
            g.fill(getX(), barY, getX() + getWidth(), barY + scrollBarWidth, 0x40000000);
            g.fill(thumbX, barY, thumbX + thumbWidth, barY + scrollBarWidth, 0xA0E0E0E0);
        }
    }

    // ---------- 事件转发 ----------

    /**
     * 把屏幕坐标转换成"内容坐标系"坐标
     */
    protected double toContentX(double mouseX) {
        return mouseX + horizontalScrollAmount;
    }

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

        if (tryScrollbarClick(mouseX, mouseY)) {
            return true;
        }

        double localX = toContentX(mouseX) - getX();
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

    /**
     * 点击滚动条：命中 thumb 时抓取并开始拖动；命中轨道时把 thumb 跳到点击处。
     * 返回 true 表示点击已被滚动条消费，不再穿透到子控件。
     */
    protected boolean tryScrollbarClick(double mouseX, double mouseY) {
        if (maxScroll > 0 && showScrollbarY()) {
            int barX = getX() + getWidth() - scrollBarWidth;
            if (mouseX >= barX && mouseX < barX + scrollBarWidth
                    && mouseY >= getY() && mouseY < getY() + getHeight()) {
                double thumbOffset = getVerticalScrollbarMaxThumbOffset();
                double thumbHeight = getVerticalScrollbarThumbHeight();
                double thumbY = getY() + thumbOffset * (scrollAmount / maxScroll);
                if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
                    dragGrabOffsetY = mouseY - thumbY;
                } else {
                    dragGrabOffsetY = thumbHeight / 2.0;
                    double grabY = Mth.clamp(mouseY - getY() - dragGrabOffsetY, 0, thumbOffset);
                    scrollAmount = (grabY / thumbOffset) * maxScroll;
                }
                draggingScrollbarY = true;
                return true;
            }
        }
        if (maxHorizontalScroll > 0 && showScrollbarX()) {
            int barY = getY() + getHeight() - scrollBarWidth;
            if (mouseY >= barY && mouseY < barY + scrollBarWidth
                    && mouseX >= getX() && mouseX < getX() + getWidth()) {
                double thumbOffset = getHorizontalScrollbarMaxThumbOffset();
                double thumbWidth = getHorizontalScrollbarThumbWidth();
                double thumbX = getX() + thumbOffset * (horizontalScrollAmount / maxHorizontalScroll);
                if (mouseX >= thumbX && mouseX < thumbX + thumbWidth) {
                    dragGrabOffsetX = mouseX - thumbX;
                } else {
                    dragGrabOffsetX = thumbWidth / 2.0;
                    double grabX = Mth.clamp(mouseX - getX() - dragGrabOffsetX, 0, thumbOffset);
                    horizontalScrollAmount = (grabX / thumbOffset) * maxHorizontalScroll;
                }
                draggingScrollbarX = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbarX = false;
        draggingScrollbarY = false;
        double localX = toContentX(mouseX) - getX();
        double localY = toContentY(mouseY) - getY();
        boolean handled = false;
        for (AbstractWidget child : children) {
            if (child.mouseReleased(localX, localY, button)) handled = true;
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbarY) {
            double thumbOffset = getVerticalScrollbarMaxThumbOffset();
            double ratio = Mth.clamp((mouseY - getY() - dragGrabOffsetY) / thumbOffset, 0, 1);
            scrollAmount = ratio * maxScroll;
            return true;
        }
        if (draggingScrollbarX) {
            double thumbOffset = getHorizontalScrollbarMaxThumbOffset();
            double ratio = Mth.clamp((mouseX - getX() - dragGrabOffsetX) / thumbOffset, 0, 1);
            horizontalScrollAmount = ratio * maxHorizontalScroll;
            return true;
        }
        double localX = toContentX(mouseX) - getX();
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
        double localX = toContentX(mouseX) - getX();
        double localY = toContentY(mouseY) - getY();
        for (int i = children.size() - 1; i >= 0; i--) {
            AbstractWidget child = children.get(i);
            if (child.isMouseOver(localX, localY)
                    && child.mouseScrolled(localX, localY, scrollX, scrollY)) {
                return true;
            }
        }

        // 子控件没消费，就滚动自己
        boolean horizontal = Screen.hasShiftDown() || maxScroll <= 0;
        if (horizontal && maxHorizontalScroll > 0) {
            double amount = scrollX != 0 ? scrollX : scrollY;
            horizontalScrollAmount = Mth.clamp(horizontalScrollAmount - amount * scrollStep, 0, maxHorizontalScroll);
            return true;
        }
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