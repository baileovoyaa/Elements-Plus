package com.elementsplus;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final SoundEvent LITHOGRAPHY = register("lithography");

    private static SoundEvent register(String id) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, id);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, location,
                SoundEvent.createVariableRangeEvent(location));
    }

    public static void initialize() {
        // 静态字段在类加载时完成注册。
    }
}
