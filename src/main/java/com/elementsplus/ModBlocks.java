package com.elementsplus;

import com.elementsplus.blocks.AdvancedCraftingTableBlock;
import com.elementsplus.blocks.CrystallizerBlock;
import com.elementsplus.blocks.pipe.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;

public class ModBlocks {

    public static Block register(Block block, String id) {
        ResourceLocation blockID = ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, id);
        return Registry.register(BuiltInRegistries.BLOCK, blockID, block);
    }

    // ===== 铁管 4 种形状（未生锈） =====
    public static final Block STEEL_PIPE_L = register(
            new PipeLBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion(),
                    PipeMaterials.STEEL
            ),
            "steel_pipe_l"
    );

    public static final Block STEEL_PIPE_I = register(
            new PipeIBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion(),
                    PipeMaterials.STEEL
            ),
            "steel_pipe_i"
    );

    public static final Block STEEL_PIPE_T = register(
            new PipeTBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion(),
                    PipeMaterials.STEEL
            ),
            "steel_pipe_t"
    );

    public static final Block STEEL_PIPE_X = register(
            new PipeXBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion(),
                    PipeMaterials.STEEL
            ),
            "steel_pipe_x"
    );

    // ===== 锈管 4 种形状（已生锈，可氧化） =====
    public static final Block RUST_STEEL_PIPE_L = register(
            new PipeLBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .sound(SoundType.STONE)
                            .noOcclusion(),
                    PipeMaterials.RUST
            ),
            "rust_steel_pipe_l"
    );

    public static final Block RUST_STEEL_PIPE_I = register(
            new PipeIBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .sound(SoundType.STONE)
                            .noOcclusion(),
                    PipeMaterials.RUST
            ),
            "rust_steel_pipe_i"
    );

    public static final Block RUST_STEEL_PIPE_T = register(
            new PipeTBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .sound(SoundType.STONE)
                            .noOcclusion(),
                    PipeMaterials.RUST
            ),
            "rust_steel_pipe_t"
    );

    public static final Block RUST_STEEL_PIPE_X = register(
            new PipeXBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .sound(SoundType.STONE)
                            .noOcclusion(),
                    PipeMaterials.RUST
            ),
            "rust_steel_pipe_x"
    );

    public static final Block SILVER_PIPE_I = register(
            new PipeIBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .sound(SoundType.STONE)
                            .noOcclusion(),
                    PipeMaterials.SILVER
            ),
            "silver_pipe_i"
    );

    public static final Block SILVER_PIPE_L = register(
            new PipeLBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .sound(SoundType.STONE)
                            .noOcclusion(),
                    PipeMaterials.SILVER
            ),
            "silver_pipe_l"
    );


    public static final Block SILVER_PIPE_T = register(
            new PipeTBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .sound(SoundType.STONE)
                            .noOcclusion(),
                    PipeMaterials.SILVER
            ),
            "silver_pipe_t"
    );


    public static final Block SILVER_PIPE_X = register(
            new PipeXBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .sound(SoundType.STONE)
                            .noOcclusion(),
                    PipeMaterials.SILVER
            ),
            "silver_pipe_x"
    );

    public static final Block CRYSTALLIZER = register(
            new CrystallizerBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
            ),
            "crystallizer"
    );

    public static final Block ADVANCED_CRAFTING_TABLE = register(
            new AdvancedCraftingTableBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
            ),
            "advanced_crafting_table"
    );

    public static final Block METAL_CATALYST = register(
            new Block(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
            ),
            "metal_catalyst"
    );

    public static final Block LITHOGRAPHY_MACHINE = register(
            new Block(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
            ),
            "lithography_machine"
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
        Items.registerBlock(new BlockItem(SILVER_PIPE_I, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.silver_pipe_i")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
            }
        });
        Items.registerBlock(new BlockItem(SILVER_PIPE_L, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.silver_pipe_l")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
            }
        });
        Items.registerBlock(new BlockItem(SILVER_PIPE_T, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.silver_pipe_t")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
            }
        });
        Items.registerBlock(new BlockItem(SILVER_PIPE_X, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.silver_pipe_x")
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

        // ===== 高级工作台 BlockItem =====
        Items.registerBlock(new BlockItem(ADVANCED_CRAFTING_TABLE, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.advanced_crafting_table")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
            }
        });

        // ===== 金属催化器 BlockItem =====
        Items.registerBlock(new BlockItem(METAL_CATALYST, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.metal_catalyst")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
            }
        });

        // ===== 光刻机 BlockItem =====
        Items.registerBlock(new BlockItem(LITHOGRAPHY_MACHINE, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.lithography_machine")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
            }
        });

        ElementsPlus.LOGGER.info("Registered blocks");
    }
}