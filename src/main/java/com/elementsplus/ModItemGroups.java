package com.elementsplus;

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
                ModBlocks.STEEL_PIPE_L,
                ModBlocks.STEEL_PIPE_I,
                ModBlocks.STEEL_PIPE_T,
                ModBlocks.STEEL_PIPE_X,
                ModItems.SYRINGE,
                ModBlocks.CRYSTALLIZER
                // 可以继续添加更多物品
        );
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
    private record TabConfig(String tabId, ResourceKey<CreativeModeTab> tabKey, ItemLike[] items) {
    }
}