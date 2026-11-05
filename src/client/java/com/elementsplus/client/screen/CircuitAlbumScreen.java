package com.elementsplus.client.screen;

import com.elementsplus.ModItems;
import com.elementsplus.client.gui.GuiUtil;
import com.elementsplus.client.gui.ScrollPanelWidget;
import com.elementsplus.core.circuit.ChipManufacture;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.CircuitComponentRegistry;
import com.elementsplus.core.circuit.CircuitItemData;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.core.circuit.storage.StoredCircuit;
import com.elementsplus.menu.CircuitAlbumMenu;
import com.elementsplus.network.CircuitAlbumActionPayload;
import com.elementsplus.network.CircuitAlbumRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 无贴图电路图册界面：上方可排序/筛选的表格，下方玩家物品栏。
 */
public class CircuitAlbumScreen extends AbstractContainerScreen<CircuitAlbumMenu> {
    private static final int TABLE_X = 8;
    private static final int TABLE_Y = 56;
    private static final int TABLE_WIDTH = 304;
    private static final int TABLE_HEIGHT = 126;
    private static final int HEADER_Y = 38;
    private static final int HEADER_HEIGHT = 16;
    private static final int ROW_HEIGHT = 18;

    private static final int COL_ICON = 0;
    private static final int COL_ITEM = 18;
    private static final int COL_COMPONENT = 98;
    private static final int COL_SCALE = 178;
    private static final int COL_UUID = 228;

    private static final int COLOR_TEXT = 0xFF202020;
    private static final int COLOR_TEXT_MUTED = 0xFF606060;

    private final List<ItemStack> contents = new ArrayList<>();

    private EditBox searchBox;
    private ScrollPanelWidget table;
    private int capacity = 50;
    private SortColumn sortColumn = SortColumn.COMPONENT;
    private boolean sortAscending = true;
    private @Nullable AlbumRowWidget hoveredRow;

    public CircuitAlbumScreen(CircuitAlbumMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = CircuitAlbumMenu.IMAGE_WIDTH;
        this.imageHeight = CircuitAlbumMenu.IMAGE_HEIGHT;
        this.inventoryLabelY = 184;
    }

    @Override
    protected void init() {
        super.init();

        searchBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 18,
                230, 16, Component.translatable("gui.elements-plus.circuit_album.search"));
        searchBox.setHint(Component.translatable("gui.elements-plus.circuit_album.search_hint"));
        searchBox.setResponder(value -> refreshRows(true));
        this.addRenderableWidget(searchBox);

        table = new ScrollPanelWidget(this.leftPos + TABLE_X, this.topPos + TABLE_Y,
                TABLE_WIDTH, TABLE_HEIGHT, GuiUtil.SubPanelType.BOX, 0xFFD0D0D0);
        table.overflowBehaviorX = ScrollPanelWidget.OverflowBehavior.CLIP;
        this.addRenderableWidget(table);

