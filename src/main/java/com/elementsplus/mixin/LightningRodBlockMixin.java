package com.elementsplus.mixin;

import com.elementsplus.ElementsPlus;
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
    @Inject(method = "onLightningStrike", at = @At("HEAD"))
    public void onLightningStrike(BlockState blockState, Level level, BlockPos blockPos, CallbackInfo ci) {
        ElementsPlus.LOGGER.info("Lightning rod struck at {}", blockPos);
        if (level.getEntitiesOfClass(Marker.class, new AABB(blockPos)).isEmpty()) {
            Marker marker = EntityType.MARKER.create(level);
            if (marker != null) {
                marker.setPos(Vec3.atCenterOf(blockPos));
                marker.setCustomName(Component.literal("Lightning Strike"));
                marker.addTag("lightning_strike");
            }
            level.addFreshEntity(marker);
        }
    }
}
