package com.elementsplus.client.gui;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModDataComponents;
import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.player.PlayerToolbox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * 玩家可自定义的元件列表。作为 ScrollPanelWidget 的唯一子控件渲染，
 * 负责：折叠/展开（松开执行）、拿取元件（松开执行）、真实元件插入、
 * 分组拖动排序、元件拖动排序、右键菜单触发、拖出边界自动滚动。
 * 事件坐标均为面板内容坐标系；渲染时为屏幕坐标。
 */
public class PlayerToolboxWidget extends AbstractWidget {
    public static final int HEADER_HEIGHT = 18;
    public static final int ENTRY_HEIGHT = 18;
    public static final int BOTTOM_AREA = 50;
    private static final double DRAG_THRESHOLD = 4.0;
    private static final int SEAM_HALF = 9;
    private static final int AUTOSCROLL_EDGE = 24;
    private static final double AUTOSCROLL_SPEED = 5.0;

    public interface Listener {
        /**
         * 用户对列表做出修改（增删改排序）后回调，用于同步到服务端。
         */
        void onToolboxChanged();

        /**
         * 从列表拿取元件（虚拟放置），由屏幕决定是否可拿取。
         */
        void onPickup(CircuitComponent component);

        /**
         * 右键请求打开菜单；screenX/screenY 为屏幕坐标。
         */
        void onRequestMenu(int screenX, int screenY, MenuRequest request);
    }

    public record MenuRequest(Kind kind, int groupIndex, int entryIndex) {
        public enum Kind {CUSTOM_ENTRY, BUILTIN_GROUP, CUSTOM_GROUP, BOTTOM}
    }

    private enum DragKind {NONE, ENTRY, GROUP}

    private record PressTarget(Kind kind, int groupIndex, int entryIndex) {
        enum Kind {HEADER, ENTRY, SEAM, NEW_GROUP, BOTTOM, EMPTY}
    }

    /**
     * @param groupIndex 分组拖动 = 插入下标；元件拖动/插入 = 目标分组下标
     * @param entryPos   元件拖动/插入 = 组内插入位置
     * @param seamY      内容坐标下的缝隙位置
     * @param newGroup   是否落到底部空白（新建分组）
     */
    private record DropInfo(int groupIndex, int entryPos, int seamY, boolean newGroup) {
    }

    private final ScrollPanelWidget panel;
    private final Listener listener;
    private final BooleanSupplier readOnly;
    private final Supplier<ItemStack> carried;

    private PlayerToolbox toolbox;

    private final Map<String, Boolean> expanded = new HashMap<>();
    private boolean collapsedForDrag = false;

    // 按下状态
    private int pressButton = -1;
    private double pressX, pressY;
    private PressTarget pressTarget;

    // 拖动状态
    private boolean dragging = false;
    private DragKind dragKind = DragKind.NONE;
    private int dragSourceGroup = -1;
    private int dragSourceEntry = -1;

    public PlayerToolboxWidget(int x, int y, int width, ScrollPanelWidget panel,
                               Listener listener, PlayerToolbox toolbox,
                               BooleanSupplier readOnly, Supplier<ItemStack> carried) {
        super(x, y, width, HEADER_HEIGHT, Component.empty());
        this.panel = panel;
        this.listener = listener;
        this.readOnly = readOnly;
        this.carried = carried;
        this.toolbox = toolbox;
        refreshLayout();
    }

    // ───────── 布局 ─────────

    private boolean isGroupExpanded(int i) {
        if (collapsedForDrag) return false;
        PlayerToolbox.Group g = toolbox.groups.get(i);
        String key = g.isBuiltin() ? "b:" + g.id : "c:" + g.name;
        return expanded.getOrDefault(key, true);
    }

    private int groupHeight(int i) {
        int rows = isGroupExpanded(i) ? toolbox.groups.get(i).components.size() : 0;
        return HEADER_HEIGHT + rows * ENTRY_HEIGHT;
    }

    private int groupTop(int i) {
        int y = 0;
        for (int k = 0; k < i; k++) y += groupHeight(k);
        return y;
    }

