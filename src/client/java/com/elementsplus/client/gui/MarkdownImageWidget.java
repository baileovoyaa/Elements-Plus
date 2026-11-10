package com.elementsplus.client.gui;

import com.elementsplus.ElementsPlus;
import com.elementsplus.client.ClientResourceHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * Markdown 章节中的块级图片。
 *
 * <p>找不到图片时显示 16x16 的错误图标和图片描述，并把解析后的资源路径写入日志。</p>
 */
public class MarkdownImageWidget extends AbstractWidget {

    private static final ResourceLocation MISSING_ICON =
            ElementsPlus.id("textures/gui/missing_image.png");
    private static final int MISSING_ICON_SIZE = 16;
    private static final int MISSING_TEXT_OFFSET = 20;

    private final String description;
    private final ResourceLocation texture;
    private final boolean missing;
    private final int displayWidth;
    private final int displayHeight;

    public MarkdownImageWidget(int x, int y, int width,
                               @Nullable ResourceLocation image,
                               String description,
                               String rawPath) {
        super(x, y, Math.max(1, width), 0, Component.empty());
        this.description = description == null ? "" : description;

        ResourceLocation localized = image == null ? null : ClientResourceHelper.getLocalizedTexture(image);
        int[] size = localized == null ? null : ClientResourceHelper.getRawTextureSize(localized);
        if (size == null || size[0] <= 0 || size[1] <= 0) {
            this.missing = true;
            this.texture = MISSING_ICON;
            this.displayWidth = MISSING_ICON_SIZE;
            this.displayHeight = MISSING_ICON_SIZE;
            this.setHeight(MISSING_ICON_SIZE);
            ElementsPlus.LOGGER.warn("Missing Markdown chapter image: {} (declared as {})",
                    image == null ? "<invalid path>" : image, rawPath);
        } else {
            this.missing = false;
            this.texture = localized;
            this.displayWidth = Math.min(this.getWidth(), size[0]);
            this.displayHeight = Math.max(1, (int) Math.ceil(
                    size[1] * (double) this.displayWidth / size[0]));
            this.setHeight(this.displayHeight);
        }

        if (!this.description.isBlank()) {
            this.setTooltip(Tooltip.create(Component.literal(this.description)));
            this.setTooltipDelay(Duration.ofMillis(500));
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.missing) {
            graphics.blit(MISSING_ICON, this.getX(), this.getY(), 0, 0,
                    MISSING_ICON_SIZE, MISSING_ICON_SIZE, MISSING_ICON_SIZE, MISSING_ICON_SIZE);
            Font font = Minecraft.getInstance().font;
            GuiUtil.drawTruncatedText(graphics, font, Component.literal(this.description),
                    this.getX() + MISSING_TEXT_OFFSET,
                    this.getY() + (MISSING_ICON_SIZE - font.lineHeight) / 2,
                    Math.max(1, this.getWidth() - MISSING_TEXT_OFFSET),
                    0xFFFFAA00, true);
            return;
        }
        graphics.blit(this.texture, this.getX(), this.getY(), 0, 0,
                this.displayWidth, this.displayHeight,
                this.displayWidth, this.displayHeight);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
