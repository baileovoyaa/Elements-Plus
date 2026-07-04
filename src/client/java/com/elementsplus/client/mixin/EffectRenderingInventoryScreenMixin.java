package com.elementsplus.client.mixin;

import com.elementsplus.ModEffects;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.stream.Collectors;

@Mixin(EffectRenderingInventoryScreen.class)
public class EffectRenderingInventoryScreenMixin {
    @ModifyExpressionValue(method = "renderEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getActiveEffects()Ljava/util/Collection;"))
    private Collection<MobEffectInstance> filterEffects(Collection<MobEffectInstance> originalList) {
        // 这里的 originalList 就是原始的 getActiveEffects() 返回的结果
        if (originalList == null || originalList.isEmpty()) {
            return originalList;
        }

        boolean hasSteelPipeShield = false;
        for (MobEffectInstance effect : originalList) {
            if (effect.getEffect().value() == ModEffects.STEEL_PIPE_SHIELD.value()) {
                hasSteelPipeShield = true;
                break;
            }
        }

        // 如果为true，过滤掉抗火和抗性提升
        if (hasSteelPipeShield) {
            return originalList.stream()
                    .filter(effect -> effect.getEffect().value() != MobEffects.FIRE_RESISTANCE.value() && effect.getEffect().value() != MobEffects.DAMAGE_RESISTANCE.value())
                    .collect(Collectors.toList());
        } else {
            return originalList;
        }
    }

}
