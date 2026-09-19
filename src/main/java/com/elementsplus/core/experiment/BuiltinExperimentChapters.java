package com.elementsplus.core.experiment;

import com.elementsplus.ElementsPlus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.*;

public class BuiltinExperimentChapters {
    public static final List<ExperimentChapter> BUILTIN_EXPERIMENT_CHAPTERS = new ArrayList<>();

    public static final ExperimentChapter INTRO = register(
            new ExperimentChapter("intro", null,
                    List.of(
                            new ExperimentChapter.ImageSection(ElementsPlus.id("textures/gui/experiment_table/chapters/intro/intro.png"), 3167, 1061),
                            new ExperimentChapter.TextSection(List.of(
                                    Component.translatable("experiment.elements-plus.group.intro.section0.text0")
                            )),
                            new ExperimentChapter.TextSection(List.of(
                                    Component.translatable("experiment.elements-plus.group.intro.section1.text0").withStyle(ChatFormatting.BOLD),
                                    Component.translatable("experiment.elements-plus.group.intro.section1.text1"),
                                    Component.translatable("experiment.elements-plus.group.intro.section1.text2"),
                                    Component.translatable("experiment.elements-plus.group.intro.section1.text3"),
                                    Component.translatable("experiment.elements-plus.group.intro.section1.text4"),
                                    Component.translatable("experiment.elements-plus.group.intro.section1.text5")
                            )),
                            new ExperimentChapter.TextSection(List.of(
                                    Component.translatable("experiment.elements-plus.group.intro.section2.text0")
                            ))
                    ), Set.of()
            )
    );

    public static final ExperimentChapter AMPLIFIER = register(
            new ExperimentChapter("amplifier", null,
                    List.of(
                            new ExperimentChapter.TextSection(List.of(
                                    Component.translatable("experiment.elements-plus.group.amplifier.section0.text0"),
                                    Component.translatable("experiment.elements-plus.group.amplifier.section0.text1"),
                                    Component.translatable("experiment.elements-plus.group.amplifier.section0.text2", Component.translatable("experiment.elements-plus.group.amplifier.section0.text2.arg0").withStyle(ChatFormatting.GOLD)),
                                    Component.translatable("experiment.elements-plus.group.amplifier.section0.text3"),
                                    Component.translatable("experiment.elements-plus.group.amplifier.section0.text4"),
                                    Component.translatable("experiment.elements-plus.group.amplifier.section1.text0"),
                                    Component.translatable("experiment.elements-plus.group.amplifier.section1.text1")
                            )),
                            new ExperimentChapter.ExperimentSection(List.of(
                                    BuiltinExperiments.AMPLIFIER
                            ))
                    ), Set.of(INTRO)
            )
    );

    public static final ExperimentChapter FIRST_GATE = register(
            new ExperimentChapter("first_gate", null,
                    List.of(
                            new ExperimentChapter.TextSection(List.of(
                                    Component.translatable("experiment.elements-plus.group.first_gate.section0.text0")
                            )),
                            new ExperimentChapter.ExperimentSection(List.of(
                                    BuiltinExperiments.NOT_GATE
                            ))
                    ), Set.of(AMPLIFIER)
            )
    );

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
