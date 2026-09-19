package com.elementsplus.client;

import com.elementsplus.ModBlocks;
import com.elementsplus.ModDataComponents;
import com.elementsplus.ModItems;
import com.elementsplus.client.config.ClientConfig;
import com.elementsplus.client.gui.CircuitDiagramPanel;
import com.elementsplus.client.screen.ExperimentTableScreen;
import com.elementsplus.client.screen.LithographyMachineScreen;
import com.elementsplus.network.ExperimentTableChapterChangePayload;
import com.elementsplus.network.ExperimentTableScreenDataPayload;
import com.elementsplus.network.ToolboxRequestPayload;
import com.elementsplus.network.ToolboxSyncPayload;
import com.elementsplus.player.PlayerToolbox;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

public class ElementsPlusClient implements ClientModInitializer {

    private static PlayerToolbox toolbox;

    @Override
    public void onInitializeClient() {
        ClientConfig.load();
        CircuitDiagramPanel.colorBackground = ClientConfig.get().darkMode ? 0xFF2B2B28 : 0xFFE0E0E0;
        CircuitDiagramPanel.colorDot = ClientConfig.get().darkMode ? 0xFF4A4A44 : 0xFFA0A0A0;
        CircuitDiagramPanel.sound = !ClientConfig.get().muted;

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

        ClientPlayNetworking.registerGlobalReceiver(ToolboxSyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> applyToolbox(payload.toolbox())));

        ClientPlayNetworking.registerGlobalReceiver(ExperimentTableScreenDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (Minecraft.getInstance().screen instanceof ExperimentTableScreen screen) {
                        screen.onServerData(payload.pos(), payload.selectedChapter(), payload.unlockedChapters());
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(ExperimentTableChapterChangePayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (Minecraft.getInstance().screen instanceof ExperimentTableScreen screen) {
                        screen.onChapterChanged(payload.pos(), payload.chapterName());
                    }
                }));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                ClientPlayNetworking.send(new ToolboxRequestPayload()));
    }

    /** 客户端缓存的元件列表。未收到服务端数据时显示默认工具箱。 */
    public static PlayerToolbox getToolbox() {
        PlayerToolbox current = toolbox;
        if (current == null) {
            current = PlayerToolbox.createDefault();
            toolbox = current;
        }
        return current;
    }

    /** 收到服务端同步时更新缓存；若光刻机界面已打开则通知其重建列表。 */
    public static void applyToolbox(PlayerToolbox toolbox) {
        PlayerToolbox clean = toolbox != null ? toolbox : PlayerToolbox.createDefault();
        PlayerToolbox installed = ElementsPlusClient.toolbox;
        ElementsPlusClient.toolbox = clean;
        if (installed != clean && net.minecraft.client.Minecraft.getInstance().screen instanceof LithographyMachineScreen screen) {
            screen.onToolboxSynced(clean);
        }
    }
}