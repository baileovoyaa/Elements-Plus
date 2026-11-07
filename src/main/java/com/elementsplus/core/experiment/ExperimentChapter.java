package com.elementsplus.core.experiment;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExperimentChapter {

    public String name;
    public ResourceLocation icon;
    public List<Section> sections;
    public Set<ExperimentChapter> dependencies;

    public abstract static class Section {
    }

    public static class TextSection extends Section {
        public List<Component> text;

        public TextSection(List<Component> text) {
            this.text = text;
        }
    }

    public static class ExperimentSection extends Section {
        public List<BaseExperiment> experiments;
        public Set<BaseExperiment> optional;

        public ExperimentSection(List<BaseExperiment> experiments) {
            this.experiments = experiments;
        }

        public ExperimentSection optional(BaseExperiment... optional) {
            this.optional = Set.of(optional);
            return this;
        }

        public ExperimentSection optional() {
            this.optional = Set.copyOf(experiments);
            return this;
        }
    }

    public static class ImageSection extends Section {
        public ResourceLocation image;
        public int width;
        public int height;

        public ImageSection(ResourceLocation image, int width, int height) {
            this.image = image;
            this.width = width;
            this.height = height;
        }
    }

    /** 内容依赖存档世界种子的文本段（例如 ROM 实验的随机字节表）。 */
    public static class DynamicTextSection extends Section {
        public java.util.function.LongFunction<List<Component>> text;

        public DynamicTextSection(java.util.function.LongFunction<List<Component>> text) {
            this.text = text;
        }
    }

    public ExperimentChapter(String name, ResourceLocation icon, List<Section> sections, Set<ExperimentChapter> dependencies) {
        this.name = name;
        this.icon = icon;
        this.sections = sections;
        this.dependencies = dependencies;
    }

    public Component getDisplayName() {
        return Component.translatable("experiment.elements-plus.group." + name);
    }

    public Set<BaseExperiment> getExperiments() {
        Set<BaseExperiment> experiments = new HashSet<>();
        for (Section section : sections) {
            if (section instanceof ExperimentSection experimentSection) {
                experiments.addAll(new HashSet<>() {{
                    addAll(experimentSection.experiments);
                    if (experimentSection.optional != null) {
                        removeAll(experimentSection.optional);
                    }
                }});
            }
        }
        return experiments;
    }

    /** 章节包含的所有实验，包括选做实验。 */
    public Set<BaseExperiment> getAllExperiments() {
        Set<BaseExperiment> experiments = new HashSet<>();
        for (Section section : sections) {
            if (section instanceof ExperimentSection experimentSection) {
                experiments.addAll(experimentSection.experiments);
            }
        }
        return experiments;
    }

    /** 章节中的选做实验。 */
    public Set<BaseExperiment> getOptionalExperiments() {
        Set<BaseExperiment> experiments = new HashSet<>();
        for (Section section : sections) {
            if (section instanceof ExperimentSection experimentSection && experimentSection.optional != null) {
                experiments.addAll(experimentSection.optional);
            }
        }
        return experiments;
    }

    public BaseExperiment getFirstExperiment() {
        for (Section section : sections) {
            if (section instanceof ExperimentSection experimentSection) {
                for (BaseExperiment experiment : experimentSection.experiments) {
                    if (experimentSection.optional == null || !experimentSection.optional.contains(experiment)) {
                        return experiment;
                    }
                }
            }
        }
        return null;
    }

    public boolean containsExperiment(BaseExperiment experiment) {
        if (experiment == null) {
            return false;
        }
        for (Section section : sections) {
            if (section instanceof ExperimentSection experimentSection) {
                if (experimentSection.experiments.contains(experiment)) {
                    return true;
                }
            }
        }
        return false;
    }

}