    private int groupEnd(int i) {
        return groupTop(i) + groupHeight(i);
    }

    private int contentHeight() {
        int y = 0;
        for (int i = 0; i < toolbox.groups.size(); i++) y += groupHeight(i);
        return y + BOTTOM_AREA;
    }

    private void refreshLayout() {
        this.height = contentHeight();
    }

    public void setToolbox(PlayerToolbox toolbox) {
        this.toolbox = toolbox;
        cancelDrag();
        refreshLayout();
    }

    public PlayerToolbox getToolbox() {
        return toolbox;
    }

    /**
     * 供屏幕读取（重命名对话框初始值）。
     */
    public String getGroupName(int groupIndex) {
        return toolbox.groups.get(groupIndex).name;
    }

    public boolean isGroupEmpty(int groupIndex) {
        return toolbox.groups.get(groupIndex).components.isEmpty();
    }

    public boolean isGroupBuiltin(int groupIndex) {
        return toolbox.groups.get(groupIndex).isBuiltin();
    }

    // ───────── 坐标转换 ─────────

    private double contentMouseX(double screenX) {
        return screenX - panel.getX() + panel.getHorizontalScrollAmount();
    }

    private double contentMouseY(double screenY) {
        return screenY - panel.getY() + panel.getScrollAmount();
    }

    private double screenX(double contentX) {
        return panel.getX() + contentX - panel.getHorizontalScrollAmount();
    }

    private double screenY(double contentY) {
        return panel.getY() + contentY - panel.getScrollAmount();
    }

    // ───────── 命中解析 ─────────

    private boolean hasRealCarried() {
        if (readOnly.getAsBoolean()) return false;
        ItemStack stack = carried.get();
        return stack != null && !stack.isEmpty() && stack.get(ModDataComponents.EQUIVALENT_COMPONENT) != null;
    }

    private ResourceLocation carriedComponentId() {
        ItemStack stack = carried.get();
        return stack == null ? null : stack.get(ModDataComponents.EQUIVALENT_COMPONENT);
    }

    private PressTarget resolvePressTarget(double y) {
        boolean carriedReady = hasRealCarried();
        for (int i = 0; i < toolbox.groups.size(); i++) {
            int top = groupTop(i);
            int end = groupEnd(i);
            if (y >= top && y < top + HEADER_HEIGHT) {
                return new PressTarget(PressTarget.Kind.HEADER, i, -1);
            }
            if (y >= top + HEADER_HEIGHT && y < end) {
                if (carriedReady) {
                    DropInfo d = resolveEntryDrop(y);
                    if (d != null) {
                        if (d.newGroup) return new PressTarget(PressTarget.Kind.NEW_GROUP, -1, -1);
                        return new PressTarget(PressTarget.Kind.SEAM, d.groupIndex, d.entryPos);
                    }
                    return new PressTarget(PressTarget.Kind.EMPTY, -1, -1);
                }
                int entry = (int) ((y - top - HEADER_HEIGHT) / ENTRY_HEIGHT);
                return new PressTarget(PressTarget.Kind.ENTRY, i, entry);
            }
        }
        if (y >= contentHeight() - BOTTOM_AREA) {
            return new PressTarget(carriedReady ? PressTarget.Kind.NEW_GROUP : PressTarget.Kind.BOTTOM, -1, -1);
        }
        return new PressTarget(PressTarget.Kind.EMPTY, -1, -1);
    }

    private MenuRequest resolveMenuRequest(double y) {
        for (int i = 0; i < toolbox.groups.size(); i++) {
            int top = groupTop(i);
            int end = groupEnd(i);
            if (y >= top && y < top + HEADER_HEIGHT) {
                boolean builtin = toolbox.groups.get(i).isBuiltin();
                return new MenuRequest(builtin ? MenuRequest.Kind.BUILTIN_GROUP : MenuRequest.Kind.CUSTOM_GROUP, i, -1);
            }
            if (y >= top + HEADER_HEIGHT && y < end) {
                if (toolbox.groups.get(i).isBuiltin()) return null;
                int entry = (int) ((y - top - HEADER_HEIGHT) / ENTRY_HEIGHT);
                return new MenuRequest(MenuRequest.Kind.CUSTOM_ENTRY, i, entry);
            }
        }
        if (y >= contentHeight() - BOTTOM_AREA) {
            return new MenuRequest(MenuRequest.Kind.BOTTOM, -1, -1);
        }
        return null;
    }

