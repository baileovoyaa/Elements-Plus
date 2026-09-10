package com.elementsplus.client.mixin;

import com.elementsplus.client.gui.Point;
import com.elementsplus.client.gui.SlotPositionProvider;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Shadow
    protected abstract boolean isHovering(int i, int j, int k, int l, double d, double e);

    @ModifyArgs(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;III)V"
            )
    )
    private void modifySlotHighlightPos(Args args) {
        if ((Object) this instanceof SlotPositionProvider provider) {
            Slot slot = this.hoveredSlot;
            if (slot == null) return;
            Point point = provider.getSlotPosition(slot);
            args.set(1, point.x());
            args.set(2, point.y());
        }
    }

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    private void isHovering(Slot slot, double d, double e, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof SlotPositionProvider provider) {
            Point point = provider.getSlotPosition(slot);
            if (point == null) {
                cir.setReturnValue(false);
            } else {
                cir.setReturnValue(this.isHovering(point.x(), point.y(), 16, 16, d, e));
            }
        }
    }
}