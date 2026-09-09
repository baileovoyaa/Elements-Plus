package com.elementsplus;

import com.elementsplus.item.WrenchItem;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.component.DebugStickState;
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
    public static final Item WRENCH = register(
            new WrenchItem(new Item.Properties()
                    .stacksTo(1)
                    .component(DataComponents.DEBUG_STICK_STATE, DebugStickState.EMPTY)
            ) {
                @Override
                public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                    tooltip.add(Component.translatable("tooltip.elements-plus.wrench")
                            .withStyle(style -> style.withColor(0xFFD700))); // 金色
                }
            },
            "wrench"
    );

    public static final Item AMETHYST_TRANSISTOR = register(
            new Item(new Item.Properties()
                    .stacksTo(64)
            ) {
                @Override
                public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                    tooltip.add(Component.translatable("tooltip.elements-plus.amethyst_transistor")
                            .withStyle(style -> style.withColor(0xAAAAAA))); // 灰色
                }
            },
            "amethyst_transistor"
    );

    public static final Item AMETHYST_DIODE = register(
            new Item(new Item.Properties()
                    .stacksTo(64)
            ) {
                @Override
                public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                    tooltip.add(Component.translatable("tooltip.elements-plus.amethyst_diode")
                            .withStyle(style -> style.withColor(0xAAAAAA))); // 灰色
                }
            },
            "amethyst_diode"
    );

    public static final Item AMETHYST_CAPACITOR = register(
            new Item(new Item.Properties()
                    .stacksTo(64)
            ) {
                @Override
                public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                    tooltip.add(Component.translatable("tooltip.elements-plus.amethyst_capacitor")
                            .withStyle(style -> style.withColor(0xAAAAAA))); // 灰色
                }
            },
            "amethyst_capacitor"
    );

    public static final Item AMETHYST_RESISTOR = register(
            new Item(new Item.Properties()
                    .stacksTo(64)
            ) {
                @Override
                public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                    tooltip.add(Component.translatable("tooltip.elements-plus.amethyst_resistor")
                            .withStyle(style -> style.withColor(0xAAAAAA))); // 灰色
                }
            },
            "amethyst_resistor"
    );

    public static final Item AMETHYST_RESONATOR = register(
            new Item(new Item.Properties()
                    .stacksTo(64)
            ) {
                @Override
                public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                    tooltip.add(Component.translatable("tooltip.elements-plus.amethyst_resonator")
                            .withStyle(style -> style.withColor(0xAAAAAA))); // 灰色
                }
            },
            "amethyst_resonator"
    );

    public static final Item AMETHYST_BATTERY = register(
            new Item(new Item.Properties()
                    .stacksTo(64)
            ) {
                @Override
                public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                    tooltip.add(Component.translatable("tooltip.elements-plus.amethyst_battery")
                            .withStyle(style -> style.withColor(0xAAAAAA))); // 灰色
                }
            },
            "amethyst_battery"
    );

    public static final Item SILVER_INGOT = register(
            new Item(new Item.Properties()
                    .stacksTo(64)
            ) {
                @Override
                public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                    tooltip.add(Component.translatable("tooltip.elements-plus.silver_ingot")
                            .withStyle(style -> style.withColor(0xFFD700))); // 金色
                }
            },
            "silver_ingot"
    );

    // 引线
    public static final Item GOLD_WIRE = register(new Item(new Item.Properties()), "gold_wire");
    public static final Item COPPER_WIRE = register(new Item(new Item.Properties()), "copper_wire");

    // 晶圆
    public static final Item EMPTY_WAFER = register(new Item(new Item.Properties()), "empty_wafer");
    public static final Item WAFER_N = register(new Item(new Item.Properties()), "wafer_n");
    public static final Item WAFER_P = register(new Item(new Item.Properties()), "wafer_p");
    public static final Item WAFER_PN = register(new Item(new Item.Properties()), "wafer_pn");
    public static final Item ETCHED_WAFER = register(new Item(new Item.Properties()), "etched_wafer");
    public static final Item METALLIZED_WAFER = register(new Item(new Item.Properties()), "metallized_wafer");

    // 光刻机部件
    public static final Item LITHOGRAPHY_CORE = register(new Item(new Item.Properties()), "lithography_core");
    public static final Item EXPOSURE_ROOM = register(new Item(new Item.Properties()), "exposure_room");
    public static final Item DEVELOPMENT_TANK = register(new Item(new Item.Properties()), "development_tank");
    public static final Item ION_IMPLANTER = register(new Item(new Item.Properties()), "ion_implanter");

    // 掺杂用品
    public static final Item DOPED_REDSTONE_DUST = register(new Item(new Item.Properties()), "doped_redstone_dust");
    public static final Item DOPED_GLOWSTONE_DUST = register(new Item(new Item.Properties()), "doped_glowstone_dust");

    // 电路载体
    public static final Item CIRCUIT_BOARD = register(new Item(new Item.Properties().stacksTo(1)), "circuit_board");
    public static final Item CIRCUIT_DIAGRAM = register(new Item(new Item.Properties().stacksTo(1)), "circuit_diagram");
    public static final Item SMALL_EMPTY_CHIP = register(new Item(new Item.Properties()), "small_empty_chip");
    public static final Item MEDIUM_EMPTY_CHIP = register(new Item(new Item.Properties()), "medium_empty_chip");
    public static final Item LARGE_EMPTY_CHIP = register(new Item(new Item.Properties()), "large_empty_chip");
    public static final Item HUGE_EMPTY_CHIP = register(new Item(new Item.Properties()), "huge_empty_chip");
    public static final Item SMALL_CHIP = register(new Item(new Item.Properties().stacksTo(1)), "small_chip");
    public static final Item MEDIUM_CHIP = register(new Item(new Item.Properties().stacksTo(1)), "medium_chip");
    public static final Item LARGE_CHIP = register(new Item(new Item.Properties().stacksTo(1)), "large_chip");
    public static final Item HUGE_CHIP = register(new Item(new Item.Properties().stacksTo(1)), "huge_chip");

    // 杂件
    public static final Item HEAT_SINK_SUBSTRATE = register(new Item(new Item.Properties()), "heat_sink_substrate");
    public static final Item HIGH_VOLTAGE_COIL = register(new Item(new Item.Properties()), "high_voltage_coil");
    public static final Item AMETHYST_LENS = register(new Item(new Item.Properties()), "amethyst_lens");
    public static final Item INSULATING_LAYER = register(new Item(new Item.Properties()), "insulating_layer");
    public static final Item CATALYST = register(new Item(new Item.Properties()), "catalyst");
    public static final Item NETHERITE_FRAGMENT = register(new Item(new Item.Properties()), "netherite_fragment");
    public static final Item PHOTORESIST = register(new Item(new Item.Properties()), "photoresist");
    public static final Item LITHOGRAPHY_MASK = register(new Item(new Item.Properties()), "lithography_mask");
    public static final Item LIGHTNING_BOTTLE = register(new Item(new Item.Properties()), "lightning_bottle");
    public static final Item COMPUTER = register(new Item(new Item.Properties()), "computer");
    public static final Item WASTE_BOTTLE = register(new Item(new Item.Properties()), "waste_bottle");

    public static void initialize() {
        // 物品已在静态块中注册
    }
}