package com.elementsplus.core.experiment;

import com.elementsplus.ElementsPlus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    public static final ExperimentChapter BASIC_GATE = register(
            new ExperimentChapter("basic_gate", null,
                    List.of(
                            new ExperimentChapter.TextSection(List.of(
                                    Component.translatable("experiment.elements-plus.group.basic_gate.text1")
                            )),
                            new ExperimentChapter.ExperimentSection(List.of(
                                    BuiltinExperiments.AND_GATE,
                                    BuiltinExperiments.OR_GATE,
                                    BuiltinExperiments.NOT_GATE,
                                    BuiltinExperiments.XOR_GATE
                            )) {{
                                setOptional(List.of(BuiltinExperiments.XOR_GATE));
                            }}
                    ), Set.of(INTRO)
            )
    );

    public static ExperimentChapter register(ExperimentChapter experimentChapter) {
        BUILTIN_EXPERIMENT_CHAPTERS.add(experimentChapter);
        return experimentChapter;
    }

}
