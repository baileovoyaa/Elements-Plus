package com.elementsplus.client.mixin;

import com.elementsplus.client.ElementsPlusClient;
import com.elementsplus.item.SyringeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 左键蓄力使用 SPEAR 姿态，右键注射使用 BOW 姿态。 */
@Mixin(SyringeItem.class)
public class SyringeItemMixin {
    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void elementsplus$syringeUseAnimation(ItemStack stack, CallbackInfoReturnable<UseAnim> cir) {
        cir.setReturnValue(ElementsPlusClient.isSyringeCharging() ? UseAnim.SPEAR : UseAnim.BOW);
    }
}
