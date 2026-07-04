package com.elementsplus;

import com.elementsplus.blocks.SteelPipeLBlock;
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

    public static final Block STEEL_PIPE_L = register(
            new SteelPipeLBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
            ),
            "steel_pipe_l"
    );

    public static final Block STEEL_PIPE_I = register(
            new SteelPipeLBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
            ),
            "steel_pipe_i"
    );

    public static void initialize() {
        Items.registerBlock(new BlockItem(STEEL_PIPE_L, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.steel_pipe_l")
                        .withStyle(style -> style.withColor(0xAAAAAA))); // 灰色
            }
        });
        Items.registerBlock(new BlockItem(STEEL_PIPE_I, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
                tooltip.add(Component.translatable("tooltip.elements-plus.steel_pipe_i")
                        .withStyle(style -> style.withColor(0xAAAAAA))); // 灰色
            }
        });
        ElementsPlus.LOGGER.info("Registered blocks");
    }
}