package com.elementsplus.client;

import com.elementsplus.ModBlocks;
import com.elementsplus.ModDataComponents;
import com.elementsplus.ModEntities;
import com.elementsplus.ModItems;
import com.elementsplus.client.config.ClientConfig;
import com.elementsplus.client.gui.CircuitDiagramPanel;
import com.elementsplus.client.screen.ExperimentTableScreen;
import com.elementsplus.client.screen.LithographyMachineScreen;
import com.elementsplus.core.circuit.storage.CircuitStorage;
import com.elementsplus.entity.SyringeProjectile;
import com.elementsplus.item.SyringeItem;
import com.elementsplus.network.AdvancementActionPayload;
import com.elementsplus.network.CircuitStorageSyncPayload;
import com.elementsplus.core.config.ElementsPlusServerConfig;
import com.elementsplus.network.ExperimentTableScreenDataPayload;
import com.elementsplus.network.ServerConfigSyncPayload;
import com.elementsplus.network.ExperimentTableSelectionChangePayload;
import com.elementsplus.network.ExperimentTableStatusPayload;
import com.elementsplus.network.SyringeChargePayload;
import com.elementsplus.network.ToolboxRequestPayload;
import com.elementsplus.network.ToolboxSyncPayload;
import com.elementsplus.player.PlayerToolbox;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public class ElementsPlusClient implements ClientModInitializer {

    private static PlayerToolbox toolbox;

    /** 本次连接中已经发送过的进度条件，避免拖动操作重复发包。 */
    private static final Set<String> SENT_ADVANCEMENT_CRITERIA = new HashSet<>();

    /** 上一次同步给服务端的注射器蓄力状态。 */
    private static boolean lastSyringeCharging;

    /** 按键绑定分类（在「选项 → 按键控制」中显示）。 */
    public static final String KEY_CATEGORY = "key.categories.elements-plus";

    /** 水平镜像元件（默认为 M）。 */
    public static KeyMapping keyMirrorHorizontal;
    /** 垂直镜像元件（默认为 N）。 */
    public static KeyMapping keyMirrorVertical;

    @Override
    public void onInitializeClient() {
        ClientConfig.load();
        CircuitDiagramPanel.colorBackground = ClientConfig.get().darkMode ? 0xFF2B2B28 : 0xFFE0E0E0;
        CircuitDiagramPanel.colorDot = ClientConfig.get().darkMode ? 0xFF4A4A44 : 0xFFA0A0A0;
        CircuitDiagramPanel.sound = !ClientConfig.get().muted;

        keyMirrorHorizontal = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.elements-plus.mirror_horizontal", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, KEY_CATEGORY));
        keyMirrorVertical = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.elements-plus.mirror_vertical", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, KEY_CATEGORY));

        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        ModMenuScreens.initialize();
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CHARGED_LIGHTNING_ROD, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CRYSTALLIZER, RenderType.translucent());
        EntityRendererRegistry.register(ModEntities.SYRINGE_PROJECTILE,
                (context) -> new ThrownItemRenderer<SyringeProjectile>(context));

        ItemProperties.register(
                ModItems.CIRCUIT_DIAGRAM,
                ResourceLocation.withDefaultNamespace("element-plus.circuit_diagram"),
                (itemStack, clientWorld, livingEntity, seed) -> {
                    var equivalentComponent = itemStack.getComponentsPatch().get(ModDataComponents.EQUIVALENT_COMPONENT);
                    if (equivalentComponent != null && equivalentComponent.isPresent()) {
                        return 1.0f;
                    }
                    var compiledPatch = itemStack.getComponentsPatch().get(ModDataComponents.COMPILED_CIRCUIT);
                    if (compiledPatch != null && compiledPatch.isPresent()) {
                        return 1.0f;
                    }
                    var circuitDiagramPatch = itemStack.getComponentsPatch().get(ModDataComponents.CIRCUIT_DIAGRAM);
                    if (circuitDiagramPatch != null && circuitDiagramPatch.isPresent()) {
                        return 0.5f;
                    }
                    return 0.0F;
                }
        );

        ItemProperties.register(
                ModItems.CREATIVE_COMPONENT,
                ResourceLocation.withDefaultNamespace("element-plus.creative_component"),
                (itemStack, clientWorld, livingEntity, seed) ->
                        itemStack.has(ModDataComponents.EQUIVALENT_COMPONENT) ? 1.0f : 0.0f
        );

        ClientPlayNetworking.registerGlobalReceiver(CircuitStorageSyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> CircuitStorage.get().replaceAll(payload.circuits())));

        ClientPlayNetworking.registerGlobalReceiver(ServerConfigSyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ElementsPlusServerConfig.applySynced(
                        payload.customComponentNestingLimit(), payload.allowInfiniteInFiniteCircuit(),
                        payload.asyncExperimentTests(), payload.allowSurvivalCreativeComponent())));

        ClientPlayNetworking.registerGlobalReceiver(ToolboxSyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> applyToolbox(payload.toolbox())));

        ClientPlayNetworking.registerGlobalReceiver(ExperimentTableScreenDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (Minecraft.getInstance().screen instanceof ExperimentTableScreen screen) {
                        screen.onServerData(payload.pos(), payload.selectedChapter(), payload.selectedExperiment(),
                                payload.unlockedChapters(), payload.completedExperiments(), payload.worldSeed());
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(ExperimentTableSelectionChangePayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (Minecraft.getInstance().screen instanceof ExperimentTableScreen screen) {
                        screen.onSelectionChanged(payload.pos(), payload.chapterName(), payload.experimentName());
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(ExperimentTableStatusPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (Minecraft.getInstance().screen instanceof ExperimentTableScreen screen) {
                        screen.onStatusUpdate(payload.pos(), payload.status(), payload.paused(), payload.experimentName(), payload.progress(), payload.testResults(), payload.tooltipLines(), payload.failureInfo(), payload.currentTestIndex(), payload.sequentialFailure());
                    }
                }));

        // 仅主手注射器接管左键：取消原版挥砍/挖掘，保持平举姿态。
        ClientPreAttackCallback.EVENT.register((client, player, clickCount) ->
                player.getMainHandItem().is(ModItems.SYRINGE));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SyringeTargeting.tick(client);
            boolean charging = client.player != null
                    && client.screen == null
                    && client.options.keyAttack.isDown()
                    && client.player.getMainHandItem().is(ModItems.SYRINGE);
            if (charging != lastSyringeCharging) {
                lastSyringeCharging = charging;
                if (client.player != null) {
                    if (charging) {
                        SyringeItem.startChargePose(client.player);
                    } else {
                        SyringeItem.stopChargePose(client.player);
                    }
                }
                if (ClientPlayNetworking.canSend(SyringeChargePayload.TYPE)) {
                    ClientPlayNetworking.send(new SyringeChargePayload(charging));
                }
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                ClientPlayNetworking.send(new ToolboxRequestPayload()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CircuitStorage.get().clear();
            SENT_ADVANCEMENT_CRITERIA.clear();
            lastSyringeCharging = false;
        });
    }

    /** 当前是否处于主手注射器左键蓄力姿态。 */
    public static boolean isSyringeCharging() {
        return lastSyringeCharging;
    }

    /** 上报一个由客户端操作触发的进度条件。 */
    public static void awardAdvancement(String advancement, String criterion) {
        if (!ClientPlayNetworking.canSend(AdvancementActionPayload.TYPE)) {
            return;
        }
        if (SENT_ADVANCEMENT_CRITERIA.add(advancement + ":" + criterion)) {
            ClientPlayNetworking.send(new AdvancementActionPayload(advancement, criterion));
        }
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