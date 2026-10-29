package com.elementsplus;

import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class ModDataComponents {

    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    public static final StreamCodec<FriendlyByteBuf, UUID> UUID_STREAM_CODEC = StreamCodec.of(
            (buf, uuid) -> buf.writeUUID(uuid),
            buf -> buf.readUUID()
    );

    public static final DataComponentType<CircuitDiagram> CIRCUIT_DIAGRAM = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ElementsPlus.id("circuit_diagram"),
            DataComponentType.<CircuitDiagram>builder()
                    .persistent(CircuitDiagram.CODEC)
                    .networkSynchronized(CircuitDiagram.STREAM_CODEC)
                    .build()
    );

    /** 已编译电路在分离存储中的 UUID；物品不再直接保存电路本体。 */
    public static final DataComponentType<UUID> COMPILED_CIRCUIT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ElementsPlus.id("compiled_circuit"),
            DataComponentType.<UUID>builder()
                    .persistent(UUID_CODEC)
                    .networkSynchronized(UUID_STREAM_CODEC)
                    .build()
    );

    public static final DataComponentType<ResourceLocation> EQUIVALENT_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ElementsPlus.id("equivalent_component"),
            DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC.fieldOf("id").codec())
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build()
    );

    public static void initialize() {
    }
}