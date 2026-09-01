package com.elementsplus.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Marker.class)
public class MarkerMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        Marker marker = (Marker) (Object) this;
        if (marker.getTags().contains("lightning_strike") && !marker.level().getBlockState(marker.blockPosition()).is(Blocks.LIGHTNING_ROD)) {
            marker.remove(Entity.RemovalReason.DISCARDED);
        }
    }
}
