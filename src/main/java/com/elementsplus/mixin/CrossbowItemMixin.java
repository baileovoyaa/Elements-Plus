package com.elementsplus.mixin;

import com.elementsplus.ModItems;
import com.elementsplus.entity.SyringeProjectile;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/** 让原版弩接受注射器作为弹药，并发射注射器弹射物。 */
@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {
    @Inject(method = "getSupportedHeldProjectiles", at = @At("RETURN"), cancellable = true)
    private void elementsplus$addSyringeToHeldProjectiles(CallbackInfoReturnable<Predicate<ItemStack>> cir) {
        Predicate<ItemStack> original = cir.getReturnValue();
        cir.setReturnValue(original.or(stack -> stack.is(ModItems.SYRINGE)));
    }

    @Inject(method = "getAllSupportedProjectiles", at = @At("RETURN"), cancellable = true)
    private void elementsplus$addSyringeToProjectiles(CallbackInfoReturnable<Predicate<ItemStack>> cir) {
        Predicate<ItemStack> original = cir.getReturnValue();
        cir.setReturnValue(original.or(stack -> stack.is(ModItems.SYRINGE)));
    }

    @Inject(method = "createProjectile", at = @At("HEAD"), cancellable = true)
    private void elementsplus$createSyringeProjectile(Level level, LivingEntity shooter, ItemStack weapon,
                                                      ItemStack ammo, boolean isCrit,
                                                      CallbackInfoReturnable<Projectile> cir) {
        if (ammo.is(ModItems.SYRINGE)) {
            cir.setReturnValue(new SyringeProjectile(level, shooter, ammo));
        }
    }
}
