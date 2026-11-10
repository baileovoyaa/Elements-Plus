package com.elementsplus.core.experiment;

import com.elementsplus.ElementsPlus;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 数据驱动的实验章节。
 *
 * <p>章节定义位于 {@code data/*}{@code /elements-plus/experiment_chapter/*.json}，
 * 章节顺序由标签 {@code elements-plus:index} 决定：
 * {@code data/elements-plus/tags/elements-plus/experiment_chapter/index.json}。</p>
 */
public class BuiltinExperimentChapters {
    public static final ResourceKey<Registry<ExperimentChapterDefinition>> CHAPTER_REGISTRY =
            ResourceKey.createRegistryKey(ElementsPlus.id("experiment_chapter"));
    public static final TagKey<ExperimentChapterDefinition> ORDER_TAG =
            TagKey.create(CHAPTER_REGISTRY, ElementsPlus.id("index"));

    private static final Codec<ExperimentChapterDefinition> CHAPTER_CODEC = ExperimentChapterDefinition.CODEC;
    private static boolean initialized;

    /** 当前服务端/客户端已解析的章节列表，顺序来自 index 标签。 */
    public static volatile List<ExperimentChapter> BUILTIN_EXPERIMENT_CHAPTERS = List.of();
    private static volatile Map<ResourceLocation, ExperimentChapter> chaptersById = Map.of();

    private BuiltinExperimentChapters() {
    }

    /** 注册同步的自定义动态注册表。应在模组初始化时调用一次。 */
    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        DynamicRegistries.registerSynced(CHAPTER_REGISTRY, CHAPTER_CODEC);
    }

    /**
     * 从注册表重新构建章节对象。服务端启动时调用；客户端在首次打开实验桌时补加载。
     */
    public static void reload(RegistryAccess registryAccess) {
        Registry<ExperimentChapterDefinition> definitions =
                registryAccess.registry(CHAPTER_REGISTRY).orElse(null);
        if (definitions == null) {
            clear();
            ElementsPlus.LOGGER.warn("Experiment chapter registry is not available yet");
            return;
        }
        List<ResourceLocation> order = new ArrayList<>();
        LinkedHashSet<ResourceLocation> seenOrder = new LinkedHashSet<>();
        definitions.getTag(ORDER_TAG).ifPresent(tag -> {
            for (var holder : tag) {
                holder.unwrapKey().map(ResourceKey::location).ifPresent(id -> {
                    if (seenOrder.add(id)) {
                        order.add(id);
                    }
                });
            }
        });
        for (ResourceKey<ExperimentChapterDefinition> key : definitions.registryKeySet()) {
            if (seenOrder.add(key.location())) {
                order.add(key.location());
            }
        }

        Map<ResourceLocation, ExperimentChapter> byId = new LinkedHashMap<>();
        List<ExperimentChapter> chapters = new ArrayList<>();
        for (ResourceLocation id : order) {
            ExperimentChapterDefinition definition = definitions.get(id);
            if (definition == null) {
                continue;
            }
            ExperimentChapter chapter = buildChapter(id, definition);
            byId.put(id, chapter);
            chapters.add(chapter);
        }

        // 第二遍解析依赖，此时所有章节对象已经存在。
        for (ResourceLocation id : byId.keySet()) {
            ExperimentChapterDefinition definition = definitions.get(id);
            if (definition == null) {
                continue;
            }
            ExperimentChapter chapter = byId.get(id);
            for (ResourceLocation dependencyId : definition.dependencies()) {
                ExperimentChapter dependency = byId.get(dependencyId);
                if (dependency == null) {
                    ElementsPlus.LOGGER.warn("Experiment chapter {} references unknown dependency {}",
                            id, dependencyId);
                } else {
                    chapter.dependencies.add(dependency);
                }
            }
        }

        BUILTIN_EXPERIMENT_CHAPTERS = List.copyOf(chapters);
        chaptersById = Map.copyOf(byId);
        ElementsPlus.LOGGER.info("Loaded {} data-driven experiment chapters", chapters.size());
    }

    /** 客户端可能在服务端章节尚未加载的本 JVM 中使用；为空时从当前 registry access 补加载。 */
    public static void ensureLoaded(RegistryAccess registryAccess) {
        if (BUILTIN_EXPERIMENT_CHAPTERS.isEmpty()) {
            reload(registryAccess);
        }
    }

    /** 客户端断开连接时清空，避免下次连接其他服务器时沿用旧章节。 */
    public static void clear() {
        BUILTIN_EXPERIMENT_CHAPTERS = List.of();
        chaptersById = Map.of();
    }

    private static ExperimentChapter buildChapter(ResourceLocation id, ExperimentChapterDefinition definition) {
        // 内置章节沿用短名称以保持现有语言键；自定义命名空间使用完整 id，避免同名冲突。
        String name = id.getNamespace().equals(ElementsPlus.MOD_ID) ? id.getPath() : id.toString();
        List<ExperimentChapter.Section> sections = new ArrayList<>();
        if (definition.markdown().isPresent()) {
            List<BaseExperiment> experiments = resolveExperiments(definition.experiments());
            Set<BaseExperiment> optional = resolveOptionalExperiments(
                    definition.optional(), experiments);
            sections.add(new ExperimentChapter.MarkdownSection(
                    definition.markdown().get(), experiments, optional, definition.specialText()));
        }
        for (ExperimentChapterDefinition.SectionDefinition section : definition.sections()) {
            if (section instanceof ExperimentChapterDefinition.TextSectionDefinition text) {
                sections.add(buildTextSection(name, text));
            } else if (section instanceof ExperimentChapterDefinition.ExperimentSectionDefinition experiments) {
                sections.add(buildExperimentSection(experiments));
            } else if (section instanceof ExperimentChapterDefinition.ImageSectionDefinition image) {
                sections.add(new ExperimentChapter.ImageSection(image.image(), image.width(), image.height()));
            } else if (section instanceof ExperimentChapterDefinition.DynamicTextSectionDefinition dynamic) {
                sections.add(new ExperimentChapter.DynamicTextSection(
                        seed -> buildDynamicText(dynamic.provider(), seed)));
            }
        }
        ResourceLocation icon = definition.icon().orElse(null);
        return new ExperimentChapter(name, icon, sections, new LinkedHashSet<>());
    }

    private static ExperimentChapter.TextSection buildTextSection(
            String chapterName, ExperimentChapterDefinition.TextSectionDefinition definition) {
        String prefix = "experiment.elements-plus.group." + chapterName + ".section" + definition.section();
        List<ArgSpec> specs = new ArrayList<>();
        for (ExperimentChapterDefinition.LineDefinition line : definition.args()) {
            List<ParamSpec> params = new ArrayList<>();
            for (ExperimentChapterDefinition.ParamDefinition param : line.params()) {
                if (param.component().isPresent()) {
                    params.add(new ParamSpec(param.component().get()));
                } else {
                    params.add(new ParamSpec(parseFormats(param.style())));
                }
            }
            List<ChatFormatting> formats = parseFormats(line.style());
            specs.add(new ArgSpec(line.line(), params, formats.toArray(ChatFormatting[]::new)));
        }
        return buildTextSection(prefix, definition.lines(), specs.toArray(ArgSpec[]::new));
    }

    private static List<BaseExperiment> resolveExperiments(List<String> rawIds) {
        List<BaseExperiment> experiments = new ArrayList<>();
        for (String raw : rawIds) {
            BaseExperiment experiment = resolveExperiment(raw);
            if (experiment != null && !experiments.contains(experiment)) {
                experiments.add(experiment);
            }
        }
        return experiments;
    }

    /** 可选实验会自动补进总实验列表，避免 optional 单独出现时漏掉。 */
    private static Set<BaseExperiment> resolveOptionalExperiments(
            List<String> rawOptional, List<BaseExperiment> allExperiments) {
        Set<BaseExperiment> optional = new LinkedHashSet<>();
        for (String raw : rawOptional) {
            BaseExperiment experiment = resolveExperiment(raw);
            if (experiment == null) {
                continue;
            }
            optional.add(experiment);
            if (!allExperiments.contains(experiment)) {
                allExperiments.add(experiment);
            }
        }
        return optional;
    }

    /** 供 Markdown 章节的 epp-text 块使用。 */
    public static List<Component> dynamicText(ResourceLocation provider, long seed) {
        return buildDynamicText(provider, seed);
    }

    private static ExperimentChapter.ExperimentSection buildExperimentSection(
            ExperimentChapterDefinition.ExperimentSectionDefinition definition) {
        List<BaseExperiment> experiments = new ArrayList<>();
        for (String raw : definition.experiments()) {
            BaseExperiment experiment = resolveExperiment(raw);
            if (experiment != null) {
                experiments.add(experiment);
            }
        }
        Set<BaseExperiment> optional = new LinkedHashSet<>();
        for (String raw : definition.optional()) {
            BaseExperiment experiment = resolveExperiment(raw);
            if (experiment != null) {
                optional.add(experiment);
            }
        }
        ExperimentChapter.ExperimentSection section = new ExperimentChapter.ExperimentSection(experiments);
        if (!optional.isEmpty()) {
            section.optional(optional.toArray(BaseExperiment[]::new));
        }
        return section;
    }

    @Nullable
    private static BaseExperiment resolveExperiment(String raw) {
        ResourceLocation id = parseId(raw);
        if (id == null) {
            ElementsPlus.LOGGER.warn("Invalid experiment id in experiment chapter: {}", raw);
            return null;
        }
        BaseExperiment experiment = BuiltinExperiments.byId(id.getPath());
        if (experiment == null) {
            ElementsPlus.LOGGER.warn("Unknown experiment in experiment chapter: {}", raw);
        }
        return experiment;
    }

    @Nullable
    private static ResourceLocation parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.indexOf(':') >= 0
                ? ResourceLocation.tryParse(raw)
                : ResourceLocation.tryParse(ElementsPlus.MOD_ID + ":" + raw);
    }

    private static List<ChatFormatting> parseFormats(List<String> names) {
        List<ChatFormatting> formats = new ArrayList<>();
        for (String name : names) {
            try {
                formats.add(ChatFormatting.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                ElementsPlus.LOGGER.warn("Unknown ChatFormatting in experiment chapter: {}", name);
            }
        }
        return formats;
    }

    private static List<Component> buildDynamicText(ResourceLocation provider, long seed) {
        if (provider.equals(ElementsPlus.id("rom_values"))) {
            return buildRomValueLines(seed);
        }
        ElementsPlus.LOGGER.warn("Unknown dynamic experiment chapter text provider: {}", provider);
        return List.of(Component.literal("Unknown dynamic text provider: " + provider));
    }

    private static List<Component> buildRomValueLines(long worldSeed) {
        byte[] values = MemoryExperimentData.romValues(worldSeed);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(
                "experiment.elements-plus.group.multi_byte_memory.rom_values"));
        for (int row = 0; row < 4; row++) {
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < 4; col++) {
                if (col > 0) sb.append("  ");
                int address = row * 4 + col;
                sb.append(String.format("%02X: %02X", address, values[address] & 0xFF));
            }
            lines.add(Component.literal(sb.toString()).withColor(0xE0E0E0));
        }
        return lines;
    }

    // ========== 文本参数构建 ==========

    /**
     * 一个文本参数：要么是一组样式（会生成 key.argN 翻译键来自动着色），
     * 要么是一个直接嵌入的 Component 字面量（如物品显示名，不生成翻译键）。
     */
    public record ParamSpec(List<ChatFormatting> formats, Component literal) {

        public ParamSpec(List<ChatFormatting> formats) {
            this(formats, null);
        }

        public ParamSpec(Component literal) {
            this(List.of(), literal);
        }

        Component build(String key) {
            if (literal != null) {
                return literal;
            }
            MutableComponent c = Component.translatable(key);
            for (ChatFormatting f : formats) c = c.withStyle(f);
            return c;
        }
    }

    public record ArgSpec(int line, List<ParamSpec> params, ChatFormatting... formats) {
        public ArgSpec withStyle(ChatFormatting... formats) {
            return new ArgSpec(line, params, formats);
        }
    }

    public static ParamSpec style(ChatFormatting... formats) {
        return new ParamSpec(List.of(formats));
    }

    public static ParamSpec component(Component component) {
        return new ParamSpec(component);
    }

    public static ArgSpec line(int line, ParamSpec... params) {
        return new ArgSpec(line, List.of(params));
    }

    private static ExperimentChapter.TextSection buildTextSection(
            String keyPrefix, int lineCount, ArgSpec... specs) {
        Map<Integer, ArgSpec> byLine = new HashMap<>();
        for (ArgSpec s : specs) byLine.put(s.line(), s);

        List<Component> lines = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            String key = keyPrefix + ".text" + i;
            ArgSpec spec = byLine.get(i);
            if (spec == null) {
                lines.add(Component.translatable(key));
            } else if (spec.params().isEmpty()) {
                lines.add(Component.translatable(key).withStyle(spec.formats()));
            } else {
                Object[] args = new Object[spec.params().size()];
                for (int j = 0; j < args.length; j++) {
                    args[j] = spec.params().get(j).build(key + ".arg" + j);
                }
                lines.add(Component.translatable(key, args).withStyle(spec.formats()));
            }
        }
        return new ExperimentChapter.TextSection(lines);
    }

    // ========== 解锁 / 查询 ==========

    /** 某章节及其全部前置章节的必做实验。 */
    public static Set<BaseExperiment> requiredExperimentsFor(ExperimentChapter chapter) {
        Set<BaseExperiment> result = new HashSet<>();
        collectRequiredExperiments(chapter, result, new HashSet<>());
        return result;
    }

    private static void collectRequiredExperiments(ExperimentChapter chapter,
                                                   Set<BaseExperiment> result,
                                                   Set<ExperimentChapter> visited) {
        if (chapter == null || !visited.add(chapter)) {
            return;
        }
        result.addAll(chapter.getExperiments());
        for (ExperimentChapter dependency : chapter.dependencies) {
            collectRequiredExperiments(dependency, result, visited);
        }
    }

    /** 所有章节中出现过的实验，包括选做实验。 */
    public static Set<BaseExperiment> allExperimentsIncludingOptional() {
        Set<BaseExperiment> result = new HashSet<>();
        for (ExperimentChapter chapter : BUILTIN_EXPERIMENT_CHAPTERS) {
            for (ExperimentChapter.Section section : chapter.sections) {
                if (section instanceof ExperimentChapter.ExperimentSection experimentSection) {
                    result.addAll(experimentSection.experiments);
                }
            }
        }
        return result;
    }

    public static boolean isOptionalExperiment(BaseExperiment experiment) {
        for (ExperimentChapter chapter : BUILTIN_EXPERIMENT_CHAPTERS) {
            for (ExperimentChapter.Section section : chapter.sections) {
                if (section instanceof ExperimentChapter.ExperimentSection experimentSection
                        && experimentSection.optional != null
                        && experimentSection.optional.contains(experiment)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public static ExperimentChapter byName(String name) {
        for (ExperimentChapter c : BUILTIN_EXPERIMENT_CHAPTERS) {
            if (c.name.equals(name)) {
                return c;
            }
        }
        return null;
    }

    public static Set<ExperimentChapter> getUnlockedByName(Set<String> passedIds) {
        return getUnlockedByName(passedIds, Set.of());
    }

    public static Set<ExperimentChapter> getUnlockedByName(Set<String> passedIds, Set<String> forcedNames) {
        Set<BaseExperiment> passed = new HashSet<>();
        for (String id : passedIds) {
            BaseExperiment experiment = BuiltinExperiments.byId(id);
            if (experiment != null) {
                passed.add(experiment);
            }
        }
        return getUnlocked(passed, forcedNames);
    }

    public static Set<ExperimentChapter> getUnlocked(Set<BaseExperiment> passed) {
        return getUnlocked(passed, Set.of());
    }

    public static Set<ExperimentChapter> getUnlocked(Set<BaseExperiment> passed, Set<String> forcedNames) {
        // 1. 预计算每个章节的实验是否已经全部通过
        Map<ExperimentChapter, Boolean> chapterOk = new HashMap<>();
        for (ExperimentChapter c : BUILTIN_EXPERIMENT_CHAPTERS) {
            // 0 实验章节 levels 为空，containsAll 返回 true
            chapterOk.put(c, passed.containsAll(c.getExperiments()));
        }

        // 2. 建立反向依赖：完成 p 后可能解锁哪些章节
        Map<ExperimentChapter, List<ExperimentChapter>> successors = new HashMap<>();
        for (ExperimentChapter c : BUILTIN_EXPERIMENT_CHAPTERS) {
            for (ExperimentChapter p : c.dependencies) {
                successors.computeIfAbsent(p, k -> new ArrayList<>()).add(c);
            }
        }

        // 3. 每个章节还剩多少个前驱未完成
        Map<ExperimentChapter, Integer> predRemain = new HashMap<>();
        for (ExperimentChapter c : BUILTIN_EXPERIMENT_CHAPTERS) {
            predRemain.put(c, c.dependencies.size());
        }

        Set<ExperimentChapter> forced = new HashSet<>();
        for (String name : forcedNames) {
            ExperimentChapter chapter = byName(name);
            if (chapter != null) {
                forced.add(chapter);
            }
        }

        Set<ExperimentChapter> unlocked = new HashSet<>();
        Deque<ExperimentChapter> queue = new ArrayDeque<>();

        // 4. 无前驱的章节直接解锁；被强制解锁的章节不受依赖限制
        for (ExperimentChapter c : BUILTIN_EXPERIMENT_CHAPTERS) {
            if (predRemain.get(c) == 0 || forced.contains(c)) {
                unlocked.add(c);
                queue.add(c);
            }
        }

        Set<ExperimentChapter> completed = new HashSet<>();

        // 5. 工作队列传播
        while (!queue.isEmpty()) {
            ExperimentChapter c = queue.poll();

            // 如果该章节实验没全通，则不会完成，也就不会解锁后继
            if (!chapterOk.get(c)) {
                continue;
            }

            // 防止重复处理（正常情况下每个章节只会入队一次）
            if (!completed.add(c)) {
                continue;
            }

            List<ExperimentChapter> nextExperimentChapters = successors.get(c);
            if (nextExperimentChapters == null) {
                continue;
            }

            for (ExperimentChapter v : nextExperimentChapters) {
                int remain = predRemain.get(v) - 1;
                predRemain.put(v, remain);

                // 所有前驱都完成了，解锁 v
                if (remain == 0) {
                    unlocked.add(v);
                    queue.add(v);
                }
            }
        }

        return unlocked;
    }

    /** 收集若干章节的实验；includeOptional 为 false 时排除各章节的选做实验。 */
    public static Set<BaseExperiment> experimentsForChapters(
            Collection<ExperimentChapter> chapters, boolean includeOptional) {
        Set<BaseExperiment> result = new HashSet<>();
        for (ExperimentChapter chapter : chapters) {
            for (ExperimentChapter.Section section : chapter.sections) {
                if (!(section instanceof ExperimentChapter.ExperimentSection experimentSection)) {
                    continue;
                }
                for (BaseExperiment experiment : experimentSection.experiments) {
                    if (includeOptional || experimentSection.optional == null
                            || !experimentSection.optional.contains(experiment)) {
                        result.add(experiment);
                    }
                }
            }
        }
        return result;
    }

    /** 章节自身和/或它直接、间接依赖的章节。 */
    public static Set<ExperimentChapter> chaptersWithDependencies(ExperimentChapter chapter, boolean includeSelf) {
        Set<ExperimentChapter> result = new LinkedHashSet<>();
        Deque<ExperimentChapter> queue = new ArrayDeque<>();
        if (includeSelf && chapter != null) {
            result.add(chapter);
        }
        if (chapter != null) {
            queue.addAll(chapter.dependencies);
        }
        while (!queue.isEmpty()) {
            ExperimentChapter current = queue.poll();
            if (!result.add(current)) {
                continue;
            }
            queue.addAll(current.dependencies);
        }
        return result;
    }

    /** 章节自身和/或直接、间接依赖它的后续章节。 */
    public static Set<ExperimentChapter> chaptersDependingOn(ExperimentChapter chapter, boolean includeSelf) {
        Set<ExperimentChapter> result = new LinkedHashSet<>();
        if (chapter == null) {
            return result;
        }
        Deque<ExperimentChapter> queue = new ArrayDeque<>();
        if (includeSelf) {
            result.add(chapter);
        }
        queue.add(chapter);
        while (!queue.isEmpty()) {
            ExperimentChapter current = queue.poll();
            for (ExperimentChapter candidate : BUILTIN_EXPERIMENT_CHAPTERS) {
                if (candidate.dependencies.contains(current) && result.add(candidate)) {
                    queue.add(candidate);
                }
            }
        }
        return result;
    }

}
