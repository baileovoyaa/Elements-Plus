package com.elementsplus.mixin;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningRodBlock.class)
public class LightningRodBlockMixin {
    @Inject(method = "onLightningStrike", at = @At("RETURN"))
    public void onLightningStrike(BlockState blockState, Level level, BlockPos blockPos, CallbackInfo ci) {
        ElementsPlus.LOGGER.info("Lightning rod struck at {}", blockPos);
        level.setBlockAndUpdate(blockPos, ModBlocks.CHARGED_LIGHTNING_ROD.withPropertiesOf(blockState));
    }
}
