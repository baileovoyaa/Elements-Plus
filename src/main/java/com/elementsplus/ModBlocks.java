package com.elementsplus;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.elementsplus.ModEffects.STEEL_PIPE_SHIELD;

public class ModBlocks {

    public static Item register(Item item, String id) {
        ResourceLocation itemID = ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, id);
        return Registry.register(BuiltInRegistries.ITEM, itemID, item);
    }
    public static final Item STEEL_PIPE_L = register(
            new Item(new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                    tooltip.add(Component.translatable("tooltip.elements-plus.steel_pipe_l")
                            .withStyle(style -> style.withColor(0xAAAAAA))); // 灰色
                }
            },
            "steel_pipe_l"
    );
    public static void initialize() {
        // 物品已在静态块中注册
        ElementsPlus.LOGGER.info("Registered items");
    }
}