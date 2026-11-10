package com.elementsplus.client.experiment;

import com.elementsplus.ElementsPlus;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 读取资源包中的章节 Markdown，并拆出普通正文、实验块和特殊文本块。
 *
 * <p>默认读取 {@code assets/<namespace>/<path>}；如果当前语言是
 * {@code zh_cn}，会优先尝试 {@code <path 去掉扩展名>.zh.md}，然后再尝试
 * {@code <path 去掉扩展名>.zh_cn.md}，最后回退到默认文件。</p>
 */
public final class ExperimentMarkdownParser {

    private static final Pattern IMAGE_LINE = Pattern.compile(
            "^!\\[([^\\]]*)]\\(([^)]+)\\)\\s*$");

    private ExperimentMarkdownParser() {
    }

    public static List<Part> load(ResourceLocation markdown) {
        String source = readLocalized(markdown);
        if (source == null) {
            return List.of(new TextPart(List.of(
                    Component.literal("Missing chapter markdown: " + markdown))));
        }
        return parse(source, markdown);
    }

    /**
     * 逐行拆分 Markdown。普通代码块原样保留给富文本控件；
     * epp-experiments/epp-text 块变成独立的 Part。
     */
    public static List<Part> parse(String source) {
        return parse(source, null);
    }

    public static List<Part> parse(String source, @Nullable ResourceLocation markdown) {
        List<String> lines = source.lines().toList();
        List<Part> parts = new ArrayList<>();
        List<String> textLines = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.strip();

            if (isHorizontalRule(trimmed)) {
                flushText(parts, textLines);
                parts.add(new HorizontalRulePart());
                continue;
            }

            Matcher imageMatcher = IMAGE_LINE.matcher(trimmed);
            if (imageMatcher.matches()) {
                flushText(parts, textLines);
                parts.add(new ImagePart(imageMatcher.group(1),
                        resolveImagePath(markdown, imageMatcher.group(2)),
                        imageMatcher.group(2)));
                continue;
            }

            if (!isFence(trimmed)) {
                textLines.add(line);
                continue;
            }

            String language = trimmed.substring(3).strip();
            int end = findFenceEnd(lines, i + 1);
            if (!language.equals("epp-experiments") && !language.equals("epp-text")) {
                textLines.add(line);
                for (int j = i + 1; j <= end && j < lines.size(); j++) {
                    textLines.add(lines.get(j));
                }
                i = end;
                continue;
            }

            flushText(parts, textLines);
            if (language.equals("epp-experiments")) {
                List<String> ids = new ArrayList<>();
                for (int j = i + 1; j < end && j < lines.size(); j++) {
                    String id = lines.get(j).strip();
                    if (!id.isEmpty()) {
                        ids.add(id);
                    }
                }
                if (!ids.isEmpty()) {
                    parts.add(new ExperimentPart(ids));
                }
            } else {
                List<ResourceLocation> providers = new ArrayList<>();
                for (int j = i + 1; j < end && j < lines.size(); j++) {
                    String raw = lines.get(j).strip();
                    if (raw.isEmpty()) {
                        continue;
                    }
                    ResourceLocation provider = ResourceLocation.tryParse(raw);
                    if (provider == null) {
                        ElementsPlus.LOGGER.warn("Invalid epp-text provider in chapter Markdown: {}", raw);
                    } else {
                        providers.add(provider);
                    }
                }
                if (!providers.isEmpty()) {
                    parts.add(new SpecialTextPart(providers));
                }
            }
            i = end;
        }

