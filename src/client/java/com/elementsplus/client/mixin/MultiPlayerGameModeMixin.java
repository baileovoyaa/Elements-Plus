package com.elementsplus.client.mixin;

import com.elementsplus.ModItems;
import com.elementsplus.client.ElementsPlusClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 原版在右键使用键松开时会自动释放所有正在使用的物品。
 * 左键蓄力同样通过“使用物品”来显示平举姿态，因此需要在该分支中保留使用状态，
 * 直到客户端松开左键或主动调用 stopUsingItem 为止。
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
    private void elementsplus$keepSyringeChargePose(Player player, CallbackInfo ci) {
        if (ElementsPlusClient.isSyringeCharging()
                && player.getUseItem().is(ModItems.SYRINGE)) {
            ci.cancel();
        }
    }
}
