package com.elementsplus.client.mixin;

import com.elementsplus.client.WrenchInterfaceClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void elementsplus$wrenchScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (WrenchInterfaceClient.handleScroll(yOffset)) {
            ci.cancel();
        }
    }
}
