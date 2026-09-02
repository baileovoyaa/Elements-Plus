package com.elementsplus;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ElementsPlus implements ModInitializer {
    public static final String MOD_ID = "elements-plus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.initialize();
        ModItemGroups.registerAll();
        ModBlocks.initialize();
        ModEffects.initialize();
        ModBlockEntityTypes.initialize();
        LOGGER.info("Hello Fabric world!");

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide) {
                if (player.getItemInHand(hand).is(Items.GLASS_BOTTLE) && hitResult.getType() == HitResult.Type.BLOCK && world.getBlockState(hitResult.getBlockPos()).is(ModBlocks.CHARGED_LIGHTNING_ROD)) {
                    world.setBlockAndUpdate(hitResult.getBlockPos(), Blocks.LIGHTNING_ROD.withPropertiesOf(world.getBlockState(hitResult.getBlockPos())));
                    ItemStack itemStack = player.getItemInHand(hand);
                    itemStack.shrink(1);
                    world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (itemStack.isEmpty()) {
                        player.setItemInHand(hand, new ItemStack(ModItems.LIGHTNING_BOTTLE));
                    } else if (!player.getInventory().add(new ItemStack(ModItems.LIGHTNING_BOTTLE))) {
                        player.drop(new ItemStack(ModItems.LIGHTNING_BOTTLE), false);
                    }
                    world.gameEvent(player, GameEvent.FLUID_PICKUP, hitResult.getBlockPos());
                    return InteractionResult.CONSUME;
                }
            }
            return InteractionResult.PASS;
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
