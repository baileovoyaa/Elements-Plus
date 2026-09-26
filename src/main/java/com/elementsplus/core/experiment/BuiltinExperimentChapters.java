package com.elementsplus.core.experiment;

import com.elementsplus.ElementsPlus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Function;

import static net.minecraft.ChatFormatting.*;

public class BuiltinExperimentChapters {
    public static final List<ExperimentChapter> BUILTIN_EXPERIMENT_CHAPTERS = new ArrayList<>();

    // ========== 通用构建入口 ==========
    private static ExperimentChapter chapter(
            String id,
            ResourceLocation icon,
            Function<SectionContext, List<ExperimentChapter.Section>> body,
            Set<ExperimentChapter> dependencies
    ) {
        SectionContext ctx = new SectionContext("experiment.elements-plus.group." + id + ".");
        return new ExperimentChapter(id, icon, body.apply(ctx), dependencies);
    }

    // ========== 章节上下文：自动派生前缀 + 自动递增 section 序号 ==========
    public static final class SectionContext {
        private final String prefix;
        private int nextSection = 0;

        private SectionContext(String prefix) {
            this.prefix = prefix;
        }

        public ExperimentChapter.TextSection text(int lineCount, ArgSpec... specs) {
            String keyPrefix = prefix + "section" + nextSection++;
            return buildTextSection(keyPrefix, lineCount, specs);
        }

        public ExperimentChapter.ExperimentSection experiments(BaseExperiment... experiments) {
            return new ExperimentChapter.ExperimentSection(List.of(experiments));
        }

        public ExperimentChapter.ImageSection image(ResourceLocation image, int width, int height) {
            return new ExperimentChapter.ImageSection(image, width, height);
        }
    }

    // ========== 行 / 参数描述 ==========
    public record ParamSpec(List<ChatFormatting> formats) {
        Component build(String key) {
            MutableComponent c = Component.translatable(key);
            for (ChatFormatting f : formats) c = c.withStyle(f);
            return c;
        }
    }

    public record ArgSpec(int line, List<ParamSpec> params) {
    }

    public static ParamSpec style(ChatFormatting... formats) {
        return new ParamSpec(List.of(formats));
    }

    public static ArgSpec line(int line, ParamSpec... params) {
        return new ArgSpec(line, List.of(params));
    }

    // ========== 实际生成 TextSection ==========
    private static ExperimentChapter.TextSection buildTextSection(
            String keyPrefix, int lineCount, ArgSpec... specs
    ) {
        Map<Integer, ArgSpec> byLine = new HashMap<>();
        for (ArgSpec s : specs) byLine.put(s.line(), s);

        List<Component> lines = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            String key = keyPrefix + ".text" + i;
            ArgSpec spec = byLine.get(i);
            if (spec == null || spec.params().isEmpty()) {
                lines.add(Component.translatable(key));
            } else {
                Object[] args = new Object[spec.params().size()];
                for (int j = 0; j < args.length; j++) {
                    args[j] = spec.params().get(j).build(key + ".arg" + j);
                }
                lines.add(Component.translatable(key, args));
            }
        }
        return new ExperimentChapter.TextSection(lines);
    }

    public static final ExperimentChapter INTRO = register(chapter("intro", null, ch -> List.of(
            ch.image(ElementsPlus.id("textures/gui/experiment_table/chapters/intro/intro.png"), 3167, 1061),
            ch.text(1),
            ch.text(6, line(0, style(BOLD))),
            ch.text(1)
    ), Set.of()));

    public static final ExperimentChapter AMPLIFIER = register(chapter("amplifier", null, ch -> List.of(
            ch.text(5,
                    line(2, style(GOLD))
            ),
            ch.text(2),
            ch.experiments(BuiltinExperiments.AMPLIFIER),
            ch.text(5,
                    line(1, style(GRAY)),
                    line(2, style(BLUE)),
                    line(3, style(GREEN)),
                    line(4, style(RED))
            )
    ), Set.of(INTRO)));

    public static final ExperimentChapter FIRST_GATE = register(chapter("first_gate", null, ch -> List.of(
            ch.text(12,
                    line(5, style(BOLD))
            ),
            ch.text(5,
                    line(2, style(ITALIC, RED), style(ITALIC, RED)),
                    line(3, style(GOLD))
            ),
            ch.experiments(BuiltinExperiments.NOT_GATE),
            ch.text(4,
                    line(0, style(BLUE)),
                    line(1, style(BLUE)),
                    line(2, style(ITALIC, RED), style(GOLD))
            ),
            ch.experiments(BuiltinExperiments.ANALOG_NOT).optional()
    ), Set.of(AMPLIFIER)));

    public static final ExperimentChapter BASIC_GATE = register(
            new ExperimentChapter("basic_gate", null,
                    List.of(
                            new ExperimentChapter.TextSection(List.of(
                                    Component.translatable("experiment.elements-plus.group.basic_gate.section0.text0")
                            )),
                            new ExperimentChapter.ExperimentSection(List.of(
                                    BuiltinExperiments.OR_GATE,
                                    BuiltinExperiments.NOR_GATE,
                                    BuiltinExperiments.NAND_GATE,
                                    BuiltinExperiments.AND_GATE
                            ))
                    ), Set.of(FIRST_GATE)
            )
    );

    public static final ExperimentChapter ADVANCED_GATE = register(
            new ExperimentChapter("advanced_gate", null,
                    List.of(
                            new ExperimentChapter.TextSection(List.of(
                                    Component.translatable("experiment.elements-plus.group.advanced_gate.section0.text0")
                            )),
                            new ExperimentChapter.ExperimentSection(List.of(
                                    BuiltinExperiments.XOR_GATE,
                                    BuiltinExperiments.XNOR_GATE
                            ))
                    ), Set.of(BASIC_GATE)
            )
    );

    public static final ExperimentChapter ONE_BIT_ADDER = register(
            new ExperimentChapter("one_bit_adder", null,
                    List.of(
                            new ExperimentChapter.TextSection(List.of(
                                    Component.translatable("experiment.elements-plus.group.one_bit_adder.section0.text0")
                            )),
                            new ExperimentChapter.ExperimentSection(List.of(
                                    BuiltinExperiments.HALF_ADDER,
                                    BuiltinExperiments.FULL_ADDER
                            ))
                    ), Set.of(ADVANCED_GATE)
            )
    );

    public static ExperimentChapter register(ExperimentChapter experimentChapter) {
        BUILTIN_EXPERIMENT_CHAPTERS.add(experimentChapter);
        return experimentChapter;
    }

    public static ExperimentChapter byName(String name) {
        for (ExperimentChapter c : BUILTIN_EXPERIMENT_CHAPTERS) {
            if (c.name.equals(name)) {
                return c;
            }
        }
        return null;
    }

    public static Set<ExperimentChapter> getUnlockedByName(Set<String> passedIds) {
        Set<BaseExperiment> passed = new HashSet<>();
        for (String id : passedIds) {
            BaseExperiment experiment = BuiltinExperiments.byId(id);
            if (experiment != null) {
                passed.add(experiment);
            }
        }
        return getUnlocked(passed);
    }

    public static Set<ExperimentChapter> getUnlocked(Set<BaseExperiment> passed) {
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

        Set<ExperimentChapter> unlocked = new HashSet<>();
        Deque<ExperimentChapter> queue = new ArrayDeque<>();

        // 4. 无前驱的章节直接解锁
        for (ExperimentChapter c : BUILTIN_EXPERIMENT_CHAPTERS) {
            if (predRemain.get(c) == 0) {
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

}
