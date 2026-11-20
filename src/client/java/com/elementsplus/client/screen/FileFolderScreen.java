package com.elementsplus.client.screen;

import com.elementsplus.client.gui.GuiUtil;
import com.elementsplus.client.gui.ScrollPanelWidget;
import com.elementsplus.core.file.FileFolderData;
import com.elementsplus.menu.FileFolderMenu;
import com.elementsplus.network.FileFolderActionPayload;
import com.elementsplus.network.FileFolderRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** 文件夹界面：数据量/物品数双进度条 + 搜索/排序列表 + 玩家物品栏。 */
public class FileFolderScreen extends AbstractContainerScreen<FileFolderMenu> {
    private static final int TABLE_X = 8;
    private static final int TABLE_Y = 68;
    private static final int TABLE_WIDTH = 304;
    private static final int TABLE_HEIGHT = 110;
    private static final int HEADER_Y = 50;
    private static final int HEADER_HEIGHT = 16;
    private static final int ROW_HEIGHT = 18;
    private static final int COL_ICON = 0;
    private static final int COL_NAME = 20;
    private static final int COL_TYPE = 190;
    private static final int COL_SIZE = 260;

    private enum SortColumn {NAME, TYPE, SIZE}

    private record Entry(int index, ItemStack stack) {
    }

    private final List<ItemStack> contents = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private int capacityItems = 50;
    private int capacityBytes = 1024 * 1024;
    private @Nullable EditBox searchBox;
    private @Nullable ScrollPanelWidget table;
    private @Nullable FolderRowWidget hoveredRow;
    private SortColumn sortColumn = SortColumn.NAME;
    private boolean ascending = true;

    public FileFolderScreen(FileFolderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = FileFolderMenu.IMAGE_WIDTH;
        this.imageHeight = FileFolderMenu.IMAGE_HEIGHT;
        this.inventoryLabelY = FileFolderMenu.INVENTORY_Y - 12;
    }

    @Override
    protected void init() {
        super.init();
        searchBox = new EditBox(this.font, leftPos + 8, topPos + 30, 230, 16,
                Component.translatable("gui.elements-plus.file_folder.search"));
        searchBox.setHint(Component.translatable("gui.elements-plus.file_folder.search_hint"));
        searchBox.setResponder(value -> refreshEntries(true));
        this.addRenderableWidget(searchBox);

        table = new ScrollPanelWidget(leftPos + TABLE_X, topPos + TABLE_Y,
                TABLE_WIDTH, TABLE_HEIGHT, GuiUtil.SubPanelType.BOX, 0xFFD0D0D0);
        table.overflowBehaviorX = ScrollPanelWidget.OverflowBehavior.CLIP;
        this.addRenderableWidget(table);
        refreshEntries(false);
        ClientPlayNetworking.send(new FileFolderRequestPayload(this.menu.containerId));
    }

    public void onData(int containerId, List<ItemStack> items, int capacityItems,
                       int capacityBytes, ItemStack carried, int lockedInventoryIndex) {
        if (containerId != this.menu.containerId) {
            return;
        }
        this.capacityItems = Math.max(0, capacityItems);
        this.capacityBytes = Math.max(0, capacityBytes);
        this.menu.setCarried(carried);
        this.menu.setLockedInventoryIndex(lockedInventoryIndex);
        this.contents.clear();
        for (ItemStack stack : items) {
            if (FileFolderData.isStorable(stack)) {
                this.contents.add(stack.copy());
            }
        }
        refreshEntries(false);
    }

