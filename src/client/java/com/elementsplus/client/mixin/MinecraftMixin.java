package com.elementsplus.client.mixin;

import com.elementsplus.client.ElementsPlusClient;
import com.elementsplus.client.SyringeTargeting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    /** 左键蓄力期间禁用右键使用，避免两种使用状态互相覆盖。 */
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void elementsplus$blockRightClickWhileCharging(CallbackInfo ci) {
        if (ElementsPlusClient.isSyringeCharging()) {
            ci.cancel();
        }
    }

    /**
     * 只在渲染轮廓时返回 true，不修改实体自身的发光同步标志。
     * 若覆盖 Entity#isCurrentlyGlowing，LivingEntity 会把结果写回 shared flag，
     * 造成高亮在移开视线后仍然残留。
     */
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void elementsplus$renderSyringeTargetOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (SyringeTargeting.isTarget(entity)) {
            cir.setReturnValue(true);
        }
    }
}
