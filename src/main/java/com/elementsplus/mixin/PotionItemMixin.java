package com.elementsplus.mixin;

import com.elementsplus.ModPotions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 防锈药水不能通过饮用获得效果，只能注射。 */
@Mixin(PotionItem.class)
public class PotionItemMixin {
    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void elementsplus$blockRustProofDrink(ItemStack stack, Level level, LivingEntity entity,
                                                  CallbackInfoReturnable<ItemStack> cir) {
        if (!stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .is(ModPotions.RUST_PROOF)) {
            return;
        }

        if (!level.isClientSide()) {
            if (entity instanceof Player player) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.displayClientMessage(
                        Component.translatable("message.elements-plus.syringe.drink_only"), true);
            }
        }

        cir.setReturnValue(stack.isEmpty() ? new ItemStack(Items.GLASS_BOTTLE) : stack);
    }
}
