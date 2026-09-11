package com.elementsplus;

import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public class ModDataComponents {

    public static final DataComponentType<CircuitDiagram> CIRCUIT_DIAGRAM = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ElementsPlus.id("circuit_diagram"),
            DataComponentType.<CircuitDiagram>builder()
                    .persistent(CircuitDiagram.CODEC)
                    .networkSynchronized(CircuitDiagram.STREAM_CODEC)
                    .build()
    );

    public static void initialize() {
    }
}