    /**
     * 元件/真实物品的插入目标：只允许落在自定义（且展开）分组的缝隙。
     */
    private DropInfo resolveEntryDrop(double y) {
        DropInfo best = null;
        int bestDist = Integer.MAX_VALUE;
        int n = toolbox.groups.size();
        for (int i = 0; i < n; i++) {
            PlayerToolbox.Group g = toolbox.groups.get(i);
            if (g.isBuiltin() || !isGroupExpanded(i)) continue;
            int start = groupTop(i) + HEADER_HEIGHT;
            int size = g.components.size();
            for (int j = 0; j <= size; j++) {
                int by = start + j * ENTRY_HEIGHT;
                int d = Math.abs((int) y - by);
                if (d < bestDist) {
                    bestDist = d;
                    best = new DropInfo(i, j, by, false);
                }
            }
        }
        if (best != null && bestDist < SEAM_HALF) return best;
        if (y >= contentHeight() - BOTTOM_AREA) {
            return new DropInfo(-1, -1, contentHeight() - BOTTOM_AREA + 6, true);
        }
        return null;
    }

    /**
     * 分组拖动目标：分组边界缝隙，返回的 groupIndex 为插入下标。
     */
    private DropInfo resolveGroupDrop(double y) {
        DropInfo best = null;
        int bestDist = Integer.MAX_VALUE;
        if (toolbox.groups.isEmpty()) return null;
        int by = groupTop(0);
        int d = Math.abs((int) y - by);
        bestDist = d;
        best = new DropInfo(0, -1, by, false);
        for (int i = 0; i < toolbox.groups.size(); i++) {
            by = groupEnd(i);
            d = Math.abs((int) y - by);
            if (d < bestDist) {
                bestDist = d;
                best = new DropInfo(i + 1, -1, by, false);
            }
        }
        return bestDist < SEAM_HALF ? best : null;
    }

    // ───────── 变异操作（均触发同步） ─────────

    private void markChanged() {
        refreshLayout();
        listener.onToolboxChanged();
    }

    public void toggleGroup(int groupIndex) {
        PlayerToolbox.Group g = toolbox.groups.get(groupIndex);
        String key = g.isBuiltin() ? "b:" + g.id : "c:" + g.name;
        expanded.put(key, !isGroupExpanded(groupIndex));
        refreshLayout();
    }

    public void insertGroupBefore(int groupIndex) {
        List<PlayerToolbox.Group> groups = toolbox.groups;
        groups.add(Math.min(groupIndex, groups.size()),
                new PlayerToolbox.Group(localized("gui.elements-plus.toolbox.new_group")));
        markChanged();
    }

    public void addGroup(String name) {
        toolbox.groups.add(new PlayerToolbox.Group(name));
        markChanged();
    }

    public void addGroupWithCarried() {
        ResourceLocation id = carriedComponentId();
        if (id == null || BuiltinCircuitComponents.byId(id) == null) return;
        toolbox.groups.add(new PlayerToolbox.Group(localized("gui.elements-plus.toolbox.new_group"), id));
        markChanged();
    }

    public void removeEntry(int groupIndex, int entryIndex) {
        PlayerToolbox.Group g = toolbox.groups.get(groupIndex);
        if (g.isBuiltin()) return;
        if (entryIndex < 0 || entryIndex >= g.components.size()) return;
        g.components.remove(entryIndex);
        markChanged();
    }

    public void renameGroup(int groupIndex, String name) {
        PlayerToolbox.Group g = toolbox.groups.get(groupIndex);
        if (g.isBuiltin() || name == null) return;
        g.name = name;
        markChanged();
    }

