package com.elementsplus.mixin;

import com.elementsplus.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    /**
     * 低气压空间按“眼睛处于水中”处理，复用原版的空气消耗、气泡与溺水结算。
     * 低压标记效果由加压机同步给客户端，因此空气条在两端表现一致。
     * 只改写 baseTick 中的这一处判断，不影响其他流体判定。
     */
    @Redirect(method = "baseTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean elementsplus$lowPressureEyeInFluid(LivingEntity entity, TagKey<Fluid> fluidTag) {
        if (entity.isEyeInFluid(fluidTag)) {
            return true;
        }
        return fluidTag == FluidTags.WATER
                && entity instanceof Player
                && entity.hasEffect(ModEffects.LOW_PRESSURE);
    }
}
