package com.elementsplus;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
                        MobEffectInstance existingEffect = player.getEffect(STEEL_PIPE_SHIELD);
                        if (existingEffect == null || existingEffect.getDuration() < 400) {
                            player.addEffect(new MobEffectInstance(
                                    STEEL_PIPE_SHIELD, 600, 0, true, true, true
                            ));
                        } else {
                            MobEffectInstance existingRustProof = player.getEffect(RUST_PROOF);
                            if (existingRustProof != null && existingRustProof.getDuration() > 400) {
                                int Duration = existingRustProof.getDuration();
                                player.addEffect(new MobEffectInstance(
                                        MobEffects.REGENERATION, 100, 1, false, false, false
                                ));
                                player.removeEffect(RUST_PROOF);
                                player.addEffect(new MobEffectInstance(
                                        RUST_PROOF, Duration - 400, 0, true, true, true
                                ));
                                player.addEffect(new MobEffectInstance(
                                        STEEL_PIPE_SHIELD, 600, 0, true, true, true
                                ));
                            } else {
                                player.addEffect(new MobEffectInstance(
                                        MobEffects.HARM, 2, 1, false, false, false
                                ));
                                player.addEffect(new MobEffectInstance(
                                        MobEffects.WITHER, 200, 0, false, false, false
                                ));
                                player.removeEffect(STEEL_PIPE_SHIELD);
                            }
                        }
                    }
                    return result;
                }
            },
            "steel_pipe"
    );
    public static final Item SYRINGE = register(
            new Item(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .alwaysEdible()
                            .build()
                    )
                    .stacksTo(16)
            ) {
                @Override
                public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                    tooltip.add(Component.translatable("tooltip.elements-plus.syringe")
                            .withStyle(style -> style.withColor(0xFFD700))); // 金色
                }
                @Override
                public @NotNull ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity) {
                    ItemStack result = super.finishUsingItem(stack, level, entity);
                    if (!level.isClientSide() && entity instanceof net.minecraft.world.entity.player.Player player) {
                        player.addEffect(new MobEffectInstance(
                                RUST_PROOF, 2400, 0, true, true, true
                        ));
                        player.getCooldowns().addCooldown(this, 400);
                    }
                    return result;
                }
            },
            "syringe"
    );

    public static void initialize() {
        // 物品已在静态块中注册
    }
}