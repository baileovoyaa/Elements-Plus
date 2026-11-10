package com.elementsplus.client.gui;

import com.elementsplus.ElementsPlus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 实验章节使用的富文本显示控件。
 *
 * <p>支持：
 * <ul>
 *     <li>以 {@code #} 开头的粗体标题；</li>
 *     <li>{@code `行内代码`}，跟随普通文本换行并带背景色；</li>
 *     <li>行内 {@code **粗体**}、{@code *斜体*} 和颜色标签；</li>
 *     <li>Markdown 风格表格，表格可独立横向滚动；</li>
 *     <li>以三个反引号开始/结束的多行代码块，代码不换行、可独立横向滚动。</li>
 * </ul>
 *
 * <p>表格和代码块只消费横向滚动；普通纵向滚动返回 {@code false}，
 * 由外层的 {@link ScrollPanelWidget} 继续处理。
 */
public class RichTextSectionWidget extends AbstractWidget {

    private static final String BULLET = "•";
    private static final int PARAGRAPH_SPACING = 5;
    private static final int BLOCK_SPACING = 6;
    private static final int HEADING_TOP_MARGIN = 9;
    private static final int INLINE_CODE_PADDING = 3;
    private static final int TABLE_CELL_PADDING = 5;
    private static final int TABLE_ROW_PADDING = 3;
    private static final int CODE_PADDING = 4;
    private static final int SCROLLBAR_HEIGHT = 4;
    private static final int SCROLL_STEP = 24;
    private static final int TABLE_BG = 0xFF1E1E1E;
    private static final int TABLE_HEADER_BG = 0xFF343434;
    private static final int TABLE_ROW_BG = 0xFF252525;
    private static final int TABLE_ALT_ROW_BG = 0xFF2B2B2B;
    private static final int TABLE_BORDER = 0xFF777777;
    private static final int CODE_BG = 0xFF171717;
    private static final int CODE_BORDER = 0xFF505050;
    private static final int CODE_TEXT = 0xFFE2E2E2;
    private static final int INLINE_CODE_BG = 0x80505050;
    private static final int INLINE_CODE_TEXT = 0xFFF0F0F0;

    private final List<Block> blocks = new ArrayList<>();
    private final boolean markdownInline;
    @Nullable
    private Block draggingScrollbar;

    public RichTextSectionWidget(int x, int y, int width, List<Component> text) {
        this(x, y, width, text, false);
    }

    /**
     * @param markdownInline 是否解析 **粗体**、*斜体*、颜色标签等行内 Markdown；
     *                       旧章节的语言文本应保持为 false，避免把 A*B 之类的文本误判为斜体。
     */
    public RichTextSectionWidget(int x, int y, int width, List<Component> text, boolean markdownInline) {
        super(x, y, Math.max(1, width), 0, Component.empty());
        this.markdownInline = markdownInline;
        Font font = Minecraft.getInstance().font;
        buildBlocks(font, text);
        layoutBlocks();
    }

    private void buildBlocks(Font font, List<Component> text) {
        List<TableRow> tableRows = new ArrayList<>();
        List<String> codeLines = new ArrayList<>();
        boolean inCodeBlock = false;

        for (Component component : text) {
            String plain = component.getString();
            String trimmed = plain.stripLeading();

            if (inCodeBlock) {
                if (isCodeFence(trimmed)) {
                    addCodeBlock(font, codeLines);
                    codeLines.clear();
                    inCodeBlock = false;
                } else {
                    codeLines.add(plain);
                }
                continue;
            }

            if (isCodeFence(trimmed)) {
                flushTable(font, tableRows);
                inCodeBlock = true;
                continue;
            }

            if (isTableRow(trimmed)) {
                TableRow row = parseTableRow(plain, this.markdownInline);
                if (!row.separator()) {
                    tableRows.add(row);
                }
                continue;
            }

            flushTable(font, tableRows);
            addParagraph(font, component);
        }

        flushTable(font, tableRows);
        addCodeBlock(font, codeLines);
    }

    private void layoutBlocks() {
        int y = 0;
        for (Block block : blocks) {
            block.y = y;
            y += block.height();
            y += block instanceof ParagraphBlock ? PARAGRAPH_SPACING : BLOCK_SPACING;
        }
        this.setHeight(Math.max(1, y));
    }

    private void addParagraph(Font font, Component component) {
        List<TextRun> runs = parseRuns(component, false, this.markdownInline);
        String plain = plainText(runs);

        int headingPrefix = headingPrefixLength(plain);
        if (headingPrefix > 0) {
            runs = stripPrefix(runs, headingPrefix);
            runs = withBold(runs);
        }

        Component bullet = null;
        int indent = 0;
        int bulletPrefix = bulletPrefixLength(plainText(runs));
        if (bulletPrefix > 0) {
            Style bulletStyle = runs.isEmpty() ? Style.EMPTY : runs.get(0).style();
            bullet = Component.literal(BULLET).withStyle(bulletStyle);
            runs = stripPrefix(runs, bulletPrefix);
            indent = font.width(BULLET) + font.width(" ");
        }

        List<LayoutLine> lines = new ArrayList<>();
        for (List<TextRun> lineRuns : splitLines(runs)) {
            if (lineRuns.isEmpty()) {
                lines.add(new LayoutLine(List.of()));
            } else {
                lines.addAll(wrap(font, lineRuns, Math.max(1, this.getWidth() - indent)));
            }
        }
        if (lines.isEmpty()) {
            lines.add(new LayoutLine(List.of()));
        }
        this.blocks.add(new ParagraphBlock(lines, bullet, indent, headingPrefix > 0));
    }

    /** 段落内的强制换行；不会产生额外的段间距。 */
    private static List<List<TextRun>> splitLines(List<TextRun> runs) {
        List<List<TextRun>> lines = new ArrayList<>();
        List<TextRun> current = new ArrayList<>();
        for (TextRun run : runs) {
            int start = 0;
            for (int i = 0; i < run.text().length(); i++) {
                if (run.text().charAt(i) != '\n') {
                    continue;
                }
                if (i > start) {
                    appendRun(current, run.text().substring(start, i), run.style(), run.code());
                }
                lines.add(current);
                current = new ArrayList<>();
                start = i + 1;
            }
            if (start < run.text().length()) {
                appendRun(current, run.text().substring(start), run.style(), run.code());
            }
        }
        lines.add(current);
        return lines;
    }

    private void addCodeBlock(Font font, List<String> lines) {
        if (lines.isEmpty()) {
            return;
        }
        this.blocks.add(new CodeBlock(font, lines, this.getWidth()));
    }

    private void flushTable(Font font, List<TableRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        TableBlock table = new TableBlock(font, rows, this.getWidth());
        if (table.rowCount() > 0) {
            this.blocks.add(table);
        }
        rows.clear();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        for (Block block : blocks) {
            block.render(guiGraphics, font, this.getX(), this.getY() + block.y);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        double localX = mouseX - this.getX();
        double localY = mouseY - this.getY();
        for (Block block : blocks) {
            if (block instanceof ScrollableBlock scrollable && scrollable.startScrollbarDrag(localX, localY)) {
                this.draggingScrollbar = scrollable;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingScrollbar instanceof ScrollableBlock scrollable) {
            scrollable.dragScrollbar(mouseX - this.getX());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingScrollbar != null) {
            this.draggingScrollbar = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double localX = mouseX - this.getX();
        double localY = mouseY - this.getY();
        for (Block block : blocks) {
            if (!(block instanceof ScrollableBlock scrollable)
                    || !scrollable.isOver(localX, localY)
                    || !scrollable.canScroll()) {
                continue;
            }
            // 只有横向滚轮或 Shift+滚轮才由表格/代码块消费。
            // 普通纵向滚动返回 false，让外层章节面板继续滚动。
            if (scrollX == 0 && !Screen.hasShiftDown()) {
                return false;
            }
            double amount = scrollX != 0 ? scrollX : scrollY;
            scrollable.scrollBy(-amount * SCROLL_STEP);
            return true;
        }
        return false;
    }

    private static boolean isCodeFence(String trimmed) {
        return trimmed.startsWith("```");
    }

    private static boolean isTableRow(String trimmed) {
        return trimmed.startsWith("|") && trimmed.indexOf('|', 1) >= 0;
    }

    private static int headingPrefixLength(String plain) {
        int hashes = 0;
        while (hashes < plain.length() && hashes < 6 && plain.charAt(hashes) == '#') {
            hashes++;
        }
        if (hashes == 0 || hashes >= plain.length() || !Character.isWhitespace(plain.charAt(hashes))) {
            return 0;
        }
        while (hashes < plain.length() && Character.isWhitespace(plain.charAt(hashes))) {
            hashes++;
        }
        return hashes;
    }

    /** 兼容旧章节的 • 列表和 Markdown 的 - / * 列表。 */
    private static int bulletPrefixLength(String plain) {
        if (plain.isEmpty()) {
            return 0;
        }
        char marker = plain.charAt(0);
        boolean bullet = marker == BULLET.charAt(0)
                || ((marker == '-' || marker == '*')
                && plain.length() > 1
                && Character.isSpaceChar(plain.charAt(1)));
        if (!bullet) {
            return 0;
        }
        int prefix = 1;
        while (prefix < plain.length() && Character.isSpaceChar(plain.charAt(prefix))) {
            prefix++;
        }
        return prefix;
    }

    private static TableRow parseTableRow(String plain, boolean markdownInline) {
        String body = plain.trim();
        if (body.startsWith("|")) {
            body = body.substring(1);
        }
        if (body.endsWith("|")) {
            body = body.substring(0, body.length() - 1);
        }

        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean escaped = false;
        boolean inCode = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (escaped) {
                cell.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '`') {
                inCode = !inCode;
                cell.append(c);
            } else if (c == '|' && !inCode) {
                cells.add(cell.toString().trim());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        cells.add(cell.toString().trim());

        boolean separator = !cells.isEmpty();
        for (String value : cells) {
            if (!value.matches(":?-{3,}:?")) {
                separator = false;
                break;
            }
        }

        List<List<TextRun>> runs = new ArrayList<>();
        for (String value : cells) {
            runs.add(parseRuns(Component.literal(value), false, markdownInline));
        }
        return new TableRow(runs, separator);
    }

    private static final Pattern COLOR_TAG = Pattern.compile(
            "^<color=([#A-Za-z0-9_]+)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern FONT_COLOR_TAG = Pattern.compile(
            "^<font\\s+color=([#A-Za-z0-9_]+)>", Pattern.CASE_INSENSITIVE);

    /**
     * 展开 {@link Component} 的样式片段，同时识别行内 Markdown 标记：
     * 反引号代码、{@code **粗体**}、{@code *斜体*} 和颜色标签
     * {@code <color=red>红色</color>} / {@code <font color=red>红色</font>}。
     */
    private static List<TextRun> parseRuns(Component component, boolean bold, boolean markdownInline) {
        List<TextRun> runs = new ArrayList<>();
        if (!markdownInline) {
            for (Component part : component.toFlatList()) {
                appendRun(runs, part.getString(), part.getStyle(), false);
            }
            return runs;
        }

        InlineState state = new InlineState();
        state.bold = bold;
        Style lastBaseStyle = Style.EMPTY;

        for (Component part : component.toFlatList()) {
            String text = part.getString();
            Style baseStyle = part.getStyle();
            lastBaseStyle = baseStyle;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                if (state.code) {
                    if (c == '`') {
                        state.code = false;
                        appendInlineCode(runs, state.codeBuffer.toString(), styled(baseStyle, state));
                        state.codeBuffer.setLength(0);
                    } else {
                        state.codeBuffer.append(c);
                    }
                    continue;
                }

                if (c == '\\' && i + 1 < text.length()) {
                    appendRun(runs, String.valueOf(text.charAt(i + 1)), styled(baseStyle, state), false);
                    i++;
                    continue;
                }

                if (c == '`') {
                    state.code = true;
                    state.codeBuffer.setLength(0);
                    continue;
                }

                if (c == '*') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '*') {
                        state.bold = !state.bold;
                        i++;
                    } else {
                        state.italic = !state.italic;
                    }
                    continue;
                }

                if (c == '<') {
                    String rest = text.substring(i);
                    Matcher matched = COLOR_TAG.matcher(rest);
                    if (!matched.find()) {
                        matched = FONT_COLOR_TAG.matcher(rest);
                        if (!matched.find()) {
                            matched = null;
                        }
                    }
                    if (matched != null) {
                        TextColor color = parseColor(matched.group(1));
                        if (color != null) {
                            state.colors.push(color);
                            i += matched.end() - 1;
                            continue;
                        }
                    }
                    boolean closeColor = rest.regionMatches(true, 0, "</color>", 0, "</color>".length());
                    boolean closeFont = rest.regionMatches(true, 0, "</font>", 0, "</font>".length());
                    if (closeColor || closeFont) {
                        if (!state.colors.isEmpty()) {
                            state.colors.pop();
                        }
                        i += (closeColor ? "</color>" : "</font>").length() - 1;
                        continue;
                    }
                }

                appendRun(runs, String.valueOf(c), styled(baseStyle, state), false);
            }
        }
        if (state.code) {
            appendInlineCode(runs, state.codeBuffer.toString(), styled(lastBaseStyle, state));
        }
        return runs;
    }

    private static final String COMPONENT_PREFIX = "component::";

    /**
     * 行内代码默认按代码样式绘制；如果内容以 component:: 开头，
     * 则把后面的 JSON 解析成文本组件后按普通文本嵌入。
     */
    private static void appendInlineCode(List<TextRun> runs, String text, Style style) {
        if (text.isEmpty()) {
            return;
        }
        if (text.startsWith(COMPONENT_PREFIX)) {
            Component component = parseInlineComponent(text.substring(COMPONENT_PREFIX.length()));
            if (component != null) {
                for (Component part : component.toFlatList()) {
                    appendRun(runs, part.getString(), part.getStyle(), false);
                }
                return;
            }
        }
        appendRun(runs, text, style, true);
    }

    @Nullable
    private static Component parseInlineComponent(String json) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() != null) {
                return Component.Serializer.fromJson(json, minecraft.getConnection().registryAccess());
            }
            if (minecraft.level != null) {
                return Component.Serializer.fromJson(json, minecraft.level.registryAccess());
            }
        } catch (Exception e) {
            ElementsPlus.LOGGER.warn("Invalid inline component JSON in Markdown: {}", json);
        }
        return null;
    }

    private static Style styled(Style baseStyle, InlineState state) {
        Style style = baseStyle;
        if (state.bold) {
            style = style.applyFormat(ChatFormatting.BOLD);
        }
        if (state.italic) {
            style = style.applyFormat(ChatFormatting.ITALIC);
        }
        TextColor color = state.colors.peek();
        if (color != null) {
            style = style.withColor(color);
        }
        return style;
    }

    @Nullable
    private static TextColor parseColor(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (value.startsWith("#")) {
            if (value.length() != 7) {
                return null;
            }
            try {
                return TextColor.fromRgb(Integer.parseInt(value.substring(1), 16));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        int rgb = switch (value) {
            case "black" -> 0x000000;
            case "dark_blue" -> 0x0000AA;
            case "dark_green" -> 0x00AA00;
            case "dark_aqua" -> 0x00AAAA;
            case "dark_red" -> 0xAA0000;
            case "dark_purple" -> 0xAA00AA;
            case "gold" -> 0xFFAA00;
            case "gray", "grey" -> 0xAAAAAA;
            case "dark_gray", "dark_grey" -> 0x555555;
            case "blue" -> 0x5555FF;
            case "green" -> 0x55FF55;
            case "aqua" -> 0x55FFFF;
            case "red" -> 0xFF5555;
            case "light_purple" -> 0xFF55FF;
            case "yellow" -> 0xFFFF55;
            case "white" -> 0xFFFFFF;
            default -> -1;
        };
        return rgb < 0 ? null : TextColor.fromRgb(rgb);
    }

    private static final class InlineState {
        private boolean code;
        private boolean bold;
        private boolean italic;
        private final StringBuilder codeBuffer = new StringBuilder();
        private final Deque<TextColor> colors = new ArrayDeque<>();
    }

    private static List<TextRun> withBold(List<TextRun> runs) {
        List<TextRun> result = new ArrayList<>(runs.size());
        for (TextRun run : runs) {
            result.add(new TextRun(run.text(), run.style().applyFormat(ChatFormatting.BOLD), run.code()));
        }
        return result;
    }

    private static List<TextRun> stripPrefix(List<TextRun> runs, int count) {
        List<TextRun> result = new ArrayList<>();
        int remaining = count;
        for (TextRun run : runs) {
            String value = run.text();
            if (remaining >= value.length()) {
                remaining -= value.length();
                continue;
            }
            String rest = value.substring(remaining);
            remaining = 0;
            if (!rest.isEmpty()) {
                appendRun(result, rest, run.style(), run.code());
            }
        }
        return result;
    }

    private static void appendRun(List<TextRun> runs, String text, Style style, boolean code) {
        if (text.isEmpty()) {
            return;
        }
        if (!runs.isEmpty()) {
            TextRun last = runs.get(runs.size() - 1);
            if (last.code() == code && last.style().equals(style)) {
                runs.set(runs.size() - 1, new TextRun(last.text() + text, style, code));
                return;
            }
        }
        runs.add(new TextRun(text, style, code));
    }

    private static String plainText(List<TextRun> runs) {
        StringBuilder builder = new StringBuilder();
        for (TextRun run : runs) {
            builder.append(run.text());
        }
        return builder.toString();
    }

    private static List<LayoutLine> wrap(Font font, List<TextRun> runs, int maxWidth) {
        List<Token> tokens = tokenize(runs);
        List<LayoutLine> lines = new ArrayList<>();
        List<Span> current = new ArrayList<>();
        int x = 0;

        for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
            Token token = tokens.get(tokenIndex);
            if (token.text().isEmpty()) {
                continue;
            }
            boolean code = token.code();
            int leftPad = code && (tokenIndex == 0 || !tokens.get(tokenIndex - 1).code())
                    ? INLINE_CODE_PADDING : 0;
            int rightPad = code && (tokenIndex == tokens.size() - 1 || !tokens.get(tokenIndex + 1).code())
                    ? INLINE_CODE_PADDING : 0;
            int width = textWidth(font, token.text(), token.style()) + leftPad + rightPad;

            if (x > 0 && x + width > maxWidth) {
                lines.add(new LayoutLine(List.copyOf(current)));
                current.clear();
                x = 0;
                if (token.whitespace()) {
                    continue;
                }
            }

            if (width > maxWidth) {
                for (int i = 0; i < token.text().length(); i++) {
                    String part = token.text().substring(i, i + 1);
                    int partLeftPad = i == 0 ? leftPad : 0;
                    int partRightPad = i == token.text().length() - 1 ? rightPad : 0;
                    int partWidth = textWidth(font, part, token.style()) + partLeftPad + partRightPad;
                    if (x > 0 && x + partWidth > maxWidth) {
                        lines.add(new LayoutLine(List.copyOf(current)));
                        current.clear();
                        x = 0;
                    }
                    current.add(new Span(part, token.style(), token.code(), x, partWidth, partLeftPad));
                    x += partWidth;
                }
            } else {
                if (x > 0 || !token.whitespace()) {
                    current.add(new Span(token.text(), token.style(), token.code(), x, width, leftPad));
                    x += width;
                }
            }
        }

        if (!current.isEmpty() || lines.isEmpty()) {
            lines.add(new LayoutLine(List.copyOf(current)));
        }
        return lines;
    }

    private static List<Token> tokenize(List<TextRun> runs) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        Style wordStyle = null;
        boolean wordCode = false;

        for (TextRun run : runs) {
            String text = run.text();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                boolean whitespace = Character.isWhitespace(c);
                boolean breakable = whitespace || isIdeographic(c);
                if (breakable) {
                    if (word.length() > 0) {
                        tokens.add(new Token(word.toString(), wordStyle, wordCode, false));
                        word.setLength(0);
                        wordStyle = null;
                    }
                    if (whitespace) {
                        tokens.add(new Token(String.valueOf(c), run.style(), run.code(), true));
                    } else {
                        tokens.add(new Token(String.valueOf(c), run.style(), run.code(), false));
                    }
                } else {
                    if (word.length() > 0 && (wordStyle != run.style() || wordCode != run.code())) {
                        tokens.add(new Token(word.toString(), wordStyle, wordCode, false));
                        word.setLength(0);
                    }
                    wordStyle = run.style();
                    wordCode = run.code();
                    word.append(c);
                }
            }
        }
        if (word.length() > 0) {
            tokens.add(new Token(word.toString(), wordStyle, wordCode, false));
        }
        return tokens;
    }

    /**
     * 使用带样式的组件测量宽度，确保粗体的额外 1px 也被计入。
     */
    private static int textWidth(Font font, String text, Style style) {
        return font.width(Component.literal(text).withStyle(style));
    }

    private static boolean isIdeographic(char c) {
        return (c >= 0x2E80 && c <= 0x9FFF)
                || (c >= 0xF900 && c <= 0xFAFF)
                || (c >= 0xFF00 && c <= 0xFFEF);
    }

    private static void drawSpan(GuiGraphics graphics, Font font, int x, int y, Span span, boolean bold) {
        if (span.text().isEmpty()) {
            return;
        }
        Style style = span.style();
        if (bold) {
            style = style.applyFormat(ChatFormatting.BOLD);
        }
        int textX = x + span.leftPadding();
        if (span.code()) {
            style = style.withColor(TextColor.fromRgb(INLINE_CODE_TEXT));
            graphics.fill(x, y - 1, x + span.width(), y + font.lineHeight - 1, INLINE_CODE_BG);
        }
        graphics.drawString(font, Component.literal(span.text()).withStyle(style), textX, y, 0xFFFFFFFF, false);
    }

    private static void drawHorizontalBar(GuiGraphics graphics, int x, int y, int width,
                                          int thumbX, int thumbWidth) {
        graphics.fill(x, y, x + width, y + SCROLLBAR_HEIGHT, 0x50000000);
        graphics.fill(thumbX, y, thumbX + thumbWidth, y + SCROLLBAR_HEIGHT, 0xB0E0E0E0);
    }

    private abstract static class Block {
        int y;

        abstract int height();

        abstract void render(GuiGraphics graphics, Font font, int x, int y);
    }

    /**
     * 带独立横向滚动条的表格/代码块公共逻辑。
     */
    private abstract static class ScrollableBlock extends Block {
        final int viewportWidth;
        final int contentWidth;
        double scrollX;

        ScrollableBlock(int viewportWidth, int contentWidth) {
            this.viewportWidth = viewportWidth;
            this.contentWidth = contentWidth;
        }

        boolean canScroll() {
            return this.contentWidth > this.viewportWidth;
        }

        int maxScroll() {
            return Math.max(0, this.contentWidth - this.viewportWidth);
        }

        void scrollBy(double amount) {
            this.scrollX = Mth.clamp(this.scrollX + amount, 0, this.maxScroll());
        }

        boolean isOver(double mouseX, double mouseY) {
            return mouseX >= 0 && mouseX < this.viewportWidth
                    && mouseY >= this.y && mouseY < this.y + this.height();
        }

        private int scrollbarY() {
            return this.y + this.height() - SCROLLBAR_HEIGHT;
        }

        private int thumbWidth() {
            if (this.contentWidth <= 0) {
                return this.viewportWidth;
            }
            return Math.max(8, (int) ((long) this.viewportWidth * this.viewportWidth / this.contentWidth));
        }

        private int thumbX() {
            int max = this.maxScroll();
            if (max <= 0) {
                return 0;
            }
            return (int) ((this.viewportWidth - this.thumbWidth()) * (this.scrollX / max));
        }

        boolean startScrollbarDrag(double mouseX, double mouseY) {
            if (!this.canScroll() || mouseY < this.scrollbarY() || mouseY >= this.scrollbarY() + SCROLLBAR_HEIGHT) {
                return false;
            }
            if (mouseX < 0 || mouseX >= this.viewportWidth) {
                return false;
            }
            int thumbX = this.thumbX();
            int thumbWidth = this.thumbWidth();
            if (mouseX >= thumbX && mouseX < thumbX + thumbWidth) {
                this.dragGrabOffset = mouseX - thumbX;
            } else {
                this.dragGrabOffset = thumbWidth / 2.0;
                this.dragTo(mouseX);
            }
            return true;
        }

        private double dragGrabOffset;

        void dragScrollbar(double mouseX) {
            this.dragTo(mouseX);
        }

        private void dragTo(double mouseX) {
            int max = this.maxScroll();
            int travel = this.viewportWidth - this.thumbWidth();
            if (max <= 0 || travel <= 0) {
                return;
            }
            double position = Mth.clamp(mouseX - this.dragGrabOffset, 0, travel);
            this.scrollX = max * (position / travel);
        }

        void renderScrollbar(GuiGraphics graphics, int x, int y) {
            if (!this.canScroll()) {
                return;
            }
            drawHorizontalBar(graphics, x, y + this.height() - SCROLLBAR_HEIGHT,
                    this.viewportWidth, x + this.thumbX(), this.thumbWidth());
        }
    }

    private static final class ParagraphBlock extends Block {
        private final List<LayoutLine> lines;
        @Nullable
        private final Component bullet;
        private final int indent;
        private final boolean heading;

        ParagraphBlock(List<LayoutLine> lines, @Nullable Component bullet, int indent, boolean heading) {
            this.lines = lines;
            this.bullet = bullet;
            this.indent = indent;
            this.heading = heading;
        }

        @Override
        int height() {
            return this.lines.size() * Minecraft.getInstance().font.lineHeight
                    + (this.heading ? HEADING_TOP_MARGIN : 0);
        }

        @Override
        void render(GuiGraphics graphics, Font font, int x, int y) {
            int lineY = y + (this.heading ? HEADING_TOP_MARGIN : 0);
            for (int i = 0; i < this.lines.size(); i++) {
                LayoutLine line = this.lines.get(i);
                if (i == 0 && this.bullet != null) {
                    graphics.drawString(font, this.bullet, x, lineY, 0xFFFFFFFF, false);
                }
                for (Span span : line.spans()) {
                    drawSpan(graphics, font, x + this.indent + span.x(), lineY, span, false);
                }
                lineY += font.lineHeight;
            }
        }
    }

    private static final class TableBlock extends ScrollableBlock {
        private final List<List<List<Span>>> rows;
        private final List<Integer> columnWidths;
        private final int rowHeight;

        TableBlock(Font font, List<TableRow> tableRows, int viewportWidth) {
            super(viewportWidth, measureTableWidth(font, tableRows, viewportWidth));
            this.rowHeight = font.lineHeight + TABLE_ROW_PADDING * 2;
            List<Integer> widths = new ArrayList<>();
            this.rows = new ArrayList<>();

            for (int rowIndex = 0; rowIndex < tableRows.size(); rowIndex++) {
                TableRow row = tableRows.get(rowIndex);
                List<List<Span>> renderedCells = new ArrayList<>();
                for (int cellIndex = 0; cellIndex < row.cells().size(); cellIndex++) {
                    List<TextRun> runs = row.cells().get(cellIndex);
                    boolean header = rowIndex == 0;
                    List<Span> spans = new ArrayList<>();
                    int cellX = TABLE_CELL_PADDING;
                    for (TextRun run : runs) {
                        Style style = header ? run.style().applyFormat(ChatFormatting.BOLD) : run.style();
                        int width = textWidth(font, run.text(), style)
                                + (run.code() ? INLINE_CODE_PADDING * 2 : 0);
                        int leftPadding = run.code() ? INLINE_CODE_PADDING : 0;
                        spans.add(new Span(run.text(), style, run.code(), cellX, width, leftPadding));
                        cellX += width;
                    }
                    renderedCells.add(spans);
                    int width = cellX + TABLE_CELL_PADDING;
                    if (cellIndex >= widths.size()) {
                        widths.add(width);
                    } else {
                        widths.set(cellIndex, Math.max(widths.get(cellIndex), width));
                    }
                }
                this.rows.add(renderedCells);
            }
            this.columnWidths = widths;
        }

        private static int measureTableWidth(Font font, List<TableRow> rows, int viewportWidth) {
            List<Integer> widths = new ArrayList<>();
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                TableRow row = rows.get(rowIndex);
                for (int i = 0; i < row.cells().size(); i++) {
                    int width = TABLE_CELL_PADDING * 2;
                    for (TextRun run : row.cells().get(i)) {
                        Style style = rowIndex == 0
                                ? run.style().applyFormat(ChatFormatting.BOLD)
                                : run.style();
                        width += textWidth(font, run.text(), style)
                                + (run.code() ? INLINE_CODE_PADDING * 2 : 0);
                    }
                    if (i >= widths.size()) {
                        widths.add(width);
                    } else {
                        widths.set(i, Math.max(widths.get(i), width));
                    }
                }
            }
            int total = 0;
            for (int width : widths) {
                total += width;
            }
            return Math.max(viewportWidth, total);
        }

        int rowCount() {
            return this.rows.size();
        }

        private int tableHeight() {
            return this.rows.size() * this.rowHeight + 2;
        }

        @Override
        int height() {
            return this.tableHeight() + (this.canScroll() ? SCROLLBAR_HEIGHT : 0);
        }

        @Override
        void render(GuiGraphics graphics, Font font, int x, int y) {
            int contentTop = y + 1;
            int contentHeight = this.rows.size() * this.rowHeight;
            int contentWidth = Math.max(this.viewportWidth, this.contentWidth);
            int drawX = x - (int) this.scrollX;

            graphics.fill(x, y, x + this.viewportWidth, y + this.tableHeight(), TABLE_BG);
            graphics.enableScissor(x, contentTop, x + this.viewportWidth, contentTop + contentHeight);
            for (int rowIndex = 0; rowIndex < this.rows.size(); rowIndex++) {
                int rowY = contentTop + rowIndex * this.rowHeight;
                int background = rowIndex == 0
                        ? TABLE_HEADER_BG
                        : (rowIndex % 2 == 0 ? TABLE_ALT_ROW_BG : TABLE_ROW_BG);
                graphics.fill(drawX, rowY, drawX + contentWidth, rowY + this.rowHeight, background);

                int cellX = drawX;
                List<List<Span>> row = this.rows.get(rowIndex);
                for (int cellIndex = 0; cellIndex < row.size(); cellIndex++) {
                    for (Span span : row.get(cellIndex)) {
                        drawSpan(graphics, font, cellX + span.x(),
                                rowY + TABLE_ROW_PADDING, span, rowIndex == 0);
                    }
                    cellX += this.columnWidths.get(cellIndex);
                    graphics.fill(cellX - 1, rowY, cellX, rowY + this.rowHeight, TABLE_BORDER);
                }
                graphics.fill(drawX, rowY + this.rowHeight - 1,
                        drawX + contentWidth, rowY + this.rowHeight, TABLE_BORDER);
            }
            graphics.disableScissor();

            graphics.fill(x, y, x + this.viewportWidth, y + 1, TABLE_BORDER);
            graphics.fill(x, contentTop + contentHeight,
                    x + this.viewportWidth, contentTop + contentHeight + 1, TABLE_BORDER);
            graphics.fill(x, y, x + 1, contentTop + contentHeight, TABLE_BORDER);
            graphics.fill(x + this.viewportWidth - 1, y,
                    x + this.viewportWidth, contentTop + contentHeight, TABLE_BORDER);
            this.renderScrollbar(graphics, x, y);
        }
    }

    private static final class CodeBlock extends ScrollableBlock {
        private final List<String> lines;
        private final int contentHeight;

        CodeBlock(Font font, List<String> lines, int viewportWidth) {
            super(viewportWidth, measureCodeWidth(font, lines, viewportWidth));
            this.lines = List.copyOf(lines);
            this.contentHeight = lines.size() * font.lineHeight + CODE_PADDING * 2;
        }

        private static int measureCodeWidth(Font font, List<String> lines, int viewportWidth) {
            int max = 0;
            for (String line : lines) {
                max = Math.max(max, font.width(line));
            }
            return Math.max(viewportWidth, max + CODE_PADDING * 2);
        }

        @Override
        int height() {
            return this.contentHeight + (this.canScroll() ? SCROLLBAR_HEIGHT : 0);
        }

        @Override
        void render(GuiGraphics graphics, Font font, int x, int y) {
            int drawX = x + CODE_PADDING - (int) this.scrollX;
            graphics.fill(x, y, x + this.viewportWidth, y + this.contentHeight, CODE_BG);
            graphics.enableScissor(x, y, x + this.viewportWidth, y + this.contentHeight);
            int textY = y + CODE_PADDING;
            for (String line : this.lines) {
                graphics.drawString(font, Component.literal(line), drawX, textY, CODE_TEXT, false);
                textY += font.lineHeight;
            }
            graphics.disableScissor();
            graphics.fill(x, y, x + this.viewportWidth, y + 1, CODE_BORDER);
            graphics.fill(x, y + this.contentHeight - 1, x + this.viewportWidth, y + this.contentHeight, CODE_BORDER);
            graphics.fill(x, y, x + 1, y + this.contentHeight, CODE_BORDER);
            graphics.fill(x + this.viewportWidth - 1, y, x + this.viewportWidth, y + this.contentHeight, CODE_BORDER);
            this.renderScrollbar(graphics, x, y);
        }
    }

    private record TextRun(String text, Style style, boolean code) {
    }

    private record Span(String text, Style style, boolean code, int x, int width, int leftPadding) {
    }

    private record LayoutLine(List<Span> spans) {
    }

    private record Token(String text, Style style, boolean code, boolean whitespace) {
    }

    private record TableRow(List<List<TextRun>> cells, boolean separator) {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
