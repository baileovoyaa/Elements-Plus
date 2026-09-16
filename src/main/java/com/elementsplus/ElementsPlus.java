package com.elementsplus;

import com.elementsplus.blocks.entity.MetalCatalystBlockEntity;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.core.dispenser.MyCustomBottleBehavior;
import com.elementsplus.menu.LithographyMachineMenu;
import com.elementsplus.network.ReturnCarriedPayload;
import com.elementsplus.network.ToolboxRequestPayload;
import com.elementsplus.network.ToolboxSyncPayload;
import com.elementsplus.network.ToolboxUpdatePayload;
import com.elementsplus.network.UpdateCircuitDiagramPayload;
import com.elementsplus.player.PlayerToolboxAttachment;
import com.elementsplus.recipe.CrystallizerRecipe;
import com.elementsplus.recipe.MetalCatalystRecipe;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class ElementsPlus implements ModInitializer {
    public static final String MOD_ID = "elements-plus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModDataComponents.initialize();
        ModItems.initialize();
        ModItemGroups.registerAll();
        ModBlocks.initialize();
        ModEffects.initialize();
        ModBlockEntityTypes.initialize();
        MetalCatalystRecipe.TYPE.toString();
        CrystallizerRecipe.TYPE.toString();

        PayloadTypeRegistry.playC2S().register(ReturnCarriedPayload.TYPE, ReturnCarriedPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ReturnCarriedPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().containerMenu instanceof LithographyMachineMenu menu) {
                        menu.returnCarriedToInventory();
                    }
                }));

        PayloadTypeRegistry.playC2S().register(UpdateCircuitDiagramPayload.TYPE, UpdateCircuitDiagramPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(UpdateCircuitDiagramPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().containerMenu instanceof LithographyMachineMenu menu) {
                        ItemStack stack = menu.slots.get(36).getItem();
                        if (stack.get(ModDataComponents.CIRCUIT_DIAGRAM) != null) {
                            stack.set(ModDataComponents.CIRCUIT_DIAGRAM, payload.diagram());
                            menu.onDiagramChanged();
                        }
                    }
                }));

        PayloadTypeRegistry.playC2S().register(ToolboxRequestPayload.TYPE, ToolboxRequestPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ToolboxRequestPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayNetworking.send(context.player(),
                            new ToolboxSyncPayload(PlayerToolboxAttachment.get(context.player())));
                }));

        PayloadTypeRegistry.playS2C().register(ToolboxSyncPayload.TYPE, ToolboxSyncPayload.STREAM_CODEC);

        PayloadTypeRegistry.playC2S().register(ToolboxUpdatePayload.TYPE, ToolboxUpdatePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ToolboxUpdatePayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    payload.toolbox().sanitize();
                    context.player().setAttached(PlayerToolboxAttachment.PLAYER_TOOLBOX, payload.toolbox());
                    ServerPlayNetworking.send(context.player(), new ToolboxSyncPayload(payload.toolbox()));
                }));

        LOGGER.info("Hello Fabric world!");

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide) {
                if (player.getItemInHand(hand).is(Items.GLASS_BOTTLE) && hitResult.getType() == HitResult.Type.BLOCK && world.getBlockState(hitResult.getBlockPos()).is(ModBlocks.CHARGED_LIGHTNING_ROD)) {
                    world.setBlockAndUpdate(hitResult.getBlockPos(), Blocks.LIGHTNING_ROD.withPropertiesOf(world.getBlockState(hitResult.getBlockPos())));
                    ItemStack itemStack = player.getItemInHand(hand);
                    itemStack.shrink(1);
                    world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (itemStack.isEmpty()) {
                        player.setItemInHand(hand, new ItemStack(ModItems.LIGHTNING_BOTTLE));
                    } else if (!player.getInventory().add(new ItemStack(ModItems.LIGHTNING_BOTTLE))) {
                        player.drop(new ItemStack(ModItems.LIGHTNING_BOTTLE), false);
                    }
                    world.gameEvent(player, GameEvent.FLUID_PICKUP, hitResult.getBlockPos());
                    return InteractionResult.CONSUME;
                }
                if (player.getItemInHand(hand).is(Items.GLASS_BOTTLE) && hitResult.getType() == HitResult.Type.BLOCK && world.getBlockState(hitResult.getBlockPos()).is(ModBlocks.METAL_CATALYST)) {
                    BlockEntity blockEntity = world.getBlockEntity(hitResult.getBlockPos());
                    if (blockEntity instanceof MetalCatalystBlockEntity metalCatalyst && metalCatalyst.getWaste() >= 50) {
                        ItemStack itemStack = player.getItemInHand(hand);
                        itemStack.shrink(1);
                        world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                        if (itemStack.isEmpty()) {
                            player.setItemInHand(hand, new ItemStack(ModItems.WASTE_BOTTLE));
                        } else if (!player.getInventory().add(new ItemStack(ModItems.WASTE_BOTTLE))) {
                            player.drop(new ItemStack(ModItems.WASTE_BOTTLE), false);
                        }
                        metalCatalyst.extractWaste(50);
                        world.gameEvent(player, GameEvent.FLUID_PICKUP, hitResult.getBlockPos());
                        return InteractionResult.CONSUME;
                    }
                }
                if (player.getItemInHand(hand).is(Items.HONEYCOMB) && hitResult.getType() == HitResult.Type.BLOCK) {
                    ServerPlayer serverPlayer = (ServerPlayer) player;
                    BlockState blockState = world.getBlockState(hitResult.getBlockPos());
                    if (blockState.is(ModBlocks.STEEL_PIPE_L) || blockState.is(ModBlocks.STEEL_PIPE_I) || blockState.is(ModBlocks.STEEL_PIPE_T) || blockState.is(ModBlocks.STEEL_PIPE_X)) {
                        MinecraftServer server = world.getServer();
                        if (server != null) {
                            AdvancementHolder advancementHolder = server.getAdvancements().get(ResourceLocation.withDefaultNamespace("husbandry/wax_on"));
                            serverPlayer.getAdvancements().award(advancementHolder, "wax_on");
                        }
                    }
                }
                if (player.getItemInHand(hand).getItem() instanceof AxeItem && hitResult.getType() == HitResult.Type.BLOCK) {
                    ServerPlayer serverPlayer = (ServerPlayer) player;
                    BlockState blockState = world.getBlockState(hitResult.getBlockPos());
                    if (blockState.is(ModBlocks.WAXED_STEEL_PIPE_L) || blockState.is(ModBlocks.WAXED_STEEL_PIPE_I) || blockState.is(ModBlocks.WAXED_STEEL_PIPE_T) || blockState.is(ModBlocks.WAXED_STEEL_PIPE_X)) {
                        MinecraftServer server = world.getServer();
                        if (server != null) {
                            AdvancementHolder advancementHolder = server.getAdvancements().get(ResourceLocation.withDefaultNamespace("husbandry/wax_off"));
                            serverPlayer.getAdvancements().award(advancementHolder, "wax_off");
                        }
                    }
                }
            }
            return InteractionResult.PASS;
        });

        DispenserBlock.registerBehavior(Items.GLASS_BOTTLE, new MyCustomBottleBehavior());

        OxidizableBlocksRegistry.registerWaxableBlockPair(ModBlocks.STEEL_PIPE_L, ModBlocks.WAXED_STEEL_PIPE_L);
        OxidizableBlocksRegistry.registerWaxableBlockPair(ModBlocks.STEEL_PIPE_I, ModBlocks.WAXED_STEEL_PIPE_I);
        OxidizableBlocksRegistry.registerWaxableBlockPair(ModBlocks.STEEL_PIPE_T, ModBlocks.WAXED_STEEL_PIPE_T);
        OxidizableBlocksRegistry.registerWaxableBlockPair(ModBlocks.STEEL_PIPE_X, ModBlocks.WAXED_STEEL_PIPE_X);
        OxidizableBlocksRegistry.registerOxidizableBlockPair(ModBlocks.STEEL_PIPE_I, ModBlocks.RUST_STEEL_PIPE_I);
        OxidizableBlocksRegistry.registerOxidizableBlockPair(ModBlocks.STEEL_PIPE_L, ModBlocks.RUST_STEEL_PIPE_L);
        OxidizableBlocksRegistry.registerOxidizableBlockPair(ModBlocks.STEEL_PIPE_T, ModBlocks.RUST_STEEL_PIPE_T);
        OxidizableBlocksRegistry.registerOxidizableBlockPair(ModBlocks.STEEL_PIPE_X, ModBlocks.RUST_STEEL_PIPE_X);

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(), // 1. 选择生物群系（如主世界）
                GenerationStep.Decoration.UNDERGROUND_ORES, // 2. 生成阶段（地下矿石）
                ResourceKey.create(Registries.PLACED_FEATURE, id("silver_ore")) // 3. 你的放置地物ID
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("elements-plus").requires((source) -> source.hasPermission(2)).then(Commands.literal("debug").executes((source) -> {
                doSomething(source);
                return 1;
            })));
        });
    }

    public static void doSomething(CommandContext<CommandSourceStack> source) {
        try {
            source.getSource().sendSuccess(() -> Component.nullToEmpty(CircuitDiagram.CODEC.encode(CircuitDiagram.EXAMPLE, source.getSource().getServer().registryAccess().createSerializationContext(NbtOps.INSTANCE), new CompoundTag()).toString()), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
