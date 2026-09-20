package com.elementsplus;

import com.elementsplus.blocks.entity.ExperimentTableBlockEntity;
import com.elementsplus.blocks.entity.MetalCatalystBlockEntity;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.core.dispenser.MyCustomBottleBehavior;
import com.elementsplus.core.experiment.BaseExperiment;
import com.elementsplus.core.experiment.BuiltinExperimentChapters;
import com.elementsplus.core.experiment.BuiltinExperiments;
import com.elementsplus.core.experiment.ExperimentChapter;
import com.elementsplus.menu.ExperimentTableMenu;
import com.elementsplus.menu.LithographyMachineMenu;
import com.elementsplus.network.ExperimentTableDataRequestPayload;
import com.elementsplus.network.ExperimentTableScreenDataPayload;
import com.elementsplus.network.ExperimentTableSelectionChangePayload;
import com.elementsplus.network.ExperimentTableSelectionUpdatePayload;
import com.elementsplus.network.ReturnCarriedPayload;
import com.elementsplus.network.ToolboxRequestPayload;
import com.elementsplus.network.ToolboxSyncPayload;
import com.elementsplus.network.ToolboxUpdatePayload;
import com.elementsplus.network.UpdateCircuitDiagramPayload;
import com.elementsplus.player.PlayerExperimentsAttachment;
import com.elementsplus.player.PlayerToolboxAttachment;
import com.elementsplus.recipe.CrystallizerRecipe;
import com.elementsplus.recipe.MetalCatalystRecipe;
import com.elementsplus.recipe.ScaleUpgradeRecipe;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
        ModMenuTypes.initialize();
        MetalCatalystRecipe.TYPE.toString();
        CrystallizerRecipe.TYPE.toString();
        ScaleUpgradeRecipe.TYPE_ID.toString();

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

        PayloadTypeRegistry.playC2S().register(ExperimentTableDataRequestPayload.TYPE, ExperimentTableDataRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ExperimentTableSelectionUpdatePayload.TYPE, ExperimentTableSelectionUpdatePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ExperimentTableScreenDataPayload.TYPE, ExperimentTableScreenDataPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ExperimentTableSelectionChangePayload.TYPE, ExperimentTableSelectionChangePayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ExperimentTableDataRequestPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    BlockPos pos = ExperimentTableMenu.getOpenTable(player.getUUID());
                    if (pos == null || !(player.level().getBlockEntity(pos) instanceof ExperimentTableBlockEntity table)) {
                        return;
                    }
                    Set<String> unlocked = new HashSet<>();
                    for (ExperimentChapter chapter : BuiltinExperimentChapters.getUnlockedByName(PlayerExperimentsAttachment.get(player))) {
                        unlocked.add(chapter.name);
                    }
                    ServerPlayNetworking.send(player, new ExperimentTableScreenDataPayload(pos, table.getSelectedChapter(), table.getSelectedExperiment(), unlocked, PlayerExperimentsAttachment.get(player)));
                }));

        ServerPlayNetworking.registerGlobalReceiver(ExperimentTableSelectionUpdatePayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    BlockPos pos = payload.pos();
                    if (!(player.level().getBlockEntity(pos) instanceof ExperimentTableBlockEntity table)) {
                        return;
                    }
                    Set<ExperimentChapter> unlocked = BuiltinExperimentChapters.getUnlockedByName(PlayerExperimentsAttachment.get(player));
                    boolean changed = false;
                    String chapterName = payload.chapterName();
                    if (chapterName != null) {
                        ExperimentChapter chapter = BuiltinExperimentChapters.byName(chapterName);
                        if (chapter == null || !unlocked.contains(chapter)) {
                            return;
                        }
                        table.setSelectedChapter(chapter.name);
                        changed = true;
                    }
                    String experimentName = payload.experimentName();
                    if (experimentName != null) {
                        BaseExperiment experiment = BuiltinExperiments.byId(experimentName);
                        if (experiment == null) {
                            return;
                        }
                        boolean inUnlockedChapter = false;
                        for (ExperimentChapter chapter : unlocked) {
                            if (chapter.containsExperiment(experiment)) {
                                inUnlockedChapter = true;
                                break;
                            }
                        }
                        if (!inUnlockedChapter) {
                            return;
                        }
                        table.setSelectedExperiment(experiment.getName());
                        changed = true;
                    }
                    if (!changed) {
                        return;
                    }
                    for (ServerPlayer tracking : PlayerLookup.tracking(table)) {
                        ServerPlayNetworking.send(tracking, new ExperimentTableSelectionChangePayload(pos, table.getSelectedChapter(), table.getSelectedExperiment()));
                    }
                }));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ExperimentTableMenu.closeTable(handler.player.getUUID()));

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
            dispatcher.register(Commands.literal("elements-plus").requires((source) -> source.hasPermission(2))
                    .then(Commands.literal("debug").executes((source) -> {
                        doSomething(source);
                        return 1;
                    }))
                    .then(Commands.literal("experiments")
                            .then(Commands.literal("grant")
                                    .then(Commands.argument("players", EntityArgument.players())
                                            .then(Commands.argument("experiment", StringArgumentType.greedyString())
                                                    .suggests(ElementsPlus::suggestExperiments)
                                                    .executes((source) -> setExperiments(source, true)))))
                            .then(Commands.literal("revoke")
                                    .then(Commands.argument("players", EntityArgument.players())
                                            .then(Commands.argument("experiment", StringArgumentType.greedyString())
                                                    .suggests(ElementsPlus::suggestExperiments)
                                                    .executes((source) -> setExperiments(source, false)))))
                            .then(Commands.literal("list")
                                    .then(Commands.argument("player", EntityArgument.player())
                                            .executes(ElementsPlus::listExperiments)))));
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

    private static int setExperiments(CommandContext<CommandSourceStack> source, boolean grant) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(source, "players");
        String raw = StringArgumentType.getString(source, "experiment");

        List<String> ids;
        if ("*".equals(raw)) {
            ids = new ArrayList<>();
            for (BaseExperiment experiment : BuiltinExperiments.BUILTIN_EXPERIMENTS) {
                if (experiment.getName() != null) {
                    ids.add(experiment.getName());
                }
            }
        } else {
            if (BuiltinExperiments.byId(raw) == null) {
                source.getSource().sendFailure(Component.translatable("command.elements-plus.experiments.unknown", raw));
                return 0;
            }
            ids = List.of(raw);
        }

        if (ids.isEmpty()) {
            source.getSource().sendFailure(Component.translatable("command.elements-plus.experiments.none"));
            return 0;
        }

        for (ServerPlayer player : targets) {
            Set<String> completed = PlayerExperimentsAttachment.get(player);
            if (grant) {
                completed.addAll(ids);
            } else {
                completed.removeAll(ids);
            }
        }

        source.getSource().sendSuccess(() -> Component.translatable(
                grant ? "command.elements-plus.experiments.granted" : "command.elements-plus.experiments.revoked",
                targets.size(), ids.size()), true);
        return 1;
    }

    private static int listExperiments(CommandContext<CommandSourceStack> source) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(source, "player");
        Set<String> completed = PlayerExperimentsAttachment.get(player);

        if (completed.isEmpty()) {
            source.getSource().sendSuccess(() -> Component.translatable("command.elements-plus.experiments.list_empty", player.getName()), false);
            return 0;
        }

        MutableComponent line = Component.empty();
        boolean first = true;
        for (String id : completed) {
            BaseExperiment experiment = BuiltinExperiments.byId(id);
            Component name = experiment != null ? experiment.getDisplayName() : Component.literal(id);
            if (!first) {
                line.append(Component.literal(", "));
            }
            line.append(name);
            first = false;
        }
        source.getSource().sendSuccess(() -> Component.translatable("command.elements-plus.experiments.list",
                player.getName(), line), false);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestExperiments(CommandContext<CommandSourceStack> source, SuggestionsBuilder builder) {
        builder.suggest("*");
        for (BaseExperiment experiment : BuiltinExperiments.BUILTIN_EXPERIMENTS) {
            if (experiment.getName() != null) {
                builder.suggest(experiment.getName(), experiment.getDisplayName());
            }
        }
        return builder.buildFuture();
    }
}
