package com.elementsplus;

import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class ModDataComponents {

    public static final DataComponentType<CircuitDiagram> CIRCUIT_DIAGRAM = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ElementsPlus.id("circuit_diagram"),
            DataComponentType.<CircuitDiagram>builder()
                    .persistent(CircuitDiagram.CODEC)
                    .networkSynchronized(CircuitDiagram.STREAM_CODEC)
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