    public void deleteGroup(int groupIndex) {
        PlayerToolbox.Group g = toolbox.groups.get(groupIndex);
        if (g.isBuiltin()) return;
        toolbox.groups.remove(groupIndex);
        markChanged();
    }

    private void pickupEntry(int groupIndex, int entryIndex) {
        PlayerToolbox.Group g = toolbox.groups.get(groupIndex);
        if (entryIndex < 0 || entryIndex >= g.components.size()) return;
        CircuitComponent component = BuiltinCircuitComponents.byId(g.components.get(entryIndex));
        if (component != null) listener.onPickup(component);
    }

    private void insertCarried(int groupIndex, int entryPos) {
        ResourceLocation id = carriedComponentId();
        PlayerToolbox.Group g = toolbox.groups.get(groupIndex);
        if (id == null || g == null || g.isBuiltin()) return;
        if (BuiltinCircuitComponents.byId(id) == null) return;
        g.components.add(Math.min(entryPos, g.components.size()), id);
        markChanged();
    }

    private static String localized(String key) {
        return Component.translatable(key).getString();
    }

    // ───────── 输入 ─────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible) return false;
        if (mouseX < 0 || mouseX > getWidth() || mouseY < 0 || mouseY > contentHeight()) return false;
        pressButton = button;
        pressX = mouseX;
        pressY = mouseY;
        dragging = false;
        dragKind = DragKind.NONE;
        pressTarget = resolvePressTarget(mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (pressButton != button) return false;
        if (!dragging) {
            if (Math.hypot(mouseX - pressX, mouseY - pressY) < DRAG_THRESHOLD) {
                return true;
            }
            dragging = true;
            if (pressTarget != null && pressTarget.kind == PressTarget.Kind.ENTRY
                    && !toolbox.groups.get(pressTarget.groupIndex).isBuiltin()) {
                dragKind = DragKind.ENTRY;
                dragSourceGroup = pressTarget.groupIndex;
                dragSourceEntry = pressTarget.entryIndex;
            } else if (pressTarget != null && pressTarget.kind == PressTarget.Kind.HEADER) {
                dragKind = DragKind.GROUP;
                dragSourceGroup = pressTarget.groupIndex;
                collapsedForDrag = true;
            } else {
                dragKind = DragKind.NONE;
            }
            if (dragKind != DragKind.NONE) refreshLayout();
        }
        if (dragKind != DragKind.NONE) {
            autoscroll(mouseY);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (pressButton != button) return false;
        pressButton = -1;
        boolean wasDragging = dragging;
        dragging = false;
        if (wasDragging) {
            finishDrag(mouseY);
            return true;
        }
        if (Math.hypot(mouseX - pressX, mouseY - pressY) < DRAG_THRESHOLD) {
            if (button == 1) {
                MenuRequest req = resolveMenuRequest(pressY);
                if (req != null) {
                    listener.onRequestMenu((int) screenX(pressX), (int) screenY(pressY), req);
                }
            } else if (pressTarget != null) {
                executePress();
            }
        }
        pressTarget = null;
        return true;
    }

    private void executePress() {
        PressTarget t = pressTarget;
        if (t == null) return;
        switch (t.kind) {
            case HEADER -> toggleGroup(t.groupIndex);
            case ENTRY -> pickupEntry(t.groupIndex, t.entryIndex);
            case SEAM -> insertCarried(t.groupIndex, t.entryIndex);
            case NEW_GROUP -> addGroupWithCarried();
            case BOTTOM, EMPTY -> {
            }
        }
    }

    private void finishDrag(double y) {
        try {
            if (dragKind == DragKind.ENTRY) {
                DropInfo d = resolveEntryDrop(y);
                if (d != null && !d.newGroup) {
                    PlayerToolbox.Group src = toolbox.groups.get(dragSourceGroup);
                    int pos = d.entryPos;
                    if (d.groupIndex == dragSourceGroup && pos > dragSourceEntry) pos--;
                    ResourceLocation id = src.components.remove(dragSourceEntry);
                    toolbox.groups.get(d.groupIndex).components.add(Math.min(pos, toolbox.groups.get(d.groupIndex).components.size()), id);
                    markChanged();
                }
            } else if (dragKind == DragKind.GROUP) {
                DropInfo d = resolveGroupDrop(y);
                if (d != null) {
                    int pos = d.groupIndex;
                    if (pos > dragSourceGroup) pos--;
                    PlayerToolbox.Group g = toolbox.groups.remove(dragSourceGroup);
                    toolbox.groups.add(Math.min(pos, toolbox.groups.size()), g);
                    markChanged();
                }
            }
        } finally {
            cancelDrag();
        }
    }

    private void cancelDrag() {
        dragKind = DragKind.NONE;
        dragSourceGroup = -1;
        dragSourceEntry = -1;
        collapsedForDrag = false;
        refreshLayout();
    }

    private void autoscroll(double curY) {
        double scrollTop = panel.getScrollAmount();
        double viewBottom = scrollTop + panel.getHeight();
        if (curY < scrollTop + AUTOSCROLL_EDGE) {
            panel.scrollBy(-AUTOSCROLL_SPEED);
        } else if (curY > viewBottom - AUTOSCROLL_EDGE) {
            panel.scrollBy(AUTOSCROLL_SPEED);
        }
    }

    // ───────── 渲染 ─────────

    @Override
    protected void renderWidget(GuiGraphics g, int screenMouseX, int screenMouseY, float partialTick) {
        if (toolbox == null) return;
        Font font = Minecraft.getInstance().font;
        double curY = contentMouseY(screenMouseY);

        for (int i = 0; i < toolbox.groups.size(); i++) {
            renderGroup(g, font, i, screenMouseX, screenMouseY, curY);
        }
        renderBottom(g, font);

        boolean carriedReady = hasRealCarried();
        if (dragKind == DragKind.ENTRY) {
            DropInfo d = resolveEntryDrop(curY);
            if (d != null) {
                if (d.newGroup) renderBottomRect(g);
                else renderSeam(g, d.seamY);
            }
            renderEntryGhost(g, screenMouseX, screenMouseY);
        } else if (dragKind == DragKind.GROUP) {
            DropInfo d = resolveGroupDrop(curY);
            if (d != null) renderSeam(g, d.seamY);
            renderGroupGhost(g, font, screenMouseX, screenMouseY);
        } else if (carriedReady && !dragging) {
            DropInfo d = resolveEntryDrop(curY);
            if (d != null) {
                if (d.newGroup) renderBottomRect(g);
                else renderSeam(g, d.seamY);
            }
        }
    }

    private void renderGroup(GuiGraphics g, Font font, int i, int smx, int smy, double curY) {
        int x = getX();
        int w = getWidth();
        int top = groupTop(i);
        boolean expanded = isGroupExpanded(i);

        int hy = (int) screenY(top);
//        g.fill(x, hy, x + w, hy + HEADER_HEIGHT, 0xFF000000);
        boolean headerHovered = false;
        if (this.isHovered()) {
            PressTarget pressTarget1 = resolvePressTarget(curY);
            headerHovered = pressTarget1.kind == PressTarget.Kind.HEADER && pressTarget1.groupIndex == i;
        }
        g.fill(x, hy, x + w, hy + HEADER_HEIGHT, headerHovered ? 0xFFFFFFFF : 0xFF000000);
        GuiUtil.drawSubPanel(g, x + 1, hy + 1, x + w - 1, hy + HEADER_HEIGHT - 1,
                expanded ? 0xFFA0A0A0 : 0xFF808080, GuiUtil.SubPanelType.CONVEX);
        if (expanded) {
            g.blit(ElementsPlus.id("textures/gui/widget.png"), x + w - 11, hy + HEADER_HEIGHT / 2 - 1, 0, 0, 5, 5);
        } else {
            g.blit(ElementsPlus.id("textures/gui/widget.png"), x + w - 10, hy + HEADER_HEIGHT / 2 - 2, 5, 0, 5, 5);
        }
        g.drawString(font, toolbox.groups.get(i).displayName(), x + 4, hy + (HEADER_HEIGHT - 9) / 2, 0xFFFFFFFF, false);

        if (!expanded) return;
        PlayerToolbox.Group group = toolbox.groups.get(i);
        for (int j = 0; j < group.components.size(); j++) {
            CircuitComponent component = BuiltinCircuitComponents.byId(group.components.get(j));
            if (component == null) continue;
            int ey = (int) screenY(top + HEADER_HEIGHT + j * ENTRY_HEIGHT);
            boolean hovered = smx >= x + 1 && smx < x + w - 1 && smy >= ey && smy < ey + ENTRY_HEIGHT;
            g.fill(x + 1, ey, x + w - 1, ey + ENTRY_HEIGHT, hovered ? 0xFFA0A0A0 : 0xFF808080);
            ResourceLocation icon = component.getIcon();
            if (icon != null) {
                g.blit(icon, x + 6, ey + 1, 0, 0, 16, 16, 16, 16);
            }
            g.drawString(font, component.getName(), x + 24, ey + (ENTRY_HEIGHT - 9) / 2, 0xFFFFFFFF, false);
        }
    }

    private void renderBottom(GuiGraphics g, Font font) {
        int x = getX();
        int w = getWidth();
        int bottomTop = contentHeight() - BOTTOM_AREA;
        int by = (int) screenY(bottomTop);
        Component hint = Component.translatable("gui.elements-plus.toolbox.bottom_hint");
        g.drawString(font, hint, x + (w - font.width(hint)) / 2, by + (BOTTOM_AREA - 9) / 2, 0xFFE0E0E0, false);
    }

    private void renderBottomRect(GuiGraphics g) {
        int x = getX() + 5;
        int w = getWidth() - 10;
        int h = 24;
        int by = (int) screenY(contentHeight() - BOTTOM_AREA + 6);
        int c = 0xFF00E5FF;
        g.fill(x, by, x + w, by + 1, c);
        g.fill(x, by + h - 1, x + w, by + h, c);
        g.fill(x, by, x + 1, by + h, c);
        g.fill(x + w - 1, by, x + w, by + h, c);
    }

    private void renderSeam(GuiGraphics g, int contentY) {
        int x = getX() + 1;
        int w = getWidth() - 2;
        int sy = (int) screenY(contentY);
        g.fill(x, sy - 2, x + w, sy + 2, 0xFF00E5FF);
    }

    private void renderEntryGhost(GuiGraphics g, int smx, int smy) {
        if (dragSourceGroup < 0 || dragSourceEntry < 0) return;
        ResourceLocation id = toolbox.groups.get(dragSourceGroup).components.get(dragSourceEntry);
        CircuitComponent component = BuiltinCircuitComponents.byId(id);
        if (component == null) return;
        ResourceLocation icon = component.getIcon();
        if (icon != null) {
            g.blit(icon, smx - 8, smy - 8, 0, 0, 16, 16, 16, 16);
        } else {
            g.fill(smx - 8, smy - 8, smx + 8, smy + 8, 0xC0000000);
            g.drawCenteredString(Minecraft.getInstance().font, component.getName(), smx, smy - 5, 0xFFFFFFFF);
        }
    }

    private void renderGroupGhost(GuiGraphics g, Font font, int smx, int smy) {
        if (dragSourceGroup < 0) return;
        PlayerToolbox.Group group = toolbox.groups.get(dragSourceGroup);
        int w = 72;
        int h = HEADER_HEIGHT;
        int gx = smx - 8;
        int gy = smy - 8;
        g.fill(gx, gy, gx + w, gy + h, 0xC0000000);
        g.drawString(font, group.displayName(), gx + 3, gy + (h - 9) / 2, 0xFFFFFFFF, false);
        g.fill(gx + w - 10, gy + h / 2 - 2, gx + w - 5, gy + h / 2 + 3, 0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}