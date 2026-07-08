package com.elementsplus.mixin;

import net.minecraft.world.level.block.RedStoneWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RedStoneWireBlock.class)
public interface RedStoneWireBlockAccessor {
    @Accessor("shouldSignal")
    void setShouldSignal(boolean signal);

    @Accessor("shouldSignal")
    boolean isShouldSignal();
}