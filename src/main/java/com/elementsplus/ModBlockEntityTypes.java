package com.elementsplus;

import com.elementsplus.blocks.entity.CrystallizerBlockEntity;
import com.mojang.datafixers.types.Type;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import static com.mojang.text2speech.Narrator.LOGGER;

public class ModBlockEntityTypes {
    public static final BlockEntityType<CrystallizerBlockEntity> COUNTER_BLOCK_ENTITY =
            register("counter", CrystallizerBlockEntity::new, ModBlocks.CRYSTALLIZER);

    private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType.BlockEntitySupplier<T> entityFactory, Block... blocks) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, name);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, BlockEntityType.Builder.of(entityFactory, blocks).build());
    }

    public static void initialize() {
    }
}