        flushText(parts, textLines);
        return parts;
    }

    private static void flushText(List<Part> parts, List<String> textLines) {
        if (textLines.isEmpty()) {
            return;
        }
        List<Component> text = paragraphize(textLines);
        if (!text.isEmpty()) {
            parts.add(new TextPart(text));
        }
        textLines.clear();
    }

    /**
     * 把 Markdown 源行整理成段落：
     * <ul>
     *     <li>空行表示段落结束，连续空行不会产生额外段落；</li>
     *     <li>同一段落的普通文本行合并，行尾两个空格表示强制换行（{@code \\n}）；</li>
     *     <li>标题、表格、列表和代码块保持独立，交给富文本控件渲染。</li>
     * </ul>
     */
    private static List<Component> paragraphize(List<String> lines) {
        List<Component> result = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();
        boolean[] hardBreak = {false};
        boolean inCodeBlock = false;

        for (String line : lines) {
            String trimmed = line.strip();

            if (inCodeBlock) {
                result.add(Component.literal(line));
                if (isFence(trimmed)) {
                    inCodeBlock = false;
                }
                continue;
            }

            if (isFence(trimmed)) {
                flushParagraph(result, paragraph, hardBreak);
                result.add(Component.literal(line));
                inCodeBlock = true;
                continue;
            }

            if (trimmed.isEmpty()) {
                flushParagraph(result, paragraph, hardBreak);
                continue;
            }

            if (isHeading(trimmed) || isTableRow(trimmed) || isBullet(trimmed)) {
                flushParagraph(result, paragraph, hardBreak);
                result.add(Component.literal(line));
                continue;
            }

            if (paragraph.length() > 0) {
                paragraph.append(hardBreak[0] ? '\n' : ' ');
            }
            boolean lineBreak = line.endsWith("  ");
            paragraph.append(lineBreak ? line.substring(0, line.length() - 2) : line);
            hardBreak[0] = lineBreak;
        }

        flushParagraph(result, paragraph, hardBreak);
        return result;
    }

    private static void flushParagraph(List<Component> result, StringBuilder paragraph, boolean[] hardBreak) {
        if (paragraph.length() > 0) {
            result.add(Component.literal(paragraph.toString()));
            paragraph.setLength(0);
        }
        hardBreak[0] = false;
    }

    private static boolean isHeading(String trimmed) {
        int hashes = 0;
        while (hashes < trimmed.length() && hashes < 6 && trimmed.charAt(hashes) == '#') {
            hashes++;
        }
        return hashes > 0 && hashes < trimmed.length() && Character.isWhitespace(trimmed.charAt(hashes));
    }

    private static boolean isTableRow(String trimmed) {
        return trimmed.startsWith("|") && trimmed.indexOf('|', 1) >= 0;
    }

    private static boolean isBullet(String trimmed) {
        if (trimmed.startsWith("• ")) {
            return true;
        }
        return (trimmed.startsWith("- ") || trimmed.startsWith("* "))
                && trimmed.length() > 2;
    }

    private static int findFenceEnd(List<String> lines, int start) {
        for (int i = start; i < lines.size(); i++) {
            if (isFence(lines.get(i).stripLeading())) {
                return i;
            }
        }
        return lines.size();
    }

    private static boolean isFence(String line) {
        return line.startsWith("```");
    }

    private static boolean isHorizontalRule(String trimmed) {
        if (trimmed.length() < 3) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (trimmed.charAt(i) != '-') {
                return false;
            }
        }
        return true;
    }

    /**
     * 将图片路径解析为资源位置。相对路径以 Markdown 文件所在目录为基准，
     * 并且不允许越过 {@code assets/<namespace>/} 根目录。
     */
    @Nullable
    private static ResourceLocation resolveImagePath(@Nullable ResourceLocation markdown, String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }
        String value = rawPath.trim().replace('\\', '/');
        String namespace;
        String path;

        if (value.indexOf(':') > 0) {
            ResourceLocation absolute = ResourceLocation.tryParse(value);
            if (absolute == null) {
                return null;
            }
            namespace = absolute.getNamespace();
            path = absolute.getPath();
        } else {
            if (markdown == null) {
                return null;
            }
            namespace = markdown.getNamespace();
            String markdownPath = markdown.getPath();
            int slash = markdownPath.lastIndexOf('/');
            String directory = slash >= 0 ? markdownPath.substring(0, slash) : "";
            path = directory.isEmpty() ? value : directory + "/" + value;
        }

        List<String> segments = new ArrayList<>();
        for (String segment : path.split("/")) {
            if (segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                if (segments.isEmpty()) {
                    return null;
                }
                segments.remove(segments.size() - 1);
            } else {
                segments.add(segment);
            }
        }
        if (segments.isEmpty()) {
            return null;
        }
        return ResourceLocation.tryBuild(namespace, String.join("/", segments));
    }

    @Nullable
    private static String readLocalized(ResourceLocation markdown) {
        Minecraft minecraft = Minecraft.getInstance();
        String language = minecraft.getLanguageManager().getSelected();
        String shortLanguage = language;
        int underscore = language.indexOf('_');
        if (underscore >= 0) {
            shortLanguage = language.substring(0, underscore);
        }

        List<String> candidates = new ArrayList<>();
        addSuffixCandidate(candidates, markdown, shortLanguage);
        addSuffixCandidate(candidates, markdown, language);
        candidates.add(markdown.getPath());

        for (String path : candidates) {
            ResourceLocation id = ResourceLocation.tryBuild(markdown.getNamespace(), path);
            if (id == null) {
                continue;
            }
            Optional<Resource> resource = minecraft.getResourceManager().getResource(id);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().open()) {
                    return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    ElementsPlus.LOGGER.error("Failed to read experiment chapter Markdown {}", id, e);
                }
            }
        }

        ElementsPlus.LOGGER.warn("Missing experiment chapter Markdown resource: {}", markdown);
        return null;
    }

    private static void addSuffixCandidate(List<String> candidates, ResourceLocation markdown, String suffix) {
        if (suffix == null || suffix.isBlank()) {
            return;
        }
        String path = markdown.getPath();
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        String candidate;
        if (dot > slash) {
            candidate = path.substring(0, dot) + "." + suffix + path.substring(dot);
        } else {
            candidate = path + "." + suffix;
        }
        if (!candidate.equals(path)) {
            candidates.add(candidate);
        }
    }

    public sealed interface Part permits TextPart, ExperimentPart, SpecialTextPart,
            ImagePart, HorizontalRulePart {
    }

    public record TextPart(List<Component> lines) implements Part {
    }

    public record ExperimentPart(List<String> experimentIds) implements Part {
    }

    public record SpecialTextPart(List<ResourceLocation> providers) implements Part {
    }

    public record ImagePart(String description, @Nullable ResourceLocation image, String rawPath) implements Part {
    }

    public record HorizontalRulePart() implements Part {
    }
}
