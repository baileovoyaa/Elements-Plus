package com.elementsplus.client;

import com.elementsplus.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

public class ElementsPlusClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ModMenuScreens.initialize();
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CHARGED_LIGHTNING_ROD, RenderType.cutout());
	}
}