        if (!contents.isEmpty()) {
            refreshRows(false);
        }
        ClientPlayNetworking.send(new CircuitAlbumRequestPayload(this.menu.containerId));
    }

    public void onData(int containerId, int capacity, List<ItemStack> items, ItemStack carried,
                       int lockedInventoryIndex) {
        if (containerId != this.menu.containerId) {
            return;
        }
        this.capacity = Math.max(0, capacity);
        this.menu.setCarried(carried);
        this.menu.setLockedInventoryIndex(lockedInventoryIndex);
        this.contents.clear();
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && stack.is(ModItems.CIRCUIT_DIAGRAM)) {
                this.contents.add(stack.copy());
            }
        }
        refreshRows(false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiUtil.drawMainPanel(graphics, this.leftPos, this.topPos,
                this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFFFFFFF);

        GuiUtil.drawSubPanel(graphics, this.leftPos + TABLE_X, this.topPos + HEADER_Y,
                this.leftPos + TABLE_X + TABLE_WIDTH, this.topPos + HEADER_Y + HEADER_HEIGHT,
                0xFFA0A0A0, GuiUtil.SubPanelType.BOX);

        for (var slot : this.menu.slots) {
            GuiUtil.drawSlot(graphics, this.leftPos + slot.x - 1, this.topPos + slot.y - 1);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, COLOR_TEXT, false);

        String remaining = this.contents.size() + "/" + this.capacity;
        graphics.drawString(this.font, remaining, this.imageWidth - 8 - this.font.width(remaining),
                6, COLOR_TEXT, false);
        graphics.drawString(this.font, Component.translatable("gui.elements-plus.circuit_album.inventory"),
                80, 184, COLOR_TEXT_MUTED, false);

        drawHeader(graphics, "gui.elements-plus.circuit_album.header.item", COL_ITEM, SortColumn.ITEM);
        drawHeader(graphics, "gui.elements-plus.circuit_album.header.component",
                COL_COMPONENT, SortColumn.COMPONENT);
        drawHeader(graphics, "gui.elements-plus.circuit_album.header.scale", COL_SCALE, SortColumn.SCALE);
        drawHeader(graphics, "gui.elements-plus.circuit_album.header.uuid", COL_UUID, SortColumn.UUID);
    }

    private void drawHeader(GuiGraphics graphics, String key, int x, SortColumn column) {
        String text = Component.translatable(key).getString();
        if (this.sortColumn == column) {
            text += this.sortAscending ? " ^" : " v";
        }
        graphics.drawString(this.font, text, TABLE_X + x + 2, HEADER_Y + 4, COLOR_TEXT, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.hoveredRow = null;
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.hoveredRow != null) {
            graphics.renderTooltip(this.font, this.hoveredRow.entry.stack(), mouseX, mouseY);
        } else {
            this.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHeader(mouseX, mouseY)) {
            SortColumn column = headerColumnAt(mouseX);
            if (column != null) {
                if (this.sortColumn == column) {
                    this.sortAscending = !this.sortAscending;
                } else {
                    this.sortColumn = column;
                    this.sortAscending = true;
                }
                refreshRows(true);
                return true;
            }
        }

        // 手持电路图点击上方表格即可放入，包括表格为空时点击空白区域。
        if (button == 0 && isTable(mouseX, mouseY)
                && this.menu.getCarried().is(ModItems.CIRCUIT_DIAGRAM)
                && this.contents.size() < this.capacity) {
            sendAction(CircuitAlbumActionPayload.Action.INSERT_CARRIED, -1);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHeader(double mouseX, double mouseY) {
        return mouseX >= this.leftPos + TABLE_X && mouseX < this.leftPos + TABLE_X + TABLE_WIDTH
                && mouseY >= this.topPos + HEADER_Y && mouseY < this.topPos + HEADER_Y + HEADER_HEIGHT;
    }

    private boolean isTable(double mouseX, double mouseY) {
        return mouseX >= this.leftPos + TABLE_X && mouseX < this.leftPos + TABLE_X + TABLE_WIDTH
                && mouseY >= this.topPos + TABLE_Y && mouseY < this.topPos + TABLE_Y + TABLE_HEIGHT;
    }

    private @Nullable SortColumn headerColumnAt(double mouseX) {
        int x = (int) (mouseX - this.leftPos - TABLE_X);
        if (x >= COL_ITEM && x < COL_COMPONENT) return SortColumn.ITEM;
        if (x >= COL_COMPONENT && x < COL_SCALE) return SortColumn.COMPONENT;
        if (x >= COL_SCALE && x < COL_UUID) return SortColumn.SCALE;
        if (x >= COL_UUID && x < TABLE_WIDTH) return SortColumn.UUID;
        return null;
    }

    private void sendAction(CircuitAlbumActionPayload.Action action, int index) {
        ClientPlayNetworking.send(new CircuitAlbumActionPayload(this.menu.containerId, action, index));
    }

    private void refreshRows(boolean resetScroll) {
        if (this.table == null) {
            return;
        }
        String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<AlbumEntry> entries = new ArrayList<>();
        for (int i = 0; i < this.contents.size(); i++) {
            AlbumEntry entry = AlbumEntry.of(this.contents.get(i), i);
            if (query.isEmpty() || entry.matches(query)) {
                entries.add(entry);
            }
        }

        Comparator<AlbumEntry> comparator = switch (this.sortColumn) {
            case ITEM -> Comparator.comparing(entry -> entry.itemNameLower);
            case COMPONENT -> Comparator.comparing(entry -> entry.componentNameLower);
            case SCALE -> Comparator.comparingInt(entry ->
                    entry.scale == null ? Integer.MIN_VALUE : entry.scale.ordinal());
            case UUID -> Comparator.comparing(entry -> entry.uuidLower);
        };
        if (this.sortColumn == SortColumn.COMPONENT && this.sortAscending) {
            comparator = Comparator.comparingInt((AlbumEntry entry) -> entry.compiled ? 1 : 0)
                    .thenComparing(entry -> entry.componentNameLower);
        }
        if (!this.sortAscending) {
            comparator = comparator.reversed();
        }
        comparator = comparator.thenComparing(entry -> entry.itemNameLower)
                .thenComparingInt(entry -> entry.sourceIndex);
        entries.sort(comparator);

        this.table.clearChildren();
        int y = 0;
        for (AlbumEntry entry : entries) {
            AlbumRowWidget row = new AlbumRowWidget(0, y, TABLE_WIDTH - 2, entry);
            this.table.addChild(row);
            y += ROW_HEIGHT;
        }
        if (resetScroll) {
            this.table.scrollToTop();
        }
        this.hoveredRow = null;
    }

    private static String fit(Font font, String text, int width) {
        if (width <= 0 || text == null || text.isEmpty() || font.width(text) <= width) {
            return text == null ? "" : text;
        }
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        if (width <= ellipsisWidth) {
            return font.plainSubstrByWidth(text, width);
        }
        return font.plainSubstrByWidth(text, width - ellipsisWidth) + ellipsis;
    }

    /** 按列宽裁剪带样式的文本，保留组件原本的颜色/斜体等样式。 */
    private static void drawStyledText(GuiGraphics graphics, Font font, Component text,
                                       int x, int y, int width, int color) {
        if (text == null || text.getString().isEmpty() || width <= 0) {
            return;
        }
        if (font.width(text) <= width) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }

        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        FormattedText clipped = font.substrByWidth(text, width - ellipsisWidth);
        if (width <= ellipsisWidth) {
            drawFormattedText(graphics, font, clipped, x, y, width, color);
            return;
        }

        int clippedWidth = font.width(clipped);
        drawFormattedText(graphics, font, clipped,
                x, y, clippedWidth, color);
        graphics.drawString(font, ellipsis, x + clippedWidth, y, color, false);
    }

    /** GuiGraphics.drawString 没有 FormattedText 重载，借助 Font.split 转成 FormattedCharSequence。 */
    private static void drawFormattedText(GuiGraphics graphics, Font font, FormattedText text,
                                          int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = font.split(text, Math.max(1, width));
        if (!lines.isEmpty()) {
            graphics.drawString(font, lines.get(0), x, y, color, false);
        }
    }

    private static void drawComponentIcon(GuiGraphics graphics, @Nullable CircuitComponent component,
                                          int x, int y) {
        if (component == null) {
            return;
        }
        if (component.isCustom()) {
            String text = component.getCustomIconText();
            if (text != null && !text.isBlank()) {
                graphics.drawCenteredString(Minecraft.getInstance().font, Component.literal(text),
                        x + 8, y + 4, 0xFFFFFFFF);
                return;
            }
            Item item = component.getCustomIconItem() == null
                    ? null : BuiltInRegistries.ITEM.get(component.getCustomIconItem());
            if (item == null && component.getCustomScale() != null) {
                item = ChipManufacture.chipItemForScale(component.getCustomScale());
            }
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                graphics.renderFakeItem(new ItemStack(item), x, y);
            }
            return;
        }
        ResourceLocation icon = component.getIcon();
        if (icon != null) {
            graphics.blit(icon, x, y, 0, 0, 16, 16, 16, 16);
        }
    }

    private enum SortColumn {
        ITEM,
        COMPONENT,
        SCALE,
        UUID
    }

    private record AlbumEntry(ItemStack stack,
                              int sourceIndex,
                              Component itemNameComponent,
                              String itemName,
                              String itemNameLower,
                              Component componentNameComponent,
                              String componentName,
                              String componentNameLower,
                              @Nullable CircuitDiagram.Scale scale,
                              String scaleName,
                              String uuid,
                              String uuidLower,
                              boolean compiled,
                              @Nullable CircuitComponent component) {
        static AlbumEntry of(ItemStack stack, int sourceIndex) {
            var compiledId = CircuitItemData.compiledId(stack);
            boolean compiled = compiledId != null;
            StoredCircuit record = compiledId == null ? null : CircuitItemData.stored(stack);
            CircuitComponent component = null;
            ResourceLocation componentId = CircuitItemData.componentId(stack);
            if (componentId != null) {
                component = CircuitComponentRegistry.byId(componentId);
            }

            Component componentNameComponent = Component.empty();
            if (compiled) {
                if (record != null && record.custom() != null) {
                    componentNameComponent = Component.literal(record.custom().name())
                            .withStyle(ChatFormatting.ITALIC);
                } else if (component != null) {
                    componentNameComponent = component.getName().copy();
                    if (component.isCustom()) {
                        componentNameComponent = componentNameComponent.copy().withStyle(ChatFormatting.ITALIC);
                    }
                }
            }
            String componentName = componentNameComponent.getString();

            CircuitDiagram diagram = CircuitItemData.diagram(stack);
            CircuitDiagram.Scale scale = diagram == null ? null : diagram.scale;
            String scaleName = scale == null ? ""
                    : Component.translatable("circuit_diagram.scale." + scale.getSerializedName()).getString();
            String uuid = compiledId == null ? "" : compiledId.toString();
            Component itemNameComponent = stack.getHoverName().copy();
            String itemName = itemNameComponent.getString();
            return new AlbumEntry(stack, sourceIndex, itemNameComponent, itemName,
                    itemName.toLowerCase(Locale.ROOT),
                    componentNameComponent, componentName, componentName.toLowerCase(Locale.ROOT),
                    scale, scaleName, uuid, uuid.toLowerCase(Locale.ROOT), compiled, component);
        }

        boolean matches(String query) {
            return itemNameLower.contains(query)
                    || componentNameLower.contains(query)
                    || scaleName.toLowerCase(Locale.ROOT).contains(query)
                    || uuidLower.contains(query);
        }
    }

    private final class AlbumRowWidget extends AbstractWidget {
        private final AlbumEntry entry;

        private AlbumRowWidget(int x, int y, int width, AlbumEntry entry) {
            super(x, y, width, ROW_HEIGHT, Component.empty());
            this.entry = entry;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (isHovered()) {
                hoveredRow = this;
                graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1,
                        0x30FFFFFF);
            }

            int iconX = getX() + COL_ICON + 1;
            int iconY = getY() + 1;
            if (!entry.compiled || entry.component == null) {
                graphics.renderFakeItem(entry.stack(), iconX, iconY);
            } else {
                drawComponentIcon(graphics, entry.component(), iconX, iconY);
            }

            Font font = Minecraft.getInstance().font;
            int textY = getY() + (ROW_HEIGHT - font.lineHeight) / 2;
            drawStyledText(graphics, font, entry.itemNameComponent(),
                    getX() + COL_ITEM + 2, textY, COL_COMPONENT - COL_ITEM - 4, COLOR_TEXT);
            drawStyledText(graphics, font, entry.componentNameComponent(),
                    getX() + COL_COMPONENT + 2, textY, COL_SCALE - COL_COMPONENT - 4, COLOR_TEXT_MUTED);
            graphics.drawString(font, fit(font, entry.scaleName(),
                    COL_UUID - COL_SCALE - 4), getX() + COL_SCALE + 2, textY, COLOR_TEXT_MUTED, false);
            graphics.drawString(font, fit(font, entry.uuid(),
                    TABLE_WIDTH - COL_UUID - 6), getX() + COL_UUID + 2, textY, COLOR_TEXT_MUTED, false);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (Screen.hasShiftDown()) {
                sendAction(CircuitAlbumActionPayload.Action.TAKE_TO_INVENTORY, entry.sourceIndex());
                return;
            }
            ItemStack carried = menu.getCarried();
            if (carried.isEmpty()) {
                sendAction(CircuitAlbumActionPayload.Action.TAKE_TO_CURSOR, entry.sourceIndex());
            } else if (carried.is(ModItems.CIRCUIT_DIAGRAM) && contents.size() < capacity) {
                sendAction(CircuitAlbumActionPayload.Action.INSERT_CARRIED, -1);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }
}
