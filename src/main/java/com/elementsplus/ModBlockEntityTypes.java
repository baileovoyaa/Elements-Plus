package com.elementsplus;

import com.elementsplus.blocks.entity.CrystallizerBlockEntity;
import com.elementsplus.blocks.entity.ExperimentTableBlockEntity;
import com.elementsplus.blocks.entity.MetalCatalystBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntityTypes {
    public static final BlockEntityType<CrystallizerBlockEntity> CRYSTALLIZER = register("crystallizer", CrystallizerBlockEntity::new, ModBlocks.CRYSTALLIZER);
    public static final BlockEntityType<MetalCatalystBlockEntity> METAL_CATALYST = register("metal_catalyst", MetalCatalystBlockEntity::new, ModBlocks.METAL_CATALYST);
    public static final BlockEntityType<ExperimentTableBlockEntity> EXPERIMENT_TABLE = register("experiment_table", ExperimentTableBlockEntity::new, ModBlocks.EXPERIMENT_TABLE);

    private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType.BlockEntitySupplier<T> entityFactory, Block... blocks) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, name);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, BlockEntityType.Builder.of(entityFactory, blocks).build());
    }

    public static void initialize() {
    }
}
