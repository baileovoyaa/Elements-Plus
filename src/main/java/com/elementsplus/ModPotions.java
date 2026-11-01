package com.elementsplus;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

public class ModPotions {
    public static Holder<Potion> RUST_PROOF;

    public static void initialize() {
        RUST_PROOF = Registry.registerForHolder(
                BuiltInRegistries.POTION,
                ElementsPlus.id("rust_proof"),
                new Potion("rust_proof", new MobEffectInstance(ModEffects.RUST_PROOF, 2400, 0))
        );
        ElementsPlus.LOGGER.info("Registered custom potion(s)");
    }
}