    private void refreshEntries(boolean resetScroll) {
        entries.clear();
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        for (int index = 0; index < contents.size(); index++) {
            ItemStack stack = contents.get(index);
            if (query.isEmpty() || searchText(stack).contains(query)) {
                entries.add(new Entry(index, stack));
            }
        }
        Comparator<Entry> inner = switch (sortColumn) {
            case NAME -> Comparator.comparing(entry -> entry.stack().getHoverName().getString()
                    .toLowerCase(Locale.ROOT));
            case TYPE -> Comparator.comparing(entry -> typeKey(entry.stack()));
            case SIZE -> Comparator.comparingInt(entry -> FileFolderData.fileBytes(entry.stack()));
        };
        if (!ascending) {
            inner = inner.reversed();
        }
        entries.sort(Comparator.comparing((Entry entry) -> !FileFolderData.isFolder(entry.stack()))
                .thenComparing(inner));
        if (table == null) {
            return;
        }
        table.clearChildren();
        int y = 0;
        for (Entry entry : entries) {
            table.addChild(new FolderRowWidget(0, y, TABLE_WIDTH - 2, entry));
            y += ROW_HEIGHT;
        }
        if (resetScroll) {
            table.scrollToTop();
        }
        hoveredRow = null;
    }

    private static String searchText(ItemStack stack) {
        return (stack.getHoverName().getString() + " " + typeKey(stack)).toLowerCase(Locale.ROOT);
    }

    private static String typeKey(ItemStack stack) {
        if (FileFolderData.isFolder(stack)) {
            return "folder";
        }
        return stack.is(com.elementsplus.ModItems.TEXT_FILE) ? "text" : "punched";
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiUtil.drawMainPanel(graphics, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFFFFFFF);
        GuiUtil.drawSubPanel(graphics, leftPos + TABLE_X, topPos + HEADER_Y,
                leftPos + TABLE_X + TABLE_WIDTH, topPos + HEADER_Y + HEADER_HEIGHT,
                0xFFA0A0A0, GuiUtil.SubPanelType.BOX);
        for (var slot : this.menu.slots) {
            GuiUtil.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFF202020, false);

        int usedBytes = FileFolderData.totalBytes(contents);
        int usedItems = FileFolderData.itemCount(contents);
        drawBar(graphics, 8, 17, imageWidth - 8, usedBytes, capacityBytes, 0xFF22A040);
        drawBar(graphics, 8, 23, imageWidth - 8, usedItems, capacityItems, 0xFF3080E0);
        String values = usedItems + "/" + capacityItems + "  " + formatSize(usedBytes) + "/"
                + formatSize(capacityBytes);
        graphics.drawString(font, values, imageWidth - 8 - font.width(values), 5, 0xFF404040, false);

        drawHeader(graphics, "gui.elements-plus.file_folder.header.name", COL_NAME, SortColumn.NAME);
        drawHeader(graphics, "gui.elements-plus.file_folder.header.type", COL_TYPE, SortColumn.TYPE);
        drawHeader(graphics, "gui.elements-plus.file_folder.header.size", COL_SIZE, SortColumn.SIZE);
        graphics.drawString(font, Component.translatable("gui.elements-plus.file_folder.inventory"),
                80, inventoryLabelY, 0xFF606060, false);
    }

