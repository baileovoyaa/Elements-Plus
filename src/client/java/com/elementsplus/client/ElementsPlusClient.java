package com.elementsplus.client;

import com.elementsplus.ModBlocks;
import com.elementsplus.ModDataComponents;
import com.elementsplus.ModItems;
import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.circuit.CircuitComponentToolbox;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class ElementsPlusClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        ModMenuScreens.initialize();
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CHARGED_LIGHTNING_ROD, RenderType.cutout());

        ItemProperties.register(
                ModItems.CIRCUIT_DIAGRAM,
                ResourceLocation.withDefaultNamespace("element-plus.circuit_diagram"),
                (itemStack, clientWorld, livingEntity, seed) -> {
                    var equivalentComponent = itemStack.getComponentsPatch().get(ModDataComponents.EQUIVALENT_COMPONENT);
                    if (equivalentComponent != null && equivalentComponent.isPresent()) {
                        return 1.0f;
                    }
                    var circuitDiagramPatch = itemStack.getComponentsPatch().get(ModDataComponents.CIRCUIT_DIAGRAM);
                    if (circuitDiagramPatch != null && circuitDiagramPatch.isPresent()) {
                        return 0.5f;
                    }
                    return 0.0F;
                }
        );
    }

    public static CircuitComponentToolbox getToolbox() {
        return BuiltinCircuitComponents.EXAMPLE_TOOLBOX;
    }
}