package com.elementsplus.core.circuit.storage;

import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 一个已编译电路。内置元件和自定义元件都使用同一套分离存储格式：
 * 物品只保存 {@code id}，真正的电路和自定义元件元数据保存在存档
 * {@code data/elements-plus/circuits/<uuid>.dat} 中。
 */
public record StoredCircuit(UUID id,
                            CircuitDiagram diagram,
                            @Nullable ResourceLocation equivalentId,
                            @Nullable CustomInfo custom,
                            UUID author,
                            String authorName) {

    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<CircuitDiagram> DIAGRAM_CODEC = CircuitDiagram.CODEC;

    public record PinBinding(Direction side, int offset, CircuitComponent.PinType type,
                             int bitWidth, String label) {
        public static final Codec<PinBinding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CircuitDiagram.DIRECTION_CODEC.fieldOf("side").forGetter(PinBinding::side),
                Codec.INT.fieldOf("offset").forGetter(PinBinding::offset),
                Codec.STRING.xmap(CircuitComponent.PinType::valueOf, Enum::name).fieldOf("type").forGetter(PinBinding::type),
                Codec.INT.optionalFieldOf("bit_width", 1).forGetter(PinBinding::bitWidth),
                Codec.STRING.optionalFieldOf("label", "").forGetter(PinBinding::label)
        ).apply(instance, PinBinding::new));
    }

    public record CustomInfo(String name,
                             String description,
                             @Nullable ResourceLocation iconItem,
                             @Nullable String iconChar,
                             int color,
                             int width,
                             int height,
                             List<PinBinding> pins) {

        public static final Codec<CustomInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(CustomInfo::name),
                Codec.STRING.optionalFieldOf("description", "").forGetter(CustomInfo::description),
                ResourceLocation.CODEC.optionalFieldOf("icon_item").forGetter(info ->
                        Optional.ofNullable(info.iconItem())),
                Codec.STRING.optionalFieldOf("icon_char").forGetter(info ->
                        Optional.ofNullable(info.iconChar())),
                Codec.INT.optionalFieldOf("color", 0xFF808080).forGetter(CustomInfo::color),
                Codec.INT.optionalFieldOf("width", 1).forGetter(CustomInfo::width),
                Codec.INT.optionalFieldOf("height", 1).forGetter(CustomInfo::height),
                PinBinding.CODEC.listOf().optionalFieldOf("pins", List.of()).forGetter(CustomInfo::pins)
        ).apply(instance, (name, description, iconItem, iconChar, color, width, height, pins) ->
                new CustomInfo(name, description, iconItem.orElse(null), iconChar.orElse(null),
                        color, width, height, new ArrayList<>(pins))));
    }

    public static final Codec<StoredCircuit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUID_CODEC.fieldOf("id").forGetter(StoredCircuit::id),
            CircuitDiagram.CODEC.fieldOf("diagram").forGetter(StoredCircuit::diagram),
            ResourceLocation.CODEC.optionalFieldOf("equivalent").forGetter(circuit ->
                    Optional.ofNullable(circuit.equivalentId())),
            CustomInfo.CODEC.optionalFieldOf("custom").forGetter(circuit ->
                    Optional.ofNullable(circuit.custom())),
            UUID_CODEC.fieldOf("author").forGetter(StoredCircuit::author),
            Codec.STRING.optionalFieldOf("author_name", "").forGetter(StoredCircuit::authorName)
    ).apply(instance, (id, diagram, equivalent, custom, author, authorName) ->
            new StoredCircuit(id, diagram, equivalent.orElse(null), custom.orElse(null), author,
                    authorName.isBlank() ? author.toString() : authorName)));

    public StoredCircuit(UUID id, CircuitDiagram diagram, @Nullable ResourceLocation equivalentId,
                         @Nullable CustomInfo custom, UUID author) {
        this(id, diagram, equivalentId, custom, author, author.toString());
    }

    public String displayAuthor() {
        return authorName == null || authorName.isBlank() ? author.toString() : authorName;
    }

    public boolean isCustom() {
        return custom != null;
    }

    /** 自定义元件在元件注册表中的 id。 */
    public ResourceLocation componentId() {
        if (custom != null) {
            return ResourceLocation.fromNamespaceAndPath("elements-plus", "custom/" + id);
        }
        return equivalentId;
    }

    /** 命令中用于匹配的名称：内置元件使用 id/path，自定义元件使用玩家填写的名称。 */
    public String lookupName() {
        if (custom != null) {
            return custom.name();
        }
        if (equivalentId == null) {
            return id.toString();
        }
        return equivalentId.getPath();
    }

    public boolean matchesName(String name) {
        if (custom != null) {
            return custom.name().equals(name);
        }
        return name.equals(lookupName()) || name.equals(equivalentId == null ? "" : equivalentId.toString());
    }
}
