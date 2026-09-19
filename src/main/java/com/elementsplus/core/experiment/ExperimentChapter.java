package com.elementsplus.core.experiment;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
        public List<BaseExperiment> optional;

        public ExperimentSection(List<BaseExperiment> experiments) {
            this.experiments = experiments;
        }

        public void setOptional(List<BaseExperiment> optional) {
            this.optional = optional;
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

    public ExperimentChapter(String name, ResourceLocation icon, List<Section> sections, Set<ExperimentChapter> dependencies) {
        this.name = name;
        this.icon = icon;
        this.sections = sections;
        this.dependencies = dependencies;
    }

    public Component getDisplayName() {
        return Component.translatable("experiment.elements-plus.group." + name);
    }

}
