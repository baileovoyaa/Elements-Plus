package com.elementsplus.mixin;

import com.elementsplus.ModPotions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 防锈药水只能保持普通药水形态，不能进一步酿造为喷溅/滞留药水。 */
@Mixin(PotionBrewing.class)
public class PotionBrewingMixin {
    @Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
    private void elementsplus$blockRustProofContainerMix(ItemStack input, ItemStack ingredient,
                                                         CallbackInfoReturnable<Boolean> cir) {
        if (elementsplus$isBlocked(input, ingredient)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hasContainerMix", at = @At("HEAD"), cancellable = true)
    private void elementsplus$blockRustProofContainerMixCheck(ItemStack input, ItemStack ingredient,
                                                              CallbackInfoReturnable<Boolean> cir) {
        if (elementsplus$isBlocked(input, ingredient)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mix", at = @At("HEAD"), cancellable = true)
    private void elementsplus$blockRustProofMix(ItemStack ingredient, ItemStack input,
                                                CallbackInfoReturnable<ItemStack> cir) {
        if (elementsplus$isBlocked(input, ingredient)) {
            cir.setReturnValue(input);
        }
    }

    private static boolean elementsplus$isBlocked(ItemStack input, ItemStack ingredient) {
        if (input == null || ingredient == null) {
            return false;
        }
        if (!ingredient.is(Items.GUNPOWDER) && !ingredient.is(Items.DRAGON_BREATH)) {
            return false;
        }
        return input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .is(ModPotions.RUST_PROOF);
    }
}
