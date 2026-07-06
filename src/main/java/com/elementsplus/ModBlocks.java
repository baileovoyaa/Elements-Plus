package com.elementsplus;

import com.elementsplus.blocks.CrystallizerBlock;
import com.elementsplus.blocks.RustSteelPipe;
import com.elementsplus.blocks.WeatheringRustSteelPipe;
import com.elementsplus.blocks.SteelPipeBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;

import static com.elementsplus.blocks.WeatheringRustSteelPipe.WeatherState.UNAFFECTED;

public class ModBlocks {

    public static Block register(Block block, String id) {
        ResourceLocation blockID = ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, id);
        return Registry.register(BuiltInRegistries.BLOCK, blockID, block);
    }

    // ===== 铁管 4 种形状（未生锈） =====
    public static final Block STEEL_PIPE_L = register(
            new SteelPipeBlock(UNAFFECTED, BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
            ),
            "steel_pipe_l"
    );

    public static final Block STEEL_PIPE_I = register(
            new SteelPipeBlock(UNAFFECTED, BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
            ),
            "steel_pipe_i"
    );

    public static final Block STEEL_PIPE_T = register(
            new SteelPipeBlock(UNAFFECTED, BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
            ),
            "steel_pipe_t"
    );

    public static final Block STEEL_PIPE_X = register(
            new SteelPipeBlock(UNAFFECTED, BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
            ),
            "steel_pipe_x"
    );

    // ===== 锈管 4 种形状（已生锈，可氧化） =====
    // 使用 RustSteelPipeFullBlock，传入 WeatherState.UNAFFECTED（起始状态）
    // 会通过 randomTick 氧化为 RUST 状态
    public static final Block RUST_STEEL_PIPE_L = register(
            new RustSteelPipe(
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .sound(SoundType.STONE)
                            .noOcclusion()
            ),
            "rust_steel_pipe_l"
    );

    public static final Block RUST_STEEL_PIPE_I = register(
            new RustSteelPipe(
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .sound(SoundType.STONE)
                            .noOcclusion()
            ),
            "rust_steel_pipe_i"
    );

    public static final Block RUST_STEEL_PIPE_T = register(
            new RustSteelPipe(
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .sound(SoundType.STONE)
                            .noOcclusion()
            ),
            "rust_steel_pipe_t"
    );

    public static final Block RUST_STEEL_PIPE_X = register(
            new RustSteelPipe(
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .sound(SoundType.STONE)
                            .noOcclusion()
            ),
            "rust_steel_pipe_x"
    );

    public static final Block CRYSTALLIZER = register(
            new CrystallizerBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
            ),
            "crystallizer"
    );

    public static void initialize() {
        // ===== 铁管 BlockItem =====
        Items.registerBlock(new BlockItem(STEEL_PIPE_L, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.steel_pipe_l")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
            }
        });
        Items.registerBlock(new BlockItem(STEEL_PIPE_I, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.steel_pipe_i")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
            }
        });
        Items.registerBlock(new BlockItem(STEEL_PIPE_T, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.steel_pipe_t")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
            }
        });
        Items.registerBlock(new BlockItem(STEEL_PIPE_X, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.steel_pipe_x")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
            }
        });

        // ===== 锈管 BlockItem =====
        Items.registerBlock(new BlockItem(RUST_STEEL_PIPE_L, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.rust_steel_pipe_l")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
            }
        });
        Items.registerBlock(new BlockItem(RUST_STEEL_PIPE_I, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.rust_steel_pipe_i")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
            }
        });
        Items.registerBlock(new BlockItem(RUST_STEEL_PIPE_T, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.rust_steel_pipe_t")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
            }
        });
        Items.registerBlock(new BlockItem(RUST_STEEL_PIPE_X, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.rust_steel_pipe_x")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
            }
        });

        // ===== 结晶器 BlockItem =====
        Items.registerBlock(new BlockItem(CRYSTALLIZER, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.crystallizer")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
            }
        });

        ElementsPlus.LOGGER.info("Registered blocks");
    }
}