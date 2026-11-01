package com.elementsplus.client.mixin;

import com.elementsplus.ModItems;
import com.elementsplus.client.ElementsPlusClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 原版在 isUsingItem() 时会给移动输入乘 0.2，并禁止开始疾跑。
 * 左键注射器蓄力借用物品使用姿态，但不应继承这两种减速；右键注射仍保留原版行为。
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"
            )
    )
    private boolean elementsplus$ignoreChargeMovementSlowdown(LocalPlayer player) {
        return !elementsplus$isLeftCharging(player) && player.isUsingItem();
    }

    @Redirect(
            method = "canStartSprinting",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"
            )
    )
    private boolean elementsplus$allowSprintWhileCharging(LocalPlayer player) {
        return !elementsplus$isLeftCharging(player) && player.isUsingItem();
    }

    private static boolean elementsplus$isLeftCharging(LocalPlayer player) {
        return ElementsPlusClient.isSyringeCharging()
                && player.getUseItem().is(ModItems.SYRINGE);
    }
}