    private void drawHeader(GuiGraphics graphics, String key, int x, SortColumn column) {
        String text = Component.translatable(key).getString();
        if (sortColumn == column) {
            text += ascending ? " ^" : " v";
        }
        int color = sortColumn == column ? 0xFF202020 : 0xFF505050;
        graphics.drawString(font, text, TABLE_X + x + 2, HEADER_Y + 4, color, false);
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, int right,
                                int used, int capacity, int color) {
        int width = Math.max(1, right - x);
        graphics.fill(x, y, right, y + 4, 0xFF505050);
        int fill = capacity <= 0 ? 0 : (int) Math.min(width, (long) width * used / capacity);
        if (fill > 0) {
            graphics.fill(x, y, x + fill, y + 4, color);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoveredRow = null;
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hoveredRow != null) {
            graphics.renderTooltip(font, hoveredRow.entry.stack(), mouseX, mouseY);
        } else {
            this.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHeader(mouseX, mouseY)) {
            SortColumn column = headerColumnAt(mouseX);
            if (column != null) {
                if (sortColumn == column) {
                    ascending = !ascending;
                } else {
                    sortColumn = column;
                    ascending = true;
                }
                refreshEntries(true);
                return true;
            }
        }
        // 手持文件/文件夹点击表格空白处即可放入。
        if (button == 0 && isTable(mouseX, mouseY)
                && FileFolderData.isStorable(menu.getCarried())) {
            sendAction(FileFolderActionPayload.Action.INSERT_CARRIED, -1);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHeader(double mouseX, double mouseY) {
        return mouseX >= leftPos + TABLE_X && mouseX < leftPos + TABLE_X + TABLE_WIDTH
                && mouseY >= topPos + HEADER_Y && mouseY < topPos + HEADER_Y + HEADER_HEIGHT;
    }

    private boolean isTable(double mouseX, double mouseY) {
        return mouseX >= leftPos + TABLE_X && mouseX < leftPos + TABLE_X + TABLE_WIDTH
                && mouseY >= topPos + TABLE_Y && mouseY < topPos + TABLE_Y + TABLE_HEIGHT;
    }

    private @Nullable SortColumn headerColumnAt(double mouseX) {
        int x = (int) (mouseX - leftPos - TABLE_X);
        if (x >= COL_NAME && x < COL_TYPE) return SortColumn.NAME;
        if (x >= COL_TYPE && x < COL_SIZE) return SortColumn.TYPE;
        if (x >= COL_SIZE) return SortColumn.SIZE;
        return null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused()
                && searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (searchBox != null && searchBox.isFocused() && searchBox.charTyped(c, modifiers)) {
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    private void sendAction(FileFolderActionPayload.Action action, int index) {
        ClientPlayNetworking.send(new FileFolderActionPayload(menu.containerId, action, index));
    }

    private static String fit(Font font, String text, int width) {
        if (width <= 0 || text == null || font.width(text) <= width) {
            return text == null ? "" : text;
        }
        String ellipsis = "...";
        if (width <= font.width(ellipsis)) {
            return font.plainSubstrByWidth(text, width);
        }
        return font.plainSubstrByWidth(text, width - font.width(ellipsis)) + ellipsis;
    }

    private static String formatSize(int bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private final class FolderRowWidget extends AbstractWidget {
        private final Entry entry;

        private FolderRowWidget(int x, int y, int width, Entry entry) {
            super(x, y, width, ROW_HEIGHT, Component.empty());
            this.entry = entry;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (isHovered()) {
                hoveredRow = this;
                graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1,
                        getY() + getHeight() - 1, 0x30FFFFFF);
            }
            graphics.renderFakeItem(entry.stack(), getX() + COL_ICON + 1, getY() + 1);
            Font font = Minecraft.getInstance().font;
            int textY = getY() + (ROW_HEIGHT - font.lineHeight) / 2;
            graphics.drawString(font, fit(font, entry.stack().getHoverName().getString(),
                            COL_TYPE - COL_NAME - 4),
                    getX() + COL_NAME + 2, textY, 0xFF202020, false);
            graphics.drawString(font, fit(font, typeLabel(entry.stack()).getString(),
                            COL_SIZE - COL_TYPE - 4),
                    getX() + COL_TYPE + 2, textY, 0xFF404040, false);
            String size = formatSize(FileFolderData.fileBytes(entry.stack()));
            graphics.drawString(font, size,
                    getX() + getWidth() - 2 - font.width(size), textY, 0xFF404040, false);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (Screen.hasShiftDown()) {
                sendAction(FileFolderActionPayload.Action.TAKE_TO_INVENTORY, entry.index());
                return;
            }
            if (menu.getCarried().isEmpty()) {
                sendAction(FileFolderActionPayload.Action.TAKE_TO_CURSOR, entry.index());
            } else if (FileFolderData.isStorable(menu.getCarried())) {
                sendAction(FileFolderActionPayload.Action.INSERT_CARRIED, -1);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }

    private static Component typeLabel(ItemStack stack) {
        if (FileFolderData.isFolder(stack)) {
            return Component.translatable("gui.elements-plus.file_folder.type.folder");
        }
        return Component.translatable(stack.is(com.elementsplus.ModItems.TEXT_FILE)
                ? "gui.elements-plus.file_folder.type.text"
                : "gui.elements-plus.file_folder.type.punched");
    }
}
