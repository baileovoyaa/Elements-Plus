package com.elementsplus.mixin;

import com.elementsplus.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Final
    @Shadow
    private Map<Holder<MobEffect>, MobEffectInstance> activeEffects;  // 影子字段

    @Inject(at = @At("HEAD"), method = "hasEffect", cancellable = true)
    public void modifyHasEffect(Holder<MobEffect> holder, CallbackInfoReturnable<Boolean> cir) {
        if (holder.value() == MobEffects.DAMAGE_RESISTANCE.value() || holder.value() == MobEffects.FIRE_RESISTANCE.value()) {
            if (this.activeEffects.containsKey(ModEffects.STEEL_PIPE_SHIELD)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "getEffect", cancellable = true)
    public void modifyGetEffect(Holder<MobEffect> holder, CallbackInfoReturnable<MobEffectInstance> cir) {
        if (holder.value() == MobEffects.DAMAGE_RESISTANCE.value()) {
            if (!this.activeEffects.containsKey(MobEffects.DAMAGE_RESISTANCE)) {
                cir.setReturnValue(this.activeEffects.get(ModEffects.STEEL_PIPE_SHIELD));
            }
        } else if (holder.value() == MobEffects.FIRE_RESISTANCE.value()) {
            if (!this.activeEffects.containsKey(MobEffects.FIRE_RESISTANCE)) {
                cir.setReturnValue(this.activeEffects.get(ModEffects.STEEL_PIPE_SHIELD));
            }
        }
    }
}
