package com.elementsplus;

import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModItemGroups {

    private static final List<TabConfig> TAB_CONFIGS = new ArrayList<>();

    public static void registerAll() {
        registerTab(
                "steel_pipe",
                () -> new ItemStack(ModItems.STEEL_PIPE),
                "itemGroup.elements-plus.steel_pipe",
                ModItems.STEEL_PIPE,
                ModItems.SYRINGE,
                ModItems.WRENCH,
                ModBlocks.STEEL_PIPE_L,
                ModBlocks.STEEL_PIPE_I,
                ModBlocks.STEEL_PIPE_T,
                ModBlocks.STEEL_PIPE_X,
                ModBlocks.WAXED_STEEL_PIPE_L,
                ModBlocks.WAXED_STEEL_PIPE_I,
                ModBlocks.WAXED_STEEL_PIPE_T,
                ModBlocks.WAXED_STEEL_PIPE_X,
                ModBlocks.RUST_STEEL_PIPE_L,
                ModBlocks.RUST_STEEL_PIPE_I,
                ModBlocks.RUST_STEEL_PIPE_T,
                ModBlocks.RUST_STEEL_PIPE_X,
                ModBlocks.SILVER_PIPE_L,
                ModBlocks.SILVER_PIPE_I,
                ModBlocks.SILVER_PIPE_T,
                ModBlocks.SILVER_PIPE_X,
                ModItems.SILVER_INGOT,
                ModBlocks.SILVER_ORE,
                ModBlocks.SILVER_BLOCK,
                ModBlocks.CHARGED_LIGHTNING_ROD,
                ModBlocks.ADVANCED_CRAFTING_TABLE,
                ModBlocks.CRYSTALLIZER,
                ModBlocks.METAL_CATALYST,
                ModItems.PLASTIC,
                ModItems.AMETHYST_LENS,
                ModItems.COPPER_WIRE,
                ModItems.GOLD_WIRE,
                ModItems.HIGH_VOLTAGE_COIL,
                ModItems.CATALYST,
                ModItems.LIGHTNING_BOTTLE,
                ModItems.WASTE_BOTTLE
                // 可以继续添加更多物品
        );
        registerTab(
                "circuit",
                () -> new ItemStack(Items.AMETHYST_SHARD),
                "itemGroup.elements-plus.circuit",
                ModBlocks.LITHOGRAPHY_MACHINE,
                ModBlocks.EXPERIMENT_TABLE,
                ModItems.AMETHYST_TRANSISTOR,
                ModItems.AMETHYST_DIODE,
                ModItems.AMETHYST_CAPACITOR,
                ModItems.AMETHYST_RESISTOR,
                ModItems.AMETHYST_RESONATOR,
                ModItems.AMETHYST_BATTERY,
                ModItems.GOLD_WIRE,
                ModItems.COPPER_WIRE,
                ModItems.CIRCUIT_BOARD,
                ModItems.CIRCUIT_DIAGRAM,
                ModItems.SMALL_EMPTY_CHIP,
                ModItems.MEDIUM_EMPTY_CHIP,
                ModItems.LARGE_EMPTY_CHIP,
                ModItems.HUGE_EMPTY_CHIP,
                ModItems.EMPTY_WAFER,
                ModItems.WAFER_N,
                ModItems.WAFER_P,
                ModItems.WAFER_PN,
                ModItems.ETCHED_WAFER,
                ModItems.METALLIZED_WAFER,
                ModItems.LITHOGRAPHY_CORE,
                ModItems.EXPOSURE_ROOM,
                ModItems.DEVELOPMENT_TANK,
                ModItems.ION_IMPLANTER,
                ModItems.DOPED_REDSTONE_DUST,
                ModItems.DOPED_GLOWSTONE_DUST,
                ModItems.HEAT_SINK_SUBSTRATE,
                ModItems.INSULATING_LAYER,
                ModItems.NETHERITE_FRAGMENT,
                ModItems.PHOTORESIST,
                ModItems.LITHOGRAPHY_MASK,
                ModItems.COMPUTER,
                ModItems.AND_GATE,
                ModItems.OR_GATE,
                ModItems.NOT_GATE,
                ModItems.ADDER,
                ModItems.BITWISE_MOVE,
                ModItems.MULTIPLIER,
                ModItems.REGISTER,
                ModItems.COUNTER
                // 可以继续添加更多物品
        );
        // 无限规模电路图不能合成，仅在创造模式物品栏中提供
        ItemStack infiniteDiagram = new ItemStack(ModItems.CIRCUIT_DIAGRAM);
        CircuitDiagram infinite = new CircuitDiagram();
        infinite.scale = CircuitDiagram.Scale.INFINITE;
        infiniteDiagram.set(ModDataComponents.CIRCUIT_DIAGRAM, infinite);
        addItemStacksToTab("circuit", infiniteDiagram);
        ElementsPlus.LOGGER.info("Registered {} creative tab(s)", TAB_CONFIGS.size());
    }

    private static void registerTab(String tabId, Supplier<ItemStack> icon,
                                    String translationKey, ItemLike... items) {
        ResourceKey<CreativeModeTab> tabKey = ResourceKey.create(
                BuiltInRegistries.CREATIVE_MODE_TAB.key(),
                ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, tabId)
        );
        CreativeModeTab tab = FabricItemGroup.builder()
                .icon(icon)
                .title(Component.translatable(translationKey))
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tabKey, tab);
        ItemGroupEvents.modifyEntriesEvent(tabKey).register(itemGroup -> {
            for (ItemLike item : items) {
                if (item != null) {
                    itemGroup.accept(item);
                }
            }
        });
        TAB_CONFIGS.add(new TabConfig(tabId, tabKey, items));
        ElementsPlus.LOGGER.debug("Registered tab: {}", tabId);
    }

    public static ResourceKey<CreativeModeTab> getTabKey(String tabId) {
        return ResourceKey.create(
                BuiltInRegistries.CREATIVE_MODE_TAB.key(),
                ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, tabId)
        );
    }

    public static void addItemsToTab(String tabId, Item... items) {
        ResourceKey<CreativeModeTab> tabKey = getTabKey(tabId);
        ItemGroupEvents.modifyEntriesEvent(tabKey).register(itemGroup -> {
            for (Item item : items) {
                if (item != null) {
                    itemGroup.accept(item);
                }
            }
        });
        ElementsPlus.LOGGER.debug("Added {} item(s) to tab: {}", items.length, tabId);
    }

    public static void addItemStacksToTab(String tabId, ItemStack... stacks) {
        ResourceKey<CreativeModeTab> tabKey = getTabKey(tabId);
        ItemGroupEvents.modifyEntriesEvent(tabKey).register(itemGroup -> {
            for (ItemStack stack : stacks) {
                if (stack != null) {
                    itemGroup.accept(stack);
                }
            }
        });
        ElementsPlus.LOGGER.debug("Added {} item stack(s) to tab: {}", stacks.length, tabId);
    }

    private record TabConfig(String tabId, ResourceKey<CreativeModeTab> tabKey, ItemLike[] items) {
    }
}