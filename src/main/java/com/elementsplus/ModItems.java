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

import static com.elementsplus.ModEffects.*;

public class ModItems {

    /**
     * 注册物品
     */
    public static Item register(Item item, String id) {
        ResourceLocation itemID = ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, id);
        return Registry.register(BuiltInRegistries.ITEM, itemID, item);
    }

    public static final Item STEEL_PIPE = register(
            new Item(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(8)
                            .saturationModifier(0.3f)
                            .alwaysEdible()
                            .build()
                    )
            ) {
                @Override
                public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                    tooltip.add(Component.translatable("tooltip.elements-plus.steel_pipe")
                            .withStyle(style -> style.withColor(0xFFD700))); // 金色
                }

                @Override
                public @NotNull ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity) {
                    ItemStack result = super.finishUsingItem(stack, level, entity);

                    if (!level.isClientSide() && entity instanceof net.minecraft.world.entity.player.Player player) {
                        // 👇 应用钢管护盾效果
                        player.addEffect(new MobEffectInstance(
                                STEEL_PIPE_SHIELD,  // 效果
                                600,                           // 持续 30 秒
                                0,                             // 等级 I
                                true,                          // 显示粒子
                                true,                          // 显示图标
                                true                           // 显示环境光效
                        ));
                    }
                    return result;
                }
            },
            "steel_pipe"

    );
    public static final Item SYRINGE = register(new Item(new Item.Properties()), "syringe");

    public static void initialize() {
        // 物品已在静态块中注册
    }
}