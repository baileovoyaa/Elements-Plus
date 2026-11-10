package com.elementsplus.core.experiment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/** 数据包中的章节定义；运行时再解析依赖和实验。 */
public record ExperimentChapterDefinition(
        Optional<ResourceLocation> icon,
        List<ResourceLocation> dependencies,
        Optional<ResourceLocation> markdown,
        List<ResourceLocation> specialText,
        List<String> experiments,
        List<String> optional,
        List<SectionDefinition> sections
) {
    public static final Codec<ExperimentChapterDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(ExperimentChapterDefinition::icon),
            ResourceLocation.CODEC.listOf().optionalFieldOf("dependencies", List.of())
                    .forGetter(ExperimentChapterDefinition::dependencies),
            ResourceLocation.CODEC.optionalFieldOf("markdown").forGetter(ExperimentChapterDefinition::markdown),
            ResourceLocation.CODEC.listOf().optionalFieldOf("special_text", List.of())
                    .forGetter(ExperimentChapterDefinition::specialText),
            Codec.STRING.listOf().optionalFieldOf("experiments", List.of())
                    .forGetter(ExperimentChapterDefinition::experiments),
            Codec.STRING.listOf().optionalFieldOf("optional", List.of())
                    .forGetter(ExperimentChapterDefinition::optional),
            SectionDefinition.CODEC.listOf().optionalFieldOf("sections", List.of())
                    .forGetter(ExperimentChapterDefinition::sections)
    ).apply(instance, ExperimentChapterDefinition::new));

    /** 一个章节段落定义。 */
    public sealed interface SectionDefinition permits TextSectionDefinition,
            ExperimentSectionDefinition, ImageSectionDefinition, DynamicTextSectionDefinition {
        Codec<SectionDefinition> CODEC = Codec.STRING.dispatch("type", SectionDefinition::type,
                type -> switch (type) {
                    case "text" -> TextSectionDefinition.MAP_CODEC;
                    case "experiments" -> ExperimentSectionDefinition.MAP_CODEC;
                    case "image" -> ImageSectionDefinition.MAP_CODEC;
                    case "dynamic_text" -> DynamicTextSectionDefinition.MAP_CODEC;
                    default -> MapCodec.unit((SectionDefinition) null);
                });

        String type();
    }

    /**
     * 文本段落。section 对应语言键中的 sectionN；lines 是行数，args 描述哪些行有格式化参数。
     */
    public record TextSectionDefinition(int section, int lines, List<LineDefinition> args)
            implements SectionDefinition {
        public static final MapCodec<TextSectionDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("section").forGetter(TextSectionDefinition::section),
                Codec.INT.fieldOf("lines").forGetter(TextSectionDefinition::lines),
                LineDefinition.CODEC.listOf().optionalFieldOf("args", List.of())
                        .forGetter(TextSectionDefinition::args)
        ).apply(instance, TextSectionDefinition::new));

        @Override
        public String type() {
            return "text";
        }
    }

    /** 文本行参数定义：整行样式和行内参数。 */
    public record LineDefinition(int line, List<String> style, List<ParamDefinition> params) {
        public static final Codec<LineDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("line").forGetter(LineDefinition::line),
                Codec.STRING.listOf().optionalFieldOf("style", List.of()).forGetter(LineDefinition::style),
                ParamDefinition.CODEC.listOf().optionalFieldOf("params", List.of()).forGetter(LineDefinition::params)
        ).apply(instance, LineDefinition::new));
    }

    /**
     * 行内参数：style 使用自动翻译键 &lt;行键&gt;.argN；
     * component 直接嵌入组件（例如物品显示名）。
     */
    public record ParamDefinition(Optional<Component> component, List<String> style) {
        public static final Codec<ParamDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.optionalFieldOf("component").forGetter(ParamDefinition::component),
                Codec.STRING.listOf().optionalFieldOf("style", List.of()).forGetter(ParamDefinition::style)
        ).apply(instance, ParamDefinition::new));
    }

    /** 一组实验；optional 中的实验是选做。 */
    public record ExperimentSectionDefinition(List<String> experiments, List<String> optional)
            implements SectionDefinition {
        public static final MapCodec<ExperimentSectionDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.listOf().fieldOf("experiments").forGetter(ExperimentSectionDefinition::experiments),
                Codec.STRING.listOf().optionalFieldOf("optional", List.of())
                        .forGetter(ExperimentSectionDefinition::optional)
        ).apply(instance, ExperimentSectionDefinition::new));

        @Override
        public String type() {
            return "experiments";
        }
    }

    public record ImageSectionDefinition(ResourceLocation image, int width, int height)
            implements SectionDefinition {
        public static final MapCodec<ImageSectionDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("image").forGetter(ImageSectionDefinition::image),
                Codec.INT.fieldOf("width").forGetter(ImageSectionDefinition::width),
                Codec.INT.fieldOf("height").forGetter(ImageSectionDefinition::height)
        ).apply(instance, ImageSectionDefinition::new));

        @Override
        public String type() {
            return "image";
        }
    }

    /** 依赖世界种子的特殊文本块，目前支持 elements-plus:rom_values。 */
    public record DynamicTextSectionDefinition(ResourceLocation provider) implements SectionDefinition {
        public static final MapCodec<DynamicTextSectionDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("provider").forGetter(DynamicTextSectionDefinition::provider)
        ).apply(instance, DynamicTextSectionDefinition::new));

        @Override
        public String type() {
            return "dynamic_text";
        }
    }
}
